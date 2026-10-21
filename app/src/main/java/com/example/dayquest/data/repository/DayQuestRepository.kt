package com.example.dayquest.data.repository

import com.example.dayquest.data.db.DayQuestDatabase
import com.example.dayquest.data.model.*
import com.example.dayquest.domain.badges.BadgeCatalogue
import com.example.dayquest.domain.badges.ResolvedBadge
import com.example.dayquest.domain.ml.OnDeviceTaskClassifier
import com.example.dayquest.domain.time.TimeUtils
import com.example.dayquest.domain.xp.XpCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

class DayQuestRepository(private val database: DayQuestDatabase) {
    private val taskDao = database.taskDao()
    private val subtaskDao = database.subtaskDao()
    private val xpLogDao = database.xpLogDao()
    private val userStatsDao = database.userStatsDao()
    private val categoryProgressDao = database.categoryProgressDao()
    private val badgeDao = database.badgeDao()
    private val mlCorrectionDao = database.mlCorrectionDao()
    private val questDao = database.questDao()
    private val routineDao = database.routineDao()
    private val focusSessionDao = database.focusSessionDao()
    private val friendDao = database.friendDao()
    private val storePurchaseDao = database.storePurchaseDao()
    private val appSettingDao = database.appSettingDao()

    fun getTasksForDate(dateKey: String): Flow<List<TaskEntity>> = taskDao.getTasksForDate(dateKey)
    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()
    suspend fun getAllTasksSync(): List<TaskEntity> = taskDao.getAllTasksSync()
    suspend fun getTaskById(id: String): TaskEntity? = taskDao.getTaskById(id)
    fun getSubtasksForTask(taskId: String): Flow<List<SubtaskEntity>> = subtaskDao.getSubtasksForTask(taskId)

    fun getUserStats(): Flow<UserStatsEntity> = userStatsDao.getUserStats().map { it ?: UserStatsEntity() }
    suspend fun getUserStatsSync(): UserStatsEntity = userStatsDao.getUserStatsSync() ?: UserStatsEntity()

    fun getAllXpLogs(): Flow<List<XpLogEntity>> = xpLogDao.getAllLogs()
    fun getAllCategoryProgress(): Flow<List<CategoryProgressEntity>> = categoryProgressDao.getAllCategoryProgress()
    fun getQuestsForDate(dateKey: String): Flow<List<QuestEntity>> = questDao.getQuestsForDate(dateKey)
    fun getAllRoutines(): Flow<List<RoutineTemplateEntity>> = routineDao.getAllRoutines()
    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>> = focusSessionDao.getAllSessions()
    fun getAllFriends(): Flow<List<FriendEntity>> = friendDao.getAllFriends()
    fun getAllStorePurchases(): Flow<List<StorePurchaseEntity>> = storePurchaseDao.getAllPurchases()
    fun getAllMlCorrections(): Flow<List<MlCorrectionEntity>> = mlCorrectionDao.getAllCorrections()
    fun getAllSettings(): Flow<Map<String, String>> = appSettingDao.getAllSettings().map { list ->
        list.associate { it.key to it.value }
    }

    fun getResolvedBadgesFlow(): Flow<List<ResolvedBadge>> {
        return combine(
            getUserStats(),
            getAllCategoryProgress(),
            badgeDao.getAllBadges(),
            mlCorrectionDao.getAllCorrections()
        ) { stats, catProg, storedBadges, mlCorrs ->
            BadgeCatalogue.evaluateAll(
                stats = stats,
                categoryProgress = catProg,
                storedBadges = storedBadges,
                mlCorrectionsCount = mlCorrs.size
            )
        }
    }

