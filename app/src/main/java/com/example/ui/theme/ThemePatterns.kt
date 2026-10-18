package com.example.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Deterministic pseudo-random source so starfields and sand drifts are stable
 * across recompositions and process restarts (a real Random would flicker).
 */
internal class SeededRandom(seed: Long) {
    private var state: Long = if (seed == 0L) 88172645463325252L else seed
    fun nextLong(): Long {
        state = state xor (state shl 13)
        state = state xor (state shr 7)
        state = state xor (state shl 17)
        return state
    }
    fun nextFloat(): Float = ((nextLong() ushr 11).toDouble() / (1L shl 53).toDouble()).toFloat()
    fun nextIn(from: Float, until: Float): Float = from + nextFloat() * (until - from)
}

internal data class PatternPalette(
    val background: Color,
    val line: Color,
    val accent: Color,
    val accent2: Color
)

/* ============================================================================
   PATTERN DISPATCH
   ========================================================================== */

internal object ThemePatterns {
    fun baseAlpha(intensity: Float) = (0.095f * intensity.coerceIn(0.5f, 1.5f)).coerceAtMost(0.18f)
}

/** Single entry point: draws the theme's signature surface pattern & architectural atmosphere. */
internal fun DrawScope.drawThemePattern(pattern: ThemePattern, p: PatternPalette, intensity: Float) {
    when (pattern) {
        ThemePattern.TECH_GRID -> techGrid(p, intensity)
        ThemePattern.TUGHRA_BAND -> tughraBand(p, intensity)
        ThemePattern.AZULEJO -> azulejo(p, intensity)
        ThemePattern.EBRE -> ebre(p, intensity)
        ThemePattern.ZELLIGE -> zellige(p, intensity)
        ThemePattern.CLEAN -> Unit
        ThemePattern.WHIRL -> whirl(p, intensity)
        ThemePattern.KAABA_LINES -> kaabaLines(p, intensity)
        ThemePattern.SAND_DRIFT -> sandDrift(p, intensity)
        ThemePattern.STARFIELD -> starfield(p, intensity)
        ThemePattern.ARABESQUE -> arabesque(p, intensity)
        ThemePattern.OLIVE_DAWN -> oliveDawn(p, intensity)
        ThemePattern.PAPER_ATELIER -> paperAtelier(p, intensity)
        ThemePattern.M3_SUNSET -> m3Sunset(p, intensity)
        ThemePattern.COASTAL_PATH -> coastalPath(p, intensity)
        ThemePattern.KRAFT_WARM -> kraftWarm(p, intensity)
        ThemePattern.PASTEL_KNOT -> pastelKnot(p, intensity)
        ThemePattern.WORDMARK -> wordmark(p, intensity)
        ThemePattern.QUIET_GRID -> quietGrid(p, intensity)
        ThemePattern.HAIRLINE -> hairline(p, intensity)
        ThemePattern.PLAIN_VOICE -> plainVoice(p, intensity)
    }
}

/* ============================================================================
   SHARED ARCHITECTURAL MOTIFS (Rosettes, Arches, Horizon Silhouettes)
   ========================================================================== */

private fun DrawScope.drawEightPointStar(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float,
    filledInner: Boolean = false
) {
    for (rot in listOf(0f, 45f)) {
        rotate(rot, pivot = center) {
            val sq = Path().apply {
                moveTo(center.x - radius, center.y - radius)
                lineTo(center.x + radius, center.y - radius)
                lineTo(center.x + radius, center.y + radius)
                lineTo(center.x - radius, center.y + radius)
                close()
            }
            drawPath(sq, color, style = Stroke(strokeWidth))
        }
    }
    if (filledInner) {
        drawCircle(color.copy(alpha = (color.alpha * 0.65f).coerceIn(0f, 1f)), radius * 0.28f, center)
    }
}

private fun DrawScope.drawPointedMihrabWatermark(
    centerTop: Offset,
    width: Float,
    height: Float,
    color: Color,
    strokeWidth: Float
) {
    val left = centerTop.x - width / 2f
    val right = centerTop.x + width / 2f
    val archSpringY = centerTop.y + height * 0.42f
    val bottomY = centerTop.y + height
    val path = Path().apply {
        moveTo(left, bottomY)
        lineTo(left, archSpringY)
        cubicTo(
            left, centerTop.y + height * 0.14f,
            centerTop.x - width * 0.18f, centerTop.y + height * 0.04f,
            centerTop.x, centerTop.y
        )
        cubicTo(
            centerTop.x + width * 0.18f, centerTop.y + height * 0.04f,
            right, centerTop.y + height * 0.14f,
            right, archSpringY
        )
        lineTo(right, bottomY)
    }
    drawPath(path, color, style = Stroke(strokeWidth))
}

