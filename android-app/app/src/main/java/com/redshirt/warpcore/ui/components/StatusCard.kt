package com.redshirt.warpcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redshirt.warpcore.data.DeviceStatus
import com.redshirt.warpcore.ui.theme.TrekBlue
import com.redshirt.warpcore.ui.theme.TrekGreen
import com.redshirt.warpcore.ui.theme.TrekOrange
import com.redshirt.warpcore.ui.theme.TrekRed

/**
 * Dashboard card showing current device status: actual temp, PWM, battery, session time.
 */
@Composable
fun StatusCard(
    status: DeviceStatus,
    unitLabel: String,
    toDisplay: (Int) -> Int,
    onArmToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        status.isRedAlert -> TrekRed
        status.isSensorError -> TrekOrange
        status.armed -> TrekGreen
        else -> TrekBlue
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Actual Temperature — large display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ACTUAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (status.isSensorError) {
                        Text(
                            text = "SENSOR ERR",
                            style = MaterialTheme.typography.displayMedium,
                            color = TrekRed
                        )
                    } else {
                        Text(
                            text = "${toDisplay(status.tempActual)}$unitLabel",
                            style = MaterialTheme.typography.displayMedium,
                            color = statusColor
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SESSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = status.sessionFormatted,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // PWM and Battery row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // PWM
                Column {
                    Text(
                        text = "PWR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${status.pwm}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = TrekOrange,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Arm/disarm toggle (consolidated with status label)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                status.isRedAlert -> "RED ALERT"
                                status.armed -> "ARMED"
                                else -> "DISARMED"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = status.armed,
                            onCheckedChange = onArmToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF44FF88),
                                checkedTrackColor = Color(0xFF226644)
                            )
                        )
                    }
                }

                // Battery
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "BATT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${status.battery}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = TrekGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Red Alert warning banner
            if (status.isRedAlert) {
                Surface(
                    color = TrekRed.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "⚠ RED ALERT — THERMAL RUNAWAY ⚠",
                        style = MaterialTheme.typography.titleMedium,
                        color = TrekRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (status.isSensorError) {
                Surface(
                    color = TrekOrange.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "⚠ SENSOR FAULT ⚠",
                        style = MaterialTheme.typography.titleMedium,
                        color = TrekOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}