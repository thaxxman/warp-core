package com.redshirt.warpcore.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.redshirt.warpcore.ble.BleConnectionState
import com.redshirt.warpcore.data.DeviceStatus

/**
 * Debug screen — shows raw PID internals, voltage, and BLE status.
 * Useful for diagnosing PWM output issues, PID tuning, and sensor faults.
 */
@Composable
fun DebugScreen(
    status: DeviceStatus,
    connectionState: BleConnectionState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Connection status header
        Text(
            text = "DEBUG MONITOR",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        val connColor = when (connectionState) {
            BleConnectionState.CONNECTED -> Color(0xFF44FF88)
            BleConnectionState.CONNECTING -> Color(0xFFFFAA00)
            BleConnectionState.SCANNING -> Color(0xFFFFAA00)
            else -> Color(0xFFFF4444)
        }
        Text(
            text = "BLE: ${connectionState.name}",
            style = MaterialTheme.typography.bodyLarge,
            color = connColor
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant)

        // PID Output section
        DebugSection(title = "PID OUTPUT") {
            DebugRow("PWM %", "${status.pwm}%")
            DebugRow("PID Raw (0-255)", "${status.pidRaw}")
            DebugRow("PID Raw (hex)", "0x${status.pidRaw.toString(16).uppercase()}")
            DebugRow("PWM Voltage (est)", "%.2f V".format(status.pidRaw / 255.0 * 3.3))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Temperature section
        DebugSection(title = "TEMPERATURE") {
            DebugRow("Set Temp", "${status.tempSet} °C")
            DebugRow("Actual Temp", "${status.tempActual} °C")
            DebugRow("Error (set-actual)", "%.1f °C".format(status.debugError))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant)

        // PID Tuning section
        DebugSection(title = "PID TUNING") {
            DebugRow("Kp", "%.2f".format(status.debugKp))
            DebugRow("Ki", "%.2f".format(status.debugKi))
            DebugRow("Kd", "%.2f".format(status.debugKd))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant)

        // System section
        DebugSection(title = "SYSTEM") {
            DebugRow("Armed", if (status.armed) "YES" else "NO")
            DebugRow("Battery", "${status.battery}%")
            DebugRow("Voltage", "%.2f V".format(status.debugVoltage))
            DebugRow("Session", status.sessionFormatted)
            DebugRow("Status", status.status)
            if (status.msg != null) {
                DebugRow("Error", status.msg)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Raw data section
        DebugSection(title = "RAW DATA") {
            Text(
                text = buildString {
                    appendLine("{")
                    appendLine("  \"status\": \"${status.status}\",")
                    appendLine("  \"temp_set\": ${status.tempSet},")
                    appendLine("  \"temp_actual\": ${status.tempActual},")
                    appendLine("  \"armed\": ${if (status.armed) 1 else 0},")
                    appendLine("  \"pwm\": ${status.pwm},")
                    appendLine("  \"pid_raw\": ${status.pidRaw},")
                    appendLine("  \"battery\": ${status.battery},")
                    appendLine("  \"session\": ${status.session},")
                    appendLine("  \"dbg\": {")
                    appendLine("    \"kp\": ${status.debugKp},")
                    appendLine("    \"ki\": ${status.debugKi},")
                    appendLine("    \"kd\": ${status.debugKd},")
                    appendLine("    \"err\": ${status.debugError},")
                    appendLine("    \"pwm_raw\": ${status.debugPwmRaw},")
                    appendLine("    \"voltage\": ${status.debugVoltage}")
                    appendLine("  }")
                    append("}")
                },
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DebugSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    content()
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}