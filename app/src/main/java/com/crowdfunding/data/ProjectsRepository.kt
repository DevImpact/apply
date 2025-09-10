package com.crowdfunding.data

import com.crowdfunding.ui.projects.IntentionRecord
import com.crowdfunding.ui.projects.Project
import com.crowdfunding.ui.projects.ProjectStats
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class ProjectsRepository {

    private val rootRef = Firebase.database.reference
    private val projectsRef = rootRef.child("projects")
    private val projectIntentionsRef = rootRef.child("projectIntentions")
    private val userIntentionsRef = rootRef.child("userIntentions")

    suspend fun getProjects(): List<Project> {
        return try {
            val dataSnapshot = projectsRef.get().await()
            dataSnapshot.children.mapNotNull { it.getValue(Project::class.java) }
        } catch (e: Exception) {
            // Log the error
            emptyList()
        }
    }

    suspend fun getProjectById(projectId: String): Project? {
        return try {
            val dataSnapshot = projectsRef.child(projectId).get().await()
            dataSnapshot.getValue(Project::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun recordUserIntention(projectId: String, userId: String, intentionType: String, previousIntention: String?) {
        try {
            val timestamp = System.currentTimeMillis()
            val intentionRecord = IntentionRecord(intentionType, timestamp)

            val updates = mutableMapOf<String, Any?>()

            // Remove the previous intention if it exists
            if (previousIntention != null) {
                updates["/projectIntentions/$projectId/$userId"] = null
                updates["/userIntentions/$userId/$projectId"] = null
            }

            // Add the new intention
            updates["/projectIntentions/$projectId/$userId"] = intentionRecord
            updates["/userIntentions/$userId/$projectId"] = intentionRecord

            // Perform the atomic multi-path update
            rootRef.updateChildren(updates).await()

            // Run the stats transaction after the intention is successfully recorded
            runStatsTransaction(projectId, intentionType, previousIntention)

        } catch (e: Exception) {
            // Handle or log the exception
        }
    }

    private fun runStatsTransaction(projectId: String, newIntention: String, previousIntention: String?) {
        projectsRef.child(projectId).child("stats").runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val stats = currentData.getValue(ProjectStats::class.java) ?: ProjectStats()

                var updatedStats = stats
                // Decrement the count for the previous intention
                if (previousIntention != null) {
                    updatedStats = when (previousIntention) {
                        IntentionKeys.INVESTORS -> updatedStats.copy(investors = (updatedStats.investors - 1).coerceAtLeast(0))
                        IntentionKeys.DONORS -> updatedStats.copy(donors = (updatedStats.donors - 1).coerceAtLeast(0))
                        IntentionKeys.ADVERTISERS -> updatedStats.copy(advertisers = (updatedStats.advertisers - 1).coerceAtLeast(0))
                        else -> updatedStats
                    }
                }

                // Increment the count for the new intention
                updatedStats = when (newIntention) {
                    IntentionKeys.INVESTORS -> updatedStats.copy(investors = updatedStats.investors + 1)
                    IntentionKeys.DONORS -> updatedStats.copy(donors = updatedStats.donors + 1)
                    IntentionKeys.ADVERTISERS -> updatedStats.copy(advertisers = updatedStats.advertisers + 1)
                    else -> updatedStats
                }

                currentData.value = updatedStats
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                // Optional: Log completion or error
            }
        })
    }

    suspend fun getIntentingUserIds(projectId: String, intentionType: String): List<String> {
        return try {
            val dataSnapshot = projectIntentionsRef.child(projectId).get().await()
            dataSnapshot.children.mapNotNull {
                val intention = it.getValue(IntentionRecord::class.java)
                if (intention?.type == intentionType) {
                    it.key
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addStatsListener(projectId: String, onStatsUpdate: (ProjectStats) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val stats = snapshot.getValue(ProjectStats::class.java)
                stats?.let(onStatsUpdate)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }
        projectsRef.child(projectId).child("stats").addValueEventListener(listener)
        return listener
    }

    fun removeStatsListener(projectId: String, listener: ValueEventListener) {
        projectsRef.child(projectId).child("stats").removeEventListener(listener)
    }
}
