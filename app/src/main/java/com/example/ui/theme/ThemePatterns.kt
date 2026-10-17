package com.example.ui.theme

import androidx.compose.ui.geometry.Offset
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
    fun baseAlpha(intensity: Float) = (0.045f * intensity.coerceIn(0.5f, 1.5f)).coerceAtMost(0.085f)
}

/** Single entry point: draws the theme's signature surface pattern. */
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
   12. OLIVE_DAWN — appllama-app-design-skill
   One accent, one grey family, one stated radius scale. A single concentric
   ring echoing the countdown arc; nothing else competes.
   ========================================================================== */

private fun DrawScope.oliveDawn(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity)
    val center = Offset(size.width * 0.5f, size.height * 0.28f)
    val maxR = min(size.width, size.height) * 0.42f

    var r = maxR * 0.34f
    var i = 0
    while (r < maxR) {
        drawCircle(
            color = p.accent.copy(alpha = alpha * (0.5f - i * 0.1f).coerceAtLeast(0.1f)),
            radius = r,
            center = center,
            style = Stroke((1.4f - i * 0.25f).coerceAtLeast(0.4f).dp.toPx())
        )
        r *= 1.22f
        i++
    }
    drawCircle(p.accent.copy(alpha = alpha * 0.35f), maxR * 0.12f, center)

    drawLine(
        p.line.copy(alpha = alpha * 0.5f),
        Offset(size.width * 0.06f, 0f), Offset(size.width * 0.06f, size.height), 0.6f.dp.toPx()
    )
}

/* ============================================================================
   13. PAPER_ATELIER — tokens-and-color.md (4px base grid)
   Strict 4px ruling, a hairline baseline, and margin register marks.
   ========================================================================== */

private fun DrawScope.paperAtelier(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.5f
    val base = 4.dp.toPx() * 4

    var y = base
    while (y < size.height) {
        drawLine(p.line.copy(alpha = alpha * 0.5f), Offset(0f, y), Offset(size.width, y), 0.4f.dp.toPx())
        y += base
    }
    val baseline = size.height * 0.5f
    drawLine(p.line.copy(alpha = alpha * 0.9f), Offset(0f, baseline), Offset(size.width, baseline), 0.4f.dp.toPx())

    var x = base * 4
    while (x < size.width) {
        drawLine(p.line.copy(alpha = alpha * 0.35f), Offset(x, 0f), Offset(x, 8f.dp.toPx()), 0.4f.dp.toPx())
        x += base * 4
    }
}

/* ============================================================================
   14. M3_SUNSET — material-design-3 color-system.md
   Roles only: a surface-tone ramp with no ink, plus one baseline. Hierarchy
   stays tonal, exactly as the M3 surface family prescribes.
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
            color = lerpColor(p.background, p.accent, t * 0.5f).copy(alpha = alpha * 1.4f),
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(size.width, step)
        )
        y += step
        band++
    }

    drawLine(p.accent.copy(alpha = alpha * 1.1f), Offset(0f, baseline), Offset(size.width, baseline), 1.4f.dp.toPx())
}

/* ============================================================================
   15. COASTAL_PATH — mobile-design (Fitts / thumb zone)
   A horizon rule with interval markers: a depth cue that doubles as a
   progress track, sized for one-handed reach.
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

    val gap = 24.dp.toPx()
    var x = gap
    while (x < size.width) {
        drawLine(p.line.copy(alpha = alpha * 0.6f), Offset(x, horizon - 5f.dp.toPx()), Offset(x, horizon + 5f.dp.toPx()), 0.8f.dp.toPx())
        x += gap
    }

    val deep = horizon + 44.dp.toPx()
    drawLine(p.line.copy(alpha = alpha * 0.4f), Offset(0f, deep), Offset(size.width, deep), 0.6f.dp.toPx())
}

/* ============================================================================
   16. KRAFT_WARM — ui-design-systems (3-tier tokens)
   The tiers made visible: a semantic tint field, a component hairline frame,
   and an accent rule on the 8px grid.
   ========================================================================== */

