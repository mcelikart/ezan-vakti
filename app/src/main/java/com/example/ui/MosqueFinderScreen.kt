package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocationResult
import com.example.data.Mosque
import com.example.data.MosqueRepository
import com.example.data.turkishFold
import com.example.ui.map.MapStatus
import com.example.ui.map.MosqueMap
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.buttonBorder
import com.example.ui.theme.cardShape
import com.example.ui.theme.navBarBottomPadding
import java.util.Locale

private val RADIUS_OPTIONS = listOf(500, 1000, 3000, 5000, 10000)

/** Filter applied to fetched mosques. */
private enum class MosqueFilter(val label: String) {
    All("Tümü"),
    Mosque("Camiler"),
    PrayerRoom("Mescitler")
}

private enum class MosqueSort(val label: String) {
    Distance("En Yakın"),
    Name("İsme Göre (A-Z)")
}

private fun formatDistance(distanceMeters: Double): String {
    return if (distanceMeters >= 1000) {
        String.format(Locale.forLanguageTag("tr-TR"), "%.1f km", distanceMeters / 1000.0)
    } else {
        "${distanceMeters.toInt()} m"
    }
}

private fun cardinalDirectionTurkish(bearing: Double): String {
    val normalized = ((bearing % 360.0) + 360.0) % 360.0
    return when {
        normalized < 22.5 || normalized >= 337.5 -> "Kuzey"
        normalized < 67.5 -> "Kuzeydoğu"
        normalized < 112.5 -> "Doğu"
        normalized < 157.5 -> "Güneydoğu"
        normalized < 202.5 -> "Güney"
        normalized < 247.5 -> "Güneybatı"
        normalized < 292.5 -> "Batı"
        else -> "Kuzeybatı"
    }
}

