package com.devansh.alarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

private val DAY_LETTERS = listOf(
    DayOfWeek.MONDAY to "M", DayOfWeek.TUESDAY to "T", DayOfWeek.WEDNESDAY to "W",
    DayOfWeek.THURSDAY to "T", DayOfWeek.FRIDAY to "F", DayOfWeek.SATURDAY to "S",
    DayOfWeek.SUNDAY to "S",
)

/** Bottom sheet for creating or editing an alarm. [initial] pre-fills for edits. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    initial: Alarm,
    onSave: (Alarm) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour12 by remember { mutableStateOf(to12Hour(initial.hour)) }
    var minute by remember { mutableStateOf(initial.minute) }
    var pm by remember { mutableStateOf(isPm(initial.hour)) }
    var label by remember { mutableStateOf(initial.label) }
    var days by remember { mutableStateOf(initial.repeatDays) }
    var snooze by remember { mutableStateOf(initial.snoozeEnabled) }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelPicker(
                    items = (1..12).map { it.toString() },
                    selectedIndex = hour12 - 1,
                    onSelected = { hour12 = it + 1 },
                )
                Text(":", style = MaterialTheme.typography.headlineSmall)
                WheelPicker(
                    items = (0..59).map { it.toString().padStart(2, '0') },
                    selectedIndex = minute,
                    onSelected = { minute = it },
                )
                WheelPicker(
                    items = listOf("AM", "PM"),
                    selectedIndex = if (pm) 1 else 0,
                    onSelected = { pm = it == 1 },
                )
            }

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
                Text("Snooze (${initial.snoozeMinutes} min)", color = MaterialTheme.colorScheme.onSurface)
                Switch(checked = snooze, onCheckedChange = { snooze = it })
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
                                hour = to24Hour(hour12, pm),
                                minute = minute,
                                label = label.ifBlank { "Alarm" },
                                repeatDays = days,
                                snoozeEnabled = snooze,
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
