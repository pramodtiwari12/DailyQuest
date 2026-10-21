package com.example.dayquest.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["localDateKey", "status"]),
        Index(value = ["localDateKey", "categoryId"]),
        Index(value = ["dueAt", "status"]),
        Index(value = ["seriesId", "occurrenceId"]),
        Index(value = ["createdAt"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val seriesId: String? = null,
    val occurrenceId: String? = null,
    val title: String,
    val notes: String? = null,
    val categoryId: String = "Other",
    val categorySource: String = "user", // "auto" or "user"
    val mlConfidence: Float = 1.0f,
    val mlModelVersion: String = "baseline-1.0",
    val difficulty: String = "Medium", // "Easy", "Medium", "Hard", "Epic"
    val estMinutes: Int = 30,
    val actualMinutes: Int = 0,
    val scheduleType: String = "anytime", // "anytime", "byTime", "range"
    val dueAt: Long? = null,
    val startAt: Long? = null,
    val endAt: Long? = null,
    val localDateKey: String, // "YYYY-MM-DD"
    val timezoneId: String = "UTC",
    val repeatRule: String = "none", // "none", "daily", "weekdays", "weekly", "monthly"
    val priority: String = "medium", // "low", "medium", "high"
    val status: String = "pending", // "pending", "done", "missed"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val xpAwarded: Int = 0,
    val xpBreakdownJson: String? = null,
    val timerUsed: Boolean = false,
    val timerSeconds: Int = 0,
    val notificationVersion: Int = 1
)
