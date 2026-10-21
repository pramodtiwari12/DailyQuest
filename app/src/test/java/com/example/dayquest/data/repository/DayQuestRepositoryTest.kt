package com.example.dayquest.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.dayquest.data.db.DayQuestDatabase
import com.example.dayquest.data.model.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DayQuestRepositoryTest {

    private lateinit var database: DayQuestDatabase
    private lateinit var repository: DayQuestRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DayQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DayQuestRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testInsertAndGetTasksForDate() = runBlocking {
        val task = TaskEntity(
            title = "Solve LeetCode problem",
            categoryId = "Skill/Coding",
            difficulty = "Medium",
            estMinutes = 45,
            scheduleType = "byTime",
            dueAt = 1700000000000L,
            localDateKey = "2026-10-20"
        )
        repository.insertTask(task)

        val tasks = repository.getTasksForDate("2026-10-20").first()
        assertEquals(1, tasks.size)
        assertEquals("Solve LeetCode problem", tasks[0].title)
        assertEquals("Skill/Coding", tasks[0].categoryId)
        assertEquals("byTime", tasks[0].scheduleType)
    }

    @Test
    fun testCompleteTaskAndUndo() = runBlocking {
        val task = TaskEntity(
            title = "Read a mathematics chapter",
            categoryId = "Study",
            difficulty = "Easy",
            estMinutes = 30,
            localDateKey = "2026-10-20",
            createdAt = System.currentTimeMillis() - 60000L
        )
        repository.insertTask(task)

        // Complete task
        val completed = repository.completeTask(task)
        assertEquals("done", completed.status)
        assertTrue(completed.xpAwarded > 0)

        // Verify stats updated
        val statsAfterComplete = repository.getUserStats().first()
        assertTrue(statsAfterComplete.totalXp > 0)
        assertEquals(1, statsAfterComplete.tasksCompletedTodayCount)

        // Undo complete
        repository.undoCompleteTask(completed)
        val reverted = repository.getTaskById(task.id)
        assertNotNull(reverted)
        assertEquals("pending", reverted?.status)
        assertEquals(0, reverted?.xpAwarded)

        val statsAfterUndo = repository.getUserStats().first()
        assertEquals(0, statsAfterUndo.totalXp)
        assertEquals(0, statsAfterUndo.tasksCompletedTodayCount)
    }

    @Test
    fun testSubtasksChecklist() = runBlocking {
        val task = TaskEntity(
            title = "Prepare weekly presentation",
            categoryId = "Work/Career",
            localDateKey = "2026-10-20"
        )
        repository.insertTask(task)

        repository.addSubtask(task.id, "Create title slide")
        repository.addSubtask(task.id, "Gather metrics data")

        val subtasks = repository.getSubtasksForTask(task.id).first()
        assertEquals(2, subtasks.size)
        assertFalse(subtasks[0].done)

        // Toggle first subtask
        repository.toggleSubtask(subtasks[0])
        val updated = repository.getSubtasksForTask(task.id).first()
        assertTrue(updated[0].done)
    }

    @Test
    fun testDeleteTask() = runBlocking {
        val task = TaskEntity(
            title = "Temporary task",
            localDateKey = "2026-10-20"
        )
        repository.insertTask(task)
        assertEquals(1, repository.getTasksForDate("2026-10-20").first().size)

        repository.deleteTask(task)
        assertEquals(0, repository.getTasksForDate("2026-10-20").first().size)
    }
}