private fun cardinalShortTurkish(bearing: Double): String {
    val normalized = ((bearing % 360.0) + 360.0) % 360.0
    return when {
        normalized < 22.5 || normalized >= 337.5 -> "K"
        normalized < 67.5 -> "KD"
        normalized < 112.5 -> "D"
        normalized < 157.5 -> "GD"
        normalized < 202.5 -> "G"
        normalized < 247.5 -> "GB"
        normalized < 292.5 -> "B"
        else -> "KB"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosqueFinderScreen(
    repository: MosqueRepository,
    currentLocation: LocationResult?,
    onBack: () -> Unit,
    onOpenDirections: (Mosque) -> Unit,
    onLocate: () -> Unit = {}
) {
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    var radius by remember { mutableIntStateOf(3000) }
    var filter by remember { mutableStateOf(MosqueFilter.All) }
    var sortOrder by remember { mutableStateOf(MosqueSort.Distance) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mosques by remember { mutableStateOf<List<Mosque>>(emptyList()) }
    var selectedMosque by remember { mutableStateOf<Mosque?>(null) }
    var isMapExpanded by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(true) }
    var resultArea by remember { mutableStateOf<Triple<Double, Double, Int>?>(null) }
    var mapStatus by remember { mutableStateOf(MapStatus()) }
    var retry by remember { mutableIntStateOf(0) }
    var appliedRetry by remember { mutableIntStateOf(0) }
    var recenter by remember { mutableIntStateOf(0) }
    var mapAttempt by remember { mutableIntStateOf(0) }

    val userLat = currentLocation?.latitude ?: 0.0
    val userLng = currentLocation?.longitude ?: 0.0
    val hasFix = currentLocation != null &&
        userLat in -90.0..90.0 &&
        userLng in -180.0..180.0 &&
        !(userLat == 0.0 && userLng == 0.0)

    LaunchedEffect(userLat, userLng, radius, retry) {
        val area = Triple(userLat, userLng, radius)
        if (resultArea != area) {
            mosques = emptyList()
            selectedMosque = null
        }
        if (!hasFix) return@LaunchedEffect
        loading = true
        error = null
        val forceRefresh = retry != appliedRetry
        appliedRetry = retry
        try {
            mosques = repository.findNearby(userLat, userLng, radius, forceRefresh = forceRefresh)
            resultArea = area
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "Cami servisine şu an ulaşılamadı veya yoğunluk var. Yeniden deneyebilir veya harita uygulamasında doğrudan arama yapabilirsiniz."
        } finally {
            loading = false
        }
    }

    val totalMosquesCount = remember(mosques) { mosques.count { !it.isSmallPrayerRoom } }
    val totalPrayerRoomsCount = remember(mosques) { mosques.count { it.isSmallPrayerRoom } }

    val visible = remember(mosques, query, filter, sortOrder, userLat, userLng) {
        val needle = query.turkishFold()
        val filtered = mosques.filter { mosque ->
            (when (filter) {
                MosqueFilter.All -> true
                MosqueFilter.Mosque -> !mosque.isSmallPrayerRoom
                MosqueFilter.PrayerRoom -> mosque.isSmallPrayerRoom
            }) && (needle.isEmpty() ||
                needle in mosque.displayName.turkishFold() ||
                needle in mosque.address.turkishFold())
        }
        when (sortOrder) {
            MosqueSort.Distance -> filtered.sortedBy { it.distanceMetersFrom(userLat, userLng) }
            MosqueSort.Name -> filtered.sortedBy { it.displayName.turkishFold() }
        }
    }

    val nearestOverall = remember(visible, userLat, userLng) {
        visible.minByOrNull { it.distanceMetersFrom(userLat, userLng) }
    }

    Scaffold(
        modifier = Modifier.testTag("mosque_finder_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("En Yakın Cami Bulucu", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = currentLocation?.city ?: "Konum bekleniyor",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(
                        enabled = hasFix,
                        onClick = { openNearbyMosqueSearch(context, userLat, userLng) }
                    ) {
                        Icon(Icons.Default.OpenInNew, "Harita uygulamasında cami ara")
                    }
                    IconButton(onClick = onLocate) {
                        Icon(Icons.Default.MyLocation, "Konumu güncelle")
                    }
                    IconButton(onClick = { retry++ }, enabled = hasFix && !loading) {
                        Icon(Icons.Default.Refresh, "Yenile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("mosque_results"),
            contentPadding = PaddingValues(bottom = navBarBottomPadding() + 28.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Location & Stats Banner
            item {
                LocationSummaryBanner(
                    location = currentLocation,
                    totalCount = mosques.size,
                    mosqueCount = totalMosquesCount,
                    mescitCount = totalPrayerRoomsCount,
                    radiusMeters = radius,
                    showMap = showMap,
                    onToggleMap = { showMap = !showMap },
                    onRefreshLocation = onLocate
                )
            }

            // 2. Highlighted Nearest Mosque Quick Card (when available and not searching)
            if (nearestOverall != null && query.isBlank()) {
                item(key = "nearest_highlight") {
                    NearestMosqueHighlightCard(
                        mosque = nearestOverall,
                        userLat = userLat,
                        userLng = userLng,
                        onWalkingDirections = { onOpenDirections(nearestOverall) },
                        onDrivingDirections = {
                            openDrivingDirections(
                                context,
                                nearestOverall.latitude,
                                nearestOverall.longitude,
                                nearestOverall.displayName
                            )
                        },
                        onShare = {
                            val dist = formatDistance(nearestOverall.distanceMetersFrom(userLat, userLng))
                            shareMosqueLocation(context, nearestOverall, dist)
                        }
                    )
                }
            }

            // 3. Interactive Map Section
            if (hasFix && showMap) {
                item(key = "map") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isMapExpanded) 380.dp else 240.dp)
                            ) {
                                key(mapAttempt) {
                                    MosqueMap(
                                        centerLatitude = userLat,
                                        centerLongitude = userLng,
                                        radiusMeters = radius,
                                        mosques = visible,
                                        onSelect = { tappedMosque ->
                                            selectedMosque = tappedMosque
                                        },
                                        onStatus = { mapStatus = it },
                                        modifier = Modifier.fillMaxSize(),
                                        recenterKey = recenter,
                                        focusedMosque = selectedMosque
                                    )
                                }

                                if (!mapStatus.settled) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(12.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Harita yükleniyor", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                // Floating Map Controls (Top-Right)
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledIconButton(
                                        onClick = { recenter++ },
                                        modifier = Modifier.size(38.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.MyLocation, "Konumuma dön", modifier = Modifier.size(18.dp))
                                    }
                                    FilledIconButton(
                                        onClick = { isMapExpanded = !isMapExpanded },
                                        modifier = Modifier.size(38.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isMapExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                            contentDescription = if (isMapExpanded) "Haritayı küçült" else "Haritayı büyüt",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Bottom-Start Hint Pill
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                                ) {
                                    Text(
                                        text = "İğnelere dokunarak cami seçebilirsiniz",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Selected Mosque Action Strip inside the Map Card
                            AnimatedVisibility(
                                visible = selectedMosque != null,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                selectedMosque?.let { sel ->
                                    val dist = formatDistance(sel.distanceMetersFrom(userLat, userLng))
                                    val walkMins = sel.walkingMinutesFrom(userLat, userLng)
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = sel.displayName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${sel.address} • $dist (~$walkMins dk yürüyüş)",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { selectedMosque = null },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, "Kapat", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { onOpenDirections(sel) },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Yürüyerek Git", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        openDrivingDirections(context, sel.latitude, sel.longitude, sel.displayName)
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                                ) {
                                                    Icon(Icons.Default.DirectionsCar, null, Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Araçla Git", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                FilledTonalIconButton(
                                                    onClick = { shareMosqueLocation(context, sel, dist) }
                                                ) {
                                                    Icon(Icons.Default.Share, "Paylaş", Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            mapStatus.warning?.let { warning ->
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = warning,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { mapStatus = MapStatus(); mapAttempt++ }) {
                                Text("Haritayı yeniden yükle", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Search, Radius, Filter, and Sort Controls
            item {
                SearchAndFilterControls(
                    query = query,
                    onQueryChange = { query = it },
                    radius = radius,
                    onRadiusChange = { radius = it; recenter++ },
                    filter = filter,
                    onFilterChange = { filter = it },
                    sortOrder = sortOrder,
                    onSortOrderChange = { sortOrder = it },
                    allCount = mosques.size,
                    mosqueCount = totalMosquesCount,
                    mescitCount = totalPrayerRoomsCount
                )
            }

            if (loading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(CircleShape)
                    )
                }
            }

            if (error != null && mosques.isNotEmpty()) {
                item {
                    Text(
                        text = "Yenilenemedi. Önceki arama sonuçları gösteriliyor.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // 5. Results List or Empty/Error State
            when {
                !hasFix -> item { CenteredMessage("Harita ve liste için konum gerekiyor.") }
                error != null && mosques.isEmpty() -> item {
                    ErrorMessage(
                        text = error!!,
                        onRetry = { retry++ },
                        onOpenExternalMap = { openNearbyMosqueSearch(context, userLat, userLng) }
                    )
                }
                loading && mosques.isEmpty() -> item {
                    CenteredMessage("Camiler aranıyor…")
                }
                visible.isEmpty() -> item {
                    EmptyRadiusCard(
                        hasAnyMosques = mosques.isNotEmpty(),
                        currentRadius = radius,
                        onExpandRadius = {
                            val nextRadius = RADIUS_OPTIONS.firstOrNull { it > radius } ?: 10000
                            radius = nextRadius
                            recenter++
                        },
                        onClearFilter = {
                            query = ""
                            filter = MosqueFilter.All
                        }
                    )
                }
                else -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (query.isBlank()) "En yakın • ${visible.size} sonuç" else "${visible.size} sonuç",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${radius / 1000.0} km yarıçap".replace(".0 ", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    itemsIndexed(
                        items = visible,
                        key = { _, it -> "${it.type}:${it.id}:${it.latitude}:${it.longitude}" }
                    ) { index, mosque ->
                        Box(Modifier.padding(horizontal = 14.dp)) {
                            MosqueRow(
                                mosque = mosque,
                                rank = index + 1,
                                isNearest = mosque == nearestOverall,
                                isSelected = mosque == selectedMosque,
                                userLat = userLat,
                                userLng = userLng,
                                onClick = {
                                    selectedMosque = mosque
                                },
                                onDriveClick = {
                                    openDrivingDirections(context, mosque.latitude, mosque.longitude, mosque.displayName)
                                },
                                onShareClick = {
                                    val d = formatDistance(mosque.distanceMetersFrom(userLat, userLng))
                                    shareMosqueLocation(context, mosque, d)
                                }
                            )
                        }
                    }
                    item {
                        Text(
                            text = "Doğrulanmış cami kayıtları ve harita verileri • Mesafeler kuş uçuşudur. Yürüme süreleri ortalama 4,8 km/sa hızla hesaplanır.",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationSummaryBanner(
    location: LocationResult?,
    totalCount: Int,
    mosqueCount: Int,
    mescitCount: Int,
    radiusMeters: Int,
    showMap: Boolean,
    onToggleMap: () -> Unit,
    onRefreshLocation: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .clickable { onRefreshLocation() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = location?.city ?: "Konum bekleniyor",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val accText = location?.accuracyMeters?.let { "GPS doğruluğu: ~${it.toInt()} m • " }.orEmpty()
                    Text(
                        text = "${accText}$mosqueCount Cami, $mescitCount Mescit bulundu",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FilterChip(
                selected = showMap,
                onClick = onToggleMap,
                label = { Text(if (showMap) "Harita Açık" else "Harita Kapalı", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun NearestMosqueHighlightCard(
    mosque: Mosque,
    userLat: Double,
    userLng: Double,
    onWalkingDirections: () -> Unit,
    onDrivingDirections: () -> Unit,
    onShare: () -> Unit
) {
    val theme = LocalAppTheme.current
    val distance = mosque.distanceMetersFrom(userLat, userLng)
    val distanceLabel = formatDistance(distance)
    val walkMinutes = mosque.walkingMinutesFrom(userLat, userLng)
    val bearing = mosque.bearingFrom(userLat, userLng)
    val directionName = cardinalDirectionTurkish(bearing)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .border(theme.buttonBorder, theme.cardShape),
        shape = theme.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "SİZE EN YAKIN İBADETHANE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "$distanceLabel • ~$walkMinutes dk",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = mosque.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${mosque.address} • Yön: $directionName (${bearing.toInt()}°)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onWalkingDirections,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Yürüyerek Yol Tarifi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDrivingDirections,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Araçla", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, "Konumu Paylaş", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterControls(
    query: String,
    onQueryChange: (String) -> Unit,
    radius: Int,
    onRadiusChange: (Int) -> Unit,
    filter: MosqueFilter,
    onFilterChange: (MosqueFilter) -> Unit,
    sortOrder: MosqueSort,
    onSortOrderChange: (MosqueSort) -> Unit,
    allCount: Int,
    mosqueCount: Int,
    mescitCount: Int
) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Cami veya mahalle ara") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Aramayı temizle")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp)
        )

        // Radius Selector Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(RADIUS_OPTIONS) { option ->
                val label = if (option < 1000) "$option m" else "${option / 1000} km"
                FilterChip(
                    selected = radius == option,
                    onClick = { onRadiusChange(option) },
                    label = { Text(label, fontSize = 12.sp, fontWeight = if (radius == option) FontWeight.Bold else FontWeight.Medium) }
                )
            }
        }

        // Category Filter + Sort Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MosqueFilter.entries.forEach { option ->
                    val count = when (option) {
                        MosqueFilter.All -> allCount
                        MosqueFilter.Mosque -> mosqueCount
                        MosqueFilter.PrayerRoom -> mescitCount
                    }
                    FilterChip(
                        selected = filter == option,
                        onClick = { onFilterChange(option) },
                        label = {
                            Text(
                                text = if (allCount > 0) "${option.label} ($count)" else option.label,
                                fontSize = 11.5.sp
                            )
                        }
                    )
                }
            }

            AssistChip(
                onClick = {
                    onSortOrderChange(
                        if (sortOrder == MosqueSort.Distance) MosqueSort.Name else MosqueSort.Distance
                    )
                },
                label = { Text(sortOrder.label, fontSize = 11.sp) },
                leadingIcon = {
                    Icon(Icons.Default.SwapVert, null, modifier = Modifier.size(15.dp))
                }
            )
        }
    }
}

@Composable
private fun MosqueRow(
    mosque: Mosque,
    rank: Int,
    isNearest: Boolean,
    isSelected: Boolean = false,
    userLat: Double,
    userLng: Double,
    onClick: () -> Unit,
    onDriveClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val distance = mosque.distanceMetersFrom(userLat, userLng)
    val distanceLabel = formatDistance(distance)
    val walkMinutes = mosque.walkingMinutesFrom(userLat, userLng)
    val bearing = mosque.bearingFrom(userLat, userLng)
    val shortDir = cardinalShortTurkish(bearing)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else if (isNearest) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else if (isNearest) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Architectural Icon Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (mosque.isSmallPrayerRoom) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mosque.isSmallPrayerRoom) Icons.Default.Place else Icons.Default.Mosque,
                        contentDescription = null,
                        tint = if (mosque.isSmallPrayerRoom) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isNearest) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "#1 EN YAKIN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "#$rank",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (mosque.isSmallPrayerRoom) "Mescit" else "Cami",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = mosque.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = mosque.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Distance & Compass Direction Column
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = distanceLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(12.dp)
                                .rotate(bearing.toFloat())
                        )
                        Text(
                            text = "$shortDir • ~$walkMinutes dk",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Bottom Quick Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Yaklaşık $walkMinutes dk • Yol tarifi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.clickable(onClick = onClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Yürüyerek",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable(onClick = onDriveClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Araçla",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable(onClick = onShareClick)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Paylaş",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRadiusCard(
    hasAnyMosques: Boolean,
    currentRadius: Int,
    onExpandRadius: () -> Unit,
    onClearFilter: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mosque,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = if (!hasAnyMosques) {
                    "Bu yarıçapta kayıt bulunamadı. Arama alanını büyütebilirsiniz."
                } else {
                    "Aramanıza uyan sonuç yok."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!hasAnyMosques && currentRadius < 10000) {
                Button(onClick = onExpandRadius, shape = RoundedCornerShape(12.dp)) {
                    Text("Arama Alanını Genişlet")
                }
            } else if (hasAnyMosques) {
                OutlinedButton(onClick = onClearFilter, shape = RoundedCornerShape(12.dp)) {
                    Text("Filtreyi Temizle")
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorMessage(
    text: String,
    onRetry: () -> Unit,
    onOpenExternalMap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onRetry) {
                    Text("Tekrar dene")
                }
                OutlinedButton(onClick = onOpenExternalMap) {
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Haritada Ara")
                }
            }
        }
    }
}
