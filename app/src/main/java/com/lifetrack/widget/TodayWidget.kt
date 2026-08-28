package com.lifetrack.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lifetrack.LifeTrackApplication
import com.lifetrack.MainActivity
import com.lifetrack.habit.data.HabitSchedule
import com.lifetrack.water.data.WaterGoal
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * "Today" home-screen widget — a small subset of the dashboard: habit and water
 * progress, plus a one-tap +250ml action, so logging water never requires opening
 * the app at all. Session 12, added by request.
 *
 * Built with Glance rather than classic RemoteViews, matching the rest of the app's
 * declarative-UI style, even though Glance's composables are a distinct, more
 * limited set from full Jetpack Compose — see MEMORY.md for the exact API this was
 * verified against.
 *
 * A single fixed layout ([SizeMode.Single]) rather than a responsive one — this is a
 * small, content-light widget where resizing doesn't need a different layout.
 */
class TodayWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as LifeTrackApplication).container
        val today = LocalDate.now()

        // A widget renders once per update rather than staying subscribed to a Flow,
        // so a one-shot read is enough — freshness comes from calling updateAll()
        // after anything that changes these numbers (see AddWaterAction, and the
        // dashboard's habit-toggle / water quick-add call sites).
        val habits = container.habitRepository.observeHabits().first()
        val completions = container.habitRepository.observeRecentCompletions(today).first()
        val doneToday = completions.filter { it.date == today }.mapTo(mutableSetOf()) { it.habitId }
        val dueToday = habits.filter { HabitSchedule.isScheduledOn(it, today) }

        val waterMl = container.waterRepository.observeLogsBetween(today, today).first().sumOf { it.mlAmount }
        val waterTargetMl = container.waterRepository.observeGoal().first()?.dailyTargetMl
            ?: WaterGoal.DEFAULT_DAILY_TARGET_ML

        provideContent {
            WidgetContent(
                habitsDone = dueToday.count { it.id in doneToday },
                habitsDue = dueToday.size,
                waterMl = waterMl,
                waterTargetMl = waterTargetMl,
            )
        }
    }

    companion object {
        suspend fun refresh(context: Context) = TodayWidget().updateAll(context)
    }
}

@Composable
private fun WidgetContent(
    habitsDone: Int,
    habitsDue: Int,
    waterMl: Int,
    waterTargetMl: Int,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .padding(12)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            text = "LifeTrack",
            style = TextStyle(color = ColorProvider(WidgetColors.onBackground), fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = GlanceModifier.height(8))
        Text(
            text = "Habits: $habitsDone/$habitsDue",
            style = TextStyle(color = ColorProvider(WidgetColors.onBackground)),
        )
        Text(
            text = "Water: $waterMl/$waterTargetMl ml",
            style = TextStyle(color = ColorProvider(WidgetColors.onBackground)),
        )
        Spacer(modifier = GlanceModifier.height(8))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            androidx.glance.Button(
                text = "+250ml",
                onClick = actionRunCallback<AddWaterAction>(),
            )
        }
    }
}

/** Runs when the widget's +250ml button is tapped — writes directly, no app launch. */
class AddWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val container = (context.applicationContext as LifeTrackApplication).container
        container.waterRepository.add(QUICK_ADD_ML)
        TodayWidget.refresh(context)
    }

    private companion object {
        const val QUICK_ADD_ML = 250
    }
}

/** Widget-local color values — Glance doesn't have access to the app's MaterialTheme. */
private object WidgetColors {
    val background = Color(0xFF1B1B24)
    val onBackground = Color(0xFFFFFFFF)
}
