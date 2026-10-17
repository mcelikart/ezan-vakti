package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ============================================================================
   1. COLOR MODEL
   ========================================================================== */

data class ThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val cardGradient: List<Color>,
    val buttonTextColor: Color,
    val glowColor: Color,
    val goldMotifColor: Color
)

/* ============================================================================
   2. SHAPE MODEL
   ========================================================================== */

data class ThemeShapes(
    val buttonShape: CornerBasedShape,
    val cardShape: CornerBasedShape,
    val badgeShape: CornerBasedShape = CutCornerShape(topEnd = 6.dp, bottomStart = 6.dp)
)

/** Extra per-theme metrics so themes differ structurally, not only in hue. */
data class ThemeMetrics(
    val cardPadding: Dp = 16.dp,
    val screenGutter: Dp = 20.dp,
    val cardGap: Dp = 12.dp,
    val cardRadiusTop: Dp = 20.dp,
    val cardRadiusBottom: Dp = 10.dp,
    val buttonHeight: Dp = 52.dp,
    val listRowHeight: Dp = 64.dp,
    val heroCorner: Dp = 28.dp
)

/* ============================================================================
   3. TYPOGRAPHY MODEL
   ========================================================================== */

enum class TypeFamily { DEFAULT, SERIF, MONOSPACE, GEOMETRIC }

enum class LabelCase { NONE, UPPERCASE, SMALL_CAPS_LIKE }

data class ThemeTypographySpec(
    val family: TypeFamily = TypeFamily.DEFAULT,
    val displayWeight: FontWeight = FontWeight.Bold,
    val displaySize: Int = 30,
    val displayTracking: Double = 0.0,
    val displayAlign: TextAlign = TextAlign.Start,
    val titleWeight: FontWeight = FontWeight.Bold,
    val titleTracking: Double = 0.0,
    val bodyWeight: FontWeight = FontWeight.Normal,
    val bodySize: Int = 14,
    val bodyLineHeight: Int = 20,
    val labelWeight: FontWeight = FontWeight.Medium,
    val labelTracking: Double = 0.1,
    val labelCase: LabelCase = LabelCase.NONE,
    val numericTabular: Boolean = false,
    val italicQuotes: Boolean = false
)

/* ============================================================================
   4. MOTION MODEL
   ========================================================================== */

enum class EntranceStyle { NONE, FADE, SLIDE_UP, SCALE_IN, ORBIT, PAGE_TURN, FLIP }

enum class LoopStyle { NONE, TWINKLE, BREATHE, SHIMMER, DRIFT }

data class ThemeMotionSpec(
    val durationMillis: Int = 220,
    val useSpring: Boolean = false,
    val dampingRatio: Float = 0.85f,
    val stiffness: Float = 380f,
    val entrance: EntranceStyle = EntranceStyle.FADE,
    val loop: LoopStyle = LoopStyle.NONE,
    val loopPeriodMillis: Long = 6000L,
    val respectReduceMotion: Boolean = true
)

/* ============================================================================
   5. PATTERN MODEL
   ========================================================================== */

enum class ThemePattern {
    TECH_GRID,        // Kubbeli Zümrüt (mevcut)
    TUGHRA_BAND,     // Osmanlı Hümayun
    AZULEJO,          // Andalusya
    EBRE,             // Perşe Bahçe
    ZELLIGE,          // Magreb
    CLEAN,            // Hicri Minimal
    WHIRL,            // Semazen
    KAABA_LINES,      // Kaabe Mono
    SAND_DRIFT,       // Çöl Gülü
    STARFIELD,        // Celestial
    ARABESQUE,        // Bağdat Altın Çağı
    OLIVE_DAWN,       // Zeytinyağı Sabah
    PAPER_ATELIER,    // Kâğıt Atölye
    M3_SUNSET,        // M3 Gün Batımı
    COASTAL_PATH,     // Sahil Yolu
    KRAFT_WARM,       // Sıcak Kâğıt
    PASTEL_KNOT,      // Gökkuşağı Düğümü
    WORDMARK,         // Söz Güvenli
    QUIET_GRID,       // Sıfır Gürültü
    HAIRLINE,         // Sistem Işığı
    PLAIN_VOICE       // Net Söz
}

/* ============================================================================
   6. COMPONENT BEHAVIOR MODEL
   ========================================================================== */

enum class ButtonFill { SOLID, GRADIENT, TONAL, OUTLINED, TEXT }

enum class ThemeDensity { COMPACT, STANDARD, RELAXED, SPACIOUS }

enum class IconStyle { FILLED, OUTLINED, ROUNDED, SHARP }