private fun DrawScope.kraftWarm(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.4f
    val inset = 8.dp.toPx()

    drawRect(
        color = p.accent.copy(alpha = alpha * 0.35f),
        topLeft = Offset(inset, inset),
        size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
    )

    drawRect(
        color = p.line.copy(alpha = alpha * 0.7f),
        topLeft = Offset(inset * 1.5f, inset * 1.5f),
        size = androidx.compose.ui.geometry.Size(
            (size.width - inset * 3).coerceAtLeast(0f),
            (size.height - inset * 3).coerceAtLeast(0f)
        ),
        style = Stroke(0.6f.dp.toPx())
    )

    val y = inset * 4
    drawLine(p.accent.copy(alpha = alpha * 0.9f), Offset(inset * 2, y), Offset(size.width - inset * 2, y), 0.9f.dp.toPx())
}

/* ============================================================================
   17. PASTEL_KNOT — design-tokens (OKLCH shade ladder)
   A ladder of tonal steps, the visual equivalent of an 11-shade OKLCH ramp.
   ========================================================================== */

private fun DrawScope.pastelKnot(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.5f
    val left = 20.dp.toPx()
    val step = (size.height * 0.055f).coerceAtLeast(8f.dp.toPx())
    val count = 11

    for (i in 0 until count) {
        val t = (1f - i / (count - 1f)).coerceIn(0f, 1f)
        val y = size.height * 0.30f + i * step
        drawLine(
            lerpColor(p.accent, p.background, 1f - t).copy(alpha = alpha * 0.9f),
            Offset(left, y),
            Offset(left + (size.width - left * 2) * t.coerceAtLeast(0.04f), y),
            (2.6f - i * 0.16f).coerceAtLeast(0.6f).dp.toPx()
        )
    }
}

/* ============================================================================
   18. WORDMARK — a11y-audit (WCAG 2.2)
   Wide letterpress tracking rules. No thin hairlines: every stroke is heavy
   enough to survive low vision and increased-contrast modes.
   ========================================================================== */

private fun DrawScope.wordmark(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.8f
    val track = 48.dp.toPx()
    val weight = 1.6f.dp.toPx()

    var x = track
    while (x < size.width) {
        drawLine(p.accent.copy(alpha = alpha * 0.30f), Offset(x, 0f), Offset(x, size.height), weight)
        x += track
    }
    val rule = size.height * 0.5f
    drawLine(p.accent.copy(alpha = alpha * 0.7f), Offset(0f, rule), Offset(size.width, rule), weight * 1.6f)
}

/* ============================================================================
   19. QUIET_GRID — android-kotlin-compose (REVIEW MODE)
   A single functional ruling. No diagonal flourish: nothing here that a
   Compose audit could classify as ornamental.
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
   20. HAIRLINE — hig-foundations ("content over chrome")
   The quietest motif in the set: two inset hairlines so grouping reads
   without wrapping every card in a border.
   ========================================================================== */

private fun DrawScope.hairline(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity)
    val inset = 12.dp.toPx()

    drawLine(p.line.copy(alpha = alpha * 1.6f), Offset(inset, 0f), Offset(inset, size.height), 0.5f.dp.toPx())
    drawLine(p.line.copy(alpha = alpha * 0.8f), Offset(size.width - inset, 0f), Offset(size.width - inset, size.height), 0.5f.dp.toPx())
}

/* ============================================================================
   21. PLAIN_VOICE — ux-writing (sentence case, no ALL CAPS)
   Typographic baselines at the leading interval: the motif is punctuation,
   not ornament.
   ========================================================================== */