private fun DrawScope.drawHangingKandil(
    topAnchor: Offset,
    chainLength: Float,
    lampRadius: Float,
    color: Color,
    glowColor: Color
) {
    val lampCenter = Offset(topAnchor.x, topAnchor.y + chainLength + lampRadius)
    drawLine(
        color = color,
        start = topAnchor,
        end = Offset(topAnchor.x, topAnchor.y + chainLength),
        strokeWidth = 1.dp.toPx()
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glowColor.copy(alpha = (color.alpha * 1.4f).coerceIn(0f, 0.35f)), Color.Transparent),
            center = lampCenter,
            radius = lampRadius * 3.5f
        ),
        radius = lampRadius * 3.5f,
        center = lampCenter
    )
    val body = Path().apply {
        moveTo(lampCenter.x, lampCenter.y - lampRadius)
        quadraticTo(
            lampCenter.x + lampRadius * 1.15f, lampCenter.y,
            lampCenter.x, lampCenter.y + lampRadius * 1.35f
        )
        quadraticTo(
            lampCenter.x - lampRadius * 1.15f, lampCenter.y,
            lampCenter.x, lampCenter.y - lampRadius
        )
        close()
    }
    drawPath(body, color, style = Stroke(1.2.dp.toPx()))
    drawCircle(glowColor.copy(alpha = (color.alpha * 1.2f).coerceIn(0f, 0.45f)), lampRadius * 0.38f, lampCenter)
}

/* ============================================================================
   1. TECH GRID — Kubbeli Zümrüt (Emerald Dome signature background)
   ========================================================================== */

private fun DrawScope.techGrid(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity)
    val grid = 56.dp.toPx()
    val fine = 14.dp.toPx()

    // Subtle radial emerald-gold dome aura at top center
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                p.accent2.copy(alpha = (alpha * 1.1f).coerceAtMost(0.20f)),
                p.accent.copy(alpha = (alpha * 0.55f).coerceAtMost(0.12f)),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.14f),
            radius = min(size.width, size.height) * 0.72f
        ),
        radius = min(size.width, size.height) * 0.72f,
        center = Offset(size.width * 0.5f, size.height * 0.14f)
    )

    var x = 0f
    while (x <= size.width) {
        drawLine(p.line.copy(alpha = alpha * 0.55f), Offset(x, 0f), Offset(x, size.height), 0.6.dp.toPx())
        x += fine
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(p.line.copy(alpha = alpha * 0.55f), Offset(0f, y), Offset(size.width, y), 0.6.dp.toPx())
        y += fine
    }
    x = 0f
    while (x <= size.width + size.height) {
        drawLine(p.accent.copy(alpha = alpha * 0.65f), Offset(x, 0f), Offset(x - size.height, size.height), 0.8.dp.toPx())
        x += grid
    }
    // Classic mihrab arch watermark
    drawPointedMihrabWatermark(
        centerTop = Offset(size.width * 0.5f, size.height * 0.06f),
        width = size.width * 0.86f,
        height = size.height * 0.55f,
        color = p.accent2.copy(alpha = alpha * 0.85f),
        strokeWidth = 1.2.dp.toPx()
    )
}

/* ============================================================================
   2. TUGHRA BAND — Kubbeli Yakut (Imperial Hünkâr Mahfili & Velvet Frieze)
   ========================================================================== */

private fun DrawScope.tughraBand(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.25f
    val band = 114.dp.toPx()
    val stroke = 1.dp.toPx()

    // Rich crimson-gold radial glow from top arch
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                p.accent2.copy(alpha = (alpha * 1.25f).coerceAtMost(0.24f)),
                p.accent.copy(alpha = (alpha * 0.7f).coerceAtMost(0.14f)),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.12f),
            radius = size.width * 0.85f
        ),
        radius = size.width * 0.85f,
        center = Offset(size.width * 0.5f, size.height * 0.12f)
    )

    // Vertical ruling, the way Ottoman calligraphy was ruled on silk paper
    var x = 14.dp.toPx()
    while (x < size.width) {
        drawLine(p.line.copy(alpha = alpha * 0.45f), Offset(x, 0f), Offset(x, size.height), 0.6.dp.toPx())
        x += 12.dp.toPx()
    }

    var bandIndex = 0
    var y = band * 0.52f
    while (y < size.height) {
        val isGoldBand = bandIndex % 2 == 0
        val color = (if (isGoldBand) p.accent2 else p.accent).copy(alpha = (alpha * 1.15f).coerceAtMost(0.26f))
        val seg = 78.dp.toPx()
        var sx = -seg * 0.5f
        while (sx < size.width + seg) {
            tughraLeaf(Offset(sx, y), seg, color)
            sx += seg
        }
        y += band
        bandIndex++
    }

    // Ceremonial double rule beneath each band + corner rosettes
    var ry = band * 0.52f + 16.dp.toPx()
    while (ry < size.height) {
        drawLine(p.accent2.copy(alpha = (alpha * 0.9f).coerceAtMost(0.22f)), Offset(0f, ry), Offset(size.width, ry), 1.3.dp.toPx())
        drawLine(
            p.accent.copy(alpha = (alpha * 0.55f).coerceAtMost(0.15f)),
            Offset(0f, ry + 3.dp.toPx()), Offset(size.width, ry + 3.dp.toPx()), stroke * 0.7f
        )
        ry += band
    }

    // Imperial lobed arch watermark at top
    drawPointedMihrabWatermark(
        centerTop = Offset(size.width * 0.5f, size.height * 0.04f),
        width = size.width * 0.90f,
        height = size.height * 0.48f,
        color = p.accent2.copy(alpha = (alpha * 1.1f).coerceAtMost(0.24f)),
        strokeWidth = 1.6.dp.toPx()
    )
}

