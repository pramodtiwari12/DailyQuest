package com.example.dayquest.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "category_progress")
data class CategoryProgressEntity(
    @PrimaryKey
    val categoryId: String,
    val attributedXp: Int = 0,
    val level: Int = 1,
    val tasksCompletedCount: Int = 0,
    val totalFocusMinutes: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "badges")
data class BadgeProgressEntity(
    @PrimaryKey
    val id: String,
    val progress: Int = 0,
    val unlockedAt: Long? = null,
    val rewardedAt: Long? = null
)

@Entity(
    tableName = "ml_corrections",
    indices = [Index(value = ["createdAt"])]
)
data class MlCorrectionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val normalizedText: String,
    val originalCategory: String,
    val correctedCategory: String,
    val correctedDifficulty: String = "Medium",
    val correctedDuration: Int = 30,
    val wasAutoCorrect: Boolean = false,
    val modelVersion: String = "multilingual-ngram-knn-1.0",
    val weight: Int = 3, // User corrections count 3x per master spec
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "quests",
    indices = [Index(value = ["localDateKey"])]
)
data class QuestEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val localDateKey: String,
    val type: String, // "category_count", "hard_task", "focus_minutes", "early_completion"
    val title: String,
    val description: String,
    val categoryId: String? = null,
    val target: Int = 1,
    val progress: Int = 0,
    val done: Boolean = false,
    val rewardedAt: Long? = null,
    val rewardXp: Int = 35
)

@Entity(tableName = "routines")
data class RoutineTemplateEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val tasksPipeDelimited: String, // e.g. "Morning hydration|Health & Fitness|Easy|15;10m Meditation|Mindfulness/Self-care|Easy|15"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "focus_sessions",
    indices = [Index(value = ["localDateKey"]), Index(value = ["taskId"])]
)
data class FocusSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskId: String? = null,
    val taskTitle: String = "Focus Session",
    val categoryId: String = "Study",
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Int,
    val completed: Boolean = true,
    val localDateKey: String
)

@Entity(tableName = "friends_challenges")
data class FriendEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val inviteCode: String,
    val weeklyXp: Int = 120,
    val leagueTier: String = "Silver",
    val challengeActive: Boolean = false,
    val challengeTargetXp: Int = 300,
    val challengeRewardClaimed: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "store_purchases")
data class StorePurchaseEntity(
    @PrimaryKey
    val itemId: String,
    val itemType: String, // "theme_accent", "avatar_frame", "streak_freeze"
    val costCoins: Int,
    val purchasedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)
