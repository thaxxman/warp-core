package com.redshirt.warpcore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.redshirt.warpcore.ble.BleConnectionState
import com.redshirt.warpcore.ui.theme.WarpCoreTheme
import com.redshirt.warpcore.ui.navigation.NavGraph
import com.redshirt.warpcore.viewmodel.ConnectionViewModel
import com.redshirt.warpcore.viewmodel.SettingsViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private val connectionVm: ConnectionViewModel by viewModels()
    private val settingsVm: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            WarpCoreTheme {
                val connectionState by connectionVm.connectionState.collectAsState()
                val deviceStatus by connectionVm.deviceStatus.collectAsState()
                val profiles by settingsVm.profiles.collectAsState()
                val useFahrenheit by settingsVm.useFahrenheit.collectAsState()
                val tempStepSize by settingsVm.tempStepSize.collectAsState()

                NavGraph(
                    connectionState = connectionState,
                    deviceStatus = deviceStatus,
                    profiles = profiles,
                    useFahrenheit = useFahrenheit,
                    unitLabel = settingsVm.getUnitLabel(),
                    toDisplay = { settingsVm.toDisplayTemp(it) },
                    fromDisplay = { settingsVm.fromDisplayTemp(it) },
                    onConnect = { address -> connectionVm.connectToDevice(address) },
                    onDisconnect = { connectionVm.disconnect() },
                    onTempChange = { temp -> connectionVm.sendSetTemp(temp) },
                    onArmToggle = { armed -> connectionVm.sendArm(armed) },
                    onEStop = { connectionVm.sendEStop() },
                    onGetStatus = { connectionVm.sendGetStatus() },
                    onSaveProfile = { name, temp -> settingsVm.saveProfile(name, temp) },
                    onApplyProfile = { temp -> connectionVm.sendSetTemp(temp) },
                    onDeleteProfile = { profile -> settingsVm.deleteProfile(profile) },
                    onUnitToggle = { useF -> settingsVm.setUseFahrenheit(useF) },
                    tempStepSize = tempStepSize,
                    onStepSizeChange = { step -> settingsVm.setTempStepSize(step) },
                    onSetPid = { kp, ki, kd -> connectionVm.sendSetPid(kp, ki, kd) },
                    onExportSession = { sessionId -> settingsVm.exportSessionToCsv(sessionId) }
                )
            }
        }
    }

    override fun onDestroy() {
        connectionVm.disconnect()
        super.onDestroy()
    }
}