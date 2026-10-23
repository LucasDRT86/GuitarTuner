package com.lucas.guitartuner.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.guitartuner.TunerUiState
import com.lucas.guitartuner.TunerViewModel
import com.lucas.guitartuner.audio.GuitarString
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TunerScreen(viewModel: TunerViewModel, onStart: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Accordeur guitare classique", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            NoteDisplay(state)
            Spacer(Modifier.height(8.dp))
            CentsGauge(cents = state.cents, modifier = Modifier.fillMaxWidth().aspectRatio(2.6f))
            Spacer(Modifier.height(4.dp))
            TuningHint(state)
            Spacer(Modifier.height(8.dp))
            GuitarView(
                active = state.targetString,
                highlight = if (state.cents == null) MaterialTheme.colorScheme.primary else tuneColor(state.cents),
                onStringTap = { string -> viewModel.selectString(if (state.selectedString == string) null else string) },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            ModeLine(selected = state.selectedString, onAuto = { viewModel.selectString(null) })
            Spacer(Modifier.height(12.dp))
            if (state.listening) {
                OutlinedButton(onClick = viewModel::stop, modifier = Modifier.fillMaxWidth()) { Text("Arrêter") }
            } else {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Démarrer l'écoute") }
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

private fun tuneColor(cents: Float?): Color = when {
    cents == null -> Color.Gray
    abs(cents) <= TunerUiState.IN_TUNE_CENTS -> InTuneGreen
    abs(cents) <= 20f -> OffTuneOrange
    else -> FarOffRed
}

@Composable
private fun NoteDisplay(state: TunerUiState) {
    val color by animateColorAsState(tuneColor(state.cents), label = "noteColor")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = state.targetString?.noteName ?: "—",
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.cents == null) MaterialTheme.colorScheme.onBackground else color,
        )
        val detail = state.frequency?.let { f ->
            "Corde ${state.targetString?.label} · entendu ${state.detectedNote} · ${"%.1f".format(f)} Hz"
        } ?: if (state.listening) "Pincez une corde…" else "Appuyez sur « Démarrer l'écoute »"
        Text(detail, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TuningHint(state: TunerUiState) {
    val cents = state.cents ?: return
    val text = when {
        state.inTune -> "Juste ✓"
        cents < 0 -> "Trop bas : tendez la corde (${cents.roundToInt()} cents)"
        else -> "Trop haut : détendez la corde (+${cents.roundToInt()} cents)"
    }
    Text(text, color = tuneColor(cents), style = MaterialTheme.typography.titleMedium)
}

/** Demi-cadran de -50 à +50 cents, avec une aiguille animée. */
@Composable
private fun CentsGauge(cents: Float?, modifier: Modifier = Modifier) {
    val target = (cents ?: 0f).coerceIn(-50f, 50f)
    val animated by animateFloatAsState(target, animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f), label = "needle")
    val needleColor by animateColorAsState(tuneColor(cents), label = "needleColor")
    val trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height * 0.95f)
            val radius = minOf(size.width / 2, size.height) * 0.9f

            // Graduations tous les 10 cents, zone juste en vert
            for (c in -50..50 step 5) {
                val angle = Math.toRadians(90.0 - c * 1.6).toFloat() // ±50 cents → ±80°
                val long = c % 10 == 0
                val inner = radius * if (long) 0.82f else 0.88f
                val dir = Offset(cos(angle), -sin(angle))
                drawLine(
                    color = if (abs(c) <= TunerUiState.IN_TUNE_CENTS) InTuneGreen else trackColor,
                    start = center + dir * inner,
                    end = center + dir * radius,
                    strokeWidth = if (long) 5f else 3f,
                    cap = StrokeCap.Round,
                )
            }
            drawArc(
                color = trackColor,
                startAngle = 190f, sweepAngle = 160f, useCenter = false,
                topLeft = center - Offset(radius, radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = 3f),
            )

            if (cents != null) {
                val angle = Math.toRadians(90.0 - animated * 1.6).toFloat()
                val tip = center + Offset(cos(angle), -sin(angle)) * (radius * 0.95f)
                drawLine(needleColor, center, tip, strokeWidth = 8f, cap = StrokeCap.Round)
            }
            drawCircle(needleColor, radius = 12f, center = center)
        }
    }
}

@Composable
private fun ModeLine(selected: GuitarString?, onAuto: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(40.dp)) {
        Text(
            text = if (selected == null) "Mode auto · touchez une corde pour la choisir"
            else "Corde ${selected.label} (${selected.noteName}) choisie",
            style = MaterialTheme.typography.bodySmall,
        )
        if (selected != null) {
            TextButton(onClick = onAuto) { Text("Mode auto") }
        }
    }
}
