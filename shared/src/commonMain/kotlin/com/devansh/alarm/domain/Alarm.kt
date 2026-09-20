package com.devansh.alarm.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

sealed interface ToneSelection {
    data object Random : ToneSelection
    data class Pinned(val toneId: String) : ToneSelection
}

data class Alarm(
    val id: String,
    val hour: Int,                 // 0..23
    val minute: Int,               // 0..59
    val repeatDays: Set<DayOfWeek> = emptySet(),  // empty = one-shot
    val label: String = "Alarm",
    val tone: ToneSelection = ToneSelection.Random,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 9,    // 1..30
    val enabled: Boolean = true,
) {
    init {
        require(hour in 0..23) { "hour=$hour out of 0..23" }
        require(minute in 0..59) { "minute=$minute out of 0..59" }
        require(snoozeMinutes in 1..30) { "snoozeMinutes=$snoozeMinutes out of 1..30" }
    }
}

fun repeatDaysToCsv(days: Set<DayOfWeek>): String =
    days.sortedBy { it.isoDayNumber }.joinToString(",") { it.isoDayNumber.toString() }

fun repeatDaysFromCsv(csv: String): Set<DayOfWeek> =
    csv.split(",")
        .mapNotNull { token -> token.trim().toIntOrNull()?.takeIf { it in 1..7 } }
        .map { DayOfWeek(it) }
        .toSet()