private fun DrawScope.tughraLeaf(origin: Offset, seg: Float, color: Color) {
    val h = seg * 0.36f
    val outer = Path().apply {
        moveTo(origin.x, origin.y)
        cubicTo(
            origin.x + seg * 0.28f, origin.y - h,
            origin.x + seg * 0.72f, origin.y + h,
            origin.x + seg, origin.y
        )
    }
    drawPath(outer, color, style = Stroke(width = 1.1.dp.toPx()))

    val inner = Path().apply {
        moveTo(origin.x + seg * 0.18f, origin.y)
        cubicTo(
            origin.x + seg * 0.38f, origin.y - h * 0.55f,
            origin.x + seg * 0.62f, origin.y + h * 0.55f,
            origin.x + seg * 0.82f, origin.y
        )
    }
    drawPath(inner, color.copy(alpha = color.alpha * 0.75f), style = Stroke(width = 0.75.dp.toPx()))
    drawCircle(color, 2.dp.toPx(), Offset(origin.x + seg * 0.5f, origin.y))
}

/* ============================================================================
   3. AZULEJO — Kubbeli Safir (Sultanahmet Iznik Tile & Kandil Medallions)
   ========================================================================== */

private fun DrawScope.azulejo(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.35f
    val cell = 48.dp.toPx()
    val stroke = 0.9.dp.toPx()

    // Deep sapphire dome radial light
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                p.accent2.copy(alpha = (alpha * 1.1f).coerceAtMost(0.22f)),
                p.accent.copy(alpha = (alpha * 0.65f).coerceAtMost(0.14f)),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.16f),
            radius = size.width * 0.80f
        ),
        radius = size.width * 0.80f,
        center = Offset(size.width * 0.5f, size.height * 0.16f)
    )

    var row = 0
    var y = -cell
    while (y < size.height + cell) {
        var col = 0
        var x = -cell + (if (row % 2 == 0) 0f else cell / 2f)
        while (x < size.width + cell) {
            azulejoStar(
                center = Offset(x, y),
                r = cell * 0.44f,
                color = p.accent.copy(alpha = (alpha * 0.95f).coerceAtMost(0.22f)),
                diamondColor = p.accent2.copy(alpha = (alpha * 0.85f).coerceAtMost(0.20f)),
                stroke = stroke,
                rotate45 = (row + col) % 2 != 0
            )
            col++
            x += cell
        }
        row++
        y += cell * 0.86f
    }

    // Twin hanging Sultanahmet kandils in upper corners
    val kandilColor = p.accent2.copy(alpha = (alpha * 1.25f).coerceAtMost(0.28f))
    drawHangingKandil(Offset(size.width * 0.14f, 0f), 42.dp.toPx(), 10.dp.toPx(), kandilColor, p.accent2)
    drawHangingKandil(Offset(size.width * 0.86f, 0f), 42.dp.toPx(), 10.dp.toPx(), kandilColor, p.accent2)
}

private fun DrawScope.azulejoStar(
    center: Offset,
    r: Float,
    color: Color,
    diamondColor: Color,
    stroke: Float,
    rotate45: Boolean
) {
    rotate(if (rotate45) 45f else 0f, pivot = center) {
        val square = Path().apply {
            moveTo(center.x - r, center.y - r)
            lineTo(center.x + r, center.y - r)
            lineTo(center.x + r, center.y + r)
            lineTo(center.x - r, center.y + r)
            close()
        }
        drawPath(square, color, style = Stroke(width = stroke))

        val diamond = Path().apply {
            moveTo(center.x, center.y - r * 1.18f)
            lineTo(center.x + r * 1.18f, center.y)
            lineTo(center.x, center.y + r * 1.18f)
            lineTo(center.x - r * 1.18f, center.y)
            close()
        }
        drawPath(diamond, diamondColor, style = Stroke(width = stroke * 0.85f))
        drawCircle(diamondColor.copy(alpha = (diamondColor.alpha * 0.7f).coerceIn(0f, 1f)), r * 0.16f, center)
    }
}

/* ============================================================================
   4. EBRE — Kubbeli Ametist (Ottoman Ebru Marbling & Tezhip Gold Dust)
   ========================================================================== */

private fun DrawScope.ebre(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.55f
    val rnd = SeededRandom(0x51A7E)
    val bands = 9

    // Soft amethyst-gold nebula wash
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                p.accent.copy(alpha = (alpha * 0.9f).coerceAtMost(0.22f)),
                p.accent2.copy(alpha = (alpha * 0.5f).coerceAtMost(0.12f)),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.25f),
            radius = size.width * 0.9f
        ),
        radius = size.width * 0.9f,
        center = Offset(size.width * 0.5f, size.height * 0.25f)
    )

    // Ebru: floated pigment combed into organic horizontal currents
    for (band in 0 until bands) {
        val baseY = size.height * (band + 0.4f) / bands
        val amp = rnd.nextIn(10f, 32f).dp.toPx()
        val wavelength = rnd.nextIn(90f, 220f).dp.toPx()
        val thickness = rnd.nextIn(1.6f, 5.2f).dp.toPx()
        val color = (if (band % 3 == 0) p.accent2 else if (band % 3 == 1) p.accent else p.line)
            .copy(alpha = (alpha * rnd.nextIn(0.75f, 1.25f)).coerceAtMost(0.26f))

        val path = Path()
        var x = -20f
        var first = true
        while (x <= size.width + 20f) {
            val phase = (x / wavelength) * TWO_PI
            val y = baseY + sin(phase + band * 1.3f) * amp + cos(phase * 0.47f + band) * amp * 0.38f
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
            x += 6f
        }
        drawPath(path, color, style = Stroke(width = thickness))
    }

    // Tezhip gold droplets (serpme altın)
    for (i in 0 until 38) {
        val dropletCenter = Offset(rnd.nextIn(0f, size.width), rnd.nextIn(0f, size.height))
        val dropletR = rnd.nextIn(1.4f, 5.0f).dp.toPx()
        drawCircle(
            color = (if (i % 2 == 0) p.accent2 else p.accent).copy(alpha = (alpha * 0.95f).coerceAtMost(0.25f)),
            radius = dropletR,
            center = dropletCenter
        )
        if (i % 6 == 0) {
            drawCircle(
                color = p.accent2.copy(alpha = (alpha * 0.5f).coerceAtMost(0.16f)),
                radius = dropletR * 2.1f,
                center = dropletCenter,
                style = Stroke(0.8.dp.toPx())
            )
        }
    }
}

