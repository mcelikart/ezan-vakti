package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/* ============================================================================
   THEME ENUM — 11 themes, Emerald Dome first and default + 10 Kubbeli themes
   ========================================================================== */

enum class AppTheme(val specId: String) {
    CLASSIC_EMERALD("CLASSIC_EMERALD"),
    KUBBELI_SAFIR("KUBBELI_SAFIR"),
    KUBBELI_YAKUT("KUBBELI_YAKUT"),
    KUBBELI_FIRUZE("KUBBELI_FIRUZE"),
    KUBBELI_KEHRIBAR("KUBBELI_KEHRIBAR"),
    KUBBELI_AMETIST("KUBBELI_AMETIST"),
    KUBBELI_ZEYTIN("KUBBELI_ZEYTIN"),
    KUBBELI_SAHRA("KUBBELI_SAHRA"),
    KUBBELI_SEDEF("KUBBELI_SEDEF"),
    KUBBELI_KABE_GECESI("KUBBELI_KABE_GECESI"),
    KUBBELI_GECE_MAVISI("KUBBELI_GECE_MAVISI");

    val spec: AppThemeSpec get() = ThemeRegistry.getThemeById(specId)
    val displayName: String get() = spec.englishName
    val turkishName: String get() = spec.turkishName
    val description: String get() = spec.description
    val category: String get() = spec.category
    val designMovement: String get() = spec.designMovement
    val featureTag: String get() = spec.featureTag

