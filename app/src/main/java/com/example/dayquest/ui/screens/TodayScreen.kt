package com.example.dayquest.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.dayquest.data.model.MlCorrectionEntity
import com.example.dayquest.data.model.TaskEntity
import com.example.dayquest.domain.time.TimeUtils
import com.example.dayquest.ui.TodayTasksGroup
import com.example.dayquest.ui.components.QuickAddBar
import com.example.dayquest.ui.components.TaskRowItem
import com.example.dayquest.ui.theme.OverdueAmber

@Composable
fun TodayScreen(
    selectedDateKey: String,
    groupedTasks: TodayTasksGroup,
    searchQuery: String,
    categoryFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onToggleComplete: (TaskEntity) -> Unit,
    onTaskClick: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onQuickAdd: (rawText: String, categoryOverride: String?, dateKey: String, dueAt: Long?) -> Boolean,
    onOpenFullAddSheet: () -> Unit,
    onRescheduleTask: (TaskEntity, String) -> Unit,
    userCorrections: List<MlCorrectionEntity> = emptyList(),
    autoCategorizeEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var searchVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Search & Filter Bar
            AnimatedVisibility(
                visible = searchVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search quests...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            onSearchQueryChange("")
                            searchVisible = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .testTag("search_text_field"),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }

            if (!searchVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateFormatted = remember(selectedDateKey) {
                        if (selectedDateKey == TimeUtils.todayDateKey()) "Today's Quests"
                        else "Quests for $selectedDateKey"
                    }
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { searchVisible = true },
                        modifier = Modifier.size(36.dp).testTag("open_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search tasks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
                    .testTag("category_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    val isAllSelected = categoryFilter == null
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { onCategoryFilterChange(null) },
                        label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("filter_chip_all")
                    )
                }
                items(Categories.ALL) { cat ->
                    val isSelected = categoryFilter.equals(cat.id, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryFilterChange(cat.id) },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(cat.color)
                                )
                                Text(cat.name, style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        modifier = Modifier.testTag("filter_chip_${cat.id}")
                    )
                }
            }

            // Tasks List
            val totalTaskCount = groupedTasks.byTimeTasks.size +
                    groupedTasks.anytimeTasks.size +
                    groupedTasks.completedTasks.size +
                    groupedTasks.overdueTasks.size

            if (totalTaskCount == 0) {
                EmptyTaskState(
                    onSuggestionClick = { onQuickAdd(it, null, selectedDateKey, null) },
                    onOpenAdd = onOpenFullAddSheet,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .testTag("tasks_lazy_column"),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Overdue Banner if any
                    if (groupedTasks.overdueTasks.isNotEmpty()) {
                        item {
                            OverdueTasksBanner(
                                overdueTasks = groupedTasks.overdueTasks,
                                onRescheduleToToday = { task ->
                                    onRescheduleTask(task, TimeUtils.todayDateKey())
                                },
                                onToggleComplete = onToggleComplete,
                                onTaskClick = onTaskClick,
                                onDeleteTask = onDeleteTask
                            )
                        }
                    }

                    // 2. By Time Section
                    if (groupedTasks.byTimeTasks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "BY TIME",
                                count = groupedTasks.byTimeTasks.size,
                                icon = Icons.Default.Schedule,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(groupedTasks.byTimeTasks, key = { it.id }) { task ->
                            TaskRowItem(
                                task = task,
                                onToggleComplete = { onToggleComplete(task) },
                                onClick = { onTaskClick(task) },
                                onDelete = { onDeleteTask(task) }
                            )
                        }
                    }

                    // 3. Anytime Section
                    if (groupedTasks.anytimeTasks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "ANYTIME",
                                count = groupedTasks.anytimeTasks.size,
                                icon = Icons.Default.FormatListBulleted,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(groupedTasks.anytimeTasks, key = { it.id }) { task ->
                            TaskRowItem(
                                task = task,
                                onToggleComplete = { onToggleComplete(task) },
                                onClick = { onTaskClick(task) },
                                onDelete = { onDeleteTask(task) }
                            )
                        }
                    }

                    // 4. Completed Section
                    if (groupedTasks.completedTasks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "COMPLETED",
                                count = groupedTasks.completedTasks.size,
                                icon = Icons.Default.CheckCircleOutline,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(groupedTasks.completedTasks, key = { it.id }) { task ->
                            TaskRowItem(
                                task = task,
                                onToggleComplete = { onToggleComplete(task) },
                                onClick = { onTaskClick(task) },
                                onDelete = { onDeleteTask(task) }
                            )
                        }
                    }
                }
            }
        }

        // Quick Add Bar Anchored at Bottom
        QuickAddBar(
            selectedDateKey = selectedDateKey,
            onQuickAddScheduled = onQuickAdd,
            onOpenFullAddSheet = onOpenFullAddSheet,
            userCorrections = userCorrections,
            autoCategorizeEnabled = autoCategorizeEnabled,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = tint
        )
        Text(
            text = "($count)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OverdueTasksBanner(
    overdueTasks: List<TaskEntity>,
    onRescheduleToToday: (TaskEntity) -> Unit,
    onToggleComplete: (TaskEntity) -> Unit,
    onTaskClick: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = OverdueAmber.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OverdueAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .testTag("overdue_tasks_banner")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Overdue",
                        tint = OverdueAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "OVERDUE QUESTS (${overdueTasks.size})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = OverdueAmber
                    )
                }
                Text(
                    text = "Requires attention",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = OverdueAmber
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                overdueTasks.forEach { task ->
                    TaskRowItem(
                        task = task,
                        onToggleComplete = { onToggleComplete(task) },
                        onClick = { onTaskClick(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTaskState(
    onSuggestionClick: (String) -> Unit,
    onOpenAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "No quests planned for today",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Start your day with intent. Pick a quick quest below or create a custom task:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Actionable Suggestion Chips (vector icons, no emojis)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            listOf(
                Triple("Morning workout 45m", Icons.Default.FitnessCenter, "Health & Fitness"),
                Triple("Study mathematics 45m", Icons.Default.MenuBook, "Study"),
                Triple("Write Python functions 30m", Icons.Default.Code, "Skill/Coding"),
                Triple("15 minutes of meditation", Icons.Default.SelfImprovement, "Mindfulness/Self-care")
            ).forEach { (taskTitle, icon, _) ->
                SuggestionChip(
                    onClick = { onSuggestionClick(taskTitle) },
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = { Text(taskTitle, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.testTag("suggestion_chip_${taskTitle.take(6)}")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onOpenAdd,
            modifier = Modifier.testTag("empty_state_add_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create Quest Task")
        }
    }
}
