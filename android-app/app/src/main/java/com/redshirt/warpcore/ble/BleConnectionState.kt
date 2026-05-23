package com.redshirt.warpcore.ble

/**
 * BLE connection states for the eHMD device.
 */
enum class BleConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}