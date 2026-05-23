package com.redshirt.warpcore.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.redshirt.warpcore.data.Profile

/**
 * Profile management screen — save and recall temperature presets.
 */
@Composable
fun ProfileScreen(
    profiles: List<Profile>,
    unitLabel: String,
    toDisplay: (Int) -> Int,
    onSaveProfile: (String, Int) -> Unit,
    onApplyProfile: (Int) -> Unit,
    onDeleteProfile: (Profile) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var newProfileTemp by remember { mutableStateOf("200") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "PROFILES",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ NEW PROFILE")
        }

        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No profiles saved yet.\nCreate one to quickly recall temperatures.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles) { profile ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${toDisplay(profile.targetTempCelsius)}$unitLabel",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row {
                                TextButton(onClick = { onApplyProfile(profile.targetTempCelsius) }) {
                                    Text("APPLY")
                                }
                                TextButton(
                                    onClick = { onDeleteProfile(profile) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("DELETE")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add profile dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newProfileTemp,
                        onValueChange = { newProfileTemp = it },
                        label = { Text("Temperature ($unitLabel)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val temp = newProfileTemp.toIntOrNull() ?: 200
                    // Convert display to Celsius if needed — simplified here since
                    // the caller handles conversion
                    onSaveProfile(newProfileName, temp)
                    newProfileName = ""
                    newProfileTemp = "200"
                    showAddDialog = false
                }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("CANCEL") }
            }
        )
    }
}