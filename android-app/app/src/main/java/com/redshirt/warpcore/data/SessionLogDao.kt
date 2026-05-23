package com.redshirt.warpcore.data

import androidx.room.*

@Dao
interface SessionLogDao {
    @Insert
    suspend fun insert(log: SessionLog)

    @Query("SELECT * FROM session_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySession(sessionId: Long): List<SessionLog>

    @Query("SELECT DISTINCT sessionId FROM session_logs ORDER BY sessionId DESC")
    suspend fun getSessionIds(): List<Long>

    @Query("DELETE FROM session_logs WHERE timestamp < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("DELETE FROM session_logs WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: Long)
}