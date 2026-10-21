package com.example.dayquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayquest.data.model.Categories
import com.example.dayquest.data.model.SubtaskEntity
import com.example.dayquest.data.model.TaskEntity
import com.example.dayquest.domain.time.TimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    task: TaskEntity,
    subtasksFlow: Flow<List<SubtaskEntity>>,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRescheduleTomorrow: () -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onAddSubtask: (String) -> Unit,
    onDeleteSubtask: (SubtaskEntity) -> Unit,
    onFocusSessionCompleted: (durationSeconds: Int, completeTask: Boolean) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtasks by subtasksFlow.collectAsState(initial = emptyList())
    var newSubtaskText by remember { mutableStateOf("") }
    val category = Categories.fromId(task.categoryId)
    val isDone = task.status == "done"

    // Built-in Focus Timer State (Timestamp-backed per Master Prompt)
    var focusPresetMinutes by remember { mutableIntStateOf(25) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerStartInstantMs by remember { mutableLongStateOf(0L) }
    var accumulatedElapsedSeconds by remember { mutableIntStateOf(task.timerSeconds) }
    var liveElapsedSeconds by remember { mutableIntStateOf(task.timerSeconds) }

    LaunchedEffect(isTimerRunning, timerStartInstantMs) {
        while (isTimerRunning) {
            val deltaSec = ((System.currentTimeMillis() - timerStartInstantMs) / 1000L).toInt().coerceAtLeast(0)
            liveElapsedSeconds = accumulatedElapsedSeconds + deltaSec
            delay(500L)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("task_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Category Badge & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(category.color.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(category.color)
                    )
                    Text(
                        text = "${category.name} • ${task.categorySource.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = category.color
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close sheet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!task.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Schedule", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = when (task.scheduleType) {
                            "byTime" -> if (task.dueAt != null) "by ${TimeUtils.formatTime(task.dueAt)}" else "By Time"
                            "range" -> "Range"
                            else -> "Anytime"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${task.estMinutes}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Difficulty", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(task.difficulty, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("XP Award", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (task.xpAwarded > 0) "+${task.xpAwarded} XP" else "+10% w/ Timer",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Built-in Focus / Pomodoro Timer Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .testTag("focus_timer_card"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Focus Timer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "POMODORO FOCUS TIMER (+10% XP)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Preset duration pills (15m / 25m / 45m)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(15, 25, 45).forEach { m ->
                                val selected = focusPresetMinutes == m
                                Text(
                                    text = "${m}m",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val totalTargetSeconds = focusPresetMinutes * 60
                    val remainingSec = (totalTargetSeconds - liveElapsedSeconds).coerceAtLeast(0)
                    val dispMin = remainingSec / 60
                    val dispSec = remainingSec % 60

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = String.format("%02d:%02d", dispMin, dispSec),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Focused: ${liveElapsedSeconds / 60}m ${liveElapsedSeconds % 60}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    if (isTimerRunning) {
                                        val delta = ((System.currentTimeMillis() - timerStartInstantMs) / 1000L).toInt().coerceAtLeast(0)
                                        accumulatedElapsedSeconds += delta
                                        liveElapsedSeconds = accumulatedElapsedSeconds
                                        isTimerRunning = false
                                    } else {
                                        timerStartInstantMs = System.currentTimeMillis()
                                        isTimerRunning = true
                                    }
                                },
                                modifier = Modifier.testTag("timer_start_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isTimerRunning) "Pause" else "Start")
                            }

                            Button(
                                onClick = {
                                    isTimerRunning = false
                                    val loggedSeconds = maxOf(60, if (liveElapsedSeconds > 0) liveElapsedSeconds else focusPresetMinutes * 60)
                                    onFocusSessionCompleted(loggedSeconds, true)
                                    onDismiss()
                                },
                                modifier = Modifier.testTag("timer_complete_with_bonus_button")
                            ) {
                                Text("Log & Complete")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtasks checklist (pays up to 20% XP per subtask, capped at 80%)
            Text(
                text = "SUBTASKS (${subtasks.count { it.done }}/${subtasks.size}) • +20% XP per subtask (max 80%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            subtasks.forEach { sub ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = sub.done,
                        onCheckedChange = { onToggleSubtask(sub) },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sub.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sub.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (sub.awardedXp > 0) {
                        Text(
                            text = "+${sub.awardedXp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDeleteSubtask(sub) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove subtask",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    placeholder = { Text("Add subtask...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        if (newSubtaskText.isNotBlank()) {
                            onAddSubtask(newSubtaskText.trim())
                            newSubtaskText = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add subtask item",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onToggleComplete()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("detail_toggle_complete_button")
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Default.Undo else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isDone) "Reopen Task" else "Complete Task")
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onEdit()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("detail_edit_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onRescheduleTomorrow()
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Update, contentDescription = "Reschedule to tomorrow", modifier = Modifier.size(16.dp))
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onDelete()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("detail_delete_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
