package com.redshirt.warpcore.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.redshirt.warpcore.data.DeviceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * BLE Manager for Warp Core eHMD.
 *
 * Handles scanning, connection with retry, GATT 133 recovery,
 * and characteristic operations using raw Android BLE APIs.
 *
 * Connection flow:
 *   1. Scan for device named "WarpCore*" (2-minute advertising window on ESP32)
 *   2. Direct connect (autoConnect=false) with 8s timeout
 *   3. On failure: close GATT, wait 1s, retry with autoConnect=true
 *   4. On GATT connect: discover services first, then request MTU
 *   5. On services discovered: find characteristics, enable notifications
 *   6. Only then set state to CONNECTED
 */
@Suppress("DEPRECATION")
class WarpCoreBleManager(private val context: Context) {

    companion object {
        private const val TAG = "WarpCoreBle"
        const val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
        const val WRITE_CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
        const val NOTIFY_CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a9"
        const val CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"
        private const val DEVICE_NAME = "WarpCore-eHMD"
        private const val SCAN_TIMEOUT_MS = 15000L
        private const val CONNECT_TIMEOUT_MS = 8000L
        private const val MAX_RETRIES = 2
        private const val MTU_REQUEST = 247  // Safe value for ESP32 NimBLE (max 517, but 247 is reliable)
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var bleScanner: BluetoothLeScanner? = null
    private val handler = Handler(Looper.getMainLooper())

    private var targetDeviceAddress: String? = null
    private var retryCount = 0
    private var isRetrying = false

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _deviceStatus = MutableStateFlow(DeviceStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    // Runnable references for timeout/cleanup
    private var scanTimeoutRunnable: Runnable? = null
    private var connectTimeoutRunnable: Runnable? = null

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i(TAG, "onConnectionStateChange: status=$status newState=$newState retry=$retryCount")

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    // status == 0 means GATT_SUCCESS
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i(TAG, "GATT connected — discovering services...")
                        cancelConnectTimeout()

                        // Small delay before service discovery to let the BLE stack settle.
                        // ESP32 NimBLE can reject rapid GATT requests after connect.
                        handler.postDelayed({
                            try {
                                gatt.discoverServices()
                            } catch (e: SecurityException) {
                                Log.e(TAG, "SecurityException discovering services: ${e.message}")
                                _connectionState.value = BleConnectionState.DISCONNECTED
                                closeGatt(gatt)
                            }
                        }, 300)

                        _connectionState.value = BleConnectionState.CONNECTING
                    } else {
                        // Connected but with error status — treat as failure
                        Log.w(TAG, "GATT connected with error status $status — closing")
                        handleGattError(gatt, status)
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    cancelConnectTimeout()
                    Log.w(TAG, "GATT disconnected — status=$status retry=$retryCount")

                    if (status == 133 && retryCount < MAX_RETRIES) {
                        // GATT 133: The infamous Android BLE error. Retry with autoConnect.
                        Log.i(TAG, "GATT 133 — will retry (attempt ${retryCount + 1}/$MAX_RETRIES)")
                        closeGatt(gatt)
                        retryCount++
                        handler.postDelayed({ retryConnect() }, 1000)
                    } else if (status == 8 || status == 19 || status == 22) {
                        // status 8 = connection timeout, 19 = remote device terminated, 22 = local host terminated
                        Log.w(TAG, "Connection lost (status=$status) — giving up")
                        closeGatt(gatt)
                        _connectionState.value = BleConnectionState.DISCONNECTED
                    } else {
                        Log.w(TAG, "Disconnected with status=$status")
                        closeGatt(gatt)
                        _connectionState.value = BleConnectionState.DISCONNECTED
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed: status=$status")
                // Some devices succeed on retry
                if (retryCount < MAX_RETRIES) {
                    retryCount++
                    closeGatt(gatt)
                    handler.postDelayed({ retryConnect() }, 500)
                } else {
                    gatt.disconnect()
                    _connectionState.value = BleConnectionState.DISCONNECTED
                }
                return
            }

            val service = gatt.getService(UUID.fromString(SERVICE_UUID))
            if (service == null) {
                Log.e(TAG, "Warp Core service not found — available services:")
                gatt.services.forEach { Log.w(TAG, "  ${it.uuid}") }
                gatt.disconnect()
                _connectionState.value = BleConnectionState.DISCONNECTED
                return
            }

            writeCharacteristic = service.getCharacteristic(UUID.fromString(WRITE_CHAR_UUID))
            notifyCharacteristic = service.getCharacteristic(UUID.fromString(NOTIFY_CHAR_UUID))

            if (writeCharacteristic == null || notifyCharacteristic == null) {
                Log.e(TAG, "Required characteristics not found")
                gatt.disconnect()
                _connectionState.value = BleConnectionState.DISCONNECTED
                return
            }

            Log.i(TAG, "Services discovered — requesting MTU...")
            // Request MTU AFTER service discovery (not before — reduces GATT 133 risk)
            try {
                gatt.requestMtu(MTU_REQUEST)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException requesting MTU: ${e.message}")
                // Continue without MTU adjustment
                enableNotifications(gatt)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.i(TAG, "MTU negotiated: $mtu (status=$status)")
            // Now enable notifications after MTU is set
            enableNotifications(gatt)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Notifications enabled — CONNECTED")
                try {
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                } catch (e: SecurityException) {
                    Log.w(TAG, "SecurityException setting priority: ${e.message}")
                }
                _connectionState.value = BleConnectionState.CONNECTED
                retryCount = 0  // Reset retry count on successful connection
            } else {
                Log.e(TAG, "Descriptor write failed: status=$status")
                // Retry enabling notifications once
                enableNotifications(gatt)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == UUID.fromString(NOTIFY_CHAR_UUID)) {
                val json = String(value, Charsets.UTF_8)
                Log.d(TAG, "Received: $json")
                val status = BleMessageParser.parseStatus(json)
                if (status != null) {
                    _deviceStatus.value = status
                }
            }
        }

        // Legacy callback for Android < 13
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == UUID.fromString(NOTIFY_CHAR_UUID)) {
                val json = characteristic.getStringValue(0)
                Log.d(TAG, "Received (legacy): $json")
                val status = BleMessageParser.parseStatus(json)
                if (status != null) {
                    _deviceStatus.value = status
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Write successful")
            } else {
                Log.w(TAG, "Write failed: status=$status")
            }
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt) {
        val notifyChar = notifyCharacteristic ?: return
        try {
            gatt.setCharacteristicNotification(notifyChar, true)
            val descriptor = notifyChar.getDescriptor(UUID.fromString(CCCD_UUID))
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            } else {
                Log.w(TAG, "CCCD descriptor not found on notify characteristic")
                // Still set connected — we just won't get notifications
                _connectionState.value = BleConnectionState.CONNECTED
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException enabling notifications: ${e.message}")
            _connectionState.value = BleConnectionState.CONNECTED  // Connected but no notifications
        }
    }

