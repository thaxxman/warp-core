package com.redshirt.warpcore.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import com.redshirt.warpcore.ble.BleConnectionState
import com.redshirt.warpcore.data.DeviceStatus
import com.redshirt.warpcore.ui.components.*

/**
 * Main control screen — shown when connected to the device.
 * Contains temp slider, status display (with arm toggle), and emergency stop.
 */
@Composable
fun ControlScreen(
    status: DeviceStatus,
    connectionState: BleConnectionState,
    unitLabel: String,
    toDisplay: (Int) -> Int,
    fromDisplay: (Int) -> Int,
    tempStepSize: Int = 1,
    onTempChange: (Int) -> Unit,
    onArmToggle: (Boolean) -> Unit,
    onEStop: () -> Unit,
    onGetStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Request status periodically when connected
    LaunchedEffect(connectionState) {
        if (connectionState == BleConnectionState.CONNECTED) {
            while (true) {
                onGetStatus()
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Connection indicator
        ConnectionIndicator(connectionState = connectionState)

        // Status card (actual temp, PWM, battery, session, arm toggle, alerts)
        StatusCard(
            status = status,
            unitLabel = unitLabel,
            toDisplay = toDisplay,
            onArmToggle = onArmToggle
        )

        // Target temperature slider
        TempSlider(
            currentSetTempC = status.tempSet,
            onTempChange = onTempChange,
            unitLabel = unitLabel,
            minTempC = 0,
            maxTempC = 400,
            toDisplay = toDisplay,
            fromDisplay = fromDisplay,
            step = tempStepSize
        )

        // Emergency stop button (always visible, always active)
        EmergencyStopButton(onEStop = onEStop)
    }
}