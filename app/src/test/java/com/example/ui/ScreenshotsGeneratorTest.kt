package com.example.ui

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h850dp-xhdpi", sdk = [36])
class ScreenshotsGeneratorTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var app: Application
    private lateinit var viewModel: PrayerViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = PrayerViewModel(app)
        File("build/all-screens").mkdirs()
    }

    @Test
    fun captureCompass() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.KUBBELI_SAFIR) {
                CompassScreen(viewModel = viewModel)
            }
        }
        compose.onRoot().captureRoboImage("build/all-screens/02_kible_pusulasi.png")
    }

    @Test
    fun captureDhikr() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.KUBBELI_SAFIR) {
                DhikrScreen(viewModel = viewModel)
            }
        }
        compose.onRoot().captureRoboImage("build/all-screens/03_zikirmatik.png")
    }

    @Test
    fun captureMiracles() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.KUBBELI_SAFIR) {
                QuranMiraclesScreen(viewModel = viewModel, onBack = {})
            }
        }
        compose.onRoot().captureRoboImage("build/all-screens/04_kuran_mucizeleri.png")
    }

    @Test
    fun captureCalendar() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.KUBBELI_SAFIR) {
                CalendarScreen(viewModel = viewModel)
            }
        }
        compose.onRoot().captureRoboImage("build/all-screens/05_hicri_takvim.png")
    }
}
