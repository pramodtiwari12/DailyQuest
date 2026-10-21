package com.example.dayquest.data.db

import androidx.room.*
import com.example.dayquest.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE localDateKey = :dateKey ORDER BY CASE WHEN scheduleType = 'byTime' THEN 0 WHEN scheduleType = 'range' THEN 1 ELSE 2 END, dueAt ASC, createdAt ASC")
    fun getTasksForDate(dateKey: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE localDateKey = :dateKey")
    suspend fun getTasksForDateSync(dateKey: String): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("SELECT * FROM tasks WHERE localDateKey = :dateKey AND status = 'pending' AND dueAt IS NOT NULL AND dueAt < :nowMillis")
    suspend fun getOverdueTasks(dateKey: String, nowMillis: Long): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE localDateKey = :dateKey AND status = 'done'")
    suspend fun getCompletedCountForDate(dateKey: String): Int
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun getSubtasksForTask(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getSubtasksForTaskSync(taskId: String): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity)

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)
}

@Dao
interface XpLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: XpLogEntity): Long

    @Query("SELECT * FROM xp_logs ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<XpLogEntity>>

    @Query("SELECT * FROM xp_logs ORDER BY createdAt DESC")
    suspend fun getAllLogsSync(): List<XpLogEntity>

    @Query("SELECT * FROM xp_logs WHERE localDateKey = :dateKey ORDER BY createdAt DESC")
    fun getLogsForDate(dateKey: String): Flow<List<XpLogEntity>>

    @Query("SELECT * FROM xp_logs WHERE localDateKey = :dateKey ORDER BY createdAt DESC")
    suspend fun getLogsForDateSync(dateKey: String): List<XpLogEntity>

    @Query("SELECT * FROM xp_logs WHERE taskId = :taskId ORDER BY createdAt DESC")
    suspend fun getLogsForTask(taskId: String): List<XpLogEntity>

    @Query("DELETE FROM xp_logs WHERE idempotencyKey = :idempotencyKey")
    suspend fun deleteLogByIdempotency(idempotencyKey: String)

    @Query("DELETE FROM xp_logs")
    suspend fun deleteAllLogs()

    @Query("SELECT SUM(xpDelta) FROM xp_logs")
    suspend fun getTotalXp(): Int?
}

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 'primary_user' LIMIT 1")
    fun getUserStats(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 'primary_user' LIMIT 1")
    suspend fun getUserStatsSync(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStatsEntity)

    @Query("DELETE FROM user_stats")
    suspend fun deleteAll()
}

@Dao
interface CategoryProgressDao {
    @Query("SELECT * FROM category_progress")
    fun getAllCategoryProgress(): Flow<List<CategoryProgressEntity>>

    @Query("SELECT * FROM category_progress")
    suspend fun getAllCategoryProgressSync(): List<CategoryProgressEntity>

    @Query("SELECT * FROM category_progress WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getCategoryProgress(categoryId: String): CategoryProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: CategoryProgressEntity)

    @Query("DELETE FROM category_progress")
    suspend fun deleteAll()
}

@Dao
interface BadgeDao {
    @Query("SELECT * FROM badges")
    fun getAllBadges(): Flow<List<BadgeProgressEntity>>

    @Query("SELECT * FROM badges")
    suspend fun getAllBadgesSync(): List<BadgeProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(badge: BadgeProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(badges: List<BadgeProgressEntity>)

    @Query("DELETE FROM badges")
    suspend fun deleteAll()
}

@Dao
interface MlCorrectionDao {
    @Query("SELECT * FROM ml_corrections ORDER BY createdAt DESC")
    fun getAllCorrections(): Flow<List<MlCorrectionEntity>>

    @Query("SELECT * FROM ml_corrections ORDER BY createdAt DESC LIMIT 200")
    suspend fun getRecentCorrectionsSync(): List<MlCorrectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(correction: MlCorrectionEntity)

    @Query("DELETE FROM ml_corrections")
    suspend fun clearAllCorrections()
}

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests WHERE localDateKey = :dateKey ORDER BY id ASC")
    fun getQuestsForDate(dateKey: String): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE localDateKey = :dateKey ORDER BY id ASC")
    suspend fun getQuestsForDateSync(dateKey: String): List<QuestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<QuestEntity>)

    @Update
    suspend fun updateQuest(quest: QuestEntity)

    @Query("DELETE FROM quests")
    suspend fun deleteAll()
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY createdAt ASC")
    fun getAllRoutines(): Flow<List<RoutineTemplateEntity>>

    @Query("SELECT * FROM routines ORDER BY createdAt ASC")
    suspend fun getAllRoutinesSync(): List<RoutineTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutines(routines: List<RoutineTemplateEntity>)

    @Delete
    suspend fun deleteRoutine(routine: RoutineTemplateEntity)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    suspend fun getAllSessionsSync(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends_challenges ORDER BY weeklyXp DESC")
    fun getAllFriends(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends_challenges ORDER BY weeklyXp DESC")
    suspend fun getAllFriendsSync(): List<FriendEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Update
    suspend fun updateFriend(friend: FriendEntity)

    @Delete
    suspend fun deleteFriend(friend: FriendEntity)
}

@Dao
interface StorePurchaseDao {
    @Query("SELECT * FROM store_purchases ORDER BY purchasedAt DESC")
    fun getAllPurchases(): Flow<List<StorePurchaseEntity>>

    @Query("SELECT * FROM store_purchases")
    suspend fun getAllPurchasesSync(): List<StorePurchaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: StorePurchaseEntity)
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingEntity)

    @Query("DELETE FROM app_settings")
    suspend fun clearAll()
}
