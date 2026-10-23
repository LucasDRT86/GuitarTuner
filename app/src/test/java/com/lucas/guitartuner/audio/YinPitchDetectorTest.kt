package com.lucas.guitartuner.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class YinPitchDetectorTest {
    private val sampleRate = 44100
    private val size = 4096
    private val detector = YinPitchDetector(sampleRate, size)

    /** Son proche d'une corde pincée : harmoniques décroissantes + léger bruit. */
    private fun pluckedString(f: Float, random: Random = Random(1)): FloatArray =
        FloatArray(size) { i ->
            val t = i.toDouble() / sampleRate
            var v = 0.0
            for (h in 1..8) v += sin(2 * PI * f * h * t) * (0.6 / h) * (if (h == 2) 1.4 else 1.0)
            (v * exp(-t * 2) * 0.5 + (random.nextDouble() - 0.5) * 0.02).toFloat()
        }

    @Test
    fun detectsEveryOpenStringWithinOneCent() {
        for (string in GuitarString.entries) {
            val result = detector.detect(pluckedString(string.frequency))!!
            val cents = string.centsFrom(result.frequency)
            assert(abs(cents) < 1f) { "${string.name}: ${result.frequency} Hz ($cents cents)" }
            assertEquals(string, GuitarString.closestTo(result.frequency))
        }
    }

    @Test
    fun detectsDetunedString() {
        val flat = GuitarString.A2.frequency * Math.pow(2.0, -15.0 / 1200).toFloat()
        val result = detector.detect(pluckedString(flat))!!
        assertEquals(-15f, GuitarString.A2.centsFrom(result.frequency), 1f)
    }

    @Test
    fun noiseGivesNoPitch() {
        val random = Random(42)
        assertNull(detector.detect(FloatArray(size) { (random.nextFloat() - 0.5f) }))
    }

    @Test
    fun noteNames() {
        assertEquals("La4", noteNameFor(440f))
        assertEquals("Mi2", noteNameFor(82.41f))
        assertEquals("Si3", noteNameFor(246.94f))
    }
}