    suspend fun ensureDefaultsSeeded(todayKey: String) {
        // Ensure 3 deterministic daily quests exist for todayKey
        val existingQuests = questDao.getQuestsForDateSync(todayKey)
        if (existingQuests.isEmpty()) {
            val defaultQuests = listOf(
                QuestEntity(
                    id = "quest_${todayKey}_1",
                    localDateKey = todayKey,
                    type = "any_tasks",
                    title = "Daily Momentum",
                    description = "Complete 2 planned tasks today",
                    target = 2,
                    progress = 0,
                    rewardXp = 35
                ),
                QuestEntity(
                    id = "quest_${todayKey}_2",
                    localDateKey = todayKey,
                    type = "hard_or_bytime",
                    title = "High-Impact Execution",
                    description = "Finish 1 Hard/Epic or By Time task",
                    target = 1,
                    progress = 0,
                    rewardXp = 40
                ),
                QuestEntity(
                    id = "quest_${todayKey}_3",
                    localDateKey = todayKey,
                    type = "focus_minutes",
                    title = "Deep Focus Sprint",
                    description = "Complete 25 minutes on the Focus Timer",
                    target = 25,
                    progress = 0,
                    rewardXp = 45
                )
            )
            questDao.insertQuests(defaultQuests)
        }

        // Ensure default Routine templates exist
        val existingRoutines = routineDao.getAllRoutinesSync()
        if (existingRoutines.isEmpty()) {
            routineDao.insertRoutines(
                listOf(
                    RoutineTemplateEntity(
                        id = "routine_morning",
                        name = "Morning Launchpad",
                        description = "Hydration, stretching & mindful priority check",
                        tasksPipeDelimited = "Drink 500ml water|Health & Fitness|Easy|15;Morning stretching routine|Health & Fitness|Easy|15;10 minutes of meditation|Mindfulness/Self-care|Easy|15"
                    ),
                    RoutineTemplateEntity(
                        id = "routine_study",
                        name = "Deep Study Block",
                        description = "Core concept reading, problem practice & summary",
                        tasksPipeDelimited = "Read core chapter notes|Study|Medium|45;Practice problem set|Study|Hard|60;Write quick summary|Study|Easy|15"
                    ),
                    RoutineTemplateEntity(
                        id = "routine_workout",
                        name = "Evening Fitness",
                        description = "Warm-up, main workout & protein recovery",
                        tasksPipeDelimited = "Complete a 5 km run or workout|Health & Fitness|Hard|45;Post-workout stretching|Health & Fitness|Easy|15;Track protein-rich meal|Health & Fitness|Easy|15"
                    )
                )
            )
        }

    }

    suspend fun insertTask(task: TaskEntity) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun addSubtask(taskId: String, title: String) {
        subtaskDao.insertSubtask(SubtaskEntity(taskId = taskId, title = title))
    }

    /**
     * Subtask completion with anti-farming XP cap:
     * Each completed subtask pays at most 20% of parent task XP, capped at 80% before parent completion.
     */
    suspend fun toggleSubtask(subtask: SubtaskEntity) {
        val now = System.currentTimeMillis()
        val parent = taskDao.getTaskById(subtask.taskId)
        val allSubtasks = subtaskDao.getSubtasksForTaskSync(subtask.taskId)

        if (!subtask.done && parent != null) {
            val parentMaxXp = XpCalculator.calculateTaskXp(
                durationMinutes = parent.estMinutes,
                difficulty = Difficulty.fromString(parent.difficulty),
                dueAt = parent.dueAt,
                completedAt = now,
                createdAt = parent.createdAt,
                currentStreak = 0
            ).finalXp

            val alreadyPaidSubtaskXp = allSubtasks.filter { it.done }.sumOf { it.awardedXp }
            val maxSubtaskPool = (parentMaxXp * 0.80).toInt()
            val singleSubtaskCap = maxOf(1, (parentMaxXp * 0.20).toInt())
            val remainingPool = (maxSubtaskPool - alreadyPaidSubtaskXp).coerceAtLeast(0)
            val payout = if (subtask.awardedXp == 0) minOf(singleSubtaskCap, remainingPool) else 0

            val updated = subtask.copy(
                done = true,
                completedAt = now,
                awardedXp = subtask.awardedXp + payout
            )
            subtaskDao.updateSubtask(updated)

            if (payout > 0) {
                xpLogDao.insertLog(
                    XpLogEntity(
                        idempotencyKey = "subtask_comp_${subtask.id}",
                        taskId = parent.id,
                        categoryId = parent.categoryId,
                        xpDelta = payout,
                        coinsDelta = 0,
                        reason = "subtask_progress",
                        localDateKey = parent.localDateKey,
                        createdAt = now
                    )
                )
                val stats = getUserStatsSync()
                val newTotal = stats.totalXp + payout
                userStatsDao.insertOrUpdate(
                    stats.copy(
                        totalXp = newTotal,
                        currentLevel = XpCalculator.getLevelProgress(newTotal).level,
                        weeklyLeagueXp = stats.weeklyLeagueXp + payout
                    )
                )
            }
        } else {
            subtaskDao.updateSubtask(subtask.copy(done = false, completedAt = null))
        }
    }

