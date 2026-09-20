@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.devansh.alarm.engine

import kotlinx.datetime.Instant

/**
 * A concrete request for the platform to ring at [fireAt].
 * [toneFileName] is a bundled .caf resolved ahead of time so the platform
 * layer never needs domain logic.
 */
data class FireRequest(
    val alarmId: String,
    val fireAt: Instant,
    val label: String,
    val toneFileName: String,
    val isSnooze: Boolean = false,
)

/**
 * Platform boundary: iOS implements this with UNUserNotificationCenter etc.
 * (Swift side, Task 13). Kotlin never talks to platform APIs directly.
 */
interface AlarmEngine {
    fun schedule(request: FireRequest)
    fun cancel(alarmId: String)
    fun cancelAll()
}
