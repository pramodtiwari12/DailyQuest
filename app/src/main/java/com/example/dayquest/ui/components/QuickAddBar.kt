package com.example.dayquest.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayquest.data.model.Categories
import com.example.dayquest.data.model.Difficulty
import com.example.dayquest.data.model.MlCorrectionEntity
import com.example.dayquest.data.model.ScheduleType
import com.example.dayquest.domain.ml.ConfidenceBand
import com.example.dayquest.domain.ml.OnDeviceTaskClassifier
import com.example.dayquest.domain.time.NaturalTimeParser
import com.example.dayquest.domain.time.TimeUtils
import com.example.dayquest.domain.xp.XpCalculator
import kotlin.math.roundToInt

@Composable
fun QuickAddBar(
    selectedDateKey: String,
    onQuickAddScheduled: (rawText: String, categoryOverride: String?, dateKey: String, dueAt: Long?) -> Boolean,
    onOpenFullAddSheet: () -> Unit,
    userCorrections: List<MlCorrectionEntity> = emptyList(),
    autoCategorizeEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var selectedCategoryOverride by remember(text) { mutableStateOf<String?>(null) }

    // Quick scheduling state directly on the bar
    var quickDateKey by remember(selectedDateKey) { mutableStateOf(selectedDateKey) }
    var quickScheduleType by remember { mutableStateOf(ScheduleType.ANYTIME) }
    var quickHour24 by remember { mutableStateOf<Int?>(null) }
    var quickMinute by remember { mutableStateOf<Int?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val parsedResult = remember(text) {
        if (text.length >= 2) NaturalTimeParser.parse(text) else null
    }
    val mlPrediction = remember(parsedResult?.cleanedTitle, userCorrections, autoCategorizeEnabled) {
        val cleaned = parsedResult?.cleanedTitle ?: ""
        if (cleaned.length >= 2) {
            OnDeviceTaskClassifier.predict(cleaned, userCorrections, autoCategorizeEnabled)
        } else null
    }

    // Two-step date-first then time-second modals
    if (showDatePicker) {
        DatePickerModal(
            initialDateKey = quickDateKey,
            onDateSelected = { pickedDate ->
                quickDateKey = pickedDate
                showDatePicker = false
                showTimePicker = true
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        TimePickerModal(
            initialHour = quickHour24 ?: 18,
            initialMinute = quickMinute ?: 0,
            onTimeSelected = { h, m ->
                quickHour24 = h
                quickMinute = m
                quickScheduleType = ScheduleType.BY_TIME
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_add_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Live Preview of Detected Category, XP and Time
            AnimatedVisibility(
                visible = parsedResult != null && mlPrediction != null && text.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (parsedResult != null && mlPrediction != null) {
                    val activeCatId = selectedCategoryOverride ?: mlPrediction.topCategoryId
                    val category = Categories.fromId(activeCatId)
                    val diff = Difficulty.fromString(mlPrediction.suggestedDifficulty)
                    val duration = parsedResult.durationMinutes ?: mlPrediction.suggestedDurationMinutes
                    val confPct = (mlPrediction.confidence * 100).roundToInt()

                    val xpPreview = XpCalculator.calculateTaskXp(
                        durationMinutes = duration,
                        difficulty = diff,
                        dueAt = if (quickScheduleType == ScheduleType.BY_TIME || parsedResult.scheduleType == ScheduleType.BY_TIME) System.currentTimeMillis() + 3600000L else null,
                        completedAt = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis() - 60000L,
                        currentStreak = 0
                    ).finalXp

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Category & Confidence Chip
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(category.color.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .testTag("ml_confidence_chip")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(category.color)
                                )
                                val labelText = when {
                                    selectedCategoryOverride != null -> "Confirmed · ${category.name}"
                                    mlPrediction.confidenceBand == ConfidenceBand.HIGH -> "Auto · ${category.name} $confPct%"
                                    mlPrediction.confidenceBand == ConfidenceBand.MEDIUM -> "Suggested · ${category.name} $confPct%"
                                    else -> "Confirm · ${category.name}"
                                }
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = category.color
                                )
                            }

                            // Duration token preview
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Duration",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${duration}m",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (parsedResult.isAmbiguous) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Notice",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = parsedResult.ambiguityReason ?: "Confirm schedule",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "+$xpPreview XP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Alternative Suggestions Strip
                        if (mlPrediction.confidenceBand != ConfidenceBand.HIGH && selectedCategoryOverride == null) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    Text(
                                        text = "Confirm category:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val options = (listOf(mlPrediction.topCategoryId to mlPrediction.confidence) + mlPrediction.alternatives)
                                    .distinctBy { it.first }
                                    .take(4)
                                items(options) { (altCatId, _) ->
                                    val altCat = Categories.fromId(altCatId)
                                    Text(
                                        text = altCat.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = altCat.color,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(1.dp, altCat.color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .clickable { selectedCategoryOverride = altCat.id }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Add Input Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            text = "Add a task...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_input"),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (text.isNotBlank()) {
                            var dueAtMillis: Long? = null
                            if (quickScheduleType == ScheduleType.BY_TIME && quickHour24 != null && quickMinute != null) {
                                dueAtMillis = TimeUtils.combineDateAndHourMinute(quickDateKey, quickHour24!!, quickMinute!!)
                            }
                            val success = onQuickAddScheduled(text, selectedCategoryOverride, quickDateKey, dueAtMillis)
                            if (success) {
                                text = ""
                                quickScheduleType = ScheduleType.ANYTIME
                                quickHour24 = null
                                quickMinute = null
                            }
                        }
                    })
                )

                IconButton(
                    onClick = onOpenFullAddSheet,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("quick_add_expand_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Expand task details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable(enabled = text.isNotBlank()) {
                            var dueAtMillis: Long? = null
                            if (quickScheduleType == ScheduleType.BY_TIME && quickHour24 != null && quickMinute != null) {
                                dueAtMillis = TimeUtils.combineDateAndHourMinute(quickDateKey, quickHour24!!, quickMinute!!)
                            }
                            val success = onQuickAddScheduled(text, selectedCategoryOverride, quickDateKey, dueAtMillis)
                            if (success) {
                                text = ""
                                quickScheduleType = ScheduleType.ANYTIME
                                quickHour24 = null
                                quickMinute = null
                            }
                        }
                        .testTag("quick_add_submit_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Add task",
                        tint = if (text.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Compact Discoverable Scheduling Strip directly below input
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Today / Date Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (quickDateKey == TimeUtils.todayDateKey()) "Today" else quickDateKey,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // By Time / Anytime Toggle Chip
                val hasTime = quickScheduleType == ScheduleType.BY_TIME && quickHour24 != null && quickMinute != null
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (hasTime) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                        .clickable {
                            if (hasTime) {
                                // Reset to anytime
                                quickScheduleType = ScheduleType.ANYTIME
                                quickHour24 = null
                                quickMinute = null
                            } else {
                                // Requirement 9: Ask for date first, then time
                                showDatePicker = true
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = if (hasTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (hasTime) String.format("By %02d:%02d", quickHour24, quickMinute) else "Add By Time",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (hasTime) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (hasTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Anytime indicator
                if (quickScheduleType == ScheduleType.ANYTIME) {
                    Text(
                        text = "Anytime",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
