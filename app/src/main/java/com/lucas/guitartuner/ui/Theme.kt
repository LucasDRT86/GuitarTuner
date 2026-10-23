package com.lucas.guitartuner.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val InTuneGreen = Color(0xFF2E9E5B)
val OffTuneOrange = Color(0xFFE08A1E)
val FarOffRed = Color(0xFFD64545)

@Composable
fun TunerTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFFE0B97A), background = Color(0xFF17130F), surface = Color(0xFF221C16))
    } else {
        lightColorScheme(primary = Color(0xFF8A5A2B), background = Color(0xFFFBF7F2), surface = Color(0xFFF2EBE2))
    }
    MaterialTheme(colorScheme = colors, content = content)
}
