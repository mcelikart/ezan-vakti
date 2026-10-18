package com.example.ui.theme

import com.example.ui.parseImportedNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test fun collectionHasTenNewKubbeliThemesAndPreservedEmeraldDome() {
        assertEquals(11, AppTheme.entries.size)
        assertEquals(setOf(PrayerComposition.DOME), AppTheme.entries.map { it.spec.composition }.toSet())
        assertEquals(setOf(ScheduleStyle.CLASSIC), AppTheme.entries.map { it.spec.scheduleStyle }.toSet())
        assertEquals(setOf(HomeHeaderStyle.CLASSIC), AppTheme.entries.map { it.spec.headerStyle }.toSet())
        assertEquals(setOf(NavStyle.PILL), AppTheme.entries.map { it.spec.navStyle }.toSet())
        assertEquals(setOf(HeroScale.DOMINANT), AppTheme.entries.map { it.spec.heroScale }.toSet())

        listOf(
            "SELCUKLU", "IZNIK_PORCELAIN", "HATTAT_PARCHMENT", "ENDERUN_LIBRARY",
            "HAGIA_SOPHIA", "TOPKAPI_GOLD", "MIMAR_SINAN", "KUDUS_AQSA", "SEMAZEN_KONYA"
        ).forEach {
            assertEquals(AppTheme.DEFAULT, AppTheme.fromStored(it))
        }
    }

    @Test fun emeraldDomeIsUntouchedWhileOtherTenAreGlassmorphicWithDistinctShapes() {
        val emerald = AppTheme.CLASSIC_EMERALD.spec
        assertFalse(emerald.isGlass)
        assertEquals(CardTreatment.OUTLINED, emerald.cardTreatment)

        val glassThemes = AppTheme.entries.drop(1)
        assertEquals(10, glassThemes.size)
        glassThemes.forEach { theme ->
            assertTrue("${theme.name} must be glassmorphic", theme.spec.isGlass)
            assertEquals(CardTreatment.GLASS, theme.spec.cardTreatment)
            assertNotEquals(
                "${theme.name} cardShape should differ from CLASSIC_EMERALD",
                emerald.shapes.cardShape,
                theme.spec.shapes.cardShape
            )
            assertNotEquals(
                "${theme.name} buttonShape should differ from CLASSIC_EMERALD",
                emerald.shapes.buttonShape,
                theme.spec.shapes.buttonShape
            )
        }
    }

    @Test fun allThemesHaveFiveColorOptionsWithAccessibleContrast() {
        for (theme in AppTheme.entries) {
            val options = theme.spec.colorOptions
            assertEquals(5, options.size)
            for (opt in options) {
                for (dark in listOf(false, true)) {
                    val scheme = theme.spec.toColorScheme(dark, opt.index)
                    val ratio = contrast(scheme.onSurface, scheme.surface)
                    assertTrue("${theme.name} variant=${opt.index} dark=$dark surface contrast=$ratio", ratio >= 4.5)
                }
            }
        }
    }

    @Test fun importedNoteUsesFirstLineAsTitle() {
        val raw = "Cuma Hutbesi Notlarım\nSabır ve şükür üzerine ayetler\nİbrahim Suresi 7. Ayet"
        val (title, body) = parseImportedNote(raw)
        assertEquals("Cuma Hutbesi Notlarım", title)
        assertEquals("Sabır ve şükür üzerine ayetler\nİbrahim Suresi 7. Ayet", body)
    }

    @Test fun unknownThemeNamesFallBackToEmeraldDome() {
        assertEquals(AppTheme.CLASSIC_EMERALD, AppTheme.fromStored("EMERALD"))
        assertEquals(AppTheme.CLASSIC_EMERALD, AppTheme.fromStored("NIGHT_ORANGE"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromStored("OBSIDIAN_GOLD"))
        listOf(
            "APPLE_CUPERTINO", "SWISS_MONOCHROME", "AMOLED", "OCEAN",
            "SUNSET", "FOREST", "CLASSIC", "MIDNIGHT", "ROSE", "GOLD", null, ""
        ).forEach { assertEquals(AppTheme.CLASSIC_EMERALD, AppTheme.fromStored(it)) }
        assertEquals(ThemeRegistry.all.size, AppTheme.entries.size)
    }

    @Test fun emeraldDomeIsTheDefaultAndFirstEntry() {
        assertEquals(AppTheme.CLASSIC_EMERALD, AppTheme.DEFAULT)
        assertEquals(AppTheme.CLASSIC_EMERALD, AppTheme.entries.first())
    }

    @Test fun everyEnumEntryHasARegisteredSpec() {
        for (theme in AppTheme.entries) {
            assertEquals(
                "enum entry ${theme.name} has no registry spec",
                theme.specId,
                theme.spec.id
            )
        }
    }

    @Test fun everyThemeHasAUniquePalette() {
        val primaries = AppTheme.entries.map { it.spec.lightColors.primary }.toSet()
        assertEquals(AppTheme.entries.size, primaries.size)
    }

    @Test fun allThemesHaveReadableTextInBothModes() {
        for (theme in AppTheme.entries) for (dark in listOf(false, true)) {
            val c = theme.spec.toColorScheme(dark)
            val pairs = mapOf(
                "primary" to (c.onPrimary to c.primary),
                "secondary" to (c.onSecondary to c.secondary),
                "tertiary" to (c.onTertiary to c.tertiary),
                "primary container" to (c.onPrimaryContainer to c.primaryContainer),
                "secondary container" to (c.onSecondaryContainer to c.secondaryContainer),
                "tertiary container" to (c.onTertiaryContainer to c.tertiaryContainer),
                "surface" to (c.onSurface to c.surface),
                "surface variant" to (c.onSurfaceVariant to c.surfaceVariant),
                "background" to (c.onBackground to c.background)
            )
            pairs.forEach { (name, pair) ->
                val ratio = contrast(pair.first, pair.second)
                assertTrue("${theme.name} dark=$dark $name contrast=$ratio", ratio >= 4.5)
            }
        }
    }

    @Test fun controlOutlinesMeetUiComponentContrast() {
        for (theme in AppTheme.entries) {
            if (theme.spec.emphasizeContent) continue
            val c = theme.spec.colors(theme.spec.prefersDark)
            val ratio = contrast(c.outline, c.surface)
            assertTrue("${theme.name} outline contrast=$ratio", ratio >= 3.0)
        }
    }
}
