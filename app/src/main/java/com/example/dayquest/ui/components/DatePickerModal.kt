package com.example.dayquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dayquest.domain.time.TimeUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DatePickerModal(
    initialDateKey: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialCal = remember(initialDateKey) {
        val cal = Calendar.getInstance()
        try {
            val d = TimeUtils.DATE_KEY_FORMAT.parse(initialDateKey)
            if (d != null) cal.time = d
        } catch (ignored: Exception) {}
        cal
    }

    var displayedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) } // 0-based
    var selectedDateKey by remember { mutableStateOf(initialDateKey) }

    val monthHeader = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    }

    // Days in current displayed month
    val daysInMonth = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sun = 1, Mon = 2
        // Shift so Monday is first column (Mon=0, Tue=1 ... Sun=6)
        val leadingBlanks = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val list = mutableListOf<Int?>()
        repeat(leadingBlanks) { list.add(null) }
        for (day in 1..maxDays) {
            list.add(day)
        }
        list
    }

    val todayKey = remember { TimeUtils.todayDateKey() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("date_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Select Due Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Pick the date for this scheduled task",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Month navigation row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (displayedMonth == 0) {
                                displayedMonth = 11
                                displayedYear -= 1
                            } else {
                                displayedMonth -= 1
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }

                    Text(
                        text = monthHeader,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = {
                            if (displayedMonth == 11) {
                                displayedMonth = 0
                                displayedYear += 1
                            } else {
                                displayedMonth += 1
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Day of week headers (Mon..Sun)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.width(32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Calendar grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    items(daysInMonth) { day ->
                        if (day == null) {
                            Box(modifier = Modifier.size(34.dp))
                        } else {
                            val thisDateKey = String.format("%04d-%02d-%02d", displayedYear, displayedMonth + 1, day)
                            val isSelected = thisDateKey == selectedDateKey
                            val isToday = thisDateKey == todayKey

                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isToday && !isSelected) 1.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedDateKey = thisDateKey
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDateSelected(selectedDateKey)
                        },
                        modifier = Modifier.testTag("confirm_date_button")
                    ) {
                        Text("Select Date")
                    }
                }
            }
        }
    }
}
