package com.example.dayquest.data.cloud

import android.content.Context
import com.example.dayquest.data.model.UserStatsEntity
import com.example.dayquest.domain.league.CohortZone
import com.example.dayquest.domain.league.LeaderboardEntry
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(private val context: Context) {

    private val isFirebaseInitialized: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }

    private val firestore: FirebaseFirestore?
        get() = if (isFirebaseInitialized) FirebaseFirestore.getInstance() else null

    /**
     * Synchronize permitted public profile and weekly ranking data to Firestore.
     * Idempotent: uses document ID = uid.
     */
    suspend fun syncUserToLeaderboard(
        uid: String,
        stats: UserStatsEntity
    ): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firebase is not initialized"))

        if (!stats.optInLeaderboard) {
            // If user opted out, remove from public leaderboard collection
            return try {
                db.collection("leaderboard").document(uid).delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        return try {
            val entryData = hashMapOf(
                "uid" to uid,
                "displayName" to stats.displayName,
                "avatarId" to stats.avatarId,
                "weeklyXp" to stats.weeklyLeagueXp,
                "leagueTier" to stats.leagueTier,
                "occupation" to stats.occupation,
                "totalXp" to stats.totalXp,
                "currentLevel" to stats.currentLevel,
                "lastUpdated" to System.currentTimeMillis()
            )

            db.collection("leaderboard")
                .document(uid)
                .set(entryData, SetOptions.merge())
                .await()

            // Also update private user doc
            val privateUserData = hashMapOf(
                "uid" to uid,
                "displayName" to stats.displayName,
                "avatarId" to stats.avatarId,
                "bio" to stats.bio,
                "occupation" to stats.occupation,
                "weeklyXp" to stats.weeklyLeagueXp,
                "totalXp" to stats.totalXp,
                "currentStreak" to stats.currentStreak,
                "longestStreak" to stats.longestStreak,
                "perfectDaysCount" to stats.perfectDaysCount,
                "optInLeaderboard" to stats.optInLeaderboard,
                "publicProfile" to stats.publicProfile,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(uid)
                .set(privateUserData, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch real registered competitors for the specified league tier and optional occupation segment.
     * Returns ONLY real registered users from Firestore. No bots, no synthetic records.
     */
    suspend fun fetchRealLeaderboard(
        leagueTier: String,
        currentUid: String?,
        occupationFilter: String? = null
    ): Result<List<LeaderboardEntry>> {
        val db = firestore ?: return Result.success(emptyList())

        return try {
            var query: Query = db.collection("leaderboard")
                .whereEqualTo("leagueTier", leagueTier)
                .orderBy("weeklyXp", Query.Direction.DESCENDING)
                .limit(50)

            if (!occupationFilter.isNullOrBlank() && occupationFilter != "All") {
                query = query.whereEqualTo("occupation", occupationFilter)
            }

            val snapshot = query.get().await()
            val entries = mutableListOf<LeaderboardEntry>()

            snapshot.documents.forEachIndexed { index, doc ->
                val uid = doc.getString("uid") ?: doc.id
                val name = doc.getString("displayName") ?: "Adventurer"
                val xp = doc.getLong("weeklyXp")?.toInt() ?: 0
                val avatarId = doc.getString("avatarId") ?: "vanguard"
                val occ = doc.getString("occupation") ?: "General"
                val isMe = uid == currentUid

                entries.add(
                    LeaderboardEntry(
                        id = uid,
                        rank = index + 1,
                        displayName = if (isMe) "$name (You)" else name,
                        weeklyXp = xp,
                        isCurrentUser = isMe,
                        zone = CohortZone.SAFE,
                        avatarId = avatarId,
                        occupation = occ
                    )
                )
            }

            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
