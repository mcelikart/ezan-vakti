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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/* ============================================================================
   THEME ENUM — 21 themes, Emerald Dome first and default
   ========================================================================== */

enum class AppTheme(val specId: String) {
    CLASSIC_EMERALD("CLASSIC_EMERALD"),
    SELCUKLU("SELCUKLU"),
    IZNIK("IZNIK"),
    REVAK("REVAK"),
    SEDEF("SEDEF"),
    LALEZAR("LALEZAR"),
    HUNKAR("HUNKAR"),
    FIRUZE("FIRUZE"),
    YESIL_AVLU("YESIL_AVLU"),
    GUL_BAHCESI("GUL_BAHCESI"),
    SAHIL("SAHIL"),
    KAMER("KAMER"),
    DIVAN("DIVAN"),
    SELSEBIL("SELSEBIL"),
    MEDRESE("MEDRESE"),
    NAKKAS("NAKKAS"),
    MERCAN("MERCAN"),
    GECE_CINISI("GECE_CINISI"),
    AK_MERMER("AK_MERMER"),
    SERVI("SERVI"),
    SAFAK("SAFAK");

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
val LocalThemeColors = staticCompositionLocalOf { ThemeRegistry.default.colors(true) }
val LocalThemeIsDark = staticCompositionLocalOf { true }

/* ============================================================================
   CONVENIENCE ACCESSORS
   ========================================================================== */

val AppTheme.buttonShape: Shape get() = spec.shapes.buttonShape
val AppTheme.cardShape: Shape get() = spec.shapes.cardShape
val AppTheme.badgeShape: Shape get() = spec.shapes.badgeShape
val AppTheme.buttonBorder: BorderStroke get() = spec.buttonBorder
val AppTheme.buttonGradient: List<Color> @Composable get() = spec.colors(LocalThemeIsDark.current).cardGradient
val AppTheme.buttonTextColor: Color @Composable get() = spec.colors(LocalThemeIsDark.current).buttonTextColor
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
   THEME-AWARE COMPONENTS
   ========================================================================== */

@Composable
fun ThemedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val spec = LocalThemeSpec.current
    val colors = LocalThemeColors.current
    val shape = spec.shapes.buttonShape
    val base = modifier
        .clip(shape)
        .background(Brush.horizontalGradient(colors.cardGradient))
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
        CardTreatment.GLASS -> BorderStroke(1.dp, colors.outline.copy(alpha = 0.28f))
        else -> spec.buttonBorder
    }

    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = shape,
        color = container,
        border = borderStroke,
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

// Kept as a one-item compatibility model for old saved background values.
data class AppBackgroundItem(
    val id: Int,
    val name: String,
    val category: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color
)

val APP_BACKGROUND_ITEMS = listOf(
    AppBackgroundItem(0, "Teknik Izgara", "Sistem", "Temayla birlikte gelen hafif teknik yüzey", Color(0xFF080A0B), Color(0xFFFF6B1A))
)

fun Modifier.drawCustomBackground(
    bgId: Int = 0,
    theme: AppTheme = AppTheme.DEFAULT,
    isDark: Boolean = true,
    intensity: Float = 1f
): Modifier = drawWithCache {
    val spec = theme.spec
    val dark = isDark || spec.prefersDark
    val palette = spec.colors(dark)

    val base = palette.background
    val line = palette.outline
    val accent = palette.primary
    val accent2 = palette.goldMotifColor

    onDrawBehind {
        drawRect(base)
        if (spec.pattern == ThemePattern.CLEAN) return@onDrawBehind

        // Soft top-light wash gives every non-flat theme depth without a shadow pass.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.05f * clampPatternIntensity(intensity)),
                    Color.Transparent,
                    accent2.copy(alpha = 0.035f * clampPatternIntensity(intensity))
                )
            )
        )

        drawThemePattern(
            pattern = spec.pattern,
            p = PatternPalette(
                background = base,
                line = line,
                accent = accent,
                accent2 = accent2
            ),
            intensity = clampPatternIntensity(intensity)
        )
    }
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

private fun String.toLabelCase(style: LabelCase): String = when (style) {
    LabelCase.NONE -> this
    LabelCase.UPPERCASE -> uppercase()
    LabelCase.SMALL_CAPS_LIKE -> replaceFirstChar { it.uppercase() }
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
    content: @Composable () -> Unit
) {
    val spec = appTheme.spec
    val effectiveDark = darkTheme || spec.prefersDark
    val colors = spec.colors(effectiveDark)
    val scheme = spec.toColorScheme(effectiveDark)

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

    CompositionLocalProvider(
        LocalAppTheme provides appTheme,
        LocalThemeSpec provides spec,
        LocalThemeColors provides colors,
        LocalThemeIsDark provides effectiveDark,
        LocalThemeTypography provides spec.typography,
        LocalThemeMotion provides spec.motion,
        LocalThemeMetrics provides spec.metrics,
        LocalSelectedBackground provides 0,
        LocalPatternIntensity provides clampPatternIntensity(patternIntensity)
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
    // Soft-lift themes stay flat; hard-shadow themes get a grounded drop shadow.
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

/**
 * Measured height of the floating navigation bar, in Dp.
 *
 * Screens that scroll add this to their bottom content padding so the last item
 * can always be scrolled clear of the bar. Reading it from a CompositionLocal
 * rather than hard-coding a value matters because the bar's height changes with
 * the active theme's nav style and with the system navigation inset.
 */
val LocalNavBarHeight = staticCompositionLocalOf { 0.dp }

/**
 * Bottom content padding a scrolling screen should reserve.
 *
 * Defaults to the bar height alone. An extra margin here reads as a dead band
 * once the list is scrolled to the end, so callers that want breathing room
 * should add it themselves rather than have it baked in for every screen.
 */
@Composable
fun navBarBottomPadding(extra: androidx.compose.ui.unit.Dp = 0.dp): androidx.compose.ui.unit.Dp =
    LocalNavBarHeight.current + extra
