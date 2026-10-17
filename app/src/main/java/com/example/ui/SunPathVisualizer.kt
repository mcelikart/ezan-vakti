package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Timings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SunPathVisualizer(timings: Timings?, modifier: Modifier = Modifier) {
    if (timings == null) return

    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

    fun parseToMinutes(timeStr: String): Int {
        return try {
            val clean = timeStr.replace(Regex("[^0-9:]"), "")
            val parts = clean.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }
    }

    val sunriseMinutes = parseToMinutes(timings.Sunrise)
    val sunsetMinutes = parseToMinutes(timings.Maghrib)
    val imsakMinutes = parseToMinutes(timings.Fajr)

    // Calculate day progress and if it's day or night
    val isDay = nowMinutes in sunriseMinutes..sunsetMinutes
    val progress = if (isDay) {
        val denom = sunsetMinutes - sunriseMinutes
        if (denom > 0) {
            (nowMinutes - sunriseMinutes).toFloat() / denom
        } else 0f
    } else {
        // Night progress: from sunset to next sunrise
        val totalNightMinutes = if (sunriseMinutes > sunsetMinutes) {
            (1440 - sunsetMinutes) + sunriseMinutes
        } else {
            sunriseMinutes + (1440 - sunsetMinutes)
        }
        val currentNightMinutes = if (nowMinutes >= sunsetMinutes) {
            nowMinutes - sunsetMinutes
        } else {
            (1440 - sunsetMinutes) + nowMinutes
        }
        if (totalNightMinutes > 0) {
            currentNightMinutes.toFloat() / totalNightMinutes
        } else 0f
    }

    // Animate the progress transition smoothly
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(progress, animationSpec = tween(durationMillis = 1000))
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val cardBgColor = MaterialTheme.colorScheme.surface

    CardWithBorder(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isDay) "Güneşin Gökyüzündeki Konumu" else "Ayın Gökyüzündeki Konumu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Drawing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    val margin = 40f
                    val radiusX = (w - margin * 2) / 2
                    val radiusY = h - 20f
                    val centerX = w / 2
                    val centerY = h - 10f

                    val cardSurfaceColor = cardBgColor
                    
                    // 1. Draw Horizon ground line with higher contrast
                    drawLine(
                        color = onBgColor.copy(alpha = 0.3f),
                        start = Offset(10f, centerY),
                        end = Offset(w - 10f, centerY),
                        strokeWidth = 4f
                    )

                    // 2. Draw Semi-elliptic arc path with thicker and higher contrast lines
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                    drawArc(
                        color = if (isDay) Color(0xFFFFB300) else Color(0xFF2196F3),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(centerX - radiusX, centerY - radiusY),
                        size = Size(radiusX * 2, radiusY * 2),
                        style = Stroke(width = 6f, pathEffect = pathEffect, cap = StrokeCap.Round)
                    )

                    // 3. Compute sun/moon coordinates based on progress (0.0 to 1.0)
                    val angleRad = Math.PI - (animatedProgress.value * Math.PI)
                    val bodyX = centerX + radiusX * cos(angleRad).toFloat()
                    val bodyY = centerY - radiusY * sin(angleRad).toFloat()

                    // 4. Draw celestial body with high glow and sharp contrast
                    if (isDay) {
                        // Draw Sun outer glow
                        drawCircle(
                            color = Color(0xFFFFD54F).copy(alpha = 0.5f),
                            radius = 36f,
                            center = Offset(bodyX, bodyY)
                        )
                        // Draw Sun body
                        drawCircle(
                            color = Color(0xFFFF6D00),
                            radius = 18f,
                            center = Offset(bodyX, bodyY)
                        )
                        // Draw Sun core
                        drawCircle(
                            color = Color(0xFFFFEB3B),
                            radius = 10f,
                            center = Offset(bodyX, bodyY)
                        )
                    } else {
                        // Draw Moon outer glow (Cyan/Teal glow)
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                            radius = 34f,
                            center = Offset(bodyX, bodyY)
                        )
                        // Draw Moon body (Silver-blue)
                        drawCircle(
                            color = Color(0xFFE0F7FA),
                            radius = 15f,
                            center = Offset(bodyX, bodyY)
                        )
                        // Cut crescent using card background color dynamically
                        drawCircle(
                            color = cardSurfaceColor,
                            radius = 13f,
                            center = Offset(bodyX - 7f, bodyY - 4f)
                        )
                    }
                }
            }

            // Labels under the arc
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = if (isDay) "Doğuş (Güneş)" else "Batış (Akşam)",
                        fontSize = 11.sp,
                        color = onBgColor.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (isDay) timings.Sunrise else timings.Maghrib,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }

                // Center indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isDay) "Gün Konumu" else "Gece Konumu",
                        fontSize = 11.sp,
                        color = onBgColor.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "%${(animatedProgress.value * 100).toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isDay) "Batış (Akşam)" else "Doğuş (İmsak)",
                        fontSize = 11.sp,
                        color = onBgColor.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (isDay) timings.Maghrib else timings.Fajr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun CardWithBorder(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        content()
    }
}
