package com.lifetrack.notification.domain

import com.lifetrack.notification.data.FeatureType
import java.time.LocalTime

/** One line in the digest. Numbers only — the worker formats them with string resources. */
sealed interface DigestItem {
    val feature: FeatureType

    data class Habits(val done: Int, val due: Int) : DigestItem {
        override val feature = FeatureType.HABIT
    }

    data class Goals(val names: List<String>) : DigestItem {
        override val feature = FeatureType.GOAL
    }

    data class CaloriesUnder(val eaten: Int, val target: Int) : DigestItem {
        override val feature = FeatureType.CALORIE
    }

    data class CaloriesOver(val eaten: Int, val target: Int) : DigestItem {
        override val feature = FeatureType.CALORIE
    }

    data class Water(val drunkMl: Int, val targetMl: Int) : DigestItem {
        override val feature = FeatureType.WATER
    }

    data object Diary : DigestItem {
        override val feature = FeatureType.DIARY
    }
}

/** Everything the digest needs to know about the day so far. */
data class DigestSnapshot(
    val now: LocalTime,
    val habitsDue: Int = 0,
    val habitsDone: Int = 0,
    val goalsDueSoon: List<String> = emptyList(),
    val caloriesEaten: Int = 0,
    val calorieTarget: Int = 0,
    val waterMl: Int = 0,
    val waterTargetMl: Int = 0,
    val diaryWritten: Boolean = false,
)

/**
 * Builds the single consolidated digest described in PRD 7.8.
 *
 * Pure so the *content* of a notification is unit-testable, which matters because a
 * job that fires once a day is otherwise almost impossible to verify.
 *
 * A feature contributes a line only when all three hold:
 *  1. it is enabled in `notification_settings`,
 *  2. at least one of its configured reminder times has already passed, and
 *  3. the thing it is reminding about is actually unmet.
 *
 * Nothing unmet means no notification at all — see [build] returning an empty list.
 */
object DailyDigest {

    /**
     * Being *under* target is only worth mentioning if meaningfully under. Without a
     * threshold, any day not landing exactly on target would trigger a reminder.
     */
    const val UNDER_TARGET_FRACTION = 0.8

    /**
     * The waking window used to judge water pace. Water is expected to be spread
     * across the day, so "behind" is measured against elapsed waking hours rather
     * than against the whole 24.
     */
    val DAY_START: LocalTime = LocalTime.of(8, 0)
    val DAY_END: LocalTime = LocalTime.of(22, 0)

    /** Fraction of the daily water target that should be drunk by [now], in 0f..1f. */
    fun expectedWaterFraction(now: LocalTime): Float {
        if (!now.isAfter(DAY_START)) return 0f
        if (!now.isBefore(DAY_END)) return 1f
        val total = (DAY_END.toSecondOfDay() - DAY_START.toSecondOfDay()).toFloat()
        val elapsed = (now.toSecondOfDay() - DAY_START.toSecondOfDay()).toFloat()
        return (elapsed / total).coerceIn(0f, 1f)
    }

    fun isBehindOnWater(snapshot: DigestSnapshot): Boolean {
        if (snapshot.waterTargetMl <= 0) return false
        val expected = expectedWaterFraction(snapshot.now) * snapshot.waterTargetMl
        return snapshot.waterMl < expected
    }

    /**
     * @param reminders enabled reminder times per feature. A feature absent from the
     *   map is disabled and contributes nothing, which is how PRD 7.8's
     *   "users can disable any category independently" is honoured.
     */
    fun build(
        snapshot: DigestSnapshot,
        reminders: Map<FeatureType, List<LocalTime>>,
    ): List<DigestItem> = buildList {
        if (reminders.isDue(FeatureType.HABIT, snapshot.now) &&
            snapshot.habitsDue > 0 &&
            snapshot.habitsDone < snapshot.habitsDue
        ) {
            add(DigestItem.Habits(snapshot.habitsDone, snapshot.habitsDue))
        }

        if (reminders.isDue(FeatureType.GOAL, snapshot.now) && snapshot.goalsDueSoon.isNotEmpty()) {
            add(DigestItem.Goals(snapshot.goalsDueSoon))
        }

        if (reminders.isDue(FeatureType.CALORIE, snapshot.now) && snapshot.calorieTarget > 0) {
            when {
                snapshot.caloriesEaten > snapshot.calorieTarget ->
                    add(DigestItem.CaloriesOver(snapshot.caloriesEaten, snapshot.calorieTarget))

                snapshot.caloriesEaten < snapshot.calorieTarget * UNDER_TARGET_FRACTION ->
                    add(DigestItem.CaloriesUnder(snapshot.caloriesEaten, snapshot.calorieTarget))
            }
        }

        if (reminders.isDue(FeatureType.WATER, snapshot.now) && isBehindOnWater(snapshot)) {
            add(DigestItem.Water(snapshot.waterMl, snapshot.waterTargetMl))
        }

        if (reminders.isDue(FeatureType.DIARY, snapshot.now) && !snapshot.diaryWritten) {
            add(DigestItem.Diary)
        }
    }

    /** True once any of the feature's reminder times has passed today. */
    private fun Map<FeatureType, List<LocalTime>>.isDue(
        feature: FeatureType,
        now: LocalTime,
    ): Boolean = this[feature]?.any { !now.isBefore(it) } == true

    /**
     * The next moment the worker should wake up: the earliest enabled reminder time
     * still ahead of [now] today, or null when the day's checks are all done and the
     * caller should schedule for tomorrow's first time instead.
     */
    fun nextCheckAfter(
        now: LocalTime,
        reminders: Map<FeatureType, List<LocalTime>>,
    ): LocalTime? = reminders.values.flatten().filter { it.isAfter(now) }.minOrNull()

    /** The earliest enabled reminder time of any day, used to schedule tomorrow. */
    fun firstCheckOfDay(reminders: Map<FeatureType, List<LocalTime>>): LocalTime? =
        reminders.values.flatten().minOrNull()
}