/* ============================================================================
   5. ZELLIGE — Kubbeli Firuze (Samarkand Registan Glazed Mosaic Rosettes)
   ========================================================================== */

private fun DrawScope.zellige(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.35f
    val cell = 64.dp.toPx()
    val stroke = 1.dp.toPx()
    val color = p.accent.copy(alpha = (alpha * 1.05f).coerceAtMost(0.22f))
    val pip = p.accent2.copy(alpha = (alpha * 1.15f).coerceAtMost(0.26f))

    // Turquoise ribbed dome halo at top
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                p.accent2.copy(alpha = (alpha * 1.1f).coerceAtMost(0.22f)),
                p.accent.copy(alpha = (alpha * 0.7f).coerceAtMost(0.14f)),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.14f),
            radius = size.width * 0.85f
        ),
        radius = size.width * 0.85f,
        center = Offset(size.width * 0.5f, size.height * 0.14f)
    )

    var row = 0
    var y = -cell * 0.5f
    while (y < size.height + cell) {
        var col = 0
        var x = -cell * 0.5f + (if (row % 2 == 0) 0f else cell / 2f)
        while (x < size.width + cell) {
            zelligeRosette(Offset(x, y), cell * 0.44f, color, pip, stroke, (row + col) % 3)
            col++
            x += cell
        }
        row++
        y += cell * 0.72f
    }
}

private fun DrawScope.zelligeRosette(
    center: Offset,
    r: Float,
    color: Color,
    pip: Color,
    stroke: Float,
    variant: Int
) {
    val inner = r * 0.42f
    drawCircle(color.copy(alpha = color.alpha * 0.7f), r * 0.9f, center, style = Stroke(stroke))
    drawCircle(pip.copy(alpha = pip.alpha * 0.85f), inner, center, style = Stroke(stroke))

    rotate(variant * 15f, pivot = center) {
        for (i in 0 until 8) {
            val a = (i * 45f) * DEG_TO_RAD
            val tipX = center.x + cos(a) * r * 0.9f
            val tipY = center.y + sin(a) * r * 0.9f
            val petal = Path().apply {
                moveTo(center.x + cos(a) * inner, center.y + sin(a) * inner)
                quadraticTo(
                    center.x + cos(a - 0.32f) * r * 0.66f,
                    center.y + sin(a - 0.32f) * r * 0.66f,
                    tipX, tipY
                )
                quadraticTo(
                    center.x + cos(a + 0.32f) * r * 0.66f,
                    center.y + sin(a + 0.32f) * r * 0.66f,
                    center.x + cos(a) * inner, center.y + sin(a) * inner
                )
                close()
            }
            drawPath(petal, color.copy(alpha = color.alpha * 0.7f), style = Stroke(stroke * 0.9f))
        }
    }
    drawCircle(pip, inner * 0.42f, center)
}

/* ============================================================================
   7. WHIRL — Semazen
   ========================================================================== */

private fun DrawScope.whirl(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.4f
    val center = Offset(size.width / 2f, size.height * 0.42f)
    val maxR = min(size.width, size.height) * 1.15f
    val stroke = 1.dp.toPx()

    var r = maxR * 0.12f
    var i = 0
    while (r < maxR) {
        val t = (i / 9f).coerceIn(0f, 1f)
        drawCircle(
            color = lerpColor(p.accent, p.accent2, t).copy(alpha = alpha * (1f - t * 0.65f)),
            radius = r,
            center = center,
            style = Stroke(stroke * (1f - t * 0.4f))
        )
        r *= 1.17f
        i++
    }

    for (k in 0 until 16) {
        val a = (k * 22.5f) * DEG_TO_RAD
        val dirX = cos(a)
        val dirY = sin(a)
        val path = Path().apply {
            moveTo(center.x, center.y)
            quadraticTo(
                center.x + dirX * maxR * 0.34f - dirY * maxR * 0.11f,
                center.y + dirY * maxR * 0.34f + dirX * maxR * 0.11f,
                center.x + dirX * maxR * 0.62f,
                center.y + dirY * maxR * 0.62f
            )
        }
        drawPath(path, p.accent2.copy(alpha = alpha * 0.6f), style = Stroke(stroke * 0.85f))
    }
}

/* ============================================================================
   8. KAABA_LINES — Kubbeli Kâbe Gecesi (Kiswah Black Silk & Gold Hizam Belt)
   ========================================================================== */

