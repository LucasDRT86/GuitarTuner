package com.lucas.guitartuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucas.guitartuner.audio.GuitarString
import com.lucas.guitartuner.audio.TunerEngine
import com.lucas.guitartuner.audio.noteNameFor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log2

data class TunerUiState(
    val listening: Boolean = false,
    val frequency: Float? = null,
    val detectedNote: String? = null,
    val targetString: GuitarString? = null,
    val cents: Float? = null,
    val error: String? = null,
) {
    val inTune: Boolean get() = cents != null && abs(cents) <= IN_TUNE_CENTS

    companion object {
        const val IN_TUNE_CENTS = 5f
    }
}

class TunerViewModel : ViewModel() {
    private val engine = TunerEngine()
    private val _state = MutableStateFlow(TunerUiState())
    val state: StateFlow<TunerUiState> = _state.asStateFlow()

    private var job: Job? = null
    private val recent = ArrayDeque<Float>()
    private var silentFrames = 0
    private var pendingString: GuitarString? = null
    private var pendingCount = 0

    fun start() {
        if (job != null) return
        _state.update { it.copy(listening = true, error = null) }
        job = viewModelScope.launch {
            engine.frequencies()
                .catch { e -> _state.update { it.copy(listening = false, error = e.message) } }
                .collect(::onFrequency)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        recent.clear()
        _state.update { it.copy(listening = false) }
    }

    private fun onFrequency(raw: Float?) {
        if (raw == null) {
            // On garde l'affichage un court instant pour éviter le clignotement.
            if (++silentFrames > 15) {
                recent.clear()
                _state.update { it.copy(frequency = null, detectedNote = null, cents = null, targetString = null) }
            }
            return
        }
        silentFrames = 0

        // Nouvelle corde pincée (écart de plus d'un demi-ton) : on repart d'une médiane vide.
        recent.lastOrNull()?.let { if (abs(1200 * log2(raw / it)) > 100) recent.clear() }

        // Médiane glissante pour lisser l'aiguille et ignorer les sauts d'octave isolés.
        recent.addLast(raw)
        if (recent.size > 5) recent.removeFirst()
        val frequency = recent.sorted()[recent.size / 2]

        _state.update { current ->
            val target = stableTarget(current.targetString, GuitarString.closestTo(frequency))
            current.copy(
                frequency = frequency,
                detectedNote = noteNameFor(frequency),
                targetString = target,
                cents = target.centsFrom(frequency),
            )
        }
    }

    /** On ne change de corde visée qu'après 3 mesures consécutives d'accord, pour éviter les sauts. */
    private fun stableTarget(current: GuitarString?, closest: GuitarString): GuitarString {
        if (current == null || closest == current) {
            pendingString = null
            pendingCount = 0
            return closest
        }
        if (closest == pendingString) pendingCount++ else {
            pendingString = closest
            pendingCount = 1
        }
        return if (pendingCount >= 3) closest.also { pendingString = null; pendingCount = 0 } else current
    }

    override fun onCleared() {
        stop()
    }
}
