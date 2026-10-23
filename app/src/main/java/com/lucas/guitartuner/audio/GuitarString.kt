package com.lucas.guitartuner.audio

import kotlin.math.abs
import kotlin.math.log2

/** Les six cordes d'une guitare classique en accordage standard (La = 440 Hz). */
enum class GuitarString(val label: String, val noteName: String, val frequency: Float) {
    E2("6", "Mi grave", 82.41f),
    A2("5", "La", 110.00f),
    D3("4", "Ré", 146.83f),
    G3("3", "Sol", 196.00f),
    B3("2", "Si", 246.94f),
    E4("1", "Mi aigu", 329.63f);

    /** Écart en cents entre [frequency] et la fréquence de cette corde. */
    fun centsFrom(frequency: Float): Float = 1200f * log2(frequency / this.frequency)

    companion object {
        /** Corde la plus proche (en cents) de la fréquence donnée. */
        fun closestTo(frequency: Float): GuitarString =
            entries.minBy { abs(it.centsFrom(frequency)) }
    }
}

private val NOTE_NAMES = arrayOf("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")

/** Nom de la note chromatique la plus proche, avec l'octave (ex. "La2"). */
fun noteNameFor(frequency: Float): String {
    val midi = Math.round(69 + 12 * log2(frequency / 440f))
    return NOTE_NAMES[((midi % 12) + 12) % 12] + (midi / 12 - 1)
}