    suspend fun deleteSubtask(subtask: SubtaskEntity) {
        subtaskDao.deleteSubtask(subtask)
    }

    /**
     * Complete a task and enforce all DayQuest XP, duplicate-title, daily-cap, subtask-remainder,
     * category-level, quest-progress, and badge-unlock rules.
     */
    suspend fun completeTask(task: TaskEntity): TaskEntity {
        val now = System.currentTimeMillis()
        val stats = getUserStatsSync()

        // 1. Check Duplicate Normalized Title on same localDateKey
        val tasksToday = taskDao.getTasksForDateSync(task.localDateKey)
        val normTitle = OnDeviceTaskClassifier.normalizeText(task.title)
        val isDuplicateCompletedToday = tasksToday.any {
            it.id != task.id &&
                    it.status == "done" &&
                    it.xpAwarded > 0 &&
                    OnDeviceTaskClassifier.normalizeText(it.title) == normTitle
        }

        // 2. Check Daily Cap of 30 task XP awards per local day
        val logsToday = xpLogDao.getLogsForDateSync(task.localDateKey)
        val taskXpAwardsCountToday = logsToday.count { it.reason == "task_completion" && it.xpDelta > 0 }
        val isDailyCapReached = taskXpAwardsCountToday >= 30

        val diff = Difficulty.fromString(task.difficulty)
        val effectiveDuration = if (task.timerUsed && task.actualMinutes > 0) task.actualMinutes else task.estMinutes
        val breakdown = XpCalculator.calculateTaskXp(
            durationMinutes = effectiveDuration,
            difficulty = diff,
            dueAt = task.dueAt,
            completedAt = now,
            createdAt = task.createdAt,
            currentStreak = stats.currentStreak,
            timerUsed = task.timerUsed
        )

        // Subtract any pre-paid subtask XP so total task-related XP never exceeds calculated task XP
        val subtasks = subtaskDao.getSubtasksForTaskSync(task.id)
        val prepaidSubtaskXp = subtasks.sumOf { it.awardedXp }

        val finalAwardedXp = if (isDuplicateCompletedToday || isDailyCapReached) {
            0
        } else {
            (breakdown.finalXp - prepaidSubtaskXp).coerceAtLeast(5)
        }
        val finalCoins = if (finalAwardedXp > 0) (finalAwardedXp / 10) else 0

        val updatedTask = task.copy(
            status = "done",
            completedAt = now,
            xpAwarded = finalAwardedXp,
            xpBreakdownJson = "{\"base\":${breakdown.base},\"diffMult\":${breakdown.difficultyMultiplier},\"timingMult\":${breakdown.timingMultiplier},\"streakMult\":${breakdown.streakMultiplier},\"focusBonus\":${breakdown.focusBonusMultiplier},\"coins\":$finalCoins,\"duplicateZero\":$isDuplicateCompletedToday,\"dailyCapZero\":$isDailyCapReached}",
            updatedAt = now
        )
        taskDao.updateTask(updatedTask)

        // Append-only XP ledger entry
        xpLogDao.insertLog(
            XpLogEntity(
                idempotencyKey = "task_comp_${task.id}",
                taskId = task.id,
                categoryId = task.categoryId,
                xpDelta = finalAwardedXp,
                coinsDelta = finalCoins,
                reason = "task_completion",
                localDateKey = task.localDateKey,
                createdAt = now,
                breakdownJson = updatedTask.xpBreakdownJson
            )
        )

        // 3. Check Daily Bonuses (Early Bird before 09:00 +25 XP, All Planned >=3 +50 XP)
        var bonusXp = 0
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        if (cal.get(Calendar.HOUR_OF_DAY) < 9) {
            val earlyBirdInserted = xpLogDao.insertLog(
                XpLogEntity(
                    idempotencyKey = "bonus_early9_${task.localDateKey}",
                    xpDelta = 25,
                    coinsDelta = 2,
                    reason = "daily_bonus_before_9am",
                    localDateKey = task.localDateKey,
                    createdAt = now
                )
            )
            if (earlyBirdInserted != -1L) bonusXp += 25
        }

        val updatedTasksToday = taskDao.getTasksForDateSync(task.localDateKey)
        val allCompletedToday = updatedTasksToday.size >= 3 && updatedTasksToday.all { it.status == "done" }
        var newPerfectDays = stats.perfectDaysCount
        if (allCompletedToday) {
            val allPlannedInserted = xpLogDao.insertLog(
                XpLogEntity(
                    idempotencyKey = "bonus_all_planned_${task.localDateKey}",
                    xpDelta = 50,
                    coinsDelta = 5,
                    reason = "daily_bonus_all_planned",
                    localDateKey = task.localDateKey,
                    createdAt = now
                )
            )
            if (allPlannedInserted != -1L) {
                bonusXp += 50
                newPerfectDays += 1
            }
        }

        // 4. Update Category Progress
        val existingCat = categoryProgressDao.getCategoryProgress(task.categoryId) ?: CategoryProgressEntity(categoryId = task.categoryId)
        val newCatXp = existingCat.attributedXp + finalAwardedXp
        val newCatLevel = XpCalculator.getLevelProgress(newCatXp).level
        categoryProgressDao.insertOrUpdate(
            existingCat.copy(
                attributedXp = newCatXp,
                level = newCatLevel,
                tasksCompletedCount = existingCat.tasksCompletedCount + 1,
                updatedAt = now
            )
        )

        // 5. Update Quests reactively
        val questsToday = questDao.getQuestsForDateSync(task.localDateKey)
        for (q in questsToday) {
            if (!q.done) {
                val inc = when (q.type) {
                    "any_tasks" -> 1
                    "hard_or_bytime" -> if (task.difficulty == "Hard" || task.difficulty == "Epic" || task.scheduleType == "byTime") 1 else 0
                    else -> 0
                }
                if (inc > 0) {
                    val newProg = (q.progress + inc).coerceAtMost(q.target)
                    val isNowDone = newProg >= q.target
                    questDao.updateQuest(
                        q.copy(
                            progress = newProg,
                            done = isNowDone,
                            rewardedAt = if (isNowDone) now else null
                        )
                    )
                    if (isNowDone) {
                        val qInserted = xpLogDao.insertLog(
                            XpLogEntity(
                                idempotencyKey = "quest_reward_${q.id}",
                                xpDelta = q.rewardXp,
                                coinsDelta = 3,
                                reason = "quest_completed",
                                localDateKey = task.localDateKey,
                                createdAt = now
                            )
                        )
                        if (qInserted != -1L) bonusXp += q.rewardXp
                    }
                }
            }
        }

        // Check if all 3 Daily Quests are now finished (+100 XP bonus once per date)
        val refreshedQuests = questDao.getQuestsForDateSync(task.localDateKey)
        if (refreshedQuests.size >= 3 && refreshedQuests.all { it.done }) {
            val allQuestsInserted = xpLogDao.insertLog(
                XpLogEntity(
                    idempotencyKey = "bonus_all_quests_${task.localDateKey}",
                    xpDelta = 100,
                    coinsDelta = 10,
                    reason = "daily_quest_all_3_bonus",
                    localDateKey = task.localDateKey,
                    createdAt = now
                )
            )
            if (allQuestsInserted != -1L) bonusXp += 100
        }

        // 6. Update UserStats
        val totalXpGain = finalAwardedXp + bonusXp
        val newTotalXp = stats.totalXp + totalXpGain
        val newLevelProgress = XpCalculator.getLevelProgress(newTotalXp)
        val newCoins = stats.coins + finalCoins + (bonusXp / 10)
        val isEarlyCompletion = task.dueAt != null && (task.dueAt - now) > 5 * 60 * 1000L

        val todayKey = TimeUtils.todayDateKey()
        val updatedStreak = if (stats.lastActiveDateKey != todayKey) {
            val yesterdayKey = TimeUtils.dateKeyForOffset(-1)
            if (stats.lastActiveDateKey == yesterdayKey) {
                stats.currentStreak + 1
            } else {
                maxOf(1, stats.currentStreak)
            }
        } else {
            maxOf(1, stats.currentStreak)
        }
        val longestStreak = maxOf(stats.longestStreak, updatedStreak)

        val updatedStats = stats.copy(
            totalXp = newTotalXp,
            currentLevel = newLevelProgress.level,
            coins = newCoins,
            currentStreak = updatedStreak,
            longestStreak = longestStreak,
            lastActiveDateKey = todayKey,
            tasksCompletedTodayCount = stats.tasksCompletedTodayCount + 1,
            totalTasksCompleted = stats.totalTasksCompleted + 1,
            totalEarlyCompletions = stats.totalEarlyCompletions + (if (isEarlyCompletion) 1 else 0),
            perfectDaysCount = newPerfectDays,
            weeklyLeagueXp = stats.weeklyLeagueXp + totalXpGain,
            updatedAt = now
        )
        userStatsDao.insertOrUpdate(updatedStats)

        // 7. Reconcile Badge unlocks
        reconcileBadges(updatedStats)

        return updatedTask
    }

