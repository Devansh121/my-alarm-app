package com.devansh.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devansh.alarm.domain.Alarm

/** Full-screen takeover while an alarm rings. */
@Composable
fun RingingScreen(
    alarm: Alarm,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⏰", style = MaterialTheme.typography.displayLarge)
        Text(
            formatTime(alarm.hour, alarm.minute),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            alarm.label,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (alarm.snoozeEnabled) {
                OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f)) {
                    Text("Snooze ${alarm.snoozeMinutes} min")
                }
            }
            Button(
                onClick = onStop,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("Stop")
            }
        }
    }
}
