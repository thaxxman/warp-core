package com.redshirt.warpcore.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved temperature profile.
 * Stored locally on the app — not synced to ESP32.
 */
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    /** Target temp stored in Celsius, displayed in user's preferred unit */
    val targetTempCelsius: Int,
    val createdAt: Long = System.currentTimeMillis()
)