    companion object {
        val DEFAULT = CLASSIC_EMERALD

        // Removed themes intentionally migrate to the preserved default.
        fun fromStored(value: String?): AppTheme =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

/* ============================================================================
   COMPOSITION LOCALS
   ========================================================================== */

val LocalAppTheme = staticCompositionLocalOf { AppTheme.DEFAULT }
val LocalSelectedBackground = staticCompositionLocalOf { 0 }
val LocalPatternIntensity = staticCompositionLocalOf { 1f }
val LocalThemeColorVariant = staticCompositionLocalOf { 0 }
val LocalThemeColors = staticCompositionLocalOf { ThemeRegistry.default.colors(true) }
val LocalThemeIsDark = staticCompositionLocalOf { true }
val LocalBlurredBackgroundBitmap = staticCompositionLocalOf<ImageBitmap?> { null }

/* ============================================================================
   CONVENIENCE ACCESSORS
   ========================================================================== */

val AppTheme.buttonShape: Shape get() = spec.shapes.buttonShape
val AppTheme.cardShape: Shape get() = spec.shapes.cardShape
val AppTheme.badgeShape: Shape get() = spec.shapes.badgeShape
val AppTheme.buttonBorder: BorderStroke get() = spec.buttonBorder
val AppTheme.buttonGradient: List<Color> @Composable get() = LocalThemeColors.current.cardGradient
val AppTheme.buttonTextColor: Color @Composable get() = LocalThemeColors.current.buttonTextColor
val AppTheme.primarySwatch: Color get() = spec.colors(spec.prefersDark).primary
val AppTheme.secondarySwatch: Color get() = spec.colors(spec.prefersDark).secondary
val AppTheme.tertiarySwatch: Color get() = spec.colors(spec.prefersDark).tertiary
val AppTheme.metrics: ThemeMetrics get() = spec.metrics
val AppTheme.typography: ThemeTypographySpec get() = spec.typography
val AppTheme.motion: ThemeMotionSpec get() = spec.motion
val AppTheme.pattern: ThemePattern get() = spec.pattern
val AppTheme.isDarkPalette: Boolean get() = spec.prefersDark

@Composable fun getActiveCardShape(): Shape = LocalThemeSpec.current.shapes.cardShape
@Composable fun getActiveButtonShape(): Shape = LocalThemeSpec.current.shapes.buttonShape
@Composable fun getActiveBadgeShape(): Shape = LocalThemeSpec.current.shapes.badgeShape
@Composable fun getActiveButtonBorder(): BorderStroke = LocalThemeSpec.current.buttonBorder
@Composable fun getActiveButtonGradient(): List<Color> = LocalThemeColors.current.cardGradient
@Composable fun getActiveCardPadding(): androidx.compose.ui.unit.Dp = LocalThemeMetrics.current.cardPadding
@Composable fun getActiveScreenGutter(): androidx.compose.ui.unit.Dp = LocalThemeMetrics.current.screenGutter

/* ============================================================================
   GLASSMORPHISM & REAL-TIME BACKDROP BLUR HELPERS
   ========================================================================== */

/**
 * Draws the window-aligned pre-blurred background bitmap (`LocalBlurredBackgroundBitmap`)
 * directly underneath any clipped glass card, button, prayer box, or bottom bar so that
 * the geometric background motifs behind the glass are genuinely out-of-focus and blurred.
 */
@Composable
fun Modifier.frostedGlassBackdrop(
    shape: Shape,
    tintColor: Color = Color.Unspecified,
    tintAlpha: Float = 0.18f,
    surfaceVeilAlpha: Float = -1f,
    blurAlpha: Float = 1f
): Modifier {
    val blurredBitmap = LocalBlurredBackgroundBitmap.current
    val colors = LocalThemeColors.current
    val isDark = LocalThemeIsDark.current
    val view = LocalView.current
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    val effectiveVeilAlpha = if (surfaceVeilAlpha >= 0f) {
        surfaceVeilAlpha
    } else {
        if (isDark) 0.34f else 0.40f
    }
    val glassHighlight = Color.White.copy(alpha = (if (isDark) 0.18f else 0.40f) * blurAlpha.coerceIn(0.5f, 1f))
    val glassTint = if (tintColor != Color.Unspecified) {
        tintColor.copy(alpha = tintAlpha)
    } else {
        colors.primary.copy(alpha = if (isDark) 0.15f else 0.12f)
    }

    return this
        .onGloballyPositioned { coords ->
            if (coords.isAttached) {
                rootOffset = coords.positionInRoot()
            }
        }
        .clip(shape)
        .drawWithContent {
            // 1. Window-aligned Gaussian-blurred background directly underneath this element
            if (blurredBitmap != null) {
                val winW = view.width.coerceAtLeast(size.width.toInt()).coerceAtLeast(1)
                val winH = view.height.coerceAtLeast(size.height.toInt()).coerceAtLeast(1)
                drawImage(
                    image = blurredBitmap,
                    dstOffset = IntOffset(-rootOffset.x.toInt(), -rootOffset.y.toInt()),
                    dstSize = IntSize(winW, winH),
                    alpha = blurAlpha.coerceIn(0f, 1f)
                )
            } else {
                drawRect(colors.surface.copy(alpha = 0.76f * blurAlpha.coerceIn(0f, 1f)))
            }
            // 2. Balanced frosted-glass diffusion veil & diagonal specular sheen
            drawRect(colors.surface.copy(alpha = effectiveVeilAlpha))
            if (tintColor != Color.Unspecified) {
                drawRect(tintColor.copy(alpha = tintAlpha))
            }
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        glassHighlight,
                        glassTint,
                        Color.Transparent,
                        colors.glowColor.copy(alpha = (if (isDark) 0.12f else 0.10f) * blurAlpha.coerceIn(0.5f, 1f))
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
            )
            drawContent()
        }
}

/**
 * Applies frosted glassmorphism, real-time blurred backdrop, and bespoke architectural
 * inner frame ornaments ONLY to non-Kubbeli-Zümrüt themes (`theme != AppTheme.CLASSIC_EMERALD && theme.spec.isGlass`).
 * Kubbeli Zümrüt (`CLASSIC_EMERALD`) is never modified and keeps its classic border.
 */
@Composable
fun Modifier.glassmorphicCard(
    theme: AppTheme = LocalAppTheme.current,
    isDark: Boolean = LocalThemeIsDark.current,
    shape: Shape = theme.cardShape,
    border: BorderStroke = theme.buttonBorder
): Modifier {
    if (theme == AppTheme.CLASSIC_EMERALD || !theme.spec.isGlass) {
        return this
            .paperLiftShadow(theme = theme, isDarkTheme = isDark)
            .border(border, shape)
    }
    val colors = LocalThemeColors.current
    val motifColor = colors.goldMotifColor.copy(alpha = if (isDark) 0.42f else 0.34f)

    return this
        .frostedGlassBackdrop(shape = shape)
        .drawWithContent {
            drawContent()
            // Specular top crystal rim & theme-distinctive corner frame ornaments
            val w = size.width
            val h = size.height
            val inset = 7.dp.toPx()
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isDark) 0.48f else 0.68f),
                        motifColor,
                        Color.White.copy(alpha = if (isDark) 0.48f else 0.68f),
                        Color.Transparent
                    )
                ),
                start = Offset(inset * 2f, 2.dp.toPx()),
                end = Offset((w - inset * 2f).coerceAtLeast(inset * 2f), 2.dp.toPx()),
                strokeWidth = 1.4.dp.toPx()
            )
            // Distinct inner corner brackets per glass theme
            val bracketLen = 14.dp.toPx()
            when (theme) {
                AppTheme.KUBBELI_FIRUZE, AppTheme.KUBBELI_KEHRIBAR, AppTheme.KUBBELI_SEDEF -> {
                    drawCircle(motifColor, 2.5.dp.toPx(), Offset(inset * 1.6f, inset * 1.6f))
                    drawCircle(motifColor, 2.5.dp.toPx(), Offset(w - inset * 1.6f, inset * 1.6f))
                    drawCircle(motifColor, 2.5.dp.toPx(), Offset(inset * 1.6f, h - inset * 1.6f))
                    drawCircle(motifColor, 2.5.dp.toPx(), Offset(w - inset * 1.6f, h - inset * 1.6f))
                }
                AppTheme.KUBBELI_YAKUT, AppTheme.KUBBELI_AMETIST, AppTheme.KUBBELI_SAHRA -> {
                    val p1 = Path().apply {
                        moveTo(w - inset - bracketLen, inset)
                        lineTo(w - inset, inset)
                        lineTo(w - inset, inset + bracketLen)
                    }
                    val p2 = Path().apply {
                        moveTo(inset, h - inset - bracketLen)
                        lineTo(inset, h - inset)
                        lineTo(inset + bracketLen, h - inset)
                    }
                    drawPath(p1, motifColor, style = Stroke(width = 1.2.dp.toPx()))
                    drawPath(p2, motifColor, style = Stroke(width = 1.2.dp.toPx()))
                }
                else -> {
                    drawLine(
                        color = motifColor.copy(alpha = 0.25f),
                        start = Offset(inset * 2.5f, h - 4.dp.toPx()),
                        end = Offset((w - inset * 2.5f).coerceAtLeast(inset * 2.5f), h - 4.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
        .border(border, shape)
}

@Composable
fun glassSurfaceColor(
    theme: AppTheme = LocalAppTheme.current,
    isDark: Boolean = LocalThemeIsDark.current
): Color {
    val base = MaterialTheme.colorScheme.surface
    if (theme == AppTheme.CLASSIC_EMERALD || !theme.spec.isGlass) return base
    // Return transparent so Card/Surface does not paint an opaque solid layer over frostedGlassBackdrop!
    return Color.Transparent
}

/* ============================================================================
   THEME-AWARE COMPONENTS
   ========================================================================== */

@Composable
fun ThemedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val spec = LocalThemeSpec.current
    val colors = LocalThemeColors.current
    val isDark = LocalThemeIsDark.current
    val shape = spec.shapes.buttonShape
    val buttonBrush = if (theme != AppTheme.CLASSIC_EMERALD && spec.isGlass) {
        Brush.linearGradient(
            colors = listOf(
                colors.cardGradient.first().copy(alpha = if (isDark) 0.78f else 0.88f),
                colors.cardGradient.last().copy(alpha = if (isDark) 0.64f else 0.76f)
            )
        )
    } else {
        Brush.horizontalGradient(colors.cardGradient)
    }
    val base = modifier
        .clip(shape)
        .background(buttonBrush)
        .then(if (spec.buttonBorder.width > 0.dp) Modifier.border(spec.buttonBorder, shape) else Modifier)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp)

    CompositionLocalProvider(LocalContentColor provides colors.buttonTextColor) {
        Row(
            modifier = base,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/** Card container honoring the theme's structural treatment. */
@Composable
fun ThemedSurface(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val spec = LocalThemeSpec.current
    val colors = LocalThemeColors.current
    val isDark = LocalThemeIsDark.current
    val shape = spec.shapes.cardShape
    val treatment = spec.cardTreatment

    val container = when (treatment) {
        CardTreatment.GLASS -> colors.surface.copy(alpha = spec.surfaceAlpha(isDark))
        CardTreatment.FLAT -> colors.surfaceVariant
        else -> colors.surface
    }

    val borderStroke: BorderStroke? = when (treatment) {
        CardTreatment.BRUTAL_TOPLINE -> BorderStroke(2.dp, colors.goldMotifColor)
        CardTreatment.ELEVATED, CardTreatment.GLOW, CardTreatment.FLAT -> null
        CardTreatment.GLASS -> spec.buttonBorder
        else -> spec.buttonBorder
    }

    androidx.compose.material3.Surface(
        modifier = if (theme != AppTheme.CLASSIC_EMERALD && spec.isGlass) {
            modifier.glassmorphicCard(theme = theme, isDark = isDark, shape = shape, border = spec.buttonBorder)
        } else {
            modifier
        },
        shape = shape,
        color = container,
        border = if (theme != AppTheme.CLASSIC_EMERALD && spec.isGlass) null else borderStroke,
        tonalElevation = if (treatment == CardTreatment.ELEVATED) 3.dp else 0.dp,
        shadowElevation = if (spec.hasHardShadow && treatment == CardTreatment.ELEVATED) 6.dp else 0.dp
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(LocalThemeMetrics.current.cardPadding),
            content = content
        )
    }
}

/* ============================================================================
   BACKGROUND ENGINE
   ========================================================================== */

data class AppBackgroundItem(
    val id: Int,
    val name: String,
    val category: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val overridePattern: ThemePattern? = null
)

val APP_BACKGROUND_ITEMS = listOf(
    AppBackgroundItem(0, "Temaya Özel Atmosfer", "Otomatik", "Seçili Kubbeli temanın kendi mimari dokusu, kemer silüeti ve atmosferik arka planı", Color(0xFF0F4C3A), Color(0xFFD4AF37), null),
    AppBackgroundItem(1, "Sultanahmet Çinisi", "Klasik", "İznik çini yıldızları ve çifte altın kandil silüeti", Color(0xFF0F3D6E), Color(0xFFD4AF37), ThemePattern.AZULEJO),
    AppBackgroundItem(2, "Hünkâr Mahfili Tuğrası", "Klasik", "Bordo-yakut hat cetvelleri, tuğra yaprak kuşakları ve kemer", Color(0xFF6E1428), Color(0xFFE0B43A), ThemePattern.TUGHRA_BAND),
    AppBackgroundItem(3, "Semerkand Firuze Mozaiği", "Klasik", "Sekiz yapraklı sırlı Registan rozetleri ve firuze kubbe halesi", Color(0xFF0A5C66), Color(0xFFD4AF37), ThemePattern.ZELLIGE),
    AppBackgroundItem(4, "Selçuklu Yıldızı & Rumi", "Klasik", "Anadolu Selçuklu sekiz köşeli yıldız ve rumi yaprak geçmeleri", Color(0xFF6B3E0B), Color(0xFFD99B26), ThemePattern.ARABESQUE),
    AppBackgroundItem(5, "Battal Ebru & Tezhip", "Sanat", "Klasik ebru dalgaları ve serpme altın tezhip damlaları", Color(0xFF4A205A), Color(0xFFD4AF37), ThemePattern.EBRE),
    AppBackgroundItem(6, "Medine & Kudüs Revakları", "Mimari", "Avlu kemer dizisi, kubbe halkaları ve zeytin dalı motifleri", Color(0xFF334F24), Color(0xFFC9A227), ThemePattern.OLIVE_DAWN),
    AppBackgroundItem(7, "Hicaz Sahra Kumulları", "Doğa", "Yumuşak çöl tepeleri, rüzgâr izleri ve kervan yıldızı", Color(0xFF5C4028), Color(0xFFD4AF37), ThemePattern.SAND_DRIFT),
    AppBackgroundItem(8, "Topkapı Sedef Kündekâri", "Sanat", "Sedef kakmalı sekizgen kündekâri kafes ve altın yıldızlar", Color(0xFF1F4E5B), Color(0xFFD4AF37), ThemePattern.PASTEL_KNOT),
    AppBackgroundItem(9, "Kisve-i Şerif & Altın Hizam", "Gece", "Siyah ipek zikzak dokuma, altın Hizam kuşağı ve üçlü kandil", Color(0xFF141316), Color(0xFFE8C86B), ThemePattern.KAABA_LINES),
    AppBackgroundItem(10, "Uluğ Bey Rasathanesi", "Gece", "Usturlap yörünge halkaları, hilal ve Kadir gecesi yıldız haritası", Color(0xFF0D162E), Color(0xFFE5C66E), ThemePattern.STARFIELD)
)

internal fun DrawScope.renderCustomBackgroundCanvas(
    bgId: Int,
    theme: AppTheme,
    isDark: Boolean,
    intensity: Float,
    colorVariant: Int,
    boostForBlur: Boolean = false
) {
    val spec = theme.spec
    val dark = isDark || spec.prefersDark
    val palette = spec.colors(dark, colorVariant)

    val base = palette.background
    val surfaceTint = palette.surfaceVariant
    val line = palette.outline
    val accent = palette.primary
    val accent2 = palette.goldMotifColor
    val effectivePattern = APP_BACKGROUND_ITEMS.firstOrNull { it.id == bgId }?.overridePattern ?: spec.pattern
    val clampedIntensity = clampPatternIntensity(intensity)

    drawRect(base)
    if (effectivePattern == ThemePattern.CLEAN) return

    // Multi-layered atmospheric vertical wash so every theme has a distinct sky/sanctuary depth
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                accent.copy(alpha = (if (dark) 0.18f else 0.13f) * clampedIntensity),
                surfaceTint.copy(alpha = (if (dark) 0.25f else 0.38f) * clampedIntensity),
                base,
                accent2.copy(alpha = (if (dark) 0.12f else 0.09f) * clampedIntensity)
            )
        )
    )

    // Ambient light orbs behind glass surfaces
    if (boostForBlur || (theme != AppTheme.CLASSIC_EMERALD && spec.isGlass)) {
        val orbMult = if (boostForBlur) 1.35f else 1f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = ((if (dark) 0.24f else 0.18f) * clampedIntensity * orbMult).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.82f, size.height * 0.16f),
                radius = size.width * 0.65f
            ),
            radius = size.width * 0.65f,
            center = Offset(size.width * 0.82f, size.height * 0.16f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent2.copy(alpha = ((if (dark) 0.20f else 0.16f) * clampedIntensity * orbMult).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.18f, size.height * 0.54f),
                radius = size.width * 0.58f
            ),
            radius = size.width * 0.58f,
            center = Offset(size.width * 0.18f, size.height * 0.54f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = ((if (dark) 0.20f else 0.15f) * clampedIntensity * orbMult).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.75f, size.height * 0.84f),
                radius = size.width * 0.55f
            ),
            radius = size.width * 0.55f,
            center = Offset(size.width * 0.75f, size.height * 0.84f)
        )
    }

    val patternBoost = if (boostForBlur) (clampedIntensity * 1.45f).coerceAtMost(1.5f) else clampedIntensity
    drawThemePattern(
        pattern = effectivePattern,
        p = PatternPalette(
            background = base,
            line = if (boostForBlur) line.copy(alpha = (line.alpha * 1.35f).coerceIn(0f, 1f)) else line,
            accent = accent,
            accent2 = accent2
        ),
        intensity = patternBoost
    )
}

