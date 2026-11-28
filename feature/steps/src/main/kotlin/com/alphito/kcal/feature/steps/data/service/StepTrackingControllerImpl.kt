package com.alphito.kcal.feature.steps.data.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.feature.steps.domain.StepTrackingController
import com.alphito.kcal.feature.steps.domain.TrackingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class StepTrackingControllerImpl(
    private val context: Context,
    private val preferences: PreferencesRepository,
    private val appScope: CoroutineScope,
) : StepTrackingController {

    private val sensorManager = context.getSystemService<SensorManager>()

    override val hasStepSensor: Boolean =
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
            sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null

    private val _status = MutableStateFlow(if (hasStepSensor) TrackingStatus.Idle else TrackingStatus.NoSensor)
    override val status: StateFlow<TrackingStatus> = _status.asStateFlow()

    override fun start() {
        if (!hasStepSensor) return
        appScope.launch { preferences.setActivityTracking(true) }
        startService()
    }

    override fun stop() {
        appScope.launch { preferences.setActivityTracking(false) }
        context.stopService(Intent(context, StepCounterService::class.java))
        // Steps walked while tracking is off must not be counted when it's turned back on.
        StepCounterStore(context).clear()
    }

    override suspend fun resumeIfEnabled() {
        if (!hasStepSensor || _status.value == TrackingStatus.Tracking) return
        if (!preferences.snapshot().activityTrackingEnabled || !hasActivityPermission()) return
        startService()
    }

    private fun startService() {
        // Android 12+ refuses foreground-service starts from the background; the service also
        // guards startForeground, so a refused start just leaves tracking idle.
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, StepCounterService::class.java).setAction(StepCounterService.ACTION_START))
        }.onFailure { Log.w("StepTracking", "Cannot start step tracking now", it) }
    }

    /** Step sensors need ACTIVITY_RECOGNITION from Android 10; on Android 9 there is no runtime permission. */
    private fun hasActivityPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    /** Called by the service – the single source of truth for "is it running". */
    internal fun onServiceState(running: Boolean) {
        _status.value = when {
            !hasStepSensor -> TrackingStatus.NoSensor
            running -> TrackingStatus.Tracking
            else -> TrackingStatus.Idle
        }
    }
}
