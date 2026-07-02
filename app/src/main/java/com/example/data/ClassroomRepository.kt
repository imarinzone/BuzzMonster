package com.example.data

import kotlinx.coroutines.flow.Flow

class ClassroomRepository(private val classroomDao: ClassroomDao) {

    val allSessionLogs: Flow<List<SessionLog>> = classroomDao.getAllSessionLogs()
    val allNoiseLogs: Flow<List<NoiseLog>> = classroomDao.getAllNoiseLogs()

    fun getNoiseLogsForSession(sessionId: Long): Flow<List<NoiseLog>> {
        return classroomDao.getNoiseLogsForSession(sessionId)
    }

    fun getSessionLogsForDay(dateString: String): Flow<List<SessionLog>> {
        return classroomDao.getSessionLogsForDay(dateString)
    }

    suspend fun insertNoiseLog(noiseLog: NoiseLog) {
        classroomDao.insertNoiseLog(noiseLog)
    }

    suspend fun insertSessionLog(sessionLog: SessionLog) {
        classroomDao.insertSessionLog(sessionLog)
    }

    suspend fun clearAllData() {
        classroomDao.clearAllNoiseLogs()
        classroomDao.clearAllSessionLogs()
    }

    suspend fun deleteNoiseLogsForSession(sessionId: Long) {
        classroomDao.deleteNoiseLogsForSession(sessionId)
    }
}
