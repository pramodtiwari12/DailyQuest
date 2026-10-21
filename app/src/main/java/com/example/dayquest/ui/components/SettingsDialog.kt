package com.example.dayquest.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dayquest.data.cloud.AuthState
import com.example.dayquest.data.model.TaskEntity
import com.example.dayquest.data.model.UserStatsEntity
import com.example.dayquest.domain.security.BackupCryptoManager
import com.example.dayquest.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class DayQuestSettings(
    val darkTheme: Boolean = false,
    val amoled: Boolean = false,
    val accentColorIndex: Int = 0,
    val dayResetHour: Int = 0
)

val ACCENT_PALETTE = listOf(
    "Indigo" to AccentIndigo,
    "Blue" to AccentBlue,
    "Teal" to AccentTeal,
    "Green" to AccentGreen,
    "Amber" to AccentAmber,
    "Orange" to AccentOrange,
    "Rose" to AccentRose,
    "Violet" to AccentViolet
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    userStats: UserStatsEntity,
    authState: AuthState,
    settings: DayQuestSettings,
    appSettings: Map<String, String>,
    allTasks: List<TaskEntity>,
    onSettingsChanged: (DayQuestSettings) -> Unit,
    onSaveSetting: (key: String, value: String) -> Unit,
    onSignInGoogle: () -> Unit,
    onSignOutGoogle: () -> Unit,
    onManualCloudSync: () -> Unit,
    onCreateEncryptedBackup: (passphrase: String, onResult: (Result<String>) -> Unit) -> Unit,
    onRestoreEncryptedBackup: (envelope: String, passphrase: String, onResult: (Result<Int>) -> Unit) -> Unit,
    onResetAllData: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var dark by remember { mutableStateOf(settings.darkTheme) }
    var amoled by remember { mutableStateOf(settings.amoled) }
    var accentIdx by remember { mutableIntStateOf(settings.accentColorIndex) }
    var resetHour by remember { mutableIntStateOf(settings.dayResetHour) }

    val penaltiesEnabled = appSettings["penalties_enabled"] != "false"
    val optInLeaderboard = appSettings["opt_in_leaderboard"] != "false"
    val publicProfile = appSettings["public_profile"] == "true"
    val reduceMotion = appSettings["reduce_motion"] == "true"
    val mlAutoEnabled = appSettings["ml_auto_categorize"] != "false"
    val notificationsEnabled = appSettings["notifications_enabled"] != "false"
    val fontScalePct = (appSettings["font_scale_pct"]?.toIntOrNull() ?: 90).coerceIn(80, 130)

    var showBackupDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var backupResultEnvelope by remember { mutableStateOf("") }
    var backupStatusText by remember { mutableStateOf("") }

    val lastSyncFormatted = remember(userStats.lastCloudSyncTime) {
        if (userStats.lastCloudSyncTime > 0) {
            SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(userStats.lastCloudSyncTime))
        } else {
            "Never synced"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("settings_screen_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DayQuestLogo(size = 26.dp, animated = !reduceMotion)
                        Text(
                            text = "Settings & Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp).testTag("close_settings_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Account & Google Cloud Synchronization
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "ACCOUNT & CLOUD SYNCHRONIZATION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            when (authState) {
                                is AuthState.Authenticated -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = authState.displayName.ifBlank { "Google Account" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = authState.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Last cloud sync: $lastSyncFormatted",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(onClick = onManualCloudSync) {
                                                Icon(Icons.Default.Sync, contentDescription = "Sync now", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            OutlinedButton(onClick = onSignOutGoogle) {
                                                Text("Sign Out", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }

                                is AuthState.Loading -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Text("Connecting with Google...", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                else -> {
                                    Text(
                                        text = "Not connected. DayQuest operates 100% offline. Sign in with Google to synchronize your authorized profile and weekly league ranking.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = onSignInGoogle,
                                        modifier = Modifier.fillMaxWidth().testTag("settings_google_signin_button")
                                    ) {
                                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Continue with Google")
                                    }
                                }
                            }
                        }
                    }

                    // 2. Appearance & Expressive Theme
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "APPEARANCE & THEME",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !dark,
                                    onClick = {
                                        dark = false
                                        amoled = false
                                        onSettingsChanged(settings.copy(darkTheme = false, amoled = false))
                                    },
                                    label = { Text("Light") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = dark && !amoled,
                                    onClick = {
                                        dark = true
                                        amoled = false
                                        onSettingsChanged(settings.copy(darkTheme = true, amoled = false))
                                    },
                                    label = { Text("Dark") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = dark && amoled,
                                    onClick = {
                                        dark = true
                                        amoled = true
                                        onSettingsChanged(settings.copy(darkTheme = true, amoled = true))
                                    },
                                    label = { Text("AMOLED") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 8 Accent Colors
                            Text("ACCENT COLOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ACCENT_PALETTE.forEachIndexed { index, (name, color) ->
                                    val isSelected = accentIdx == index
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .clickable {
                                                accentIdx = index
                                                onSettingsChanged(settings.copy(accentColorIndex = index))
                                            }
                                            .border(if (isSelected) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            .testTag("accent_color_$name"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = name, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            // Font scale slider
                            Text("Font Scale: $fontScalePct%", style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = fontScalePct.toFloat(),
                                onValueChange = { onSaveSetting("font_scale_pct", it.roundToInt().toString()) },
                                valueRange = 80f..130f,
                                steps = 4
                            )

                            // Reduce Motion
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Reduce Motion Globally", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = reduceMotion,
                                    onCheckedChange = { onSaveSetting("reduce_motion", it.toString()) }
                                )
                            }
                        }
                    }

                    // 3. Notifications & Schedule Boundary
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "NOTIFICATIONS & DAY BOUNDARY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Task & Reminder Notifications", style = MaterialTheme.typography.bodyMedium)
                                    Text("Alerts for scheduled By Time tasks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = { onSaveSetting("notifications_enabled", it.toString()) }
                                )
                            }

                            Text("DAY RESET BOUNDARY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = resetHour == 0,
                                    onClick = {
                                        resetHour = 0
                                        onSettingsChanged(settings.copy(dayResetHour = 0))
                                    },
                                    label = { Text("Midnight (00:00)") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = resetHour == 4,
                                    onClick = {
                                        resetHour = 4
                                        onSettingsChanged(settings.copy(dayResetHour = 4))
                                    },
                                    label = { Text("Night Owl (04:00)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 4. Privacy & Data Controls
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth().testTag("privacy_controls_section")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "PRIVACY & LEADERBOARD CONTROLS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Participate in Public League", style = MaterialTheme.typography.bodyMedium)
                                    Text("Show your weekly XP on genuine real-user leaderboards", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = optInLeaderboard,
                                    onCheckedChange = { onSaveSetting("opt_in_leaderboard", it.toString()) }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Public Profile Visibility", style = MaterialTheme.typography.bodyMedium)
                                    Text("Allow other players to view your avatar and level", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = publicProfile,
                                    onCheckedChange = { onSaveSetting("public_profile", it.toString()) }
                                )
                            }
                        }
                    }

                    // 5. Gamification & ML Options
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "TASK & GAMIFICATION DEFAULTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Missed By Time Penalty (-10 XP)", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = penaltiesEnabled,
                                    onCheckedChange = { onSaveSetting("penalties_enabled", it.toString()) }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("On-Device Auto Categorization", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = mlAutoEnabled,
                                    onCheckedChange = { onSaveSetting("ml_auto_categorize", it.toString()) }
                                )
                            }
                        }
                    }

                    // 6. Data Export & AES Backup
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "DATA EXPORT & BACKUP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val csv = BackupCryptoManager.exportTasksToCsv(allTasks)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(Intent.EXTRA_TEXT, csv)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Export Tasks CSV"))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export CSV", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = { showBackupDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AES-GCM Backup", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete All Local Data", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // About / Language Notice
                    Text(
                        text = "DayQuest v1.0 • English-Only Interface • 100% Real Registered Data • No Bots",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

    // Encrypted Backup Modal
    if (showBackupDialog) {
        var passphrase by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("AES-256-GCM Encrypted Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Passphrase (min 4 chars)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = backupResultEnvelope,
                        onValueChange = { backupResultEnvelope = it },
                        label = { Text("Encrypted Envelope JSON") },
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    if (backupStatusText.isNotBlank()) {
                        Text(backupStatusText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = {
                            onCreateEncryptedBackup(passphrase) { res ->
                                res.onSuccess { env ->
                                    backupResultEnvelope = env
                                    backupStatusText = "Encrypted successfully with AES-256-GCM"
                                }.onFailure { err ->
                                    backupStatusText = "Error: ${err.message}"
                                }
                            }
                        }
                    ) {
                        Text("Encrypt")
                    }
                    Button(
                        onClick = {
                            onRestoreEncryptedBackup(backupResultEnvelope, passphrase) { res ->
                                res.onSuccess { count ->
                                    backupStatusText = "Restored $count tasks successfully!"
                                }.onFailure { err ->
                                    backupStatusText = "Restore failed: ${err.message}"
                                }
                            }
                        }
                    ) {
                        Text("Restore")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Close") }
            }
        )
    }

    // Delete Confirmation
    if (showDeleteConfirmDialog) {
        var confirmText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete All Data", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type DELETE in uppercase to permanently erase all local data.")
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        label = { Text("Type DELETE") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (confirmText == "DELETE") {
                            onResetAllData()
                            showDeleteConfirmDialog = false
                            onDismiss()
                        }
                    },
                    enabled = confirmText == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Erase Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}
