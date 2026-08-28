package com.lifetrack.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifetrack.LifeTrackApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the "+250ml"/"+500ml" action buttons on the water reminder notification —
 * logs the drink without the user having to open the app at all.
 *
 * `goAsync()` is required because [onReceive] is not suspending but the DB write is;
 * it gives the system a short window (a few seconds) to let the coroutine finish
 * before the receiver is considered done, which a plain Room insert comfortably fits.
 */
class WaterReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mlAmount = intent.getIntExtra(EXTRA_ML_AMOUNT, 0)
        if (mlAmount <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as LifeTrackApplication).container
                container.waterRepository.add(mlAmount)
                Notifier.cancelWaterReminder(context)
                com.lifetrack.widget.TodayWidget.refresh(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ML_AMOUNT = "ml_amount"
    }
}
