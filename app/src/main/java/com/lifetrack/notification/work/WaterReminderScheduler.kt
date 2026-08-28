package com.lifetrack.notification.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lifetrack.LifeTrackApplication
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Schedules (or cancels) the interval water reminder.
 *
 * A true [android.app.AlarmManager] exact alarm was deliberately not used here: on
 * API 31+ that needs `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`, both subject to Play
 * Store policy review and intended for actual alarm-clock apps, not a wellness nudge.
 * A `PeriodicWorkRequest` needs no special permission and is the same mechanism the
 * daily digest already relies on — the trade-off is the OS may shift the exact
 * minute it fires by a few minutes around Doze, which is an acceptable trade for a
 * "roughly every N minutes" reminder.
 */
object WaterReminderScheduler {

    suspend fun apply(context: Context) {
        val app = context.applicationContext as LifeTrackApplication
        val preferences = app.container.preferencesRepository.preferences.first()

        if (!preferences.waterReminderEnabled) {
            cancel(context)
            return
        }

        // WorkManager rejects a periodic interval under 15 minutes outright;
        // PreferencesRepository already clamps on write, this is a second backstop.
        val minutes = preferences.waterReminderIntervalMinutes.coerceAtLeast(15).toLong()
        val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(minutes, TimeUnit.MINUTES).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WaterReminderWorker.WORK_NAME,
            // UPDATE so changing the interval takes effect without waiting out the
            // old period first, and without ever running two copies at once.
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WaterReminderWorker.WORK_NAME)
    }
}
