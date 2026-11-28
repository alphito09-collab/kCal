package com.alphito.kcal.feature.steps.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alphito.kcal.core.common.di.ApplicationScope
import com.alphito.kcal.feature.steps.domain.StepTrackingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Receives the notification "Stop" action. Goes through the controller so the "tracking on" preference turns off too. */
class StepTrackingReceiver : BroadcastReceiver(), KoinComponent {
    private val controller: StepTrackingController by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_TRACKING) controller.stop()
    }

    companion object {
        const val ACTION_STOP_TRACKING = "com.alphito.kcal.steps.STOP_TRACKING"
    }
}

/** Resumes tracking after a reboot (or an app update) if the user had it on. */
class StepTrackingBootReceiver : BroadcastReceiver(), KoinComponent {
    private val controller: StepTrackingController by inject()
    private val appScope: CoroutineScope by inject(ApplicationScope)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        appScope.launch {
            try { controller.resumeIfEnabled() } finally { pending.finish() }
        }
    }
}
