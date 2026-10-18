package com.example.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
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
@Config(qualifiers = "w360dp-h800dp-xhdpi", sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PrayerLearningScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Before fun clearSavedLessonProgress() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("prayer_learning_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
        File("build/learning-scenes").mkdirs()
    }

    private fun openCatalog() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false) { PrayerLearningScreen {} }
        }
        compose.onNodeWithTag("prayer-learning-catalog").assertIsDisplayed()
    }

    private fun assertStep(title: String) {
        compose.onNodeWithTag("lesson-step-title").assertTextEquals(title)
    }

    @Test fun catalogStartsLessonAndSavedStepCanResumeOrRestart() {
        openCatalog()
        compose.onNodeWithTag("learn-FAJR").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/learning-scenes/catalog.png")

        compose.onNodeWithTag("learn-FAJR").performClick()
        assertStep("Niyet")
        compose.onNodeWithTag("lesson-next").performClick()
        assertStep("İftitah tekbiri")
        compose.onNodeWithTag("lesson-next").performClick()
        assertStep("Kıyam ve kıraat")

        compose.onNodeWithContentDescription("Namazlara dön").performClick()
        compose.onNodeWithTag("prayer-learning-catalog").assertIsDisplayed()
        compose.onNodeWithTag("learn-FAJR").performClick()
        assertStep("Kıyam ve kıraat")
        compose.onNodeWithContentDescription("Dersi baştan başlat").performClick()
        assertStep("Niyet")
    }

    @Test fun lessonShowsBothGenderIllustrationsAndEnlargedArtwork() {
        openCatalog()
        compose.onNodeWithTag("learn-FAJR").performClick()
        repeat(2) { compose.onNodeWithTag("lesson-next").performClick() }
        assertStep("Kıyam ve kıraat")
        compose.onNodeWithTag("posture-artwork-STANDING-male", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/learning-scenes/male.png")

        compose.onNodeWithTag("lesson-gender-toggle").performClick()
        compose.onNodeWithTag("posture-artwork-STANDING-female", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("posture-artwork-STANDING-male", useUnmergedTree = true)
            .assertDoesNotExist()
        compose.onRoot().captureRoboImage("build/learning-scenes/female.png")

        compose.onNodeWithTag("posture-image-frame").performClick()
        compose.onNodeWithTag("posture-zoom").assertIsDisplayed()
        compose.onAllNodes(isRoot()).onLast()
            .captureRoboImage("build/learning-scenes/zoom.png")
        compose.onNodeWithText("Kapat").performClick()
        compose.onNodeWithTag("posture-zoom").assertDoesNotExist()
        assertStep("Kıyam ve kıraat")
    }

    @Test fun catalogAtlasCanScrollAndSelectDifferentPostures() {
        openCatalog()
        compose.onNodeWithTag("prayer-learning-catalog")
            .performScrollToNode(hasTestTag("posture-atlas"))
        compose.onNodeWithTag("posture-atlas").assertIsDisplayed()
        compose.onNodeWithTag("posture-artwork-STANDING-male", useUnmergedTree = true)
            .assertExists()

        compose.onNodeWithTag("atlas-pose-BOWING").performClick()
        compose.onNodeWithTag("posture-artwork-BOWING-male", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithTag("posture-atlas-tabs")
            .performScrollToNode(hasTestTag("atlas-pose-PROSTRATION"))
        compose.onNodeWithTag("atlas-pose-PROSTRATION").performClick()
        compose.onNodeWithTag("posture-artwork-PROSTRATION-male", useUnmergedTree = true)
            .assertExists()

        compose.onNodeWithTag("prayer-learning-catalog").performScrollToIndex(0)
        compose.onNodeWithTag("catalog-gender-toggle").performClick()
        compose.onNodeWithText("Kadın Duruşu", substring = false).assertExists()
        compose.onNodeWithTag("learn-DHUHR").performClick()
        assertStep("Niyet")
        compose.onNodeWithTag("posture-artwork-INTENTION-female", useUnmergedTree = true)
            .assertExists()
    }
}
