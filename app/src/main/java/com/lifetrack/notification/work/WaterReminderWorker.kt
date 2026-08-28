package com.lifetrack.notification.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifetrack.LifeTrackApplication
import com.lifetrack.notification.Notifier
import com.lifetrack.notification.domain.DailyDigest
import com.lifetrack.water.data.WaterGoal
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * The interval "drink water" nudge (session 12) — separate from [DailyDigestWorker]
 * on purpose. See [com.lifetrack.core.data.AppPreferences.waterReminderEnabled] for
 * why this is exempt from the app's usual "one consolidated notification" rule.
 *
 * Two things keep this from nagging when there is no point:
 *  - **Waking hours only.** Reuses [DailyDigest]'s existing 08:00–22:00 window rather
 *    than inventing a second one — a fixed-interval alert has no other natural
 *    "quiet hours" concept, and this one already exists and is tested.
 *  - **Stops once today's target is met.** No reason to keep pinging after the goal
 *    is hit; the reminder resumes tomorrow.
 */
class WaterReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as LifeTrackApplication).container
        val preferences = container.preferencesRepository.preferences.first()

        // Defensive: guards a race where this run was already queued when the user
        // just turned the reminder off. The scheduler is the source of truth for
        // whether future runs happen at all.
        if (!preferences.waterReminderEnabled) return Result.success()

        val now = LocalTime.now()
        if (now.isBefore(DailyDigest.DAY_START) || !now.isBefore(DailyDigest.DAY_END)) {
            return Result.success()
        }

        val today = LocalDate.now()
        val drunkMl = container.waterRepository.observeLogsBetween(today, today).first().sumOf { it.mlAmount }
        val targetMl = container.waterRepository.observeGoal().first()?.dailyTargetMl
            ?: WaterGoal.DEFAULT_DAILY_TARGET_ML

        if (drunkMl >= targetMl) return Result.success()

        Notifier.postWaterReminder(
            context = applicationContext,
            drunkMl = drunkMl,
            targetMl = targetMl,
            smallMl = preferences.waterIncrementSmallMl,
            largeMl = preferences.waterIncrementLargeMl,
        )
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "lifetrack_water_reminder"
    }
}
