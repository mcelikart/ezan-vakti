package com.example.steps

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

private val Turkish = Locale.forLanguageTag("tr-TR")
private fun integer(value: Long) = NumberFormat.getIntegerInstance(Turkish).format(value)
private fun decimal(value: Double) = String.format(Turkish, "%.1f", value)
private data class WalkingReport(val days: List<DailySteps> = emptyList(), val chart: List<DailySteps> = emptyList(), val profile: WalkingProfile? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { StepStore.get(context) }
    val scope = rememberCoroutineScope()
    val available = remember { StepCounterService.sensor(context) != null }
    var enabled by remember { mutableStateOf(StepCounterService.enabled(context)) }
    var permission by remember { mutableStateOf(StepCounterService.hasPermission(context)) }
    var notifications by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var goal by remember { mutableIntStateOf(StepCounterService.settings(context).getInt("goal", 6000)) }
    var today by remember { mutableStateOf(StepStore.dayKey()) }
    var selected by rememberSaveable { mutableStateOf(StepStore.dayKey()) }
    var period by rememberSaveable { mutableStateOf(ReportPeriod.DAY) }
    var editProfile by rememberSaveable { mutableStateOf(false) }
    var showModel by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }
    val revision by store.changes.collectAsStateWithLifecycle()
    val range = remember(selected, period) { WalkingDates.range(selected, period) }
    val report by produceState(WalkingReport(), range, today, revision) {
        value = withContext(Dispatchers.IO) {
            val rows = store.range(range.start, range.end).associateBy { it.date }
            val days = range.days.filter { it <= today }.map { rows[it] ?: DailySteps(it, 0, store.profile(it)) }
            val chartRange = if (period == ReportPeriod.DAY) {
                (6 downTo 0).map { WalkingDates.shift(selected, ReportPeriod.DAY, -it) }
            } else range.days
            val chartRows = store.range(chartRange.first(), chartRange.last()).associateBy { it.date }
            WalkingReport(days, chartRange.map { chartRows[it] ?: DailySteps(it, 0) }, store.profile())
        }
    }
    val total = report.days.sumOf { it.steps }
    val estimates = report.days.mapNotNull { it.estimate }
    val hasEstimate = report.profile != null
    val distance = estimates.sumOf { it.meters }
    val calories = estimates.sumOf { it.activeKcal }
    val activeDays = report.days.count { it.steps > 0 }
    val average = if (report.days.isEmpty()) 0 else total / report.days.size
    val colors = MaterialTheme.colorScheme

    fun activate() {
        scope.launch {
            if (!StepCounterService.enabled(context)) withContext(Dispatchers.IO) { store.resetBaseline() }
            StepCounterService.settings(context).edit().putBoolean("enabled", true).apply()
            enabled = true
            StepCounterService.startIfEnabled(context)
        }
    }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permission = it
        if (it) activate()
    }
    val notificationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifications = it }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permission = StepCounterService.hasPermission(context)
                enabled = StepCounterService.enabled(context)
                notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            val nextToday = StepStore.dayKey()
            if (selected == today) selected = nextToday
            today = nextToday
            delay(30_000)
        }
    }
    if (editProfile) {
    ProfileEditor(report.profile, profileError,
        onDismiss = { editProfile = false; profileError = null },
        onSave = { profile ->
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { store.saveProfile(profile) }
                    editProfile = false
                    profileError = null
                } catch (_: Exception) { profileError = "Profil kaydedilemedi. Yeniden deneyin." }
            }
        })
        return
    }
    if (showModel) AlertDialog(
        onDismissRequest = { showModel = false },
        title = { Text("Kalori tahmini") },
        text = { Text("Aktif kalori, dinlenme enerjisi hariç düz zeminde yürüyüş için hesaplanır. Mesafe, adım sayısı ve kişisel adım uzunluğuna dayanır. Ölçülmüş adım uzunluğu yoksa boydan tahmin edilir. Eğim, koşu, taşınan yük ve kişisel metabolizma ölçülmez; sonuç laboratuvar ölçümü değildir.") },
        confirmButton = { TextButton(onClick = { showModel = false }) { Text("Tamam") } }
    )

    Scaffold(containerColor = colors.background, topBar = {
        TopAppBar(title = { Text("Yürüyüş", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
            actions = {
                TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(), tooltip = { PlainTooltip { Text("Boy, kilo ve kalibrasyon") } }, state = rememberTooltipState()) {
                    IconButton(onClick = { editProfile = true }) { Icon(Icons.Default.PersonOutline, "Yürüyüş profili") }
                }
            })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(when {
                            !available -> "Adım sensörü bulunamadı"
                            !permission -> "Fiziksel aktivite izni gerekli"
                            enabled -> "Adım sayımı açık"
                            else -> "Adım sayımı duraklatıldı"
                        }, style = MaterialTheme.typography.titleSmall)
                        if (available && permission && enabled) Text(
                            StepCounterService.settings(context).getString("sensor_mode", "Donanım sensörü") ?: "Donanım sensörü",
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Switch(checked = enabled && permission && available, enabled = available, onCheckedChange = {
                        if (it) {
                            if (permission) activate() else permissionRequest.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            StepCounterService.settings(context).edit().putBoolean("enabled", false).apply()
                            enabled = false
                            context.stopService(Intent(context, StepCounterService::class.java))
                        }
                    })
                }
                if (!permission && available) TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Uygulama izinleri") }
                if (!notifications) TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= 33) notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Bildirim izni") }
                TabRow(selectedTabIndex = period.ordinal, containerColor = colors.background) {
                    ReportPeriod.entries.forEach { item ->
                        Tab(selected = item == period, onClick = { period = item }, text = { Text(item.title) })
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selected = WalkingDates.shift(selected, period, -1) }) { Icon(Icons.Default.ChevronLeft, "Önceki dönem") }
                    val label = when (period) {
                        ReportPeriod.DAY -> SimpleDateFormat("d MMMM yyyy", Turkish).format(WalkingDates.calendar(selected).time)
                        ReportPeriod.WEEK -> {
                            val format = SimpleDateFormat("d MMM", Turkish)
                            "${format.format(WalkingDates.calendar(range.start).time)} - ${format.format(WalkingDates.calendar(range.end).time)}"
                        }
                        ReportPeriod.MONTH -> SimpleDateFormat("MMMM yyyy", Turkish).format(WalkingDates.calendar(selected).time)
                    }
                    Text(label, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { selected = WalkingDates.shift(selected, period, 1).coerceAtMost(today) }, enabled = range.end < today) {
                        Icon(Icons.Default.ChevronRight, "Sonraki dönem")
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(integer(total), fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.SemiBold)
                            Text("adım", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                        }
                        if (period == ReportPeriod.DAY) Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { (total.toFloat() / goal).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(), color = colors.primary, trackColor = colors.outlineVariant, strokeWidth = 5.dp)
                            Text("%${(total * 100 / goal).coerceAtMost(100)}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Metric("Tahmini mesafe", if (hasEstimate) "${decimal(distance / 1000)} km" else "--",
                            Icons.Default.Route, colors.primary, Modifier.weight(1f))
                        Metric("Tahmini aktif kcal", if (hasEstimate) integer(kotlin.math.round(calories).toLong()) else "--",
                            Icons.Default.LocalFireDepartment, colors.tertiary, Modifier.weight(1f))
                    }
                    if (period != ReportPeriod.DAY) {
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Metric("Günlük ortalama", integer(average), Icons.Default.BarChart, colors.secondary, Modifier.weight(1f))
                            Metric("Aktif gün", "$activeDays / ${report.days.size}", Icons.Default.DateRange, colors.primary, Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (!hasEstimate) TextButton(onClick = { editProfile = true }, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.PersonOutline, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Boy ve kilo ekle")
                    } else Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (report.profile?.calibratedStepCm != null) "Kişisel adım uzunluğu" else "Boydan tahmini adım uzunluğu",
                            Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        IconButton(onClick = { showModel = true }) { Icon(Icons.Default.Info, "Kalori hesabı hakkında", Modifier.size(20.dp)) }
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), color = colors.outlineVariant)
                Text(if (period == ReportPeriod.DAY) "Son 7 gün" else "Günlük dağılım",
                    Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.titleMedium)
                StepChart(report.chart, today)
            }
            if (period != ReportPeriod.DAY) items(report.days.asReversed(), key = { it.date }) { day ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = colors.background),
                    headlineContent = { Text(SimpleDateFormat("d MMMM, EEE", Turkish).format(WalkingDates.calendar(day.date).time)) },
                    supportingContent = { day.estimate?.let { Text("${decimal(it.meters / 1000)} km · ${integer(kotlin.math.round(it.activeKcal).toLong())} kcal tahmini") } },
                    trailingContent = { Text("${integer(day.steps)} adım", fontWeight = FontWeight.Medium) },
                    modifier = Modifier.clickable { selected = day.date; period = ReportPeriod.DAY })
                HorizontalDivider(Modifier.padding(horizontal = 24.dp), color = colors.outlineVariant)
            } else item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Günlük hedef", style = MaterialTheme.typography.titleSmall)
                        Text("${integer(goal.toLong())} adım", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        goal -= 500
                        StepCounterService.settings(context).edit().putInt("goal", goal).apply()
                    }, enabled = goal > 1000) { Icon(Icons.Default.Remove, "Hedefi azalt") }
                    IconButton(onClick = {
                        goal += 500
                        StepCounterService.settings(context).edit().putInt("goal", goal).apply()
                    }, enabled = goal < 30000) { Icon(Icons.Default.Add, "Hedefi artır") }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Icon(icon, null, Modifier.size(20.dp), tint = tint)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepChart(days: List<DailySteps>, today: String) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Canvas(Modifier.fillMaxWidth().height(112.dp).semantics {
            contentDescription = days.joinToString { "${it.date}: ${it.steps} adım" }
        }) {
            if (days.isEmpty()) return@Canvas
            val maximum = days.maxOf { it.steps }.coerceAtLeast(1)
            val slot = size.width / days.size
            drawLine(colors.outlineVariant, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            days.forEachIndexed { index, day ->
                val height = (size.height * day.steps.toFloat() / maximum).coerceAtLeast(2.dp.toPx())
                drawRoundRect(if (day.date > today || day.steps == 0L) colors.outlineVariant else if (day.date == today) colors.tertiary else colors.primary,
                    Offset(index * slot + slot * 0.2f, size.height - height), Size(slot * 0.6f, height),
                    androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            }
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val labelWidth = if (days.size <= 7) 36.dp else 24.dp
            days.forEachIndexed { index, day ->
                if (days.size <= 7 || index == 0 || index == days.lastIndex || ((index + 1) % 7 == 0 && days.lastIndex - index >= 3)) {
                    val position = (maxWidth * ((index + 0.5f) / days.size) - labelWidth / 2)
                        .coerceIn(0.dp, (maxWidth - labelWidth).coerceAtLeast(0.dp))
                    Text(if (days.size <= 7) SimpleDateFormat("EEE", Turkish).format(WalkingDates.calendar(day.date).time) else day.date.takeLast(2).toInt().toString(),
                        modifier = Modifier.offset(x = position).width(labelWidth), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditor(profile: WalkingProfile?, error: String?, onDismiss: () -> Unit, onSave: (WalkingProfile) -> Unit) {
    var height by rememberSaveable { mutableStateOf(profile?.heightCm?.let(::decimal) ?: "") }
    var weight by rememberSaveable { mutableStateOf(profile?.weightKg?.let(::decimal) ?: "") }
    var stride by rememberSaveable { mutableStateOf(profile?.calibratedStepCm?.let(::decimal) ?: "") }
    var distance by rememberSaveable { mutableStateOf("") }
    var steps by rememberSaveable { mutableStateOf("") }
    fun parse(value: String) = value.trim().replace(',', '.').toDoubleOrNull()
    val entered = WalkingProfile(parse(height) ?: Double.NaN, parse(weight) ?: Double.NaN, if (stride.isBlank()) null else parse(stride) ?: Double.NaN)
    val calibration = WalkingMetrics.calibratedStepCm(parse(distance) ?: Double.NaN, steps.toIntOrNull() ?: 0)
    BackHandler(onBack = onDismiss)
    Scaffold(topBar = {
        TopAppBar(title = { Text("Yürüyüş profili", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Vazgeç") } },
            actions = {
                TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(), tooltip = { PlainTooltip { Text("Kaydet") } }, state = rememberTooltipState()) {
                    IconButton(onClick = { onSave(entered) }, enabled = entered.valid) { Icon(Icons.Default.Check, "Kaydet") }
                }
            })
    }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(height, { height = it }, label = { Text("Boy (cm)") }, singleLine = true,
                    isError = height.isNotBlank() && (parse(height)?.let { it in 80.0..250.0 } != true),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(weight, { weight = it }, label = { Text("Kilo (kg)") }, singleLine = true,
                    isError = weight.isNotBlank() && (parse(weight)?.let { it in 20.0..350.0 } != true),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(stride, { stride = it }, label = { Text("Adım uzunluğu (cm)") }, singleLine = true,
                    placeholder = { Text("İsteğe bağlı") },
                    isError = stride.isNotBlank() && (parse(stride)?.let { it in 20.0..150.0 } != true),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                HorizontalDivider()
                Text("Mesafe kalibrasyonu", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(distance, { distance = it }, label = { Text("Ölçülmüş yürüyüş mesafesi (m)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(steps, { steps = it }, label = { Text("Bu mesafede elle sayılan adım") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                TextButton(enabled = calibration != null, onClick = { stride = decimal(calibration!!) }) {
                    Text(if (calibration != null) "${decimal(calibration)} cm uygula" else "Kalibrasyonu uygula")
                }
                Text(if (profile == null) "İlk profil, geçmiş kayıtların tahmininde de kullanılır."
                    else "Profil değişikliği bugün ve sonraki günler için geçerlidir.",
                    style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
    }
}
