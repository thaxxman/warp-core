package com.redshirt.warpcore.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.redshirt.warpcore.data.DeviceStatus
import com.redshirt.warpcore.data.Profile
import com.redshirt.warpcore.ble.BleConnectionState
import com.redshirt.warpcore.ui.screens.*
import java.io.File

data class NavScreen(
    val route: String,
    val label: String,
    val icon: ImageVector
)

object Screens {
    val Connection = NavScreen("connection", "Connect", Icons.Filled.Bluetooth)
    val Control = NavScreen("control", "Control", Icons.Filled.Thermostat)
    val Profiles = NavScreen("profiles", "Profiles", Icons.Filled.Bookmark)
    val Settings = NavScreen("settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun NavGraph(
    connectionState: BleConnectionState,
    deviceStatus: DeviceStatus,
    profiles: List<Profile>,
    useFahrenheit: Boolean,
    unitLabel: String,
    toDisplay: (Int) -> Int,
    fromDisplay: (Int) -> Int,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onTempChange: (Int) -> Unit,
    onArmToggle: (Boolean) -> Unit,
    onEStop: () -> Unit,
    onGetStatus: () -> Unit,
    onSaveProfile: (String, Int) -> Unit,
    onApplyProfile: (Int) -> Unit,
    onDeleteProfile: (Profile) -> Unit,
    onUnitToggle: (Boolean) -> Unit,
    tempStepSize: Int,
    onStepSizeChange: (Int) -> Unit,
    onSetPid: (Double, Double, Double) -> Unit,
    onExportSession: (Long) -> File?,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState == BleConnectionState.CONNECTED

    if (!isConnected) {
        // Not connected: Show connection screen full-screen (no bottom bar)
        ConnectionScreen(
            connectionState = connectionState,
            onConnect = onConnect,
            onDisconnect = onDisconnect
        )
    } else {
        // Connected: Show tabbed interface with Control, Profiles, Settings
        ConnectedNavHost(
            connectionState = connectionState,
            deviceStatus = deviceStatus,
            profiles = profiles,
            useFahrenheit = useFahrenheit,
            unitLabel = unitLabel,
            toDisplay = toDisplay,
            fromDisplay = fromDisplay,
            tempStepSize = tempStepSize,
            onDisconnect = onDisconnect,
            onTempChange = onTempChange,
            onArmToggle = onArmToggle,
            onEStop = onEStop,
            onGetStatus = onGetStatus,
            onSaveProfile = onSaveProfile,
            onApplyProfile = onApplyProfile,
            onDeleteProfile = onDeleteProfile,
            onUnitToggle = onUnitToggle,
            onStepSizeChange = onStepSizeChange,
            onSetPid = onSetPid,
            onExportSession = onExportSession,
            modifier = modifier
        )
    }
}

/**
 * Tabbed navigation shown when connected to the device.
 * Bottom bar: Control | Profiles | Settings
 */
@Composable
private fun ConnectedNavHost(
    connectionState: BleConnectionState,
    deviceStatus: DeviceStatus,
    profiles: List<Profile>,
    useFahrenheit: Boolean,
    unitLabel: String,
    toDisplay: (Int) -> Int,
    fromDisplay: (Int) -> Int,
    tempStepSize: Int,
    onDisconnect: () -> Unit,
    onTempChange: (Int) -> Unit,
    onArmToggle: (Boolean) -> Unit,
    onEStop: () -> Unit,
    onGetStatus: () -> Unit,
    onSaveProfile: (String, Int) -> Unit,
    onApplyProfile: (Int) -> Unit,
    onDeleteProfile: (Profile) -> Unit,
    onUnitToggle: (Boolean) -> Unit,
    onStepSizeChange: (Int) -> Unit,
    onSetPid: (Double, Double, Double) -> Unit,
    onExportSession: (Long) -> File?,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val connectedScreens = listOf(Screens.Control, Screens.Profiles, Screens.Settings)

    Scaffold(
        modifier = modifier.statusBarsPadding(),
        bottomBar = {
            NavigationBar {
                connectedScreens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screens.Control.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screens.Control.route) {
                ControlScreen(
                    status = deviceStatus,
                    connectionState = connectionState,
                    unitLabel = unitLabel,
                    toDisplay = toDisplay,
                    fromDisplay = fromDisplay,
                    tempStepSize = tempStepSize,
                    onTempChange = onTempChange,
                    onArmToggle = onArmToggle,
                    onEStop = onEStop,
                    onGetStatus = onGetStatus
                )
            }
            composable(Screens.Profiles.route) {
                ProfileScreen(
                    profiles = profiles,
                    unitLabel = unitLabel,
                    toDisplay = toDisplay,
                    onSaveProfile = onSaveProfile,
                    onApplyProfile = onApplyProfile,
                    onDeleteProfile = onDeleteProfile
                )
            }
            composable(Screens.Settings.route) {
                SettingsScreen(
                    useFahrenheit = useFahrenheit,
                    onUnitToggle = onUnitToggle,
                    tempStepSize = tempStepSize,
                    onStepSizeChange = onStepSizeChange,
                    onExportSession = onExportSession,
                    onSetPid = onSetPid
                )
            }
        }
    }
}