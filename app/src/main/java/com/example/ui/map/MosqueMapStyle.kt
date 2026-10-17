package com.example.ui.map

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ThemeColors
import java.util.Locale
import org.maplibre.android.maps.Style
import kotlin.math.roundToInt

/**
 * MapLibre style for the mosque map, built from the active app theme.
 *
 * Raster basemap by design:
 *  - a vector tile in central Istanbul measured 593 KB against 15 KB for raster at
 *    the same zoom, which matters a great deal on a mobile connection;
 *  - vector tiles need glyphs and a sprite, and any failure there takes the whole
 *    basemap down silently. Raster has no such dependency, so only the optional
 *    cluster count can ever be missing.
 *
 * Endpoints verified live before being written here:
 *   https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}
 *   https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}
 * Esri's template is {z}/{y}/{x} (row before column), which is not XYZ order.
 */
internal fun buildMapStyle(colors: ThemeColors, isDark: Boolean): String {
    val accent = colors.primary
    val base = colors.background
    val clusterInk = colors.onPrimary

    fun Color.css(): String {
        val r = (red * 255).roundToInt().coerceIn(0, 255)
        val g = (green * 255).roundToInt().coerceIn(0, 255)
        val b = (blue * 255).roundToInt().coerceIn(0, 255)
        // Locale.US matters: a Turkish default locale renders "0,750" and produces
        // invalid JSON, which previously took the whole style down.
        return String.format(Locale.US, "#%02x%02x%02x", r, g, b)
    }

    fun Float.paint(): String = String.format(Locale.US, "%.3f", this)

    val canvasBase = if (isDark) DARK_CANVAS else LIGHT_CANVAS
    val canvasLabels = if (isDark) DARK_LABELS else LIGHT_LABELS

    val layers = listOf(
        """{"id":"base","type":"raster","source":"base","paint":{"raster-opacity":1.0}}""",
        """{"id":"base-labels","type":"raster","source":"base-labels","paint":{"raster-opacity":${(if (isDark) 0.7f else 0.85f).paint()}}}""",
        """{"id":"radius-line","type":"line","source":"radius","paint":{"line-color":"${accent.css()}","line-width":1.5,"line-opacity":${0.7f.paint()},"line-dasharray":[4,3]}}""",
        """{"id":"$MOSQUE_SOURCE-halo","type":"circle","source":"$MOSQUE_SOURCE","filter":["!=","kind","cluster"],"paint":{"circle-radius":["interpolate",["linear"],["zoom"],12,9,17,15],"circle-color":"${base.css()}","circle-opacity":${0.92f.paint()}}}""",
        """{"id":"$MOSQUE_SOURCE-points","type":"circle","source":"$MOSQUE_SOURCE","filter":["!=","kind","cluster"],"paint":{"circle-radius":["interpolate",["linear"],["zoom"],12,5,17,8],"circle-color":"${accent.css()}","circle-stroke-width":0}}""",
        """{"id":"$MOSQUE_SOURCE-cluster-halo","type":"circle","source":"$MOSQUE_SOURCE","filter":["==","kind","cluster"],"paint":{"circle-radius":["step",["get","count"],19,25,24,60,30,140,37],"circle-color":"${accent.css()}","circle-opacity":${0.24f.paint()}}}""",
        """{"id":"$MOSQUE_SOURCE-cluster-core","type":"circle","source":"$MOSQUE_SOURCE","filter":["==","kind","cluster"],"paint":{"circle-radius":["step",["get","count"],14,25,18,60,22,140,27],"circle-color":"${accent.css()}","circle-stroke-width":2,"circle-stroke-color":"${base.css()}"}}""",
        """{"id":"$MOSQUE_SOURCE-cluster-label","type":"symbol","source":"$MOSQUE_SOURCE","filter":["==","kind","cluster"],"layout":{"text-field":["get","label"],"text-size":11,"text-font":["Noto Sans Bold"],"text-allow-overlap":true},"paint":{"text-color":"${clusterInk.css()}"}}"""
        ,"""{"id":"user-location","type":"circle","source":"user-location","paint":{"circle-radius":7,"circle-color":"#1976d2","circle-stroke-width":3,"circle-stroke-color":"#ffffff"}}"""
    ).joinToString(",")

    return """
    {
      "version": 8,
      "glyphs": "$GLYPHS",
      "sources": {
        "base": { "type":"raster", "tiles":["$canvasBase"], "tileSize":256, "minzoom":0, "maxzoom":16, "attribution":"$ATTRIBUTION" },
        "base-labels": { "type":"raster", "tiles":["$canvasLabels"], "tileSize":256, "minzoom":0, "maxzoom":16 },
        "$MOSQUE_SOURCE": { "type":"geojson", "data":{"type":"FeatureCollection","features":[]} },
        "user-location": { "type":"geojson", "data":{"type":"FeatureCollection","features":[]} },
        "$RADIUS_SOURCE": { "type":"geojson", "data":{"type":"FeatureCollection","features":[]} }
      },
      "layers": [ $layers ]
    }
    """.trimIndent()
}

private const val ESRI = "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas"
// The String overload of setStyle treats JSON as a URI and leaves a blank map.
internal fun mosqueStyleBuilder(json: String): Style.Builder = Style.Builder().fromJson(json)
private const val LIGHT_CANVAS = "$ESRI/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}"
private const val LIGHT_LABELS = "$ESRI/World_Light_Gray_Reference/MapServer/tile/{z}/{y}/{x}"
private const val DARK_CANVAS = "$ESRI/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}"
private const val DARK_LABELS = "$ESRI/World_Dark_Gray_Reference/MapServer/tile/{z}/{y}/{x}"
private const val GLYPHS = "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf"
private const val ATTRIBUTION = "© Esri, HERE, Garmin, © OpenStreetMap katkıcıları"