private fun DrawScope.plainVoice(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.2f
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
   1. TECH GRID — Kubbeli Zümrüt (unchanged from the original engine)
   ========================================================================== */

private fun DrawScope.techGrid(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity)
    val grid = 56.dp.toPx()
    val fine = 14.dp.toPx()

    var x = 0f
    while (x <= size.width) {
        drawLine(p.line.copy(alpha = alpha), Offset(x, 0f), Offset(x, size.height), 0.6.dp.toPx())
        x += fine
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(p.line.copy(alpha = alpha), Offset(0f, y), Offset(size.width, y), 0.6.dp.toPx())
        y += fine
    }
    x = 0f
    while (x <= size.width + size.height) {
        drawLine(p.accent.copy(alpha = alpha * 0.7f), Offset(x, 0f), Offset(x - size.height, size.height), 0.8.dp.toPx())
        x += grid
    }
    drawLine(p.accent.copy(alpha = 0.35f), Offset(0f, 0f), Offset(size.width * 0.24f, 0f), 1.dp.toPx())
}

/* ============================================================================
   2. TUGHRA BAND — Osmanlı Hümayun
   ========================================================================== */

private fun DrawScope.tughraBand(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.35f
    val band = 118.dp.toPx()
    val stroke = 0.9.dp.toPx()

    // Vertical ruling, the way Ottoman calligraphy was ruled on paper.
    var x = 12.dp.toPx()
    while (x < size.width) {
        drawLine(p.line.copy(alpha = alpha * 0.8f), Offset(x, 0f), Offset(x, size.height), stroke)
        x += 9.dp.toPx()
    }

    var bandIndex = 0
    var y = band * 0.6f
    while (y < size.height) {
        val isGoldBand = bandIndex % 2 == 0
        val color = (if (isGoldBand) p.accent else p.accent2).copy(alpha = alpha * 1.2f)
        val seg = 84.dp.toPx()
        var sx = -seg * 0.5f
        while (sx < size.width + seg) {
            tughraLeaf(Offset(sx, y), seg, color)
            sx += seg
        }
        y += band
        bandIndex++
    }

    // Ceremonial double rule beneath each band.
    var ry = band * 0.6f + 16.dp.toPx()
    while (ry < size.height) {
        drawLine(p.accent.copy(alpha = alpha * 0.9f), Offset(0f, ry), Offset(size.width, ry), 1.2.dp.toPx())
        drawLine(
            p.accent.copy(alpha = alpha * 0.45f),
            Offset(0f, ry + 2.5.dp.toPx()), Offset(size.width, ry + 2.5.dp.toPx()), 0.6.dp.toPx()
        )
        ry += band
    }
}

/** Mirrored leaf flourish used as the band's repeating unit. */
private fun DrawScope.tughraLeaf(origin: Offset, seg: Float, color: Color) {
    val h = seg * 0.34f
    val outer = Path().apply {
        moveTo(origin.x, origin.y)
        cubicTo(
            origin.x + seg * 0.28f, origin.y - h,
            origin.x + seg * 0.72f, origin.y + h,
            origin.x + seg, origin.y
        )
    }
    drawPath(outer, color, style = Stroke(width = 0.8.dp.toPx()))

    val inner = Path().apply {
        moveTo(origin.x + seg * 0.18f, origin.y)
        cubicTo(
            origin.x + seg * 0.38f, origin.y - h * 0.55f,
            origin.x + seg * 0.62f, origin.y + h * 0.55f,
            origin.x + seg * 0.82f, origin.y
        )
    }
    drawPath(inner, color.copy(alpha = color.alpha * 0.7f), style = Stroke(width = 0.6.dp.toPx()))
}

/* ============================================================================
   3. AZULEJO — Andalusya
   ========================================================================== */