private fun DrawScope.kaabaLines(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.65f
    val gold = p.accent2.copy(alpha = (alpha * 1.25f).coerceAtMost(0.32f))
    val silkWeave = p.line.copy(alpha = (alpha * 0.55f).coerceAtMost(0.16f))

    // Kiswah chevron silk weave (damask texture) across the background
    val zigStep = 28.dp.toPx()
    var zy = 0f
    while (zy < size.height + zigStep) {
        val path = Path()
        var zx = 0f
        var up = true
        path.moveTo(0f, zy)
        while (zx <= size.width + zigStep) {
            path.lineTo(zx + zigStep * 0.5f, if (up) zy - 5.dp.toPx() else zy + 5.dp.toPx())
            zx += zigStep * 0.5f
            up = !up
        }
        drawPath(path, silkWeave, style = Stroke(0.7.dp.toPx()))
        zy += 22.dp.toPx()
    }

    // Golden Hizam (Kâbe Kuşağı) ornamental band across upper screen
    val bandY = size.height * 0.17f
    val bandHeight = 28.dp.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                gold.copy(alpha = (gold.alpha * 0.35f).coerceIn(0f, 1f)),
                gold.copy(alpha = (gold.alpha * 0.12f).coerceIn(0f, 1f)),
                gold.copy(alpha = (gold.alpha * 0.35f).coerceIn(0f, 1f))
            ),
            startY = bandY,
            endY = bandY + bandHeight
        ),
        topLeft = Offset(0f, bandY),
        size = Size(size.width, bandHeight)
    )
    drawLine(gold, Offset(0f, bandY), Offset(size.width, bandY), 1.8.dp.toPx(), cap = StrokeCap.Butt)
    drawLine(gold, Offset(0f, bandY + bandHeight), Offset(size.width, bandY + bandHeight), 1.8.dp.toPx(), cap = StrokeCap.Butt)
    drawLine(
        gold.copy(alpha = gold.alpha * 0.65f),
        Offset(0f, bandY + 4.dp.toPx()), Offset(size.width, bandY + 4.dp.toPx()), 0.8.dp.toPx()
    )
    drawLine(
        gold.copy(alpha = gold.alpha * 0.65f),
        Offset(0f, bandY + bandHeight - 4.dp.toPx()), Offset(size.width, bandY + bandHeight - 4.dp.toPx()), 0.8.dp.toPx()
    )

    // Calligraphic cartouche dividers & medallions inside the Hizam belt
    val cartoucheW = 72.dp.toPx()
    var cx = 20.dp.toPx()
    while (cx < size.width) {
        drawEightPointStar(
            center = Offset(cx, bandY + bandHeight * 0.5f),
            radius = 7.dp.toPx(),
            color = gold,
            strokeWidth = 1.dp.toPx(),
            filledInner = true
        )
        cx += cartoucheW
    }

    // Three golden hanging kandils from top
    drawHangingKandil(Offset(size.width * 0.18f, 0f), 36.dp.toPx(), 9.dp.toPx(), gold, p.accent2)
    drawHangingKandil(Offset(size.width * 0.50f, 0f), 26.dp.toPx(), 11.dp.toPx(), gold, p.accent2)
    drawHangingKandil(Offset(size.width * 0.82f, 0f), 36.dp.toPx(), 9.dp.toPx(), gold, p.accent2)
}

/* ============================================================================
   9. SAND_DRIFT — Kubbeli Sahra (Hijaz Sand Dunes & Andalusian Horseshoe Arch)
   ========================================================================== */

private fun DrawScope.sandDrift(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.45f
    val rnd = SeededRandom(0xD35E47)

    // Warm desert sun / oasis glow at upper horizon
    val sunCenter = Offset(size.width * 0.76f, size.height * 0.16f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                p.accent2.copy(alpha = (alpha * 1.4f).coerceAtMost(0.28f)),
                p.accent.copy(alpha = (alpha * 0.6f).coerceAtMost(0.14f)),
                Color.Transparent
            ),
            center = sunCenter,
            radius = 140.dp.toPx()
        ),
        radius = 140.dp.toPx(),
        center = sunCenter
    )
    drawEightPointStar(sunCenter, 18.dp.toPx(), p.accent2.copy(alpha = (alpha * 1.2f).coerceAtMost(0.26f)), 1.2.dp.toPx(), true)

    // Layered desert sand dunes at the lower third
    for (dune in 0 until 4) {
        val baseY = size.height * (0.58f + dune * 0.11f)
        val dunePath = Path().apply {
            moveTo(0f, size.height)
            lineTo(0f, baseY)
            cubicTo(
                size.width * (0.28f + dune * 0.05f), baseY - (34 - dune * 5).dp.toPx(),
                size.width * (0.65f - dune * 0.04f), baseY + (24 - dune * 3).dp.toPx(),
                size.width, baseY - 14.dp.toPx()
            )
            lineTo(size.width, size.height)
            close()
        }
        drawPath(
            path = dunePath,
            color = (if (dune % 2 == 0) p.accent else p.accent2).copy(alpha = (alpha * (0.32f + dune * 0.12f)).coerceAtMost(0.16f))
        )
    }

    // Wind-swept sand ripple lines
    val strokes = 18
    for (i in 0 until strokes) {
        val y = size.height * (i + rnd.nextIn(0.1f, 0.9f)) / strokes
        val amplitude = rnd.nextIn(3f, 12f).dp.toPx()
        val wavelength = rnd.nextIn(80f, 210f).dp.toPx()
        val weight = rnd.nextIn(0.6f, 1.8f).dp.toPx()
        val color = (if (i % 3 == 0) p.accent2 else p.line).copy(alpha = (alpha * rnd.nextIn(0.6f, 1.1f)).coerceAtMost(0.22f))

        val path = Path()
        var x = -10f
        var first = true
        while (x <= size.width + 10f) {
            val phase = (x / wavelength) * TWO_PI
            val yy = y + sin(phase + i * 0.9f) * amplitude
            if (first) {
                path.moveTo(x, yy)
                first = false
            } else {
                path.lineTo(x, yy)
            }
            x += 8f
        }
        drawPath(path, color, style = Stroke(width = weight))
    }
}

