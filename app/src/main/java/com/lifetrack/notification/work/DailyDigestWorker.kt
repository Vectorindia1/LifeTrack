package com.lifetrack.notification.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * The single daily consolidation job from PRD 7.8.
 *
 * Runs at each enabled reminder time and delegates the actual work to [DigestRunner],
 * which it shares with Settings' manual test action. It never posts per-tracker
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
        return try {
            DigestRunner.run(applicationContext, ignoreTiming = false)
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
