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
import androidx.compose.ui.Modifier
import com.devansh.alarm.AppCore
import com.devansh.alarm.domain.Alarm
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(core: AppCore) {
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
                        // Edit sheet lands in Task 11; until then + adds a 7:00 AM alarm
                        core.upsert(
                            Alarm(
                                id = "alarm-${Random.nextLong().toULong()}",
                                hour = 7,
                                minute = 0,
                                label = "Alarm",
                            ),
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
                onEdit = { /* Task 11 */ },
                modifier = Modifier.padding(padding),
            )
        }
    }
}