enum class CardTreatment { FLAT, OUTLINED, ELEVATED, GLOW, GLASS, BRUTAL_TOPLINE, GRADIENT_EDGE }

enum class DividerStyle { NONE, HAIRLINE, DASHED, GRADIENT, DOUBLE }

/**
 * Structural layout family.
 *
 * A theme that only swaps hues is a filter, not a design. This is the axis that
 * makes two themes feel like different products: the same prayer data is
 * arranged as a poster, a dashboard, a list or a journal depending on the
 * active theme, so switching theme re-composes the screen rather than
 * recolouring it.
 */
enum class ThemeLayout {
    /** Full-bleed hero countdown, everything else in one long column. */
    POSTER,

    /** Hero plus a two-column grid of dense information tiles. */
    DASHBOARD,

    /** Row-based list, tight rhythm, best for scanning many prayers. */
    LEDGER,

    /** Sectioned with rules and generous leading, reads like a page. */
    JOURNAL,

    /** Single centred column, maximum breathing room per item. */
    CALM
}

enum class NavStyle { PILL, TABS, RAIL_DOCK, UNDERLINE }

/**
 * Where the app's global actions live.
 *
 * A theme that only restyles the home screen is still a filter. Moving the
 * actions themselves — into a side rail, into a bottom drawer, into a top bar —
 * changes the shape of the whole application, not just its palette.
 */
enum class ActionPlacement {
    /** Three tiles in the home content column, as today. */
    INLINE_TILES,

    /** A compact strip of chips directly under the location header. */
    HEADER_CHIPS,

    /** Pushed to the very bottom of the home list, below the detail cards. */
    FOOTER_TILES,

    /** Folded into the navigation bar as a fourth destination. */
    NAV_DESTINATION,

    /** A single icon on the right of the home header that opens a sheet. */
    HEADER_ACTION_SHEET
}

/**
 * Hero prominence, in descending order. This drives the countdown block's
 * height and whether the supporting details are visible without scrolling.
 */
enum class HeroScale { DOMINANT, BALANCED, COMPACT }

enum class PrayerComposition {
    DOME, DIAL, EDITORIAL, HORIZON, DEPARTURES, MOSAIC,
    LUNAR, POSTER, TIMELINE, CALENDAR, FOCUS
}

enum class HomeSection { WEATHER, MIRACLE, QUOTE, DETAILS }

data class ThemePatternSpec(val motifId: Int = 0, val intensity: Float = 1f)

/* ============================================================================
   7. ROOT SPEC
   ========================================================================== */

