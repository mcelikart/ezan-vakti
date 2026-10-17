package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

@Composable
fun HourglassAnimation(progress: Float, modifier: Modifier = Modifier) {
    val sandColor = MaterialTheme.colorScheme.primary
    val sandColorDark = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    val frameStartColor = MaterialTheme.colorScheme.secondary
    val frameEndColor = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "sand_falling")
    val particleY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "particle_y"
    )

    Box(modifier = modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2

            val bulbWidth = width * 0.6f
            val bulbHeight = height * 0.45f
            val neckWidth = width * 0.05f

            val glassBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.4f)),
                start = Offset(centerX - bulbWidth/2, 0f),
                end = Offset(centerX + bulbWidth/2, 0f)
            )

            val frameBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(frameStartColor, frameEndColor),
                startY = 0f,
                endY = height
            )

            val sandBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(sandColorDark, sandColor, sandColorDark),
                startX = centerX - bulbWidth/2,
                endX = centerX + bulbWidth/2
            )

            // Top Bulb Path
            val topBulbPath = Path().apply {
                moveTo(centerX - bulbWidth / 2, centerY - bulbHeight)
                lineTo(centerX + bulbWidth / 2, centerY - bulbHeight)
                quadraticTo(
                    centerX + bulbWidth / 2, centerY - neckWidth * 2,
                    centerX + neckWidth, centerY
                )
                lineTo(centerX - neckWidth, centerY)
                quadraticTo(
                    centerX - bulbWidth / 2, centerY - neckWidth * 2,
                    centerX - bulbWidth / 2, centerY - bulbHeight
                )
                close()
            }

            // Bottom Bulb Path
            val bottomBulbPath = Path().apply {
                moveTo(centerX - neckWidth, centerY)
                lineTo(centerX + neckWidth, centerY)
                quadraticTo(
                    centerX + bulbWidth / 2, centerY + neckWidth * 2,
                    centerX + bulbWidth / 2, centerY + bulbHeight
                )
                lineTo(centerX - bulbWidth / 2, centerY + bulbHeight)
                quadraticTo(
                    centerX - bulbWidth / 2, centerY + neckWidth * 2,
                    centerX - neckWidth, centerY
                )
                close()
            }

            // Draw Glass Background with 3D reflection
            drawPath(topBulbPath, brush = glassBrush, style = Fill)
            drawPath(bottomBulbPath, brush = glassBrush, style = Fill)

            // Draw Top Sand (Decreasing)
            val topSandHeight = bulbHeight * (1f - progress)
            if (topSandHeight > 0) {
                clipPath(topBulbPath) {
                    drawRect(
                        brush = sandBrush,
                        topLeft = Offset(0f, centerY - topSandHeight),
                        size = androidx.compose.ui.geometry.Size(width, topSandHeight)
                    )
                }
            }

            // Draw Bottom Sand (Increasing)
            val bottomSandHeight = bulbHeight * progress
            if (bottomSandHeight > 0) {
                clipPath(bottomBulbPath) {
                    val sandTopY = centerY + bulbHeight - bottomSandHeight
                    // Draw a little mound
                    val moundPath = Path().apply {
                        moveTo(centerX - bulbWidth / 2, centerY + bulbHeight)
                        lineTo(centerX + bulbWidth / 2, centerY + bulbHeight)
                        lineTo(centerX + bulbWidth / 2, sandTopY + bottomSandHeight * 0.2f)
                        quadraticTo(
                            centerX, sandTopY - bottomSandHeight * 0.2f,
                            centerX - bulbWidth / 2, sandTopY + bottomSandHeight * 0.2f
                        )
                        close()
                    }
                    drawPath(moundPath, brush = sandBrush, style = Fill)
                    // Fill the rest below mound if needed
                    drawRect(
                        brush = sandBrush,
                        topLeft = Offset(0f, sandTopY + bottomSandHeight * 0.1f),
                        size = androidx.compose.ui.geometry.Size(width, bottomSandHeight)
                    )
                }
            }

            // Draw Falling Sand Stream
            if (progress > 0f && progress < 1f) {
                drawLine(
                    brush = sandBrush,
                    start = Offset(centerX, centerY),
                    end = Offset(centerX, centerY + bulbHeight - bottomSandHeight + bottomSandHeight*0.2f),
                    strokeWidth = neckWidth * 0.8f
                )
                
                // Falling particles
                val particleStart = centerY
                val particleEnd = centerY + bulbHeight - bottomSandHeight
                val currentParticleY = particleStart + (particleEnd - particleStart) * particleY
                drawCircle(
                    color = sandColor,
                    radius = neckWidth * 0.6f,
                    center = Offset(centerX, currentParticleY)
                )
            }

            // Draw Frame inner shadow / stroke
            drawPath(topBulbPath, brush = frameBrush, style = Stroke(width = 4.dp.toPx()))
            drawPath(bottomBulbPath, brush = frameBrush, style = Stroke(width = 4.dp.toPx()))

            // Add highlight on glass
            val highlightPathTop = Path().apply {
                moveTo(centerX - bulbWidth / 2 + 10f, centerY - bulbHeight + 10f)
                quadraticTo(
                    centerX - bulbWidth / 2 + 10f, centerY - neckWidth * 3,
                    centerX - neckWidth, centerY - 10f
                )
            }
            drawPath(highlightPathTop, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))

            // Top and Bottom Caps
            drawRoundRect(
                brush = frameBrush,
                topLeft = Offset(centerX - bulbWidth / 2 - 15f, centerY - bulbHeight - 12f),
                size = androidx.compose.ui.geometry.Size(bulbWidth + 30f, 12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            drawRoundRect(
                brush = frameBrush,
                topLeft = Offset(centerX - bulbWidth / 2 - 15f, centerY + bulbHeight),
                size = androidx.compose.ui.geometry.Size(bulbWidth + 30f, 12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
        }
    }
}
