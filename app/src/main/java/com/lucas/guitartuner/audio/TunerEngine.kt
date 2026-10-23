package com.lucas.guitartuner.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.sqrt

/**
 * Lit le micro en continu et émet la fréquence détectée (ou null quand
 * il n'y a pas de son exploitable). La permission RECORD_AUDIO doit
 * avoir été accordée avant de collecter le flux.
 */
class TunerEngine(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 4096,
    private val silenceRms: Float = 0.01f,
) {
    @SuppressLint("MissingPermission")
    fun frequencies(): Flow<Float?> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, bufferSize * 2),
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Micro indisponible" }

        val detector = YinPitchDetector(sampleRate, bufferSize)
        val pcm = ShortArray(bufferSize)
        val samples = FloatArray(bufferSize)
        // Fenêtre glissante : on recouvre de moitié pour rafraîchir plus souvent.
        val hop = bufferSize / 2

        recorder.startRecording()
        try {
            var filled = 0
            while (true) {
                val read = recorder.read(pcm, filled, bufferSize - filled)
                if (read <= 0) continue
                filled += read
                if (filled < bufferSize) continue

                var energy = 0f
                for (i in 0 until bufferSize) {
                    val s = pcm[i] / 32768f
                    samples[i] = s
                    energy += s * s
                }
                val rms = sqrt(energy / bufferSize)
                emit(if (rms < silenceRms) null else detector.detect(samples)?.frequency)

                System.arraycopy(pcm, hop, pcm, 0, bufferSize - hop)
                filled = bufferSize - hop
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.Default)
}
