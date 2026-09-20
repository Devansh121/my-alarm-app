package com.devansh.alarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devansh.alarm.domain.Alarm
import com.devansh.alarm.domain.BUNDLED_TONES
import com.devansh.alarm.domain.ToneSelection
import kotlinx.datetime.DayOfWeek

private val DAY_LETTERS = listOf(
    DayOfWeek.MONDAY to "M", DayOfWeek.TUESDAY to "T", DayOfWeek.WEDNESDAY to "W",
    DayOfWeek.THURSDAY to "T", DayOfWeek.FRIDAY to "F", DayOfWeek.SATURDAY to "S",
    DayOfWeek.SUNDAY to "S",
)

private val SNOOZE_OPTIONS = listOf(5, 9, 10, 15, 20, 30)

/** Bottom sheet for creating or editing an alarm. [initial] pre-fills for edits. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditSheet(
    initial: Alarm,
    onSave: (Alarm) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var label by remember { mutableStateOf(initial.label) }
    var days by remember { mutableStateOf(initial.repeatDays) }
    var snooze by remember { mutableStateOf(initial.snoozeEnabled) }
    var snoozeMinutes by remember { mutableStateOf(initial.snoozeMinutes) }
    var tone by remember { mutableStateOf(initial.tone) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Set alarm",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            NativeTimeWheel(
                hour = hour,
                minute = minute,
                onTimeChange = { h, m ->
                    hour = h
                    minute = m
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DAY_LETTERS.forEach { (day, letter) ->
                    FilterChip(
                        selected = day in days,
                        onClick = { days = if (day in days) days - day else days + day },
                        label = { Text(letter) },
                    )
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Snooze", color = MaterialTheme.colorScheme.onSurface)
                Switch(checked = snooze, onCheckedChange = { snooze = it })
            }

            if (snooze) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SNOOZE_OPTIONS.forEach { minutes ->
                        FilterChip(
                            selected = snoozeMinutes == minutes,
                            onClick = { snoozeMinutes = minutes },
                            label = { Text("$minutes") },
                        )
                    }
                }
            }

            Text(
                "Tone",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = tone == ToneSelection.Random,
                    onClick = { tone = ToneSelection.Random },
                    label = { Text("Random") },
                )
                BUNDLED_TONES.forEach { bundled ->
                    FilterChip(
                        selected = (tone as? ToneSelection.Pinned)?.toneId == bundled.id,
                        onClick = { tone = ToneSelection.Pinned(bundled.id) },
                        label = { Text(bundled.displayName) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            initial.copy(
                                hour = hour,
                                minute = minute,
                                label = label.ifBlank { "Alarm" },
                                repeatDays = days,
                                snoozeEnabled = snooze,
                                snoozeMinutes = snoozeMinutes,
                                tone = tone,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save")
                }
            }
        }
    }
}
