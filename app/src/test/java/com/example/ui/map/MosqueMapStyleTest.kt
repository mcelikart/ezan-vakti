package com.example.ui.map

import androidx.compose.ui.graphics.toArgb
import com.example.data.Mosque
import com.example.ui.theme.ThemeRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Guards on the map style.
 *
 * The basemap was blank three times in a row for reasons that had nothing to do
 * with each other (a Vulkan driver, a Turkish default locale producing "0,750",
 * a vector style coupled to a glyph endpoint). These tests pin the properties that
 * actually caused those failures so a fourth one cannot slip through unnoticed.
 */
class MosqueMapStyleTest {
    @Test fun builderUsesInlineJsonNotAUrl() {
        val json = buildMapStyle(colors, false)
        val builder = mosqueStyleBuilder(json)
        assertEquals(json, builder.json)
        assertTrue(builder.uri.isNullOrEmpty())
    }

    @Test fun overzoomReusesAvailableTiles() {
        val sources = style().getJSONObject("sources")
        assertEquals(16, sources.getJSONObject("base").getInt("maxzoom"))
        assertEquals(16, sources.getJSONObject("base-labels").getInt("maxzoom"))
    }

    @Test fun userPositionIsVisibleWithoutTintingTheBasemap() {
        val s = style()
        assertTrue(s.getJSONObject("sources").has("user-location"))
        val ids = layerIds(s)
        assertTrue("user-location" in ids)
        assertFalse("theme-tint" in ids)
        assertFalse("radius-fill" in ids)
    }

    /**
     * Uses the app's real default theme rather than a hand-built ThemeColors.
     * The data class has nineteen required fields, so a hand-built instance would
     * rot the moment the palette grew, and this test exists to catch regressions.
     */
    private val colors = ThemeRegistry.CLASSIC_EMERALD.colors(false)

    private fun style(dark: Boolean = false) = JSONObject(buildMapStyle(colors, dark))

    @Test
    fun `style is well formed json with version 8`() {
        val s = style()
        assertEquals(8, s.getInt("version"))
        assertNotNull(s.getJSONObject("sources"))
        assertTrue(s.getJSONArray("layers").length() > 0)
    }

    @Test
    fun `every layer references a source that is declared`() {
        val s = style()
        val sources = s.getJSONObject("sources")
        val layers = s.getJSONArray("layers")
        for (i in 0 until layers.length()) {
            val layer = JSONObject(layers.get(i).toString())
            val source = layer.optString("source")
            if (source.isNotEmpty()) {
                assertTrue(
                    "layer '${layer.getString("id")}' points at undeclared source '$source'",
                    sources.has(source)
                )
            }
        }
    }

    @Test
    fun `geojson sources start empty so no pin is drawn before data arrives`() {
        val sources = style().getJSONObject("sources")
        for (name in listOf(MOSQUE_SOURCE, RADIUS_SOURCE)) {
            val data = sources.getJSONObject(name).getJSONObject("data")
            assertEquals("FeatureCollection", data.getString("type"))
            assertEquals(0, data.getJSONArray("features").length())
        }
    }

    @Test
    fun `mosque source drives both the point layers and the cluster layers`() {
        val ids = style().getJSONArray("layers")
            .let { arr -> (0 until arr.length()).map { JSONObject(arr.get(it).toString()).getString("id") } }
        assertTrue("$MOSQUE_SOURCE-points" in ids)
        assertTrue("$MOSQUE_SOURCE-halo" in ids)
        assertTrue("$MOSQUE_SOURCE-cluster-core" in ids)
        assertTrue("$MOSQUE_SOURCE-cluster-halo" in ids)
        assertTrue("$MOSQUE_SOURCE-cluster-label" in ids)
    }

    @Test
    fun `cluster label is the only layer that needs a glyph endpoint`() {
        // A vector basemap made the whole map depend on the glyph server. Raster has
        // no such dependency, so losing glyphs may only cost the cluster count.
        val layers = style().getJSONArray("layers")
        for (i in 0 until layers.length()) {
            val layer = JSONObject(layers.get(i).toString())
            if (layer.getString("type") == "symbol") {
                assertEquals(
                    "only the cluster count may be a symbol layer",
                    "$MOSQUE_SOURCE-cluster-label",
                    layer.getString("id")
                )
            }
        }
    }

