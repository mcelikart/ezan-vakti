package com.example.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.ui.ACTION_UPDATE_WIDGET") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, PrayerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private data class WidgetThemeColors(
        val bgStart: Int,
        val bgEnd: Int,
        val bgStroke: Int,
        val cityColor: Int,
        val dateColor: Int,
        val labelColor: Int,
        val highlightStart: Int,
        val highlightEnd: Int,
        val highlightStroke: Int,
        val highlightTextColor: Int,
        val chronometerColor: Int
    )

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, PrayerWidgetProvider::class.java).apply {
                action = "com.example.ui.ACTION_UPDATE_WIDGET"
            }
            context.sendBroadcast(intent)
        }

        private fun createRoundedGradientBitmap(
            widthPx: Int,
            heightPx: Int,
            startColor: Int,
            endColor: Int,
            strokeColor: Int,
            cornerRadiusDp: Float,
            density: Float
        ): Bitmap {
            val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            val radiusPx = cornerRadiusDp * density
            val rect = RectF(2f, 2f, widthPx - 2f, heightPx - 2f)
            
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                shader = LinearGradient(
                    0f, 0f, widthPx.toFloat(), heightPx.toFloat(),
                    startColor, endColor,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)
            
            if (strokeColor != 0) {
                val strokePaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f * density
                    color = strokeColor
                }
                canvas.drawRoundRect(rect, radiusPx, radiusPx, strokePaint)
            }
            
            return bitmap
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)
            val prefs = context.getSharedPreferences("widget_prayer_prefs", Context.MODE_PRIVATE)
            
            val city = prefs.getString("city", null)
            val imsak = prefs.getString("imsak", null)
            val gunes = prefs.getString("gunes", null)
            val ogle = prefs.getString("ogle", null)
            val ikindi = prefs.getString("ikindi", null)
            val aksam = prefs.getString("aksam", null)
            val yatsi = prefs.getString("yatsi", null)
            val themeName = prefs.getString("theme", "EMERALD") ?: "EMERALD"

            val themeColors = when (themeName) {
                "KUBBELI_SAFIR" -> WidgetThemeColors(
                    bgStart = 0xFF0F3D6E.toInt(),
                    bgEnd = 0xFF061526.toInt(),
                    bgStroke = 0x66D4AF37.toInt(),
                    cityColor = 0xFFE4C56E.toInt(),
                    dateColor = 0xFFE6F0FA.toInt(),
                    labelColor = 0xFF9AD0F5.toInt(),
                    highlightStart = 0xFF175C9E.toInt(),
                    highlightEnd = 0xFF0C2E54.toInt(),
                    highlightStroke = 0xFFE4C56E.toInt(),
                    highlightTextColor = 0xFFFFECB3.toInt(),
                    chronometerColor = 0xFFE4C56E.toInt()
                )
                "KUBBELI_YAKUT", "OTTOMAN_GOLD" -> WidgetThemeColors(
                    bgStart = 0xFF4E1423.toInt(),
                    bgEnd = 0xFF1F070E.toInt(),
                    bgStroke = 0x66D4AF37.toInt(),
                    cityColor = 0xFFE6C66E.toInt(),
                    dateColor = 0xFFFAEAEE.toInt(),
                    labelColor = 0xFFF4A6B8.toInt(),
                    highlightStart = 0xFF7A1C33.toInt(),
                    highlightEnd = 0xFF4A0E1D.toInt(),
                    highlightStroke = 0xFFE6C66E.toInt(),
                    highlightTextColor = 0xFFFFECB3.toInt(),
                    chronometerColor = 0xFFE6C66E.toInt()
                )
                "KUBBELI_FIRUZE" -> WidgetThemeColors(
                    bgStart = 0xFF0A5C66.toInt(),
                    bgEnd = 0xFF051B1E.toInt(),
                    bgStroke = 0x66D4AF37.toInt(),
                    cityColor = 0xFFE2C46C.toInt(),
                    dateColor = 0xFFE4F6F8.toInt(),
                    labelColor = 0xFF8CE2EC.toInt(),
                    highlightStart = 0xFF137A87.toInt(),
                    highlightEnd = 0xFF09434A.toInt(),
                    highlightStroke = 0xFFE2C46C.toInt(),
                    highlightTextColor = 0xFFFFECB3.toInt(),
                    chronometerColor = 0xFFE2C46C.toInt()
                )
                "KUBBELI_KEHRIBAR", "DESERT_SAND" -> WidgetThemeColors(
                    bgStart = 0xFF4D2D0A.toInt(),
                    bgEnd = 0xFF1C1004.toInt(),
                    bgStroke = 0x66D4AF37.toInt(),
                    cityColor = 0xFFE8C368.toInt(),
                    dateColor = 0xFFFAF0E4.toInt(),
                    labelColor = 0xFFF5C77E.toInt(),
                    highlightStart = 0xFF784610.toInt(),
                    highlightEnd = 0xFF452708.toInt(),
                    highlightStroke = 0xFFE8C368.toInt(),
                    highlightTextColor = 0xFFFFECB3.toInt(),
                    chronometerColor = 0xFFE8C368.toInt()
                )
                "KUBBELI_AMETIST" -> WidgetThemeColors(
                    bgStart = 0xFF3D1A4C.toInt(),
                    bgEnd = 0xFF16081C.toInt(),
                    bgStroke = 0x66D4AF37.toInt(),
                    cityColor = 0xFFE4C56E.toInt(),
                    dateColor = 0xFFF3EAF7.toInt(),
                    labelColor = 0xFFDAB6F2.toInt(),
                    highlightStart = 0xFF5E2875.toInt(),
                    highlightEnd = 0xFF361544.toInt(),
                    highlightStroke = 0xFFE4C56E.toInt(),
                    highlightTextColor = 0xFFFFECB3.toInt(),
                    chronometerColor = 0xFFE4C56E.toInt()
                )
                "KUBBELI_ZEYTIN" -> WidgetThemeColors(
                    bgStart = 0xFF29421D.toInt(),
                    bgEnd = 0xFF0F190A.toInt(),
                    bgStroke = 0x66D4AF37.toInt(),
                    cityColor = 0xFFE2C46C.toInt(),
                    dateColor = 0xFFEBF4E6.toInt(),
                    labelColor = 0xFFB8E29E.toInt(),
                    highlightStart = 0xFF3F632D.toInt(),
                    highlightEnd = 0xFF233818.toInt(),
                    highlightStroke = 0xFFE2C46C.toInt(),
                    highlightTextColor = 0xFFFFECB3.toInt(),
                    chronometerColor = 0xFFE2C46C.toInt()
                )
                "KUBBELI_SAHRA" -> WidgetThemeColors(
                    bgStart = 0xFF452F1C.toInt(),
                    bgEnd = 0xFF170F08.toInt(),
                    bgStroke = 0x66D4AF37.toInt(),
                    cityColor = 0xFFE5C56E.toInt(),
                    dateColor = 0xFFF6EFE8.toInt(),
                    labelColor = 0xFFE8C7A7.toInt(),
                    highlightStart = 0xFF6B4A2D.toInt(),
                    highlightEnd = 0xFF3D2918.toInt(),
                    highlightStroke = 0xFFE5C56E.toInt(),
                    highlightTextColor = 0xFFFFECB3.toInt(),
                    chronometerColor = 0xFFE5C56E.toInt()
                )
                "KUBBELI_SEDEF", "MARBLE_WHITE" -> WidgetThemeColors(
                    bgStart = 0xFF183E4A.toInt(),
                    bgEnd = 0xFF08171C.toInt(),
                    bgStroke = 0x66D4AF37.toInt(),
                    cityColor = 0xFFE2C46C.toInt(),
                    dateColor = 0xFFE8F3F6.toInt(),
                    labelColor = 0xFFA3DAE8.toInt(),
                    highlightStart = 0xFF266073.toInt(),
                    highlightEnd = 0xFF143540.toInt(),
                    highlightStroke = 0xFFE2C46C.toInt(),
                    highlightTextColor = 0xFFFFECB3.toInt(),
                    chronometerColor = 0xFFE2C46C.toInt()
                )
                "KUBBELI_KABE_GECESI", "MINIMAL_DARK" -> WidgetThemeColors(
                    bgStart = 0xFF242018.toInt(),
                    bgEnd = 0xFF0A0A0C.toInt(),
                    bgStroke = 0x80E8C86B.toInt(),
                    cityColor = 0xFFE8C86B.toInt(),
                    dateColor = 0xFFF5F1E8.toInt(),
                    labelColor = 0xFFD4AF37.toInt(),
                    highlightStart = 0xFF3B3322.toInt(),
                    highlightEnd = 0xFF1F1B12.toInt(),
                    highlightStroke = 0xFFE8C86B.toInt(),
                    highlightTextColor = 0xFFFFECB3.toInt(),
                    chronometerColor = 0xFFE8C86B.toInt()
                )
                "KUBBELI_GECE_MAVISI", "ANDALUSIAN_NIGHT" -> WidgetThemeColors(
                    bgStart = 0xFF18274E.toInt(),
                    bgEnd = 0xFF070C1A.toInt(),
                    bgStroke = 0x66E5C66E.toInt(),
                    cityColor = 0xFFE5C66E.toInt(),
                    dateColor = 0xFFE8EEFA.toInt(),
                    labelColor = 0xFFA8C2F7.toInt(),
                    highlightStart = 0xFF263E7A.toInt(),
                    highlightEnd = 0xFF142145.toInt(),
                    highlightStroke = 0xFFE5C66E.toInt(),
                    highlightTextColor = 0xFFFFECB3.toInt(),
                    chronometerColor = 0xFFE5C66E.toInt()
                )
                else -> WidgetThemeColors( // CLASSIC_EMERALD (Kubbeli Zümrüt)
                    bgStart = 0xFF0C2E24.toInt(),
                    bgEnd = 0xFF051D16.toInt(),
                    bgStroke = 0x66D4AF37.toInt(),
                    cityColor = 0xFFE2C46C.toInt(),
                    dateColor = 0xFFE8F5E9.toInt(),
                    labelColor = 0xFFA1E8C5.toInt(),
                    highlightStart = 0xFF154D3B.toInt(),
                    highlightEnd = 0xFF0A291F.toInt(),
                    highlightStroke = 0xFFE2C46C.toInt(),
                    highlightTextColor = 0xFFFFECB3.toInt(),
                    chronometerColor = 0xFFE2C46C.toInt()
                )
            }

            val density = context.resources.displayMetrics.density
            val bgBitmap = createRoundedGradientBitmap(
                widthPx = (320 * density).toInt(),
                heightPx = (180 * density).toInt(),
                startColor = themeColors.bgStart,
                endColor = themeColors.bgEnd,
                strokeColor = themeColors.bgStroke,
                cornerRadiusDp = 22f,
                density = density
            )
            views.setImageViewBitmap(R.id.img_widget_bg, bgBitmap)

            views.setTextColor(R.id.tv_widget_city, themeColors.cityColor)
            views.setTextColor(R.id.tv_widget_date, themeColors.dateColor)
            views.setTextColor(R.id.tv_widget_current_label, themeColors.labelColor)
            views.setTextColor(R.id.tv_widget_next_time, themeColors.dateColor)
            views.setTextColor(R.id.widget_chronometer, themeColors.chronometerColor)

            // Click intent to open application
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 
                2001, 
                mainIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_widget_city, pendingIntent)
            
            // Handle empty state
            if (city == null || imsak == null || gunes == null || ogle == null || ikindi == null || aksam == null || yatsi == null) {
                views.setTextViewText(R.id.tv_widget_city, "KONUM YOK")
                views.setTextViewText(R.id.tv_widget_next_name, "-")
                views.setTextViewText(R.id.tv_widget_next_time, "--:--")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            views.setTextViewText(R.id.tv_widget_city, city.uppercase(Locale.getDefault()))
            
            // Set date
            val sdfDate = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale.forLanguageTag("tr-TR"))
            views.setTextViewText(R.id.tv_widget_date, sdfDate.format(Calendar.getInstance().time))

            // Populate timings texts
            views.setTextViewText(R.id.tv_widget_time_0, imsak)
            views.setTextViewText(R.id.tv_widget_time_1, gunes)
            views.setTextViewText(R.id.tv_widget_time_2, ogle)
            views.setTextViewText(R.id.tv_widget_time_3, ikindi)
            views.setTextViewText(R.id.tv_widget_time_4, aksam)
            views.setTextViewText(R.id.tv_widget_time_5, yatsi)

            // Parse current and determine active/next
            val calendar = Calendar.getInstance()
            val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            
            fun timeToMinutes(t: String): Int {
                return try {
                    val p = t.trim().split(":")
                    p[0].toInt() * 60 + p[1].toInt()
                } catch (e: Exception) {
                    0
                }
            }

            val timesM = intArrayOf(
                timeToMinutes(imsak),
                timeToMinutes(gunes),
                timeToMinutes(ogle),
                timeToMinutes(ikindi),
                timeToMinutes(aksam),
                timeToMinutes(yatsi)
            )
            val names = arrayOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")
            val timesStr = arrayOf(imsak, gunes, ogle, ikindi, aksam, yatsi)

            // Determine active (current) prayer period and next prayer
            var activeIndex = 5 // Default Yatsı
            if (currentMinutes >= timesM[0] && currentMinutes < timesM[1]) {
                activeIndex = 0 // İmsak
            } else if (currentMinutes >= timesM[1] && currentMinutes < timesM[2]) {
                activeIndex = 1 // Güneş
            } else if (currentMinutes >= timesM[2] && currentMinutes < timesM[3]) {
                activeIndex = 2 // Öğle
            } else if (currentMinutes >= timesM[3] && currentMinutes < timesM[4]) {
                activeIndex = 3 // İkindi
            } else if (currentMinutes >= timesM[4] && currentMinutes < timesM[5]) {
                activeIndex = 4 // Akşam
            } else if (currentMinutes >= timesM[5] || currentMinutes < timesM[0]) {
                activeIndex = 5 // Yatsı
            }

            val nextIndex = (activeIndex + 1) % 6
            val activeName = names[activeIndex] + " Vakti"
            val nextName = names[nextIndex]
            val nextTimeStr = timesStr[nextIndex]
            
            // Calculate previous and next time in minutes for progress bar
            var prevM = timesM[activeIndex]
            var nextM = timesM[nextIndex]
            var currM = currentMinutes
            
            if (activeIndex == 5) {
                if (currM >= timesM[5]) {
                    nextM += 24 * 60
                } else {
                    prevM -= 24 * 60
                }
            }

            val totalDiff = nextM - prevM
            val elapsed = currM - prevM
            
            val progress = if (totalDiff > 0) {
                ((elapsed.toFloat() / totalDiff.toFloat()) * 1000).toInt()
            } else {
                0
            }
            
            views.setProgressBar(R.id.widget_progress, 1000, progress, false)
            
            // Set Current Active Vakit and Next Vakit info strings
            views.setTextViewText(R.id.tv_widget_next_name, activeName)
            views.setTextViewText(R.id.tv_widget_next_time, "Sonraki: $nextName • $nextTimeStr")
            
            // Setup Chronometer for countdown
            val nextCal = Calendar.getInstance()
            val p = nextTimeStr.split(":")
            nextCal.set(Calendar.HOUR_OF_DAY, p[0].toInt())
            nextCal.set(Calendar.MINUTE, p[1].toInt())
            nextCal.set(Calendar.SECOND, 0)
            
            if (nextCal.timeInMillis < System.currentTimeMillis()) {
                nextCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            val targetTimeMillis = nextCal.timeInMillis
            val timeToNext = targetTimeMillis - System.currentTimeMillis()
            val chronometerBase = android.os.SystemClock.elapsedRealtime() + timeToNext
            
            views.setChronometer(R.id.widget_chronometer, chronometerBase, "%s", true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                views.setBoolean(R.id.widget_chronometer, "setCountDown", true)
            }

            // Create item background bitmaps
            val activeItemBitmap = createRoundedGradientBitmap(
                widthPx = (50 * density).toInt(),
                heightPx = (45 * density).toInt(),
                startColor = themeColors.highlightStart,
                endColor = themeColors.highlightEnd,
                strokeColor = themeColors.highlightStroke,
                cornerRadiusDp = 10f,
                density = density
            )
            val normalItemBitmap = createRoundedGradientBitmap(
                widthPx = (50 * density).toInt(),
                heightPx = (45 * density).toInt(),
                startColor = 0x22FFFFFF.toInt(),
                endColor = 0x11FFFFFF.toInt(),
                strokeColor = 0x1AFFFFFF.toInt(),
                cornerRadiusDp = 10f,
                density = density
            )

            val imgIds = intArrayOf(
                R.id.img_widget_item_0,
                R.id.img_widget_item_1,
                R.id.img_widget_item_2,
                R.id.img_widget_item_3,
                R.id.img_widget_item_4,
                R.id.img_widget_item_5
            )
            val nameIds = intArrayOf(
                R.id.tv_widget_name_0,
                R.id.tv_widget_name_1,
                R.id.tv_widget_name_2,
                R.id.tv_widget_name_3,
                R.id.tv_widget_name_4,
                R.id.tv_widget_name_5
            )

            for (i in 0..5) {
                val isCurrent = (activeIndex == i)
                views.setImageViewBitmap(imgIds[i], if (isCurrent) activeItemBitmap else normalItemBitmap)
                views.setTextColor(nameIds[i], if (isCurrent) themeColors.highlightTextColor else themeColors.dateColor)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