fun Modifier.drawCustomBackground(
    bgId: Int = 0,
    theme: AppTheme = AppTheme.DEFAULT,
    isDark: Boolean = true,
    intensity: Float = 1f,
    colorVariant: Int = 0
): Modifier = drawWithCache {
    onDrawBehind {
        renderCustomBackgroundCanvas(
            bgId = bgId,
            theme = theme,
            isDark = isDark,
            intensity = intensity,
            colorVariant = colorVariant,
            boostForBlur = false
        )
    }
}

internal fun createBlurredBackgroundBitmap(
    widthPx: Int,
    heightPx: Int,
    baseDensity: Float,
    bgId: Int,
    theme: AppTheme,
    isDark: Boolean,
    intensity: Float,
    colorVariant: Int
): ImageBitmap {
    // Render at 1/3 resolution with proportionally scaled density so every motif,
    // arch, and pattern aligns 1:1 with the full-window background.
    val scale = 3
    val w = (widthPx / scale).coerceIn(180, 480)
    val h = (heightPx / scale).coerceIn(320, 960)
    val scaledDensity = (baseDensity / scale.toFloat()).coerceAtLeast(0.5f)

    val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val composeCanvas = Canvas(bitmap.asImageBitmap())
    val pureDensity = Density(density = scaledDensity, fontScale = 1f)
    val drawScope = CanvasDrawScope()
    drawScope.draw(
        density = pureDensity,
        layoutDirection = LayoutDirection.Ltr,
        canvas = composeCanvas,
        size = Size(w.toFloat(), h.toFloat())
    ) {
        renderCustomBackgroundCanvas(
            bgId = bgId,
            theme = theme,
            isDark = isDark,
            intensity = intensity,
            colorVariant = colorVariant,
            boostForBlur = true
        )
    }

    // Apply 3-pass separable Gaussian-approximating box blur in-place (radius 8 at 1/3 scale ≈ 24px full-res)
    fastGaussianBoxBlur(bitmap, radius = 8)
    return bitmap.asImageBitmap()
}

