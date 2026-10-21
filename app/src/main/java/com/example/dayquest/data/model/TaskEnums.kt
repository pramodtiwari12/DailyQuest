package com.example.dayquest.data.model

enum class Difficulty(val label: String, val multiplier: Double) {
    EASY("Easy", 1.0),
    MEDIUM("Medium", 1.5),
    HARD("Hard", 2.0),
    EPIC("Epic", 3.0);

    companion object {
        fun fromString(value: String): Difficulty {
            return entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class ScheduleType(val label: String) {
    ANYTIME("Anytime Today"),
    BY_TIME("By Time"),
    RANGE("At Time Range");

    companion object {
        fun fromString(value: String): ScheduleType {
            return when (value.lowercase()) {
                "bytime", "by_time" -> BY_TIME
                "range", "at_range", "at_time_range" -> RANGE
                else -> ANYTIME
            }
        }
    }
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    companion object {
        fun fromString(value: String): TaskPriority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class RepeatRule(val label: String) {
    NONE("Does not repeat"),
    DAILY("Daily"),
    WEEKDAYS("Weekdays (Mon-Fri)"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    companion object {
        fun fromString(value: String): RepeatRule {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

enum class TaskStatus {
    PENDING,
    DONE,
    MISSED;

    companion object {
        fun fromString(value: String): TaskStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}
