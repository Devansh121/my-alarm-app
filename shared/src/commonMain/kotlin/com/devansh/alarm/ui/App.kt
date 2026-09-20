package com.devansh.alarm.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.devansh.alarm.AppCore
import com.devansh.alarm.domain.Alarm
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(core: AppCore) {
    var editing by remember { mutableStateOf<Alarm?>(null) }

    AppTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("My Alarm") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = {
                        editing = Alarm(
                            id = "alarm-${Random.nextLong().toULong()}",
                            hour = 7,
                            minute = 0,
                            label = "Alarm",
                        )
                    },
                ) {
                    Text("+", style = MaterialTheme.typography.headlineMedium)
                }
            },
        ) { padding ->
            AlarmListScreen(
                alarms = core.alarms.value,
                onToggle = core::setEnabled,
                onDelete = core::delete,
                onEdit = { editing = it },
                modifier = Modifier.padding(padding),
            )
        }

        editing?.let { alarm ->
            AlarmEditSheet(
                initial = alarm,
                onSave = { saved ->
                    core.upsert(saved)
                    editing = null
                },
                onDismiss = { editing = null },
            )
        }

        core.ringing.value?.let { alarm ->
            RingingScreen(
                alarm = alarm,
                onSnooze = core::snoozeRinging,
                onStop = core::stopRinging,
            )
        }
    }
}