    private fun handleGattError(gatt: BluetoothGatt, status: Int) {
        if (retryCount < MAX_RETRIES) {
            retryCount++
            closeGatt(gatt)
            handler.postDelayed({ retryConnect() }, 1000)
        } else {
            closeGatt(gatt)
            _connectionState.value = BleConnectionState.DISCONNECTED
        }
    }

    // ========================================================================
    // BLE Scan
    // ========================================================================

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name ?: ""
            if (deviceName.contains("WarpCore", ignoreCase = true)) {
                Log.i(TAG, "Found device: $deviceName [${result.device.address}]")
                stopScan()
                targetDeviceAddress = result.device.address
                connectToDevice(result.device.address, autoConnect = false)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed: $errorCode")
            _connectionState.value = BleConnectionState.DISCONNECTED
        }
    }

    /**
     * Start scanning for the Warp Core eHMD device.
     * Call this ONLY after verifying BLUETOOTH_SCAN permission is granted.
     */
    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "Bluetooth not available or not enabled")
            return
        }

        bleScanner = bluetoothAdapter!!.bluetoothLeScanner
        if (bleScanner == null) {
            Log.e(TAG, "BLE scanner not available")
            return
        }

        // Cancel any previous connection attempt
        disconnect()

        _connectionState.value = BleConnectionState.SCANNING
        retryCount = 0
        Log.i(TAG, "Starting BLE scan for $DEVICE_NAME...")

        try {
            bleScanner!!.startScan(scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting scan: ${e.message}")
            _connectionState.value = BleConnectionState.DISCONNECTED
            return
        }

        // Auto-stop scan after timeout
        scanTimeoutRunnable = Runnable {
            if (_connectionState.value == BleConnectionState.SCANNING) {
                Log.w(TAG, "Scan timed out — device not found")
                stopScan()
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
        }
        handler.postDelayed(scanTimeoutRunnable!!, SCAN_TIMEOUT_MS)
    }

    fun stopScan() {
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "Stop scan failed: ${e.message}")
        }
        scanTimeoutRunnable?.let { handler.removeCallbacks(it) }
        scanTimeoutRunnable = null
    }

    // ========================================================================
    // Connection
    // ========================================================================

    private fun connectToDevice(address: String, autoConnect: Boolean) {
        _connectionState.value = BleConnectionState.CONNECTING
        val device = bluetoothAdapter!!.getRemoteDevice(address)

        // TRANSPORT_LE forces BLE transport — prevents Android from trying
        // classic Bluetooth which causes 10s timeout then disconnect
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, autoConnect, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, autoConnect, gattCallback)
        }

        Log.i(TAG, "Connecting to $address (autoConnect=$autoConnect)")

        // Set a timeout — if no state change in N seconds, close and retry
        connectTimeoutRunnable = Runnable {
            Log.w(TAG, "Connection timeout — no GATT response")
            bluetoothGatt?.let { gatt ->
                try {
                    gatt.disconnect()
                } catch (e: SecurityException) { /* already gone */ }
            }
        }
        handler.postDelayed(connectTimeoutRunnable!!, CONNECT_TIMEOUT_MS)
    }

    private fun retryConnect() {
        val address = targetDeviceAddress
        if (address == null) {
            Log.e(TAG, "No target device address for retry")
            _connectionState.value = BleConnectionState.DISCONNECTED
            return
        }

        Log.i(TAG, "Retrying connection to $address (autoConnect=true, attempt $retryCount)")
        isRetrying = true
        connectToDevice(address, autoConnect = true)
    }

    private fun cancelConnectTimeout() {
        connectTimeoutRunnable?.let { handler.removeCallbacks(it) }
        connectTimeoutRunnable = null
    }

    private fun closeGatt(gatt: BluetoothGatt) {
        try {
            gatt.close()
        } catch (e: SecurityException) { /* ignore */ }
        if (bluetoothGatt == gatt) {
            bluetoothGatt = null
        }
    }

    fun disconnect() {
        stopScan()
        cancelConnectTimeout()
        retryCount = MAX_RETRIES  // Prevent retries after manual disconnect
        _connectionState.value = BleConnectionState.DISCONNECTING
        bluetoothGatt?.let { gatt ->
            try {
                gatt.disconnect()
            } catch (e: SecurityException) { /* ignore */ }
            // Don't call close() here — onConnectionStateChange will do it
        }
    }

    // ========================================================================
    // Write Commands
    // ========================================================================

    private fun writeCommand(json: String) {
        val char = writeCharacteristic ?: return
        val gatt = bluetoothGatt ?: return
        try {
            char.value = json.toByteArray(Charsets.UTF_8)
            // Write without response — matches ESP32's PROPERTY_WRITE_NR
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            gatt.writeCharacteristic(char)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException writing characteristic: ${e.message}")
        }
    }

    fun sendSetTemp(tempCelsius: Int) {
        writeCommand(BleMessageParser.buildSetTemp(tempCelsius))
    }

    fun sendArm(armed: Boolean) {
        writeCommand(BleMessageParser.buildArm(armed))
    }

    fun sendEStop() {
        writeCommand(BleMessageParser.buildEStop())
    }

    fun sendGetStatus() {
        writeCommand(BleMessageParser.buildGetStatus())
    }

    fun sendSetPid(kp: Double, ki: Double, kd: Double) {
        writeCommand(BleMessageParser.buildSetPid(kp, ki, kd))
    }

    fun isConnected(): Boolean = _connectionState.value == BleConnectionState.CONNECTED
}