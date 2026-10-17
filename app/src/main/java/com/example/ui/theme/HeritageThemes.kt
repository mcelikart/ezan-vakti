package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ScheduleStyle { CLASSIC, ARCADES, TILES, LINES, TIMELINE, PAIRED, RIBBON, COMPACT, ALMANAC, MEDALLIONS, PANELS }
enum class HomeHeaderStyle { CLASSIC, MIHRAB, MASTHEAD, SIDEBAR, RULE, COMPACT }

/** The sun dome is shared. These tokens compose the surrounding architecture. */
internal fun heritageThemes(): List<AppThemeSpec> = listOf(
    heritage("SELCUKLU", "Selçuklu Taşı", "Kesme taş ritmi, sekizgen detaylar ve sıralı vakitler.",
        0xFF31584C, 0xFF98692E, 0xFF734E65, ThemePattern.ZELLIGE,
        ScheduleStyle.ARCADES, HomeHeaderStyle.MIHRAB, NavStyle.PILL, ActionPlacement.INLINE_TILES, cut = true),
    heritage("IZNIK", "İznik Çinisi", "Beyaz porselen, çini bordürleri ve simetrik vakit panoları.",
        0xFF175C91, 0xFFAA3C4D, 0xFF287E77, ThemePattern.AZULEJO,
        ScheduleStyle.TILES, HomeHeaderStyle.MASTHEAD, NavStyle.TABS, ActionPlacement.HEADER_CHIPS),
    heritage("REVAK", "Revak", "İnce sütunlar, açık avlular ve düşey vakit akışı.",
        0xFF52644A, 0xFF906432, 0xFF3D7187, ThemePattern.HAIRLINE,
        ScheduleStyle.TIMELINE, HomeHeaderStyle.SIDEBAR, NavStyle.RAIL_DOCK, ActionPlacement.HEADER_ACTION_SHEET),
    heritage("SEDEF", "Sedef Kakma", "Sedef beyazı, erik moru ve dengeli ikili vakitler.",
        0xFF69465E, 0xFF3B7B78, 0xFF9D712B, ThemePattern.PASTEL_KNOT,
        ScheduleStyle.PAIRED, HomeHeaderStyle.RULE, NavStyle.UNDERLINE, ActionPlacement.NAV_DESTINATION),
    heritage("LALEZAR", "Lâlezar", "Lale kırmızısı, ince dallar ve madalyon vakitler.",
        0xFFA63248, 0xFF377365, 0xFF8F6F22, ThemePattern.EBRE,
        ScheduleStyle.MEDALLIONS, HomeHeaderStyle.MASTHEAD, NavStyle.PILL, ActionPlacement.FOOTER_TILES, serif = true),
    heritage("HUNKAR", "Hünkâr", "Koyu kömür üzerine altın hatlar ve geniş vakit panoları.",
        0xFF80601C, 0xFF4A7D76, 0xFF92525D, ThemePattern.TUGHRA_BAND,
        ScheduleStyle.PANELS, HomeHeaderStyle.MIHRAB, NavStyle.RAIL_DOCK, ActionPlacement.INLINE_TILES, dark = true, serif = true),
    heritage("FIRUZE", "Firuze", "Turkuaz sır, keskin köşeler ve kesintisiz bir vakit şeridi.",
        0xFF057D80, 0xFFB44546, 0xFF767132, ThemePattern.ZELLIGE,
        ScheduleStyle.RIBBON, HomeHeaderStyle.COMPACT, NavStyle.TABS, ActionPlacement.HEADER_CHIPS, cut = true),
    heritage("YESIL_AVLU", "Yeşil Avlu", "Yeşil ve beyaz dengesi, açık satırlar ve ferah aralıklar.",
        0xFF28764E, 0xFF996137, 0xFF4F6D95, ThemePattern.OLIVE_DAWN,
        ScheduleStyle.LINES, HomeHeaderStyle.SIDEBAR, NavStyle.UNDERLINE, ActionPlacement.HEADER_ACTION_SHEET),
    heritage("GUL_BAHCESI", "Gül Bahçesi", "Gül kurusu, yaprak yeşili ve günlük takvim sayfası.",
        0xFF964E63, 0xFF52733D, 0xFF3D738C, ThemePattern.EBRE,
        ScheduleStyle.ALMANAC, HomeHeaderStyle.RULE, NavStyle.PILL, ActionPlacement.FOOTER_TILES, serif = true),
    heritage("SAHIL", "Sahil Mescidi", "Açık deniz tonları, zarif yatay çizgiler ve sade bir çizelge.",
        0xFF286F85, 0xFFA34F3E, 0xFF658031, ThemePattern.COASTAL_PATH,
        ScheduleStyle.COMPACT, HomeHeaderStyle.MASTHEAD, NavStyle.TABS, ActionPlacement.NAV_DESTINATION),
    heritage("KAMER", "Kamer", "Grafit gece, gümüş detaylar ve kemerli vakitler.",
        0xFF58687B, 0xFF477E6D, 0xFFA46E3C, ThemePattern.STARFIELD,
        ScheduleStyle.ARCADES, HomeHeaderStyle.RULE, NavStyle.UNDERLINE, ActionPlacement.HEADER_ACTION_SHEET, dark = true),
    heritage("DIVAN", "Divan", "Bordo mürekkep, kitap başlıkları ve okunaklı vakit sütunları.",
        0xFF7E3345, 0xFF297275, 0xFF957B32, ThemePattern.PAPER_ATELIER,
        ScheduleStyle.LINES, HomeHeaderStyle.MASTHEAD, NavStyle.TABS, ActionPlacement.FOOTER_TILES, serif = true),
    heritage("SELSEBIL", "Selsebil", "Su yeşili, hafif konturlar ve birbirini izleyen vakitler.",
        0xFF2D7064, 0xFF82608E, 0xFFAA6437, ThemePattern.WHIRL,
        ScheduleStyle.TIMELINE, HomeHeaderStyle.MIHRAB, NavStyle.PILL, ActionPlacement.INLINE_TILES),
    heritage("MEDRESE", "Medrese", "Mürekkep ve taş, numaralı kayıtlar ve güçlü bölüm ayrımları.",
        0xFF3B566A, 0xFF8E563C, 0xFF638044, ThemePattern.QUIET_GRID,
        ScheduleStyle.COMPACT, HomeHeaderStyle.SIDEBAR, NavStyle.RAIL_DOCK, ActionPlacement.HEADER_CHIPS, cut = true),
    heritage("NAKKAS", "Nakkaş", "Mürdüm ve firuze, geometrik nakışlar ve çerçeveli vakitler.",
        0xFF754875, 0xFF267C82, 0xFFA67626, ThemePattern.ARABESQUE,
        ScheduleStyle.TILES, HomeHeaderStyle.MIHRAB, NavStyle.PILL, ActionPlacement.NAV_DESTINATION, serif = true),
    heritage("MERCAN", "Mercan", "Kırmızı mercan, soğuk yeşil ve geniş ikili bloklar.",
        0xFFB4473D, 0xFF337D78, 0xFF646A8F, ThemePattern.HAIRLINE,
        ScheduleStyle.PAIRED, HomeHeaderStyle.COMPACT, NavStyle.RAIL_DOCK, ActionPlacement.INLINE_TILES, cut = true),
    heritage("GECE_CINISI", "Gece Çinisi", "Antrasit zeminde petrol ve mercan, kesintisiz vakit kuşağı.",
        0xFF126577, 0xFFA15351, 0xFF9D8840, ThemePattern.AZULEJO,
        ScheduleStyle.RIBBON, HomeHeaderStyle.SIDEBAR, NavStyle.UNDERLINE, ActionPlacement.HEADER_ACTION_SHEET, dark = true),
    heritage("AK_MERMER", "Ak Mermer", "Mermer beyazı, siyah konturlar ve düzenli bir gün defteri.",
        0xFF333D41, 0xFF507663, 0xFFA16348, ThemePattern.KAABA_LINES,
        ScheduleStyle.ALMANAC, HomeHeaderStyle.COMPACT, NavStyle.TABS, ActionPlacement.HEADER_CHIPS, serif = true),
    heritage("SERVI", "Servi", "Servi yeşili, ince altın işaretler ve düşey panolar.",
        0xFF225747, 0xFF9D733C, 0xFF795979, ThemePattern.OLIVE_DAWN,
        ScheduleStyle.PANELS, HomeHeaderStyle.RULE, NavStyle.RAIL_DOCK, ActionPlacement.FOOTER_TILES, serif = true),
    heritage("SAFAK", "Şafak", "Beyaz sabah ışığı, kızıl vurgular ve hafif madalyonlar.",
        0xFFAC533B, 0xFF377783, 0xFF76618A, ThemePattern.CLEAN,
        ScheduleStyle.MEDALLIONS, HomeHeaderStyle.COMPACT, NavStyle.PILL, ActionPlacement.NAV_DESTINATION)
)

