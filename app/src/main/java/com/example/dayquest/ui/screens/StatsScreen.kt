package com.example.dayquest.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayquest.data.model.*
import com.example.dayquest.domain.time.TimeUtils
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    allTasks: List<TaskEntity>,
    xpLogs: List<XpLogEntity>,
    categoryProgress: List<CategoryProgressEntity>,
    userStats: UserStatsEntity,
    onDrillDownDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedRange by remember { mutableStateOf("Week") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedPointTooltip by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var showWeeklyReportDialog by remember { mutableStateOf(false) }

    val filteredTasks = remember(allTasks, selectedCategory) {
        if (selectedCategory == null) allTasks
        else allTasks.filter { it.categoryId.equals(selectedCategory, ignoreCase = true) }
    }

    val totalPlanned = filteredTasks.size
    val totalCompleted = filteredTasks.count { it.status == "done" }
    val completionRate = if (totalPlanned > 0) ((totalCompleted.toFloat() / totalPlanned) * 100).roundToInt() else 0
    val focusHours = String.format("%.1f", userStats.totalFocusSeconds / 3600f)

    // Last 7 days XP series
    val last7DaysSeries = remember(xpLogs, selectedCategory) {
        (-6..0).map { offset ->
            val dateKey = TimeUtils.dateKeyForOffset(offset)
            val dayXp = xpLogs.filter {
                it.localDateKey == dateKey &&
                        (selectedCategory == null || it.categoryId.equals(selectedCategory, ignoreCase = true))
            }.sumOf { it.xpDelta }.coerceAtLeast(0)
            dateKey to dayXp
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("stats_screen"),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Range Selector + Weekly Report CTA
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("Day", "Week", "Month", "Year", "All").forEach { range ->
                        val active = selectedRange == range
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedRange = range }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("stats_range_$range")
                        ) {
                            Text(
                                text = range,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = { showWeeklyReportDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("weekly_report_button")
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Category Filter Row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All Categories", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                items(Categories.ALL) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.id,
                        onClick = { selectedCategory = if (selectedCategory == cat.id) null else cat.id },
                        label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // 4 Summary KPI Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiCard("Total XP", "${userStats.totalXp}", Modifier.weight(1f))
                KpiCard("Completion", "$completionRate%", Modifier.weight(1f))
                KpiCard("Focus Time", "${focusHours}h", Modifier.weight(1f))
                KpiCard("Best Streak", "${userStats.longestStreak}d", Modifier.weight(1f))
            }
        }

        // XP Trend Line Chart (Tappable points + Date Drill-Down)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .testTag("xp_line_chart_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "XP PROGRESSION TREND (7 DAYS)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedPointTooltip != null) {
                            TextButton(
                                onClick = { onDrillDownDate(selectedPointTooltip!!.first) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = "${selectedPointTooltip!!.first.takeLast(5)}: ${selectedPointTooltip!!.second} XP → View Day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val primaryColor = MaterialTheme.colorScheme.primary
                    val maxVal = (last7DaysSeries.maxOfOrNull { it.second } ?: 100).coerceAtLeast(50)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val stepX = if (last7DaysSeries.size > 1) w / (last7DaysSeries.size - 1) else w

                        val path = Path()
                        last7DaysSeries.forEachIndexed { idx, (_, xp) ->
                            val x = idx * stepX
                            val y = h - ((xp.toFloat() / maxVal) * (h * 0.8f)) - 10f
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )

                        last7DaysSeries.forEachIndexed { idx, (_, xp) ->
                            val x = idx * stepX
                            val y = h - ((xp.toFloat() / maxVal) * (h * 0.8f)) - 10f
                            drawCircle(
                                color = primaryColor,
                                radius = 9f,
                                center = Offset(x, y)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tappable Day Labels under chart
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        last7DaysSeries.forEach { (dateKey, xp) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedPointTooltip = dateKey to xp }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = dateKey.takeLast(5),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$xp",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category Level & XP Share Breakdown
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CATEGORY MASTERY & XP SHARE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val maxCatXp = (categoryProgress.maxOfOrNull { it.attributedXp } ?: 100).coerceAtLeast(50)
                    Categories.ALL.forEach { cat ->
                        val prog = categoryProgress.firstOrNull { it.categoryId.equals(cat.id, ignoreCase = true) }
                        val xp = prog?.attributedXp ?: 0
                        val level = prog?.level ?: 1
                        val count = prog?.tasksCompletedCount ?: 0
                        val fraction = (xp.toFloat() / maxCatXp).coerceIn(0.03f, 1f)

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(cat.color)
                                    )
                                    Text(
                                        text = "${cat.name} (Lv.$level)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "$xp XP • $count tasks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cat.color)
                                )
                            }
                        }
                    }
                }
            }
        }

        // GitHub-style Activity Heatmap (Last 28 Days Intensity Grid)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .testTag("activity_heatmap_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ACTIVITY HEATMAP (28-DAY INTENSITY)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val heatmapDays = (-27..0).map { offset ->
                        val dKey = TimeUtils.dateKeyForOffset(offset)
                        val xp = xpLogs.filter { it.localDateKey == dKey }.sumOf { it.xpDelta }.coerceAtLeast(0)
                        dKey to xp
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        heatmapDays.chunked(7).forEach { weekChunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                weekChunk.forEach { (dKey, xp) ->
                                    val intensityAlpha = when {
                                        xp >= 150 -> 1.0f
                                        xp >= 75 -> 0.70f
                                        xp >= 25 -> 0.45f
                                        xp > 0 -> 0.25f
                                        else -> 0.08f
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(22.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = intensityAlpha))
                                            .clickable { onDrillDownDate(dKey) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dKey.takeLast(2),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                            color = if (intensityAlpha >= 0.6f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Legend: Lighter = 0 XP • Darker = 150+ Daily XP. Tap any cell to drill into that date.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Rule-Based Insights (Sample-Size Guarded)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ON-DEVICE PRODUCTIVITY INSIGHTS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val insightText = if (totalCompleted < 3) {
                        "Sample guard active (${totalCompleted}/3 completed tasks): Complete at least 3 tasks to unlock statistically grounded weekday and time-of-day trends."
                    } else {
                        val topCat = categoryProgress.maxByOrNull { it.attributedXp }?.categoryId ?: "Study"
                        "Based on $totalCompleted completed tasks ($completionRate% completion rate), your highest XP velocity is in $topCat. Scheduled By Time tasks earn 1.2x XP when finished >5m early."
                    }
                    Text(
                        text = insightText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Append-Only XP Ledger History
        item {
            Text(
                text = "APPEND-ONLY XP LEDGER (${xpLogs.size} ENTRIES)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        items(xpLogs.take(15), key = { it.id }) { log ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.reason.replace("_", " ").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${log.localDateKey} • Key: ${log.idempotencyKey.take(22)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${if (log.xpDelta >= 0) "+" else ""}${log.xpDelta} XP",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Weekly Report Modal
    if (showWeeklyReportDialog) {
        val topCategory = categoryProgress.maxByOrNull { it.attributedXp }?.categoryId ?: "Study"
        AlertDialog(
            onDismissRequest = { showWeeklyReportDialog = false },
            title = { Text("Weekly Quest Summary Report", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Total XP Earned: ${userStats.totalXp} XP (Lv.${userStats.currentLevel})")
                    Text("• Best Category: $topCategory")
                    Text("• Active Streak: ${userStats.currentStreak} days (Best: ${userStats.longestStreak}d)")
                    Text("• Tasks Completed: $totalCompleted / $totalPlanned ($completionRate%)")
                    Text("• Focus Time: $focusHours hours")
                    Text("• League Standing: ${userStats.leagueTier} League (${userStats.weeklyLeagueXp} Weekly XP)")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "DayQuest Weekly Report\nLevel ${userStats.currentLevel} (${userStats.totalXp} XP)\nStreak: ${userStats.currentStreak} days\nTop Category: $topCategory\nTasks Completed: $totalCompleted"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share DayQuest Report"))
                        showWeeklyReportDialog = false
                    }
                ) {
                    Text("Share Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeeklyReportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
