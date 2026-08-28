package com.lifetrack.notification.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifetrack.LifeTrackApplication
import com.lifetrack.calorie.data.CalorieGoal
import com.lifetrack.goal.data.GoalProgress
import com.lifetrack.habit.data.HabitSchedule
import com.lifetrack.notification.Notifier
import com.lifetrack.notification.toLine
import com.lifetrack.notification.domain.DailyDigest
import com.lifetrack.notification.domain.DigestSnapshot
import com.lifetrack.water.data.WaterGoal
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * The single daily consolidation job from PRD 7.8.
 *
 * Runs at each enabled reminder time, gathers the state of every tracker, and posts
 * **one** notification covering whatever is unmet. It never posts per-tracker
 * notifications — see MEMORY.md, this is an explicit product requirement.
 *
 * The worker reschedules itself at the end of every run, so a change to reminder
 * times takes effect from the next run without any extra plumbing.
 */
class DailyDigestWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as LifeTrackApplication).container
        val today = LocalDate.now()
        val now = LocalTime.now()

        return try {
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

            val lines = DailyDigest.build(snapshot, reminders).map { it.toLine(applicationContext) }
            // An empty digest posts nothing at all — silence is the correct output
            // when the user is on top of everything.
            Notifier.postDigest(applicationContext, lines)

            Result.success()
        } catch (error: Exception) {
            // Deliberately not Result.retry(): the finally block below re-enqueues the
            // unique work with REPLACE, which would cancel the retry anyway. Missing
            // one check is harmless — the next scheduled one re-reads the same state.
            Result.success()
        } finally {
            // Always line up the next check, including after a failure, so one bad
            // run cannot silently end all future reminders.
            DigestScheduler.scheduleNext(applicationContext)
        }
    }

    companion object {
        const val WORK_NAME = "lifetrack_daily_digest"
    }
}
