package com.example.dayquest.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayquest.data.model.Categories
import com.example.dayquest.data.model.Difficulty
import com.example.dayquest.data.model.TaskEntity
import com.example.dayquest.domain.time.TimeUtils
import com.example.dayquest.domain.xp.XpCalculator
import com.example.dayquest.ui.theme.OverdueAmber
import com.example.dayquest.ui.theme.XpGold

@Composable
fun TaskRowItem(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isOverdue: Boolean = false
) {
    val isDone = task.status == "done"
    val category = Categories.fromId(task.categoryId)
    val diff = Difficulty.fromString(task.difficulty)

    val xpPreview = if (task.xpAwarded > 0) {
        task.xpAwarded
    } else {
        XpCalculator.calculateTaskXp(
            durationMinutes = task.estMinutes,
            difficulty = diff,
            dueAt = task.dueAt,
            completedAt = System.currentTimeMillis(),
            createdAt = task.createdAt,
            currentStreak = 0,
            timerUsed = task.timerUsed
        ).finalXp
    }

    val cardAlpha = if (isDone) 0.65f else 1.0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (isOverdue && !isDone) OverdueAmber.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag("task_row_${task.id}"),
        color = MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox Button with 48dp touch target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onToggleComplete() }
                    .testTag("task_checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (isDone) category.color else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .background(
                            if (isDone) category.color else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Task Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                        fontSize = 13.5.sp,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Metadata Row: Category, Due Time, Duration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(category.color)
                        )
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = category.color
                        )
                    }

                    // Due time if By Time or Range
                    if (task.scheduleType == "byTime" && task.dueAt != null) {
                        val timeStr = TimeUtils.formatTime(task.dueAt)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isOverdue && !isDone) OverdueAmber.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Due time",
                                tint = if (isOverdue && !isDone) OverdueAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "by $timeStr",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isOverdue && !isDone) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isOverdue && !isDone) OverdueAmber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Duration chip
                    Text(
                        text = "${task.estMinutes}m • ${task.difficulty}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // XP Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isDone) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$xpPreview XP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    ),
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
