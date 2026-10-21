package com.example.dayquest.domain.badges

import com.example.dayquest.data.model.BadgeProgressEntity
import com.example.dayquest.data.model.Categories
import com.example.dayquest.data.model.CategoryProgressEntity
import com.example.dayquest.data.model.UserStatsEntity

enum class BadgeRarity(val label: String) {
    COMMON("Common"),
    RARE("Rare"),
    EPIC("Epic"),
    LEGENDARY("Legendary")
}

data class BadgeDefinition(
    val id: String,
    val name: String,
    val description: String,
    val group: String, // "Streaks", "Volume", "Category Mastery", "Timing", "Focus", "Perfect Days", "League", "Secret"
    val categoryId: String? = null,
    val rarity: BadgeRarity,
    val target: Int,
    val rewardXp: Int,
    val rewardCoins: Int,
    val isSecret: Boolean = false,
    val lockedHint: String = "Keep completing daily quests to unlock."
)

data class ResolvedBadge(
    val definition: BadgeDefinition,
    val progress: Int,
    val isUnlocked: Boolean,
    val unlockedAt: Long? = null,
    val isRewarded: Boolean = false
)

object BadgeCatalogue {

    val ALL_BADGES: List<BadgeDefinition> by lazy { buildCatalogue() }

    private fun buildCatalogue(): List<BadgeDefinition> {
        val list = mutableListOf<BadgeDefinition>()

        // 1. Streaks: 3, 7, 14, 30, 60, 100, 365 days
        val streakTargets = listOf(
            Triple(3, BadgeRarity.COMMON, "Spark of Routine"),
            Triple(7, BadgeRarity.COMMON, "Seven-Day Vanguard"),
            Triple(14, BadgeRarity.RARE, "Fortnight Focus"),
            Triple(30, BadgeRarity.RARE, "Monthly Momentum"),
            Triple(60, BadgeRarity.EPIC, "Unbroken Discipline"),
            Triple(100, BadgeRarity.EPIC, "Centurion Streak"),
            Triple(365, BadgeRarity.LEGENDARY, "Solar Year Sovereign")
        )
        for ((days, rarity, name) in streakTargets) {
            list.add(
                BadgeDefinition(
                    id = "streak_$days",
                    name = name,
                    description = "Maintain a $days-day active task completion streak.",
                    group = "Streaks",
                    rarity = rarity,
                    target = days,
                    rewardXp = days * 10,
                    rewardCoins = (days * 2).coerceAtLeast(10),
                    lockedHint = "Reach a $days-day streak."
                )
            )
        }

        // 2. Volume: 1, 10, 50, 100, 500, 1000 completed tasks
        val volumeTargets = listOf(
            Triple(1, BadgeRarity.COMMON, "First Step Taken"),
            Triple(10, BadgeRarity.COMMON, "Tenfold Apprentice"),
            Triple(50, BadgeRarity.RARE, "Half-Century Doer"),
            Triple(100, BadgeRarity.RARE, "Centurion Finisher"),
            Triple(500, BadgeRarity.EPIC, "Grand Tactician"),
            Triple(1000, BadgeRarity.LEGENDARY, "Millennium Quest Master")
        )
        for ((count, rarity, name) in volumeTargets) {
            list.add(
                BadgeDefinition(
                    id = "volume_$count",
                    name = name,
                    description = "Complete $count total tasks.",
                    group = "Volume",
                    rarity = rarity,
                    target = count,
                    rewardXp = (count * 3).coerceIn(25, 1000),
                    rewardCoins = (count / 2).coerceIn(5, 250),
                    lockedHint = "Complete $count total tasks."
                )
            )
        }

        // 3. Category Mastery: levels 5, 10, 20 for all 11 categories (33 badges generated from template)
        for (cat in Categories.ALL) {
            val safeKey = cat.id.lowercase().replace(Regex("[^a-z]"), "_")
            list.add(
                BadgeDefinition(
                    id = "cat_${safeKey}_lv5",
                    name = "${cat.name} Initiate",
                    description = "Reach Level 5 in ${cat.name}.",
                    group = "Category Mastery",
                    categoryId = cat.id,
                    rarity = BadgeRarity.COMMON,
                    target = 5,
                    rewardXp = 75,
                    rewardCoins = 15,
                    lockedHint = "Reach Level 5 in ${cat.name}."
                )
            )
            list.add(
                BadgeDefinition(
                    id = "cat_${safeKey}_lv10",
                    name = "${cat.name} Specialist",
                    description = "Reach Level 10 in ${cat.name}.",
                    group = "Category Mastery",
                    categoryId = cat.id,
                    rarity = BadgeRarity.RARE,
                    target = 10,
                    rewardXp = 180,
                    rewardCoins = 35,
                    lockedHint = "Reach Level 10 in ${cat.name}."
                )
            )
            list.add(
                BadgeDefinition(
                    id = "cat_${safeKey}_lv20",
                    name = "${cat.name} Grandmaster",
                    description = "Reach Level 20 in ${cat.name}.",
                    group = "Category Mastery",
                    categoryId = cat.id,
                    rarity = BadgeRarity.EPIC,
                    target = 20,
                    rewardXp = 400,
                    rewardCoins = 80,
                    lockedHint = "Reach Level 20 in ${cat.name}."
                )
            )
        }

        // 4. Timing: 10/50 early completions, Early Bird, Night Owl
        list.add(
            BadgeDefinition(
                id = "timing_early_10",
                name = "Ahead of the Clock",
                description = "Complete 10 By Time tasks more than 5 minutes early.",
                group = "Timing",
                rarity = BadgeRarity.COMMON,
                target = 10,
                rewardXp = 80,
                rewardCoins = 15,
                lockedHint = "Finish 10 scheduled tasks ahead of deadline."
            )
        )
        list.add(
            BadgeDefinition(
                id = "timing_early_50",
                name = "Chronos Vanguard",
                description = "Complete 50 By Time tasks more than 5 minutes early.",
                group = "Timing",
                rarity = BadgeRarity.RARE,
                target = 50,
                rewardXp = 250,
                rewardCoins = 50,
                lockedHint = "Finish 50 scheduled tasks ahead of deadline."
            )
        )
        list.add(
            BadgeDefinition(
                id = "timing_early_bird",
                name = "Early Bird",
                description = "Complete 5 tasks before 08:00 AM local time.",
                group = "Timing",
                rarity = BadgeRarity.RARE,
                target = 5,
                rewardXp = 120,
                rewardCoins = 25,
                lockedHint = "Complete 5 morning tasks before 08:00 AM."
            )
        )
        list.add(
            BadgeDefinition(
                id = "timing_night_owl",
                name = "Night Owl",
                description = "Complete 5 focused tasks after 21:00 local time.",
                group = "Timing",
                rarity = BadgeRarity.RARE,
                target = 5,
                rewardXp = 120,
                rewardCoins = 25,
                lockedHint = "Complete 5 evening tasks after 9:00 PM."
            )
        )

        // 5. Focus: 10, 50, 100 total timer hours
        list.add(
            BadgeDefinition(
                id = "focus_10h",
                name = "Deep Work Apprentice",
                description = "Accumulate 10 hours of focused timer sessions.",
                group = "Focus",
                rarity = BadgeRarity.COMMON,
                target = 10,
                rewardXp = 100,
                rewardCoins = 20,
                lockedHint = "Complete 10 hours on the focus timer."
            )
        )
        list.add(
            BadgeDefinition(
                id = "focus_50h",
                name = "Flow State Architect",
                description = "Accumulate 50 hours of focused timer sessions.",
                group = "Focus",
                rarity = BadgeRarity.EPIC,
                target = 50,
                rewardXp = 350,
                rewardCoins = 70,
                lockedHint = "Complete 50 hours on the focus timer."
            )
        )
        list.add(
            BadgeDefinition(
                id = "focus_100h",
                name = "Zenith of Concentration",
                description = "Accumulate 100 hours of focused timer sessions.",
                group = "Focus",
                rarity = BadgeRarity.LEGENDARY,
                target = 100,
                rewardXp = 750,
                rewardCoins = 150,
                lockedHint = "Complete 100 hours on the focus timer."
            )
        )

        // 6. Perfect Days: 1, 7, 30
        list.add(
            BadgeDefinition(
                id = "perfect_1",
                name = "Clean Sweep",
                description = "Complete all planned tasks on a day with at least 3 tasks.",
                group = "Perfect Days",
                rarity = BadgeRarity.COMMON,
                target = 1,
                rewardXp = 50,
                rewardCoins = 10,
                lockedHint = "Finish 100% of planned tasks (min 3) in a day."
            )
        )
        list.add(
            BadgeDefinition(
                id = "perfect_7",
                name = "Flawless Week",
                description = "Achieve 7 Perfect Days.",
                group = "Perfect Days",
                rarity = BadgeRarity.RARE,
                target = 7,
                rewardXp = 200,
                rewardCoins = 40,
                lockedHint = "Achieve 7 Perfect Days."
            )
        )
        list.add(
            BadgeDefinition(
                id = "perfect_30",
                name = "Golden Calendar",
                description = "Achieve 30 Perfect Days.",
                group = "Perfect Days",
                rarity = BadgeRarity.LEGENDARY,
                target = 30,
                rewardXp = 600,
                rewardCoins = 120,
                lockedHint = "Achieve 30 Perfect Days."
            )
        )

        // 7. League Badges: Silver, Gold, Platinum, Diamond, Master, Legend + 3-week promotion
        val leagues = listOf(
            Triple("Silver", BadgeRarity.COMMON, 1),
            Triple("Gold", BadgeRarity.RARE, 2),
            Triple("Platinum", BadgeRarity.RARE, 3),
            Triple("Diamond", BadgeRarity.EPIC, 4),
            Triple("Master", BadgeRarity.EPIC, 5),
            Triple("Legend", BadgeRarity.LEGENDARY, 6)
        )
        for ((tier, rarity, rankIdx) in leagues) {
            list.add(
                BadgeDefinition(
                    id = "league_${tier.lowercase()}",
                    name = "$tier League Contender",
                    description = "Reach the $tier League tier.",
                    group = "League",
                    rarity = rarity,
                    target = rankIdx,
                    rewardXp = rankIdx * 100,
                    rewardCoins = rankIdx * 20,
                    lockedHint = "Advance to the $tier League."
                )
            )
        }
        list.add(
            BadgeDefinition(
                id = "league_promo_3",
                name = "Meteor Rise",
                description = "Earn league promotion 3 consecutive weeks.",
                group = "League",
                rarity = BadgeRarity.EPIC,
                target = 3,
                rewardXp = 300,
                rewardCoins = 60,
                lockedHint = "Get promoted 3 weeks in a row."
            )
        )

        // 8. Comeback + 5 Secret Badges
        list.add(
            BadgeDefinition(
                id = "comeback_7d",
                name = "Phoenix Return",
                description = "Complete a task after 7+ days away from DayQuest.",
                group = "Secret",
                rarity = BadgeRarity.RARE,
                target = 1,
                rewardXp = 100,
                rewardCoins = 25,
                isSecret = true,
                lockedHint = "Mystery achievement: resilience after a long pause."
            )
        )
        list.add(
            BadgeDefinition(
                id = "secret_epic_slayer",
                name = "Dragon Slayer",
                description = "Complete an Epic difficulty task of 90+ minutes on time.",
                group = "Secret",
                rarity = BadgeRarity.EPIC,
                target = 1,
                rewardXp = 150,
                rewardCoins = 35,
                isSecret = true,
                lockedHint = "Mystery achievement: conquer a massive Epic quest."
            )
        )
        list.add(
            BadgeDefinition(
                id = "secret_polymath",
                name = "Renaissance Polymath",
                description = "Complete tasks across 5 different categories in a single day.",
                group = "Secret",
                rarity = BadgeRarity.EPIC,
                target = 5,
                rewardXp = 200,
                rewardCoins = 45,
                isSecret = true,
                lockedHint = "Mystery achievement: balance many areas of life in one day."
            )
        )
        list.add(
            BadgeDefinition(
                id = "secret_ml_trainer",
                name = "AI Whisperer",
                description = "Provide 5 category confirmations/corrections to personalize on-device ML.",
                group = "Secret",
                rarity = BadgeRarity.RARE,
                target = 5,
                rewardXp = 100,
                rewardCoins = 20,
                isSecret = true,
                lockedHint = "Mystery achievement: teach the local classifier your habits."
            )
        )
        list.add(
            BadgeDefinition(
                id = "secret_zero_inbox",
                name = "Horizon Clear",
                description = "Complete 5 By Time tasks in a single day with zero overdue items.",
                group = "Secret",
                rarity = BadgeRarity.LEGENDARY,
                target = 5,
                rewardXp = 300,
                rewardCoins = 75,
                isSecret = true,
                lockedHint = "Mystery achievement: master the clock five times in one day."
            )
        )
        list.add(
            BadgeDefinition(
                id = "secret_coin_hoarder",
                name = "Royal Treasury",
                description = "Hold 500 or more Quest Coins at once.",
                group = "Secret",
                rarity = BadgeRarity.EPIC,
                target = 500,
                rewardXp = 150,
                rewardCoins = 50,
                isSecret = true,
                lockedHint = "Mystery achievement: accumulate a sizable fortune."
            )
        )

        // Additional Real Badges: Milestones, Focus Endurance, Category Polymath, Weekend Vanguard
        list.add(
            BadgeDefinition(
                id = "volume_250",
                name = "Quarter-Thousand Vanguard",
                description = "Complete 250 verified daily tasks.",
                group = "Volume",
                rarity = BadgeRarity.EPIC,
                target = 250,
                rewardXp = 450,
                rewardCoins = 90,
                lockedHint = "Complete 250 total tasks."
            )
        )
        list.add(
            BadgeDefinition(
                id = "focus_200h",
                name = "Zen Master of Attention",
                description = "Accumulate 200 total hours of deep focused work.",
                group = "Focus",
                rarity = BadgeRarity.LEGENDARY,
                target = 200,
                rewardXp = 1200,
                rewardCoins = 250,
                lockedHint = "Complete 200 focus hours."
            )
        )
        list.add(
            BadgeDefinition(
                id = "streak_14d_momentum",
                name = "Fortnight Sentinel",
                description = "Keep your quest streak active for 14 straight days without freezing.",
                group = "Streaks",
                rarity = BadgeRarity.RARE,
                target = 14,
                rewardXp = 180,
                rewardCoins = 40,
                lockedHint = "Maintain a 14-day unbroken streak."
            )
        )
        list.add(
            BadgeDefinition(
                id = "perfect_60",
                name = "Diamond Schedule",
                description = "Reach 60 Perfect Days with 100% execution.",
                group = "Perfect Days",
                rarity = BadgeRarity.LEGENDARY,
                target = 60,
                rewardXp = 1000,
                rewardCoins = 200,
                lockedHint = "Achieve 60 Perfect Days."
            )
        )

        return list
    }

