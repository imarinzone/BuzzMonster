package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "noise_logs")
data class NoiseLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val amplitude: Float, // Normalized value between 0.0f and 100.0f
    val threshold: Float, // The active threshold when recorded
    val sessionId: Long // ID of the session it belongs to (0 if background/untracked)
)

@Entity(tableName = "session_logs")
data class SessionLog(
    @PrimaryKey val id: Long = System.currentTimeMillis(), // Unique session identifier
    val dateString: String, // "YYYY-MM-DD" for easy aggregation
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int, // The target quiet time
    val resetsCount: Int, // Number of times students exceeded threshold and reset the timer
    val successful: Boolean, // Whether they successfully finished the session
    val maxNoise: Float, // Maximum noise level during this session
    val averageNoise: Float // Average noise level during this session
)
