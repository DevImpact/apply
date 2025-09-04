package com.crowdfunding.data

import com.crowdfunding.ui.projects.IntentionRecord
import com.facebook.AccessToken
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val activated: Boolean = false,
    val facebookName: String? = null,
    val facebookPhotoUrl: String? = null
)

class AuthRepository {
    private val auth = Firebase.auth
    private val rootRef = Firebase.database.reference
    private val usersRef = rootRef.child("users")
    private val userIntentionsRef = rootRef.child("userIntentions")

    fun getAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser).isSuccess
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val userProfileListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(UserProfile::class.java)).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        val ref = usersRef.child(userId)
        ref.addValueEventListener(userProfileListener)
        awaitClose { ref.removeEventListener(userProfileListener) }
    }

    fun getUserIntentionForProject(userId: String, projectId: String): Flow<IntentionRecord?> = callbackFlow {
        val intentionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(IntentionRecord::class.java)).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        val ref = userIntentionsRef.child(userId).child(projectId)
        ref.addValueEventListener(intentionListener)
        awaitClose { ref.removeEventListener(intentionListener) }
    }

    suspend fun signIn(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUser(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(fullName: String, activated: Boolean): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("User not logged in")
            val userProfile = UserProfile(uid = user.uid, fullName = fullName, email = user.email ?: "", activated = activated)
            usersRef.child(user.uid).setValue(userProfile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val dataSnapshot = usersRef.child(userId).get().await()
            dataSnapshot.getValue(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun linkFacebookAccount(token: AccessToken): Result<Unit> {
        return try {
            val credential = FacebookAuthProvider.getCredential(token.token)
            val user = auth.currentUser ?: throw IllegalStateException("User not logged in")

            // Link the credential to the existing user
            user.linkWithCredential(credential).await()

            // After successful linking, save the provider info to the database
            val providerData = mapOf(
                "linked" to true,
                "appScopedId" to token.userId,
                "linkedAt" to System.currentTimeMillis()
            )
            usersRef.child(user.uid).child("providers").child("facebook").setValue(providerData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserFacebookProfile(name: String, photoUrl: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("User not logged in")
            val updates = mapOf(
                "facebookName" to name,
                "facebookPhotoUrl" to photoUrl
            )
            usersRef.child(user.uid).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
