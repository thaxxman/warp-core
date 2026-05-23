package com.redshirt.warpcore.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single data point logged during a session.
 * Used for session history and CSV export.
 */
@Entity(tableName = "session_logs")
data class SessionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Long,              // Unique session identifier (arm timestamp)
    val timestamp: Long,              // Unix millis of this reading
    val tempSet: Int,                  // Target temp in °C
    val tempActual: Int,               // Actual temp in °C
    val pwm: Int,                      // PWM percentage
    val battery: Int,                  // Battery percentage
    val armed: Boolean                 // Armed at time of reading
)