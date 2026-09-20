@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.devansh.alarm

import app.cash.sqldelight.driver.native.inMemoryDriver
import com.devansh.alarm.data.AlarmRepository
import com.devansh.alarm.db.AlarmDb
import com.devansh.alarm.domain.Alarm
import com.devansh.alarm.domain.ToneSelection
import com.devansh.alarm.engine.AlarmEngine
import com.devansh.alarm.engine.FireRequest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class NotificationActionEngine : AlarmEngine {
    val pending = mutableMapOf<String, FireRequest>()
    override fun schedule(request: FireRequest) { pending[request.alarmId] = request }
    override fun cancel(alarmId: String) { pending.remove(alarmId) }
    override fun cancelAll() { pending.clear() }
}

private class NotificationActionRinger : RingerControl {
    var playing: String? = null
    override fun start(toneFileName: String) { playing = toneFileName }
    override fun stop() { playing = null }
}

/** Covers the Snooze/Stop notification-action paths, which bypass the ringer. */
class NotificationActionsTest {

    private val zone = TimeZone.of("Asia/Kolkata")
    private val now = LocalDateTime(2026, 9, 20, 10, 0).toInstant(zone)
    private val engine = NotificationActionEngine()
    private val ringer = NotificationActionRinger()
    private val core = AppCore(
        repository = AlarmRepository(inMemoryDriver(AlarmDb.Schema)),
        engine = engine,
        ringer = ringer,
        now = { now },
        zone = { zone },
    )

    private val oneShot = Alarm(
        id = "a1", hour = 18, minute = 0,
        tone = ToneSelection.Pinned("waves"), snoozeMinutes = 9,
    )

    // --- snoozeFromNotification ---

    @Test
    fun snoozeFromNotificationSchedulesSnoozeWithPendingTone() {
        core.upsert(oneShot)
        core.snoozeFromNotification("a1")
        val request = engine.pending.getValue("a1")
        assertTrue(request.isSnooze)
        assertEquals("tone_waves.caf", request.toneFileName)
        assertEquals(
            LocalDateTime(2026, 9, 20, 10, 9).toInstant(zone),
            request.fireAt,
        )
    }

    @Test
    fun snoozeFromNotificationDoesNotTouchRingingState() {
        core.upsert(oneShot)
        core.snoozeFromNotification("a1")
        assertNull(core.ringing.value)
        assertNull(ringer.playing)
    }

    @Test
    fun snoozeFromNotificationIgnoresUnknownAlarm() {
        core.snoozeFromNotification("ghost")
        assertTrue(engine.pending.isEmpty())
    }

    @Test
    fun snoozeFromNotificationIgnoredWhenSnoozeDisabled() {
        core.upsert(oneShot.copy(snoozeEnabled = false))
        val before = engine.pending.getValue("a1")
        core.snoozeFromNotification("a1")
        assertEquals(before, engine.pending.getValue("a1"))
        assertFalse(engine.pending.getValue("a1").isSnooze)
    }

    @Test
    fun snoozedAlarmFromNotificationRingsWithSameToneOnNextFire() {
        core.upsert(oneShot)
        core.snoozeFromNotification("a1")
        core.onAlarmFired("a1")
        assertEquals("tone_waves.caf", ringer.playing)
    }

    // --- stopFromNotification ---

    @Test
    fun stopFromNotificationDisablesOneShotAndClearsPending() {
        core.upsert(oneShot)
        core.stopFromNotification("a1")
        assertFalse(core.alarms.value.single().enabled)
        assertTrue(engine.pending.isEmpty())
    }

    @Test
    fun stopFromNotificationKeepsRepeatingAlarmEnabledAndRescheduled() {
        core.upsert(oneShot.copy(id = "a2", repeatDays = DayOfWeek.entries.toSet()))
        core.stopFromNotification("a2")
        assertTrue(core.alarms.value.single().enabled)
        assertTrue("a2" in engine.pending)
    }

    @Test
    fun stopFromNotificationDoesNotTouchRingingState() {
        core.upsert(oneShot)
        core.onAlarmFired("a1") // simulate the fire, then Stop from the banner
        core.stopFromNotification("a1")
        // The notification action never drives the in-app ringer/screen.
        assertEquals(oneShot, core.ringing.value)
        assertEquals("tone_waves.caf", ringer.playing)
    }

    @Test
    fun stopFromNotificationIgnoresUnknownAlarm() {
        core.upsert(oneShot)
        core.stopFromNotification("ghost")
        assertTrue(core.alarms.value.single().enabled)
        assertTrue("a1" in engine.pending)
    }
}
