package com.example.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp-xhdpi", sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PrayerLearningScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lessonCanAdvanceReturnAndResume() {
        File("build/learning-scenes").mkdirs()
        compose.setContent { MyApplicationTheme(darkTheme = false) { PrayerLearningScreen {} } }
        compose.onRoot().captureRoboImage("build/learning-scenes/catalog.png")
        compose.onNodeWithTag("learn-FAJR").performClick()
        compose.onNodeWithText("Niyet", substring = false).assertExists()
        compose.onNodeWithTag("lesson-next").performClick()
        compose.onNodeWithText("İftitah tekbiri").assertExists()
        compose.onNodeWithTag("lesson-next").performClick()
        compose.onNodeWithText("Kıyam ve kıraat").assertExists()
        compose.onRoot().captureRoboImage("build/learning-scenes/lesson.png")
        compose.onNodeWithContentDescription("Namazlara dön").performClick()
        compose.onNodeWithTag("learn-FAJR").performClick()
        compose.onNodeWithText("Kıyam ve kıraat").assertExists()
        compose.onNodeWithContentDescription("Dersi baştan başlat").performClick()
        compose.onNodeWithText("Niyet", substring = false).assertExists()
    }

    @Test fun completionIsSavedSeparatelyForEachPrayer() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("prayer_learning_v1", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        compose.setContent { MyApplicationTheme(darkTheme = false) { PrayerLearningScreen {} } }
        compose.onNodeWithTag("learn-FAJR").performClick()
        repeat(com.example.data.PrayerLessons.steps(com.example.data.DailyPrayer.FAJR).size) {
            compose.onNodeWithTag("lesson-next").performClick()
        }
        compose.onNodeWithText("Ders tamamlandı").assertExists()
        compose.onNodeWithTag("lesson-next").performClick()
        compose.onNodeWithTag("learn-DHUHR").performClick()
        compose.onNodeWithText("Niyet", substring = false).assertExists()
        compose.onNodeWithContentDescription("Namazlara dön").performClick()
        compose.onNodeWithTag("learn-FAJR").performClick()
        compose.onNodeWithText("Ders tamamlandı").assertExists()
    }
}
