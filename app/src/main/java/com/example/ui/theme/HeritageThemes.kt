package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ScheduleStyle { CLASSIC, ARCADES, TILES, LINES, TIMELINE, PAIRED, RIBBON, COMPACT, ALMANAC, MEDALLIONS, PANELS }
enum class HomeHeaderStyle { CLASSIC, MIHRAB, MASTHEAD, SIDEBAR, RULE, COMPACT }

/**
 * 10 glassmorphic dome themes faithful to the layout structure of Kubbeli Zümrüt
 * (CLASSIC_EMERALD), while each features frosted glassmorphism, distinct button
 * and frame silhouettes, custom specular borders, and architectural backgrounds.
 */
internal fun heritageThemes(): List<AppThemeSpec> = listOf(
    // 1. KUBBELİ SAFİR — High-Arch Crystal Glass + Pill-Arch Buttons
    kubbeliTheme(
        id = "KUBBELI_SAFIR",
        turkishName = "Kubbeli Safir",
        englishName = "Sapphire Dome",
        description = "Sultanahmet çinilerinden ilham alan buzlu safir cam yüzeyler, kemer kesimli kristal butonlar ve çift kandilli kubbe.",
        category = "Cam (Glass)",
        featureTag = "Buzlu safir cam • Kemer buton",
        designMovement = "Sultanahmet Kristal Cam",
        pattern = ThemePattern.AZULEJO,
        shapes = ThemeShapes(
            buttonShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 10.dp, bottomEnd = 10.dp),
            cardShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            badgeShape = RoundedCornerShape(50)
        ),
        metrics = ThemeMetrics(
            cardPadding = 16.dp,
            screenGutter = 20.dp,
            cardGap = 12.dp,
            cardRadiusTop = 36.dp,
            cardRadiusBottom = 16.dp,
            heroCorner = 36.dp
        ),
        typography = ThemeTypographySpec(
            family = TypeFamily.DEFAULT,
            displayWeight = FontWeight.ExtraBold,
            displaySize = 31,
            titleWeight = FontWeight.Bold,
            labelTracking = 0.14
        ),
        motion = ThemeMotionSpec(loop = LoopStyle.SHIMMER, loopPeriodMillis = 5500L),
        cardTreatment = CardTreatment.GLASS,
        dividerStyle = DividerStyle.DOUBLE,
        isGlass = true,
        customBorder = BorderStroke(
            1.8.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.78f),
                    Color(0xFF64B5F6).copy(alpha = 0.65f),
                    Color(0xFFD4AF37).copy(alpha = 0.85f)
                )
            )
        ),
        lightColors = ThemeColors(
            primary = Color(0xFF0F3D6E), onPrimary = Color.White,
            primaryContainer = Color(0xFFD8E8F8), onPrimaryContainer = Color(0xFF072547),
            secondary = Color(0xFFC89D28), onSecondary = Color(0xFF14273D),
            secondaryContainer = Color(0xFFFBF1D4), onSecondaryContainer = Color(0xFF543F04),
            tertiary = Color(0xFF1E6B99), onTertiary = Color.White,
            background = Color(0xFFEAF2FA), onBackground = Color(0xFF0D233A),
            surface = Color(0xFFF8FBFF), onSurface = Color(0xFF0D233A),
            surfaceVariant = Color(0xFFDCEAF8), onSurfaceVariant = Color(0xFF234261),
            outline = Color(0xFF94741B),
            cardGradient = listOf(Color(0xFF124B87), Color(0xFF071E38)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFD4AF37), goldMotifColor = Color(0xFFC59B27)
        ),
        darkColors = ThemeColors(
            primary = Color(0xFFE4C56E), onPrimary = Color(0xFF081F38),
            primaryContainer = Color(0xFF173F69), onPrimaryContainer = Color(0xFFF3E8C6),
            secondary = Color(0xFF9AD0F5), onSecondary = Color(0xFF081F38),
            secondaryContainer = Color(0xFF112F4E), onSecondaryContainer = Color(0xFF9AD0F5),
            tertiary = Color(0xFF64B5F6), onTertiary = Color(0xFF061526),
            background = Color(0xFF061629), onBackground = Color(0xFFE6F0FA),
            surface = Color(0xFF0D2542), onSurface = Color(0xFFE6F0FA),
            surfaceVariant = Color(0xFF15355A), onSurfaceVariant = Color(0xFFC2D7EC),
            outline = Color(0xFFE4C56E),
            cardGradient = listOf(Color(0xFF164373), Color(0xFF07182E)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFE4C56E), goldMotifColor = Color(0xFFF3C64F)
        ),
        borderGold = Color(0xFFD4AF37)
    ),

    // 2. KUBBELİ YAKUT — Asymmetric Leaf-Cut Glass Frames & Diagonal Buttons
    kubbeliTheme(
        id = "KUBBELI_YAKUT",
        turkishName = "Kubbeli Yakut",
        englishName = "Ruby Dome",
        description = "Hünkâr mahfili yakut buzlu cam katmanları, çapraz yaprak kesimli butonlar ve asimetrik saray çerçeveleri.",
        category = "Cam (Glass)",
        featureTag = "Yakut kristal • Yaprak kesim",
        designMovement = "Osmanlı Hünkâr Cam Sanatı",
        pattern = ThemePattern.TUGHRA_BAND,
        shapes = ThemeShapes(
            buttonShape = RoundedCornerShape(topStart = 24.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 24.dp),
            cardShape = RoundedCornerShape(topStart = 34.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 34.dp),
            badgeShape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
        ),
        metrics = ThemeMetrics(
            cardPadding = 17.dp,
            screenGutter = 20.dp,
            cardGap = 12.dp,
            cardRadiusTop = 34.dp,
            cardRadiusBottom = 12.dp,
            heroCorner = 34.dp
        ),
        typography = ThemeTypographySpec(
            family = TypeFamily.SERIF,
            displayWeight = FontWeight.Bold,
            displaySize = 30,
            titleWeight = FontWeight.Bold,
            italicQuotes = true,
            labelTracking = 0.16
        ),
        motion = ThemeMotionSpec(loop = LoopStyle.BREATHE, loopPeriodMillis = 6500L),
        cardTreatment = CardTreatment.GLASS,
        dividerStyle = DividerStyle.DOUBLE,
        isGlass = true,
        customBorder = BorderStroke(
            2.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFFFFCDD2).copy(alpha = 0.8f),
                    Color(0xFFE0B43A).copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.65f)
                )
            )
        ),
        lightColors = ThemeColors(
            primary = Color(0xFF6E1428), onPrimary = Color.White,
            primaryContainer = Color(0xFFF8DCE2), onPrimaryContainer = Color(0xFF470816),
            secondary = Color(0xFFC99E26), onSecondary = Color(0xFF38121B),
            secondaryContainer = Color(0xFFFCF2D4), onSecondaryContainer = Color(0xFF564003),
            tertiary = Color(0xFF9E2A42), onTertiary = Color.White,
            background = Color(0xFFF9EEF1), onBackground = Color(0xFF330E18),
            surface = Color(0xFFFFF9FA), onSurface = Color(0xFF330E18),
            surfaceVariant = Color(0xFFF2DDE2), onSurfaceVariant = Color(0xFF5E2836),
            outline = Color(0xFF96731A),
            cardGradient = listOf(Color(0xFF7D172E), Color(0xFF380712)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFD4AF37), goldMotifColor = Color(0xFFC59B27)
        ),
        darkColors = ThemeColors(
            primary = Color(0xFFE6C66E), onPrimary = Color(0xFF330A14),
            primaryContainer = Color(0xFF5C1828), onPrimaryContainer = Color(0xFFF5E9C8),
            secondary = Color(0xFFF4A6B8), onSecondary = Color(0xFF330A14),
            secondaryContainer = Color(0xFF45121E), onSecondaryContainer = Color(0xFFF4A6B8),
            tertiary = Color(0xFFE5738A), onTertiary = Color(0xFF21060D),
            background = Color(0xFF1D060D), onBackground = Color(0xFFFAEAEE),
            surface = Color(0xFF300C17), onSurface = Color(0xFFFAEAEE),
            surfaceVariant = Color(0xFF451523), onSurfaceVariant = Color(0xFFE8C8D0),
            outline = Color(0xFFE6C66E),
            cardGradient = listOf(Color(0xFF5C1628), Color(0xFF24070F)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFE6C66E), goldMotifColor = Color(0xFFF3C64F)
        ),
        borderGold = Color(0xFFE0B43A)
    ),

    // 3. KUBBELİ FİRUZE — Diamond-Chamfered Glass Frames & Beveled Buttons
    kubbeliTheme(
        id = "KUBBELI_FIRUZE",
        turkishName = "Kubbeli Firuze",
        englishName = "Turquoise Dome",
        description = "Semerkand medreselerinin sırlı firuze kristal cam yüzeyleri, elmas pah kesimli butonlar ve sekizgen cam çerçeveler.",
        category = "Cam (Glass)",
        featureTag = "Firuze prizma • Elmas pah",
        designMovement = "Timurlu Semerkand Kristal",
        pattern = ThemePattern.ZELLIGE,
        shapes = ThemeShapes(
            buttonShape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 5.dp, bottomStart = 5.dp),
            cardShape = CutCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 10.dp, bottomEnd = 10.dp),
            badgeShape = CutCornerShape(8.dp)
        ),
        metrics = ThemeMetrics(
            cardPadding = 16.dp,
            screenGutter = 18.dp,
            cardGap = 12.dp,
            cardRadiusTop = 22.dp,
            cardRadiusBottom = 10.dp,
            heroCorner = 26.dp
        ),
        typography = ThemeTypographySpec(
            family = TypeFamily.GEOMETRIC,
            displayWeight = FontWeight.Bold,
            displaySize = 30,
            titleWeight = FontWeight.SemiBold,
            numericTabular = true
        ),
        motion = ThemeMotionSpec(loop = LoopStyle.SHIMMER, loopPeriodMillis = 5000L),
        cardTreatment = CardTreatment.GLASS,
        dividerStyle = DividerStyle.GRADIENT,
        isGlass = true,
        customBorder = BorderStroke(
            1.8.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFF80DEEA).copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.75f),
                    Color(0xFFD4AF37).copy(alpha = 0.85f)
                )
            )
        ),
        lightColors = ThemeColors(
            primary = Color(0xFF0A5C66), onPrimary = Color.White,
            primaryContainer = Color(0xFFD4F0F2), onPrimaryContainer = Color(0xFF05383E),
            secondary = Color(0xFFC79F27), onSecondary = Color(0xFF103236),
            secondaryContainer = Color(0xFFFBF2D4), onSecondaryContainer = Color(0xFF554104),
            tertiary = Color(0xFF17808C), onTertiary = Color.White,
            background = Color(0xFFE8F6F8), onBackground = Color(0xFF0B2E33),
            surface = Color(0xFFF7FEFF), onSurface = Color(0xFF0B2E33),
            surfaceVariant = Color(0xFFD6EEF1), onSurfaceVariant = Color(0xFF214B52),
            outline = Color(0xFF91741B),
            cardGradient = listOf(Color(0xFF0D6B78), Color(0xFF052E33)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFD4AF37), goldMotifColor = Color(0xFFC59B27)
        ),
        darkColors = ThemeColors(
            primary = Color(0xFFE2C46C), onPrimary = Color(0xFF07292E),
            primaryContainer = Color(0xFF134E57), onPrimaryContainer = Color(0xFFF0E6C2),
            secondary = Color(0xFF8CE2EC), onSecondary = Color(0xFF07292E),
            secondaryContainer = Color(0xFF0E3B42), onSecondaryContainer = Color(0xFF8CE2EC),
            tertiary = Color(0xFF4DD0E1), onTertiary = Color(0xFF051B1E),
            background = Color(0xFF04191C), onBackground = Color(0xFFE4F6F8),
            surface = Color(0xFF0A2B30), onSurface = Color(0xFFE4F6F8),
            surfaceVariant = Color(0xFF123D45), onSurfaceVariant = Color(0xFFBFE0E4),
            outline = Color(0xFFE2C46C),
            cardGradient = listOf(Color(0xFF124E57), Color(0xFF061E22)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFE2C46C), goldMotifColor = Color(0xFFF3C64F)
        ),
        borderGold = Color(0xFFD4AF37)
    ),

    // 4. KUBBELİ KEHRİBAR — Seljuk Octagonal Cut Glass & Crown Portal Frame
    kubbeliTheme(
        id = "KUBBELI_KEHRIBAR",
        turkishName = "Kubbeli Kehribar",
        englishName = "Amber Dome",
        description = "Selçuklu sekiz köşeli yıldız kesimli kehribar buzlu cam paneller, mukarnas köşeli butonlar ve bakır-altın ışıltı.",
        category = "Cam (Glass)",
        featureTag = "Kehribar cam • Sekizgen kesim",
        designMovement = "Anadolu Selçuklu Kristal Taçkapı",
        pattern = ThemePattern.ARABESQUE,
        shapes = ThemeShapes(
            buttonShape = CutCornerShape(14.dp),
            cardShape = CutCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            badgeShape = CutCornerShape(50)
        ),
        metrics = ThemeMetrics(
            cardPadding = 16.dp,
            screenGutter = 18.dp,
            cardGap = 12.dp,
            cardRadiusTop = 26.dp,
            cardRadiusBottom = 8.dp,
            heroCorner = 26.dp
        ),
        typography = ThemeTypographySpec(
            family = TypeFamily.SERIF,
            displayWeight = FontWeight.ExtraBold,
            displaySize = 30,
            titleWeight = FontWeight.Bold
        ),
        motion = ThemeMotionSpec(loop = LoopStyle.BREATHE, loopPeriodMillis = 6000L),
        cardTreatment = CardTreatment.GLASS,
        dividerStyle = DividerStyle.DOUBLE,
        isGlass = true,
        customBorder = BorderStroke(
            2.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFFFFE082).copy(alpha = 0.9f),
                    Color(0xFFD99B26).copy(alpha = 0.75f),
                    Color.White.copy(alpha = 0.65f)
                )
            )
        ),
        lightColors = ThemeColors(
            primary = Color(0xFF6B3E0B), onPrimary = Color.White,
            primaryContainer = Color(0xFFFAE9D2), onPrimaryContainer = Color(0xFF422404),
            secondary = Color(0xFFB8860B), onSecondary = Color.White,
            secondaryContainer = Color(0xFFFDF0CE), onSecondaryContainer = Color(0xFF4D3602),
            tertiary = Color(0xFF8D5518), onTertiary = Color.White,
            background = Color(0xFFF9F1E6), onBackground = Color(0xFF2E1B06),
            surface = Color(0xFFFFFAF2), onSurface = Color(0xFF2E1B06),
            surfaceVariant = Color(0xFFF1E3D0), onSurfaceVariant = Color(0xFF593B1A),
            outline = Color(0xFF8C6918),
            cardGradient = listOf(Color(0xFF7A470D), Color(0xFF381F04)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFD4AF37), goldMotifColor = Color(0xFFC59B27)
        ),
        darkColors = ThemeColors(
            primary = Color(0xFFE8C368), onPrimary = Color(0xFF2E1A05),
            primaryContainer = Color(0xFF57330A), onPrimaryContainer = Color(0xFFF6E7C4),
            secondary = Color(0xFFF5C77E), onSecondary = Color(0xFF2E1A05),
            secondaryContainer = Color(0xFF422708), onSecondaryContainer = Color(0xFFF5C77E),
            tertiary = Color(0xFFFFB74D), onTertiary = Color(0xFF1F1103),
            background = Color(0xFF1A0E03), onBackground = Color(0xFFFAF0E4),
            surface = Color(0xFF2B1806), onSurface = Color(0xFFFAF0E4),
            surfaceVariant = Color(0xFF3F240B), onSurfaceVariant = Color(0xFFE6D1B8),
            outline = Color(0xFFE8C368),
            cardGradient = listOf(Color(0xFF59340B), Color(0xFF241303)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFE8C368), goldMotifColor = Color(0xFFF3C64F)
        ),
        borderGold = Color(0xFFD99B26)
    ),

    // 5. KUBBELİ AMETİST — Organic Ebru Capsule Glass & Fluid Pebble Frames
    kubbeliTheme(
        id = "KUBBELI_AMETIST",
        turkishName = "Kubbeli Ametist",
        englishName = "Amethyst Dome",
        description = "Ebru dalgalı mürdüm-ametist buzlu cam yüzeyler, tam kapsül (pill) butonlar ve organik kıvrımlı tezhip çerçeveleri.",
        category = "Cam (Glass)",
        featureTag = "Ametist buzlu cam • Kapsül buton",
        designMovement = "Osmanlı Ebru ve Kristal Tezhip",
        pattern = ThemePattern.EBRE,
        shapes = ThemeShapes(
            buttonShape = RoundedCornerShape(50),
            cardShape = RoundedCornerShape(topStart = 38.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 38.dp),
            badgeShape = RoundedCornerShape(50)
        ),
        metrics = ThemeMetrics(
            cardPadding = 17.dp,
            screenGutter = 20.dp,
            cardGap = 13.dp,
            cardRadiusTop = 38.dp,
            cardRadiusBottom = 18.dp,
            heroCorner = 36.dp
        ),
        typography = ThemeTypographySpec(
            family = TypeFamily.SERIF,
            displayWeight = FontWeight.Bold,
            displaySize = 30,
            italicQuotes = true
        ),
        motion = ThemeMotionSpec(loop = LoopStyle.DRIFT, loopPeriodMillis = 7000L),
        cardTreatment = CardTreatment.GLASS,
        dividerStyle = DividerStyle.GRADIENT,
        isGlass = true,
        customBorder = BorderStroke(
            1.7.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.8f),
                    Color(0xFFCE93D8).copy(alpha = 0.75f),
                    Color(0xFFD4AF37).copy(alpha = 0.85f)
                )
            )
        ),
        lightColors = ThemeColors(
            primary = Color(0xFF4A205A), onPrimary = Color.White,
            primaryContainer = Color(0xFFEEE0F5), onPrimaryContainer = Color(0xFF2E1139),
            secondary = Color(0xFFC89F28), onSecondary = Color(0xFF281330),
            secondaryContainer = Color(0xFFFBF2D5), onSecondaryContainer = Color(0xFF544004),
            tertiary = Color(0xFF723887), onTertiary = Color.White,
            background = Color(0xFFF4ECF8), onBackground = Color(0xFF24102D),
            surface = Color(0xFFFCF8FF), onSurface = Color(0xFF24102D),
            surfaceVariant = Color(0xFFE9DCF0), onSurfaceVariant = Color(0xFF4B2B57),
            outline = Color(0xFF91731C),
            cardGradient = listOf(Color(0xFF58256B), Color(0xFF260E30)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFD4AF37), goldMotifColor = Color(0xFFC59B27)
        ),
        darkColors = ThemeColors(
            primary = Color(0xFFE4C56E), onPrimary = Color(0xFF260F30),
            primaryContainer = Color(0xFF482059), onPrimaryContainer = Color(0xFFF2E7C6),
            secondary = Color(0xFFDAB6F2), onSecondary = Color(0xFF260F30),
            secondaryContainer = Color(0xFF361742), onSecondaryContainer = Color(0xFFDAB6F2),
            tertiary = Color(0xFFBA68C8), onTertiary = Color(0xFF190920),
            background = Color(0xFF14071A), onBackground = Color(0xFFF3EAF7),
            surface = Color(0xFF230F2D), onSurface = Color(0xFFF3EAF7),
            surfaceVariant = Color(0xFF351842), onSurfaceVariant = Color(0xFFD9C6E3),
            outline = Color(0xFFE4C56E),
            cardGradient = listOf(Color(0xFF491E5B), Color(0xFF1B0924)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFE4C56E), goldMotifColor = Color(0xFFF3C64F)
        ),
        borderGold = Color(0xFFD4AF37)
    ),

    // 6. KUBBELİ ZEYTİN — Mihrab-Arch Top Glass Buttons & Arcade Frames
    kubbeliTheme(
        id = "KUBBELI_ZEYTIN",
        turkishName = "Kubbeli Zeytin",
        englishName = "Olive Dome",
        description = "Medine ve Kudüs revaklarını çağrıştıran zeytin yeşili buzlu cam yüzeyler, mihrap kemerli butonlar ve avlu çerçeveleri.",
        category = "Cam (Glass)",
        featureTag = "Buzlu zeytin cam • Mihrap buton",
        designMovement = "Kudüs ve Medine Kristal Revak",
        pattern = ThemePattern.OLIVE_DAWN,
        shapes = ThemeShapes(
            buttonShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
            cardShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            badgeShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        ),
        metrics = ThemeMetrics(
            cardPadding = 16.dp,
            screenGutter = 20.dp,
            cardGap = 12.dp,
            cardRadiusTop = 32.dp,
            cardRadiusBottom = 8.dp,
            heroCorner = 32.dp
        ),
        typography = ThemeTypographySpec(
            family = TypeFamily.DEFAULT,
            displayWeight = FontWeight.Bold,
            displaySize = 29,
            titleWeight = FontWeight.SemiBold
        ),
        motion = ThemeMotionSpec(loop = LoopStyle.BREATHE, loopPeriodMillis = 6500L),
        cardTreatment = CardTreatment.GLASS,
        dividerStyle = DividerStyle.HAIRLINE,
        isGlass = true,
        customBorder = BorderStroke(
            1.7.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFFC5E1A5).copy(alpha = 0.8f),
                    Color.White.copy(alpha = 0.7f),
                    Color(0xFFC9A227).copy(alpha = 0.85f)
                )
            )
        ),
        lightColors = ThemeColors(
            primary = Color(0xFF334F24), onPrimary = Color.White,
            primaryContainer = Color(0xFFE0EED8), onPrimaryContainer = Color(0xFF1D3012),
            secondary = Color(0xFFC49B25), onSecondary = Color(0xFF212E19),
            secondaryContainer = Color(0xFFFBF2D4), onSecondaryContainer = Color(0xFF523E04),
            tertiary = Color(0xFF4E7539), onTertiary = Color.White,
            background = Color(0xFFEEF4E9), onBackground = Color(0xFF1B2B13),
            surface = Color(0xFFFAFDF8), onSurface = Color(0xFF1B2B13),
            surfaceVariant = Color(0xFFE0ECD8), onSurfaceVariant = Color(0xFF364D2B),
            outline = Color(0xFF8E721A),
            cardGradient = listOf(Color(0xFF3B5C2A), Color(0xFF1A2B11)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFD4AF37), goldMotifColor = Color(0xFFC59B27)
        ),
        darkColors = ThemeColors(
            primary = Color(0xFFE2C46C), onPrimary = Color(0xFF192810),
            primaryContainer = Color(0xFF2E4A20), onPrimaryContainer = Color(0xFFF0E6C2),
            secondary = Color(0xFFB8E29E), onSecondary = Color(0xFF192810),
            secondaryContainer = Color(0xFF223817), onSecondaryContainer = Color(0xFFB8E29E),
            tertiary = Color(0xFF9CCC65), onTertiary = Color(0xFF101B0A),
            background = Color(0xFF0D1708), onBackground = Color(0xFFEBF4E6),
            surface = Color(0xFF172610), onSurface = Color(0xFFEBF4E6),
            surfaceVariant = Color(0xFF23381A), onSurfaceVariant = Color(0xFFC9DEC0),
            outline = Color(0xFFE2C46C),
            cardGradient = listOf(Color(0xFF2F4D21), Color(0xFF121F0C)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFE2C46C), goldMotifColor = Color(0xFFF3C64F)
        ),
        borderGold = Color(0xFFC9A227)
    ),

    // 7. KUBBELİ SAHRA — Horseshoe Arch Glass & Desert Wave Frames
    kubbeliTheme(
        id = "KUBBELI_SAHRA",
        turkishName = "Kubbeli Sahra",
        englishName = "Sahara Dome",
        description = "Hicaz kumtaşı ve Endülüs nal kemer kesimli sıcak buzlu cam paneller, kumul kıvrımlı butonlar ve altın halesi.",
        category = "Cam (Glass)",
        featureTag = "Kumtaşı buzlu cam • Nal kemer",
        designMovement = "Hicaz ve Endülüs Cam Mimarisi",
        pattern = ThemePattern.SAND_DRIFT,
        shapes = ThemeShapes(
            buttonShape = RoundedCornerShape(topStart = 26.dp, topEnd = 10.dp, bottomStart = 26.dp, bottomEnd = 10.dp),
            cardShape = RoundedCornerShape(topStart = 36.dp, topEnd = 16.dp, bottomStart = 28.dp, bottomEnd = 16.dp),
            badgeShape = RoundedCornerShape(14.dp)
        ),
        metrics = ThemeMetrics(
            cardPadding = 16.dp,
            screenGutter = 20.dp,
            cardGap = 12.dp,
            cardRadiusTop = 36.dp,
            cardRadiusBottom = 16.dp,
            heroCorner = 32.dp
        ),
        typography = ThemeTypographySpec(
            family = TypeFamily.SERIF,
            displayWeight = FontWeight.Bold,
            displaySize = 30,
            italicQuotes = true
        ),
        motion = ThemeMotionSpec(loop = LoopStyle.DRIFT, loopPeriodMillis = 7500L),
        cardTreatment = CardTreatment.GLASS,
        dividerStyle = DividerStyle.DASHED,
        isGlass = true,
        customBorder = BorderStroke(
            1.8.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFFFFE0B2).copy(alpha = 0.85f),
                    Color(0xFFD4AF37).copy(alpha = 0.8f),
                    Color.White.copy(alpha = 0.65f)
                )
            )
        ),
        lightColors = ThemeColors(
            primary = Color(0xFF5C4028), onPrimary = Color.White,
            primaryContainer = Color(0xFFF2E6DA), onPrimaryContainer = Color(0xFF382414),
            secondary = Color(0xFFC69C28), onSecondary = Color(0xFF2E2013),
            secondaryContainer = Color(0xFFFBF1D4), onSecondaryContainer = Color(0xFF533F04),
            tertiary = Color(0xFF87603E), onTertiary = Color.White,
            background = Color(0xFFF6EFE6), onBackground = Color(0xFF2B1D11),
            surface = Color(0xFFFFFBF6), onSurface = Color(0xFF2B1D11),
            surfaceVariant = Color(0xFFEDE2D3), onSurfaceVariant = Color(0xFF523C29),
            outline = Color(0xFF8F701E),
            cardGradient = listOf(Color(0xFF6B4A2E), Color(0xFF2E1E11)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFD4AF37), goldMotifColor = Color(0xFFC59B27)
        ),
        darkColors = ThemeColors(
            primary = Color(0xFFE5C56E), onPrimary = Color(0xFF291B0E),
            primaryContainer = Color(0xFF4D3520), onPrimaryContainer = Color(0xFFF3E7C4),
            secondary = Color(0xFFE8C7A7), onSecondary = Color(0xFF291B0E),
            secondaryContainer = Color(0xFF3B2818), onSecondaryContainer = Color(0xFFE8C7A7),
            tertiary = Color(0xFFD7A97C), onTertiary = Color(0xFF1A1108),
            background = Color(0xFF160E07), onBackground = Color(0xFFF6EFE8),
            surface = Color(0xFF24180D), onSurface = Color(0xFFF6EFE8),
            surfaceVariant = Color(0xFF362515), onSurfaceVariant = Color(0xFFDFCFC0),
            outline = Color(0xFFE5C56E),
            cardGradient = listOf(Color(0xFF4F3620), Color(0xFF1F140B)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFE5C56E), goldMotifColor = Color(0xFFF3C64F)
        ),
        borderGold = Color(0xFFD4AF37)
    ),

    // 8. KUBBELİ SEDEF — Mother-of-Pearl Jewel-Facet Cut Glass
    kubbeliTheme(
        id = "KUBBELI_SEDEF",
        turkishName = "Kubbeli Sedef",
        englishName = "Mother of Pearl Dome",
        description = "Topkapı sedef kakmalı prizmatik buzlu cam yüzeyler, mücevher faset kesimli butonlar ve kündekâri çerçeveler.",
        category = "Cam (Glass)",
        featureTag = "Sedef prizma cam • Faset kesim",
        designMovement = "Osmanlı Sedefkâr Kristal",
        pattern = ThemePattern.PASTEL_KNOT,
        shapes = ThemeShapes(
            buttonShape = CutCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
            cardShape = CutCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            badgeShape = CutCornerShape(topStart = 4.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 4.dp)
        ),
        metrics = ThemeMetrics(
            cardPadding = 16.dp,
            screenGutter = 20.dp,
            cardGap = 12.dp,
            cardRadiusTop = 18.dp,
            cardRadiusBottom = 18.dp,
            heroCorner = 24.dp
        ),
        typography = ThemeTypographySpec(
            family = TypeFamily.GEOMETRIC,
            displayWeight = FontWeight.Bold,
            displaySize = 30,
            numericTabular = true
        ),
        motion = ThemeMotionSpec(loop = LoopStyle.SHIMMER, loopPeriodMillis = 5200L),
        cardTreatment = CardTreatment.GLASS,
        dividerStyle = DividerStyle.DOUBLE,
        isGlass = true,
        customBorder = BorderStroke(
            1.9.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.9f),
                    Color(0xFFB2EBF2).copy(alpha = 0.8f),
                    Color(0xFFD4AF37).copy(alpha = 0.85f)
                )
            )
        ),
        lightColors = ThemeColors(
            primary = Color(0xFF1F4E5B), onPrimary = Color.White,
            primaryContainer = Color(0xFFDCEEF2), onPrimaryContainer = Color(0xFF0E2F38),
            secondary = Color(0xFFC49A27), onSecondary = Color(0xFF162D33),
            secondaryContainer = Color(0xFFFBF1D5), onSecondaryContainer = Color(0xFF523E04),
            tertiary = Color(0xFF387484), onTertiary = Color.White,
            background = Color(0xFFEDF5F7), onBackground = Color(0xFF112930),
            surface = Color(0xFFF9FDFE), onSurface = Color(0xFF112930),
            surfaceVariant = Color(0xFFDCECEF), onSurfaceVariant = Color(0xFF284852),
            outline = Color(0xFF8F721D),
            cardGradient = listOf(Color(0xFF245B6B), Color(0xFF0E2830)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFD4AF37), goldMotifColor = Color(0xFFC59B27)
        ),
        darkColors = ThemeColors(
            primary = Color(0xFFE2C46C), onPrimary = Color(0xFF0D242B),
            primaryContainer = Color(0xFF1B4552), onPrimaryContainer = Color(0xFFF0E6C2),
            secondary = Color(0xFFA3DAE8), onSecondary = Color(0xFF0D242B),
            secondaryContainer = Color(0xFF14343E), onSecondaryContainer = Color(0xFFA3DAE8),
            tertiary = Color(0xFF68B7CC), onTertiary = Color(0xFF08181C),
            background = Color(0xFF07161B), onBackground = Color(0xFFE8F3F6),
            surface = Color(0xFF0F252D), onSurface = Color(0xFFE8F3F6),
            surfaceVariant = Color(0xFF183640), onSurfaceVariant = Color(0xFFC4DCE2),
            outline = Color(0xFFE2C46C),
            cardGradient = listOf(Color(0xFF1B4754), Color(0xFF0B1E24)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFE2C46C), goldMotifColor = Color(0xFFF3C64F)
        ),
        borderGold = Color(0xFFD4AF37)
    ),

    // 9. KUBBELİ KÂBE GECESİ — Smoked Obsidian Glass & Metallic Hizam Bevel Frames
    kubbeliTheme(
        id = "KUBBELI_KABE_GECESI",
        turkishName = "Kubbeli Kâbe Gecesi",
        englishName = "Kaaba Night Dome",
        description = "Kisve-i Şerif füme kristal cam yüzeyler, altın Hizam pahlı dikdörtgen butonlar ve vakur sırma çerçeveler.",
        category = "Cam (Glass)",
        featureTag = "Füme kristal cam • Altın Hizam",
        designMovement = "Mekke Kisve-i Şerif Kristal",
        pattern = ThemePattern.KAABA_LINES,
        prefersDark = true,
        shapes = ThemeShapes(
            buttonShape = CutCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 3.dp, bottomEnd = 3.dp),
            cardShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            badgeShape = CutCornerShape(4.dp)
        ),
        metrics = ThemeMetrics(
            cardPadding = 17.dp,
            screenGutter = 20.dp,
            cardGap = 12.dp,
            cardRadiusTop = 20.dp,
            cardRadiusBottom = 4.dp,
            heroCorner = 24.dp
        ),
        typography = ThemeTypographySpec(
            family = TypeFamily.SERIF,
            displayWeight = FontWeight.ExtraBold,
            displaySize = 31,
            titleWeight = FontWeight.Bold,
            labelTracking = 0.18
        ),
        motion = ThemeMotionSpec(loop = LoopStyle.TWINKLE, loopPeriodMillis = 4800L),
        cardTreatment = CardTreatment.GLASS,
        dividerStyle = DividerStyle.DOUBLE,
        isGlass = true,
        customBorder = BorderStroke(
            2.2.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFFFFE082),
                    Color(0xFFD4AF37),
                    Color.White.copy(alpha = 0.7f),
                    Color(0xFFE5BE46)
                )
            )
        ),
        lightColors = ThemeColors(
            primary = Color(0xFF211E1A), onPrimary = Color.White,
            primaryContainer = Color(0xFFEBE5DC), onPrimaryContainer = Color(0xFF171411),
            secondary = Color(0xFFC9A227), onSecondary = Color(0xFF1C1914),
            secondaryContainer = Color(0xFFFBF2D4), onSecondaryContainer = Color(0xFF564204),
            tertiary = Color(0xFF6B5839), onTertiary = Color.White,
            background = Color(0xFFF5F1EB), onBackground = Color(0xFF171512),
            surface = Color(0xFFFFFCF7), onSurface = Color(0xFF171512),
            surfaceVariant = Color(0xFFE8E2D8), onSurfaceVariant = Color(0xFF3E3931),
            outline = Color(0xFF94761B),
            cardGradient = listOf(Color(0xFF2C2721), Color(0xFF12100E)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFD4AF37), goldMotifColor = Color(0xFFC59B27)
        ),
        darkColors = ThemeColors(
            primary = Color(0xFFE8C86B), onPrimary = Color(0xFF1A1508),
            primaryContainer = Color(0xFF332B18), onPrimaryContainer = Color(0xFFF6E9C4),
            secondary = Color(0xFFD4AF37), onSecondary = Color(0xFF1A1508),
            secondaryContainer = Color(0xFF282214), onSecondaryContainer = Color(0xFFF2DC99),
            tertiary = Color(0xFFC5A059), onTertiary = Color(0xFF12100C),
            background = Color(0xFF08080A), onBackground = Color(0xFFF5F1E8),
            surface = Color(0xFF141316), onSurface = Color(0xFFF5F1E8),
            surfaceVariant = Color(0xFF211F24), onSurfaceVariant = Color(0xFFD8D0C2),
            outline = Color(0xFFE8C86B),
            cardGradient = listOf(Color(0xFF282319), Color(0xFF0C0B09)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFE8C86B), goldMotifColor = Color(0xFFF3C64F)
        ),
        borderGold = Color(0xFFE5BE46)
    ),

    // 10. KUBBELİ GECE MAVİSİ — Astrolabe Lens Glass & Celestial Orbit Frames
    kubbeliTheme(
        id = "KUBBELI_GECE_MAVISI",
        turkishName = "Kubbeli Gece Mavisi",
        englishName = "Midnight Lapis Dome",
        description = "Uluğ Bey Rasathanesi mercek kesimli gece mavisi buzlu cam yüzeyler, yörünge kavisli butonlar ve yıldız haritası.",
        category = "Cam (Glass)",
        featureTag = "Mercek buzlu cam • Usturlap kavis",
        designMovement = "Uluğ Bey Rasathanesi Kristal Mercek",
        pattern = ThemePattern.STARFIELD,
        prefersDark = true,
        shapes = ThemeShapes(
            buttonShape = RoundedCornerShape(topStart = 10.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 10.dp),
            cardShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 22.dp, bottomEnd = 22.dp),
            badgeShape = RoundedCornerShape(50)
        ),
        metrics = ThemeMetrics(
            cardPadding = 16.dp,
            screenGutter = 20.dp,
            cardGap = 12.dp,
            cardRadiusTop = 40.dp,
            cardRadiusBottom = 22.dp,
            heroCorner = 36.dp
        ),
        typography = ThemeTypographySpec(
            family = TypeFamily.GEOMETRIC,
            displayWeight = FontWeight.Bold,
            displaySize = 30,
            numericTabular = true,
            labelTracking = 0.15
        ),
        motion = ThemeMotionSpec(loop = LoopStyle.TWINKLE, loopPeriodMillis = 4200L),
        cardTreatment = CardTreatment.GLASS,
        dividerStyle = DividerStyle.GRADIENT,
        isGlass = true,
        customBorder = BorderStroke(
            1.8.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFF90CAF9).copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.75f),
                    Color(0xFFE5C66E).copy(alpha = 0.85f)
                )
            )
        ),
        lightColors = ThemeColors(
            primary = Color(0xFF17274E), onPrimary = Color.White,
            primaryContainer = Color(0xFFDCE2F5), onPrimaryContainer = Color(0xFF0D1733),
            secondary = Color(0xFFC9A227), onSecondary = Color(0xFF141E38),
            secondaryContainer = Color(0xFFFBF2D4), onSecondaryContainer = Color(0xFF564204),
            tertiary = Color(0xFF39528C), onTertiary = Color.White,
            background = Color(0xFFECF0FA), onBackground = Color(0xFF101B36),
            surface = Color(0xFFF9FBFF), onSurface = Color(0xFF101B36),
            surfaceVariant = Color(0xFFDCE3F5), onSurfaceVariant = Color(0xFF28375C),
            outline = Color(0xFF94751B),
            cardGradient = listOf(Color(0xFF1C3163), Color(0xFF0B1329)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFD4AF37), goldMotifColor = Color(0xFFC59B27)
        ),
        darkColors = ThemeColors(
            primary = Color(0xFFE5C66E), onPrimary = Color(0xFF0B142B),
            primaryContainer = Color(0xFF1C2E59), onPrimaryContainer = Color(0xFFF2E7C4),
            secondary = Color(0xFFA8C2F7), onSecondary = Color(0xFF0B142B),
            secondaryContainer = Color(0xFF152344), onSecondaryContainer = Color(0xFFA8C2F7),
            tertiary = Color(0xFF799CE8), onTertiary = Color(0xFF070D1C),
            background = Color(0xFF050A17), onBackground = Color(0xFFE8EEFA),
            surface = Color(0xFF0D162E), onSurface = Color(0xFFE8EEFA),
            surfaceVariant = Color(0xFF162447), onSurfaceVariant = Color(0xFFC5D1EC),
            outline = Color(0xFFE5C66E),
            cardGradient = listOf(Color(0xFF1B2E5E), Color(0xFF091024)),
            buttonTextColor = Color(0xFFFFECB3), glowColor = Color(0xFFE5C66E), goldMotifColor = Color(0xFFF3C64F)
        ),
        borderGold = Color(0xFFE5C66E)
    )
)

