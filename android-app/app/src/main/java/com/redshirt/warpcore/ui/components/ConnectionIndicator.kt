package com.redshirt.warpcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.redshirt.warpcore.ble.BleConnectionState

/**
 * Connection status indicator shown at the top of all screens.
 * Shows current BLE connection state with color coding.
 */
@Composable
fun ConnectionIndicator(
    connectionState: BleConnectionState,
    deviceName: String = "Warp Core eHMD",
    modifier: Modifier = Modifier
) {
    val (text, color) = when (connectionState) {
        BleConnectionState.CONNECTED -> "● CONNECTED — $deviceName" to androidx.compose.ui.graphics.Color(0xFF44FF88)
        BleConnectionState.CONNECTING -> "○ CONNECTING..." to androidx.compose.ui.graphics.Color(0xFFFFFF44)
        BleConnectionState.SCANNING -> "○ SCANNING..." to androidx.compose.ui.graphics.Color(0xFFFFFF44)
        BleConnectionState.DISCONNECTING -> "○ DISCONNECTING..." to androidx.compose.ui.graphics.Color(0xFFFF8844)
        BleConnectionState.DISCONNECTED -> "○ DISCONNECTED" to androidx.compose.ui.graphics.Color(0xFFFF4444)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = androidx.compose.ui.graphics.Color(0xFF1A1A2E),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
        }
    }
}