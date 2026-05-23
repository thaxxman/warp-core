package com.redshirt.warpcore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redshirt.warpcore.data.AppDatabase
import com.redshirt.warpcore.data.Profile
import com.redshirt.warpcore.data.SessionLog
import com.redshirt.warpcore.data.DeviceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for app settings, profiles, and data logging.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    // ===== Temperature Unit =====
    private val _useFahrenheit = MutableStateFlow(false)
    val useFahrenheit: StateFlow<Boolean> = _useFahrenheit.asStateFlow()

    fun setUseFahrenheit(useF: Boolean) {
        _useFahrenheit.value = useF
    }

    /** Convert Celsius to the user's preferred unit */
    fun toDisplayTemp(tempC: Int): Int {
        return if (_useFahrenheit.value) {
            (tempC * 9 / 5 + 32)
        } else {
            tempC
        }
    }

    /** Convert from the user's preferred unit to Celsius */
    fun fromDisplayTemp(tempDisplay: Int): Int {
        return if (_useFahrenheit.value) {
            ((tempDisplay - 32) * 5 / 9)
        } else {
            tempDisplay
        }
    }

    fun getUnitLabel(): String = if (_useFahrenheit.value) "°F" else "°C"

    // ===== Temperature Step Size =====
    private val _tempStepSize = MutableStateFlow(1)
    val tempStepSize: StateFlow<Int> = _tempStepSize.asStateFlow()

    fun setTempStepSize(step: Int) {
        _tempStepSize.value = step.coerceIn(1, 10)
    }

    // ===== Profiles =====
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            db.profileDao().getAll().let { _profiles.value = it }
        }
    }

    fun refreshProfiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _profiles.value = db.profileDao().getAll()
        }
    }

    fun saveProfile(name: String, targetTempCelsius: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            db.profileDao().insert(Profile(name = name, targetTempCelsius = targetTempCelsius))
            refreshProfiles()
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch(Dispatchers.IO) {
            db.profileDao().delete(profile)
            refreshProfiles()
        }
    }

    // ===== Session Logging =====
    private var currentSessionId: Long = 0L

    fun startSession() {
        currentSessionId = System.currentTimeMillis()
    }

    fun stopSession() {
        currentSessionId = 0L
    }

    fun logDataPoint(status: DeviceStatus) {
        if (currentSessionId == 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            db.sessionLogDao().insert(
                SessionLog(
                    sessionId = currentSessionId,
                    timestamp = System.currentTimeMillis(),
                    tempSet = status.tempSet,
                    tempActual = status.tempActual,
                    pwm = status.pwm,
                    battery = status.battery,
                    armed = status.armed
                )
            )
        }
    }

    // ===== CSV Export =====
    fun exportSessionToCsv(sessionId: Long): File? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val fileName = "warpcore_session_${dateFormat.format(Date(sessionId))}.csv"
        val file = File(getApplication<Application>().getExternalFilesDir(null), fileName)

        return try {
            val logs = kotlinx.coroutines.runBlocking {
                db.sessionLogDao().getBySession(sessionId)
            }
            FileWriter(file).use { writer ->
                writer.append("timestamp,temp_set_c,temp_actual_c,pwm_pct,battery_pct,armed\n")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                for (log in logs) {
                    writer.append("${sdf.format(Date(log.timestamp))},${log.tempSet},${log.tempActual},${log.pwm},${log.battery},${log.armed}\n")
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    fun getSessionIds(): List<Long> {
        return kotlinx.coroutines.runBlocking {
            db.sessionLogDao().getSessionIds()
        }
    }
}