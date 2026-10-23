package com.lucas.guitartuner.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.lucas.guitartuner.audio.GuitarString
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

private val SpruceTop = Color(0xFFF2C77E)
private val SpruceEdge = Color(0xFFE0A65A)
private val Binding = Color(0xFF6E3413)
private val DarkWood = Color(0xFF4A2412)
private val Fretboard = Color(0xFF24150D)
private val FretMetal = Color(0xFFC9C3B8)
private val Bone = Color(0xFFF5EBD6)
private val SoundHole = Color(0xFF140A05)
private val Rosette = Color(0xFF00A88F)
private val RosetteInner = Color(0xFFE85D3F)
private val PegColor = Color(0xFFF7E3A6)
private val WoundString = Color(0xFFD6B77A)
private val NylonString = Color(0xFFE9E4DA)

/**
 * Guitare classique vue de face, tête en haut. Les dimensions sont exprimées
 * dans un repère de 100 × 262 unités centré horizontalement, puis mises à
 * l'échelle de la zone disponible.
 */
private class GuitarGeometry(width: Float, height: Float) {
    val scale = min(width / WIDTH, height / HEIGHT)
    private val originX = width / 2
    private val originY = (height - HEIGHT * scale) / 2

    fun point(x: Float, y: Float) = Offset(originX + x * scale, originY + y * scale)
    fun toGuitarX(px: Float) = (px - originX) / scale
    fun toGuitarY(py: Float) = (py - originY) / scale

    /** Position horizontale de la corde [index] (0 = Mi grave) à la hauteur [y], entre sillet et chevalet. */
    fun stringX(index: Int, y: Float): Float {
        val t = ((y - NUT_Y) / (BRIDGE_Y - NUT_Y)).coerceIn(0f, 1f)
        val atNut = -10f + 4f * index
        val atBridge = -12.5f + 5f * index
        return atNut + (atBridge - atNut) * t
    }

    companion object {
        const val WIDTH = 100f
        const val HEIGHT = 262f
        const val NUT_Y = 46f
        const val BRIDGE_Y = 231f
        const val BODY_JOINT_Y = 142f

        /** Hauteur de la mécanique de chaque corde : les cordes extérieures vont aux mécaniques les plus hautes. */
        val PEG_Y = floatArrayOf(12f, 24f, 36f, 36f, 24f, 12f)
        fun pegSide(index: Int) = if (index < 3) -1f else 1f
        const val SLOT_X = 6f
        const val KNOB_X = 29f
        const val KNOB_R = 6f
    }
}

/**
 * Dessine une guitare dont la corde [active] est mise en surbrillance avec [highlight].
 * Toucher une corde ou une mécanique appelle [onStringTap].
 */
@Composable
fun GuitarView(
    active: GuitarString?,
    highlight: Color,
    onStringTap: (GuitarString) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val onTap by rememberUpdatedState(onStringTap)
    // Une valeur animée par corde, pour un fondu doux quand la corde visée change.
    val glow = GuitarString.entries.map { string ->
        animateFloatAsState(if (string == active) 1f else 0f, tween(250), label = "glow${string.name}").value
    }

    Canvas(
        modifier.pointerInput(Unit) {
            detectTapGestures { position ->
                val g = GuitarGeometry(size.width.toFloat(), size.height.toFloat())
                stringAt(g, g.toGuitarX(position.x), g.toGuitarY(position.y))?.let(onTap)
            }
        },
    ) {
        val g = GuitarGeometry(size.width, size.height)
        drawBody(g)
        drawNeck(g)
        drawHead(g)
        GuitarString.entries.forEachIndexed { i, string ->
            drawPeg(g, i, string, glow[i], highlight, textMeasurer)
        }
        GuitarString.entries.indices.forEach { i -> drawString(g, i, glow[i], highlight) }
    }
}

