package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassScreen(viewModel: PrayerViewModel) {
    val rawAzimuth by viewModel.azimuthFlow.collectAsState()
    val qiblaAngle by viewModel.qiblaAngle.collectAsState()

    // Smooth out the azimuth reading
    val azimuth by animateFloatAsState(
        targetValue = rawAzimuth,
        animationSpec = tween(durationMillis = 200),
        label = "azimuth"
    )

    val hasSensors = remember { viewModel.hasCompassSensors() }
    CompassContent(azimuth, qiblaAngle, hasSensors)
}

@Composable
internal fun CompassContent(azimuth: Float, qiblaAngle: Float, hasSensors: Boolean) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().testTag("compass-scroll-content").verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kıble Pusulası",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            if (!hasSensors) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Pusula donanımı bulunamadı!\nKıble Açısı: ${qiblaAngle.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cihazınızı yatay ve düz tutun",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            CompassDialDetailed(azimuth = azimuth, qiblaAngle = qiblaAngle)

            Spacer(modifier = Modifier.height(32.dp))

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
                        text = "${azimuth.toInt()}°",
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
                        text = "${qiblaAngle.toInt()}°",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val diff = kotlin.math.abs((azimuth - qiblaAngle + 360) % 360).let { if (it > 180) 360 - it else it }
            val isCorrect = diff < 3
            
            if (isCorrect) {
                LaunchedEffect(isCorrect) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .testTag("compass-alignment-status")
                    .background(
                        if (isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCorrect) "Kıble Yönündesiniz" else "Kıble Yönünü Bulunuz",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCorrect) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(com.example.ui.theme.navBarBottomPadding()))
        }
    }
}

@Composable
fun CompassDialDetailed(azimuth: Float, qiblaAngle: Float) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .testTag("compass-dial")
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(16.dp, CircleShape)
            .background(surfaceColor, CircleShape)
    ) {
        // Rotating Dial
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(-azimuth),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                val center = Offset(size.width / 2, size.height / 2)

                drawCircle(
                    color = primaryColor.copy(alpha = 0.1f),
                    radius = radius,
                    center = center
                )

                drawCircle(
                    color = primaryColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )

                // Tick marks
                for (i in 0 until 360 step 6) {
                    val angleRad = Math.toRadians(i.toDouble() - 90.0)
                    val isCardinal = i % 90 == 0
                    val isIntermediate = i % 30 == 0
                    
                    val tickLength = when {
                        isCardinal -> 24.dp.toPx()
                        isIntermediate -> 16.dp.toPx()
                        else -> 8.dp.toPx()
                    }
                    val tickWidth = when {
                        isCardinal -> 4.dp.toPx()
                        isIntermediate -> 2.dp.toPx()
                        else -> 1.dp.toPx()
                    }

                    val color = if (i == 0) Color.Red else primaryColor.copy(alpha = if (isCardinal) 1f else 0.5f)

                    val startX = center.x + (radius - tickLength) * cos(angleRad).toFloat()
                    val startY = center.y + (radius - tickLength) * sin(angleRad).toFloat()

                    val endX = center.x + radius * cos(angleRad).toFloat()
                    val endY = center.y + radius * sin(angleRad).toFloat()

                    drawLine(
                        color = color,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round
                    )
                }

                // Qibla Indicator Line on Dial
                val qiblaRad = Math.toRadians(qiblaAngle.toDouble() - 90.0)
                drawLine(
                    color = secondaryColor,
                    start = center,
                    end = Offset(
                        center.x + (radius - 30.dp.toPx()) * cos(qiblaRad).toFloat(),
                        center.y + (radius - 30.dp.toPx()) * sin(qiblaRad).toFloat()
                    ),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }
            
            // Kaaba Icon at Qibla Angle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(qiblaAngle),
                contentAlignment = Alignment.TopCenter
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.kaaba_icon_1782894019643),
                    contentDescription = "Qibla",
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                )
            }

            // N E S W Labels
            Box(modifier = Modifier.fillMaxSize()) {
                Text("K", fontWeight = FontWeight.ExtraBold, color = Color.Red, modifier = Modifier.align(Alignment.TopCenter).padding(32.dp))
                Text("D", fontWeight = FontWeight.Bold, color = onSurfaceColor, modifier = Modifier.align(Alignment.CenterEnd).padding(32.dp))
                Text("G", fontWeight = FontWeight.Bold, color = onSurfaceColor, modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp))
                Text("B", fontWeight = FontWeight.Bold, color = onSurfaceColor, modifier = Modifier.align(Alignment.CenterStart).padding(32.dp))
            }
        }
        
        // Fixed Center Needle (Phone's top direction)
        Canvas(modifier = Modifier.size(240.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val needleLength = size.minDimension / 2
            
            val path = Path().apply {
                moveTo(center.x, center.y - needleLength)
                lineTo(center.x + 15.dp.toPx(), center.y + 20.dp.toPx())
                lineTo(center.x, center.y + 10.dp.toPx())
                lineTo(center.x - 15.dp.toPx(), center.y + 20.dp.toPx())
                close()
            }
            
            drawPath(
                path = path,
                color = primaryColor
            )
            
            drawCircle(
                color = primaryColor,
                radius = 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = surfaceColor,
                radius = 4.dp.toPx(),
                center = center
            )
        }
    }
}
