package com.example.dayquest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dayquest.data.model.TaskEntity
import com.example.dayquest.domain.time.TimeUtils
import com.example.dayquest.ui.components.*
import com.example.dayquest.ui.screens.*
import com.example.dayquest.ui.theme.DayQuestTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayQuestApp(
    viewModel: DayQuestViewModel = viewModel()
) {
    var settings by remember { mutableStateOf(DayQuestSettings()) }
    val currentAccent = ACCENT_PALETTE.getOrNull(settings.accentColorIndex)?.second ?: ACCENT_PALETTE[0].second

    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
    val groupedTasks by viewModel.groupedTasks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val userStats by viewModel.userStats.collectAsStateWithLifecycle()
    val levelProgress by viewModel.levelProgress.collectAsStateWithLifecycle()

    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val xpLogs by viewModel.xpLogs.collectAsStateWithLifecycle()
    val categoryProgress by viewModel.categoryProgress.collectAsStateWithLifecycle()
    val dailyQuests by viewModel.dailyQuests.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val resolvedBadges by viewModel.resolvedBadges.collectAsStateWithLifecycle()
    val mlCorrections by viewModel.mlCorrections.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val levelUpCelebration by viewModel.levelUpCelebration.collectAsStateWithLifecycle()

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val realLeaderboardEntries by viewModel.realLeaderboard.collectAsStateWithLifecycle()
    val isLoadingCloudRankings by viewModel.isLoadingCloudRankings.collectAsStateWithLifecycle()
    val cloudSyncError by viewModel.cloudSyncError.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var taskForDetail by remember { mutableStateOf<TaskEntity?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var currentNavDestination by remember { mutableStateOf("today") }

    // BackHandler on secondary destinations
    BackHandler(enabled = currentNavDestination != "today") {
        currentNavDestination = "today"
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val autoCategorizeEnabled = appSettings["ml_auto_categorize"] != "false"
    val reduceMotion = appSettings["reduce_motion"] == "true"

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = if (viewModel.lastCompletedTask != null || viewModel.lastDeletedTask != null) "Undo" else null,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    if (viewModel.lastCompletedTask != null) {
                        viewModel.undoLastCompletion()
                    } else if (viewModel.lastDeletedTask != null) {
                        viewModel.undoDelete()
                    }
                }
            }
        }
    }

    DayQuestTheme(
        darkTheme = settings.darkTheme,
        accentColor = currentAccent,
        amoled = settings.amoled
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dayquest_scaffold"),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.testTag("snackbar_host")
                )
            },
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DayQuestLogo(size = 28.dp, animated = !reduceMotion)
                                Text(
                                    text = "DayQuest",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "v1.0",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Application Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    XpHeaderBar(
                        userStats = userStats,
                        levelProgress = levelProgress
                    )

                    if (currentNavDestination == "today") {
                        WeekCalendarStrip(
                            selectedDateKey = selectedDateKey,
                            onDateSelected = { dateKey ->
                                viewModel.selectDate(dateKey)
                            }
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("dayquest_navigation_bar"),
                    tonalElevation = 2.dp
                ) {
                    NavigationBarItem(
                        selected = currentNavDestination == "today",
                        onClick = { currentNavDestination = "today" },
                        icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                        label = { Text("Today", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_today")
                    )
                    NavigationBarItem(
                        selected = currentNavDestination == "stats",
                        onClick = { currentNavDestination = "stats" },
                        icon = { Icon(Icons.Outlined.BarChart, contentDescription = "Stats") },
                        label = { Text("Stats", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_stats")
                    )
                    NavigationBarItem(
                        selected = currentNavDestination == "quests",
                        onClick = { currentNavDestination = "quests" },
                        icon = { Icon(Icons.Outlined.MilitaryTech, contentDescription = "Quests") },
                        label = { Text("Quests", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_quests")
                    )
                    NavigationBarItem(
                        selected = currentNavDestination == "league",
                        onClick = {
                            currentNavDestination = "league"
                            viewModel.refreshLeaderboard()
                        },
                        icon = { Icon(Icons.Outlined.Leaderboard, contentDescription = "League") },
                        label = { Text("League", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_league")
                    )
                    NavigationBarItem(
                        selected = currentNavDestination == "profile",
                        onClick = { currentNavDestination = "profile" },
                        icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
                        label = { Text("Profile", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        ) { paddingValues ->
            when (currentNavDestination) {
                "today" -> {
                    TodayScreen(
                        selectedDateKey = selectedDateKey,
                        groupedTasks = groupedTasks,
                        searchQuery = searchQuery,
                        categoryFilter = categoryFilter,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                        onToggleComplete = { task -> viewModel.toggleTaskComplete(task) },
                        onTaskClick = { task -> taskForDetail = task },
                        onDeleteTask = { task -> viewModel.deleteTask(task) },
                        onQuickAdd = { text, catOverride, dateKey, dueAt ->
                            viewModel.quickAddTask(text, catOverride, dateKey, dueAt)
                        },
                        onOpenFullAddSheet = {
                            taskToEdit = null
                            showAddSheet = true
                        },
                        onRescheduleTask = { task, newDate ->
                            viewModel.rescheduleTask(task, newDate)
                        },
                        userCorrections = mlCorrections,
                        autoCategorizeEnabled = autoCategorizeEnabled,
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                "stats" -> {
                    StatsScreen(
                        allTasks = allTasks,
                        xpLogs = xpLogs,
                        categoryProgress = categoryProgress,
                        userStats = userStats,
                        onDrillDownDate = { dateKey ->
                            viewModel.selectDate(dateKey)
                            currentNavDestination = "today"
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                "quests" -> {
                    QuestsBadgesScreen(
                        dailyQuests = dailyQuests,
                        routines = routines,
                        resolvedBadges = resolvedBadges,
                        onApplyRoutine = { routine ->
                            viewModel.applyRoutine(routine)
                            currentNavDestination = "today"
                        },
                        onCreateRoutine = { name, desc, pipe ->
                            viewModel.createRoutine(name, desc, pipe)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                "league" -> {
                    LeagueScreen(
                        userStats = userStats,
                        authState = authState,
                        realLeaderboardEntries = realLeaderboardEntries,
                        isLoadingCloudRankings = isLoadingCloudRankings,
                        cloudSyncError = cloudSyncError,
                        onSignInGoogle = { viewModel.signInWithGoogle() },
                        onRefreshLeaderboard = { viewModel.refreshLeaderboard() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                "profile" -> {
                    ProfileSettingsScreen(
                        userStats = userStats,
                        levelProgress = levelProgress,
                        resolvedBadges = resolvedBadges,
                        authState = authState,
                        onUpdateProfile = { name, avatarId, bio, occupation ->
                            viewModel.updateProfile(name, avatarId, bio, occupation)
                        },
                        onSignInGoogle = { viewModel.signInWithGoogle() },
                        onOpenSettings = { showSettingsDialog = true },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }

            if (showAddSheet) {
                AddEditTaskBottomSheet(
                    initialTask = taskToEdit,
                    selectedDateKey = selectedDateKey,
                    onSave = { task, subtasks ->
                        if (taskToEdit != null) {
                            viewModel.updateTask(task)
                        } else {
                            viewModel.addTask(task, subtasks)
                        }
                        showAddSheet = false
                        taskToEdit = null
                    },
                    onDismiss = {
                        showAddSheet = false
                        taskToEdit = null
                    }
                )
            }

            if (taskForDetail != null) {
                val detailTask = taskForDetail!!
                TaskDetailSheet(
                    task = detailTask,
                    subtasksFlow = viewModel.getSubtasks(detailTask.id),
                    onToggleComplete = { viewModel.toggleTaskComplete(detailTask) },
                    onEdit = {
                        taskToEdit = detailTask
                        showAddSheet = true
                    },
                    onDelete = { viewModel.deleteTask(detailTask) },
                    onRescheduleTomorrow = {
                        viewModel.rescheduleTask(detailTask, TimeUtils.dateKeyForOffset(1))
                    },
                    onToggleSubtask = { subtask -> viewModel.toggleSubtask(subtask) },
                    onAddSubtask = { title -> viewModel.addSubtask(detailTask.id, title) },
                    onDeleteSubtask = { subtask -> viewModel.deleteSubtask(subtask) },
                    onFocusSessionCompleted = { durationSeconds, completeTask ->
                        viewModel.recordFocusTimerCompleted(detailTask, durationSeconds, completeTask)
                    },
                    onDismiss = { taskForDetail = null }
                )
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    userStats = userStats,
                    authState = authState,
                    settings = settings,
                    appSettings = appSettings,
                    allTasks = allTasks,
                    onSettingsChanged = { newSettings ->
                        settings = newSettings
                    },
                    onSaveSetting = { k, v -> viewModel.saveSetting(k, v) },
                    onSignInGoogle = { viewModel.signInWithGoogle() },
                    onSignOutGoogle = { viewModel.signOutGoogle() },
                    onManualCloudSync = { viewModel.syncCloudData() },
                    onCreateEncryptedBackup = { pass, cb -> viewModel.createEncryptedBackupPayload(pass, cb) },
                    onRestoreEncryptedBackup = { env, pass, cb -> viewModel.restoreFromEncryptedBackup(env, pass, cb) },
                    onResetAllData = { viewModel.resetAllData() },
                    onDismiss = { showSettingsDialog = false }
                )
            }

            // Level-Up Celebration Dialog (vector icon, no emoji)
            if (levelUpCelebration != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissLevelUpCelebration() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "LEVEL UP! LEVEL $levelUpCelebration",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    text = {
                        Text("Congratulations! You reached Level $levelUpCelebration (${levelProgress.title}). Keep building your daily momentum!")
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.dismissLevelUpCelebration() }) {
                            Text("Awesome!")
                        }
                    }
                )
            }
        }
    }
}
