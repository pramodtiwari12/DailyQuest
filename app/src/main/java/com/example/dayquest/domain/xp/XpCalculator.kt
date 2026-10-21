package com.example.dayquest.domain.xp

import com.example.dayquest.data.model.Difficulty
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class XpBreakdown(
    val base: Double,
    val durationMinutes: Int,
    val difficultyMultiplier: Double,
    val timingMultiplier: Double,
    val streakMultiplier: Double,
    val focusBonusMultiplier: Double,
    val isQuickCompletionAntiCheat: Boolean,
    val finalXp: Int,
    val coinsEarned: Int
)

data class LevelProgress(
    val level: Int,
    val title: String,
    val currentXp: Int,
    val levelStartXp: Int,
    val nextLevelStartXp: Int,
    val progressFraction: Float
)

object XpCalculator {

    /**
     * Standard round-half-up implementation
     */
    fun roundHalfUp(value: Double): Int {
        return floor(value + 0.5).toInt()
    }

    /**
     * Calculate XP awarded for task completion according to exact DayQuest specifications
     */
    fun calculateTaskXp(
        durationMinutes: Int,
        difficulty: Difficulty,
        dueAt: Long?,
        completedAt: Long,
        createdAt: Long,
        currentStreak: Int,
        timerUsed: Boolean = false
    ): XpBreakdown {
        val duration = durationMinutes.coerceAtLeast(1)
        val base = 10.0 + (duration * 0.5)
        val diffMult = difficulty.multiplier

        val timingMult = calculateTimingMultiplier(dueAt = dueAt, completedAt = completedAt)
        val cappedStreak = min(currentStreak, 30)
        val streakMult = 1.0 + (cappedStreak * 0.01)
        val focusBonus = if (timerUsed) 0.10 else 0.0

        val normalXpUnrounded = base * diffMult * timingMult * streakMult * (1.0 + focusBonus)
        val normalXp = roundHalfUp(normalXpUnrounded)

        // Anti-cheat: completion within 5 seconds of creation earns 25%
        val isQuick = (completedAt - createdAt) < 5000L
        val awardedXp = if (isQuick) {
            val reduced = roundHalfUp(normalXp * 0.25)
            maxOf(5, reduced)
        } else {
            maxOf(5, normalXp)
        }

        val coins = floor(awardedXp.toDouble() / 10.0).toInt()

        return XpBreakdown(
            base = base,
            durationMinutes = duration,
            difficultyMultiplier = diffMult,
            timingMultiplier = timingMult,
            streakMultiplier = streakMult,
            focusBonusMultiplier = 1.0 + focusBonus,
            isQuickCompletionAntiCheat = isQuick,
            finalXp = awardedXp,
            coinsEarned = coins
        )
    }

    fun calculateTimingMultiplier(dueAt: Long?, completedAt: Long): Double {
        if (dueAt == null) return 1.0
        val diffMillis = dueAt - completedAt
        return when {
            diffMillis > 5 * 60 * 1000L -> 1.2 // More than 5 min early
            diffMillis >= 0L -> 1.0             // Within 5 min early to exact deadline
            diffMillis >= -60 * 60 * 1000L -> 0.7 // Late by 1s to 60 min
            else -> 0.5                        // More than 60 min late
        }
    }

    /**
     * Cumulative XP threshold to enter level L (L >= 2) is roundHalfUp(100 * (L - 1)^1.5)
     */
    fun thresholdForLevel(level: Int): Int {
        if (level <= 1) return 0
        val l = (level - 1).toDouble()
        return roundHalfUp(100.0 * l.pow(1.5))
    }

    /**
     * Determine level and progress within that level given cumulative XP
     */
    fun getLevelProgress(totalXp: Int): LevelProgress {
        val safeXp = maxOf(0, totalXp)
        var level = 1
        while (thresholdForLevel(level + 1) <= safeXp) {
            level++
        }

        val levelStartXp = thresholdForLevel(level)
        val nextLevelStartXp = thresholdForLevel(level + 1)
        val xpInLevel = safeXp - levelStartXp
        val xpNeededForLevel = nextLevelStartXp - levelStartXp

        val fraction = if (xpNeededForLevel > 0) {
            (xpInLevel.toFloat() / xpNeededForLevel.toFloat()).coerceIn(0f, 1f)
        } else 1f

        val title = when {
            level in 1..4 -> "Beginner"
            level in 5..9 -> "Explorer"
            level in 10..19 -> "Achiever"
            level in 20..29 -> "Pro"
            level in 30..49 -> "Master"
            else -> "Legend"
        }

        return LevelProgress(
            level = level,
            title = title,
            currentXp = safeXp,
            levelStartXp = levelStartXp,
            nextLevelStartXp = nextLevelStartXp,
            progressFraction = fraction
        )
    }
}
