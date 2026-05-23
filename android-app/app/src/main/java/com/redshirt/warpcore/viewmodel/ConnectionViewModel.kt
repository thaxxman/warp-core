package com.redshirt.warpcore.viewmodel

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redshirt.warpcore.ble.BleConnectionState
import com.redshirt.warpcore.ble.WarpCoreBleManager
import com.redshirt.warpcore.data.DeviceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val bleManager = WarpCoreBleManager(application)

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    val deviceStatus: StateFlow<DeviceStatus> = bleManager.deviceStatus

    private val _permissionsGranted = MutableStateFlow(hasBlePermissions())
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    init {
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                _connectionState.value = state
            }
        }
    }

    fun updatePermissions(granted: Boolean) {
        _permissionsGranted.value = granted
    }

    fun hasBlePermissions(): Boolean {
        val context = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: Only SCAN + CONNECT needed (neverForLocation flag means no location required)
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-Android 12: Location permission required for BLE scanning
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun connectToDevice(address: String) {
        // Legacy — now we scan and auto-connect
        connect()
    }

    fun connect() {
        if (!hasBlePermissions()) {
            _connectionState.value = BleConnectionState.DISCONNECTED
            return
        }
        val btManager = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = BleConnectionState.DISCONNECTED
            return
        }
        bleManager.startScan()
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    // BLE command delegation
    fun sendSetTemp(tempCelsius: Int) = bleManager.sendSetTemp(tempCelsius)
    fun sendArm(armed: Boolean) = bleManager.sendArm(armed)
    fun sendEStop() = bleManager.sendEStop()
    fun sendGetStatus() = bleManager.sendGetStatus()
    fun sendSetPid(kp: Double, ki: Double, kd: Double) = bleManager.sendSetPid(kp, ki, kd)

    override fun onCleared() {
        super.onCleared()
        bleManager.disconnect()
    }
}