private fun fastGaussianBoxBlur(bitmap: android.graphics.Bitmap, radius: Int) {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 2 || h <= 2 || radius <= 0) return
    val r = radius.coerceIn(1, minOf(w - 1, h - 1) / 2)
    val div = 2 * r + 1
    val pixels = IntArray(w * h)
    val temp = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    repeat(3) {
        // Horizontal blur pass: pixels -> temp
        for (y in 0 until h) {
            val rowOffset = y * w
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (i in -r..r) {
                val c = pixels[rowOffset + i.coerceIn(0, w - 1)]
                sumR += (c shr 16) and 0xFF
                sumG += (c shr 8) and 0xFF
                sumB += c and 0xFF
            }
            for (x in 0 until w) {
                val outR = sumR / div
                val outG = sumG / div
                val outB = sumB / div
                temp[rowOffset + x] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB

                val removeIdx = (x - r).coerceIn(0, w - 1)
                val addIdx = (x + r + 1).coerceIn(0, w - 1)
                val cRem = pixels[rowOffset + removeIdx]
                val cAdd = pixels[rowOffset + addIdx]
                sumR += ((cAdd shr 16) and 0xFF) - ((cRem shr 16) and 0xFF)
                sumG += ((cAdd shr 8) and 0xFF) - ((cRem shr 8) and 0xFF)
                sumB += (cAdd and 0xFF) - (cRem and 0xFF)
            }
        }

        // Vertical blur pass: temp -> pixels
        for (x in 0 until w) {
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (i in -r..r) {
                val c = temp[i.coerceIn(0, h - 1) * w + x]
                sumR += (c shr 16) and 0xFF
                sumG += (c shr 8) and 0xFF
                sumB += c and 0xFF
            }
            for (y in 0 until h) {
                val outR = sumR / div
                val outG = sumG / div
                val outB = sumB / div
                pixels[y * w + x] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB

                val removeIdx = (y - r).coerceIn(0, h - 1)
                val addIdx = (y + r + 1).coerceIn(0, h - 1)
                val cRem = temp[removeIdx * w + x]
                val cAdd = temp[addIdx * w + x]
                sumR += ((cAdd shr 16) and 0xFF) - ((cRem shr 16) and 0xFF)
                sumG += ((cAdd shr 8) and 0xFF) - ((cRem shr 8) and 0xFF)
                sumB += (cAdd and 0xFF) - (cRem and 0xFF)
            }
        }
    }

    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
}

