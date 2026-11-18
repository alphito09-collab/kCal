package com.alphito.kcal.feature.steps.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Receives the notification "Stop" action. Does no work on the main thread beyond stopService. */
class StepTrackingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_TRACKING) {
            context.stopService(Intent(context, StepCounterService::class.java))
        }
    }

    companion object {
        const val ACTION_STOP_TRACKING = "com.alphito.kcal.steps.STOP_TRACKING"
    }
}