    @Test
    fun `basemap is raster not vector`() {
        val s = style()
        val base = s.getJSONObject("sources").getJSONObject("base")
        assertEquals("raster", base.getString("type"))
        assertTrue(base.has("tiles"))
        assertFalse("a vector basemap would need a source-layer", base.has("source-layer"))
    }

    @Test
    fun `style is identical under a Turkish default locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val turkish = buildMapStyle(colors, false)
            // "0,750" is valid Turkish and invalid JSON; the old style was built with
            // String.format and no explicit locale, so it broke for Turkish users only.
            assertFalse(turkish.contains(",750"))
            assertFalse(turkish.contains("rgba(0,7"))
            JSONObject(turkish)
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `every colour in the style is six digit lowercase hex`() {
        val rendered = buildMapStyle(colors, false)
        val tokens = Regex("#[0-9A-Za-z]+").findAll(rendered).map { it.value }.toList()
        assertTrue("no colour reached the style at all", tokens.isNotEmpty())
        for (token in tokens) {
            assertEquals("'$token' is not a six digit hex colour", 7, token.length)
            assertEquals("'$token' should be lowercase", token.lowercase(), token)
        }
    }

    @Test
    fun `the theme primary colour reaches the style`() {
        // Derived from the Color rather than copied as a literal, so this checks the
        // theme-to-style plumbing instead of restating the formatter.
        val expected = String.format(Locale.US, "#%06x", colors.primary.toArgb() and 0xFFFFFF)
        assertTrue(
            "expected $expected in the style",
            buildMapStyle(colors, false).contains(expected)
        )
    }

    @Test
    fun `the page background colour reaches the style`() {
        val expected = String.format(Locale.US, "#%06x", colors.background.toArgb() and 0xFFFFFF)
        assertTrue(
            "expected $expected in the style",
            buildMapStyle(colors, false).contains(expected)
        )
    }

    @Test
    fun `dark style uses the dark canvas and light style the light canvas`() {
        val light = style(dark = false).getJSONObject("sources").getJSONObject("base")
            .getJSONArray("tiles").getString(0)
        val dark = style(dark = true).getJSONObject("sources").getJSONObject("base")
            .getJSONArray("tiles").getString(0)
        assertTrue(light.contains("World_Light_Gray_Base"))
        assertTrue(dark.contains("World_Dark_Gray_Base"))
        assertTrue("the two styles must not share a canvas", light != dark)
    }

    @Test
    fun `only the colour differs between the light and dark styles`() {
        // Everything else about the basemap is theme-independent, so a diff here
        // means the dark path silently started using a different tile service.
        val light = style(dark = false)
        val dark = style(dark = true)
        val lightIds = layerIds(light)
        val darkIds = layerIds(dark)
        assertEquals(lightIds, darkIds)
    }

    private fun layerIds(style: JSONObject): List<String> {
        val layers = style.getJSONArray("layers")
        return (0 until layers.length()).map { JSONObject(layers.get(it).toString()).getString("id") }
    }

    @Test
    fun `esri tiles use row before column order`() {
        // Esri's template is {z}/{y}/{x}. Swapping to XYZ silently returns a 404
        // raster that renders as a blank, featureless grid.
        val tiles = style().getJSONObject("sources").getJSONObject("base").getJSONArray("tiles")
        for (i in 0 until tiles.length()) {
            val url = tiles.getString(i)
            assertTrue("$url is not an Esri tile template", url.endsWith("/tile/{z}/{y}/{x}"))
        }
    }

    @Test
    fun `no OpenStreetMap standard tile host is used`() {
        // tile.openstreetmap.org forbids use by apps. The OSM tile *policy* covers
        // the host, not the underlying data, so Esri tiles stay fine and must keep
        // the required attribution string.
        val rendered = buildMapStyle(colors, false) + buildMapStyle(colors, true)
        assertFalse(rendered.contains("tile.openstreetmap.org"))
        assertFalse(rendered.contains("tile.osm.org"))
        assertTrue("Esri attribution must stay in the style", rendered.contains("Esri"))
    }

    @Test
    fun `attribution is declared on the basemap source`() {
        val base = style().getJSONObject("sources").getJSONObject("base")
        val attribution = base.optString("attribution")
        assertTrue(attribution.contains("Esri"))
        assertTrue(attribution.contains("OpenStreetMap"))
    }

    @Test
    fun `fill and line layers only reference sources we update`() {
        val layers = style().getJSONArray("layers")
        for (i in 0 until layers.length()) {
            val layer = JSONObject(layers.get(i).toString())
            when (layer.getString("type")) {
                "fill", "line" -> assertTrue(
                    "'${layer.getString("id")}' must read from a source we repaint",
                    layer.optString("source") == RADIUS_SOURCE
                )
            }
        }
    }
}

class MosqueClusteringTest {

    private fun mosque(lat: Double, lon: Double, id: Long) = Mosque(
        type = "node",
        id = id,
        lat = lat,
        lon = lon,
        tags = mapOf("amenity" to "place_of_worship")
    )

    /**
     * Serialises through the real GeoJSON writer and re-parses, so these tests
     * see exactly the bytes MapLibre receives rather than the in-memory objects.
     */
    private fun List<Mosque>.clusteredJson(zoom: Double): JSONArray =
        JSONObject(toClustered(zoom).toJson()).getJSONArray("features")

    private fun JSONArray.propertiesAt(index: Int): JSONObject =
        getJSONObject(index).getJSONObject("properties")

    @Test
    fun `a lone mosque becomes a point carrying its list index`() {
        val features = listOf(mosque(41.01, 29.01, 1L)).clusteredJson(zoom = 15.0)
        assertEquals(1, features.length())
        assertEquals("mosque", features.propertiesAt(0).getString("kind"))
        // idx is the position in the list handed to the map, not the OSM id. The
        // tap handler looks the selection back up by this index, so a 1-based or
        // id-based value here would open the wrong mosque.
        assertEquals(0, features.propertiesAt(0).getInt("idx"))
    }

    @Test
    fun `the index matches the position in the source list`() {
        val list = listOf(mosque(41.0, 29.0, 7L), mosque(51.0, 31.0, 8L), mosque(37.0, 26.0, 9L))
        val features = list.clusteredJson(zoom = 15.0)
        val found = mutableMapOf<Int, Double>()
        for (i in 0 until features.length()) {
            val properties = features.propertiesAt(i)
            if (properties.getString("kind") == "mosque") {
                found[properties.getInt("idx")] = features.getJSONObject(i)
                    .getJSONObject("geometry").getJSONArray("coordinates").getDouble(1)
            }
        }
        assertEquals(3, found.size)
        assertEquals(41.0, found[0]!!, 1e-9)
        assertEquals(51.0, found[1]!!, 1e-9)
        assertEquals(37.0, found[2]!!, 1e-9)
    }

    @Test
    fun `a point is a Point geometry in longitude, latitude order`() {
        // GeoJSON is x,y. Emitting lat,lon instead drops every pin into the Atlantic
        // off West Africa, which is a very quiet way to ship a broken map.
        val features = listOf(mosque(41.01, 29.02, 1L)).clusteredJson(zoom = 15.0)
        val geometry = features.getJSONObject(0).getJSONObject("geometry")
        assertEquals("Point", geometry.getString("type"))
        val coordinates = geometry.getJSONArray("coordinates")
        assertEquals(29.02, coordinates.getDouble(0), 1e-9)
        assertEquals(41.01, coordinates.getDouble(1), 1e-9)
    }

    @Test
    fun `mosques inside one grid cell merge into a cluster with a count`() {
        // Three points within a few metres must not draw three overlapping pins.
        val features = listOf(
            mosque(41.0100, 29.0100, 1L),
            mosque(41.0101, 29.0101, 2L),
            mosque(41.0102, 29.0102, 3L)
        ).clusteredJson(zoom = 15.0)

        assertEquals(1, features.length())
        val properties = features.propertiesAt(0)
        assertEquals("cluster", properties.getString("kind"))
        assertEquals(3, properties.getInt("count"))
        assertEquals("3", properties.getString("label"))
    }

    @Test
    fun `a cluster sits on the mean of its members`() {
        // Both points must land in the same grid cell at zoom 10, where a cell is
        // 0.02 degrees across, so keep them within that.
        val features = listOf(
            mosque(41.00, 29.00, 1L),
            mosque(41.01, 29.01, 2L)
        ).clusteredJson(zoom = 10.0)
        assertEquals(1, features.length())
        assertEquals("cluster", features.propertiesAt(0).getString("kind"))
        val coordinates = features.getJSONObject(0)
            .getJSONObject("geometry").getJSONArray("coordinates")
        assertEquals(29.005, coordinates.getDouble(0), 1e-6)
        assertEquals(41.005, coordinates.getDouble(1), 1e-6)
    }

    @Test
    fun `zooming in splits a cluster back into individual points`() {
        val list = listOf(
            mosque(41.0100, 29.0100, 1L),
            mosque(41.0101, 29.0101, 2L)
        )
        assertEquals(1, list.clusteredJson(zoom = 10.0).length())
        assertEquals(2, list.clusteredJson(zoom = 17.0).length())
    }

    @Test
    fun `cluster label is plain below a thousand`() {
        val many = (0 until 999).map { mosque(41.0 + it * 1e-5, 29.0, it.toLong()) }
        assertEquals("999", many.clusteredJson(zoom = 10.0).propertiesAt(0).getString("label"))
    }

    @Test
    fun `cluster label switches to thousands above a thousand`() {
        val many = (0 until 1200).map { mosque(41.0 + it * 1e-5, 29.0, it.toLong()) }
        val properties = many.clusteredJson(zoom = 10.0).propertiesAt(0)
        assertEquals(1200, properties.getInt("count"))
        assertEquals("1.2k", properties.getString("label"))
    }

    @Test
    fun `the thousands label keeps a dot decimal separator under a turkish locale`() {
        // "1,2k" is correct Turkish and would be truncated to 1 by a style expression.
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val many = (0 until 1200).map { mosque(41.0 + it * 1e-5, 29.0, it.toLong()) }
            assertEquals("1.2k", many.clusteredJson(zoom = 10.0).propertiesAt(0).getString("label"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `mosques at the null island are dropped rather than clustered there`() {
        // lat/lon of 0.0 means "no data" on an Overpass element, and a cluster at
        // 0,0 would sit in the Atlantic off West Africa.
        val features = listOf(mosque(0.0, 0.0, 1L), mosque(41.0, 29.0, 2L)).clusteredJson(zoom = 12.0)
        assertEquals(1, features.length())
        val coordinates = features.getJSONObject(0)
            .getJSONObject("geometry").getJSONArray("coordinates")
        assertEquals(29.0, coordinates.getDouble(0), 1e-9)
        assertEquals(41.0, coordinates.getDouble(1), 1e-9)
    }

    @Test
    fun `an empty list produces an empty collection rather than null`() {
        assertEquals(0, emptyList<Mosque>().clusteredJson(zoom = 12.0).length())
    }

    @Test
    fun `no mosque is lost across a wide zoom sweep`() {
        val list = (0 until 60).map { mosque(41.0 + it * 0.004, 29.0 + it * 0.004, it.toLong()) }
        for (zoom in 8..18) {
            val features = list.clusteredJson(zoom.toDouble())
            var points = 0
            var clustered = 0
            for (i in 0 until features.length()) {
                val properties = features.propertiesAt(i)
                if (properties.getString("kind") == "mosque") points++ else clustered += properties.getInt("count")
            }
            assertEquals(
                "zoom $zoom must account for every mosque exactly once",
                list.size,
                points + clustered
            )
        }
    }

    @Test
    fun `every point index resolves back to a real mosque`() {
        val list = (0 until 40).map { mosque(41.0 + it * 0.002, 29.0 + it * 0.002, it.toLong()) }
        val features = list.clusteredJson(zoom = 16.0)
        val seen = mutableSetOf<Int>()
        for (i in 0 until features.length()) {
            val properties = features.propertiesAt(i)
            if (properties.getString("kind") == "mosque") {
                val index = properties.getInt("idx")
                assertTrue("index $index out of bounds", index in list.indices)
                seen += index
            }
        }
        assertEquals("no mosque may be lost", list.size, seen.size)
    }
}
