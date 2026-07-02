package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassroomDao {
    @Query("SELECT * FROM noise_logs ORDER BY timestamp DESC")
    fun getAllNoiseLogs(): Flow<List<NoiseLog>>

    @Query("SELECT * FROM noise_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getNoiseLogsForSession(sessionId: Long): Flow<List<NoiseLog>>

    @Query("SELECT * FROM session_logs ORDER BY timestamp DESC")
    fun getAllSessionLogs(): Flow<List<SessionLog>>

    @Query("SELECT * FROM session_logs WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getSessionLogsForDay(dateString: String): Flow<List<SessionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoiseLog(noiseLog: NoiseLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionLog(sessionLog: SessionLog)

    @Query("DELETE FROM noise_logs")
    suspend fun clearAllNoiseLogs()

    @Query("DELETE FROM session_logs")
    suspend fun clearAllSessionLogs()

    @Query("DELETE FROM noise_logs WHERE sessionId = :sessionId")
    suspend fun deleteNoiseLogsForSession(sessionId: Long)
}