fun Modifier.drawPaperTexture(theme: AppTheme, isDark: Boolean): Modifier =
    drawCustomBackground(theme = theme, isDark = isDark)

/* ============================================================================
   TYPOGRAPHY BRIDGE
   ========================================================================== */

private fun familyOf(family: TypeFamily): FontFamily = when (family) {
    TypeFamily.SERIF -> FontFamily.Serif
    TypeFamily.MONOSPACE -> FontFamily.Monospace
    TypeFamily.GEOMETRIC, TypeFamily.DEFAULT -> FontFamily.Default
}

/** Builds a Material3 Typography from the active theme's type spec. */
fun typographyFor(spec: AppThemeSpec): androidx.compose.material3.Typography {
    val t = spec.typography
    val family = familyOf(t.family)
    fun style(
        size: Int,
        lineHeight: Int,
        weight: FontWeight,
        tracking: Double,
        italic: Boolean = false
    ) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.sp,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal
    )

    val quoteItalic = t.italicQuotes

    return androidx.compose.material3.Typography(
        displayLarge = style(t.displaySize, (t.displaySize * 1.12f).toInt(), t.displayWeight, t.displayTracking),
        displayMedium = style((t.displaySize * 0.88f).toInt(), (t.displaySize).toInt(), t.displayWeight, t.displayTracking),
        displaySmall = style((t.displaySize * 0.74f).toInt(), (t.displaySize * 0.92f).toInt(), t.displayWeight, t.displayTracking),
        headlineLarge = style((t.displaySize * 0.66f).toInt(), (t.displaySize * 0.86f).toInt(), t.titleWeight, t.titleTracking),
        headlineMedium = style((t.displaySize * 0.58f).toInt(), (t.displaySize * 0.80f).toInt(), t.titleWeight, t.titleTracking),
        headlineSmall = style((t.displaySize * 0.52f).toInt(), (t.displaySize * 0.74f).toInt(), t.titleWeight, t.titleTracking),
        titleLarge = style(t.displaySize / 2 + 4, t.displaySize / 2 + 8, t.titleWeight, t.titleTracking),
        titleMedium = style(16, 22, t.titleWeight, t.titleTracking),
        titleSmall = style(14, 20, t.titleWeight, t.titleTracking),
        bodyLarge = style(t.bodySize + 1, t.bodyLineHeight + 3, t.bodyWeight, 0.0, quoteItalic),
        bodyMedium = style(t.bodySize, t.bodyLineHeight, t.bodyWeight, 0.0),
        bodySmall = style(t.bodySize - 2, t.bodyLineHeight - 4, t.bodyWeight, 0.05),
        labelLarge = style(14, 20, t.labelWeight, t.labelTracking),
        labelMedium = style(12, 16, t.labelWeight, t.labelTracking),
        labelSmall = style(11, 15, t.labelWeight, t.labelTracking)
    )
}

