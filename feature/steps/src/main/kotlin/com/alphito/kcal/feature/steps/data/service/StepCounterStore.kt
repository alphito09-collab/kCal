package com.alphito.kcal.feature.steps.data.service

import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import com.alphito.kcal.feature.steps.domain.StepCounterTracker

/**
 * Remembers the last step-counter reading whose steps are saved, so a service restarted by the
 * system recovers the steps walked in between instead of losing them.
 *
 * The reading is only valid for the same boot (the counter restarts at 0 after a reboot) and the
 * same day (steps from a gap spanning midnight can't be split between the two days).
 */
internal class StepCounterStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("step_counter_state", Context.MODE_PRIVATE)

    fun restore(epochDay: Long): StepCounterTracker.Restored? {
        if (!prefs.contains(KEY_VALUE)) return null
        if (prefs.getInt(KEY_BOOT, -1) != bootCount() || prefs.getLong(KEY_DAY, -1) != epochDay) return null
        return StepCounterTracker.Restored(prefs.getFloat(KEY_VALUE, 0f), prefs.getInt(KEY_LEAD, 0))
    }

    /** Called only after the steps up to [value] are in Room. */
    fun save(value: Float, detectorLead: Int, epochDay: Long) = prefs.edit {
        putFloat(KEY_VALUE, value).putInt(KEY_LEAD, detectorLead).putInt(KEY_BOOT, bootCount()).putLong(KEY_DAY, epochDay)
    }

    /** Tracking was stopped on purpose: steps walked until it's started again must not count. */
    fun clear() = prefs.edit { clear() }

    private fun bootCount(): Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    private companion object {
        const val KEY_VALUE = "counter_value"
        const val KEY_LEAD = "detector_lead"
        const val KEY_BOOT = "boot_count"
        const val KEY_DAY = "epoch_day"
    }
}
