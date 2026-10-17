package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.DeviceLocationSource
import com.example.data.LocationResult
import com.example.data.RetrofitClient
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearestMosqueScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val locator = remember { DeviceLocationSource(context.applicationContext) }
    var fix by remember { mutableStateOf<LocationResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    fun permitted() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    var granted by remember { mutableStateOf(permitted()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = permitted()
        if (granted) retry++ else message = "Yakındaki camiler için konum izni gerekiyor."
    }
    fun requestFix() {
        if (permitted()) { granted = true; retry++ }
        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = permitted()
                if (nowGranted != granted) { granted = nowGranted; retry++ }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(granted, retry) {
        fix = null
        if (!granted) { message = "Yakındaki camiler için konum izni gerekiyor."; return@LaunchedEffect }
        loading = true
        message = null
        try {
            fix = locator.current()
            if (fix == null) message = "Konum alınamadı. Telefonun konumunu açıp yeniden deneyin."
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
            repository = RetrofitClient.mosqueRepository, currentLocation = fix,
            onBack = onBack, onLocate = { requestFix() },
            onOpenDirections = { openWalkingDirections(context, it.latitude, it.longitude, it.displayName) }
        )
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text("En Yakın Cami") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
            }) }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (loading) {
                    CircularProgressIndicator()
                    Text("Konumunuz alınıyor…")
                } else {
                    Text(message.orEmpty())
                    Button(onClick = { requestFix() }) { Text(if (granted) "Tekrar dene" else "Konum izni ver") }
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(if (granted) Settings.ACTION_LOCATION_SOURCE_SETTINGS else Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            if (!granted) data = Uri.parse("package:${context.packageName}")
                        })
                    }) { Text(if (granted) "Konum ayarları" else "Uygulama ayarları") }
                }
            }
        }
    }
}

