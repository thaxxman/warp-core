package com.redshirt.warpcore.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * Settings screen — temperature unit, PID tuning, data export.
 */
@Composable
fun SettingsScreen(
    useFahrenheit: Boolean,
    onUnitToggle: (Boolean) -> Unit,
    tempStepSize: Int,
    onStepSizeChange: (Int) -> Unit,
    onExportSession: (Long) -> File?,
    onSetPid: (Double, Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPidDialog by remember { mutableStateOf(false) }
    var kpInput by remember { mutableStateOf("2.0") }
    var kiInput by remember { mutableStateOf("5.0") }
    var kdInput by remember { mutableStateOf("1.0") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // Temperature Unit
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Temperature Unit", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (useFahrenheit) "Displaying in °F" else "Displaying in °C",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useFahrenheit,
                    onCheckedChange = onUnitToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = androidx.compose.ui.graphics.Color(0xFF44FF88)
                    )
                )
            }
        }

        // Temperature Step Size
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Step Size", style = MaterialTheme.typography.titleMedium)
                Text(
                    "±$tempStepSize° per button tap on the control screen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..10).forEach { step ->
                        FilterChip(
                            selected = tempStepSize == step,
                            onClick = { onStepSizeChange(step) },
                            label = { Text("$step") }
                        )
                    }
                }
            }
        }

        // PID Tuning
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PID Tuning", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Adjust Kp, Ki, Kd parameters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = { showPidDialog = true }) {
                    Text("EDIT")
                }
            }
        }

        // Safety Info
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Safety Limits", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Maximum temperature: 400°C / 752°F", style = MaterialTheme.typography.bodyMedium)
                Text("• Thermal runaway: Auto-shutdown at +50°C over target", style = MaterialTheme.typography.bodyMedium)
                Text("• Emergency stop available on control screen", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // About
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Warp Core eHMD v1.0", style = MaterialTheme.typography.bodyMedium)
                Text("Electronic Heat Management Device", style = MaterialTheme.typography.bodyMedium)
                Text("BLE Protocol: See docs/BLE_PROTOCOL.md", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Donate
        val context = LocalContext.current
        TextButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/GThaxton"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "🪝 Buy me a Bowl",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    // PID tuning dialog
    if (showPidDialog) {
        AlertDialog(
            onDismissRequest = { showPidDialog = false },
            title = { Text("PID Tuning") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kpInput,
                        onValueChange = { kpInput = it },
                        label = { Text("Kp (Proportional)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = kiInput,
                        onValueChange = { kiInput = it },
                        label = { Text("Ki (Integral)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = kdInput,
                        onValueChange = { kdInput = it },
                        label = { Text("Kd (Derivative)") },
                        singleLine = true
                    )
                    Text(
                        "⚠ These values are saved to the device. Incorrect values may cause unstable heating.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val kp = kpInput.toDoubleOrNull() ?: 2.0
                    val ki = kiInput.toDoubleOrNull() ?: 5.0
                    val kd = kdInput.toDoubleOrNull() ?: 1.0
                    onSetPid(kp, ki, kd)
                    showPidDialog = false
                }) { Text("APPLY") }
            },
            dismissButton = {
                TextButton(onClick = { showPidDialog = false }) { Text("CANCEL") }
            }
        )
    }
}