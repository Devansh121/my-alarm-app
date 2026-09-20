package com.devansh.alarm.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeConversionsTest {

    @Test
    fun noonIs12Pm() {
        assertEquals(12, to24Hour(12, isPm = true))
        assertEquals(12, to12Hour(12))
        assertTrue(isPm(12))
    }

    @Test
    fun midnightIs12Am() {
        assertEquals(0, to24Hour(12, isPm = false))
        assertEquals(12, to12Hour(0))
        assertFalse(isPm(0))
    }

    @Test
    fun morningHoursPassThrough() {
        assertEquals(7, to24Hour(7, isPm = false))
        assertEquals(7, to12Hour(7))
    }

    @Test
    fun eveningHoursShiftBy12() {
        assertEquals(19, to24Hour(7, isPm = true))
        assertEquals(7, to12Hour(19))
        assertTrue(isPm(19))
    }

    @Test
    fun everyHourRoundTrips() {
        for (h in 0..23) {
            assertEquals(h, to24Hour(to12Hour(h), isPm(h)), "hour $h failed round trip")
        }
    }

    @Test
    fun invalidInputsRejected() {
        assertFailsWith<IllegalArgumentException> { to24Hour(0, false) }
        assertFailsWith<IllegalArgumentException> { to24Hour(13, false) }
        assertFailsWith<IllegalArgumentException> { to12Hour(24) }
    }
}