    private suspend fun reconcileBadges(stats: UserStatsEntity) {
        val catProg = categoryProgressDao.getAllCategoryProgressSync()
        val stored = badgeDao.getAllBadgesSync()
        val mlCount = mlCorrectionDao.getRecentCorrectionsSync().size
        val resolved = BadgeCatalogue.evaluateAll(stats, catProg, stored, mlCount)

        val now = System.currentTimeMillis()
        var badgeBonusXp = 0
        var badgeBonusCoins = 0
        val toSave = mutableListOf<BadgeProgressEntity>()

        for (r in resolved) {
            if (r.isUnlocked && !r.isRewarded) {
                val logAdded = xpLogDao.insertLog(
                    XpLogEntity(
                        idempotencyKey = "badge_unlock_${r.definition.id}",
                        xpDelta = r.definition.rewardXp,
                        coinsDelta = r.definition.rewardCoins,
                        reason = "badge_unlocked_${r.definition.name}",
                        localDateKey = TimeUtils.todayDateKey(),
                        createdAt = now
                    )
                )
                if (logAdded != -1L) {
                    badgeBonusXp += r.definition.rewardXp
                    badgeBonusCoins += r.definition.rewardCoins
                }
                toSave.add(
                    BadgeProgressEntity(
                        id = r.definition.id,
                        progress = r.progress,
                        unlockedAt = r.unlockedAt ?: now,
                        rewardedAt = now
                    )
                )
            } else if (r.progress > 0) {
                toSave.add(
                    BadgeProgressEntity(
                        id = r.definition.id,
                        progress = r.progress,
                        unlockedAt = r.unlockedAt,
                        rewardedAt = if (r.isRewarded) now else null
                    )
                )
            }
        }

        if (toSave.isNotEmpty()) {
            badgeDao.insertOrUpdateAll(toSave)
        }
        if (badgeBonusXp > 0 || badgeBonusCoins > 0) {
            val newXp = stats.totalXp + badgeBonusXp
            userStatsDao.insertOrUpdate(
                stats.copy(
                    totalXp = newXp,
                    currentLevel = XpCalculator.getLevelProgress(newXp).level,
                    coins = stats.coins + badgeBonusCoins
                )
            )
        }
    }