private fun heritage(
    id: String, name: String, description: String,
    primary: Long, secondary: Long, tertiary: Long, motif: ThemePattern,
    schedule: ScheduleStyle, header: HomeHeaderStyle, nav: NavStyle, actions: ActionPlacement,
    dark: Boolean = false, serif: Boolean = false, cut: Boolean = false
): AppThemeSpec {
    val a = Color(primary)
    val b = Color(secondary)
    val c = Color(tertiary)
    val paper = lerp(Color.White, b, .035f)
    val ink = Color(0xFF20292A)
    val shape = if (cut) CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
        else RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    return AppThemeSpec(
        id = id, turkishName = name, englishName = name, description = description,
        category = if (dark) "Gece" else "Aydınlık", featureTag = name, designMovement = description,
        lightColors = lightPalette(a, b, c, paper, Color.White, lerp(paper, a, .075f),
            ink, Color(0xFF526062), Color(0xFF758280)),
        darkColors = darkPalette(lerp(a, Color.White, .6f), lerp(b, Color.White, .55f),
            lerp(c, Color.White, .5f), Color(0xFF131817), Color(0xFF1D2523), Color(0xFF2B3432),
            Color(0xFFF1F4F2), Color(0xFFBCC9C4), Color(0xFF83968E)),
        shapes = ThemeShapes(if (cut) CutCornerShape(8.dp) else RoundedCornerShape(16.dp), shape),
        metrics = ThemeMetrics(screenGutter = if (schedule == ScheduleStyle.COMPACT) 16.dp else 20.dp,
            cardGap = if (serif) 20.dp else 12.dp, cardPadding = if (serif) 20.dp else 16.dp),
        typography = ThemeTypographySpec(family = if (serif) TypeFamily.SERIF else TypeFamily.DEFAULT,
            displaySize = 32, displayWeight = FontWeight.SemiBold, labelTracking = 0.0),
        motion = ThemeMotionSpec(durationMillis = 180, loop = LoopStyle.NONE),
        pattern = motif, buttonFill = if (cut) ButtonFill.OUTLINED else ButtonFill.TONAL,
        cardTreatment = if (header == HomeHeaderStyle.RULE) CardTreatment.FLAT else CardTreatment.OUTLINED,
        buttonBorder = BorderStroke(1.dp, if (dark) lerp(b, Color.White, .45f) else b.copy(alpha = .5f)),
        prefersDark = dark, scheduleStyle = schedule, headerStyle = header,
        navStyle = nav, actionPlacement = actions, heroScale = HeroScale.DOMINANT,
        layout = when (schedule) {
            ScheduleStyle.TILES, ScheduleStyle.PAIRED, ScheduleStyle.PANELS -> ThemeLayout.DASHBOARD
            ScheduleStyle.LINES, ScheduleStyle.COMPACT, ScheduleStyle.TIMELINE -> ThemeLayout.LEDGER
            ScheduleStyle.ALMANAC -> ThemeLayout.JOURNAL
            ScheduleStyle.MEDALLIONS -> ThemeLayout.CALM
            else -> ThemeLayout.POSTER
        },
        sections = if (serif) listOf(HomeSection.QUOTE, HomeSection.WEATHER, HomeSection.MIRACLE, HomeSection.DETAILS)
            else listOf(HomeSection.WEATHER, HomeSection.MIRACLE, HomeSection.DETAILS, HomeSection.QUOTE)
    )
}
