package com.lifetrack.notification.work

import android.content.Context
import com.lifetrack.LifeTrackApplication
import com.lifetrack.calorie.data.CalorieGoal
import com.lifetrack.goal.data.GoalProgress
import com.lifetrack.habit.data.HabitSchedule
import com.lifetrack.notification.Notifier
import com.lifetrack.notification.domain.DailyDigest
import com.lifetrack.notification.domain.DigestSnapshot
import com.lifetrack.notification.toLine
import com.lifetrack.water.data.WaterGoal
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * Gathers every tracker's state, builds the digest, and posts it — the one piece of
 * logic shared by the scheduled [DailyDigestWorker] and Settings' manual "send a test
 * notification now" action. Keeping this in one place means the two can never drift:
 * whatever the test button shows is exactly what the real 20:00 check would show.
 */
object DigestRunner {

    suspend fun run(context: Context, ignoreTiming: Boolean = false) {
        val container = (context.applicationContext as LifeTrackApplication).container
        val today = LocalDate.now()
        val now = LocalTime.now()

        val reminders = container.notificationSettingsRepository.enabledReminders()

        val habits = container.habitRepository.observeHabits().first()
        val completions = container.habitRepository.observeRecentCompletions(today).first()
        val doneToday = completions.filter { it.date == today }.mapTo(mutableSetOf()) { it.habitId }
        val dueToday = habits.filter { HabitSchedule.isScheduledOn(it, today) }

        val goals = container.goalRepository.observeGoals().first()
        val calorieLogs = container.calorieRepository.observeLogsBetween(today, today).first()
        val calorieGoal = container.calorieRepository.observeGoal().first()
        val waterLogs = container.waterRepository.observeLogsBetween(today, today).first()
        val waterGoal = container.waterRepository.observeGoal().first()
        val diaryEntry = container.diaryRepository.observeEntryForDate(today).first()

        val snapshot = DigestSnapshot(
            now = now,
            habitsDue = dueToday.size,
            habitsDone = dueToday.count { it.id in doneToday },
            goalsDueSoon = goals
                .filter { GoalProgress.isDeadlineNear(it, today) }
                .map { it.name },
            caloriesEaten = calorieLogs.sumOf { it.calories },
            calorieTarget = calorieGoal?.dailyTarget ?: CalorieGoal.DEFAULT_DAILY_TARGET,
            waterMl = waterLogs.sumOf { it.mlAmount },
            waterTargetMl = waterGoal?.dailyTargetMl ?: WaterGoal.DEFAULT_DAILY_TARGET_ML,
            diaryWritten = diaryEntry != null,
        )

        val lines = DailyDigest.build(snapshot, reminders, ignoreTiming)
            .map { it.toLine(context) }
        // An empty digest posts nothing at all — silence is the correct output when
        // the user is on top of everything, including in test mode.
        Notifier.postDigest(context, lines)
    }
}