private fun stringAt(g: GuitarGeometry, x: Float, y: Float): GuitarString? {
    val strings = GuitarString.entries
    if (y < GuitarGeometry.NUT_Y) {
        // Zone de la tête : on vise la mécanique la plus proche.
        val index = strings.indices.minBy { i ->
            val dx = x - GuitarGeometry.pegSide(i) * GuitarGeometry.KNOB_X
            val dy = y - GuitarGeometry.PEG_Y[i]
            dx * dx + dy * dy
        }
        val dx = abs(x) - GuitarGeometry.KNOB_X
        return if (abs(dx) < 10f && abs(y - GuitarGeometry.PEG_Y[index]) < 7f) strings[index] else null
    }
    if (y > GuitarGeometry.BRIDGE_Y + 8f) return null
    val index = strings.indices.minBy { abs(g.stringX(it, y) - x) }
    return if (abs(g.stringX(index, y) - x) < 4f) strings[index] else null
}

private fun DrawScope.drawOval(g: GuitarGeometry, cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
    drawOval(color, topLeft = g.point(cx - rx, cy - ry), size = Size(2 * rx * g.scale, 2 * ry * g.scale))
}

private fun DrawScope.drawBody(g: GuitarGeometry) {
    // Deux lobes qui se rejoignent pour former la taille de la guitare : filet, puis table.
    drawOval(g, 0f, 168f, 37f, 41f, Binding)
    drawOval(g, 0f, 224f, 47f, 39f, Binding)
    drawOval(g, 0f, 168f, 35f, 39f, SpruceEdge)
    drawOval(g, 0f, 224f, 45f, 37f, SpruceEdge)
    drawOval(g, 0f, 168f, 32f, 36f, SpruceTop)
    drawOval(g, 0f, 224f, 42f, 34f, SpruceTop)

    // Rosace et bouche
    drawCircle(Rosette, radius = 18f * g.scale, center = g.point(0f, 172f))
    drawCircle(RosetteInner, radius = 15.5f * g.scale, center = g.point(0f, 172f))
    drawCircle(SpruceTop, radius = 14.5f * g.scale, center = g.point(0f, 172f))
    drawCircle(SoundHole, radius = 13f * g.scale, center = g.point(0f, 172f))

    // Chevalet et sillet de chevalet
    drawRoundRect(
        DarkWood,
        topLeft = g.point(-24f, 227f),
        size = Size(48f * g.scale, 9f * g.scale),
        cornerRadius = CornerRadius(2f * g.scale),
    )
    drawLine(Bone, g.point(-15f, 229.5f), g.point(15f, 229.5f), strokeWidth = 1.6f * g.scale)
}

private fun DrawScope.drawNeck(g: GuitarGeometry) {
    val neck = Path().apply {
        val a = g.point(-12f, GuitarGeometry.NUT_Y)
        val b = g.point(12f, GuitarGeometry.NUT_Y)
        val c = g.point(15f, GuitarGeometry.BODY_JOINT_Y + 10f)
        val d = g.point(-15f, GuitarGeometry.BODY_JOINT_Y + 10f)
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
    }
    drawPath(neck, Fretboard)

    // Frettes placées selon la règle des 12 demi-tons par octave (diapason fictif de 230 unités).
    val scaleLength = 230f
    for (fret in 1..12) {
        val y = GuitarGeometry.NUT_Y + scaleLength * (1 - 2f.pow(-fret / 12f))
        if (y > GuitarGeometry.BODY_JOINT_Y + 8f) break
        val half = 12f + 3f * (y - GuitarGeometry.NUT_Y) / (GuitarGeometry.BODY_JOINT_Y + 10f - GuitarGeometry.NUT_Y)
        drawLine(FretMetal, g.point(-half, y), g.point(half, y), strokeWidth = 0.9f * g.scale)
    }
    // Repères aux cases 5, 7 et 9
    for (fret in intArrayOf(5, 7, 9)) {
        val y = GuitarGeometry.NUT_Y + scaleLength * (1 - 2f.pow(-(fret - 0.5f) / 12f))
        drawCircle(Bone.copy(alpha = 0.5f), radius = 1.4f * g.scale, center = g.point(0f, y))
    }
    // Sillet de tête
    drawLine(Bone, g.point(-12.5f, GuitarGeometry.NUT_Y), g.point(12.5f, GuitarGeometry.NUT_Y), strokeWidth = 2.2f * g.scale)
}