private fun DrawScope.azulejo(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.6f
    val cell = 44.dp.toPx()
    val stroke = 0.7.dp.toPx()

    var row = 0
    var y = -cell
    while (y < size.height + cell) {
        var col = 0
        var x = -cell + (if (row % 2 == 0) 0f else cell / 2f)
        while (x < size.width + cell) {
            azulejoStar(
                center = Offset(x, y),
                r = cell * 0.46f,
                color = p.accent.copy(alpha = alpha),
                diamondColor = p.line.copy(alpha = alpha * 0.9f),
                stroke = stroke,
                rotate45 = (row + col) % 2 != 0
            )
            col++
            x += cell
        }
        row++
        y += cell * 0.86f
    }

    var gx = 0f
    while (gx < size.width) {
        drawLine(p.line.copy(alpha = alpha * 0.4f), Offset(gx, 0f), Offset(gx, size.height), 0.4.dp.toPx())
        gx += cell
    }
}

/** Eight-pointed Andalusian star: a square plus an overlapping diamond. */
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
    }
}

/* ============================================================================
   4. EBRE — Perşe Bahçe
   ========================================================================== */

private fun DrawScope.ebre(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.7f
    val rnd = SeededRandom(0x51A7E)
    val bands = 7

    // Ebru: floated pigment combed into slow horizontal currents.
    for (band in 0 until bands) {
        val baseY = size.height * (band + 0.5f) / bands
        val amp = rnd.nextIn(6f, 26f).dp.toPx()
        val wavelength = rnd.nextIn(90f, 240f).dp.toPx()
        val thickness = rnd.nextIn(1.2f, 4.5f).dp.toPx()
        val color = (if (band % 3 == 0) p.accent else if (band % 3 == 1) p.accent2 else p.line)
            .copy(alpha = alpha * rnd.nextIn(0.7f, 1.25f))

        val path = Path()
        var x = -20f
        var first = true
        while (x <= size.width + 20f) {
            val phase = (x / wavelength) * TWO_PI
            val y = baseY + sin(phase + band * 1.3f) * amp + cos(phase * 0.47f + band) * amp * 0.35f
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

    for (i in 0 until 26) {
        drawCircle(
            color = (if (i % 2 == 0) p.accent else p.accent2).copy(alpha = alpha * 0.9f),
            radius = rnd.nextIn(1.2f, 4.2f).dp.toPx(),
            center = Offset(rnd.nextIn(0f, size.width), rnd.nextIn(0f, size.height))
        )
    }
}

/* ============================================================================
   5. ZELLIGE — Magreb
   ========================================================================== */

private fun DrawScope.zellige(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.45f
    val cell = 62.dp.toPx()
    val stroke = 0.85.dp.toPx()
    val color = p.accent.copy(alpha = alpha)
    val pip = p.accent2.copy(alpha = alpha * 0.9f)

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

/** 8-fold interlaced rosette: strapwork ring plus radiating petals. */
private fun DrawScope.zelligeRosette(
    center: Offset,
    r: Float,
    color: Color,
    pip: Color,
    stroke: Float,
    variant: Int
) {
    val inner = r * 0.42f
    drawCircle(color.copy(alpha = color.alpha * 0.6f), r * 0.9f, center, style = Stroke(stroke))
    drawCircle(color, inner, center, style = Stroke(stroke))

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
            drawPath(petal, color.copy(alpha = color.alpha * 0.55f), style = Stroke(stroke * 0.9f))
        }
    }
    drawCircle(pip, inner * 0.42f, center)
}

/* ============================================================================
   6. CLEAN — Hicri Minimal
   Deliberately no pattern: minimal themes get pure surfaces.
   ========================================================================== */

/* ============================================================================
   7. WHIRL — Semazen
   ========================================================================== */

private fun DrawScope.whirl(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.5f
    val center = Offset(size.width / 2f, size.height * 0.42f)
    val maxR = min(size.width, size.height) * 1.15f
    val stroke = 0.9.dp.toPx()

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
        drawPath(path, p.accent2.copy(alpha = alpha * 0.5f), style = Stroke(stroke * 0.8f))
    }
}

/* ============================================================================
   8. KAABA_LINES — Kaabe Monokrom
   ========================================================================== */