/* ============================================================================
   10. STARFIELD — Kubbeli Gece Mavisi (Observatory Astrolabe & Night Sky)
   ========================================================================== */

private fun DrawScope.starfield(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.75f
    val rnd = SeededRandom(0xCE1E57)

    // Milky way diagonal nebula wash
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                p.accent.copy(alpha = (alpha * 0.55f).coerceAtMost(0.18f)),
                p.accent2.copy(alpha = (alpha * 0.35f).coerceAtMost(0.12f)),
                Color.Transparent
            ),
            start = Offset(0f, size.height * 0.10f),
            end = Offset(size.width, size.height * 0.72f)
        )
    )

    // Ulugh Beg Observatory Astrolabe concentric celestial rings
    val astroCenter = Offset(size.width * 0.5f, size.height * 0.24f)
    val astroRadius = min(size.width, size.height) * 0.44f
    val goldOrbit = p.accent2.copy(alpha = (alpha * 0.75f).coerceAtMost(0.22f))
    drawCircle(goldOrbit, astroRadius, astroCenter, style = Stroke(1.1.dp.toPx()))
    drawCircle(goldOrbit.copy(alpha = goldOrbit.alpha * 0.7f), astroRadius * 0.74f, astroCenter, style = Stroke(0.8.dp.toPx()))
    drawCircle(goldOrbit.copy(alpha = goldOrbit.alpha * 0.5f), astroRadius * 0.48f, astroCenter, style = Stroke(0.7.dp.toPx()))

    // Astrolabe degree ticks and cardinal crosshairs
    for (deg in 0 until 360 step 15) {
        val rad = deg * DEG_TO_RAD
        val innerR = if (deg % 45 == 0) astroRadius * 0.90f else astroRadius * 0.95f
        drawLine(
            color = goldOrbit,
            start = Offset(astroCenter.x + cos(rad) * innerR, astroCenter.y + sin(rad) * innerR),
            end = Offset(astroCenter.x + cos(rad) * astroRadius, astroCenter.y + sin(rad) * astroRadius),
            strokeWidth = if (deg % 45 == 0) 1.2.dp.toPx() else 0.6.dp.toPx()
        )
    }

    // Starfield with 4-point sparkling stars
    for (i in 0 until 130) {
        val magnitude = rnd.nextFloat()
        val starPos = Offset(rnd.nextIn(0f, size.width), rnd.nextIn(0f, size.height))
        val color = (if (i % 4 == 0) p.accent2 else p.line)
            .copy(alpha = (alpha * (0.4f + magnitude * 0.9f)).coerceAtMost(0.55f))
        val r = (0.5f + magnitude * 2.0f).dp.toPx()
        drawCircle(color = color, radius = r, center = starPos)
        if (i % 14 == 0) {
            drawLine(color, Offset(starPos.x - r * 3f, starPos.y), Offset(starPos.x + r * 3f, starPos.y), 0.7.dp.toPx())
            drawLine(color, Offset(starPos.x, starPos.y - r * 3f), Offset(starPos.x, starPos.y + r * 3f), 0.7.dp.toPx())
        }
    }

    // Golden Crescent Moon at top right
    val center = Offset(size.width * 0.84f, size.height * 0.11f)
    val radius = 26.dp.toPx()
    drawCircle(p.accent2.copy(alpha = (alpha * 1.2f).coerceAtMost(0.36f)), radius, center)
    drawCircle(p.background.copy(alpha = 0.94f), radius * 0.88f, center + Offset(radius * 0.34f, -radius * 0.16f))
}

/* ============================================================================
   11. ARABESQUE — Kubbeli Kehribar (Seljuk 8-Pointed Star & Rumi Scrollwork)
   ========================================================================== */