data class AppThemeSpec(
    val id: String,
    val turkishName: String,
    val englishName: String,
    val description: String,
    val category: String,
    val featureTag: String,
    val designMovement: String,
    val lightColors: ThemeColors,
    val darkColors: ThemeColors,
    val shapes: ThemeShapes,
    val metrics: ThemeMetrics = ThemeMetrics(),
    val typography: ThemeTypographySpec = ThemeTypographySpec(),
    val motion: ThemeMotionSpec = ThemeMotionSpec(),
    val pattern: ThemePattern = ThemePattern.TECH_GRID,
    val buttonFill: ButtonFill = ButtonFill.GRADIENT,
    val cardTreatment: CardTreatment = CardTreatment.OUTLINED,
    val density: ThemeDensity = ThemeDensity.STANDARD,
    val iconStyle: IconStyle = IconStyle.FILLED,
    val dividerStyle: DividerStyle = DividerStyle.HAIRLINE,
    val layout: ThemeLayout = ThemeLayout.POSTER,
    val navStyle: NavStyle = NavStyle.PILL,
    val actionPlacement: ActionPlacement = ActionPlacement.INLINE_TILES,
    val heroScale: HeroScale = HeroScale.BALANCED,
    val composition: PrayerComposition = PrayerComposition.DOME,
    val scheduleStyle: ScheduleStyle = ScheduleStyle.CLASSIC,
    val headerStyle: HomeHeaderStyle = HomeHeaderStyle.CLASSIC,
    val sections: List<HomeSection> = HomeSection.entries,
    val defaultPattern: ThemePatternSpec = ThemePatternSpec(),
    val buttonBorder: BorderStroke,
    val isGlass: Boolean = false,
    val hasHardShadow: Boolean = false,
    val prefersDark: Boolean = true,
    /**
     * Content-first themes (HIG "content over chrome") drop non-text contrast
     * requirements: their `outline` is a decorative hairline, not a control
     * border, so the 3:1 UI-component gate does not apply.
     */
    val emphasizeContent: Boolean = true
) {
    fun colors(dark: Boolean): ThemeColors = (if (dark || prefersDark) darkColors else lightColors).accessible()

    fun toColorScheme(dark: Boolean): ColorScheme {
        val useDark = dark || prefersDark
        val c = colors(useDark)
        val base = if (useDark) darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer, onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.secondary, onSecondary = c.onSecondary,
            secondaryContainer = c.secondaryContainer, onSecondaryContainer = c.onSecondaryContainer,
            tertiary = c.tertiary, onTertiary = c.onTertiary,
            background = c.background, onBackground = c.onBackground,
            surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.onSurfaceVariant,
            outline = c.outline
        ) else lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer, onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.secondary, onSecondary = c.onSecondary,
            secondaryContainer = c.secondaryContainer, onSecondaryContainer = c.onSecondaryContainer,
            tertiary = c.tertiary, onTertiary = c.onTertiary,
            background = c.background, onBackground = c.onBackground,
            surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.onSurfaceVariant,
            outline = c.outline
        )
        val tertiaryContainer = lerp(c.surface, c.tertiary, if (useDark) 0.18f else 0.12f)
        return base.copy(
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = readable(c.tertiary, tertiaryContainer),
            surfaceTint = c.primary,
            surfaceDim = lerp(c.surface, Color.Black, if (useDark) 0.18f else 0.04f),
            surfaceBright = lerp(c.surface, Color.White, if (useDark) 0.08f else 0.5f),
            surfaceContainerLowest = c.background,
            surfaceContainerLow = lerp(c.surface, c.surfaceVariant, 0.22f),
            surfaceContainer = lerp(c.surface, c.surfaceVariant, 0.42f),
            surfaceContainerHigh = lerp(c.surface, c.surfaceVariant, 0.68f),
            surfaceContainerHighest = c.surfaceVariant,
            outlineVariant = lerp(c.surface, c.outline, 0.55f),
            inverseSurface = c.onSurface,
            inverseOnSurface = c.surface,
            inversePrimary = readable(c.primary, c.onSurface),
            scrim = Color.Black.copy(alpha = if (useDark) 0.72f else 0.42f)
        )
    }

    /** Surfaces that must read as translucent (glass themes). */
    fun surfaceAlpha(isDark: Boolean): Float = when (cardTreatment) {
        CardTreatment.GLASS -> if (isDark) 0.62f else 0.78f
        else -> 1f
    }

    /** Overlay/sheet color for this theme. */
    fun overlayColor(isDark: Boolean): Color =
        if (cardTreatment == CardTreatment.GLASS) {
            colors(isDark).surface.copy(alpha = 0.86f)
        } else {
            colors(isDark).surface
        }
}

/* ============================================================================
   8. BUILD HELPERS
   ========================================================================== */

private fun shapeToken(
    metrics: ThemeMetrics,
    button: Dp,
    cardTop: Dp = metrics.cardRadiusTop,
    cardBottom: Dp = metrics.cardRadiusBottom,
    badge: Dp = 8.dp
) = ThemeShapes(
    buttonShape = RoundedCornerShape(button),
    cardShape = RoundedCornerShape(
        topStart = cardTop, topEnd = cardTop,
        bottomEnd = cardBottom, bottomStart = cardBottom
    ),
    badgeShape = RoundedCornerShape(badge)
)

internal fun darkPalette(
    accent: Color,
    secondary: Color,
    tertiary: Color,
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    outline: Color,
    onPrimary: Color = Color(0xFF120A04),
    gradientPair: List<Color>? = null,
    buttonText: Color = Color.White
) = ThemeColors(
    primary = accent,
    onPrimary = onPrimary,
    primaryContainer = lerp(surface, accent, 0.20f),
    onPrimaryContainer = Color(0xFFFFF8F2),
    secondary = secondary,
    onSecondary = readable(secondary, surface),
    secondaryContainer = lerp(surface, secondary, 0.16f),
    onSecondaryContainer = Color(0xFFF3F5F6),
    tertiary = tertiary,
    onTertiary = readable(tertiary, surface),
    background = background,
    onBackground = onSurface,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    cardGradient = gradientPair ?: listOf(lerp(surface, accent, 0.16f), lerp(background, accent, 0.04f)),
    buttonTextColor = buttonText,
    glowColor = accent,
    goldMotifColor = tertiary
)

