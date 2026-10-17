package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocationResult
import com.example.data.Mosque
import com.example.data.MosqueRepository
import com.example.data.turkishFold
import com.example.ui.map.MapStatus
import com.example.ui.map.MapPhase
import com.example.ui.map.MosqueMap
import com.example.ui.theme.navBarBottomPadding
import kotlinx.coroutines.launch

private val RADIUS_OPTIONS = listOf(1000, 3000, 5000, 10000)

/** Filter applied to fetched mosques. */
private enum class MosqueFilter(val label: String) {
    All("Tümü"),
    Mosque("Camiler"),
    PrayerRoom("Mescitler")
}

/**
 * Nearby mosques, rebuilt from scratch.
 *
 * Everything the previous versions relied on is gone: no hand-built vector style,
 * no tiled DOM, no shared state with a global ViewModel. The screen owns its own
 * state, fetches through its own repository, and renders the map through a
 * component that reports which stage it reached.
 */
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
    var radius by remember { mutableIntStateOf(3000) }
    var filter by remember { mutableStateOf(MosqueFilter.All) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mosques by remember { mutableStateOf<List<Mosque>>(emptyList()) }
    var resultArea by remember { mutableStateOf<Triple<Double, Double, Int>?>(null) }
    var mapStatus by remember { mutableStateOf(MapStatus()) }
    var retry by remember { mutableIntStateOf(0) }
    var appliedRetry by remember { mutableIntStateOf(0) }
    var recenter by remember { mutableIntStateOf(0) }
    var mapAttempt by remember { mutableIntStateOf(0) }
    val userLat = currentLocation?.latitude ?: 0.0
    val userLng = currentLocation?.longitude ?: 0.0
    val hasFix = currentLocation != null && userLat in -90.0..90.0 && userLng in -180.0..180.0

    LaunchedEffect(userLat, userLng, radius, retry) {
        val area = Triple(userLat, userLng, radius)
        if (resultArea != area) mosques = emptyList()
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
            error = "Cami servisine ulaşılamadı veya servis yoğun. Biraz sonra tekrar deneyin; harita uygulamasında arama da yapabilirsiniz."
        } finally {
            loading = false
        }
    }
    val visible = remember(mosques, query, filter, userLat, userLng) {
        val needle = query.turkishFold()
        mosques.filter { mosque ->
            (when (filter) {
                MosqueFilter.All -> true
                MosqueFilter.Mosque -> !mosque.isSmallPrayerRoom
                MosqueFilter.PrayerRoom -> mosque.isSmallPrayerRoom
            }) && (needle.isEmpty() || needle in mosque.displayName.turkishFold() || needle in mosque.address.turkishFold())
        }.sortedBy { it.distanceMetersFrom(userLat, userLng) }
    }
    Scaffold(
        modifier = Modifier.testTag("mosque_finder_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("En Yakın Cami", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
                actions = {
                    IconButton(enabled = hasFix, onClick = { openNearbyMosqueSearch(context, userLat, userLng) }) {
                        Icon(Icons.Default.OpenInNew, "Harita uygulamasında cami ara")
                    }
                    IconButton(onClick = onLocate) { Icon(Icons.Default.MyLocation, "Konumu güncelle") }
                    IconButton(onClick = { retry++ }, enabled = hasFix && !loading) { Icon(Icons.Default.Refresh, "Yenile") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).testTag("mosque_results"),
            contentPadding = PaddingValues(bottom = navBarBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(currentLocation?.city ?: "Konum bekleniyor", style = MaterialTheme.typography.labelLarge)
                    currentLocation?.accuracyMeters?.let {
                        Text("Konum doğruluğu: yaklaşık ${it.toInt()} m", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { SearchBar(query, { query = it }, radius, { radius = it; recenter++ }, filter, { filter = it }) }
            if (hasFix) item(key = "map") {
                Box(Modifier.fillMaxWidth().height(280.dp)) {
                    key(mapAttempt) {
                        MosqueMap(userLat, userLng, radius, visible, onOpenDirections, { mapStatus = it },
                            modifier = Modifier.fillMaxSize(), recenterKey = recenter)
                    }
                    if (!mapStatus.settled) {
                        Surface(Modifier.align(Alignment.TopStart).padding(12.dp), shape = RoundedCornerShape(8.dp)) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Harita yükleniyor", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    FilledIconButton(onClick = { recenter++ }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                        Icon(Icons.Default.MyLocation, "Konumuma dön")
                    }
                }
            }
            mapStatus.warning?.let { warning ->
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(warning, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { mapStatus = MapStatus(); mapAttempt++ }) { Text("Haritayı yeniden yükle") }
                    }
                }
            }
            if (loading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            if (error != null && mosques.isNotEmpty()) item {
                Text("Yenilenemedi. Önceki arama sonuçları gösteriliyor.", Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            when {
                !hasFix -> item { CenteredMessage("Harita ve liste için konum gerekiyor.") }
                error != null && mosques.isEmpty() -> item { ErrorMessage(error!!) { retry++ } }
                loading && mosques.isEmpty() -> item { CenteredMessage("Camiler aranıyor…") }
                visible.isEmpty() -> item {
                    CenteredMessage(if (mosques.isEmpty()) "Bu yarıçapta kayıt bulunamadı. Arama alanını büyütebilirsiniz." else "Aramanıza uyan sonuç yok.")
                }
                else -> {
                    item {
                        Text(if (query.isBlank()) "En yakın • ${visible.size} sonuç" else "${visible.size} sonuç",
                            Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold)
                    }
                    items(visible, key = { "${it.type}:${it.id}:${it.latitude}:${it.longitude}" }) { mosque ->
                        Box(Modifier.padding(horizontal = 14.dp)) {
                            MosqueRow(mosque, userLat, userLng, onClick = { onOpenDirections(mosque) })
                        }
                    }
                    item {
                        Text("OpenStreetMap kayıtları • Mesafeler kuş uçuşudur. Yürüme süreleri tahminidir.",
                            Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    radius: Int,
    onRadiusChange: (Int) -> Unit,
    filter: MosqueFilter,
    onFilterChange: (MosqueFilter) -> Unit
) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Cami ara") },
            shape = RoundedCornerShape(12.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(RADIUS_OPTIONS) { option ->
                FilterChip(
                    selected = radius == option,
                    onClick = { onRadiusChange(option) },
                    label = { Text("${option / 1000} km", fontSize = 12.sp) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MosqueFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { onFilterChange(option) },
                    label = { Text(option.label, fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
private fun MosqueRow(mosque: Mosque, userLat: Double, userLng: Double, onClick: () -> Unit) {
    val distance = mosque.distanceMetersFrom(userLat, userLng)
    val distanceLabel = if (distance >= 1000) String.format(java.util.Locale.forLanguageTag("tr-TR"), "%.1f km", distance / 1000) else "${distance.toInt()} m"
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = mosque.displayName,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mosque.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Yaklaşık ${mosque.walkingMinutesFrom(userLat, userLng)} dk • Yol tarifi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = distanceLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorMessage(text: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onRetry) { Text("Tekrar dene") }
        }
    }
}
