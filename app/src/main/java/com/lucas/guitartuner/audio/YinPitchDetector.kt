package com.lucas.guitartuner.audio

/**
 * Détection de fréquence fondamentale par l'algorithme YIN
 * (de Cheveigné & Kawahara, 2002).
 *
 * @param sampleRate fréquence d'échantillonnage en Hz
 * @param bufferSize taille des blocs analysés (en échantillons)
 * @param threshold seuil absolu de l'étape 4 (0.10 à 0.20 habituellement)
 * @param minFrequency fréquence la plus basse recherchée (Hz)
 * @param maxFrequency fréquence la plus haute recherchée (Hz)
 */
class YinPitchDetector(
    private val sampleRate: Int,
    bufferSize: Int,
    private val threshold: Float = 0.15f,
    minFrequency: Float = 60f,
    maxFrequency: Float = 1000f,
) {
    private val halfSize = bufferSize / 2
    private val yinBuffer = FloatArray(halfSize)
    private val minTau = maxOf(2, (sampleRate / maxFrequency).toInt())
    private val maxTau = minOf(halfSize - 1, (sampleRate / minFrequency).toInt() + 1)

    data class Result(val frequency: Float, val probability: Float)

    /** Renvoie la fréquence détectée, ou null si aucun son périodique n'est trouvé. */
    fun detect(buffer: FloatArray): Result? {
        require(buffer.size >= halfSize * 2) { "Buffer trop court" }

        // Étape 2 : fonction de différence
        for (tau in 0 until halfSize) yinBuffer[tau] = 0f
        for (tau in 1 until maxTau + 1) {
            var sum = 0f
            for (i in 0 until halfSize) {
                val delta = buffer[i] - buffer[i + tau]
                sum += delta * delta
            }
            yinBuffer[tau] = sum
        }

        // Étape 3 : différence normalisée par la moyenne cumulée
        yinBuffer[0] = 1f
        var runningSum = 0f
        for (tau in 1 until maxTau + 1) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] = if (runningSum == 0f) 1f else yinBuffer[tau] * tau / runningSum
        }

        // Étape 4 : premier minimum sous le seuil
        var tauEstimate = -1
        var tau = minTau
        while (tau < maxTau) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 < maxTau && yinBuffer[tau + 1] < yinBuffer[tau]) tau++
                tauEstimate = tau
                break
            }
            tau++
        }
        if (tauEstimate == -1) return null

        // Étape 5 : interpolation parabolique pour une précision sous-échantillon
        val betterTau = parabolicInterpolation(tauEstimate)
        val frequency = sampleRate / betterTau
        return Result(frequency, 1f - yinBuffer[tauEstimate])
    }

    private fun parabolicInterpolation(tau: Int): Float {
        if (tau < 1 || tau + 1 > maxTau) return tau.toFloat()
        val s0 = yinBuffer[tau - 1]
        val s1 = yinBuffer[tau]
        val s2 = yinBuffer[tau + 1]
        val denom = 2f * (2f * s1 - s2 - s0)
        return if (denom == 0f) tau.toFloat() else tau + (s2 - s0) / denom
    }
}