private fun kubbeliTheme(
    id: String,
    turkishName: String,
    englishName: String,
    description: String,
    category: String,
    featureTag: String,
    designMovement: String = "Klasik İslami mimari",
    pattern: ThemePattern,
    shapes: ThemeShapes = ThemeShapes(
        buttonShape = RoundedCornerShape(20.dp),
        cardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
    ),
    metrics: ThemeMetrics = ThemeMetrics(),
    typography: ThemeTypographySpec = ThemeTypographySpec(),
    motion: ThemeMotionSpec = ThemeMotionSpec(),
    cardTreatment: CardTreatment = CardTreatment.GLASS,
    dividerStyle: DividerStyle = DividerStyle.HAIRLINE,
    isGlass: Boolean = true,
    hasHardShadow: Boolean = false,
    customBorder: BorderStroke? = null,
    lightColors: ThemeColors,
    darkColors: ThemeColors,
    borderGold: Color,
    prefersDark: Boolean = false
): AppThemeSpec = AppThemeSpec(
    id = id,
    turkishName = turkishName,
    englishName = englishName,
    description = description,
    category = category,
    featureTag = featureTag,
    designMovement = designMovement,
    lightColors = lightColors,
    darkColors = darkColors,
    shapes = shapes,
    metrics = metrics,
    typography = typography,
    motion = motion,
    pattern = pattern,
    cardTreatment = cardTreatment,
    dividerStyle = dividerStyle,
    isGlass = isGlass,
    hasHardShadow = hasHardShadow,
    buttonBorder = customBorder ?: BorderStroke(1.6.dp, borderGold),
    prefersDark = prefersDark,
    layout = ThemeLayout.POSTER,
    navStyle = NavStyle.PILL,
    actionPlacement = ActionPlacement.INLINE_TILES,
    heroScale = HeroScale.DOMINANT,
    composition = PrayerComposition.DOME,
    scheduleStyle = ScheduleStyle.CLASSIC,
    headerStyle = HomeHeaderStyle.CLASSIC
)
