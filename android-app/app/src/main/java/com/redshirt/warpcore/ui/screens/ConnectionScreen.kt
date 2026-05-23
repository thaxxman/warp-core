package com.redshirt.warpcore.ui.screens

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redshirt.warpcore.ble.BleConnectionState
import com.redshirt.warpcore.viewmodel.ConnectionViewModel

@Composable
fun ConnectionScreen(
    connectionState: BleConnectionState,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ConnectionViewModel = viewModel()
    val context = LocalContext.current

    val permissionsGranted by viewModel.permissionsGranted.collectAsState()
    val state by viewModel.connectionState.collectAsState()

    // Android 12+: only SCAN + CONNECT (neverForLocation = no location needed)
    // Pre-Android 12: BLUETOOTH + BLUETOOTH_ADMIN + ACCESS_FINE_LOCATION
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.updatePermissions(allGranted)
    }

    // Check permissions on first composition
    LaunchedEffect(Unit) {
        viewModel.updatePermissions(viewModel.hasBlePermissions())
        if (!viewModel.hasBlePermissions()) {
            permissionLauncher.launch(permissions)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "WARP CORE eHMD",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Electronic Heat Management Device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when (state) {
            BleConnectionState.CONNECTED -> {
                Text(
                    text = "Connection established. Engaging...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = androidx.compose.ui.graphics.Color(0xFF44FF88)
                )
                OutlinedButton(onClick = { viewModel.disconnect() }) {
                    Text("DISCONNECT")
                }
            }
            BleConnectionState.CONNECTING -> {
                CircularProgressIndicator()
                Text("Connecting to device...", style = MaterialTheme.typography.bodyLarge)
            }
            BleConnectionState.SCANNING -> {
                CircularProgressIndicator()
                Text("Scanning for Warp Core eHMD...", style = MaterialTheme.typography.bodyLarge)
            }
            BleConnectionState.DISCONNECTING -> {
                CircularProgressIndicator()
                Text("Disconnecting...", style = MaterialTheme.typography.bodyLarge)
            }
            BleConnectionState.DISCONNECTED -> {
                if (!permissionsGranted) {
                    Button(onClick = { permissionLauncher.launch(permissions) }) {
                        Text("GRANT BLUETOOTH PERMISSIONS")
                    }
                    Text(
                        text = "Bluetooth permissions are required to scan and connect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    val btEnabled = btManager?.adapter?.isEnabled == true

                    if (btEnabled) {
                        Button(
                            onClick = { viewModel.connect() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CONNECT TO WARP CORE")
                        }
                        Text(
                            text = "Make sure the device is powered on (BLE advertises for 2 min after boot)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Bluetooth is disabled. Please enable Bluetooth.",
                            color = androidx.compose.ui.graphics.Color(0xFFFF4444)
                        )
                    }
                }
            }
        }
    }
}