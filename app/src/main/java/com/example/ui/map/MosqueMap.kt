package com.example.ui.map

import android.content.Context
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.Mosque
import com.example.ui.theme.LocalThemeColors
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.LocalThemeSpec
import org.maplibre.android.MapLibre
import org.maplibre.android.RenderingEngine
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val MOSQUE_SOURCE = "mosques"
internal const val RADIUS_SOURCE = "radius"
private const val READY_TIMEOUT_MS = 25_000L

/**
 * Progress of the map, reported stage by stage.
 *
 * The previous three attempts all failed silently and identically: style loaded,
 * tiles downloaded, nothing drawn. Naming the stage is the only way to tell a
 * dead rendering backend from a dead network from a dead lifecycle.
 */
enum class MapPhase { Idle, Starting, MapReady, StyleLoading, Ready, Failed }

data class MapStatus(
    val phase: MapPhase = MapPhase.Idle,
    val backend: String? = null,
    val problem: String? = null
) {
    val settled: Boolean get() = phase == MapPhase.Ready || phase == MapPhase.Failed
    val warning: String? get() = if (phase == MapPhase.Failed) problem else null
}

/**
 * Mosque map.
 *
 * Rendering backend: this app deliberately links `android-sdk-opengl`. The
 * default `android-sdk` artifact is a **Vulkan** build, and MapLibre auto-selects
 * Vulkan on any device that advertises Vulkan 1.0. Several entry-level Android
 * GPUs advertise Vulkan 1.0 and then render nothing: the style loads, tiles
 * download over the network, no exception is thrown, and the map stays blank
 * (maplibre-native#4079, observed on Mali-G76). OpenGL ES has the longest
 * support record and works everywhere, so it is the only sane default here.
 *
 * A TextureView composes correctly inside scrolling/animated Compose surfaces.
 * LifecycleRegistry catches a new observer up to the owner's current state.
 */
