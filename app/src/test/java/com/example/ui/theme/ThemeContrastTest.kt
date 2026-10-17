package com.example.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test fun collectionHasTwentyNewThemesAndPreservedDome() {
        assertEquals(21, AppTheme.entries.size)
        assertEquals(setOf(PrayerComposition.DOME), AppTheme.entries.map { it.spec.composition }.toSet())
        assertEquals(ScheduleStyle.entries.toSet(), AppTheme.entries.map { it.spec.scheduleStyle }.toSet())
        listOf("VAKIT_ATLASI", "HAT_DEFTERI", "UFUK_CIZGISI", "ISTASYON", "KENT_PANOSU",
            "AY_ISIGI", "TIPOGRAFI", "VAKIT_SERIDI", "GUN_YAPRAGI", "SESSIZ_ODAK").forEach {
            assertEquals(AppTheme.DEFAULT, AppTheme.fromStored(it))
        }
        listOf("OTTOMAN_HUMAYUN", "HICRI_MINIMAL", "OLIVE_DAWN", "PLAIN_VOICE").forEach {
            assertEquals(AppTheme.DEFAULT, AppTheme.fromStored(it))
        }
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

    @Test fun everyThemeIsStructurallyDistinct() {
        // Identity must not collapse to a hue swap. Pattern, shape family, button
        // fill, card treatment, motion, type family, layout, nav style, where the
        // global actions live, and hero prominence must all count.
        val signatures = AppTheme.entries.map { theme ->
            val s = theme.spec
            listOf(
                s.composition.name,
                s.pattern.name,
                s.shapes.buttonShape::class.simpleName,
                s.shapes.cardShape.toString(),
                s.buttonFill.name,
                s.cardTreatment.name,
                s.motion.entrance.name,
                s.typography.family.name,
                s.layout.name,
                s.navStyle.name,
                s.actionPlacement.name,
                s.heroScale.name
            ).joinToString("|")
        }
        assertEquals(
            "two themes share an identical structural signature",
            AppTheme.entries.size,
            signatures.toSet().size
        )
    }

    @Test fun everyLayoutFamilyNavStylePlacementAndHeroScaleIsUsed() {
        // If one of these axes is never assigned, that design idea lives only in
        // the enum and never reaches the product.
        assertEquals(
            ThemeLayout.entries.toSet(),
            AppTheme.entries.map { it.spec.layout }.toSet()
        )
        assertEquals(
            NavStyle.entries.toSet(),
            AppTheme.entries.map { it.spec.navStyle }.toSet()
        )
        assertEquals(
            ActionPlacement.entries.toSet(),
            AppTheme.entries.map { it.spec.actionPlacement }.toSet()
        )
        assertEquals(
            setOf(HeroScale.DOMINANT),
            AppTheme.entries.map { it.spec.heroScale }.toSet()
        )
    }

    @Test fun actionPlacementsAreSpreadNotClusteredOnOneMode() {
        // A single dominant placement would mean most themes still feel the same.
        val counts = AppTheme.entries.groupingBy { it.spec.actionPlacement }.eachCount()
        assertTrue("no placement is used by more than half the themes: $counts",
            counts.values.max() <= AppTheme.entries.size / 2)
    }

    @Test fun everyThemeHasAUniquePalette() {
        val primaries = AppTheme.entries.map { it.spec.lightColors.primary }.toSet()
        assertEquals(AppTheme.entries.size, primaries.size)
    }

    @Test fun collectionBalancesLightAndDark() {
        assertEquals(3, AppTheme.entries.count { it.spec.prefersDark })
        assertTrue(!AppTheme.CLASSIC_EMERALD.spec.prefersDark)
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
            // Content-first themes use `outline` as a decorative hairline rather
            // than a control border, so the 3:1 gate does not apply to them.
            if (theme.spec.emphasizeContent) continue
            val c = theme.spec.colors(theme.spec.prefersDark)
            val ratio = contrast(c.outline, c.surface)
            assertTrue("${theme.name} outline contrast=$ratio", ratio >= 3.0)
        }
    }
}
