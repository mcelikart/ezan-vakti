package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

internal fun contrast(foreground: Color, background: Color): Double {
    val a = foreground.compositeOver(background).luminance().toDouble()
    val b = background.luminance().toDouble()
    return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
}

internal fun readable(color: Color, background: Color, minimum: Double = 4.5): Color {
    if (contrast(color, background) >= minimum) return color
    val target = if (contrast(Color.Black, background) > contrast(Color.White, background)) Color.Black else Color.White
    for (step in 1..100) {
        val candidate = lerp(color.copy(alpha = 1f), target, step / 100f)
        if (contrast(candidate, background) >= minimum) return candidate
    }
    return target
}

internal fun ThemeColors.accessible(): ThemeColors {
    val p = readable(primary, surface)
    val s = readable(secondary, surface)
    val t = readable(tertiary, surface)
    val gradientText = listOf(buttonTextColor, Color.White, Color.Black).maxBy { text ->
        cardGradient.minOf { contrast(text, it) }
    }
    return copy(
        primary = p, onPrimary = readable(onPrimary, p),
        secondary = s, onSecondary = readable(onSecondary, s),
        tertiary = t, onTertiary = readable(onTertiary, t),
        onPrimaryContainer = readable(onPrimaryContainer, primaryContainer),
        onSecondaryContainer = readable(onSecondaryContainer, secondaryContainer),
        onBackground = readable(onBackground, background),
        onSurface = readable(onSurface, surface),
        onSurfaceVariant = readable(onSurfaceVariant, surfaceVariant),
        outline = readable(outline, surface, 3.0),
        buttonTextColor = gradientText
    )
}
