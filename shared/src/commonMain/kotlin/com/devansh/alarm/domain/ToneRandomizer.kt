package com.devansh.alarm.domain

import kotlin.random.Random

/**
 * Resolves an alarm's [ToneSelection] to a concrete [Tone].
 *
 * Random picks avoid repeating [lastToneId] so back-to-back alarms sound different
 * (unless the catalog has a single tone). Unknown pinned ids fall back to the first
 * catalog tone rather than failing at ring time.
 */
class ToneRandomizer(
    private val tones: List<Tone>,
    private val random: Random = Random.Default,
) {
    init {
        require(tones.isNotEmpty()) { "tone catalog must not be empty" }
    }

    fun resolve(selection: ToneSelection, lastToneId: String?): Tone = when (selection) {
        is ToneSelection.Pinned -> tones.firstOrNull { it.id == selection.toneId } ?: tones.first()
        is ToneSelection.Random -> {
            val candidates = tones.filter { it.id != lastToneId }.ifEmpty { tones }
            candidates[random.nextInt(candidates.size)]
        }
    }
}
