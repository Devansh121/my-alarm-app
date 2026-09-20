package com.devansh.alarm.domain

data class Tone(val id: String, val displayName: String, val fileName: String)

// fileName must match a .caf bundled in the iOS app (added in a later task).
val BUNDLED_TONES: List<Tone> = listOf(
    Tone("radial", "Radial", "tone_radial.caf"),
    Tone("ascent", "Ascent", "tone_ascent.caf"),
    Tone("pulse", "Pulse", "tone_pulse.caf"),
    Tone("chimes", "Chimes", "tone_chimes.caf"),
    Tone("cosmic", "Cosmic", "tone_cosmic.caf"),
    Tone("beacon", "Beacon", "tone_beacon.caf"),
    Tone("signal", "Signal", "tone_signal.caf"),
    Tone("waves", "Waves", "tone_waves.caf"),
)