/* ============================================================================
   ROOT THEME COMPOSABLE
   ========================================================================== */

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.DEFAULT,
    selectedBgId: Int = 0,
    patternIntensity: Float = 1f,
    colorVariant: Int = 0,
    content: @Composable () -> Unit
) {
    val spec = appTheme.spec
    val effectiveDark = darkTheme || spec.prefersDark
    val colors = spec.colors(effectiveDark, colorVariant)
    val scheme = spec.toColorScheme(effectiveDark, colorVariant)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            activity.window.statusBarColor = scheme.background.toArgb()
            activity.window.navigationBarColor = scheme.surface.toArgb()
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !effectiveDark
            controller.isAppearanceLightNavigationBars = !effectiveDark
        }
    }

    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val clampedIntensity = clampPatternIntensity(patternIntensity)
    val blurredBgBitmap = remember(
        appTheme,
        selectedBgId,
        effectiveDark,
        clampedIntensity,
        colorVariant,
        config.screenWidthDp,
        config.screenHeightDp
    ) {
        if (view.isInEditMode) {
            null
        } else {
            val widthPx = (config.screenWidthDp * density.density).toInt().coerceAtLeast(360)
            val heightPx = (config.screenHeightDp * density.density).toInt().coerceAtLeast(720)
            runCatching {
                createBlurredBackgroundBitmap(
                    widthPx = widthPx,
                    heightPx = heightPx,
                    baseDensity = density.density,
                    bgId = selectedBgId,
                    theme = appTheme,
                    isDark = effectiveDark,
                    intensity = clampedIntensity,
                    colorVariant = colorVariant
                )
            }.getOrNull()
        }
    }

    CompositionLocalProvider(
        LocalAppTheme provides appTheme,
        LocalThemeSpec provides spec,
        LocalThemeColors provides colors,
        LocalThemeIsDark provides effectiveDark,
        LocalThemeTypography provides spec.typography,
        LocalThemeMotion provides spec.motion,
        LocalThemeMetrics provides spec.metrics,
        LocalSelectedBackground provides selectedBgId,
        LocalPatternIntensity provides clampedIntensity,
        LocalThemeColorVariant provides colorVariant,
        LocalBlurredBackgroundBitmap provides blurredBgBitmap
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typographyFor(spec),
            shapes = Shapes(
                extraSmall = spec.shapes.badgeShape,
                small = spec.shapes.buttonShape,
                medium = spec.shapes.cardShape,
                large = spec.shapes.cardShape,
                extraLarge = spec.shapes.cardShape
            ),
            content = content
        )
    }
}

