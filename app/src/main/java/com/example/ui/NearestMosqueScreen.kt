package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AppRepository
import com.example.data.DeviceLocationSource
import com.example.data.LocationResult
import com.example.data.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearestMosqueScreen(
    onBack: () -> Unit,
    viewModel: PrayerViewModel? = null
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val locator = remember { DeviceLocationSource(context.applicationContext) }
    val appRepo = remember { AppRepository(context.applicationContext) }

    val vmLat by (viewModel?.latitudeFlow?.collectAsState() ?: remember { mutableStateOf<Double?>(null) })
    val vmLng by (viewModel?.longitudeFlow?.collectAsState() ?: remember { mutableStateOf<Double?>(null) })
    val vmCity by (viewModel?.cityFlow?.collectAsState() ?: remember { mutableStateOf<String?>(null) })

    var fix by remember { mutableStateOf<LocationResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }

    fun permitted() = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    var granted by remember { mutableStateOf(permitted()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = permitted()
        if (granted) {
            retry++
        } else {
            message = "Yakındaki camileri tam konumunuza göre bulmak için konum izni gerekiyor."
        }
    }

    fun requestFix() {
        if (permitted()) {
            granted = true
            retry++
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = permitted()
                if (nowGranted != granted) {
                    granted = nowGranted
                    retry++
                }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(granted, retry, vmLat, vmLng) {
        loading = true
        message = null
        try {
            var resolved: LocationResult? = null
            if (granted) {
                // 1. Try fast Fused + LocationManager via AppRepository first (includes city name)
                resolved = runCatching { appRepo.getCurrentLocation() }.getOrNull()
                // 2. Fallback to direct DeviceLocationSource
                if (resolved == null) {
                    resolved = runCatching { locator.current() }.getOrNull()
                }
            }
            // 3. Fallback to active ViewModel or stored city coordinates so the screen never dead-ends
            if (resolved == null) {
                val fallbackLat = vmLat ?: runCatching { appRepo.latitudeFlow.firstOrNull() }.getOrNull()
                val fallbackLng = vmLng ?: runCatching { appRepo.longitudeFlow.firstOrNull() }.getOrNull()
                val fallbackCity = vmCity ?: runCatching { appRepo.cityFlow.firstOrNull() }.getOrNull() ?: "Seçili Konum"
                if (fallbackLat != null && fallbackLng != null &&
                    fallbackLat in -90.0..90.0 && fallbackLng in -180.0..180.0 &&
                    !(fallbackLat == 0.0 && fallbackLng == 0.0)
                ) {
                    resolved = LocationResult(
                        latitude = fallbackLat,
                        longitude = fallbackLng,
                        city = fallbackCity,
                        accuracyMeters = null,
                        capturedAtMillis = System.currentTimeMillis()
                    )
                }
            }

            if (resolved != null) {
                fix = resolved
            } else {
                message = if (!granted) {
                    "Yakındaki camiler için konum izni gerekiyor."
                } else {
                    "Konum alınamadı. Telefonun konum servisini açıp yeniden deneyin."
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            message = "Konum alınamadı. İzinleri ve konum ayarlarını kontrol edin."
        } finally {
            loading = false
        }
    }

    if (fix != null) {
        MosqueFinderScreen(
            repository = RetrofitClient.mosqueRepository,
            currentLocation = fix,
            onBack = onBack,
            onLocate = { requestFix() },
            onOpenDirections = { openWalkingDirections(context, it.latitude, it.longitude, it.displayName) }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("En Yakın Cami", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (loading) Icons.Default.MyLocation else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        if (loading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Konumunuz alınıyor…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "GPS ve ağ konum servisleri kontrol ediliyor.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Konum Bilgisi Gerekli",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = message.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { requestFix() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (granted) "Tekrar dene" else "Konum izni ver", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            if (granted) Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                            else Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                        ).apply {
                                            if (!granted) data = Uri.parse("package:${context.packageName}")
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (granted) "Konum ayarları" else "Uygulama ayarları")
                            }
                            TextButton(
                                onClick = {
                                    // Fallback to Istanbul Fatih / Sultanahmet if device has no location at all
                                    fix = LocationResult(
                                        latitude = 41.0082,
                                        longitude = 28.9784,
                                        city = vmCity ?: "İstanbul (Varsayılan)",
                                        accuracyMeters = null
                                    )
                                }
                            ) {
                                Text("Varsayılan Konum İle Haritayı Aç", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