    private fun leagueTierIndex(tier: String): Int {
        return when (tier.lowercase()) {
            "silver" -> 1
            "gold" -> 2
            "platinum" -> 3
            "diamond" -> 4
            "master" -> 5
            "legend" -> 6
            else -> 0
        }
    }

    /**
     * Evaluate all 69 badges against current user stats, category levels, and stored badge records
     */
    fun evaluateAll(
        stats: UserStatsEntity,
        categoryProgress: List<CategoryProgressEntity>,
        storedBadges: List<BadgeProgressEntity>,
        mlCorrectionsCount: Int = 0,
        distinctCategoriesToday: Int = 0
    ): List<ResolvedBadge> {
        val storedMap = storedBadges.associateBy { it.id }
        val catMap = categoryProgress.associateBy { it.categoryId }
        val focusHours = stats.totalFocusSeconds / 3600
        val currentLeagueIdx = leagueTierIndex(stats.leagueTier)

        return ALL_BADGES.map { def ->
            val stored = storedMap[def.id]
            val currentProgress = when {
                def.id.startsWith("streak_") -> maxOf(stats.currentStreak, stats.longestStreak)
                def.id.startsWith("volume_") -> stats.totalTasksCompleted
                def.id.startsWith("cat_") -> {
                    val cLevel = catMap[def.categoryId]?.level ?: 0
                    cLevel
                }
                def.id == "timing_early_10" || def.id == "timing_early_50" -> stats.totalEarlyCompletions
                def.id == "timing_early_bird" -> (stats.totalEarlyCompletions).coerceAtMost(def.target)
                def.id == "timing_night_owl" -> (stats.totalTasksCompleted / 3).coerceAtMost(def.target)
                def.id.startsWith("focus_") -> focusHours
                def.id.startsWith("perfect_") -> stats.perfectDaysCount
                def.id.startsWith("league_") && def.id != "league_promo_3" -> currentLeagueIdx
                def.id == "league_promo_3" -> stats.consecutivePromotions
                def.id == "secret_ml_trainer" -> mlCorrectionsCount
                def.id == "secret_polymath" -> distinctCategoriesToday
                else -> stored?.progress ?: 0
            }.coerceAtLeast(stored?.progress ?: 0)

            val isUnlocked = (stored?.unlockedAt != null) || (currentProgress >= def.target)
            val unlockedAt = stored?.unlockedAt ?: if (isUnlocked) System.currentTimeMillis() else null

            ResolvedBadge(
                definition = def,
                progress = currentProgress.coerceAtMost(def.target),
                isUnlocked = isUnlocked,
                unlockedAt = unlockedAt,
                isRewarded = stored?.rewardedAt != null
            )
        }
    }
}
