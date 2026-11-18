package com.alphito.kcal.feature.steps.data.service

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.alphito.kcal.feature.steps.domain.StepTrackingController
import com.alphito.kcal.feature.steps.domain.TrackingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class StepTrackingControllerImpl(private val context: Context) : StepTrackingController {

    private val sensorManager = context.getSystemService<SensorManager>()

    override val hasStepSensor: Boolean =
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
            sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null

    private val _status = MutableStateFlow(if (hasStepSensor) TrackingStatus.Idle else TrackingStatus.NoSensor)
    override val status: StateFlow<TrackingStatus> = _status.asStateFlow()

    override fun start() {
        if (!hasStepSensor) return
        ContextCompat.startForegroundService(context, Intent(context, StepCounterService::class.java).setAction(StepCounterService.ACTION_START))
    }

    override fun stop() {
        context.stopService(Intent(context, StepCounterService::class.java))
    }

    /** Called by the service – the single source of truth for "is it running". */
    internal fun onServiceState(running: Boolean) {
        _status.value = when {
            !hasStepSensor -> TrackingStatus.NoSensor
            running -> TrackingStatus.Tracking
            else -> TrackingStatus.Idle
        }
    }
}
