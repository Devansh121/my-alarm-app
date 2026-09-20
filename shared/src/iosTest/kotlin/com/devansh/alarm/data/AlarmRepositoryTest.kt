package com.devansh.alarm.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import com.devansh.alarm.db.AlarmDb
import com.devansh.alarm.domain.Alarm
import com.devansh.alarm.domain.ToneSelection
import kotlinx.datetime.DayOfWeek
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlarmRepositoryTest {

    private val driver: SqlDriver = inMemoryDriver(AlarmDb.Schema)
    private val repo = AlarmRepository(driver)

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun sample(id: String = "a1") = Alarm(
        id = id,
        hour = 7,
        minute = 30,
        repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
        label = "Wake up",
        tone = ToneSelection.Pinned("cosmic"),
        snoozeEnabled = false,
        snoozeMinutes = 5,
        enabled = true,
    )

    @Test
    fun savedAlarmRoundTripsExactly() {
        val alarm = sample()
        repo.save(alarm)
        assertEquals(alarm, repo.byId("a1"))
    }

    @Test
    fun randomToneRoundTrips() {
        repo.save(sample().copy(tone = ToneSelection.Random))
        assertEquals(ToneSelection.Random, repo.byId("a1")?.tone)
    }

    @Test
    fun oneShotEmptyRepeatDaysRoundTrips() {
        repo.save(sample().copy(repeatDays = emptySet()))
        assertEquals(emptySet(), repo.byId("a1")?.repeatDays)
    }

    @Test
    fun saveTwiceUpdatesInsteadOfDuplicating() {
        repo.save(sample())
        repo.save(sample().copy(label = "Gym", hour = 6))
        assertEquals(1L, repo.count())
        assertEquals("Gym", repo.byId("a1")?.label)
        assertEquals(6, repo.byId("a1")?.hour)
    }

    @Test
    fun allReturnsAlarmsSortedByTime() {
        repo.save(sample("late").copy(hour = 22, minute = 0))
        repo.save(sample("early").copy(hour = 6, minute = 15))
        repo.save(sample("mid").copy(hour = 12, minute = 0))
        assertEquals(listOf("early", "mid", "late"), repo.all().map { it.id })
    }

    @Test
    fun setEnabledTogglesWithoutTouchingOtherFields() {
        repo.save(sample())
        repo.setEnabled("a1", false)
        val loaded = repo.byId("a1")!!
        assertFalse(loaded.enabled)
        assertEquals("Wake up", loaded.label)
        repo.setEnabled("a1", true)
        assertTrue(repo.byId("a1")!!.enabled)
    }

    @Test
    fun deleteRemovesAlarm() {
        repo.save(sample())
        repo.delete("a1")
        assertNull(repo.byId("a1"))
        assertEquals(0L, repo.count())
    }

    @Test
    fun byIdUnknownReturnsNull() {
        assertNull(repo.byId("ghost"))
    }
}
