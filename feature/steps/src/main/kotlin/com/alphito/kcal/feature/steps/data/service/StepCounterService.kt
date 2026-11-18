package com.alphito.kcal.feature.steps.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import com.alphito.kcal.core.common.di.ApplicationScope
import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.data.repository.StepsRepository
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.feature.steps.R
import com.alphito.kcal.feature.steps.domain.StepTrackingController
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.concurrent.atomic.AtomicInteger

/**
 * Health foreground service that counts steps with the hardware step counter.
 *
 * ANR / battery safety:
 *  - sensor callbacks only add to an AtomicInteger (no I/O on the main thread);
 *  - a coroutine on the IO dispatcher flushes deltas to Room every few seconds;
 *  - the sensor is registered with batching (`maxReportLatencyUs`) so the SoC can sleep;
 *  - `START_NOT_STICKY` – tracking only runs while the user explicitly wants it.
 */
class StepCounterService : Service(), SensorEventListener {

    private val repository: StepsRepository by inject()
    private val clock: AppClock by inject()
    private val dispatchers: DispatcherProvider by inject()
    private val controller: StepTrackingController by inject()
    private val appScope: CoroutineScope by inject(ApplicationScope)

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatchers.io + CoroutineExceptionHandler { _, e -> Log.e(TAG, "step flush failed", e) })
    }
    private val pending = AtomicInteger(0)
    private val flushSignal = Channel<Unit>(Channel.CONFLATED)
    private var lastCounterValue = -1f
    private var todayTotal = 0
    private var sensorManager: SensorManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf(); return START_NOT_STICKY
        }
        startInForeground()
        if (sensorManager == null) registerSensor()
        (controller as? StepTrackingControllerImpl)?.onServiceState(true)
        return START_NOT_STICKY
    }

    private fun startInForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0
        runCatching { ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(todayTotal), type) }
            .onFailure { Log.w(TAG, "Cannot start foreground (missing permission?)", it); stopSelf() }
    }

    private fun registerSensor() {
        val manager = getSystemService<SensorManager>() ?: return stopSelf()
        sensorManager = manager
        val counter = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val detector = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        val sensor = counter ?: detector ?: return stopSelf()
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, BATCH_LATENCY_US)
        scope.launch {
            repository.observeDay(clock.today()).collect { todayTotal = it; updateNotification() }
        }
        scope.launch {
            flushSignal.receiveAsFlow().collect {
                delay(FLUSH_INTERVAL_MS)
                flush()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val delta = when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val value = event.values[0]
                val d = if (lastCounterValue < 0f) 0 else (value - lastCounterValue).toInt()
                lastCounterValue = value
                d.coerceAtLeast(0)
            }
            Sensor.TYPE_STEP_DETECTOR -> event.values.size
            else -> 0
        }
        if (delta > 0) {
            pending.addAndGet(delta)
            flushSignal.trySend(Unit)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private suspend fun flush() {
        val steps = pending.getAndSet(0)
        if (steps > 0) repository.addSteps(clock.today(), steps)
    }

    private fun updateNotification() {
        getSystemService<NotificationManager>()?.notify(NOTIFICATION_ID, buildNotification(todayTotal))
    }

    private fun buildNotification(steps: Int): Notification {
        val manager = getSystemService<NotificationManager>()
        if (manager?.getNotificationChannel(CHANNEL_ID) == null) {
            manager?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.steps_notification_channel), NotificationManager.IMPORTANCE_LOW),
            )
        }
        val stopIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, StepTrackingReceiver::class.java).setAction(StepTrackingReceiver.ACTION_STOP_TRACKING),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 1, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(getString(R.string.steps_notification_title))
            .setContentText(getString(R.string.steps_notification_text, KcalFormat.int(steps)))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.steps_notification_stop), stopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        // Persist whatever is still buffered before the scope dies.
        val remaining = pending.getAndSet(0)
        if (remaining > 0) {
            appScope.launch(dispatchers.io) { repository.addSteps(clock.today(), remaining) }
        }
        scope.cancel()
        (controller as? StepTrackingControllerImpl)?.onServiceState(false)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.alphito.kcal.steps.START"
        const val ACTION_STOP = "com.alphito.kcal.steps.STOP"
        private const val TAG = "StepCounterService"
        private const val CHANNEL_ID = "step_tracking"
        private const val NOTIFICATION_ID = 4_280
        private const val FLUSH_INTERVAL_MS = 4_000L
        private const val BATCH_LATENCY_US = 5_000_000
    }
}
