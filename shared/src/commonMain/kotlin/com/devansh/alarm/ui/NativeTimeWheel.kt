package com.devansh.alarm.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-native time wheel (on iOS: UIDatePicker in wheels style, time mode).
 * [hour] is 0..23, [minute] 0..59; [onTimeChange] reports the same ranges.
 */
@Composable
expect fun NativeTimeWheel(
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
)
