package com.devansh.alarm.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ToneRandomizerTest {

    private val tones = BUNDLED_TONES

    @Test
    fun pinned_returnsThePinnedTone() {
        val randomizer = ToneRandomizer(tones, Random(42))
        val tone = randomizer.resolve(ToneSelection.Pinned("cosmic"), lastToneId = null)
        assertEquals("cosmic", tone.id)
    }

    @Test
    fun pinned_unknownId_fallsBackToFirstTone() {
        val randomizer = ToneRandomizer(tones, Random(42))
        val tone = randomizer.resolve(ToneSelection.Pinned("does-not-exist"), lastToneId = null)
        assertEquals(tones.first().id, tone.id)
    }

    @Test
    fun random_returnsAToneFromCatalog() {
        val randomizer = ToneRandomizer(tones, Random(42))
        val tone = randomizer.resolve(ToneSelection.Random, lastToneId = null)
        assertTrue(tones.any { it.id == tone.id })
    }

    @Test
    fun random_neverRepeatsLastTone() {
        // across many draws with every possible lastToneId, the last tone never repeats
        val randomizer = ToneRandomizer(tones, Random(7))
        for (last in tones.map { it.id }) {
            repeat(50) {
                val tone = randomizer.resolve(ToneSelection.Random, lastToneId = last)
                assertNotEquals(last, tone.id, "picked $last twice in a row")
            }
        }
    }

    @Test
    fun random_eventuallyCoversAllOtherTones() {
        val randomizer = ToneRandomizer(tones, Random(1))
        val seen = mutableSetOf<String>()
        repeat(500) {
            seen += randomizer.resolve(ToneSelection.Random, lastToneId = "radial").id
        }
        assertEquals(tones.map { it.id }.toSet() - "radial", seen)
    }

    @Test
    fun random_deterministicForSameSeed() {
        val a = ToneRandomizer(tones, Random(99))
        val b = ToneRandomizer(tones, Random(99))
        repeat(20) {
            assertEquals(
                a.resolve(ToneSelection.Random, lastToneId = null).id,
                b.resolve(ToneSelection.Random, lastToneId = null).id,
            )
        }
    }

    @Test
    fun singleToneCatalog_randomAlwaysReturnsIt_evenIfLast() {
        val single = listOf(tones.first())
        val randomizer = ToneRandomizer(single, Random(3))
        val tone = randomizer.resolve(ToneSelection.Random, lastToneId = tones.first().id)
        assertEquals(tones.first().id, tone.id)
    }

    @Test
    fun emptyCatalog_isRejectedAtConstruction() {
        assertFailsWith<IllegalArgumentException> { ToneRandomizer(emptyList(), Random(0)) }
    }
}
