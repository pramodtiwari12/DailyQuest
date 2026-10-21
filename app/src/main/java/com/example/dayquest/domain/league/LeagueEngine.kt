package com.example.dayquest.domain.league

import java.util.Calendar
import java.util.TimeZone

enum class LeagueTier(val displayName: String, val index: Int) {
    BRONZE("Bronze", 0),
    SILVER("Silver", 1),
    GOLD("Gold", 2),
    PLATINUM("Platinum", 3),
    DIAMOND("Diamond", 4),
    MASTER("Master", 5),
    LEGEND("Legend", 6);

    companion object {
        fun fromString(name: String): LeagueTier {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) } ?: BRONZE
        }

        fun nextTier(current: LeagueTier): LeagueTier {
            return entries.firstOrNull { it.index == current.index + 1 } ?: LEGEND
        }

        fun previousTier(current: LeagueTier): LeagueTier {
            return entries.firstOrNull { it.index == current.index - 1 } ?: BRONZE
        }
    }
}

enum class CohortZone {
    PROMOTION, // Top rankers in cohort
    SAFE,      // Mid ranks
    DEMOTION   // Bottom ranks (if cohort is large enough)
}

data class LeaderboardEntry(
    val id: String,
    val rank: Int,
    val displayName: String,
    val weeklyXp: Int,
    val isCurrentUser: Boolean,
    val zone: CohortZone,
    val avatarId: String = "vanguard",
    val occupation: String = "General"
)

object LeagueEngine {

    /**
     * Compute leaderboard for genuine registered members only.
     * ZERO bot users, ZERO simulated users, ZERO fabricated rankings.
     */
    fun buildRealLeaderboard(
        tier: LeagueTier,
        currentUserEntry: LeaderboardEntry?,
        otherRealUsers: List<LeaderboardEntry>
    ): List<LeaderboardEntry> {
        val allEntries = mutableListOf<LeaderboardEntry>()
        if (currentUserEntry != null) {
            allEntries.add(currentUserEntry)
        }
        for (other in otherRealUsers) {
            if (currentUserEntry == null || other.id != currentUserEntry.id) {
                allEntries.add(other.copy(isCurrentUser = false))
            }
        }

        if (allEntries.isEmpty()) {
            return emptyList()
        }

        // Sort by weeklyXp DESC, then currentUser wins tie-break, then displayName
        val sorted = allEntries.sortedWith(
            compareByDescending<LeaderboardEntry> { it.weeklyXp }
                .thenByDescending { it.isCurrentUser }
                .thenBy { it.displayName }
        )

        val total = sorted.size
        return sorted.mapIndexed { idx, item ->
            val rank = idx + 1
            val zone = when {
                total >= 5 && rank <= 3 && tier != LeagueTier.LEGEND -> CohortZone.PROMOTION
                total >= 10 && rank > total - 3 && tier != LeagueTier.BRONZE -> CohortZone.DEMOTION
                else -> CohortZone.SAFE
            }
            item.copy(
                rank = rank,
                zone = zone
            )
        }
    }

    /**
     * Compute human-readable reset countdown for canonical Monday 00:00 UTC/IST cycle
     */
    fun getCanonicalResetSummary(): String {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2 ... Sat=7
        val daysUntilMonday = when (dayOfWeek) {
            Calendar.SUNDAY -> 1
            else -> 9 - dayOfWeek
        }
        val hoursLeft = 23 - cal.get(Calendar.HOUR_OF_DAY)
        return "Resets in ${daysUntilMonday}d ${hoursLeft}h (Mon 00:00)"
    }
}
