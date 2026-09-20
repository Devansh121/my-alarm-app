package com.devansh.alarm.ui

/** 12-hour wheel selection (hour 1..12 + AM/PM) to 24-hour clock. */
fun to24Hour(hour12: Int, isPm: Boolean): Int {
    require(hour12 in 1..12) { "hour12=$hour12 out of 1..12" }
    return when {
        isPm && hour12 == 12 -> 12   // 12 PM = noon
        isPm -> hour12 + 12
        hour12 == 12 -> 0            // 12 AM = midnight
        else -> hour12
    }
}

/** 24-hour clock hour to the 12-hour wheel position. */
fun to12Hour(hour24: Int): Int {
    require(hour24 in 0..23) { "hour24=$hour24 out of 0..23" }
    return when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
}

fun isPm(hour24: Int): Boolean = hour24 >= 12