@Composable
internal fun MosqueMap(
    centerLatitude: Double,
    centerLongitude: Double,
    radiusMeters: Int,
    mosques: List<Mosque>,
    onSelect: (Mosque) -> Unit,
    onStatus: (MapStatus) -> Unit,
    modifier: Modifier = Modifier,
    recenterKey: Int = 0,
    focusedMosque: Mosque? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val spec = LocalThemeSpec.current
    val isDark = LocalThemeIsDark.current
    val palette = LocalThemeColors.current

    val latestSelect by rememberUpdatedState(onSelect)
    val latestStatus by rememberUpdatedState(onStatus)
    val latestAccent by rememberUpdatedState(palette.primary)
    val latestMosques by rememberUpdatedState(mosques)
    // The camera-idle listener is registered once and would otherwise close over
    // the radius and centre from the composition that registered it, so changing
    // the radius would repaint the search circle at its previous size.
    val latestCenterLat by rememberUpdatedState(centerLatitude)
    val latestCenterLon by rememberUpdatedState(centerLongitude)
    val latestRadius by rememberUpdatedState(radiusMeters)

    val styleJson = remember(spec.id, isDark) { buildMapStyle(palette, isDark) }

    var status by remember { mutableStateOf(MapStatus()) }
    var readyMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var listenedMap by remember { mutableStateOf<MapLibreMap?>(null) }
    fun publish(next: MapStatus) {
        status = next
        latestStatus(next)
    }

    // Always repaints from the latest values, never from whatever a listener
    // happened to capture when it was registered.
    fun refreshOverlays(map: MapLibreMap) {
        drawOverlays(map, latestMosques, latestCenterLat, latestCenterLon, latestRadius, latestAccent)
    }

    // The native library must exist before a MapView is constructed.
    val engineReady = remember {
        runCatching { MapLibre.getInstance(context.applicationContext) }.isSuccess
    }
    val backend = remember(engineReady) {
        if (!engineReady) null else runCatching { RenderingEngine.getCurrentType()?.name }.getOrNull()
    }

    val mapView = remember {
        if (!engineReady) null else MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(true)).apply {
            onCreate(null)
            setMaximumFps(60)
            setOnTouchListener { view, event ->
                view.parent?.requestDisallowInterceptTouchEvent(
                    event.actionMasked != MotionEvent.ACTION_UP && event.actionMasked != MotionEvent.ACTION_CANCEL
                )
                if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
                false
            }
        }
    }

    DisposableEffect(mapView) {
        val loaded = MapView.OnDidFinishLoadingMapListener { publish(MapStatus(MapPhase.Ready, backend)) }
        val failed = MapView.OnDidFailLoadingMapListener { problem ->
            android.util.Log.w("MosqueMap", problem)
            publish(MapStatus(MapPhase.Failed, backend, "Harita yüklenemedi. Bağlantıyı kontrol edip yeniden deneyin."))
        }
        mapView?.addOnDidFinishLoadingMapListener(loaded)
        mapView?.addOnDidFailLoadingMapListener(failed)
        onDispose {
            mapView?.removeOnDidFinishLoadingMapListener(loaded)
            mapView?.removeOnDidFailLoadingMapListener(failed)
        }
    }

    LaunchedEffect(mapView, backend) {
        if (!engineReady) {
            publish(MapStatus(MapPhase.Failed, null, "Harita motoru başlatılamadı."))
        } else {
            publish(MapStatus(MapPhase.Starting, backend, null))
        }
    }

    Box(modifier) {
        if (mapView == null) {
            // The engine failed to load. The red diagnostic band is the UI's
            // responsibility; this placeholder just keeps the layout intact.
            Box(Modifier.fillMaxSize())
        } else {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { _: Context -> mapView })
        }
    }

    LaunchedEffect(mapView, styleJson) {
        val view = mapView ?: return@LaunchedEffect
        view.getMapAsync { map ->
            if (view.isDestroyed) return@getMapAsync
            publish(MapStatus(MapPhase.MapReady, backend, null))
            configureMap(map)
            publish(MapStatus(MapPhase.StyleLoading, backend, null))
            map.setStyle(mosqueStyleBuilder(styleJson)) {
                if (view.isDestroyed) return@setStyle
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(centerLatitude, centerLongitude))
                    .zoom(zoomFor(radiusMeters))
                    .build()
                readyMap = map
                refreshOverlays(map)

                // Recluster when the camera settles, not on every frame. A move
                // listener would re-run the whole grid clustering and re-serialise
                // the GeoJSON 60 times a second while panning, which is exactly the
                // kind of load that makes a map feel broken on a budget phone.
                if (listenedMap !== map) {
                listenedMap = map
                map.addOnCameraIdleListener(MapLibreMap.OnCameraIdleListener {
                    refreshOverlays(map)
                })
                map.addOnMapClickListener(MapLibreMap.OnMapClickListener { point ->
                    val hit = map.queryRenderedFeatures(
                        map.projection.toScreenLocation(point),
                        "$MOSQUE_SOURCE-points", "$MOSQUE_SOURCE-cluster-core"
                    )
                    val properties = hit.firstOrNull()?.properties() ?: return@OnMapClickListener false
                    if (properties.get("kind")?.asString == "cluster") {
                        map.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(point, map.cameraPosition.zoom + 2.0))
                        return@OnMapClickListener true
                    }
                    // properties() is a Gson JsonObject, so the value is a
                    // JsonPrimitive. Casting it to Number would always yield null
                    // and the pin would silently do nothing.
                    val index = properties.get("idx")?.takeIf { it.isJsonPrimitive }?.asInt
                        ?: return@OnMapClickListener false
                    latestMosques.getOrNull(index)?.let { latestSelect(it) }
                    true
                })
                }
            }
        }
    }

    // The data can change without the camera moving at all: a new radius, a
    // different filter, a search, or a refresh after a failed fetch. Without this
    // the pins would keep showing whatever arrived when the style first loaded.
    LaunchedEffect(readyMap, mosques, radiusMeters, centerLatitude, centerLongitude) {
        val map = readyMap ?: return@LaunchedEffect
        refreshOverlays(map)
    }

    LaunchedEffect(readyMap, centerLatitude, centerLongitude, radiusMeters, recenterKey) {
        val latDelta = radiusMeters / 111_320.0
        val lonDelta = latDelta / maxOf(.01, cos(Math.toRadians(centerLatitude)))
        val bounds = org.maplibre.android.geometry.LatLngBounds.from(
            (centerLatitude + latDelta).coerceAtMost(90.0),
            (centerLongitude + lonDelta).coerceAtMost(180.0),
            (centerLatitude - latDelta).coerceAtLeast(-90.0),
            (centerLongitude - lonDelta).coerceAtLeast(-180.0)
        )
        readyMap?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(bounds, 28))
    }

    LaunchedEffect(readyMap, focusedMosque) {
        val map = readyMap ?: return@LaunchedEffect
        val target = focusedMosque ?: return@LaunchedEffect
        if (target.isValid) {
            map.animateCamera(
                org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                    LatLng(target.latitude, target.longitude),
                    maxOf(map.cameraPosition.zoom, 16.0)
                ),
                500
            )
        }
    }

    // Never let a silent stall look like a working map.
    LaunchedEffect(mapView, styleJson) {
        kotlinx.coroutines.delay(READY_TIMEOUT_MS)
        if (!status.settled) {
            publish(
                MapStatus(
                    MapPhase.Failed, backend,
                    "Harita katmanları yüklenemedi. Cami listesini kullanabilir veya haritayı yeniden deneyebilirsiniz."
                )
            )
        }
    }

    // The observer receives existing STARTED/RESUMED states immediately.
    DisposableEffect(lifecycleOwner, mapView) {
        val view = mapView
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> view?.onStart()
                Lifecycle.Event.ON_PAUSE -> view?.onPause()
                Lifecycle.Event.ON_RESUME -> view?.onResume()
                Lifecycle.Event.ON_STOP -> view?.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching {
                view?.onPause()
                view?.onStop()
                view?.onDestroy()
            }
        }
    }
}

