package com.example.dayquest.ui.screens

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayquest.data.model.QuestEntity
import com.example.dayquest.data.model.RoutineTemplateEntity
import com.example.dayquest.domain.badges.BadgeRarity
import com.example.dayquest.domain.badges.ResolvedBadge
import com.example.dayquest.ui.theme.XpGold

@Composable
fun QuestsBadgesScreen(
    dailyQuests: List<QuestEntity>,
    routines: List<RoutineTemplateEntity>,
    resolvedBadges: List<ResolvedBadge>,
    onApplyRoutine: (RoutineTemplateEntity) -> Unit,
    onCreateRoutine: (name: String, description: String, tasksPipeDelimited: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Quests & Routines, 1 = Badges (69)
    var selectedRarity by remember { mutableStateOf<BadgeRarity?>(null) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var badgeSearchQuery by remember { mutableStateOf("") }
    var selectedBadgeDetail by remember { mutableStateOf<ResolvedBadge?>(null) }
    var showCreateRoutineDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("quests_badges_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Segmented Switcher: Daily Quests vs Badges
        val unlockedCount = resolvedBadges.count { it.isUnlocked }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(3.dp)
        ) {
            listOf(
                0 to "Daily Quests & Routines",
                1 to "Badges ($unlockedCount/${resolvedBadges.size})"
            ).forEach { (idx, title) ->
                val selected = activeTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeTab = idx }
                        .padding(vertical = 8.dp)
                        .testTag("quests_subtab_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeTab == 0) {
            val allThreeDone = dailyQuests.size >= 3 && dailyQuests.all { it.done }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Daily Quest Grand Bonus Banner
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (allThreeDone) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DAILY QUEST GRAND BONUS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (allThreeDone) "All 3 Quests Complete! +100 XP Claimed"
                                    else "Complete all 3 mini-quests today for +100 Bonus XP",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "+100 XP",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 3 Deterministic Mini-Quests
                items(dailyQuests, key = { it.id }) { quest ->
                    val frac = if (quest.target > 0) (quest.progress.toFloat() / quest.target).coerceIn(0f, 1f) else 0f
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                                        imageVector = if (quest.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (quest.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = quest.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "+${quest.rewardXp} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = quest.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { frac },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Text(
                                    text = "${quest.progress}/${quest.target}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Routines & One-Tap Templates Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ROUTINES & TEMPLATES (1-TAP ADD)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showCreateRoutineDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Routine", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                items(routines, key = { it.id }) { routine ->
                    val count = routine.tasksPipeDelimited.split(";").count { it.isNotBlank() }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = routine.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${routine.description} ($count tasks)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = { onApplyRoutine(routine) },
                                modifier = Modifier.testTag("apply_routine_${routine.id}")
                            ) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add All", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        } else {
            // Badges Catalogue Tab (69 Unique Badges)
            OutlinedTextField(
                value = badgeSearchQuery,
                onValueChange = { badgeSearchQuery = it },
                placeholder = { Text("Search 69 badges by name or group...", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("badge_search_input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Rarity Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedRarity == null,
                        onClick = { selectedRarity = null },
                        label = { Text("All Rarities", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                items(BadgeRarity.entries) { rarity ->
                    FilterChip(
                        selected = selectedRarity == rarity,
                        onClick = { selectedRarity = if (selectedRarity == rarity) null else rarity },
                        label = { Text(rarity.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Group Filter Chips
            val groups = listOf("Streaks", "Volume", "Category Mastery", "Timing", "Focus", "Perfect Days", "League", "Secret")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(groups) { grp ->
                    FilterChip(
                        selected = selectedGroup == grp,
                        onClick = { selectedGroup = if (selectedGroup == grp) null else grp },
                        label = { Text(grp, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val filteredBadges = remember(resolvedBadges, selectedRarity, selectedGroup, badgeSearchQuery) {
                resolvedBadges.filter { b ->
                    val matchRarity = selectedRarity == null || b.definition.rarity == selectedRarity
                    val matchGroup = selectedGroup == null || b.definition.group.equals(selectedGroup, ignoreCase = true)
                    val matchSearch = badgeSearchQuery.isBlank() ||
                            b.definition.name.contains(badgeSearchQuery, ignoreCase = true) ||
                            b.definition.description.contains(badgeSearchQuery, ignoreCase = true)
                    matchRarity && matchGroup && matchSearch
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredBadges, key = { it.definition.id }) { badge ->
                    val def = badge.definition
                    val isHiddenSecret = def.isSecret && !badge.isUnlocked
                    val rarityColor = when (def.rarity) {
                        BadgeRarity.COMMON -> Color(0xFF64748B)
                        BadgeRarity.RARE -> Color(0xFF3B82F6)
                        BadgeRarity.EPIC -> Color(0xFF8B5CF6)
                        BadgeRarity.LEGENDARY -> XpGold
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = if (badge.isUnlocked) rarityColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedBadgeDetail = badge },
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(rarityColor.copy(alpha = if (badge.isUnlocked) 0.2f else 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (badge.isUnlocked) Icons.Default.EmojiEvents
                                        else if (def.isSecret) Icons.Default.HelpOutline
                                        else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = rarityColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isHiddenSecret) "Secret Badge ???" else def.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = def.rarity.label.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = rarityColor
                                        )
                                    }
                                    Text(
                                        text = if (isHiddenSecret) def.lockedHint else def.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${badge.progress}/${def.target}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (badge.isUnlocked) rarityColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+${def.rewardXp} XP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Badge Detail & Share Dialog
    if (selectedBadgeDetail != null) {
        val b = selectedBadgeDetail!!
        val def = b.definition
        AlertDialog(
            onDismissRequest = { selectedBadgeDetail = null },
            title = { Text(if (def.isSecret && !b.isUnlocked) "Secret Achievement" else def.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Rarity: ${def.rarity.label} • Group: ${def.group}")
                    Text(if (def.isSecret && !b.isUnlocked) def.lockedHint else def.description)
                    Text("Progress: ${b.progress} / ${def.target}")
                    Text("Reward: +${def.rewardXp} XP & +${def.rewardCoins} Coins")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "DayQuest Badge: ${def.name} (${def.rarity.label})\n${def.description}\nProgress: ${b.progress}/${def.target}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Badge"))
                        selectedBadgeDetail = null
                    }
                ) {
                    Text("Share Badge")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBadgeDetail = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Create Custom Routine Dialog
    if (showCreateRoutineDialog) {
        var rName by remember { mutableStateOf("") }
        var rDesc by remember { mutableStateOf("") }
        var rTask1 by remember { mutableStateOf("") }
        var rTask2 by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateRoutineDialog = false },
            title = { Text("Create Custom Routine", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rName,
                        onValueChange = { rName = it },
                        label = { Text("Routine Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rDesc,
                        onValueChange = { rDesc = it },
                        label = { Text("Short Description") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rTask1,
                        onValueChange = { rTask1 = it },
                        label = { Text("Task 1 Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rTask2,
                        onValueChange = { rTask2 = it },
                        label = { Text("Task 2 Title") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rName.isNotBlank() && rTask1.isNotBlank()) {
                            val pipe = buildString {
                                append("${rTask1.trim()}|Study|Medium|30")
                                if (rTask2.isNotBlank()) append(";${rTask2.trim()}|Study|Easy|15")
                            }
                            onCreateRoutine(rName.trim(), rDesc.ifBlank { "Custom routine" }, pipe)
                            showCreateRoutineDialog = false
                        }
                    }
                ) {
                    Text("Save Routine")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoutineDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
