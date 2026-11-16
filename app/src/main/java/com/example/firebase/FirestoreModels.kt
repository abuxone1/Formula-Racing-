package com.example.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val driverName: String = "",
    val teamName: String = "",
    val helmetColor: String = "#00E5FF",
    val carColor: String = "#FF1801",
    val accentColor: String = "#00E5FF",
    val racesCompleted: Long = 0L,
    val wins: Long = 0L,
    val podiums: Long = 0L,
    val championshipPoints: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val driverName: String = "",
    val teamName: String = "",
    val trackId: String = "",
    val lapTimeSec: Double = 0.0,
    val topSpeedKmh: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)
