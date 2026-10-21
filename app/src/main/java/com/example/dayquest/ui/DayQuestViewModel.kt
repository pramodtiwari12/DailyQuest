package com.example.dayquest.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dayquest.data.cloud.AuthState
import com.example.dayquest.data.cloud.FirebaseAuthManager
import com.example.dayquest.data.cloud.FirestoreSyncManager
import com.example.dayquest.data.db.DayQuestDatabase
import com.example.dayquest.data.model.*
import com.example.dayquest.data.repository.DayQuestRepository
import com.example.dayquest.domain.badges.ResolvedBadge
import com.example.dayquest.domain.league.CohortZone
import com.example.dayquest.domain.league.LeaderboardEntry
import com.example.dayquest.domain.league.LeagueEngine
import com.example.dayquest.domain.league.LeagueTier
import com.example.dayquest.domain.ml.MlCorpusEvaluation
import com.example.dayquest.domain.ml.MlPrediction
import com.example.dayquest.domain.ml.OnDeviceTaskClassifier
import com.example.dayquest.domain.security.BackupCryptoManager
import com.example.dayquest.domain.time.NaturalTimeParser
import com.example.dayquest.domain.time.TimeUtils
import com.example.dayquest.domain.xp.LevelProgress
import com.example.dayquest.domain.xp.XpCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TodayTasksGroup(
    val byTimeTasks: List<TaskEntity> = emptyList(),
    val anytimeTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val overdueTasks: List<TaskEntity> = emptyList()
)

class DayQuestViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DayQuestRepository
    val authManager: FirebaseAuthManager = FirebaseAuthManager(application)
    private val syncManager: FirestoreSyncManager = FirestoreSyncManager(application)

    val authState: StateFlow<AuthState> = authManager.authState

    private val _realLeaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val realLeaderboard: StateFlow<List<LeaderboardEntry>> = _realLeaderboard.asStateFlow()

    private val _isLoadingCloudRankings = MutableStateFlow(false)
    val isLoadingCloudRankings: StateFlow<Boolean> = _isLoadingCloudRankings.asStateFlow()

    private val _cloudSyncError = MutableStateFlow<String?>(null)
    val cloudSyncError: StateFlow<String?> = _cloudSyncError.asStateFlow()

    private val _corpusEvaluation = MutableStateFlow<MlCorpusEvaluation?>(null)
    val corpusEvaluation: StateFlow<MlCorpusEvaluation?> = _corpusEvaluation.asStateFlow()

    // Temporary task draft to avoid accidental loss
    var activeTaskDraft: TaskEntity? = null

    init {
        val db = DayQuestDatabase.getInstance(application)
        repository = DayQuestRepository(db)

        viewModelScope.launch(Dispatchers.IO) {
            OnDeviceTaskClassifier.loadSeedDatasetFromAssets(application)
            _corpusEvaluation.value = OnDeviceTaskClassifier.evaluateSeedCorpus()
            repository.ensureDefaultsSeeded(TimeUtils.todayDateKey())
        }

        // Listen for Auth changes and sync if signed in
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    repository.updateGoogleAuth(state.email, state.user.uid)
                    syncCloudData()
                    refreshLeaderboard()
                }
            }
        }
    }

    private val _selectedDateKey = MutableStateFlow(TimeUtils.todayDateKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    val userStats: StateFlow<UserStatsEntity> = repository.getUserStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatsEntity())

    val levelProgress: StateFlow<LevelProgress> = userStats
        .map { XpCalculator.getLevelProgress(it.totalXp) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), XpCalculator.getLevelProgress(0))

    val allTasks: StateFlow<List<TaskEntity>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val xpLogs: StateFlow<List<XpLogEntity>> = repository.getAllXpLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryProgress: StateFlow<List<CategoryProgressEntity>> = repository.getAllCategoryProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyQuests: StateFlow<List<QuestEntity>> = _selectedDateKey
        .flatMapLatest { dateKey -> repository.getQuestsForDate(dateKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routines: StateFlow<List<RoutineTemplateEntity>> = repository.getAllRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusSessions: StateFlow<List<FocusSessionEntity>> = repository.getAllFocusSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val resolvedBadges: StateFlow<List<ResolvedBadge>> = repository.getResolvedBadgesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friends: StateFlow<List<FriendEntity>> = repository.getAllFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storePurchases: StateFlow<List<StorePurchaseEntity>> = repository.getAllStorePurchases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mlCorrections: StateFlow<List<MlCorrectionEntity>> = repository.getAllMlCorrections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<Map<String, String>> = repository.getAllSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val rawTasksForSelectedDate: StateFlow<List<TaskEntity>> = _selectedDateKey
        .flatMapLatest { dateKey -> repository.getTasksForDate(dateKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedTasks: StateFlow<TodayTasksGroup> = combine(
        rawTasksForSelectedDate,
        _searchQuery,
        _categoryFilter
    ) { tasks, query, catFilter ->
        val now = System.currentTimeMillis()
        val filtered = tasks.filter { task ->
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    (task.notes?.contains(query, ignoreCase = true) == true)
            val matchesCat = catFilter == null || task.categoryId.equals(catFilter, ignoreCase = true)
            matchesQuery && matchesCat
        }

        val completed = filtered.filter { it.status == "done" }
        val pending = filtered.filter { it.status != "done" }

        val overdue = pending.filter { it.dueAt != null && it.dueAt < now }
        val activePending = pending.filter { it !in overdue }

        val byTime = activePending.filter { it.scheduleType == "byTime" || it.scheduleType == "range" }
            .sortedBy { it.dueAt ?: Long.MAX_VALUE }
        val anytime = activePending.filter { it.scheduleType != "byTime" && it.scheduleType != "range" }

        TodayTasksGroup(
            byTimeTasks = byTime,
            anytimeTasks = anytime,
            completedTasks = completed,
            overdueTasks = overdue
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayTasksGroup())

    var lastCompletedTask: TaskEntity? = null
        private set
    var lastDeletedTask: TaskEntity? = null
        private set

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _levelUpCelebration = MutableStateFlow<Int?>(null)
    val levelUpCelebration: StateFlow<Int?> = _levelUpCelebration.asStateFlow()

    fun dismissLevelUpCelebration() {
        _levelUpCelebration.value = null
    }

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
        viewModelScope.launch {
            repository.ensureDefaultsSeeded(dateKey)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(categoryId: String?) {
        _categoryFilter.value = if (_categoryFilter.value == categoryId) null else categoryId
    }

    fun predictTaskWithMl(title: String): MlPrediction {
        val autoEnabled = appSettings.value["ml_auto_categorize"] != "false"
        return OnDeviceTaskClassifier.predict(
            taskTitle = title,
            userCorrections = mlCorrections.value,
            autoCategorizationEnabled = autoEnabled
        )
    }

    fun toggleTaskComplete(task: TaskEntity) {
        viewModelScope.launch {
            if (task.status == "done") {
                repository.undoCompleteTask(task)
                _snackbarMessage.emit("Reopened: ${task.title}")
            } else {
                val previousLevel = userStats.value.currentLevel
                val completed = repository.completeTask(task)
                lastCompletedTask = completed
                val newLevel = repository.getUserStatsSync().currentLevel
                if (newLevel > previousLevel && appSettings.value["level_celebrations"] != "false") {
                    _levelUpCelebration.value = newLevel
                }
                val xpMsg = if (completed.xpAwarded > 0) "+${completed.xpAwarded} XP awarded" else "0 XP (duplicate or daily cap)"
                _snackbarMessage.emit("Completed! $xpMsg")
            }
            // Auto sync eligible progress to cloud if signed in
            syncCloudData()
        }
    }

    fun undoLastCompletion() {
        val taskToUndo = lastCompletedTask ?: return
        viewModelScope.launch {
            repository.undoCompleteTask(taskToUndo)
            lastCompletedTask = null
            _snackbarMessage.emit("Undid completion for: ${taskToUndo.title}")
            syncCloudData()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            lastDeletedTask = task
            _snackbarMessage.emit("Deleted task: ${task.title}")
        }
    }

    fun undoDelete() {
        val taskToRestore = lastDeletedTask ?: return
        viewModelScope.launch {
            repository.insertTask(taskToRestore)
            lastDeletedTask = null
            _snackbarMessage.emit("Restored task: ${taskToRestore.title}")
        }
    }

    fun quickAddTask(
        rawText: String,
        confirmedCategoryOverride: String? = null,
        targetDateKey: String = _selectedDateKey.value,
        explicitDueAt: Long? = null
    ): Boolean {
        if (rawText.isBlank()) return false
        val parsed = NaturalTimeParser.parse(rawText)
        val mlPred = predictTaskWithMl(parsed.cleanedTitle)

        val dateKey = if (explicitDueAt != null) {
            targetDateKey
        } else if (parsed.dateOffsetDays > 0) {
            TimeUtils.dateKeyForOffset(parsed.dateOffsetDays)
        } else {
            targetDateKey
        }

        var dueAt: Long? = explicitDueAt
        if (dueAt == null && parsed.scheduleType == ScheduleType.BY_TIME && parsed.targetTime24h != null) {
            val parts = parsed.targetTime24h.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 18
                val min = parts[1].toIntOrNull() ?: 0
                dueAt = TimeUtils.combineDateAndHourMinute(dateKey, hour, min)
            }
        }

        val finalCategory = confirmedCategoryOverride ?: if (mlPred.confidence >= 0.40f) mlPred.topCategoryId else parsed.suggestedCategoryId
        val finalDuration = parsed.durationMinutes ?: mlPred.suggestedDurationMinutes
        val finalDiff = mlPred.suggestedDifficulty

        val task = TaskEntity(
            title = parsed.cleanedTitle.take(120),
            categoryId = finalCategory,
            categorySource = if (confirmedCategoryOverride != null) "user" else "auto",
            difficulty = finalDiff,
            estMinutes = finalDuration,
            scheduleType = if (dueAt != null) "byTime" else "anytime",
            dueAt = dueAt,
            localDateKey = dateKey
        )

        viewModelScope.launch {
            repository.insertTask(task)
            _snackbarMessage.emit("Added quest: ${task.title}")
        }
        return true
    }

    fun addTask(task: TaskEntity, subtaskTitles: List<String>) {
        viewModelScope.launch {
            repository.insertTask(task)
            for (title in subtaskTitles) {
                repository.addSubtask(task.id, title)
            }
            activeTaskDraft = null
            _snackbarMessage.emit("Created: ${task.title}")
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
            activeTaskDraft = null
            _snackbarMessage.emit("Updated: ${task.title}")
        }
    }

    fun rescheduleTask(task: TaskEntity, newDateKey: String) {
        viewModelScope.launch {
            repository.rescheduleTask(task, newDateKey)
            _snackbarMessage.emit("Rescheduled to $newDateKey")
        }
    }

    fun getSubtasks(taskId: String): Flow<List<SubtaskEntity>> = repository.getSubtasksForTask(taskId)

    fun addSubtask(taskId: String, title: String) {
        viewModelScope.launch {
            repository.addSubtask(taskId, title)
        }
    }

    fun toggleSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            repository.toggleSubtask(subtask)
        }
    }

    fun deleteSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            repository.deleteSubtask(subtask)
        }
    }

    fun recordFocusTimerCompleted(task: TaskEntity?, durationSeconds: Int, completeTask: Boolean) {
        viewModelScope.launch {
            repository.recordFocusSession(task, durationSeconds, completeTask)
            val mins = durationSeconds / 60
            _snackbarMessage.emit("Focus session completed ($mins mins)!")
            syncCloudData()
        }
    }

    fun applyRoutine(routine: RoutineTemplateEntity) {
        viewModelScope.launch {
            val count = repository.applyRoutineTemplate(routine, _selectedDateKey.value)
            _snackbarMessage.emit("Added $count tasks from ${routine.name}!")
        }
    }

    fun createRoutine(name: String, desc: String, pipeDelimited: String) {
        viewModelScope.launch {
            repository.createCustomRoutine(name, desc, pipeDelimited)
            _snackbarMessage.emit("Created Routine template: $name")
        }
    }

    fun buyStreakFreeze() {
        viewModelScope.launch {
            val ok = repository.purchaseStreakFreeze()
            if (ok) {
                _snackbarMessage.emit("Purchased Streak Freeze (-100 Coins)!")
            } else {
                _snackbarMessage.emit("Cannot purchase: Max 2 freezes or insufficient coins.")
            }
        }
    }

    fun buyStoreItem(itemId: String, itemType: String, costCoins: Int) {
        viewModelScope.launch {
            val ok = repository.purchaseStoreItem(itemId, itemType, costCoins)
            if (ok) {
                _snackbarMessage.emit("Unlocked $itemId!")
            } else {
                _snackbarMessage.emit("Insufficient coins ($costCoins required).")
            }
        }
    }

    fun updateProfile(name: String, avatarId: String, bio: String, occupation: String) {
        viewModelScope.launch {
            val optIn = appSettings.value["opt_in_leaderboard"] != "false"
            val publicProf = appSettings.value["public_profile"] == "true"
            repository.updateProfileFull(name, avatarId, bio, occupation, optIn, publicProf)
            _snackbarMessage.emit("Profile updated!")
            syncCloudData()
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _cloudSyncError.value = null
            val result = authManager.signInWithGoogle()
            result.onSuccess { user ->
                repository.updateGoogleAuth(user.email, user.uid)
                _snackbarMessage.emit("Signed in as ${user.email}!")
                syncCloudData()
                refreshLeaderboard()
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Sign in failed"
                _cloudSyncError.value = msg
                _snackbarMessage.emit(msg)
            }
        }
    }

    fun signOutGoogle() {
        authManager.signOut()
        viewModelScope.launch {
            repository.updateGoogleAuth(null, null)
            _realLeaderboard.value = emptyList()
            _snackbarMessage.emit("Signed out of Google.")
        }
    }

    fun syncCloudData() {
        val auth = authState.value
        if (auth !is AuthState.Authenticated) return

        viewModelScope.launch(Dispatchers.IO) {
            val stats = repository.getUserStatsSync()
            val res = syncManager.syncUserToLeaderboard(auth.user.uid, stats)
            res.onSuccess {
                repository.updateLastCloudSyncTime(System.currentTimeMillis())
            }.onFailure { err ->
                _cloudSyncError.value = "Sync notice: ${err.message}"
            }
        }
    }

    fun refreshLeaderboard() {
        val auth = authState.value
        val currentUid = if (auth is AuthState.Authenticated) auth.user.uid else null

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingCloudRankings.value = true
            _cloudSyncError.value = null
            val stats = repository.getUserStatsSync()
            val currentTier = LeagueTier.fromString(stats.leagueTier)

            val fetchRes = syncManager.fetchRealLeaderboard(
                leagueTier = currentTier.displayName,
                currentUid = currentUid
            )

            fetchRes.onSuccess { remoteEntries ->
                // Build real-only leaderboard without any synthetic bots
                val userEntry = if (stats.optInLeaderboard && stats.weeklyLeagueXp > 0) {
                    LeaderboardEntry(
                        id = currentUid ?: "local_user",
                        rank = 1,
                        displayName = "${stats.displayName} (You)",
                        weeklyXp = stats.weeklyLeagueXp,
                        isCurrentUser = true,
                        zone = CohortZone.SAFE,
                        avatarId = stats.avatarId,
                        occupation = stats.occupation
                    )
                } else null

                val processed = LeagueEngine.buildRealLeaderboard(
                    tier = currentTier,
                    currentUserEntry = userEntry,
                    otherRealUsers = remoteEntries
                )
                _realLeaderboard.value = processed
            }.onFailure { err ->
                _cloudSyncError.value = "Unable to fetch online rankings: ${err.message}"
            }
            _isLoadingCloudRankings.value = false
        }
    }

    fun addFriend(name: String, code: String) {
        viewModelScope.launch {
            repository.addFriend(name, code)
            _snackbarMessage.emit("Friend added: $name")
        }
    }

    fun claimChallengeReward(friend: FriendEntity) {
        viewModelScope.launch {
            val ok = repository.claimFriendChallengeReward(friend)
            if (ok) {
                _snackbarMessage.emit("1v1 Challenge Won! +50 XP & +50 Coins!")
            } else {
                _snackbarMessage.emit("Earn more weekly XP than ${friend.displayName} to claim!")
            }
        }
    }

    fun saveSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
            if (key == "opt_in_leaderboard" || key == "public_profile") {
                val stats = repository.getUserStatsSync()
                repository.updateProfileFull(
                    stats.displayName,
                    stats.avatarId,
                    stats.bio,
                    stats.occupation,
                    optInLeaderboard = value != "false",
                    publicProfile = stats.publicProfile
                )
                syncCloudData()
            }
        }
    }

    fun clearMlPersonalization() {
        viewModelScope.launch {
            repository.clearMlCorrections()
            _snackbarMessage.emit("Cleared personal ML correction history.")
        }
    }

    fun createEncryptedBackupPayload(passphrase: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tasks = repository.getAllTasksSync()
                val json = BackupCryptoManager.exportTasksToJson(tasks)
                val encrypted = BackupCryptoManager.encryptBackup(json, passphrase)
                repository.saveSetting("last_backup_sha256", BackupCryptoManager.sha256Hex(json).take(12))
                onResult(Result.success(encrypted))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun restoreFromEncryptedBackup(envelope: String, passphrase: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val decryptedJson = BackupCryptoManager.decryptBackup(envelope, passphrase)
                val tasks = BackupCryptoManager.importTasksFromJson(decryptedJson)
                repository.importTasks(tasks)
                onResult(Result.success(tasks.size))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.deleteAllUserData()
            repository.ensureDefaultsSeeded(TimeUtils.todayDateKey())
            _realLeaderboard.value = emptyList()
            _snackbarMessage.emit("All local data has been reset.")
        }
    }
}
