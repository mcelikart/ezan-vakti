package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.example.data.Timings
import com.example.steps.*
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.drawCustomBackground
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSensor
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp-xhdpi", sdk = [36])
class ScreenshotsGeneratorTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var app: Application
    private lateinit var viewModel: PrayerViewModel
    private lateinit var outDir: File

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = PrayerViewModel(app)
        
        val userDir = File(System.getProperty("user.dir") ?: ".")
        outDir = if (File(userDir, "screenshots").exists()) {
            File(userDir, "screenshots")
        } else if (userDir.parentFile != null && File(userDir.parentFile, "screenshots").exists()) {
            File(userDir.parentFile, "screenshots")
        } else {
            File(userDir, "screenshots").apply { mkdirs() }
        }
    }

    @Composable
    private fun BottomBarMock(selectedRoute: String) {
        val items = listOf(
            Triple("home", "Ana Sayfa", Icons.Default.Home),
            Triple("quran_reading", "Kur'an", Icons.Default.Book),
            Triple("compass", "Kıble", Icons.Default.Explore),
            Triple("dhikr", "Zikirmatik", Icons.Default.Star),
            Triple("calendar", "Takvim", Icons.Default.DateRange)
        )
        val barShape = RoundedCornerShape(26.dp)
        val primaryColor = MaterialTheme.colorScheme.primary
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                shape = barShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { (route, label, icon) ->
                        val isSelected = route == selectedRoute
                        Column(
                            modifier = Modifier
                                .weight(if (isSelected) 1.25f else 1.0f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = if (isSelected) primaryColor else onSurfaceColor.copy(alpha = 0.55f),
                                modifier = Modifier.size(if (isSelected) 23.dp else 21.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                label,
                                fontSize = if (isSelected) 10.5.sp else 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) primaryColor else onSurfaceColor.copy(alpha = 0.55f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun capture01_AnaEkranSelcuklu() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.KUBBELI_SAFIR) {
                Scaffold(
                    bottomBar = { BottomBarMock("home") }
                ) { innerPadding ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .drawCustomBackground(theme = AppTheme.KUBBELI_SAFIR, isDark = false)
                            .padding(bottom = innerPadding.calculateBottomPadding())
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                            HeritageHomeHeader("Alcı, Sincan, Ankara", Locale.forLanguageTag("tr-TR")) {}
                        }
                        ThemePrayerScene(
                            Timings("05:30", "06:45", "12:46", "16:12", "18:52", "18:52", "20:15", "05:20", "00:10"),
                            "02:28:01", "Güneş" to "Öğle", 0.4f
                        )
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage(File(outDir, "01_ana_ekran_selcuklu.png").absolutePath)
    }

    @Test
    fun capture02_KiblePusulasi() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.KUBBELI_SAFIR) {
                Scaffold(
                    bottomBar = { BottomBarMock("compass") }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(bottom = innerPadding.calculateBottomPadding()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Kıble Pusulası",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cihazınızı yatay ve düz tutun",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(44.dp))

                            CompassDialDetailed(azimuth = 152f, qiblaAngle = 152f)

                            Spacer(modifier = Modifier.height(30.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Yönünüz",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "152°",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Kıble",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "152°",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 32.dp)
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.medium
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Kıble Yönündesiniz",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage(File(outDir, "02_kible_pusulasi.png").absolutePath)
    }

    @Test
    fun capture03_Zikirmatik() {
        val dhikrPrefs = app.getSharedPreferences("dhikr_prefs", Context.MODE_PRIVATE)
        dhikrPrefs.edit()
            .putInt("count_Sübhanallah", 33)
            .putInt("total_count", 99)
            .putInt("selected_index", 0)
            .commit()

        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.KUBBELI_SAFIR) {
                Scaffold(
                    bottomBar = { BottomBarMock("dhikr") }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        DhikrScreen(viewModel = viewModel)
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage(File(outDir, "03_zikirmatik.png").absolutePath)
    }

    @Test
    fun capture04_KuranMucizeleri() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.KUBBELI_SAFIR) {
                QuranMiraclesScreen(viewModel = viewModel, onBack = {})
            }
        }
        compose.onRoot().captureRoboImage(File(outDir, "04_kuran_mucizeleri.png").absolutePath)
    }

    @Test
    fun capture05_AdimsayarRaporu() {
        shadowOf(app).grantPermissions(Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.POST_NOTIFICATIONS)
        val sm = app.getSystemService(SensorManager::class.java)
        if (sm != null) {
            shadowOf(sm).addSensor(ShadowSensor.newInstance(Sensor.TYPE_STEP_COUNTER))
        }
        StepCounterService.settings(app).edit().putBoolean("enabled", true).commit()
        val store = StepStore.get(app)
        store.writableDatabase.delete("daily_steps", null, null)
        store.writableDatabase.delete("body_profiles", null, null)
        store.resetBaseline()
        for (offset in 6 downTo 0) {
            val day = WalkingDates.shift(StepStore.dayKey(), ReportPeriod.DAY, -offset)
            store.writableDatabase.execSQL(
                "INSERT INTO daily_steps VALUES (?, ?)",
                arrayOf<Any>(day, if (offset == 0) 6200 else 2500 + offset * 300)
            )
        }
        store.saveProfile(WalkingProfile(175.0, 75.0, 70.0))

        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.KUBBELI_SAFIR) {
                StepReportScreen(onBack = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("6.200")).fetchSemanticsNodes().isNotEmpty() }
        compose.onRoot().captureRoboImage(File(outDir, "05_adimsayar_raporu.png").absolutePath)
    }

    @Test
    fun capture06_NamazOgreniyorum() {
        app.getSharedPreferences("prayer_learning_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()

        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.KUBBELI_SAFIR) {
                PrayerLearningScreen(onBack = {})
            }
        }
        compose.onNodeWithTag("learn-FAJR").performClick()
        repeat(2) { compose.onNodeWithTag("lesson-next").performClick() }
        compose.onNodeWithTag("posture-artwork-STANDING-male", useUnmergedTree = true).assertIsDisplayed()
        compose.onRoot().captureRoboImage(File(outDir, "06_namaz_ogreniyorum.png").absolutePath)
    }

    @Test
    fun capture07_AnaEkranGeceCinisi() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true, appTheme = AppTheme.KUBBELI_GECE_MAVISI) {
                Scaffold(
                    bottomBar = { BottomBarMock("home") }
                ) { innerPadding ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .drawCustomBackground(theme = AppTheme.KUBBELI_GECE_MAVISI, isDark = true)
                            .padding(bottom = innerPadding.calculateBottomPadding())
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                            HeritageHomeHeader("Alcı, Sincan, Ankara", Locale.forLanguageTag("tr-TR")) {}
                        }
                        ThemePrayerScene(
                            Timings("05:30", "06:45", "12:46", "16:12", "18:52", "18:52", "20:15", "05:20", "00:10"),
                            "01:14:22", "Akşam" to "Yatsı", 0.75f
                        )
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage(File(outDir, "07_ana_ekran_gece_cinisi.png").absolutePath)
    }

    @Test
    fun capture08_TemaGalerisi() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.KUBBELI_SAFIR) {
                ThemeGalleryScreen(
                    currentTheme = AppTheme.KUBBELI_SAFIR,
                    onThemeSelect = {},
                    onBack = {}
                )
            }
        }
        compose.onNodeWithText("Tema Galerisi").assertExists()
        compose.onRoot().captureRoboImage(File(outDir, "08_tema_galerisi.png").absolutePath)
    }

    @Test
    fun capture09_NamazDurusDetayi() {
        app.getSharedPreferences("prayer_learning_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()

        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.KUBBELI_SAFIR) {
                PrayerLearningScreen(onBack = {})
            }
        }
        compose.onNodeWithTag("learn-FAJR").performClick()
        repeat(2) { compose.onNodeWithTag("lesson-next").performClick() }
        compose.onNodeWithTag("posture-image-frame").performClick()
        compose.onNodeWithTag("posture-zoom").assertIsDisplayed()
        compose.onAllNodes(isRoot()).onLast().captureRoboImage(File(outDir, "09_namaz_durus_detayi.png").absolutePath)
    }
}
