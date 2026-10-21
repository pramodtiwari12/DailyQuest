package com.example.dayquest.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayquest.data.cloud.AuthState
import com.example.dayquest.data.model.UserStatsEntity
import com.example.dayquest.domain.league.CohortZone
import com.example.dayquest.domain.league.LeaderboardEntry
import com.example.dayquest.domain.league.LeagueEngine
import com.example.dayquest.domain.league.LeagueTier
import com.example.dayquest.ui.components.DayQuestAvatar
import com.example.dayquest.ui.components.DayQuestLogo
import com.example.dayquest.ui.theme.CategoryHealth
import com.example.dayquest.ui.theme.OverdueAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LeagueScreen(
    userStats: UserStatsEntity,
    authState: AuthState,
    realLeaderboardEntries: List<LeaderboardEntry>,
    isLoadingCloudRankings: Boolean,
    cloudSyncError: String?,
    onSignInGoogle: () -> Unit,
    onRefreshLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTier = LeagueTier.fromString(userStats.leagueTier)
    var selectedOccupationFilter by remember { mutableStateOf("All") }

    val segments = listOf(
        "All",
        "Software / Engineering",
        "Academics / Student",
        "Medicine / Healthcare",
        "Creative / Arts",
        "Business / Finance"
    )

    val filteredEntries = remember(realLeaderboardEntries, selectedOccupationFilter) {
        if (selectedOccupationFilter == "All") {
            realLeaderboardEntries
        } else {
            realLeaderboardEntries.filter { it.occupation.equals(selectedOccupationFilter, ignoreCase = true) }
        }
    }

    val lastSyncFormatted = remember(userStats.lastCloudSyncTime) {
        if (userStats.lastCloudSyncTime > 0) {
            SimpleDateFormat("HH:mm", Locale.US).format(Date(userStats.lastCloudSyncTime))
        } else {
            "Not synced"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("league_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // When Signed Out: Show polished Sign-In card with Continue with Google
        if (authState !is AuthState.Authenticated) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .testTag("league_signin_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DayQuestLogo(size = 48.dp)

                    Text(
                        text = "Join the Global League",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Sign in with Google to participate in real-time weekly leaderboards with genuine registered adventurers. No bots, no fabricated scores.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Your personal notes, tasks, and habits remain 100% private and on-device.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onSignInGoogle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("league_continue_google_button")
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue with Google", style = MaterialTheme.typography.labelLarge)
                    }

                    if (authState is AuthState.Error) {
                        Text(
                            text = authState.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Honest preview of league tiers
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LEAGUE TIERS (REAL COMPETITORS ONLY)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tiers: Bronze -> Silver -> Gold -> Platinum -> Diamond -> Master -> Legend.\nRankings reset weekly on Monday at 00:00.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        // When Signed In: Real Registered Users Leaderboard
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "League",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "${currentTier.displayName.uppercase()} LEAGUE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = LeagueEngine.getCanonicalResetSummary(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefreshLeaderboard,
                        modifier = Modifier.testTag("refresh_leaderboard_button")
                    ) {
                        if (isLoadingCloudRankings) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progression Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LeagueTier.entries.forEach { t ->
                        val isCur = t == currentTier
                        Text(
                            text = t.displayName.take(3).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isCur) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (userStats.optInLeaderboard) "Active in Rankings • Synced at $lastSyncFormatted • Your Weekly XP: ${userStats.weeklyLeagueXp} XP"
                    else "Private Mode Active • Hidden from public leaderboard",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Occupation / Segment Selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(segments) { seg ->
                val isSelected = selectedOccupationFilter == seg
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedOccupationFilter = seg },
                    label = { Text(seg, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (cloudSyncError != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = cloudSyncError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Leaderboard List
        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No other registered users in this segment yet",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "As more users register with Google and earn weekly XP, real verified rankings will appear here automatically. You currently have ${userStats.weeklyLeagueXp} Weekly XP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("real_leaderboard_list"),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredEntries, key = { it.id }) { entry ->
                    val zoneColor = when (entry.zone) {
                        CohortZone.PROMOTION -> CategoryHealth
                        CohortZone.DEMOTION -> OverdueAmber
                        CohortZone.SAFE -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (entry.isCurrentUser) 2.dp else 1.dp,
                                color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "#${entry.rank}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = zoneColor,
                                    modifier = Modifier.width(28.dp)
                                )
                                DayQuestAvatar(avatarId = entry.avatarId, size = 32.dp, showRing = false)
                                Column {
                                    Text(
                                        text = entry.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = entry.occupation,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "${entry.weeklyXp} XP",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
