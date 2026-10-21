package com.example.dayquest.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.dayquest.data.model.*
import com.example.dayquest.domain.time.TimeUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskBottomSheet(
    initialTask: TaskEntity? = null,
    selectedDateKey: String,
    onSave: (TaskEntity, List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = initialTask != null

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var notes by remember { mutableStateOf(initialTask?.notes ?: "") }
    var selectedCategory by remember { mutableStateOf(initialTask?.categoryId ?: "Study") }
    var selectedDifficulty by remember { mutableStateOf(initialTask?.difficulty ?: "Medium") }
    var selectedDuration by remember { mutableIntStateOf(initialTask?.estMinutes ?: 30) }
    var taskDateKey by remember { mutableStateOf(initialTask?.localDateKey ?: selectedDateKey) }

    var selectedScheduleType by remember {
        mutableStateOf(
            when (initialTask?.scheduleType) {
                "byTime" -> ScheduleType.BY_TIME
                "range" -> ScheduleType.RANGE
                else -> ScheduleType.ANYTIME
            }
        )
    }

    var selectedHour24 by remember {
        val cal = Calendar.getInstance()
        if (initialTask?.dueAt != null) cal.timeInMillis = initialTask.dueAt
        mutableStateOf<Int?>(if (initialTask?.dueAt != null) cal.get(Calendar.HOUR_OF_DAY) else null)
    }
    var selectedMinute by remember {
        val cal = Calendar.getInstance()
        if (initialTask?.dueAt != null) cal.timeInMillis = initialTask.dueAt
        mutableStateOf<Int?>(if (initialTask?.dueAt != null) cal.get(Calendar.MINUTE) else null)
    }

    var selectedPriority by remember { mutableStateOf(initialTask?.priority ?: "medium") }
    var selectedRepeatRule by remember { mutableStateOf(initialTask?.repeatRule ?: "none") }

    var showDatePickerModal by remember { mutableStateOf(false) }
    var showTimePickerModal by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    val subtasks = remember { mutableStateListOf<String>() }
    var newSubtaskInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    // Determine if form has unsaved changes
    val hasUnsavedChanges = remember(title, notes, subtasks.size, selectedScheduleType, selectedHour24, selectedMinute, taskDateKey) {
        if (initialTask == null) {
            title.isNotBlank() || notes.isNotBlank() || subtasks.isNotEmpty() || selectedHour24 != null
        } else {
            title != initialTask.title ||
                    notes != (initialTask.notes ?: "") ||
                    subtasks.isNotEmpty() ||
                    taskDateKey != initialTask.localDateKey
        }
    }

    val requestExit = {
        if (hasUnsavedChanges) {
            showDiscardConfirmDialog = true
        } else {
            onDismiss()
        }
    }

    // Intercept hardware and gesture back presses to preserve draft
    BackHandler(enabled = true) {
        requestExit()
    }

    // Validation per Requirement 9:
    // By Time task CANNOT be saved without both a valid date and a valid time!
    val isTitleValid = title.isNotBlank() && title.length <= 120
    val isTimeValidForByTime = selectedScheduleType != ScheduleType.BY_TIME ||
            (taskDateKey.isNotBlank() && selectedHour24 != null && selectedMinute != null)
    val canSave = isTitleValid && isTimeValidForByTime

    // Step 1: Date Picker Modal
    if (showDatePickerModal) {
        DatePickerModal(
            initialDateKey = taskDateKey,
            onDateSelected = { pickedDate ->
                taskDateKey = pickedDate
                showDatePickerModal = false
                // Sequence requirement: Date first, then open time picker for that selected date!
                showTimePickerModal = true
            },
            onDismiss = {
                showDatePickerModal = false
            }
        )
    }

    // Step 2: Time Picker Modal
    if (showTimePickerModal) {
        TimePickerModal(
            initialHour = selectedHour24 ?: 18,
            initialMinute = selectedMinute ?: 0,
            onTimeSelected = { h, m ->
                selectedHour24 = h
                selectedMinute = m
                showTimePickerModal = false
            },
            onDismiss = {
                showTimePickerModal = false
            }
        )
    }

    // Discard Confirmation Dialog
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            title = {
                Text(
                    text = "Discard this task?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes in this task. Are you sure you want to discard your draft?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_discard_button")
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDiscardConfirmDialog = false },
                    modifier = Modifier.testTag("keep_editing_button")
                ) {
                    Text("Keep Editing")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = requestExit,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("add_edit_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isEditing) "Edit Task" else "New Quest Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = requestExit, modifier = Modifier.size(32.dp).testTag("close_sheet_button")) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close sheet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it },
                label = { Text("Task title *") },
                placeholder = { Text("e.g. Study calculus formulas") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                singleLine = true,
                isError = title.isNotBlank() && title.length > 120,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (title.isBlank()) {
                            Text("Title is required", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("")
                        }
                        Text("${title.length}/120")
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Schedule Segmented Control
            Text(
                text = "SCHEDULE TYPE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    ScheduleType.ANYTIME to "Anytime",
                    ScheduleType.BY_TIME to "By Time",
                    ScheduleType.RANGE to "Range"
                ).forEach { (type, label) ->
                    val isSelected = selectedScheduleType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable {
                                selectedScheduleType = type
                                // Requirement 9: Do NOT open the clock immediately when By Time is selected!
                            }
                            .padding(vertical = 7.dp)
                            .testTag("schedule_tab_${label.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // By Time: Date First, Time Second Picker Controls
            if (selectedScheduleType == ScheduleType.BY_TIME) {
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Due Date & Time (Date first, then time)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Date Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { showDatePickerModal = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("pick_date_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Date: $taskDateKey",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text("Change Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }

                        // Time Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    if (selectedHour24 == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    // If date not chosen yet, open date first; otherwise open time
                                    if (taskDateKey.isBlank()) {
                                        showDatePickerModal = true
                                    } else {
                                        showTimePickerModal = true
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("pick_time_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (selectedHour24 == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                if (selectedHour24 != null && selectedMinute != null) {
                                    val timeStr = String.format("%02d:%02d", selectedHour24, selectedMinute)
                                    Text(
                                        text = "Due Time: $timeStr",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "Set Time (Required for By Time)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                text = if (selectedHour24 == null) "Pick Time" else "Change",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (selectedHour24 == null) {
                            Text(
                                text = "By Time requires both a valid date and time before saving.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Selection (11 Categories)
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().testTag("category_selector_row")
            ) {
                items(Categories.ALL) { cat ->
                    val isSelected = selectedCategory.equals(cat.id, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) cat.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                if (isSelected) cat.color.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { selectedCategory = cat.id }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("category_chip_${cat.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(cat.color)
                        )
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Difficulty & Duration Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Difficulty Chips
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DIFFICULTY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Difficulty.entries.forEach { diff ->
                            val isSelected = selectedDifficulty.equals(diff.label, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedDifficulty = diff.label }
                                    .padding(vertical = 6.dp)
                                    .testTag("difficulty_${diff.label.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = diff.label.take(1),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Duration Chips
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DURATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(15, 30, 45, 60).forEach { mins ->
                            val isSelected = selectedDuration == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedDuration = mins }
                                    .padding(vertical = 6.dp)
                                    .testTag("duration_${mins}m"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtasks Section
            Text(
                text = "SUBTASKS (CHECKLIST)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            subtasks.forEachIndexed { index, sub ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• $sub",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { subtasks.removeAt(index) },
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSubtaskInput,
                    onValueChange = { newSubtaskInput = it },
                    placeholder = { Text("Add subtask item...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtask_input"),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        if (newSubtaskInput.isNotBlank()) {
                            subtasks.add(newSubtaskInput.trim())
                            newSubtaskInput = ""
                        }
                    },
                    modifier = Modifier.testTag("add_subtask_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add subtask",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Notes Field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Add extra context or checklist") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_notes_input"),
                maxLines = 3,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    if (canSave) {
                        var dueAtMillis: Long? = null
                        if (selectedScheduleType == ScheduleType.BY_TIME && selectedHour24 != null && selectedMinute != null) {
                            dueAtMillis = TimeUtils.combineDateAndHourMinute(
                                taskDateKey,
                                selectedHour24!!,
                                selectedMinute!!
                            )
                        }

                        val taskToSave = (initialTask ?: TaskEntity(
                            title = title.trim(),
                            localDateKey = taskDateKey
                        )).copy(
                            title = title.trim(),
                            notes = notes.trim().ifEmpty { null },
                            categoryId = selectedCategory,
                            categorySource = "user",
                            difficulty = selectedDifficulty,
                            estMinutes = selectedDuration,
                            localDateKey = taskDateKey,
                            scheduleType = when (selectedScheduleType) {
                                ScheduleType.BY_TIME -> "byTime"
                                ScheduleType.RANGE -> "range"
                                else -> "anytime"
                            },
                            dueAt = dueAtMillis,
                            priority = selectedPriority,
                            repeatRule = selectedRepeatRule,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(taskToSave, subtasks.toList())
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_task_button")
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Create Quest Task",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
