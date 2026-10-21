package com.example.dayquest.domain.time

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {
    val DATE_KEY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val DISPLAY_DATE_FORMAT = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val DISPLAY_TIME_12H = SimpleDateFormat("h:mm a", Locale.getDefault())
    val DISPLAY_TIME_24H = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun todayDateKey(): String {
        return DATE_KEY_FORMAT.format(Date())
    }

    fun dateKeyForOffset(daysOffset: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        return DATE_KEY_FORMAT.format(cal.time)
    }

    fun formatDisplayDate(dateKey: String): String {
        return try {
            val date = DATE_KEY_FORMAT.parse(dateKey) ?: return dateKey
            val cal = Calendar.getInstance()
            val todayKey = todayDateKey()
            val tomorrowKey = dateKeyForOffset(1)
            val yesterdayKey = dateKeyForOffset(-1)

            when (dateKey) {
                todayKey -> "Today, ${DISPLAY_DATE_FORMAT.format(date)}"
                tomorrowKey -> "Tomorrow, ${DISPLAY_DATE_FORMAT.format(date)}"
                yesterdayKey -> "Yesterday, ${DISPLAY_DATE_FORMAT.format(date)}"
                else -> DISPLAY_DATE_FORMAT.format(date)
            }
        } catch (e: Exception) {
            dateKey
        }
    }

    fun formatTime(epochMillis: Long?, is24Hour: Boolean = false): String {
        if (epochMillis == null || epochMillis <= 0L) return ""
        val formatter = if (is24Hour) DISPLAY_TIME_24H else DISPLAY_TIME_12H
        return formatter.format(Date(epochMillis))
    }

    fun formatMinutes(minutes: Int): String {
        return when {
            minutes < 60 -> "${minutes}m"
            minutes % 60 == 0 -> "${minutes / 60}h"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }

    fun combineDateAndHourMinute(dateKey: String, hour24: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        try {
            val parsedDate = DATE_KEY_FORMAT.parse(dateKey)
            if (parsedDate != null) {
                cal.time = parsedDate
            }
        } catch (_: Exception) {}
        cal.set(Calendar.HOUR_OF_DAY, hour24)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
