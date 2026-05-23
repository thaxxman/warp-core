package com.redshirt.warpcore.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Temperature slider with manual entry.
 * Displays in the user's preferred unit (C or F) but sends Celsius to the device.
 * Tap the temperature value to type a number directly.
 *
 * Uses local slider state so BLE status updates don't fight the thumb
 * while dragging — only syncs from device when idle.
 */
@Composable
fun TempSlider(
    currentSetTempC: Int,
    onTempChange: (Int) -> Unit,
    unitLabel: String,
    minTempC: Int = 0,
    maxTempC: Int = 400,
    toDisplay: (Int) -> Int,
    fromDisplay: (Int) -> Int,
    step: Int = 1,
    modifier: Modifier = Modifier
) {
    val displayMin = toDisplay(minTempC)
    val displayMax = toDisplay(maxTempC)

    // Local slider state — owned by the slider during drag, synced from device when idle
    var sliderValue by remember { mutableFloatStateOf(toDisplay(currentSetTempC).toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    // When device reports a new temp AND we're not dragging, snap the slider to it
    val deviceDisplayTemp = toDisplay(currentSetTempC)
    LaunchedEffect(deviceDisplayTemp) {
        if (!isDragging) {
            sliderValue = deviceDisplayTemp.toFloat()
        }
    }

    var showInputDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // +/– buttons flanking the temperature display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledIconButton(
                    onClick = {
                        val newDisplay = (sliderValue.toInt() - step).coerceIn(displayMin, displayMax)
                        sliderValue = newDisplay.toFloat()
                        onTempChange(fromDisplay(newDisplay))
                    },
                    enabled = sliderValue.toInt() > displayMin,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("−", fontSize = 24.sp)
                }

                // Large temperature display — tappable to open input dialog
                Text(
                    text = "${sliderValue.toInt()}$unitLabel",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showInputDialog = true }
                )

                FilledIconButton(
                    onClick = {
                        val newDisplay = (sliderValue.toInt() + step).coerceIn(displayMin, displayMax)
                        sliderValue = newDisplay.toFloat()
                        onTempChange(fromDisplay(newDisplay))
                    },
                    enabled = sliderValue.toInt() < displayMax,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("+", fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slider — smooth drag, only sends BLE command when released
            Slider(
                value = sliderValue,
                onValueChange = {
                    isDragging = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    isDragging = false
                    val celsius = fromDisplay(sliderValue.toInt().coerceIn(displayMin, displayMax))
                    onTempChange(celsius)
                },
                valueRange = displayMin.toFloat()..displayMax.toFloat(),
                steps = (displayMax - displayMin) / 5,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Manual input dialog
    if (showInputDialog) {
        var input by remember { mutableStateOf(sliderValue.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = { Text("Set Temperature") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Temperature ($unitLabel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    input.toIntOrNull()?.let { v ->
                        val celsius = fromDisplay(v.coerceIn(displayMin, displayMax))
                        onTempChange(celsius)
                    }
                    showInputDialog = false
                }) { Text("SET") }
            },
            dismissButton = {
                TextButton(onClick = { showInputDialog = false }) { Text("CANCEL") }
            }
        )
    }
}