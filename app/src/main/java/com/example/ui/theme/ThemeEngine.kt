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
   1. COLOR MODEL & COLOR OPTIONS
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

data class ThemeColorOption(
    val index: Int,
    val name: String,
    val subtitle: String,
    val primaryPreview: Color,
    val accentPreview: Color,
    val surfacePreview: Color
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

enum class ThemeLayout {
    POSTER,
    DASHBOARD,
    LEDGER,
    JOURNAL,
    CALM
}

enum class NavStyle { PILL, TABS, RAIL_DOCK, UNDERLINE }

enum class ActionPlacement {
    INLINE_TILES,
    HEADER_CHIPS,
    FOOTER_TILES,
    NAV_DESTINATION,
    HEADER_ACTION_SHEET
}

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
    val emphasizeContent: Boolean = true
) {
    val colorOptions: List<ThemeColorOption>
        get() = buildColorOptionsForSpec(this)

    fun colors(dark: Boolean, colorVariant: Int = 0): ThemeColors {
        val useDark = dark || prefersDark
        val base = if (useDark) darkColors else lightColors
        if (colorVariant <= 0) return base.accessible()
        return applyColorVariant(base, id, useDark, colorVariant).accessible()
    }

    fun toColorScheme(dark: Boolean, colorVariant: Int = 0): ColorScheme {
        val useDark = dark || prefersDark
        val c = colors(useDark, colorVariant)
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
        CardTreatment.GLASS -> if (isDark) 0.62f else 0.76f
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
   7B. CURATED COLOR VARIANTS FOR KUBBELİ ZÜMRÜT & ALL GLASS THEMES
   ========================================================================== */

private data class VariantSeed(
    val name: String,
    val subtitle: String,
    val lightPrimary: Color,
    val darkPrimary: Color,
    val accentGold: Color,
    val tertiaryTint: Color,
    val lightBgTint: Color,
    val darkBgTint: Color
)

private fun variantSeedsFor(spec: AppThemeSpec): List<VariantSeed> {
    if (spec.id == "CLASSIC_EMERALD") {
        return listOf(
            VariantSeed(
                name = "Asli Zümrüt",
                subtitle = "Klasik Zümrüt & Altın",
                lightPrimary = spec.lightColors.primary,
                darkPrimary = spec.darkColors.primary,
                accentGold = spec.lightColors.goldMotifColor,
                tertiaryTint = spec.lightColors.tertiary,
                lightBgTint = spec.lightColors.background,
                darkBgTint = spec.darkColors.background
            ),
            VariantSeed(
                name = "Ravza Yeşili",
                subtitle = "Medine Yeşili & Kehribar",
                lightPrimary = Color(0xFF0B5D46),
                darkPrimary = Color(0xFFF0C96B),
                accentGold = Color(0xFFD99B26),
                tertiaryTint = Color(0xFF1F8A65),
                lightBgTint = Color(0xFFF1F8F5),
                darkBgTint = Color(0xFF051912)
            ),
            VariantSeed(
                name = "Selçuklu Çam",
                subtitle = "Çam Yeşili & Bakır",
                lightPrimary = Color(0xFF184A30),
                darkPrimary = Color(0xFFE0B46A),
                accentGold = Color(0xFFC88A32),
                tertiaryTint = Color(0xFF3E7A52),
                lightBgTint = Color(0xFFF4F7F2),
                darkBgTint = Color(0xFF091A10)
            ),
            VariantSeed(
                name = "Firuze Zümrüt",
                subtitle = "Su Yeşili & Şampanya",
                lightPrimary = Color(0xFF0D5550),
                darkPrimary = Color(0xFFE5CC7A),
                accentGold = Color(0xFFC9A646),
                tertiaryTint = Color(0xFF207A74),
                lightBgTint = Color(0xFFF0F8F7),
                darkBgTint = Color(0xFF061B19)
            ),
            VariantSeed(
                name = "Hünkâr Zümrüt",
                subtitle = "Derin Zümrüt & Gümüş Altın",
                lightPrimary = Color(0xFF123D2E),
                darkPrimary = Color(0xFFDFC880),
                accentGold = Color(0xFFBFA044),
                tertiaryTint = Color(0xFF2A6B52),
                lightBgTint = Color(0xFFF2F6F4),
                darkBgTint = Color(0xFF05130E)
            )
        )
    }

    // 5 curated glass color options for each of the 10 glassmorphic themes
    return listOf(
        VariantSeed(
            name = "Asli Kristal",
            subtitle = "Varsayılan Cam Tonu",
            lightPrimary = spec.lightColors.primary,
            darkPrimary = spec.darkColors.primary,
            accentGold = spec.lightColors.goldMotifColor,
            tertiaryTint = spec.lightColors.tertiary,
            lightBgTint = spec.lightColors.background,
            darkBgTint = spec.darkColors.background
        ),
        VariantSeed(
            name = "Altın Hale",
            subtitle = "Sıcak Kehribar Yansıma",
            lightPrimary = lerp(spec.lightColors.primary, Color(0xFF7A440E), 0.35f),
            darkPrimary = Color(0xFFF5C85B),
            accentGold = Color(0xFFE5A930),
            tertiaryTint = lerp(spec.lightColors.tertiary, Color(0xFFC87D20), 0.40f),
            lightBgTint = lerp(spec.lightColors.background, Color(0xFFFFF6E5), 0.45f),
            darkBgTint = lerp(spec.darkColors.background, Color(0xFF1E1205), 0.38f)
        ),
        VariantSeed(
            name = "Buzul Prizma",
            subtitle = "Soğuk Kristal & Safir",
            lightPrimary = lerp(spec.lightColors.primary, Color(0xFF0B4870), 0.42f),
            darkPrimary = Color(0xFF8CD2FA),
            accentGold = Color(0xFF90CAF9),
            tertiaryTint = lerp(spec.lightColors.tertiary, Color(0xFF1E78B4), 0.45f),
            lightBgTint = lerp(spec.lightColors.background, Color(0xFFEAF6FF), 0.48f),
            darkBgTint = lerp(spec.darkColors.background, Color(0xFF051626), 0.42f)
        ),
        VariantSeed(
            name = "Mercan & Gül",
            subtitle = "Yakut Kristal Parıltı",
            lightPrimary = lerp(spec.lightColors.primary, Color(0xFF781B38), 0.40f),
            darkPrimary = Color(0xFFF4A9BE),
            accentGold = Color(0xFFE5989B),
            tertiaryTint = lerp(spec.lightColors.tertiary, Color(0xFFB5385A), 0.42f),
            lightBgTint = lerp(spec.lightColors.background, Color(0xFFFFF0F4), 0.45f),
            darkBgTint = lerp(spec.darkColors.background, Color(0xFF210812), 0.38f)
        ),
        VariantSeed(
            name = "Zümrüt Sırça",
            subtitle = "Sırlı Yeşil & Altın Cam",
            lightPrimary = lerp(spec.lightColors.primary, Color(0xFF0E5442), 0.44f),
            darkPrimary = Color(0xFF8CE2C0),
            accentGold = Color(0xFFD4AF37),
            tertiaryTint = lerp(spec.lightColors.tertiary, Color(0xFF1F8066), 0.45f),
            lightBgTint = lerp(spec.lightColors.background, Color(0xFFEEF9F5), 0.48f),
            darkBgTint = lerp(spec.darkColors.background, Color(0xFF061C15), 0.40f)
        )
    )
}

internal fun buildColorOptionsForSpec(spec: AppThemeSpec): List<ThemeColorOption> {
    return variantSeedsFor(spec).mapIndexed { index, seed ->
        ThemeColorOption(
            index = index,
            name = seed.name,
            subtitle = seed.subtitle,
            primaryPreview = seed.lightPrimary,
            accentPreview = seed.accentGold,
            surfacePreview = seed.lightBgTint
        )
    }
}

private fun applyColorVariant(
    base: ThemeColors,
    specId: String,
    isDark: Boolean,
    variantIndex: Int
): ThemeColors {
    val spec = ThemeRegistry.getThemeById(specId)
    val seeds = variantSeedsFor(spec)
    val seed = seeds.getOrNull(variantIndex.coerceIn(0, seeds.lastIndex)) ?: return base
    if (variantIndex == 0) return base

    return if (!isDark) {
        val bg = seed.lightBgTint
        val surf = lerp(Color.White, bg, 0.35f)
        val surfVar = lerp(bg, seed.lightPrimary, 0.14f)
        val pri = seed.lightPrimary
        val sec = seed.accentGold
        val ter = seed.tertiaryTint
        base.copy(
            primary = pri,
            onPrimary = Color.White,
            primaryContainer = lerp(Color.White, pri, 0.18f),
            onPrimaryContainer = lerp(pri, Color.Black, 0.45f),
            secondary = sec,
            onSecondary = lerp(pri, Color.Black, 0.55f),
            secondaryContainer = lerp(Color.White, sec, 0.22f),
            onSecondaryContainer = lerp(sec, Color.Black, 0.65f),
            tertiary = ter,
            onTertiary = Color.White,
            background = bg,
            onBackground = lerp(pri, Color.Black, 0.55f),
            surface = surf,
            onSurface = lerp(pri, Color.Black, 0.55f),
            surfaceVariant = surfVar,
            onSurfaceVariant = lerp(pri, Color.Black, 0.35f),
            outline = lerp(sec, Color.Black, 0.25f),
            cardGradient = listOf(pri, lerp(pri, Color.Black, 0.45f)),
            glowColor = sec,
            goldMotifColor = sec
        )
    } else {
        val bg = seed.darkBgTint
        val surf = lerp(bg, seed.lightPrimary, 0.22f)
        val surfVar = lerp(bg, seed.lightPrimary, 0.36f)
        val pri = seed.darkPrimary
        val sec = lerp(seed.accentGold, Color.White, 0.25f)
        val ter = lerp(seed.tertiaryTint, Color.White, 0.32f)
        base.copy(
            primary = pri,
            onPrimary = lerp(bg, Color.Black, 0.25f),
            primaryContainer = lerp(surf, seed.lightPrimary, 0.45f),
            onPrimaryContainer = lerp(pri, Color.White, 0.55f),
            secondary = sec,
            onSecondary = lerp(bg, Color.Black, 0.25f),
            secondaryContainer = lerp(surf, sec, 0.22f),
            onSecondaryContainer = sec,
            tertiary = ter,
            onTertiary = lerp(bg, Color.Black, 0.3f),
            background = bg,
            onBackground = lerp(Color.White, pri, 0.12f),
            surface = surf,
            onSurface = lerp(Color.White, pri, 0.12f),
            surfaceVariant = surfVar,
            onSurfaceVariant = lerp(Color.White, pri, 0.26f),
            outline = pri,
            cardGradient = listOf(lerp(surf, seed.lightPrimary, 0.38f), bg),
            glowColor = pri,
            goldMotifColor = seed.accentGold
        )
    }
}

/* ============================================================================
   8. BUILD HELPERS
   ========================================================================== */

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
   9. REGISTRY — 11 THEMES (KUBBELİ ZÜMRÜT UNTOUCHED + 10 GLASS THEMES)
   ========================================================================== */

object ThemeRegistry {

    /* ---------- 1. KUBBELİ ZÜMRÜT (korunuyor, varsayılan — ASLA DOKUNULMADI) ---------- */
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
