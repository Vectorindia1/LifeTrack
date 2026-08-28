package com.lifetrack.notification.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lifetrack.LifeTrackApplication
import com.lifetrack.notification.domain.DailyDigest
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Schedules the next digest check.
 *
 * Uses a chain of one-shot jobs rather than `PeriodicWorkRequest` because the checks
 * are at irregular times of day (09:00, 14:00, 18:00, 20:00, 20:30, 21:30 by default),
 * which a fixed repeat interval cannot express. Each run schedules its successor —
 * see [DailyDigestWorker].
 *
 * WorkManager persists across reboot and process death, which is why PRD 5 chose it
 * over AlarmManager.
 */
object DigestScheduler {

    suspend fun scheduleNext(context: Context) {
        val app = context.applicationContext as LifeTrackApplication
        val reminders = app.container.notificationSettingsRepository.enabledReminders()

        val now = LocalDateTime.now()
        val nextToday = DailyDigest.nextCheckAfter(now.toLocalTime(), reminders)
        val next: LocalDateTime? = when {
            nextToday != null -> now.toLocalDate().atTime(nextToday)
            else -> DailyDigest.firstCheckOfDay(reminders)
                ?.let { now.toLocalDate().plusDays(1).atTime(it) }
        }

        if (next == null) {
            // Every category disabled: stop scheduling and clear anything showing.
            cancel(context)
            return
        }

        val delay = Duration.between(now, next).coerceAtLeast(Duration.ofMinutes(1))
        val request = OneTimeWorkRequestBuilder<DailyDigestWorker>()
            .setInitialDelay(delay)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DailyDigestWorker.WORK_NAME,
            // REPLACE so a settings change re-times the pending check instead of
            // queueing a second one alongside it.
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DailyDigestWorker.WORK_NAME)
    }

    /** Next check time, for showing in Settings (milestone 10). */
    fun nextCheckTime(reminders: Map<com.lifetrack.notification.data.FeatureType, List<LocalTime>>): LocalDateTime? {
        val now = LocalDateTime.now()
        val today: LocalDate = now.toLocalDate()
        return DailyDigest.nextCheckAfter(now.toLocalTime(), reminders)?.let { today.atTime(it) }
            ?: DailyDigest.firstCheckOfDay(reminders)?.let { today.plusDays(1).atTime(it) }
    }
}
