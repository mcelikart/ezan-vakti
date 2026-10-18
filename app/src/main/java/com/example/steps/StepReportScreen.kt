package com.example.steps

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.LocalPatternIntensity
import com.example.ui.theme.LocalSelectedBackground
import com.example.ui.theme.LocalThemeColors
import com.example.ui.theme.drawCustomBackground
import com.example.ui.theme.navBarBottomPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Turkish = Locale.forLanguageTag("tr-TR")
private fun integer(value: Long) = NumberFormat.getIntegerInstance(Turkish).format(value)
private fun decimal(value: Double) = String.format(Turkish, "%.1f", value)

private data class WalkingReport(
    val days: List<DailySteps> = emptyList(),
    val chart: List<DailySteps> = emptyList(),
    val profile: WalkingProfile? = null,
    val hourly: List<Long> = List(24) { 0L },
    val bestDay: DailySteps? = null,
    val currentStreak: Int = 0,
    val lifetimeSteps: Long = 0L,
    val sessions: List<WalkingSessionRecord> = emptyList()
)

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
    var selectedPace by rememberSaveable { mutableStateOf(WalkingPacePreset.MODERATE) }

    // Live Walking Session state
    var sessionRunning by rememberSaveable { mutableStateOf(false) }
    var sessionPaused by rememberSaveable { mutableStateOf(false) }
    var sessionTitle by rememberSaveable { mutableStateOf("Camiye Yürüyüş") }
    var sessionStartMillis by rememberSaveable { mutableLongStateOf(0L) }
    var sessionBaseSteps by rememberSaveable { mutableLongStateOf(0L) }
    var sessionElapsedSec by rememberSaveable { mutableIntStateOf(0) }

    val revision by store.changes.collectAsStateWithLifecycle()
    val range = remember(selected, period) { WalkingDates.range(selected, period) }
    val report by produceState(WalkingReport(), range, today, selected, revision) {
        value = withContext(Dispatchers.IO) {
            val rows = store.range(range.start, range.end).associateBy { it.date }
            val days = range.days.filter { it <= today }.map { rows[it] ?: DailySteps(it, 0, store.profile(it)) }
            val chartRange = if (period == ReportPeriod.DAY) {
                (6 downTo 0).map { WalkingDates.shift(selected, ReportPeriod.DAY, -it) }
            } else range.days
            val chartRows = store.range(chartRange.first(), chartRange.last()).associateBy { it.date }
            val activeProfile = store.profile()
            val selectedDaySteps = store.day(selected)
            val rawHourly = store.hourlySteps(selected)
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val hourlyDist = WalkingMetrics.hourlyDistribution(
                daySteps = selectedDaySteps,
                recordedHours = rawHourly,
                isToday = selected == today,
                currentHour = currentHour
            )
            WalkingReport(
                days = days,
                chart = chartRange.map { chartRows[it] ?: DailySteps(it, 0, store.profile(it)) },
                profile = activeProfile,
                hourly = hourlyDist,
                bestDay = store.bestDay(),
                currentStreak = store.currentStreak(today, minSteps = 500L),
                lifetimeSteps = store.total(),
                sessions = store.sessions(8)
            )
        }
    }

    val total = report.days.sumOf { it.steps }
    val estimates = report.days.mapNotNull { it.estimate }
    val hasEstimate = report.profile != null
    val distance = estimates.sumOf { it.meters }
    val calories = estimates.sumOf { it.activeKcal }
    val activeDays = report.days.count { it.steps > 0 }
    val average = if (report.days.isEmpty()) 0 else total / report.days.size
    val activeMinutes = WalkingMetrics.activeMinutes(total, selectedPace.cadence)
    val speedKmh = WalkingMetrics.averageSpeedKmh(distance, activeMinutes)
    val waterMl = WalkingMetrics.hydrationNeedMl(calories, activeMinutes)
    val mosqueTrips = WalkingMetrics.mosqueTripsEquivalent(total)
    val colors = MaterialTheme.colorScheme
    val appTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val ornamentColor = themeColors.goldMotifColor
    val cardShape = appTheme.spec.shapes.cardShape
    val selectedBgId = LocalSelectedBackground.current
    val patternIntensity = LocalPatternIntensity.current
    val isDark = colors.background.luminance() < 0.5f

    // Live session timer
    LaunchedEffect(sessionRunning, sessionPaused) {
        while (isActive && sessionRunning && !sessionPaused) {
            delay(1_000L)
            sessionElapsedSec += 1
        }
    }

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
        ProfileEditor(
            profile = report.profile,
            error = profileError,
            onDismiss = { editProfile = false; profileError = null },
            onSave = { profile ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { store.saveProfile(profile) }
                        editProfile = false
                        profileError = null
                    } catch (_: Exception) {
                        profileError = "Profil kaydedilemedi. Yeniden deneyin."
                    }
                }
            }
        )
        return
    }
    if (showModel) AlertDialog(
        onDismissRequest = { showModel = false },
        title = { Text("Kalori ve Yürüyüş Analizi") },
        text = {
            Text(
                "Aktif kalori, dinlenme enerjisi hariç düz zeminde yürüyüş için ACSM metabolik denklemine göre hesaplanır. " +
                    "Mesafe, adım sayısı ve kişisel adım uzunluğuna dayanır. Ölçülmüş adım uzunluğu yoksa boydan (Boy × 0,415) tahmin edilir. " +
                    "Aktif süre ve su ihtiyacı seçili yürüyüş temposuna göre modellenir."
            )
        },
        confirmButton = { TextButton(onClick = { showModel = false }) { Text("Tamam") } }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawCustomBackground(
                bgId = selectedBgId,
                theme = appTheme,
                isDark = isDark,
                intensity = patternIntensity
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Yürüyüş", fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = CircleShape,
                                color = colors.primary.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, ornamentColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "Adımsayar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                        }
                    },
                    actions = {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text("Boy, kilo ve kalibrasyon") } },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = { editProfile = true }) {
                                Icon(Icons.Default.PersonOutline, "Yürüyüş profili")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = navBarBottomPadding(36.dp))
            ) {
                item {
                    // Sensor Status & Quick Control Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surface.copy(alpha = if (isDark) 0.82f else 0.92f)
                        ),
                        border = BorderStroke(1.dp, ornamentColor.copy(alpha = 0.28f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (enabled && permission && available)
                                                colors.primary.copy(alpha = 0.15f)
                                            else colors.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.DirectionsWalk,
                                        contentDescription = null,
                                        tint = if (enabled && permission && available) colors.primary else colors.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = when {
                                            !available -> "Adım sensörü bulunamadı"
                                            !permission -> "Fiziksel aktivite izni gerekli"
                                            enabled -> "Adım sayımı açık"
                                            else -> "Adım sayımı duraklatıldı"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (available && permission && enabled) {
                                        Text(
                                            text = StepCounterService.settings(context).getString("sensor_mode", "Donanım sensörü") ?: "Donanım sensörü",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = enabled && permission && available,
                                    enabled = available,
                                    onCheckedChange = {
                                        if (it) {
                                            if (permission) activate() else permissionRequest.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                        } else {
                                            StepCounterService.settings(context).edit().putBoolean("enabled", false).apply()
                                            enabled = false
                                            context.stopService(Intent(context, StepCounterService::class.java))
                                        }
                                    }
                                )
                            }
                            if ((!permission && available) || !notifications) {
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!permission && available) {
                                        TextButton(
                                            onClick = {
                                                context.startActivity(
                                                    Intent(
                                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                        Uri.parse("package:${context.packageName}")
                                                    )
                                                )
                                            }
                                        ) { Text("Uygulama izinleri") }
                                    }
                                    if (!notifications) {
                                        TextButton(
                                            onClick = {
                                                if (Build.VERSION.SDK_INT >= 33) notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                else context.startActivity(
                                                    Intent(
                                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                        Uri.parse("package:${context.packageName}")
                                                    )
                                                )
                                            }
                                        ) { Text("Bildirim izni") }
                                    }
                                }
                            }
                        }
                    }

                    // Period Selector Tabs (Must retain Tab semantics for StepReportScreenshotTest)
                    TabRow(
                        selectedTabIndex = period.ordinal,
                        containerColor = Color.Transparent,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        ReportPeriod.entries.forEach { item ->
                            Tab(
                                selected = item == period,
                                onClick = { period = item },
                                text = {
                                    Text(
                                        text = item.title,
                                        fontWeight = if (item == period) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }

                    // Date Navigation Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selected = WalkingDates.shift(selected, period, -1) }) {
                            Icon(Icons.Default.ChevronLeft, "Önceki dönem")
                        }
                        val label = when (period) {
                            ReportPeriod.DAY -> SimpleDateFormat("d MMMM yyyy", Turkish).format(WalkingDates.calendar(selected).time)
                            ReportPeriod.WEEK -> {
                                val format = SimpleDateFormat("d MMM", Turkish)
                                "${format.format(WalkingDates.calendar(range.start).time)} - ${format.format(WalkingDates.calendar(range.end).time)}"
                            }
                            ReportPeriod.MONTH -> SimpleDateFormat("MMMM yyyy", Turkish).format(WalkingDates.calendar(selected).time)
                        }
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { selected = WalkingDates.shift(selected, period, 1).coerceAtMost(today) },
                            enabled = range.end < today
                        ) {
                            Icon(Icons.Default.ChevronRight, "Sonraki dönem")
                        }
                    }

                    // HERO MULTI-RING GAUGE & METRICS CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surface.copy(alpha = if (isDark) 0.88f else 0.96f)
                        ),
                        border = BorderStroke(1.2.dp, ornamentColor.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            val periodGoal = when (period) {
                                ReportPeriod.DAY -> goal.toLong()
                                ReportPeriod.WEEK -> goal.toLong() * 7L
                                ReportPeriod.MONTH -> goal.toLong() * report.days.size.coerceAtLeast(1)
                            }.coerceAtLeast(1L)

                            HeroMultiRingGauge(
                                totalSteps = total,
                                goalSteps = periodGoal,
                                distanceMeters = distance,
                                activeKcal = calories,
                                activeMinutes = activeMinutes,
                                isDayPeriod = period == ReportPeriod.DAY,
                                streakDays = report.currentStreak
                            )

                            Spacer(Modifier.height(18.dp))

                            // Pace Selector Strip
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WalkingPacePreset.entries.forEach { pace ->
                                    val isSelected = pace == selectedPace
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedPace = pace },
                                        label = {
                                            Text(
                                                text = pace.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    Icons.Default.Speed,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Primary Metrics Row (Preserves exact "Tahmini mesafe" and "163" / "Tahmini aktif kcal" nodes)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Metric(
                                    label = "Tahmini mesafe",
                                    value = if (hasEstimate) "${decimal(distance / 1000)} km" else "--",
                                    subtitle = if (hasEstimate && report.profile != null) {
                                        "Adım: ${decimal(report.profile!!.estimatedStepCm)} cm"
                                    } else "Profil gerekli",
                                    icon = Icons.Default.Route,
                                    tint = colors.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Metric(
                                    label = "Tahmini aktif kcal",
                                    value = if (hasEstimate) integer(kotlin.math.round(calories).toLong()) else "--",
                                    subtitle = if (hasEstimate) "kcal aktif enerji" else "Boy & kilo ekleyin",
                                    icon = Icons.Default.LocalFireDepartment,
                                    tint = colors.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Extended Wellness Metrics Row (Active Time, Speed, Hydration, Mosque Trips)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Metric(
                                    label = "Aktif yürüyüş süresi",
                                    value = "$activeMinutes dk",
                                    subtitle = if (hasEstimate && speedKmh > 0) "Ort. hız: ${decimal(speedKmh)} km/sa" else selectedPace.label,
                                    icon = Icons.Default.Timer,
                                    tint = colors.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Metric(
                                    label = "Su desteği & Bereket",
                                    value = "+$waterMl ml",
                                    subtitle = if (mosqueTrips > 0) "~$mosqueTrips cami seferi mesafesi" else "Yürüyüş su ihtiyacı",
                                    icon = Icons.Default.WaterDrop,
                                    tint = ornamentColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (period != ReportPeriod.DAY) {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Metric(
                                        label = "Günlük ortalama",
                                        value = integer(average),
                                        subtitle = "adım / gün",
                                        icon = Icons.Default.BarChart,
                                        tint = colors.secondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Metric(
                                        label = "Aktif gün",
                                        value = "$activeDays / ${report.days.size}",
                                        subtitle = "Hedef: ${report.days.count { it.steps >= goal }} gün ulaşıldı",
                                        icon = Icons.Default.DateRange,
                                        tint = colors.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            if (!hasEstimate) {
                                FilledTonalButton(
                                    onClick = { editProfile = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PersonOutline, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Boy ve kilo ekle")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (report.profile?.calibratedStepCm != null)
                                            "Kişisel adım uzunluğu (${decimal(report.profile!!.calibratedStepCm!!)} cm)"
                                        else
                                            "Boydan tahmini adım uzunluğu (${decimal(report.profile!!.estimatedStepCm)} cm)",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                    IconButton(onClick = { showModel = true }) {
                                        Icon(Icons.Default.Info, "Kalori hesabı hakkında", Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }

                    // INTERACTIVE BAR + TREND CHART CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surface.copy(alpha = if (isDark) 0.86f else 0.95f)
                        ),
                        border = BorderStroke(1.dp, ornamentColor.copy(alpha = 0.3f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (period == ReportPeriod.DAY) "Son 7 gün" else "Günlük dağılım",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Detay görmek için grafikteki sütunlara dokunun",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                val goalMetCount = report.chart.count { it.steps >= goal }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = colors.tertiaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = colors.tertiary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "$goalMetCount/${report.chart.size} Hedef",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onTertiaryContainer
                                        )
                                    }
                                }
                            }

                            StepChart(
                                days = report.chart,
                                today = today,
                                goal = goal,
                                onSelectDay = { clickedDate ->
                                    if (clickedDate <= today) {
                                        selected = clickedDate
                                    }
                                }
                            )
                        }
                    }

                    // 24-HOUR INTRADAY SKYLINE CHART (Day View) OR CONSISTENCY HEATMAP (Week/Month View)
                    if (period == ReportPeriod.DAY) {
                        HourlyStepSkylineCard(
                            hourlySteps = report.hourly,
                            totalSteps = total
                        )
                    } else {
                        PeriodConsistencyHeatmapCard(
                            days = report.days,
                            goal = goal,
                            selectedDate = selected,
                            onSelectDay = { clicked ->
                                selected = clicked
                                period = ReportPeriod.DAY
                            }
                        )
                    }
                }

                // DAILY BREAKDOWN ITEMS (Week / Month) OR GOAL & ADVANCED FEATURES (Day)
                if (period != ReportPeriod.DAY) {
                    item {
                        Text(
                            text = "Günlük Kayıt Listesi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(report.days.asReversed(), key = { it.date }) { day ->
                        val dayRatio = (day.steps.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { dayRatio },
                                        modifier = Modifier.fillMaxSize(),
                                        strokeWidth = 3.5.dp,
                                        color = if (day.steps >= goal) colors.tertiary else colors.primary,
                                        trackColor = colors.outlineVariant.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = "%${(day.steps * 100 / goal.coerceAtLeast(1)).coerceAtMost(100)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    SimpleDateFormat("d MMMM, EEE", Turkish).format(WalkingDates.calendar(day.date).time),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            supportingContent = {
                                val mins = WalkingMetrics.activeMinutes(day.steps, selectedPace.cadence)
                                val est = day.estimate
                                if (est != null) {
                                    Text("${decimal(est.meters / 1000)} km · ${integer(kotlin.math.round(est.activeKcal).toLong())} kcal tahmini · $mins dk")
                                } else {
                                    Text("Yaklaşık $mins dk aktif yürüyüş")
                                }
                            },
                            trailingContent = {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${integer(day.steps)} adım", fontWeight = FontWeight.Bold, color = colors.primary)
                                    if (day.steps >= goal) {
                                        Text(
                                            "Hedef Tamam",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.tertiary
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.clickable {
                                selected = day.date
                                period = ReportPeriod.DAY
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
                    }
                } else {
                    item {
                        // DAILY GOAL CARD WITH PRESETS & +/- CONTROLS
                        DailyGoalSelectorCard(
                            goal = goal,
                            onChangeGoal = { newGoal ->
                                val clamped = newGoal.coerceIn(1000, 30000)
                                goal = clamped
                                StepCounterService.settings(context).edit().putInt("goal", clamped).apply()
                            }
                        )

                        // LIVE WALKING SESSION / LAP TRACKER CARD
                        LiveWalkingSessionCard(
                            currentTodaySteps = report.days.lastOrNull()?.steps ?: total,
                            profile = report.profile,
                            sessionRunning = sessionRunning,
                            sessionPaused = sessionPaused,
                            sessionTitle = sessionTitle,
                            sessionElapsedSec = sessionElapsedSec,
                            sessionBaseSteps = sessionBaseSteps,
                            savedSessions = report.sessions,
                            onTitleChange = { sessionTitle = it },
                            onStart = {
                                sessionStartMillis = System.currentTimeMillis()
                                sessionBaseSteps = report.days.lastOrNull()?.steps ?: total
                                sessionElapsedSec = 0
                                sessionPaused = false
                                sessionRunning = true
                            },
                            onTogglePause = { sessionPaused = !sessionPaused },
                            onStopAndSave = {
                                val currentStepsNow = report.days.lastOrNull()?.steps ?: total
                                val deltaSteps = (currentStepsNow - sessionBaseSteps).coerceAtLeast(0L)
                                val est = WalkingMetrics.estimate(
                                    deltaSteps,
                                    report.profile ?: WalkingProfile(172.0, 72.0)
                                )
                                val meters = est?.meters ?: (deltaSteps * 0.72)
                                val kcal = est?.activeKcal ?: (meters * 72.0 * 0.0005)
                                val titleToSave = sessionTitle.ifBlank { "Yürüyüş Turu" }
                                val started = if (sessionStartMillis > 0L) sessionStartMillis else System.currentTimeMillis()
                                val dur = sessionElapsedSec.coerceAtLeast(1)
                                scope.launch(Dispatchers.IO) {
                                    store.saveSession(titleToSave, started, dur, deltaSteps, meters, kcal)
                                }
                                sessionRunning = false
                                sessionPaused = false
                                sessionElapsedSec = 0
                            },
                            onDeleteSession = { id ->
                                scope.launch(Dispatchers.IO) { store.deleteSession(id) }
                            }
                        )

                        // STREAKS, RECORDS & ACHIEVEMENT BADGES CARD
                        RecordsAndMilestonesCard(
                            bestDay = report.bestDay,
                            currentStreak = report.currentStreak,
                            lifetimeSteps = report.lifetimeSteps,
                            dailyGoal = goal,
                            profile = report.profile
                        )

                        // MOSQUE WALK & SPIRITUAL HEALTH INSIGHT CARD
                        MosqueWalkSpiritualCard(
                            steps = total,
                            mosqueTrips = mosqueTrips
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMultiRingGauge(
    totalSteps: Long,
    goalSteps: Long,
    distanceMeters: Double,
    activeKcal: Double,
    activeMinutes: Int,
    isDayPeriod: Boolean,
    streakDays: Int
) {
    val colors = MaterialTheme.colorScheme
    val themeColors = LocalThemeColors.current
    val ornamentColor = themeColors.goldMotifColor
    val stepProgress by animateFloatAsState(
        targetValue = (totalSteps.toFloat() / goalSteps.coerceAtLeast(1L)).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "stepProgress"
    )
    val distanceGoalMeters = (goalSteps * 0.72).coerceAtLeast(1000.0)
    val distProgress by animateFloatAsState(
        targetValue = (distanceMeters / distanceGoalMeters).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "distProgress"
    )
    val kcalGoal = if (isDayPeriod) 250.0 else 1500.0
    val kcalProgress by animateFloatAsState(
        targetValue = (activeKcal / kcalGoal).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "kcalProgress"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Status Badges (Streak & Goal Percent)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = colors.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = colors.tertiary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (streakDays > 0) "$streakDays Gün Seri" else "Yürüyüş Serisi Başlat",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = colors.secondaryContainer.copy(alpha = 0.65f)
            ) {
                Text(
                    text = "%${(totalSteps * 100 / goalSteps.coerceAtLeast(1L)).coerceAtMost(100)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Centerpiece Multi-Ring Radial Dial
        Box(
            modifier = Modifier.size(216.dp),
            contentAlignment = Alignment.Center
        ) {
            val primaryColor = colors.primary
            val secondaryColor = colors.secondary
            val tertiaryColor = colors.tertiary
            val trackColor = colors.outlineVariant.copy(alpha = 0.32f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeOuter = 13.dp.toPx()
                val strokeMid = 9.dp.toPx()
                val strokeInner = 6.dp.toPx()
                val gap = 7.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                // Subtle radial glow behind dial
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.14f), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 2f
                    ),
                    radius = size.minDimension / 2f,
                    center = center
                )

                // 28 subtle ornamental dial ticks around the outer perimeter
                val outerRadius = (size.minDimension - strokeOuter) / 2f
                val totalTicks = 28
                for (i in 0..totalTicks) {
                    val angleDeg = 135f + (270f * i / totalTicks)
                    val rad = angleDeg * (PI / 180.0)
                    val isMilestone = i % 7 == 0
                    val tickInner = outerRadius + strokeOuter / 2f + 2.dp.toPx()
                    val tickOuter = tickInner + if (isMilestone) 5.dp.toPx() else 2.5.dp.toPx()
                    val p1 = Offset(
                        center.x + (tickInner * cos(rad)).toFloat(),
                        center.y + (tickInner * sin(rad)).toFloat()
                    )
                    val p2 = Offset(
                        center.x + (tickOuter * cos(rad)).toFloat(),
                        center.y + (tickOuter * sin(rad)).toFloat()
                    )
                    drawLine(
                        color = if (i.toFloat() / totalTicks <= stepProgress) ornamentColor else trackColor,
                        start = p1,
                        end = p2,
                        strokeWidth = if (isMilestone) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Outer Ring: Steps Goal
                val outerTopLeft = Offset(strokeOuter / 2f + 6.dp.toPx(), strokeOuter / 2f + 6.dp.toPx())
                val outerSize = Size(size.width - strokeOuter - 12.dp.toPx(), size.height - strokeOuter - 12.dp.toPx())
                drawArc(
                    color = trackColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = outerTopLeft,
                    size = outerSize,
                    style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                )
                if (stepProgress > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(primaryColor, ornamentColor, primaryColor),
                            center = center
                        ),
                        startAngle = 135f,
                        sweepAngle = 270f * stepProgress,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerSize,
                        style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                    )
                    // Glowing endpoint orb on outer ring
                    val tipAngleRad = (135f + 270f * stepProgress) * (PI / 180.0)
                    val rOuter = outerSize.width / 2f
                    val tipOffset = Offset(
                        center.x + (rOuter * cos(tipAngleRad)).toFloat(),
                        center.y + (rOuter * sin(tipAngleRad)).toFloat()
                    )
                    drawCircle(color = Color.White, radius = strokeOuter * 0.34f, center = tipOffset)
                }

                // Middle Ring: Distance
                val midInset = strokeOuter + gap + 6.dp.toPx()
                val midTopLeft = Offset(midInset, midInset)
                val midSize = Size(size.width - midInset * 2f, size.height - midInset * 2f)
                drawArc(
                    color = trackColor.copy(alpha = 0.22f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = midTopLeft,
                    size = midSize,
                    style = Stroke(width = strokeMid, cap = StrokeCap.Round)
                )
                if (distProgress > 0f) {
                    drawArc(
                        color = secondaryColor,
                        startAngle = 135f,
                        sweepAngle = 270f * distProgress,
                        useCenter = false,
                        topLeft = midTopLeft,
                        size = midSize,
                        style = Stroke(width = strokeMid, cap = StrokeCap.Round)
                    )
                }

                // Inner Ring: Active Kcal
                val innerInset = midInset + strokeMid + gap
                val innerTopLeft = Offset(innerInset, innerInset)
                val innerSize = Size(size.width - innerInset * 2f, size.height - innerInset * 2f)
                drawArc(
                    color = trackColor.copy(alpha = 0.16f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerSize,
                    style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                )
                if (kcalProgress > 0f) {
                    drawArc(
                        color = tertiaryColor,
                        startAngle = 135f,
                        sweepAngle = 270f * kcalProgress,
                        useCenter = false,
                        topLeft = innerTopLeft,
                        size = innerSize,
                        style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                    )
                }
            }

            // Center Typography inside Gauge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = integer(totalSteps),
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "adım",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                val remaining = (goalSteps - totalSteps).coerceAtLeast(0L)
                Text(
                    text = if (remaining == 0L) "Hedef Aşıldı! ★" else "Hedefe ${integer(remaining)} kaldı",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remaining == 0L) colors.tertiary else colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Ring Legend Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RingLegendDot(color = colors.primary, label = "Adım Hedefi")
            Spacer(Modifier.width(14.dp))
            RingLegendDot(color = colors.secondary, label = "Mesafe")
            Spacer(Modifier.width(14.dp))
            RingLegendDot(color = colors.tertiary, label = "Aktif Kalori ($activeMinutes dk)")
        }
    }
}

@Composable
private fun RingLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, Modifier.size(16.dp), tint = tint)
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StepChart(
    days: List<DailySteps>,
    today: String,
    goal: Int = 6000,
    onSelectDay: (String) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val themeColors = LocalThemeColors.current
    val ornamentColor = themeColors.goldMotifColor
    var highlightedIndex by remember(days) {
        mutableIntStateOf(days.indexOfLast { it.date <= today && it.steps > 0 }.coerceAtLeast(0))
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        // Selected Bar Detail Pill
        if (days.isNotEmpty() && highlightedIndex in days.indices) {
            val item = days[highlightedIndex]
            val itemEst = item.estimate
            val itemDateText = SimpleDateFormat("d MMMM EEE", Turkish).format(WalkingDates.calendar(item.date).time)
            val pct = (item.steps * 100 / goal.coerceAtLeast(1)).coerceAtMost(999)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { onSelectDay(item.date) },
                shape = RoundedCornerShape(12.dp),
                color = colors.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = itemDateText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = buildString {
                                append("${integer(item.steps)} adım (%$pct hedef)")
                                if (itemEst != null) {
                                    append(" · ${decimal(itemEst.meters / 1000)} km · ${integer(kotlin.math.round(itemEst.activeKcal).toLong())} kcal")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    if (item.steps >= goal) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            tint = colors.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(148.dp)
                .pointerInput(days) {
                    detectTapGestures { offset ->
                        if (days.isNotEmpty() && size.width > 0) {
                            val slot = size.width.toFloat() / days.size
                            val idx = (offset.x / slot).toInt().coerceIn(0, days.lastIndex)
                            highlightedIndex = idx
                            onSelectDay(days[idx].date)
                        }
                    }
                }
                .semantics {
                    contentDescription = days.joinToString { "${it.date}: ${it.steps} adım" }
                }
        ) {
            if (days.isEmpty()) return@Canvas
            val maxSteps = maxOf(days.maxOf { it.steps }, goal.toLong(), 1000L)
            val topPad = 14.dp.toPx()
            val chartHeight = (size.height - topPad).coerceAtLeast(10f)
            val slot = size.width / days.size

            // Horizontal subtle grid lines
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            for (level in 1..3) {
                val y = topPad + chartHeight * (1f - level / 4f)
                drawLine(
                    color = colors.outlineVariant.copy(alpha = 0.28f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Dashed Goal Reference Line
            val goalRatio = (goal.toFloat() / maxSteps).coerceIn(0.05f, 1f)
            val goalY = topPad + chartHeight * (1f - goalRatio)
            drawLine(
                color = colors.tertiary.copy(alpha = 0.75f),
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashEffect
            )

            // Baseline
            drawLine(
                color = colors.outlineVariant,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.2.dp.toPx()
            )

            // Draw Rounded Gradient Bars + Goal Crown Markers
            val points = mutableListOf<Offset>()
            days.forEachIndexed { index, day ->
                val ratio = (day.steps.toFloat() / maxSteps).coerceIn(0f, 1f)
                val barHeight = (chartHeight * ratio).coerceAtLeast(3.dp.toPx())
                val barWidth = (slot * 0.58f).coerceAtMost(28.dp.toPx())
                val left = index * slot + (slot - barWidth) / 2f
                val top = size.height - barHeight
                val isSelectedBar = index == highlightedIndex
                val reachedGoal = day.steps >= goal

                val barColorTop = when {
                    day.date > today || day.steps == 0L -> colors.outlineVariant.copy(alpha = 0.45f)
                    reachedGoal -> colors.tertiary
                    day.date == today -> ornamentColor
                    else -> colors.primary
                }
                val barColorBottom = barColorTop.copy(alpha = if (day.steps == 0L) 0.25f else 0.55f)

                if (isSelectedBar) {
                    drawRoundRect(
                        color = colors.primary.copy(alpha = 0.12f),
                        topLeft = Offset(index * slot + 2.dp.toPx(), 0f),
                        size = Size((slot - 4.dp.toPx()).coerceAtLeast(4f), size.height),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(barColorTop, barColorBottom),
                        startY = top,
                        endY = size.height
                    ),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                // Crown star dot when goal is reached
                if (reachedGoal && day.date <= today) {
                    drawCircle(
                        color = ornamentColor,
                        radius = 3.5.dp.toPx(),
                        center = Offset(index * slot + slot * 0.5f, (top - 6.dp.toPx()).coerceAtLeast(4.dp.toPx()))
                    )
                }

                if (day.date <= today) {
                    points.add(Offset(index * slot + slot * 0.5f, top))
                }
            }

            // Smooth Bezier Trend Line connecting active days
            if (points.size >= 2) {
                val trendPath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.lastIndex) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val ctrlX = (p0.x + p1.x) / 2f
                        cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                    }
                }
                drawPath(
                    path = trendPath,
                    color = ornamentColor.copy(alpha = 0.85f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val labelWidth = if (days.size <= 7) 38.dp else 24.dp
            days.forEachIndexed { index, day ->
                if (days.size <= 7 || index == 0 || index == days.lastIndex || ((index + 1) % 7 == 0 && days.lastIndex - index >= 3)) {
                    val position = (maxWidth * ((index + 0.5f) / days.size) - labelWidth / 2)
                        .coerceIn(0.dp, (maxWidth - labelWidth).coerceAtLeast(0.dp))
                    val isSelectedLabel = index == highlightedIndex
                    Text(
                        text = if (days.size <= 7)
                            SimpleDateFormat("EEE", Turkish).format(WalkingDates.calendar(day.date).time)
                        else
                            day.date.takeLast(2).toInt().toString(),
                        modifier = Modifier
                            .offset(x = position)
                            .width(labelWidth),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelectedLabel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelectedLabel) colors.primary else colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyStepSkylineCard(
    hourlySteps: List<Long>,
    totalSteps: Long
) {
    val colors = MaterialTheme.colorScheme
    val appTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val ornamentColor = themeColors.goldMotifColor
    val cardShape = appTheme.spec.shapes.cardShape
    val isDark = colors.background.luminance() < 0.5f

    val peakHour = remember(hourlySteps) {
        hourlySteps.indices.maxByOrNull { hourlySteps[it] } ?: 12
    }
    val peakSteps = hourlySteps.getOrElse(peakHour) { 0L }
    val morningSteps = hourlySteps.slice(5..11).sum()
    val middaySteps = hourlySteps.slice(12..17).sum()
    val eveningSteps = (hourlySteps.slice(18..23).sum() + hourlySteps.slice(0..4).sum())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = if (isDark) 0.86f else 0.95f)
        ),
        border = BorderStroke(1.dp, ornamentColor.copy(alpha = 0.28f))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Saatlik Aktivite Dağılımı (24 Saat)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (peakSteps > 0)
                            "En aktif saat dilimi: %02d:00 (%s adım)".format(peakHour, integer(peakSteps))
                        else
                            "Yürüyüş başladığında saatlik yoğunluk burada görünür",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
            ) {
                val maxH = hourlySteps.maxOrNull()?.coerceAtLeast(1L) ?: 1L
                val slot = size.width / 24f
                drawLine(
                    color = colors.outlineVariant.copy(alpha = 0.5f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                hourlySteps.forEachIndexed { hour, s ->
                    val ratio = (s.toFloat() / maxH).coerceIn(0f, 1f)
                    val hPx = (size.height * ratio).coerceAtLeast(2.dp.toPx())
                    val barColor = when {
                        s == 0L -> colors.outlineVariant.copy(alpha = 0.35f)
                        hour == peakHour -> colors.tertiary
                        hour in 5..11 -> colors.primary
                        hour in 12..17 -> colors.secondary
                        else -> ornamentColor
                    }
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(hour * slot + slot * 0.18f, size.height - hPx),
                        size = Size(slot * 0.64f, hPx),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("00:00", "06:00", "12:00", "18:00", "23:00").forEach { t ->
                    Text(t, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }

            if (totalSteps > 0) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeZoneSummaryChip(
                        title = "Sabah (05-12)",
                        steps = morningSteps,
                        color = colors.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TimeZoneSummaryChip(
                        title = "Öğle/İkindi",
                        steps = middaySteps,
                        color = colors.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    TimeZoneSummaryChip(
                        title = "Akşam/Yatsı",
                        steps = eveningSteps,
                        color = colors.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeZoneSummaryChip(
    title: String,
    steps: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = integer(steps),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun PeriodConsistencyHeatmapCard(
    days: List<DailySteps>,
    goal: Int,
    selectedDate: String,
    onSelectDay: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val appTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val ornamentColor = themeColors.goldMotifColor
    val cardShape = appTheme.spec.shapes.cardShape
    val isDark = colors.background.luminance() < 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = if (isDark) 0.86f else 0.95f)
        ),
        border = BorderStroke(1.dp, ornamentColor.copy(alpha = 0.28f))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(
                text = "Aktivite İstikrar Haritası",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Renk yoğunluğu günlük hedefe yaklaşım oranını gösterir",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            val rows = days.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { weekChunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weekChunk.forEach { day ->
                            val ratio = (day.steps.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val cellColor = when {
                                day.steps == 0L -> colors.surfaceVariant.copy(alpha = 0.45f)
                                ratio >= 1f -> colors.tertiary
                                ratio >= 0.5f -> colors.primary.copy(alpha = 0.78f)
                                else -> colors.primary.copy(alpha = 0.36f)
                            }
                            val dayNum = day.date.takeLast(2).toIntOrNull() ?: 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cellColor)
                                    .border(
                                        width = if (day.date == selectedDate) 2.dp else 0.dp,
                                        color = if (day.date == selectedDate) ornamentColor else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectDay(day.date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ratio >= 0.5f) colors.onPrimary else colors.onSurface
                                )
                            }
                        }
                        repeat((7 - weekChunk.size).coerceAtLeast(0)) {
                            Spacer(Modifier.weight(1f).height(36.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyGoalSelectorCard(
    goal: Int,
    onChangeGoal: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val appTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val ornamentColor = themeColors.goldMotifColor
    val cardShape = appTheme.spec.shapes.cardShape
    val isDark = colors.background.luminance() < 0.5f
    val presets = listOf(
        4000 to "Hafif",
        6000 to "Dengeli",
        8000 to "Aktif",
        10000 to "Formda",
        12500 to "Performans"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = if (isDark) 0.86f else 0.95f)
        ),
        border = BorderStroke(1.dp, ornamentColor.copy(alpha = 0.28f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Günlük hedef", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${integer(goal.toLong())} adım", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                IconButton(
                    onClick = { onChangeGoal(goal - 500) },
                    enabled = goal > 1000
                ) {
                    Icon(Icons.Default.Remove, "Hedefi azalt")
                }
                IconButton(
                    onClick = { onChangeGoal(goal + 500) },
                    enabled = goal < 30000
                ) {
                    Icon(Icons.Default.Add, "Hedefi artır")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (presetSteps, presetTitle) ->
                    val selected = goal == presetSteps
                    FilterChip(
                        selected = selected,
                        onClick = { onChangeGoal(presetSteps) },
                        label = {
                            Text(
                                "$presetTitle (${integer(presetSteps.toLong())})",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveWalkingSessionCard(
    currentTodaySteps: Long,
    profile: WalkingProfile?,
    sessionRunning: Boolean,
    sessionPaused: Boolean,
    sessionTitle: String,
    sessionElapsedSec: Int,
    sessionBaseSteps: Long,
    savedSessions: List<WalkingSessionRecord>,
    onTitleChange: (String) -> Unit,
    onStart: () -> Unit,
    onTogglePause: () -> Unit,
    onStopAndSave: () -> Unit,
    onDeleteSession: (Long) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val appTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val ornamentColor = themeColors.goldMotifColor
    val cardShape = appTheme.spec.shapes.cardShape
    val isDark = colors.background.luminance() < 0.5f
    val sessionSteps = if (sessionRunning) (currentTodaySteps - sessionBaseSteps).coerceAtLeast(0L) else 0L
    val effProfile = profile ?: WalkingProfile(172.0, 72.0)
    val sessionEst = WalkingMetrics.estimate(sessionSteps, effProfile)
    val sessionMeters = sessionEst?.meters ?: 0.0
    val sessionKcal = sessionEst?.activeKcal ?: 0.0
    val mins = sessionElapsedSec / 60
    val secs = sessionElapsedSec % 60
    val timerFormatted = "%02d:%02d".format(mins, secs)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = if (isDark) 0.86f else 0.95f)
        ),
        border = BorderStroke(1.dp, ornamentColor.copy(alpha = 0.28f))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = colors.primary)
                    Column {
                        Text(
                            text = "Canlı Yürüyüş & Tur Sayacı",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cami yürüyüşü veya egzersiz turlarınızı ayrı kaydedin",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Session Title Preset Chips
            val sessionTypes = listOf("Camiye Yürüyüş", "Sabah Yürüyüşü", "Tempolu Antrenman", "Akşam Turu")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sessionTypes.forEach { preset ->
                    FilterChip(
                        selected = sessionTitle == preset,
                        onClick = { onTitleChange(preset) },
                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (sessionRunning) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = sessionTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = timerFormatted,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(integer(sessionSteps), fontWeight = FontWeight.Bold)
                                Text("Tur Adımı", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${decimal(sessionMeters)} m", fontWeight = FontWeight.Bold)
                                Text("Mesafe", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${decimal(sessionKcal)} kcal", fontWeight = FontWeight.Bold)
                                Text("Aktif Enerji", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = onTogglePause) {
                                Icon(
                                    if (sessionPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (sessionPaused) "Devam Et" else "Duraklat")
                            }
                            Button(onClick = onStopAndSave) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Turu Kaydet")
                            }
                        }
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("$sessionTitle Başlat")
                }
            }

            if (savedSessions.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Son Kaydedilen Yürüyüş Turları",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                savedSessions.take(4).forEach { rec ->
                    val dateStr = SimpleDateFormat("d MMM HH:mm", Turkish).format(Date(rec.startTimeMillis))
                    val durMin = rec.durationSec / 60
                    val durSec = rec.durationSec % 60
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "${rec.title} · $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${integer(rec.steps)} adım · ${decimal(rec.meters)} m · %02d:%02d dk".format(durMin, durSec),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteSession(rec.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Turu sil",
                                modifier = Modifier.size(15.dp),
                                tint = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsAndMilestonesCard(
    bestDay: DailySteps?,
    currentStreak: Int,
    lifetimeSteps: Long,
    dailyGoal: Int,
    profile: WalkingProfile?
) {
    val colors = MaterialTheme.colorScheme
    val appTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val ornamentColor = themeColors.goldMotifColor
    val cardShape = appTheme.spec.shapes.cardShape
    val isDark = colors.background.luminance() < 0.5f
    val bestSteps = bestDay?.steps ?: 0L
    val milestones = remember(lifetimeSteps, bestSteps, currentStreak, dailyGoal) {
        WalkingMetrics.buildMilestones(lifetimeSteps, bestSteps, currentStreak, dailyGoal)
    }
    val lifetimeKm = ((lifetimeSteps * (profile?.stepMeters ?: 0.72)) / 1000.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = if (isDark) 0.86f else 0.95f)
        ),
        border = BorderStroke(1.dp, ornamentColor.copy(alpha = 0.28f))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = colors.tertiary)
                Column {
                    Text(
                        text = "Kişisel Rekorlar & Başarı Rozetleri",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Toplam ${integer(lifetimeSteps)} adım (~${decimal(lifetimeKm)} km) yürüyüş birikimi",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Personal Best Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.tertiaryContainer.copy(alpha = 0.38f),
                border = BorderStroke(1.dp, colors.tertiary.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "En Yüksek Günlük Rekor",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = if (bestDay != null) "${integer(bestDay.steps)} adım" else "Henüz kayıt yok",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.onSurface
                        )
                    }
                    if (bestDay != null) {
                        Text(
                            text = SimpleDateFormat("d MMM yyyy", Turkish).format(WalkingDates.calendar(bestDay.date).time),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.tertiary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Milestones Grid (2x2)
            milestones.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { badge ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (badge.achieved)
                                colors.primaryContainer.copy(alpha = 0.5f)
                            else
                                colors.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (badge.achieved) ornamentColor.copy(alpha = 0.6f)
                                else colors.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (badge.achieved) Icons.Default.WorkspacePremium else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (badge.achieved) colors.tertiary else colors.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = badge.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = badge.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 2
                                )
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { badge.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape),
                                    color = if (badge.achieved) colors.tertiary else colors.primary,
                                    trackColor = colors.outlineVariant.copy(alpha = 0.35f)
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = badge.targetText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (badge.achieved) colors.tertiary else colors.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MosqueWalkSpiritualCard(
    steps: Long,
    mosqueTrips: Int
) {
    val colors = MaterialTheme.colorScheme
    val appTheme = LocalAppTheme.current
    val themeColors = LocalThemeColors.current
    val ornamentColor = themeColors.goldMotifColor
    val cardShape = appTheme.spec.shapes.cardShape
    val isDark = colors.background.luminance() < 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = if (isDark) 0.86f else 0.95f)
        ),
        border = BorderStroke(1.dp, ornamentColor.copy(alpha = 0.35f))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Mosque, contentDescription = null, tint = colors.primary)
                Text(
                    text = "Camiye Yürüyüş & Adım Bereketi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "“Kim evinde güzelce temizlenir, sonra Allah’ın farzlarından bir farzı eda etmek için Allah’ın evlerinden birine yürürse, attığı iki adımdan biri günahını siler, diğeri ise derecesini yükseltir.” (Müslim, Mesâcid 282)",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            if (steps > 0) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (mosqueTrips > 0)
                            "Bugünkü ${integer(steps)} adımınız, ortalama mahalle mesafesiyle yaklaşık $mosqueTrips kez camiye gidiş-dönüş yürüyüşüne karşılık geliyor."
                        else
                            "Bugünkü ${integer(steps)} adımınızla yürüyüş bereketini başlattınız.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditor(
    profile: WalkingProfile?,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (WalkingProfile) -> Unit
) {
    var height by rememberSaveable { mutableStateOf(profile?.heightCm?.let(::decimal) ?: "") }
    var weight by rememberSaveable { mutableStateOf(profile?.weightKg?.let(::decimal) ?: "") }
    var stride by rememberSaveable { mutableStateOf(profile?.calibratedStepCm?.let(::decimal) ?: "") }
    var distance by rememberSaveable { mutableStateOf("") }
    var steps by rememberSaveable { mutableStateOf("") }
    fun parse(value: String) = value.trim().replace(',', '.').toDoubleOrNull()
    val entered = WalkingProfile(
        parse(height) ?: Double.NaN,
        parse(weight) ?: Double.NaN,
        if (stride.isBlank()) null else parse(stride) ?: Double.NaN
    )
    val calibration = WalkingMetrics.calibratedStepCm(parse(distance) ?: Double.NaN, steps.toIntOrNull() ?: 0)
    val bmiValue = WalkingMetrics.bmi(entered)
    val colors = MaterialTheme.colorScheme
    BackHandler(onBack = onDismiss)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yürüyüş profili", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Vazgeç")
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Kaydet") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { onSave(entered) }, enabled = entered.valid) {
                            Icon(Icons.Default.Check, "Kaydet")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = navBarBottomPadding(36.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live BMI & Stride Preview Card when inputs are valid
            AnimatedVisibility(visible = entered.valid && bmiValue != null) {
                val b = bmiValue ?: 22.0
                val stepsPerKm = (1000.0 / entered.stepMeters).roundToInt()
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "VKİ: ${decimal(b)} (${WalkingMetrics.bmiCategory(b)})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Adım uzunluğu: ${decimal(entered.estimatedStepCm)} cm · 1 km ≈ ${integer(stepsPerKm.toLong())} adım",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = colors.primary)
                    }
                }
            }

            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("Boy (cm)") },
                singleLine = true,
                isError = height.isNotBlank() && (parse(height)?.let { it in 80.0..250.0 } != true),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Kilo (kg)") },
                singleLine = true,
                isError = weight.isNotBlank() && (parse(weight)?.let { it in 20.0..350.0 } != true),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = stride,
                onValueChange = { stride = it },
                label = { Text("Adım uzunluğu (cm)") },
                singleLine = true,
                placeholder = { Text("İsteğe bağlı") },
                isError = stride.isNotBlank() && (parse(stride)?.let { it in 20.0..150.0 } != true),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider()
            Text("Mesafe kalibrasyonu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = distance,
                onValueChange = { distance = it },
                label = { Text("Ölçülmüş yürüyüş mesafesi (m)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = steps,
                onValueChange = { steps = it },
                label = { Text("Bu mesafede elle sayılan adım") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(enabled = calibration != null, onClick = { stride = decimal(calibration!!) }) {
                Text(if (calibration != null) "${decimal(calibration)} cm uygula" else "Kalibrasyonu uygula")
            }
            Text(
                if (profile == null) "İlk profil, geçmiş kayıtların tahmininde de kullanılır."
                else "Profil değişikliği bugün ve sonraki günler için geçerlidir.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
