package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeGalleryScreen(
    currentTheme: AppTheme,
    onThemeSelect: (AppTheme) -> Unit,
    onBack: () -> Unit,
    viewModel: PrayerViewModel? = null
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Tema Galerisi", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("theme-list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Varsayılan: Kubbeli Zümrüt",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(AppTheme.entries, key = { it.name }) { theme ->
                ProfessionalThemeCard(theme, theme == currentTheme) { onThemeSelect(theme) }
            }
        }
    }
}

@Composable
private fun ProfessionalThemeCard(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val spec = theme.spec
    val colors = spec.colors(spec.prefersDark)
    val shape = spec.shapes.cardShape
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme-${theme.name}")
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = colors.background,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) colors.primary else colors.outline.copy(alpha = 0.45f)
        )
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header respects the theme's own metrics instead of a fixed padding.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spec.metrics.cardPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(colors.primary, colors.tertiary, colors.secondary)
                            )
                        )
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = theme.turkishName,
                        color = colors.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                if (selected) {
                    Surface(shape = spec.shapes.badgeShape, color = colors.primary) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = colors.onPrimary)
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "Aktif",
                                color = colors.onPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            ThemePreview(theme)

        }
    }
}

/**
 * Miniature of the real Home screen: hero countdown block + prayer strip.
 * Renders with the theme's own gradient, shape, border and typography so the
 * preview communicates structure, not just hue.
 */
@Composable
private fun ThemePreview(theme: AppTheme) {
    val spec = theme.spec
    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppTheme provides theme,
        LocalThemeSpec provides spec,
        LocalThemeColors provides spec.colors(spec.prefersDark),
        LocalThemeMetrics provides spec.metrics,
        LocalThemeIsDark provides spec.prefersDark,
        LocalThemeTypography provides spec.typography,
        LocalThemeMotion provides spec.motion
    ) {
        MaterialTheme(colorScheme = spec.toColorScheme(spec.prefersDark), typography = typographyFor(spec)) {
            ThemePrayerScene(
                timings = com.example.data.Timings("05:30", "06:45", "12:46", "16:12", "18:52", "18:52", "20:15", "05:20", "00:10"),
                remainingTime = "02:28:01",
                currentAndNext = "Güneş" to "Öğle",
                progress = .4f
            )
        }
    }
}

@Composable
fun ThemeGalleryDialog(
    currentTheme: AppTheme,
    onThemeSelect: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
    viewModel: PrayerViewModel? = null
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ThemeGalleryScreen(currentTheme, onThemeSelect, onDismiss, viewModel)
        }
    }
}
