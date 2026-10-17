package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

data class VibrationPatternOption(
    val id: Int,
    val title: String,
    val description: String,
    val pattern: LongArray
)

object VibrationPatternManager {
    val PATTERN_OPTIONS = listOf(
        VibrationPatternOption(0, "Standart Ezan Vuruşu", "Dengeli ve ritmik çift darbe", longArrayOf(0, 300, 200, 300, 200, 600)),
        VibrationPatternOption(1, "Hafif Nazik Titreşim", "Yumuşak ve kısa uyarı ritmi", longArrayOf(0, 150, 150, 150, 150, 300)),
        VibrationPatternOption(2, "Manevi Kalp Atışı", "Ritmik çift kalp atımı", longArrayOf(0, 120, 120, 120, 250, 500, 120, 120)),
        VibrationPatternOption(3, "Çift Darbe Uyarı", "Belirgin ve keskin çift vuruş", longArrayOf(0, 250, 100, 250, 400, 250, 100, 250)),
        VibrationPatternOption(4, "Uzun & Güçlü", "Derin ve uzun süreli güçlü titreşim", longArrayOf(0, 600, 200, 600, 200, 800)),
        VibrationPatternOption(5, "Dalgalı Ritim", "Kademeli olarak yükselen titreşim dalgası", longArrayOf(0, 100, 100, 200, 100, 300, 100, 400))
    )

    fun getPatternById(id: Int): LongArray {
        return PATTERN_OPTIONS.find { it.id == id }?.pattern ?: PATTERN_OPTIONS[0].pattern
    }

    fun getPatternTitleById(id: Int): String {
        return PATTERN_OPTIONS.find { it.id == id }?.title ?: PATTERN_OPTIONS[0].title
    }

    fun playTestVibration(context: Context, patternId: Int) {
        val pattern = getPatternById(patternId)
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, -1),
                    audioAttributes
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(pattern, -1),
                        audioAttributes
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
