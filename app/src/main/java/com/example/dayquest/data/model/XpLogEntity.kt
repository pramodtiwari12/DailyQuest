package com.example.dayquest.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "xp_logs",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["localDateKey", "reason"]),
        Index(value = ["taskId"])
    ]
)
data class XpLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val idempotencyKey: String,
    val taskId: String? = null,
    val categoryId: String? = null,
    val xpDelta: Int,
    val coinsDelta: Int = 0,
    val reason: String, // "task_completion", "subtask_completion", "daily_bonus", "undo_compensation", etc.
    val localDateKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val breakdownJson: String? = null,
    val syncStatus: String = "local_only"
)
