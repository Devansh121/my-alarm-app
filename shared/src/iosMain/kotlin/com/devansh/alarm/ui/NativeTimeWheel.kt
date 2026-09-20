package com.devansh.alarm.ui

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UIDatePicker
import platform.UIKit.UIDatePickerMode
import platform.UIKit.UIDatePickerStyle
import platform.darwin.NSObject

/** Target-action bridge: UIControl needs an NSObject receiver for its selector. */
private class DatePickerTarget(
    private val onChange: (hour: Int, minute: Int) -> Unit,
) : NSObject() {
    @ObjCAction
    fun valueChanged(sender: UIDatePicker) {
        val components = NSCalendar.currentCalendar.components(
            NSCalendarUnitHour or NSCalendarUnitMinute,
            fromDate = sender.date,
        )
        onChange(components.hour.toInt(), components.minute.toInt())
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun NativeTimeWheel(
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier,
) {
    val latestOnTimeChange by rememberUpdatedState(onTimeChange)
    val target = remember { DatePickerTarget { h, m -> latestOnTimeChange(h, m) } }

    UIKitView(
        factory = {
            UIDatePicker().apply {
                datePickerMode = UIDatePickerMode.UIDatePickerModeTime
                preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
                addTarget(
                    target = target,
                    action = NSSelectorFromString("valueChanged:"),
                    forControlEvents = UIControlEventValueChanged,
                )
            }
        },
        modifier = modifier.height(216.dp),
        update = { picker ->
            val calendar = NSCalendar.currentCalendar
            val components = calendar.components(
                NSCalendarUnitHour or NSCalendarUnitMinute,
                fromDate = picker.date,
            )
            if (components.hour.toInt() != hour || components.minute.toInt() != minute) {
                calendar.dateBySettingHour(
                    h = hour.toLong(),
                    minute = minute.toLong(),
                    second = 0,
                    ofDate = picker.date,
                    options = 0u,
                )?.let { picker.setDate(it, animated = false) }
            }
        },
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative,
        ),
    )
}
