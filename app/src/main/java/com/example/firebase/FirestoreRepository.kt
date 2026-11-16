package com.example.firebase

import android.content.Context
import com.example.R
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreRepository(
    private val db: FirebaseFirestore
) {
    // Secondary constructor resolving databaseId from R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docRef = db.collection("users").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveUserProfile(profile: UserProfile): Boolean {
        require(profile.userId.isNotBlank()) { "User ID cannot be blank" }
        val docRef = db.collection("users").document(profile.userId)
        return try {
            val payload = mapOf(
                "userId" to profile.userId,
                "driverName" to profile.driverName,
                "teamName" to profile.teamName,
                "helmetColor" to profile.helmetColor,
                "carColor" to profile.carColor,
                "accentColor" to profile.accentColor,
                "racesCompleted" to profile.racesCompleted,
                "wins" to profile.wins,
                "podiums" to profile.podiums,
                "championshipPoints" to profile.championshipPoints,
                "createdAt" to (profile.createdAt ?: com.google.firebase.Timestamp.now()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).awaitTask()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            false
        }
    }

    fun observeLeaderboard(trackId: String): Flow<List<LeaderboardEntry>> = callbackFlow {
        val query = db.collection("leaderboard")
            .whereEqualTo("trackId", trackId)
            .limit(20)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "leaderboard")
                close(error)
                return@addSnapshotListener
            }
            val entries = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }?.sortedBy { it.lapTimeSec } ?: emptyList()

            trySend(entries)
        }
        awaitClose { registration.remove() }
    }

    suspend fun submitLapTime(entry: LeaderboardEntry): Boolean {
        require(entry.userId.isNotBlank()) { "User ID cannot be blank" }
        val docRef = db.collection("leaderboard").document()
        return try {
            val payload = mapOf(
                "userId" to entry.userId,
                "driverName" to entry.driverName,
                "teamName" to entry.teamName,
                "trackId" to entry.trackId,
                "lapTimeSec" to entry.lapTimeSec,
                "topSpeedKmh" to entry.topSpeedKmh,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).awaitTask()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            false
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { result -> cont.resume(result) }
            addOnFailureListener { exception -> cont.resumeWithException(exception) }
        }
}
