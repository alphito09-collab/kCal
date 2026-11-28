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
import com.alphito.kcal.feature.steps.domain.StepCounterTracker
import com.alphito.kcal.feature.steps.domain.StepTrackingController
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.time.LocalDate

/**
 * Health foreground service that counts steps with the hardware step sensors (no accelerometer
 * maths). See [StepCounterTracker] for the counting rules.
 *
 * Responsiveness: both sensors are registered with no batching and every step the tracker accepts
 * is written to Room right away; Room's flow then updates the dashboard and Steps screen.
 *
 * ANR / battery safety:
 *  - sensor callbacks only update in-memory state (no I/O on the main thread);
 *  - writes run on the IO dispatcher, coalesced while one is in flight;
 *  - step sensors are low-power, non-wakeup hardware sensors: they never wake the SoC, and while
 *    it sleeps the hub keeps counting (the cumulative counter catches up on the next reading);
 *  - `START_STICKY` only while the user has tracking on; stopping clears everything.
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
    private val store by lazy { StepCounterStore(applicationContext) }
    private val flushSignal = Channel<Unit>(Channel.CONFLATED)
    private val lock = Any()

    // Guarded by [lock]: sensor callbacks (main thread) and the IO flush both touch them.
    private var tracker: StepCounterTracker? = null
    private var pendingSteps = 0

    private var keepBaseline = false
    private var sensorManager: SensorManager? = null
    private var notificationDay: LocalDate? = null
    private var notificationJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf(); return START_NOT_STICKY
        }
        if (!startInForeground()) return START_NOT_STICKY
        // Registering once per service instance is what keeps repeated starts (screen re-opened,
        // app relaunched) from adding a second listener and counting every step twice.
        if (sensorManager == null) registerSensors()
        (controller as? StepTrackingControllerImpl)?.onServiceState(true)
        // Restarted by the system if it kills the process while the user has tracking on.
        return START_STICKY
    }

    private fun startInForeground(): Boolean {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0
        return runCatching { ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(0), type) }
            .onFailure {
                Log.w(TAG, "Cannot start foreground (missing permission / background start?)", it)
                keepBaseline = true // tracking is still meant to be on; the next start continues from it
                stopSelf()
            }
            .isSuccess
    }

    private fun registerSensors() {
        val manager = getSystemService<SensorManager>() ?: return stopSelf()
        val counter = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val detector = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (counter == null && detector == null) return stopSelf()
        sensorManager = manager
        synchronized(lock) {
            tracker = StepCounterTracker(restored = if (counter != null) store.restore(clock.today().toEpochDay()) else null)
        }
        // maxReportLatencyUs = 0: deliver each event as it happens instead of batching for seconds.
        counter?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI, 0) }
        detector?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI, 0) }
        observeTodayForNotification()
        scope.launch { flushSignal.receiveAsFlow().collect { flush() } }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val save = synchronized(lock) {
            val t = tracker ?: return
            val save = when (event.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> t.onCounter(event.values[0])
                Sensor.TYPE_STEP_DETECTOR -> t.onDetector(event.values.size)
                else -> 0
            }
            pendingSteps += save
            save
        }
        // A counter reading always moves the baseline, so persist it even when it adds no steps.
        if (save > 0 || event.sensor.type == Sensor.TYPE_STEP_COUNTER) flushSignal.trySend(Unit)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Writes the new steps, then the counter reading they lead up to (see [StepCounterStore.save]). */
    private suspend fun flush() {
        val today = clock.today()
        val snapshot = synchronized(lock) {
            val s = pendingSteps
            pendingSteps = 0
            Triple(s, tracker?.lastCounterValue, tracker?.detectorLead ?: 0)
        }
        val (steps, counterValue, lead) = snapshot
        if (steps > 0) repository.addSteps(today, steps)
        // Skipped once the service is stopping, so a late flush can't resurrect a cleared baseline.
        synchronized(lock) { if (tracker != null && counterValue != null) store.save(counterValue, lead, today.toEpochDay()) }
        if (today != notificationDay) observeTodayForNotification()
    }

    /** The notification follows today's total and moves to the new day after midnight. */
    private fun observeTodayForNotification() {
        val today = clock.today()
        notificationDay = today
        notificationJob?.cancel()
        notificationJob = scope.launch {
            repository.observeDay(today).conflate().collect {
                updateNotification(it)
                delay(NOTIFICATION_INTERVAL_MS) // the system rate-limits notification updates anyway
            }
        }
    }

    private fun updateNotification(steps: Int) {
        getSystemService<NotificationManager>()?.notify(NOTIFICATION_ID, buildNotification(steps))
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

    /** Stopped by the user, the notification, or a refused start (process deaths never get here). */
    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        val remaining = synchronized(lock) {
            val s = pendingSteps
            pendingSteps = 0
            tracker = null
            // Steps walked while tracking is off must not count when it's turned back on. Under the
            // lock with tracker = null, so an in-flight flush can't save the baseline again afterwards.
            if (!keepBaseline) store.clear()
            s
        }
        // Persist whatever is still buffered before the scope dies.
        if (remaining > 0) {
            val today = clock.today()
            appScope.launch(dispatchers.io) { repository.addSteps(today, remaining) }
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
        private const val NOTIFICATION_INTERVAL_MS = 2_000L
    }
}
