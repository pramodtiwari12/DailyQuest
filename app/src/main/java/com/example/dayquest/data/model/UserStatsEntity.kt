package com.example.dayquest.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey
    val id: String = "primary_user",
    val displayName: String = "Adventurer",
    val avatarId: String = "vanguard",
    val avatarFrame: String = "Default",
    val bio: String = "",
    val occupation: String = "General",
    val googleEmail: String? = null,
    val googleUserId: String? = null,
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val coins: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val streakFreezes: Int = 0,
    val lastActiveDateKey: String? = null,
    val protectedDatesCsv: String = "",
    val tasksCompletedTodayCount: Int = 0,
    val totalTasksCompleted: Int = 0,
    val totalEarlyCompletions: Int = 0,
    val totalFocusSeconds: Int = 0,
    val perfectDaysCount: Int = 0,
    val leagueTier: String = "Bronze",
    val weeklyLeagueXp: Int = 0,
    val consecutivePromotions: Int = 0,
    val inviteCode: String = "DQ-1001",
    val optInLeaderboard: Boolean = true,
    val publicProfile: Boolean = false,
    val lastCloudSyncTime: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
