package com.lifetrack.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lifetrack.MainActivity
import com.lifetrack.R
import com.lifetrack.notification.domain.DigestItem

/**
 * Posts the single consolidated digest from PRD 7.8.
 *
 * Two deliberate choices keep this from becoming spam:
 *  - **One fixed notification id.** Every check updates the same notification rather
 *    than stacking, so there is never more than one LifeTrack notification present.
 *  - **`setOnlyAlertOnce`.** Later checks update the text silently instead of buzzing
 *    again, which is how the PRD 7.8 schedule coexists with PRD 8's "~3 pushes/day".
 */
object Notifier {

    private const val CHANNEL_ID = "lifetrack_daily_digest"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            // Low: a gentle reminder should not interrupt with sound by default.
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    /** Posting an empty digest would be the notification spam this app exists to avoid. */
    fun postDigest(context: Context, lines: List<String>) {
        if (lines.isEmpty() || !canPost(context)) return
        ensureChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(lines.first())
            .setStyle(NotificationCompat.InboxStyle().also { style -> lines.forEach(style::addLine) })
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    // --- Interval water reminders (session 12) ---
    //
    // Deliberately a SEPARATE channel and id from the digest above, and audible by
    // default (IMPORTANCE_DEFAULT, not _LOW): this is a per-interval nudge the user
    // opted into explicitly, not the consolidated daily digest, and it is meant to
    // actually get attention the way the user asked ("alarm wud ring"). See
    // MEMORY.md for why this is exempted from the "one notification" rule.

    private const val WATER_CHANNEL_ID = "lifetrack_water_reminder"
    private const val WATER_NOTIFICATION_ID = 1002

    fun ensureWaterReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            WATER_CHANNEL_ID,
            context.getString(R.string.water_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.water_reminder_channel_desc)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /**
     * @param smallMl / [largeMl] populate two notification actions that log water
     *   directly via [com.lifetrack.notification.WaterReminderActionReceiver] — no
     *   need to open the app for the common case.
     */
    fun postWaterReminder(context: Context, drunkMl: Int, targetMl: Int, smallMl: Int, largeMl: Int) {
        if (!canPost(context)) return
        ensureWaterReminderChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, WATER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.water_reminder_title))
            .setContentText(context.getString(R.string.water_progress, drunkMl, targetMl))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .addAction(0, context.getString(R.string.water_add_amount, smallMl), waterActionIntent(context, smallMl))
            .addAction(0, context.getString(R.string.water_add_amount, largeMl), waterActionIntent(context, largeMl))
            .build()

        NotificationManagerCompat.from(context).notify(WATER_NOTIFICATION_ID, notification)
    }

    fun cancelWaterReminder(context: Context) {
        NotificationManagerCompat.from(context).cancel(WATER_NOTIFICATION_ID)
    }

    private fun waterActionIntent(context: Context, mlAmount: Int): PendingIntent {
        val intent = Intent(context, WaterReminderActionReceiver::class.java)
            .putExtra(WaterReminderActionReceiver.EXTRA_ML_AMOUNT, mlAmount)
        return PendingIntent.getBroadcast(
            context,
            mlAmount, // distinct request code per amount, so the two actions don't collide
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

/** Renders digest items into notification lines. Kept next to the poster that uses them. */
fun DigestItem.toLine(context: Context): String = when (this) {
    is DigestItem.Habits -> context.getString(R.string.notif_habits, done, due)
    is DigestItem.Goals -> if (names.size == 1) {
        context.getString(R.string.notif_goals_one, names.first())
    } else {
        context.getString(R.string.notif_goals_many, names.size)
    }
    is DigestItem.CaloriesUnder -> context.getString(R.string.notif_calories_under, eaten, target)
    is DigestItem.CaloriesOver -> context.getString(R.string.notif_calories_over, eaten, target)
    is DigestItem.Water -> context.getString(R.string.notif_water, drunkMl, targetMl)
    DigestItem.Diary -> context.getString(R.string.notif_diary)
}
