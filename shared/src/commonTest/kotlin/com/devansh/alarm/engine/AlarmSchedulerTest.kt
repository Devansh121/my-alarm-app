@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.devansh.alarm.engine

import com.devansh.alarm.domain.Alarm
import com.devansh.alarm.domain.BUNDLED_TONES
import com.devansh.alarm.domain.ToneRandomizer
import com.devansh.alarm.domain.ToneSelection
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class FakeEngine : AlarmEngine {
    val scheduled = mutableListOf<FireRequest>()
    val cancelled = mutableListOf<String>()
    var cancelAllCount = 0

    override fun schedule(request: FireRequest) { scheduled += request }
    override fun cancel(alarmId: String) { cancelled += alarmId }
    override fun cancelAll() { cancelAllCount++; scheduled.clear() }
}

class AlarmSchedulerTest {

    private val zone = TimeZone.of("Asia/Kolkata")
    private val engine = FakeEngine()
    private val scheduler = AlarmScheduler(engine, ToneRandomizer(BUNDLED_TONES, Random(42)))

    private fun instantOf(y: Int, mo: Int, d: Int, h: Int, mi: Int): Instant =
        LocalDateTime(y, mo, d, h, mi).toInstant(zone)

    // Sat 2026-09-20 10:00 IST
    private val now = instantOf(2026, 9, 20, 10, 0)

    private fun alarm(id: String, hour: Int, minute: Int, enabled: Boolean = true) = Alarm(
        id = id, hour = hour, minute = minute, enabled = enabled,
        tone = ToneSelection.Pinned("cosmic"),
    )

    @Test
    fun rescheduleAll_cancelsEverythingFirst() {
        scheduler.rescheduleAll(listOf(alarm("a", 18, 0)), now, zone)
        scheduler.rescheduleAll(listOf(alarm("a", 18, 0)), now, zone)
        assertEquals(2, engine.cancelAllCount)
        assertEquals(1, engine.scheduled.size)
    }

    @Test
    fun rescheduleAll_skipsDisabledAlarms() {
        val requests = scheduler.rescheduleAll(
            listOf(alarm("on", 18, 0), alarm("off", 19, 0, enabled = false)),
            now, zone,
        )
        assertEquals(listOf("on"), requests.map { it.alarmId })
        assertEquals(listOf("on"), engine.scheduled.map { it.alarmId })
    }

    @Test
    fun rescheduleAll_usesNextFireTimes() {
        val requests = scheduler.rescheduleAll(
            listOf(alarm("later-today", 18, 30), alarm("tomorrow", 7, 0)),
            now, zone,
        )
        assertEquals(instantOf(2026, 9, 20, 18, 30), requests[0].fireAt)
        assertEquals(instantOf(2026, 9, 21, 7, 0), requests[1].fireAt)
    }

    @Test
    fun rescheduleAll_resolvesPinnedTone() {
        val requests = scheduler.rescheduleAll(listOf(alarm("a", 18, 0)), now, zone)
        assertEquals("tone_cosmic.caf", requests.single().toneFileName)
    }

    @Test
    fun rescheduleAll_randomToneAvoidsLastTone() {
        val a = alarm("a", 18, 0).copy(tone = ToneSelection.Random)
        repeat(30) {
            val requests = scheduler.rescheduleAll(listOf(a), now, zone, lastToneId = "radial")
            assertTrue(requests.single().toneFileName != "tone_radial.caf")
        }
    }

    @Test
    fun snooze_schedulesAtNowPlusSnoozeMinutes_keepingTone() {
        val a = alarm("a", 7, 0).copy(snoozeMinutes = 9)
        val request = scheduler.snooze(a, now, currentToneFileName = "tone_waves.caf")
        assertEquals(instantOf(2026, 9, 20, 10, 9), request.fireAt)
        assertEquals("tone_waves.caf", request.toneFileName)
        assertTrue(request.isSnooze)
        assertEquals(listOf(request), engine.scheduled)
    }

    @Test
    fun snooze_whenDisabled_throws() {
        val a = alarm("a", 7, 0).copy(snoozeEnabled = false)
        assertFailsWith<IllegalArgumentException> {
            scheduler.snooze(a, now, "tone_waves.caf")
        }
    }

    @Test
    fun cancel_delegatesToEngine() {
        scheduler.cancel("a1")
        assertEquals(listOf("a1"), engine.cancelled)
    }
}