private fun DrawScope.drawHead(g: GuitarGeometry) {
    val head = Path().apply {
        val start = g.point(-13f, GuitarGeometry.NUT_Y)
        moveTo(start.x, start.y)
        g.point(-17f, 4f).let { lineTo(it.x, it.y) }
        val c1 = g.point(-8f, -2f)
        val mid = g.point(0f, 3f)
        quadraticTo(c1.x, c1.y, mid.x, mid.y)
        val c2 = g.point(8f, -2f)
        val end = g.point(17f, 4f)
        quadraticTo(c2.x, c2.y, end.x, end.y)
        g.point(13f, GuitarGeometry.NUT_Y).let { lineTo(it.x, it.y) }
        close()
    }
    drawPath(head, DarkWood)
    // Les deux fentes de la tête classique, où passent les rouleaux des mécaniques
    for (side in floatArrayOf(-1f, 1f)) {
        drawRoundRect(
            Fretboard,
            topLeft = g.point(side * GuitarGeometry.SLOT_X - 2.6f, 6f),
            size = Size(5.2f * g.scale, 36f * g.scale),
            cornerRadius = CornerRadius(2.6f * g.scale),
        )
    }
}

private fun DrawScope.drawPeg(
    g: GuitarGeometry,
    index: Int,
    string: GuitarString,
    glow: Float,
    highlight: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val side = GuitarGeometry.pegSide(index)
    val y = GuitarGeometry.PEG_Y[index]
    val knob = g.point(side * GuitarGeometry.KNOB_X, y)
    drawLine(PegColor, g.point(side * GuitarGeometry.SLOT_X, y), knob, strokeWidth = 1.6f * g.scale)
    if (glow > 0f) {
        drawCircle(highlight.copy(alpha = 0.35f * glow), radius = (GuitarGeometry.KNOB_R + 3f) * g.scale, center = knob)
    }
    drawCircle(lerp(PegColor, highlight, glow), radius = GuitarGeometry.KNOB_R * g.scale, center = knob)

    val label = textMeasurer.measure(
        string.name.take(1),
        TextStyle(
            fontSize = (7f * g.scale).toSp(),
            fontWeight = FontWeight.Bold,
            color = lerp(DarkWood, Color.White, glow),
        ),
    )
    drawText(
        label,
        topLeft = Offset(knob.x - label.size.width / 2f, knob.y - label.size.height / 2f),
    )
}

private fun DrawScope.drawString(g: GuitarGeometry, index: Int, glow: Float, highlight: Color) {
    val side = GuitarGeometry.pegSide(index)
    val roller = g.point(side * GuitarGeometry.SLOT_X, GuitarGeometry.PEG_Y[index])
    val nut = g.point(g.stringX(index, GuitarGeometry.NUT_Y), GuitarGeometry.NUT_Y)
    val bridge = g.point(g.stringX(index, GuitarGeometry.BRIDGE_Y), GuitarGeometry.BRIDGE_Y)

    // Cordes filées (graves) plus épaisses que les cordes en nylon (aiguës)
    val base = floatArrayOf(1.5f, 1.3f, 1.15f, 1.0f, 0.85f, 0.75f)[index] * g.scale
    val color = if (index < 3) WoundString else NylonString

    if (glow > 0f) {
        val halo = highlight.copy(alpha = 0.35f * glow)
        drawLine(halo, roller, nut, strokeWidth = base + 5f * g.scale * glow, cap = StrokeCap.Round)
        drawLine(halo, nut, bridge, strokeWidth = base + 5f * g.scale * glow, cap = StrokeCap.Round)
    }
    val width = base * (1f + 0.8f * glow)
    val lineColor = lerp(color, highlight, glow)
    // Ombre légère pour que les cordes claires restent lisibles sur la table
    drawLine(Color.Black.copy(alpha = 0.25f), nut + Offset(0.6f * g.scale, 0.6f * g.scale), bridge + Offset(0.6f * g.scale, 0.6f * g.scale), strokeWidth = width)
    drawLine(lineColor, roller, nut, strokeWidth = width, cap = StrokeCap.Round)
    drawLine(lineColor, nut, bridge, strokeWidth = width, cap = StrokeCap.Round)
}

private fun lerp(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))

