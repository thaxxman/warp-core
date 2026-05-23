package com.redshirt.warpcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.redshirt.warpcore.ui.theme.TrekRed

/**
 * Emergency stop button — large, red, impossible to miss.
 * Sends repeated e_stop commands until acknowledged.
 */
@Composable
fun EmergencyStopButton(
    onEStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onEStop,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TrekRed,
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚠ EMERGENCY STOP ⚠",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "TAP TO KILL HEATER",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}