internal fun lightPalette(
    accent: Color,
    secondary: Color,
    tertiary: Color,
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    outline: Color,
    gradientPair: List<Color>? = null,
    buttonText: Color = Color.White
) = ThemeColors(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = lerp(Color.White, accent, 0.13f),
    onPrimaryContainer = Color(0xFF28120A),
    secondary = secondary,
    onSecondary = Color.White,
    secondaryContainer = lerp(Color.White, secondary, 0.13f),
    onSecondaryContainer = Color(0xFF111416),
    tertiary = tertiary,
    onTertiary = readable(tertiary, Color.White),
    background = background,
    onBackground = onSurface,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    cardGradient = gradientPair ?: listOf(accent, lerp(accent, Color.Black, 0.28f)),
    buttonTextColor = buttonText,
    glowColor = accent,
    goldMotifColor = tertiary
)

/* ============================================================================
   9. REGISTRY — 21 THEMES
   ========================================================================== */

object ThemeRegistry {

    /* ---------- 1. KUBBELİ ZÜMRÜT (korunuyor, varsayılan) ---------- */
    val CLASSIC_EMERALD = AppThemeSpec(
        id = "CLASSIC_EMERALD",
        turkishName = "Kubbeli Zümrüt",
        englishName = "Emerald Dome",
        description = "Önceki yeşil temanın zümrüt yüzeyleri, altın hatları ve kubbe kıvrımları.",
        category = "Klasik",
        featureTag = "Zümrüt ve altın",
        designMovement = "Klasik İslami mimari",
        lightColors = ThemeColors(
            primary = Color(0xFF0F4C3A), onPrimary = Color.White,
            primaryContainer = Color(0xFFD8EFE4), onPrimaryContainer = Color(0xFF083226),
            secondary = Color(0xFFC9A227), onSecondary = Color(0xFF1E3329),
            secondaryContainer = Color(0xFFFBF3D5), onSecondaryContainer = Color(0xFF5A4505),
            tertiary = Color(0xFF2E7D32), onTertiary = Color.White,
            background = Color(0xFFF4F8F5), onBackground = Color(0xFF0F2D23),
            surface = Color.White, onSurface = Color(0xFF0F2D23),
            surfaceVariant = Color(0xFFE5F2EC), onSurfaceVariant = Color(0xFF2B4E41),
            outline = Color(0xFF92751C),
            cardGradient = listOf(Color(0xFF0F4C3A), Color(0xFF07271E)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFD4AF37), goldMotifColor = Color(0xFFC59B27)
        ),
        darkColors = ThemeColors(
            primary = Color(0xFFE2C46C), onPrimary = Color(0xFF0A281E),
            primaryContainer = Color(0xFF1D5241), onPrimaryContainer = Color(0xFFF0E6C2),
            secondary = Color(0xFFA1E8C5), onSecondary = Color(0xFF0A281E),
            secondaryContainer = Color(0xFF153B2F), onSecondaryContainer = Color(0xFFA1E8C5),
            tertiary = Color(0xFF81C784), onTertiary = Color(0xFF071B14),
            background = Color(0xFF071B14), onBackground = Color(0xFFE8F5E9),
            surface = Color(0xFF0E2E23), onSurface = Color(0xFFE8F5E9),
            surfaceVariant = Color(0xFF163C30), onSurfaceVariant = Color(0xFFC4DFD5),
            outline = Color(0xFFE2C46C),
            cardGradient = listOf(Color(0xFF124032), Color(0xFF092019)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFE2C46C), goldMotifColor = Color(0xFFF3C64F)
        ),
        shapes = ThemeShapes(
            buttonShape = RoundedCornerShape(20.dp),
            cardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
        ),
        buttonBorder = BorderStroke(1.6.dp, Color(0xFFD4AF37)),
        prefersDark = false,
        layout = ThemeLayout.POSTER,
        navStyle = NavStyle.PILL,
        actionPlacement = ActionPlacement.INLINE_TILES,
        heroScale = HeroScale.DOMINANT,
    )

    val default = CLASSIC_EMERALD
    val all: List<AppThemeSpec> = listOf(CLASSIC_EMERALD) + heritageThemes()


    fun getThemeById(id: String): AppThemeSpec = all.firstOrNull { it.id == id } ?: default
}

/* ============================================================================
   10. COMPOSITION LOCALS
   ========================================================================== */

val LocalThemeSpec = staticCompositionLocalOf { ThemeRegistry.default }
val LocalThemeTypography = staticCompositionLocalOf { ThemeRegistry.default.typography }
val LocalThemeMotion = staticCompositionLocalOf { ThemeRegistry.default.motion }
val LocalThemeMetrics = staticCompositionLocalOf { ThemeRegistry.default.metrics }