private fun DrawScope.kaabaLines(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 2.0f
    val gold = p.accent.copy(alpha = alpha * 0.9f)
    val white = p.line.copy(alpha = alpha * 0.7f)

    val bandY = size.height * 0.18f
    drawLine(gold, Offset(0f, bandY), Offset(size.width, bandY), 1.1.dp.toPx(), cap = StrokeCap.Butt)
    drawLine(
        gold.copy(alpha = alpha * 0.4f),
        Offset(0f, bandY + 3.dp.toPx()), Offset(size.width, bandY + 3.dp.toPx()), 0.6.dp.toPx()
    )

    var x = 6.dp.toPx()
    while (x < size.width) {
        drawLine(white, Offset(x, bandY - 9.dp.toPx()), Offset(x, bandY - 2.dp.toPx()), 0.7.dp.toPx())
        x += 12.dp.toPx()
    }

    var y = bandY + 64.dp.toPx()
    while (y < size.height) {
        drawLine(white.copy(alpha = alpha * 0.4f), Offset(0f, y), Offset(size.width, y), 0.5.dp.toPx())
        y += 44.dp.toPx()
    }
}

/* ============================================================================
   9. SAND_DRIFT — Çöl Gülü
   ========================================================================== */

private fun DrawScope.sandDrift(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.35f
    val rnd = SeededRandom(0xD35E47)
    val strokes = 16

    for (i in 0 until strokes) {
        val y = size.height * (i + rnd.nextIn(0.1f, 0.9f)) / strokes
        val amplitude = rnd.nextIn(2f, 9f).dp.toPx()
        val wavelength = rnd.nextIn(70f, 200f).dp.toPx()
        val weight = rnd.nextIn(0.4f, 1.5f).dp.toPx()
        val color = (if (i % 4 == 0) p.accent2 else p.line).copy(alpha = alpha * rnd.nextIn(0.5f, 1f))

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
   10. STARFIELD — Celestial Gece
   ========================================================================== */

private fun DrawScope.starfield(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 2.4f
    val rnd = SeededRandom(0xCE1E57)

    for (i in 0 until 120) {
        val magnitude = rnd.nextFloat()
        val color = (if (i % 5 == 0) p.accent2 else p.line)
            .copy(alpha = alpha * (0.35f + magnitude * 0.9f))
        drawCircle(
            color = color,
            radius = (0.4f + magnitude * 1.9f).dp.toPx(),
            center = Offset(rnd.nextIn(0f, size.width), rnd.nextIn(0f, size.height))
        )
    }

    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, p.accent.copy(alpha = alpha * 0.5f), Color.Transparent),
            start = Offset(0f, size.height * 0.18f),
            end = Offset(size.width, size.height * 0.62f)
        )
    )

    val center = Offset(size.width * 0.84f, size.height * 0.12f)
    val radius = 26.dp.toPx()
    drawCircle(p.accent2.copy(alpha = alpha * 1.2f), radius, center)
    drawCircle(p.background.copy(alpha = 0.92f), radius * 0.92f, center + Offset(radius * 0.34f, -radius * 0.16f))
}

/* ============================================================================
   11. ARABESQUE — Bağdat Altın Çağı
   ========================================================================== */

private fun DrawScope.arabesque(p: PatternPalette, intensity: Float) {
    val alpha = ThemePatterns.baseAlpha(intensity) * 1.4f
    val cell = 58.dp.toPx()
    val stroke = 0.75.dp.toPx()
    val color = p.accent2.copy(alpha = alpha)
    val pip = p.accent.copy(alpha = alpha * 0.8f)

    var row = 0
    var y = -cell * 0.5f
    while (y < size.height + cell) {
        var col = 0
        var x = -cell * 0.5f + (if (row % 2 == 0) 0f else cell / 2f)
        while (x < size.width + cell) {
            arabesqueUnit(Offset(x, y), cell * 0.4f, color, pip, stroke)
            col++
            x += cell
        }
        row++
        y += cell * 0.68f
    }
}

/** Interlaced split-leaf unit, the core of arabesque scrollwork. */
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
    drawCircle(pip, r * 0.09f, center)
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
