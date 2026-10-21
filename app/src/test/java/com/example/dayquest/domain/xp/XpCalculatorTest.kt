package com.example.dayquest.domain.xp

import com.example.dayquest.data.model.Difficulty
import org.junit.Assert.*
import org.junit.Test

class XpCalculatorTest {

    @Test
    fun testBaseXpCalculation() {
        // Base = 10 + (30 * 0.5) = 25
        val now = 1000000L
        val createdAt = now - 60000L // 60 seconds ago (no anti-cheat)
        val breakdown = XpCalculator.calculateTaskXp(
            durationMinutes = 30,
            difficulty = Difficulty.EASY,
            dueAt = null,
            completedAt = now,
            createdAt = createdAt,
            currentStreak = 0,
            timerUsed = false
        )
        assertEquals(25.0, breakdown.base, 0.001)
        assertEquals(1.0, breakdown.difficultyMultiplier, 0.001)
        assertEquals(1.0, breakdown.timingMultiplier, 0.001)
        assertEquals(1.0, breakdown.streakMultiplier, 0.001)
        assertEquals(25, breakdown.finalXp)
        assertEquals(2, breakdown.coinsEarned) // floor(25 / 10) = 2
    }

    @Test
    fun testDifficultyMultipliers() {
        val now = 1000000L
        val createdAt = now - 60000L
        // Duration 60 min -> Base = 10 + 30 = 40
        // Easy: 40 * 1.0 = 40
        val easy = XpCalculator.calculateTaskXp(60, Difficulty.EASY, null, now, createdAt, 0)
        assertEquals(40, easy.finalXp)

        // Medium: 40 * 1.5 = 60
        val medium = XpCalculator.calculateTaskXp(60, Difficulty.MEDIUM, null, now, createdAt, 0)
        assertEquals(60, medium.finalXp)

        // Hard: 40 * 2.0 = 80
        val hard = XpCalculator.calculateTaskXp(60, Difficulty.HARD, null, now, createdAt, 0)
        assertEquals(80, hard.finalXp)

        // Epic: 40 * 3.0 = 120
        val epic = XpCalculator.calculateTaskXp(60, Difficulty.EPIC, null, now, createdAt, 0)
        assertEquals(120, epic.finalXp)
    }

    @Test
    fun testTimingMultipliers() {
        val now = 1000000L
        val createdAt = now - 60000L
        val dueAtEarly = now + 10 * 60 * 1000L // 10 min in future (> 5 min early) -> 1.2
        val early = XpCalculator.calculateTaskXp(30, Difficulty.EASY, dueAtEarly, now, createdAt, 0)
        // 25 * 1.0 * 1.2 = 30
        assertEquals(30, early.finalXp)

        val dueAtOnTime = now + 2 * 60 * 1000L // 2 min in future (<= 5 min) -> 1.0
        val onTime = XpCalculator.calculateTaskXp(30, Difficulty.EASY, dueAtOnTime, now, createdAt, 0)
        assertEquals(25, onTime.finalXp)

        val dueAtLateShort = now - 10 * 60 * 1000L // 10 min late (<= 60 min) -> 0.7
        val lateShort = XpCalculator.calculateTaskXp(30, Difficulty.EASY, dueAtLateShort, now, createdAt, 0)
        // 25 * 0.7 = 17.5 -> round half up 18
        assertEquals(18, lateShort.finalXp)

        val dueAtLateLong = now - 70 * 60 * 1000L // 70 min late (> 60 min) -> 0.5
        val lateLong = XpCalculator.calculateTaskXp(30, Difficulty.EASY, dueAtLateLong, now, createdAt, 0)
        // 25 * 0.5 = 12.5 -> round half up 13
        assertEquals(13, lateLong.finalXp)
    }

    @Test
    fun testStreakMultiplierCapping() {
        val now = 1000000L
        val createdAt = now - 60000L
        // Streak 10 -> 1 + 0.10 = 1.10
        val s10 = XpCalculator.calculateTaskXp(60, Difficulty.EASY, null, now, createdAt, 10)
        // 40 * 1.10 = 44
        assertEquals(44, s10.finalXp)

        // Streak 40 (capped at 30) -> 1 + 0.30 = 1.30
        val s40 = XpCalculator.calculateTaskXp(60, Difficulty.EASY, null, now, createdAt, 40)
        // 40 * 1.30 = 52
        assertEquals(52, s40.finalXp)
    }

    @Test
    fun testAntiCheatQuickCompletion() {
        val now = 1000000L
        val createdAt = now - 2000L // Created 2 seconds ago (< 5s)
        // Normal 40 XP -> 25% factor = 10 XP
        val quick = XpCalculator.calculateTaskXp(60, Difficulty.EASY, null, now, createdAt, 0)
        assertTrue(quick.isQuickCompletionAntiCheat)
        assertEquals(10, quick.finalXp)

        // Minimum 5 XP floor
        val quickSmall = XpCalculator.calculateTaskXp(15, Difficulty.EASY, null, now, createdAt, 0)
        // Base = 17.5 -> 18. 18 * 0.25 = 4.5 -> 5.
        assertTrue(quickSmall.finalXp >= 5)
    }

    @Test
    fun testLevelProgressionThresholds() {
        assertEquals(0, XpCalculator.thresholdForLevel(1))
        // L2: 100 * (1)^1.5 = 100
        assertEquals(100, XpCalculator.thresholdForLevel(2))
        // L3: 100 * (2)^1.5 = 100 * 2.8284 = 283
        assertEquals(283, XpCalculator.thresholdForLevel(3))
        // L4: 100 * (3)^1.5 = 100 * 5.196 = 520
        assertEquals(520, XpCalculator.thresholdForLevel(4))
        // L5: 100 * (4)^1.5 = 100 * 8 = 800
        assertEquals(800, XpCalculator.thresholdForLevel(5))
    }

    @Test
    fun testLevelProgressAndTitles() {
        val p1 = XpCalculator.getLevelProgress(50)
        assertEquals(1, p1.level)
        assertEquals("Beginner", p1.title)
        assertEquals(0.5f, p1.progressFraction, 0.01f)

        val p5 = XpCalculator.getLevelProgress(850)
        assertEquals(5, p5.level)
        assertEquals("Explorer", p5.title)

        val p10 = XpCalculator.getLevelProgress(3000)
        assertEquals("Achiever", p10.title)
    }
}
