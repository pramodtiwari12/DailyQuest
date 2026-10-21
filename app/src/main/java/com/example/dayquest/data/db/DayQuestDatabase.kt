package com.example.dayquest.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dayquest.data.model.*

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        XpLogEntity::class,
        UserStatsEntity::class,
        CategoryProgressEntity::class,
        BadgeProgressEntity::class,
        MlCorrectionEntity::class,
        QuestEntity::class,
        RoutineTemplateEntity::class,
        FocusSessionEntity::class,
        FriendEntity::class,
        StorePurchaseEntity::class,
        AppSettingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DayQuestDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun xpLogDao(): XpLogDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun categoryProgressDao(): CategoryProgressDao
    abstract fun badgeDao(): BadgeDao
    abstract fun mlCorrectionDao(): MlCorrectionDao
    abstract fun questDao(): QuestDao
    abstract fun routineDao(): RoutineDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun friendDao(): FriendDao
    abstract fun storePurchaseDao(): StorePurchaseDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: DayQuestDatabase? = null

        fun getInstance(context: Context): DayQuestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DayQuestDatabase::class.java,
                    "dayquest.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
