package com.devansh.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF1A1B2E)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("⏰", style = MaterialTheme.typography.displayLarge)
            Text(
                "Hello from My Alarm",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Text(
                placeholder(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8A8FB5),
            )
        }
    }
}
