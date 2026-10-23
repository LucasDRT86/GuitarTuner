package com.lucas.guitartuner.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.lucas.guitartuner.audio.GuitarString
import kotlin.math.min
import kotlin.math.pow

private val Rosewood = Color(0xFF2A1810)
private val RosewoodLight = Color(0xFF3B2317)
private val NeckSide = Color(0xFFB7793F)
private val Bone = Color(0xFFF3E9D2)
private val Fret = Color(0xFFCFCAC0)
private val FretShine = Color(0xFFF7F5F0)
private val Pearl = Color(0xFFEDE6DA)
private val WoundString = Color(0xFFD9B97C)
private val WoundShadow = Color(0xFF8A6A3A)
private val NylonString = Color(0xFFEDE8DE)
private val LabelIdle = Color(0xFFE9DFC9)
private val LabelText = Color(0xFF3B2317)

/**
 * Manche de guitare vu de face, sillet en haut, Mi grave à gauche.
 * La corde [active] est mise en surbrillance avec la couleur [highlight].
 */
@Composable
fun NeckView(active: GuitarString?, highlight: Color, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val glow = GuitarString.entries.map { string ->
        animateFloatAsState(if (string == active) 1f else 0f, tween(250), label = "glow${string.name}").value
    }
    // Léger battement du halo, comme une corde qui vibre
    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    ).value

    Canvas(modifier) {
        val labelBand = min(size.height * 0.09f, size.width * 0.13f)
        val top = labelBand + 6f
        val bottom = size.height
        val topWidth = min(size.width * 0.70f, size.height * 0.42f)
        val bottomWidth = topWidth * 1.14f
        val cx = size.width / 2
        // Unité d'épaisseur proportionnelle à la largeur du manche
        val unit = topWidth / 700f

        fun halfWidthAt(y: Float) = (topWidth + (bottomWidth - topWidth) * (y - top) / (bottom - top)) / 2
        fun stringX(index: Int, y: Float): Float {
            val half = halfWidthAt(y) * 0.84f
            return cx - half + 2 * half * index / 5f
        }

        drawFretboard(cx, top, bottom, topWidth, bottomWidth)
        drawFrets(cx, top, bottom, unit, ::halfWidthAt)

        // Sillet
        drawRoundRect(
            Bone,
            topLeft = Offset(cx - topWidth / 2 - 2f, top - 10f),
            size = Size(topWidth + 4f, 12f),
            cornerRadius = CornerRadius(4f),
        )

        GuitarString.entries.forEachIndexed { i, string ->
            drawStringLine(i, stringX(i, top - 4f), top - 4f, stringX(i, bottom), bottom, glow[i] * pulse, glow[i], highlight, unit)
            drawLabel(string, Offset(stringX(i, top), labelBand / 2), labelBand * 0.38f, glow[i], highlight, textMeasurer)
        }
    }
}

private fun DrawScope.drawFretboard(cx: Float, top: Float, bottom: Float, topWidth: Float, bottomWidth: Float) {
    fun trapezoid(extra: Float) = Path().apply {
        moveTo(cx - topWidth / 2 - extra, top)
        lineTo(cx + topWidth / 2 + extra, top)
        lineTo(cx + bottomWidth / 2 + extra, bottom)
        lineTo(cx - bottomWidth / 2 - extra, bottom)
        close()
    }
    // Bois du manche visible sur les côtés, puis la touche en palissandre
    drawPath(trapezoid(6f), NeckSide)
    drawPath(
        trapezoid(0f),
        Brush.horizontalGradient(
            0f to Rosewood, 0.5f to RosewoodLight, 1f to Rosewood,
            startX = cx - bottomWidth / 2, endX = cx + bottomWidth / 2,
        ),
    )
    // Veinage discret
    for (k in 1..7) {
        val x = cx - topWidth / 2 + topWidth * k / 8f
        drawLine(Color.Black.copy(alpha = 0.12f), Offset(x, top), Offset(x + (k - 4) * 3f, bottom), strokeWidth = 1.5f)
    }
}

private fun DrawScope.drawFrets(cx: Float, top: Float, bottom: Float, unit: Float, halfWidthAt: (Float) -> Float) {
    // Espacement réel des frettes (règle des demi-tons) ; on montre les 7 premières cases.
    val visibleFrets = 7
    val scaleLength = (bottom - top) * 0.97f / (1 - 2f.pow(-visibleFrets / 12f))
    fun fretY(n: Float) = top + scaleLength * (1 - 2f.pow(-n / 12f))

    for (n in 1..visibleFrets + 1) {
        val y = fretY(n.toFloat())
        if (y > bottom) break
        val half = halfWidthAt(y)
        drawLine(Fret, Offset(cx - half, y), Offset(cx + half, y), strokeWidth = 9f * unit)
        drawLine(FretShine, Offset(cx - half, y - 2f * unit), Offset(cx + half, y - 2f * unit), strokeWidth = 2.5f * unit)
    }
    // Repères de nacre aux cases 3, 5 et 7
    for (n in intArrayOf(3, 5, 7)) {
        val y = (fretY(n - 1f) + fretY(n.toFloat())) / 2
        if (y < bottom) drawCircle(Pearl, radius = halfWidthAt(y) * 0.09f, center = Offset(cx, y))
    }
}

private fun DrawScope.drawStringLine(
    index: Int,
    x0: Float, y0: Float, x1: Float, y1: Float,
    halo: Float,
    glow: Float,
    highlight: Color,
    unit: Float,
) {
    val start = Offset(x0, y0)
    val end = Offset(x1, y1)
    val base = floatArrayOf(14f, 12f, 10f, 8f, 7f, 6f)[index] * unit
    val width = base * (1f + 0.5f * glow)

    if (glow > 0f) {
        drawLine(highlight.copy(alpha = 0.25f * halo), start, end, strokeWidth = width + 60f * unit, cap = StrokeCap.Round)
        drawLine(highlight.copy(alpha = 0.5f * halo), start, end, strokeWidth = width + 24f * unit, cap = StrokeCap.Round)
    }
    // Ombre portée sur la touche
    drawLine(Color.Black.copy(alpha = 0.45f), start + Offset(5f * unit, 0f), end + Offset(5f * unit, 0f), strokeWidth = width)

    val wound = index < 3
    val color = lerp(if (wound) WoundString else NylonString, highlight, glow)
    drawLine(color, start, end, strokeWidth = width)
    if (wound) {
        // Filage des cordes graves
        drawLine(
            lerp(WoundShadow, highlight, glow * 0.5f).copy(alpha = 0.7f),
            start, end,
            strokeWidth = width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * unit, 3f * unit)),
        )
    }
    // Reflet
    drawLine(Color.White.copy(alpha = 0.35f), start - Offset(width / 4, 0f), end - Offset(width / 4, 0f), strokeWidth = width / 4)
}

private fun DrawScope.drawLabel(
    string: GuitarString,
    center: Offset,
    radius: Float,
    glow: Float,
    highlight: Color,
    textMeasurer: TextMeasurer,
) {
    if (glow > 0f) drawCircle(highlight.copy(alpha = 0.3f * glow), radius = radius * 1.35f, center = center)
    drawCircle(lerp(LabelIdle, highlight, glow), radius = radius, center = center)
    val text = textMeasurer.measure(
        string.name.take(1),
        TextStyle(
            fontSize = (radius * 1.1f).toSp(),
            fontWeight = FontWeight.Bold,
            color = lerp(LabelText, Color.White, glow),
        ),
    )
    drawText(text, topLeft = Offset(center.x - text.size.width / 2f, center.y - text.size.height / 2f))
}