/* ============================================================================
   ELEVATION HELPER
   ========================================================================== */

fun Modifier.paperLiftShadow(
    theme: AppTheme = AppTheme.DEFAULT,
    isDarkTheme: Boolean = true,
    shadowElevation: androidx.compose.ui.unit.Dp = 4.dp
): Modifier {
    val spec = theme.spec
    if (!spec.hasHardShadow) return this
    val alpha = if (isDarkTheme) 0.34f else 0.16f
    return this.drawWithCache {
        val offsetY = (shadowElevation.toPx() * 0.5f).coerceAtLeast(1f)
        val spread = shadowElevation.toPx()
        onDrawBehind {
            drawRoundRect(
                color = Color.Black.copy(alpha = alpha),
                topLeft = androidx.compose.ui.geometry.Offset(0f, offsetY),
                size = androidx.compose.ui.geometry.Size(size.width, (size.height - offsetY).coerceAtLeast(0f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(spread, spread)
            )
        }
    }
}

// Measured floating navigation island height used as trailing scroll padding.
val LocalNavBarHeight = staticCompositionLocalOf { 0.dp }

@Composable
fun navBarBottomPadding(extra: androidx.compose.ui.unit.Dp = 0.dp): androidx.compose.ui.unit.Dp =
    LocalNavBarHeight.current + extra
