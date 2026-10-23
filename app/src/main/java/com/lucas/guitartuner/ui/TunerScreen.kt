package com.lucas.guitartuner.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.guitartuner.TunerUiState
import com.lucas.guitartuner.TunerViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TunerScreen(viewModel: TunerViewModel, onStart: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val highlight by animateColorAsState(
        if (state.cents == null) MaterialTheme.colorScheme.primary else tuneColor(state.cents),
        label = "highlight",
    )
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Le quart supérieur : note, justesse, consigne
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Header(state, onStart = onStart, onStop = viewModel::stop)
                NoteLine(state, highlight)
                CentsBar(state.cents, highlight, Modifier.fillMaxWidth().height(22.dp))
                TuningHint(state)
            }
            // Les trois quarts restants : le manche
            NeckView(
                active = state.targetString,
                highlight = highlight,
                modifier = Modifier.fillMaxWidth().weight(3f),
            )
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
private fun Header(state: TunerUiState, onStart: () -> Unit, onStop: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Accordeur", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (state.listening) {
            TextButton(onClick = onStop) { Text("Pause") }
        } else {
            TextButton(onClick = onStart) { Text("Écouter") }
        }
    }
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun NoteLine(state: TunerUiState, highlight: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = state.targetString?.noteName ?: "—",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.cents == null) MaterialTheme.colorScheme.onBackground else highlight,
        )
        val detail = state.frequency?.let { f ->
            "corde ${state.targetString?.label}\n${"%.1f".format(f)} Hz"
        } ?: if (state.listening) "Pincez une corde…" else "Touchez « Écouter »"
        Text(detail, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TuningHint(state: TunerUiState) {
    val cents = state.cents
    val text = when {
        cents == null -> " "
        state.inTune -> "Juste ✓"
        cents < 0 -> "Trop bas : tendez la corde (${cents.roundToInt()} cents)"
        else -> "Trop haut : détendez la corde (+${cents.roundToInt()} cents)"
    }
    Text(text, color = tuneColor(cents), style = MaterialTheme.typography.titleMedium)
}

/** Barre de -50 à +50 cents ; le curseur est au centre quand la corde est juste. */
@Composable
private fun CentsBar(cents: Float?, color: Color, modifier: Modifier = Modifier) {
    val position by animateFloatAsState(
        (cents ?: 0f).coerceIn(-50f, 50f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label = "cursor",
    )
    val track = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    val ticks = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)

    Canvas(modifier) {
        val barHeight = size.height * 0.45f
        val y = size.height / 2
        fun xFor(c: Float) = size.width / 2 + c / 50f * (size.width / 2 - size.height / 2)

        drawRoundRect(track, Offset(0f, y - barHeight / 2), Size(size.width, barHeight), CornerRadius(barHeight / 2))
        val inTuneLeft = xFor(-TunerUiState.IN_TUNE_CENTS)
        drawRect(
            InTuneGreen.copy(alpha = 0.45f),
            Offset(inTuneLeft, y - barHeight / 2),
            Size(xFor(TunerUiState.IN_TUNE_CENTS) - inTuneLeft, barHeight),
        )
        for (c in -50..50 step 10) {
            val h = if (c == 0) size.height else barHeight * 1.3f
            drawLine(ticks, Offset(xFor(c.toFloat()), y - h / 2), Offset(xFor(c.toFloat()), y + h / 2), strokeWidth = 2f)
        }
        if (cents != null) {
            drawCircle(color, radius = size.height / 2, center = Offset(xFor(position), y))
            drawCircle(Color.White, radius = size.height / 5, center = Offset(xFor(position), y))
        }
    }
}