private fun DrawScope.arabesque(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.35f
    val cell = 60.dp.toPx()
    val stroke = 0.95.dp.toPx()
    val color = p.accent2.copy(alpha = (alpha * 1.1f).coerceAtMost(0.24f))
    val pip = p.accent.copy(alpha = (alpha * 0.95f).coerceAtMost(0.22f))

    // Warm amber Seljuk portal glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                p.accent2.copy(alpha = (alpha * 1.15f).coerceAtMost(0.22f)),
                p.accent.copy(alpha = (alpha * 0.6f).coerceAtMost(0.12f)),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.18f),
            radius = size.width * 0.82f
        ),
        radius = size.width * 0.82f,
        center = Offset(size.width * 0.5f, size.height * 0.18f)
    )

    var row = 0
    var y = -cell * 0.5f
    while (y < size.height + cell) {
        var col = 0
        var x = -cell * 0.5f + (if (row % 2 == 0) 0f else cell / 2f)
        while (x < size.width + cell) {
            arabesqueUnit(Offset(x, y), cell * 0.42f, color, pip, stroke)
            if ((row + col) % 2 == 0) {
                drawEightPointStar(Offset(x, y), cell * 0.18f, color.copy(alpha = color.alpha * 0.75f), stroke * 0.8f, false)
            }
            col++
            x += cell
        }
        row++
        y += cell * 0.68f
    }
}

private fun DrawScope.arabesqueUnit(
    center: Offset,
    r: Float,
    color: Color,
    pip: Color,
    stroke: Float
) {
    for (flip in 0..1) {
        val sx = if (flip == 0) 1f else -1f

        val stem = Path().apply {
            moveTo(center.x, center.y + r * 0.55f)
            cubicTo(
                center.x + sx * r * 0.75f, center.y + r * 0.15f,
                center.x + sx * r * 0.55f, center.y - r * 0.55f,
                center.x, center.y - r * 0.75f
            )
        }
        drawPath(stem, color, style = Stroke(stroke))

        val offsets = floatArrayOf(-0.28f, 0.22f)
        for (idx in offsets.indices) {
            val lx = center.x + sx * r * 0.42f
            val ly = center.y + r * offsets[idx]
            val leaf = Path().apply {
                moveTo(lx, ly)
                quadraticTo(lx + sx * r * 0.46f, ly - r * 0.3f, lx + sx * r * 0.2f, ly - r * 0.52f)
                quadraticTo(lx - sx * r * 0.08f, ly - r * 0.3f, lx, ly)
                close()
            }
            drawPath(
                leaf,
                color.copy(alpha = color.alpha * (if (idx == 0) 0.85f else 0.6f)),
                style = Stroke(stroke * 0.85f)
            )
        }
    }
    drawCircle(pip, r * 0.11f, center)
}

/* ============================================================================
   12. OLIVE_DAWN — Kubbeli Zeytin (Al-Aqsa & Medina Courtyard Arches & Olive Branch)
   ========================================================================== */

private fun DrawScope.oliveDawn(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.45f
    val center = Offset(size.width * 0.5f, size.height * 0.24f)
    val maxR = min(size.width, size.height) * 0.46f

    // Concentric Medina courtyard dome rings
    var r = maxR * 0.28f
    var i = 0
    while (r < maxR) {
        drawCircle(
            color = p.accent2.copy(alpha = (alpha * (0.85f - i * 0.14f).coerceAtLeast(0.2f)).coerceAtMost(0.24f)),
            radius = r,
            center = center,
            style = Stroke((1.6f - i * 0.22f).coerceAtLeast(0.6f).dp.toPx())
        )
        r *= 1.22f
        i++
    }
    drawEightPointStar(center, maxR * 0.18f, p.accent2.copy(alpha = (alpha * 0.9f).coerceAtMost(0.24f)), 1.1.dp.toPx(), true)

    // Courtyard arcade colonnade arches along lower background
    val archCount = 3
    val archW = size.width / archCount
    val archTopY = size.height * 0.58f
    val archColor = p.accent.copy(alpha = (alpha * 0.75f).coerceAtMost(0.20f))
    for (col in 0 until archCount) {
        drawPointedMihrabWatermark(
            centerTop = Offset(archW * (col + 0.5f), archTopY),
            width = archW * 0.88f,
            height = size.height * 0.38f,
            color = archColor,
            strokeWidth = 1.2.dp.toPx()
        )
    }

    // Botanical olive branch leaves along the side margins
    val leafColor = p.accent.copy(alpha = (alpha * 0.85f).coerceAtMost(0.22f))
    var ly = 60.dp.toPx()
    while (ly < size.height - 60.dp.toPx()) {
        for (side in listOf(22.dp.toPx(), size.width - 22.dp.toPx())) {
            val dir = if (side < size.width / 2f) 1f else -1f
            val leaf = Path().apply {
                moveTo(side, ly)
                quadraticTo(side + dir * 14.dp.toPx(), ly - 10.dp.toPx(), side + dir * 20.dp.toPx(), ly - 2.dp.toPx())
                quadraticTo(side + dir * 10.dp.toPx(), ly + 6.dp.toPx(), side, ly)
                close()
            }
            drawPath(leaf, leafColor, style = Stroke(0.9.dp.toPx()))
        }
        ly += 38.dp.toPx()
    }
}

/* ============================================================================
   13. PAPER_ATELIER
   ========================================================================== */

private fun DrawScope.paperAtelier(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.4f
    val base = 16.dp.toPx()
    var y = base
    while (y < size.height) {
        drawLine(p.line.copy(alpha = alpha * 0.5f), Offset(0f, y), Offset(size.width, y), 0.5f.dp.toPx())
        y += base
    }
}

/* ============================================================================
   14. M3_SUNSET
   ========================================================================== */

