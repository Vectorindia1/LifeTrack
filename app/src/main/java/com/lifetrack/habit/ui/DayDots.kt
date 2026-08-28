package com.lifetrack.habit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifetrack.habit.data.HabitSchedule
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A Mon–Sun row of small dots marking this week's completions — the day-by-day
 * texture from the design reference, computed from data already on hand rather than
 * a new query: the caller already has this week's completed dates per habit.
 */
@Composable
fun WeekDots(
    weekStart: LocalDate,
    today: LocalDate,
    completedDates: Set<LocalDate>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.entries.forEach { day ->
            val date = weekStart.plusDays((HabitSchedule.dayBit(day).countTrailingZeroBits()).toLong())
            val isDone = date in completedDates
            val isFuture = date.isAfter(today)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = when {
                            isDone -> accent
                            isFuture -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}
