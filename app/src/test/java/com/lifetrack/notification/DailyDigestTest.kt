package com.lifetrack.notification

import com.lifetrack.notification.data.FeatureType
import com.lifetrack.notification.domain.DailyDigest
import com.lifetrack.notification.domain.DigestItem
import com.lifetrack.notification.domain.DigestSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/** The PRD 7.8 defaults, as seeded by LifeTrackDatabase. */
private val DEFAULTS = mapOf(
    FeatureType.HABIT to listOf(LocalTime.of(20, 0)),
    FeatureType.GOAL to listOf(LocalTime.of(9, 0)),
    FeatureType.CALORIE to listOf(LocalTime.of(20, 30)),
    FeatureType.WATER to listOf(LocalTime.of(14, 0), LocalTime.of(18, 0)),
    FeatureType.DIARY to listOf(LocalTime.of(21, 30)),
)

class DailyDigestTest {

    @Test
    fun `nothing unmet produces no notification at all`() {
        val snapshot = DigestSnapshot(
            now = LocalTime.of(23, 0),
            habitsDue = 2,
            habitsDone = 2,
            calorieTarget = 2000,
            caloriesEaten = 2000,
            waterTargetMl = 2500,
            waterMl = 2500,
            diaryWritten = true,
        )
        assertTrue(DailyDigest.build(snapshot, DEFAULTS).isEmpty())
    }

    @Test
    fun `a feature does not fire before its reminder time`() {
        // 10:00 is past the goal check but before every other one.
        val snapshot = DigestSnapshot(
            now = LocalTime.of(10, 0),
            habitsDue = 3,
            habitsDone = 0,
            diaryWritten = false,
        )
        assertTrue(DailyDigest.build(snapshot, DEFAULTS).isEmpty())
    }

    @Test
    fun `late evening consolidates everything unmet into one list`() {
        val snapshot = DigestSnapshot(
            now = LocalTime.of(22, 0),
            habitsDue = 4,
            habitsDone = 1,
            goalsDueSoon = listOf("Read 12 books"),
            calorieTarget = 2000,
            caloriesEaten = 500,
            waterTargetMl = 2500,
            waterMl = 0,
            diaryWritten = false,
        )
        val items = DailyDigest.build(snapshot, DEFAULTS)
        // One digest carrying five concerns, not five notifications.
        assertEquals(5, items.size)
        assertEquals(
            listOf(
                FeatureType.HABIT,
                FeatureType.GOAL,
                FeatureType.CALORIE,
                FeatureType.WATER,
                FeatureType.DIARY,
            ),
            items.map { it.feature },
        )
    }

    @Test
    fun `a disabled feature contributes nothing`() {
        val snapshot = DigestSnapshot(now = LocalTime.of(22, 0), diaryWritten = false)
        val withoutDiary = DEFAULTS - FeatureType.DIARY
        assertTrue(DailyDigest.build(snapshot, withoutDiary).none { it is DigestItem.Diary })
        assertTrue(DailyDigest.build(snapshot, DEFAULTS).any { it is DigestItem.Diary })
    }

    @Test
    fun `calories only nag when meaningfully under, not merely short`() {
        fun eaten(kcal: Int) = DigestSnapshot(
            now = LocalTime.of(21, 0),
            calorieTarget = 2000,
            caloriesEaten = kcal,
        )
        // 1900 of 2000 is fine; 1000 is not.
        assertTrue(DailyDigest.build(eaten(1900), DEFAULTS).none { it is DigestItem.CaloriesUnder })
        assertTrue(DailyDigest.build(eaten(1000), DEFAULTS).any { it is DigestItem.CaloriesUnder })
    }

    @Test
    fun `going over calories is always reported`() {
        val snapshot = DigestSnapshot(
            now = LocalTime.of(21, 0),
            calorieTarget = 2000,
            caloriesEaten = 2100,
        )
        assertTrue(DailyDigest.build(snapshot, DEFAULTS).any { it is DigestItem.CaloriesOver })
    }

    @Test
    fun `no calorie target means no calorie nagging`() {
        val snapshot = DigestSnapshot(now = LocalTime.of(21, 0), calorieTarget = 0, caloriesEaten = 0)
        assertTrue(DailyDigest.build(snapshot, DEFAULTS).none { it.feature == FeatureType.CALORIE })
    }

    @Test
    fun `water pace rises across the waking day`() {
        assertEquals(0f, DailyDigest.expectedWaterFraction(LocalTime.of(7, 0)), 0.001f)
        assertEquals(0f, DailyDigest.expectedWaterFraction(LocalTime.of(8, 0)), 0.001f)
        assertEquals(0.5f, DailyDigest.expectedWaterFraction(LocalTime.of(15, 0)), 0.001f)
        assertEquals(1f, DailyDigest.expectedWaterFraction(LocalTime.of(22, 0)), 0.001f)
        assertEquals(1f, DailyDigest.expectedWaterFraction(LocalTime.of(23, 30)), 0.001f)
    }

    @Test
    fun `water only fires when actually behind pace`() {
        // At 14:00, ~43% of the day has passed, so ~1071ml of 2500 is expected.
        val behind = DigestSnapshot(now = LocalTime.of(14, 0), waterTargetMl = 2500, waterMl = 200)
        val ahead = DigestSnapshot(now = LocalTime.of(14, 0), waterTargetMl = 2500, waterMl = 2000)
        assertTrue(DailyDigest.isBehindOnWater(behind))
        assertFalse(DailyDigest.isBehindOnWater(ahead))
        assertTrue(DailyDigest.build(behind, DEFAULTS).any { it is DigestItem.Water })
        assertTrue(DailyDigest.build(ahead, DEFAULTS).none { it is DigestItem.Water })
    }

    @Test
    fun `no water target means no water nagging`() {
        val snapshot = DigestSnapshot(now = LocalTime.of(18, 0), waterTargetMl = 0, waterMl = 0)
        assertFalse(DailyDigest.isBehindOnWater(snapshot))
    }

    @Test
    fun `habits do not fire when none are due today`() {
        val snapshot = DigestSnapshot(now = LocalTime.of(22, 0), habitsDue = 0, habitsDone = 0)
        assertTrue(DailyDigest.build(snapshot, DEFAULTS).none { it.feature == FeatureType.HABIT })
    }

    @Test
    fun `next check is the earliest time still ahead`() {
        assertEquals(LocalTime.of(9, 0), DailyDigest.nextCheckAfter(LocalTime.of(7, 0), DEFAULTS))
        assertEquals(LocalTime.of(14, 0), DailyDigest.nextCheckAfter(LocalTime.of(9, 30), DEFAULTS))
        assertEquals(LocalTime.of(21, 30), DailyDigest.nextCheckAfter(LocalTime.of(21, 0), DEFAULTS))
    }

    @Test
    fun `after the last check there is nothing left today`() {
        assertNull(DailyDigest.nextCheckAfter(LocalTime.of(23, 0), DEFAULTS))
        // The caller then schedules tomorrow's first check.
        assertEquals(LocalTime.of(9, 0), DailyDigest.firstCheckOfDay(DEFAULTS))
    }

    @Test
    fun `all reminders disabled means nothing to schedule`() {
        assertNull(DailyDigest.nextCheckAfter(LocalTime.of(0, 0), emptyMap()))
        assertNull(DailyDigest.firstCheckOfDay(emptyMap()))
    }
}