private fun DrawScope.m3Sunset(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.3f
    val step = 28.dp.toPx()
    val baseline = size.height * 0.28f
    var band = 0
    var y = baseline
    while (y < size.height) {
        val t = (band / 8f).coerceIn(0f, 1f)
        drawRect(
            color = lerpColor(p.background, p.accent, t * 0.5f).copy(alpha = alpha),
            topLeft = Offset(0f, y),
            size = Size(size.width, step)
        )
        y += step
        band++
    }
}

/* ============================================================================
   15. COASTAL_PATH
   ========================================================================== */

private fun DrawScope.coastalPath(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.4f
    val horizon = size.height * 0.34f
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, p.accent.copy(alpha = alpha * 1.3f), Color.Transparent)
        ),
        start = Offset(0f, horizon),
        end = Offset(size.width, horizon),
        strokeWidth = 1.6f.dp.toPx()
    )
}

/* ============================================================================
   16. KRAFT_WARM
   ========================================================================== */

private fun DrawScope.kraftWarm(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.4f
    val inset = 8.dp.toPx()
    drawRect(
        color = p.accent.copy(alpha = alpha * 0.35f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2)
    )
}

/* ============================================================================
   17. PASTEL_KNOT — Kubbeli Sedef (Mother-of-Pearl Kündekâri Inlay & Bosphorus Shimmer)
   ========================================================================== */

private fun DrawScope.pastelKnot(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.4f
    val cell = 68.dp.toPx()
    val stroke = 0.95.dp.toPx()

    // Iridescent Mother-of-Pearl (Sedef) radial shimmer
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                p.accent2.copy(alpha = (alpha * 1.05f).coerceAtMost(0.20f)),
                p.accent.copy(alpha = (alpha * 0.65f).coerceAtMost(0.14f)),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.20f),
            radius = size.width * 0.85f
        ),
        radius = size.width * 0.85f,
        center = Offset(size.width * 0.5f, size.height * 0.20f)
    )

    // Octagonal Kündekâri geometric inlay lattice
    val pearlStroke = p.accent.copy(alpha = (alpha * 0.9f).coerceAtMost(0.22f))
    val goldInlay = p.accent2.copy(alpha = (alpha * 1.0f).coerceAtMost(0.24f))

    var row = 0
    var y = 0f
    while (y < size.height + cell) {
        var col = 0
        var x = if (row % 2 == 0) 0f else cell * 0.5f
        while (x < size.width + cell) {
            drawEightPointStar(
                center = Offset(x, y),
                radius = cell * 0.36f,
                color = if ((row + col) % 2 == 0) pearlStroke else goldInlay,
                strokeWidth = stroke,
                filledInner = true
            )
            // Outer octagonal pearl frame
            drawCircle(
                color = goldInlay.copy(alpha = goldInlay.alpha * 0.55f),
                radius = cell * 0.48f,
                center = Offset(x, y),
                style = Stroke(stroke * 0.7f)
            )
            col++
            x += cell
        }
        row++
        y += cell * 0.85f
    }
}

/* ============================================================================
   18. WORDMARK
   ========================================================================== */

private fun DrawScope.wordmark(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.4f
    val track = 48.dp.toPx()
    var x = track
    while (x < size.width) {
        drawLine(p.accent.copy(alpha = alpha * 0.35f), Offset(x, 0f), Offset(x, size.height), 1.4f.dp.toPx())
        x += track
    }
}

/* ============================================================================
   19. QUIET_GRID
   ========================================================================== */

private fun DrawScope.quietGrid(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 0.9f
    val step = 48.dp.toPx()
    var x = 0f
    while (x <= size.width) {
        drawLine(p.line.copy(alpha = alpha), Offset(x, 0f), Offset(x, size.height), 0.5f.dp.toPx())
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(p.line.copy(alpha = alpha), Offset(0f, y), Offset(size.width, y), 0.5f.dp.toPx())
        y += step
    }
}

/* ============================================================================
   20. HAIRLINE
   ========================================================================== */

private fun DrawScope.hairline(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity)
    val inset = 12.dp.toPx()
    drawLine(p.line.copy(alpha = alpha * 1.4f), Offset(inset, 0f), Offset(inset, size.height), 0.5f.dp.toPx())
    drawLine(p.line.copy(alpha = alpha * 0.8f), Offset(size.width - inset, 0f), Offset(size.width - inset, size.height), 0.5f.dp.toPx())
}

/* ============================================================================
   21. PLAIN_VOICE
   ========================================================================== */

private fun DrawScope.plainVoice(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.1f
    val leading = 24.dp.toPx()
    val inset = 16f.dp.toPx()
    var y = size.height * 0.22f
    var i = 0
    while (y < size.height) {
        drawLine(
            p.line.copy(alpha = if (i % 4 == 0) alpha * 0.8f else alpha * 0.4f),
            Offset(inset, y),
            Offset(size.width - inset, y),
            0.5f.dp.toPx()
        )
        y += leading
        i++
    }
}

/* ============================================================================
   HELPERS
   ========================================================================== */

private val TWO_PI = (Math.PI * 2).toFloat()
private val DEG_TO_RAD = (Math.PI / 180.0).toFloat()

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val c = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * c,
        green = a.green + (b.green - a.green) * c,
        blue = a.blue + (b.blue - a.blue) * c,
        alpha = a.alpha + (b.alpha - a.alpha) * c
    )
}

/** Exposed intensity clamp so callers can normalize the user slider. */
internal fun clampPatternIntensity(value: Float): Float = value.coerceIn(0.5f, 1.5f)