    /**
     * Undo task completion - restores status to pending and compensates ledger
     */
    suspend fun undoCompleteTask(task: TaskEntity) {
        val stats = getUserStatsSync()
        val xpToDeduct = task.xpAwarded
        val coinsToDeduct = (xpToDeduct / 10).coerceAtLeast(0)

        val revertedTask = task.copy(
            status = "pending",
            completedAt = null,
            xpAwarded = 0,
            xpBreakdownJson = null,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(revertedTask)
        xpLogDao.deleteLogByIdempotency("task_comp_${task.id}")

        val newTotalTasksCompleted = maxOf(0, stats.totalTasksCompleted - 1)
        val newTasksToday = maxOf(0, stats.tasksCompletedTodayCount - 1)
        if (newTasksToday == 0) {
            xpLogDao.deleteLogByIdempotency("bonus_early9_${task.localDateKey}")
        }
        if (newTotalTasksCompleted == 0) {
            xpLogDao.deleteLogByIdempotency("badge_unlock_volume_1")
        }

        val newTotalXp = if (newTotalTasksCompleted == 0) 0 else maxOf(0, stats.totalXp - xpToDeduct)
        val newLevel = XpCalculator.getLevelProgress(newTotalXp).level
        val newCoins = maxOf(0, stats.coins - coinsToDeduct)

        userStatsDao.insertOrUpdate(
            stats.copy(
                totalXp = newTotalXp,
                currentLevel = newLevel,
                coins = newCoins,
                tasksCompletedTodayCount = newTasksToday,
                totalTasksCompleted = newTotalTasksCompleted,
                weeklyLeagueXp = if (newTotalTasksCompleted == 0) 0 else maxOf(0, stats.weeklyLeagueXp - xpToDeduct),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun rescheduleTask(task: TaskEntity, newDateKey: String) {
        taskDao.updateTask(
            task.copy(
                localDateKey = newDateKey,
                status = "pending",
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Record a completed Focus Timer session and update focus quests/stats
     */
    suspend fun recordFocusSession(
        task: TaskEntity?,
        durationSeconds: Int,
        completeAssociatedTask: Boolean
    ) {
        val now = System.currentTimeMillis()
        val todayKey = TimeUtils.todayDateKey()
        val minutes = maxOf(1, durationSeconds / 60)

        focusSessionDao.insertSession(
            FocusSessionEntity(
                taskId = task?.id,
                taskTitle = task?.title ?: "Deep Focus Session",
                categoryId = task?.categoryId ?: "Study",
                startedAt = now - (durationSeconds * 1000L),
                endedAt = now,
                durationSeconds = durationSeconds,
                completed = true,
                localDateKey = todayKey
            )
        )

        val stats = getUserStatsSync()
        userStatsDao.insertOrUpdate(
            stats.copy(
                totalFocusSeconds = stats.totalFocusSeconds + durationSeconds,
                updatedAt = now
            )
        )

        // Update focus minutes quest
        val quests = questDao.getQuestsForDateSync(todayKey)
        for (q in quests) {
            if (q.type == "focus_minutes" && !q.done) {
                val newProg = (q.progress + minutes).coerceAtMost(q.target)
                val isDone = newProg >= q.target
                questDao.updateQuest(q.copy(progress = newProg, done = isDone, rewardedAt = if (isDone) now else null))
            }
        }

        if (task != null) {
            val updated = task.copy(
                timerUsed = true,
                timerSeconds = task.timerSeconds + durationSeconds,
                actualMinutes = task.actualMinutes + minutes
            )
            taskDao.updateTask(updated)
            if (completeAssociatedTask && updated.status != "done") {
                completeTask(updated)
            }
        }
    }

    /**
     * Insert all tasks from a Routine template into the target date with distinct occurrence IDs
     */
    suspend fun applyRoutineTemplate(routine: RoutineTemplateEntity, dateKey: String): Int {
        val entries = routine.tasksPipeDelimited.split(";").filter { it.isNotBlank() }
        val newTasks = entries.map { raw ->
            val parts = raw.split("|")
            val title = parts.getOrElse(0) { "Routine Task" }.trim()
            val cat = parts.getOrElse(1) { "Other" }.trim()
            val diff = parts.getOrElse(2) { "Easy" }.trim()
            val mins = parts.getOrElse(3) { "15" }.trim().toIntOrNull() ?: 15
            TaskEntity(
                seriesId = routine.id,
                occurrenceId = UUID.randomUUID().toString(),
                title = title,
                categoryId = cat,
                categorySource = "user",
                difficulty = diff,
                estMinutes = mins,
                localDateKey = dateKey
            )
        }
        taskDao.insertTasks(newTasks)
        return newTasks.size
    }

    suspend fun createCustomRoutine(name: String, description: String, tasksPipeDelimited: String) {
        routineDao.insertRoutine(
            RoutineTemplateEntity(
                name = name,
                description = description,
                tasksPipeDelimited = tasksPipeDelimited
            )
        )
    }

    /**
     * Record user ML category/difficulty/duration confirmation or correction (weight = 3x for user override)
     */
    suspend fun recordMlCorrection(
        rawTitle: String,
        originalCategory: String,
        confirmedCategory: String,
        confirmedDifficulty: String,
        confirmedDuration: Int
    ) {
        val norm = OnDeviceTaskClassifier.normalizeText(rawTitle)
        val wasAutoCorrect = originalCategory.equals(confirmedCategory, ignoreCase = true)
        mlCorrectionDao.insertCorrection(
            MlCorrectionEntity(
                normalizedText = norm,
                originalCategory = originalCategory,
                correctedCategory = confirmedCategory,
                correctedDifficulty = confirmedDifficulty,
                correctedDuration = confirmedDuration,
                wasAutoCorrect = wasAutoCorrect,
                weight = if (wasAutoCorrect) 1 else 3
            )
        )
        reconcileBadges(getUserStatsSync())
    }

    suspend fun getRecentMlCorrectionsSync(): List<MlCorrectionEntity> {
        return mlCorrectionDao.getRecentCorrectionsSync()
    }

    suspend fun clearMlCorrections() {
        mlCorrectionDao.clearAllCorrections()
    }

    /**
     * Buy a Streak Freeze (costs 100 coins, max 2 stored) or Store item atomically
     */
    suspend fun purchaseStreakFreeze(): Boolean {
        val stats = getUserStatsSync()
        if (stats.streakFreezes >= 2 || stats.coins < 100) return false
        val now = System.currentTimeMillis()
        userStatsDao.insertOrUpdate(
            stats.copy(
                coins = stats.coins - 100,
                streakFreezes = stats.streakFreezes + 1,
                updatedAt = now
            )
        )
        xpLogDao.insertLog(
            XpLogEntity(
                idempotencyKey = "freeze_buy_$now",
                xpDelta = 0,
                coinsDelta = -100,
                reason = "streak_freeze_purchase",
                localDateKey = TimeUtils.todayDateKey(),
                createdAt = now
            )
        )
        return true
    }

    suspend fun purchaseStoreItem(itemId: String, itemType: String, costCoins: Int): Boolean {
        val stats = getUserStatsSync()
        if (stats.coins < costCoins) return false
        storePurchaseDao.insertPurchase(
            StorePurchaseEntity(itemId = itemId, itemType = itemType, costCoins = costCoins)
        )
        val updated = if (itemType == "avatar_frame") {
            stats.copy(coins = stats.coins - costCoins, avatarFrame = itemId)
        } else {
            stats.copy(coins = stats.coins - costCoins)
        }
        userStatsDao.insertOrUpdate(updated)
        return true
    }

    suspend fun updateProfileNameAndTier(displayName: String, leagueTier: String? = null) {
        val stats = getUserStatsSync()
        userStatsDao.insertOrUpdate(
            stats.copy(
                displayName = displayName.ifBlank { stats.displayName },
                leagueTier = leagueTier ?: stats.leagueTier
            )
        )
    }

    suspend fun updateProfileFull(
        displayName: String,
        avatarId: String,
        bio: String,
        occupation: String,
        optInLeaderboard: Boolean,
        publicProfile: Boolean
    ) {
        val stats = getUserStatsSync()
        userStatsDao.insertOrUpdate(
            stats.copy(
                displayName = displayName.ifBlank { stats.displayName },
                avatarId = avatarId,
                bio = bio,
                occupation = occupation,
                optInLeaderboard = optInLeaderboard,
                publicProfile = publicProfile,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateGoogleAuth(email: String?, uid: String?) {
        val stats = getUserStatsSync()
        userStatsDao.insertOrUpdate(
            stats.copy(
                googleEmail = email,
                googleUserId = uid,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateLastCloudSyncTime(timestamp: Long) {
        val stats = getUserStatsSync()
        userStatsDao.insertOrUpdate(
            stats.copy(
                lastCloudSyncTime = timestamp
            )
        )
    }

    suspend fun addFriend(displayName: String, inviteCode: String) {
        if (inviteCode.equals(getUserStatsSync().inviteCode, ignoreCase = true)) return // Prevent self-challenge
        friendDao.insertFriend(
            FriendEntity(
                displayName = displayName.trim(),
                inviteCode = inviteCode.trim().uppercase(),
                weeklyXp = 0, // Real user starts with 0 XP until genuine updates
                leagueTier = "Bronze",
                challengeActive = true,
                challengeTargetXp = 300
            )
        )
    }

    suspend fun claimFriendChallengeReward(friend: FriendEntity): Boolean {
        val stats = getUserStatsSync()
        if (friend.challengeRewardClaimed || stats.weeklyLeagueXp < friend.weeklyXp) return false
        friendDao.updateFriend(friend.copy(challengeRewardClaimed = true))
        userStatsDao.insertOrUpdate(
            stats.copy(coins = stats.coins + 50, totalXp = stats.totalXp + 50)
        )
        xpLogDao.insertLog(
            XpLogEntity(
                idempotencyKey = "challenge_win_${friend.id}",
                xpDelta = 50,
                coinsDelta = 50,
                reason = "friend_1v1_challenge_victory",
                localDateKey = TimeUtils.todayDateKey()
            )
        )
        return true
    }

    suspend fun saveSetting(key: String, value: String) {
        appSettingDao.setSetting(AppSettingEntity(key = key, value = value))
    }

    suspend fun importTasks(tasks: List<TaskEntity>) {
        taskDao.insertTasks(tasks)
    }

    suspend fun deleteAllUserData() {
        taskDao.deleteAllTasks()
        xpLogDao.deleteAllLogs()
        categoryProgressDao.deleteAll()
        badgeDao.deleteAll()
        mlCorrectionDao.clearAllCorrections()
        questDao.deleteAll()
        focusSessionDao.deleteAll()
        userStatsDao.deleteAll()
    }
}