private fun configureMap(map: MapLibreMap) {
    map.uiSettings.setCompassEnabled(false)
    map.uiSettings.setRotateGesturesEnabled(true)
    map.uiSettings.setTiltGesturesEnabled(false)
    map.uiSettings.setZoomGesturesEnabled(true)
    map.uiSettings.setScrollGesturesEnabled(true)
    map.uiSettings.setDoubleTapGesturesEnabled(true)
    map.uiSettings.setQuickZoomGesturesEnabled(true)
    map.uiSettings.setLogoEnabled(false)
    map.uiSettings.isAttributionEnabled = true
}

private fun drawOverlays(
    map: MapLibreMap,
    mosques: List<Mosque>,
    centerLat: Double,
    centerLon: Double,
    radiusMeters: Int,
    accent: Color
) {
    val zoom = map.cameraPosition.zoom
    val stroke = accent.toCss()
    map.getStyle { style ->
        style.getSourceAs<GeoJsonSource>(MOSQUE_SOURCE)?.setGeoJson(mosques.toClustered(zoom))
        style.getSourceAs<GeoJsonSource>("user-location")?.setGeoJson(
            FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(Point.fromLngLat(centerLon, centerLat))))
        )
        if (radiusMeters > 0) {
            style.getSourceAs<GeoJsonSource>(RADIUS_SOURCE)?.setGeoJson(
                radiusPolygon(centerLat, centerLon, radiusMeters)
            )
        }
    }
}

/**
 * Grid-clusters mosques for the current zoom. A dependency-free alternative to
 * supercluster: at mosque densities a screen-space grid is enough, and it keeps
 * the interaction responsive on low-end devices.
 */
internal fun List<Mosque>.toClustered(zoom: Double): FeatureCollection {
    if (zoom >= 18.0) return FeatureCollection.fromFeatures(mapIndexedNotNull { index, mosque ->
        if (!mosque.isValid) null else Feature.fromGeometry(Point.fromLngLat(mosque.longitude, mosque.latitude)).apply {
            addStringProperty("kind", "mosque")
            addNumberProperty("idx", index)
        }
    })
    val cell = 0.02 * Math.pow(0.55, (zoom - 10.0).coerceIn(0.0, 7.0))
    val cells = LinkedHashMap<Long, MutableList<IndexedValue<Mosque>>>()
    forEachIndexed { index, mosque ->
        if (!mosque.isValid) return@forEachIndexed
        val gx = Math.floor(mosque.longitude / cell).toLong()
        val gy = Math.floor(mosque.latitude / cell).toLong()
        cells.getOrPut(gx * 73856093L xor gy * 19349663L) { ArrayList() }
            .add(IndexedValue(index, mosque))
    }

    val features = ArrayList<Feature>(cells.size)
    for (members in cells.values) {
        if (members.size == 1) {
            val indexed = members.first()
            val m = indexed.value
            features += Feature.fromGeometry(Point.fromLngLat(m.longitude, m.latitude)).apply {
                addStringProperty("kind", "mosque")
                addNumberProperty("idx", indexed.index)
            }
        } else {
            var lon = 0.0
            var lat = 0.0
            members.forEach { lon += it.value.longitude; lat += it.value.latitude }
            val n = members.size
            features += Feature.fromGeometry(
                Point.fromLngLat(lon / n, lat / n)
            ).apply {
                addStringProperty("kind", "cluster")
                addNumberProperty("count", n)
                addStringProperty("label", if (n >= 1000) String.format(java.util.Locale.US, "%.1fk", n / 1000f) else n.toString())
            }
        }
    }
    return FeatureCollection.fromFeatures(features)
}

private fun radiusPolygon(lat: Double, lon: Double, radiusMeters: Int): FeatureCollection {
    val latDelta = radiusMeters / 111_320.0
    val lonDelta = radiusMeters / (111_320.0 * maxOf(0.01, cos(Math.toRadians(lat))))
    val points = ArrayList<Point>(65)
    for (i in 0..64) {
        val a = 2 * Math.PI * i / 64
        points += Point.fromLngLat(lon + lonDelta * cos(a), lat + latDelta * sin(a))
    }
    return FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(Polygon.fromLngLats(listOf(points)))))
}

internal fun zoomFor(radiusMeters: Int): Double = when {
    radiusMeters <= 1000 -> 15.0
    radiusMeters <= 3000 -> 14.0
    radiusMeters <= 5000 -> 13.0
    else -> 12.0
}

internal fun Color.toCss(): String {
    val r = (red * 255).roundToInt().coerceIn(0, 255)
    val g = (green * 255).roundToInt().coerceIn(0, 255)
    val b = (blue * 255).roundToInt().coerceIn(0, 255)
    return String.format(java.util.Locale.US, "#%02x%02x%02x", r, g, b)
}
