package com.example.dayquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayquest.domain.time.TimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarDay(
    val dateKey: String,
    val dayOfWeekLetter: String,
    val dayNumber: String,
    val isToday: Boolean
)

@Composable
fun WeekCalendarStrip(
    selectedDateKey: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = remember {
        val cal = Calendar.getInstance()
        val todayKey = TimeUtils.todayDateKey()
        val list = mutableListOf<CalendarDay>()
        val dayOfWeekFormat = SimpleDateFormat("EEEEE", Locale.getDefault()) // Single letter e.g. M, T, W
        val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())

        // Show window: 3 days before today to 3 days after today
        for (offset in -3..3) {
            val offsetCal = Calendar.getInstance()
            offsetCal.add(Calendar.DAY_OF_YEAR, offset)
            val dKey = TimeUtils.DATE_KEY_FORMAT.format(offsetCal.time)
            list.add(
                CalendarDay(
                    dateKey = dKey,
                    dayOfWeekLetter = dayOfWeekFormat.format(offsetCal.time),
                    dayNumber = dayNumberFormat.format(offsetCal.time),
                    isToday = (dKey == todayKey)
                )
            )
        }
        list
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("week_calendar_strip"),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            days.forEach { day ->
                val isSelected = (day.dateKey == selectedDateKey)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDateSelected(day.dateKey) }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("day_item_${day.dateKey}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day.dayOfWeekLetter,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = day.dayNumber,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    // Dot indicator for today
                    if (day.isToday) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                }
            }
        }
    }
}
