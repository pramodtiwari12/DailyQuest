package com.example.dayquest

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.dayquest.data.db.DayQuestDatabase
import com.example.dayquest.data.model.MlCorrectionEntity
import com.example.dayquest.data.model.TaskEntity
import com.example.dayquest.data.repository.DayQuestRepository
import com.example.dayquest.domain.badges.BadgeCatalogue
import com.example.dayquest.domain.league.CohortZone
import com.example.dayquest.domain.league.LeaderboardEntry
import com.example.dayquest.domain.league.LeagueEngine
import com.example.dayquest.domain.league.LeagueTier
import com.example.dayquest.domain.ml.OnDeviceTaskClassifier
import com.example.dayquest.domain.security.BackupCryptoManager
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
class Phases2To7VerificationTest {

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
    fun testPhase2DuplicateNormalizedTitleZeroXpRule() = runBlocking {
        val now = System.currentTimeMillis()
        val firstTask = TaskEntity(
            title = "Practice Calculus Problems",
            categoryId = "Study",
            difficulty = "Medium",
            estMinutes = 30,
            localDateKey = "2026-10-20",
            createdAt = now - 60000L
        )
        val duplicateTask = TaskEntity(
            title = "  practice   calculus problems! ",
            categoryId = "Study",
            difficulty = "Medium",
            estMinutes = 30,
            localDateKey = "2026-10-20",
            createdAt = now - 60000L
        )

        repository.insertTask(firstTask)
        repository.insertTask(duplicateTask)

        val comp1 = repository.completeTask(firstTask)
        assertTrue("First occurrence must earn positive XP", comp1.xpAwarded > 0)

        val comp2 = repository.completeTask(duplicateTask)
        assertEquals("Duplicate normalized title on same date must earn 0 XP", 0, comp2.xpAwarded)
    }

    @Test
    fun testPhase2DailyQuestsAndRoutinesSeeding() = runBlocking {
        repository.ensureDefaultsSeeded("2026-10-20")
        val quests = repository.getQuestsForDate("2026-10-20").first()
        assertEquals(3, quests.size)

        val routines = repository.getAllRoutines().first()
        assertTrue(routines.size >= 3)

        val addedCount = repository.applyRoutineTemplate(routines.first(), "2026-10-20")
        assertEquals(3, addedCount)
        val tasksForDay = repository.getTasksForDate("2026-10-20").first()
        assertEquals(3, tasksForDay.size)
        // Verify distinct occurrence IDs
        assertEquals(3, tasksForDay.mapNotNull { it.occurrenceId }.distinct().size)
    }

    @Test
    fun testPhase3MultilingualOnDeviceMlAndPersonalization() {
        // 1. Technical tokens preserved
        val normTech = OnDeviceTaskClassifier.normalizeText("Write C++ and C# SQL queries in 10k repo")
        assertTrue(normTech.contains("c++"))
        assertTrue(normTech.contains("c#"))
        assertTrue(normTech.contains("sql"))

        // 2. Multilingual predictions (English, Hindi, Hinglish)
        val predEn = OnDeviceTaskClassifier.predict("Read a mathematics chapter")
        assertEquals("Study", predEn.topCategoryId)

        val predHi = OnDeviceTaskClassifier.predict("गणित का अध्याय पढ़ो")
        assertEquals("Study", predHi.topCategoryId)

        val predHinglish = OnDeviceTaskClassifier.predict("subah ki workout poora karo")
        assertEquals("Health & Fitness", predHinglish.topCategoryId)

        // 3. Personalization kNN override (3x weight)
        val customCorrection = MlCorrectionEntity(
            normalizedText = OnDeviceTaskClassifier.normalizeText("sync obsidian vault"),
            originalCategory = "Other",
            correctedCategory = "Skill/Coding",
            weight = 3
        )
        val personalizedPred = OnDeviceTaskClassifier.predict(
            taskTitle = "sync obsidian vault notes",
            userCorrections = listOf(customCorrection)
        )
        assertEquals("Skill/Coding", personalizedPred.topCategoryId)

        // 4. Held-out evaluation metrics check
        val eval = OnDeviceTaskClassifier.evaluateSeedCorpus()
        assertTrue("Macro-F1 must meet or exceed 0.85 quality gate", eval.macroF1 >= 0.85f)
        assertTrue("p95 inference latency must be < 50ms", eval.p95LatencyMs < 50f)
    }

    @Test
    fun testPhase5BadgeCatalogueCoverageAndEvaluation() = runBlocking {
        val allBadges = BadgeCatalogue.ALL_BADGES
        assertTrue("Catalogue must contain at least 40 unique badges", allBadges.size >= 40)
        assertTrue(allBadges.size >= 69)

        // Verify 33 Category Mastery badges (11 categories * 3 levels)
        val catMasteryCount = allBadges.count { it.group == "Category Mastery" }
        assertEquals(33, catMasteryCount)

        // Verify at least 5 secret badges
        val secretCount = allBadges.count { it.isSecret }
        assertTrue(secretCount >= 5)
    }

    @Test
    fun testRealLeagueRankingNoBots() {
        val currentUser = LeaderboardEntry(
            id = "user_me",
            rank = 1,
            displayName = "Adventurer (You)",
            weeklyXp = 450,
            isCurrentUser = true,
            zone = CohortZone.SAFE
        )
        val realPeer = LeaderboardEntry(
            id = "user_peer",
            rank = 2,
            displayName = "Jordan_Dev",
            weeklyXp = 320,
            isCurrentUser = false,
            zone = CohortZone.SAFE
        )

        val board = LeagueEngine.buildRealLeaderboard(
            tier = LeagueTier.BRONZE,
            currentUserEntry = currentUser,
            otherRealUsers = listOf(realPeer)
        )

        assertEquals(2, board.size)
        assertEquals("user_me", board[0].id)
        assertEquals("user_peer", board[1].id)
        // Zero simulated or bot entries
        assertEquals(0, board.count { it.displayName.contains("Bot") })
    }

    @Test
    fun testPhase6AesGcmEncryptedBackupRoundtripAndWrongPassphrase() {
        val sampleTasks = listOf(
            TaskEntity(
                id = "t1",
                title = "Confidential financial review",
                categoryId = "Finance",
                difficulty = "Hard",
                estMinutes = 60,
                localDateKey = "2026-10-20"
            )
        )
        val json = BackupCryptoManager.exportTasksToJson(sampleTasks)
        val envelope = BackupCryptoManager.encryptBackup(json, "MyStrongPass!2026")

        // Ensure plaintext task title does NOT appear in the encrypted envelope
        assertFalse(envelope.contains("Confidential financial review"))

        // Roundtrip decrypt with correct passphrase
        val decrypted = BackupCryptoManager.decryptBackup(envelope, "MyStrongPass!2026")
        val restored = BackupCryptoManager.importTasksFromJson(decrypted)
        assertEquals(1, restored.size)
        assertEquals("Confidential financial review", restored[0].title)

        // Wrong passphrase must fail authentication tag check
        try {
            BackupCryptoManager.decryptBackup(envelope, "WrongPassphrase")
            fail("Decryption with wrong passphrase must throw an exception")
        } catch (expected: Exception) {
            // Expected GCM tag mismatch
        }
    }
}
