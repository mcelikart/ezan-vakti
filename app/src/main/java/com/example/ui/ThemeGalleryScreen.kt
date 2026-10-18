package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    viewModel: PrayerViewModel? = null,
    currentColorVariant: Int? = null,
    onColorVariantSelect: ((Int) -> Unit)? = null
) {
    val vmVariant by (viewModel?.themeColorVariant?.collectAsState() ?: remember { mutableIntStateOf(0) })
    val localVariant = LocalThemeColorVariant.current
    var fallbackVariant by remember { mutableIntStateOf(localVariant) }
    val activeVariant = currentColorVariant ?: if (viewModel != null) vmVariant else fallbackVariant

    val selectVariant: (Int) -> Unit = { idx ->
        fallbackVariant = idx
        onColorVariantSelect?.invoke(idx)
        viewModel?.setThemeColorVariant(idx)
    }

    val themes = remember { AppTheme.entries.toList() }
    val activeOptions = remember(currentTheme) { currentTheme.spec.colorOptions }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tema Galerisi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Kubbeli Zümrüt & 10 Glassmorphic Kubbe Teması",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (currentTheme != AppTheme.DEFAULT) {
                OutlinedButton(
                    onClick = {
                        onThemeSelect(AppTheme.DEFAULT)
                        viewModel?.resetToOriginalSingleTheme()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Restore, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Varsayılan Kubbeli Zümrüt'e Dön")
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().testTag("theme-list")
            ) {
                // Color Options Card for the currently selected theme (Kubbeli Zümrüt AND all other themes)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ThemeColorOptionsPanel(
                        theme = currentTheme,
                        options = activeOptions,
                        selectedVariant = activeVariant,
                        onSelectVariant = selectVariant
                    )
                }

                items(themes, key = { it.name }) { theme ->
                    ThemePreviewCard(
                        theme = theme,
                        selected = theme == currentTheme,
                        activeColorVariant = if (theme == currentTheme) activeVariant else 0,
                        onClick = { onThemeSelect(theme) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeColorOptionsPanel(
    theme: AppTheme,
    options: List<ThemeColorOption>,
    selectedVariant: Int,
    onSelectVariant: (Int) -> Unit
) {
    val isGlassTheme = theme != AppTheme.CLASSIC_EMERALD && theme.spec.isGlass
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_color_options_panel"),
        shape = theme.cardShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isGlassTheme) 0.68f else 0.55f),
        border = theme.buttonBorder
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${theme.turkishName} • Renk Seçenekleri",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (theme == AppTheme.CLASSIC_EMERALD)
                                "Klasik Kubbeli Zümrüt yapısını bozmadan renk tonunu seçin"
                            else
                                "Seçili Glassmorphic temanın kristal renk tonunu özelleştirin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEach { option ->
                    val isSelected = selectedVariant == option.index
                    Surface(
                        modifier = Modifier
                            .clip(theme.buttonShape)
                            .clickable { onSelectVariant(option.index) }
                            .testTag("color_option_${option.index}"),
                        shape = theme.buttonShape,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Dual-tone color swatch circle
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(option.primaryPreview, option.accentPreview)
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.subtitle,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    theme: AppTheme,
    selected: Boolean,
    activeColorVariant: Int,
    onClick: () -> Unit
) {
    val spec = theme.spec
    val colors = spec.colors(spec.prefersDark, activeColorVariant)
    val isGlass = theme != AppTheme.CLASSIC_EMERALD && spec.isGlass
    val cardFrameShape = if (isGlass) spec.shapes.cardShape else RoundedCornerShape(18.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardFrameShape)
            .clickable(onClick = onClick),
        shape = cardFrameShape,
        border = if (selected) BorderStroke(2.2.dp, colors.primary)
        else if (isGlass) spec.buttonBorder
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (isGlass)
                MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .drawCustomBackground(
                        theme = theme,
                        isDark = spec.prefersDark,
                        intensity = 0.85f,
                        colorVariant = activeColorVariant
                    )
                    .padding(10.dp)
            ) {
                Surface(
                    shape = spec.shapes.cardShape,
                    color = if (isGlass) colors.surface.copy(alpha = 0.62f) else colors.surface.copy(alpha = 0.90f),
                    border = if (isGlass) spec.buttonBorder else BorderStroke(1.dp, colors.goldMotifColor.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isGlass) {
                            // Frosted diagonal glass highlight band inside preview
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.25f),
                                                Color.Transparent,
                                                colors.glowColor.copy(alpha = 0.14f)
                                            )
                                        )
                                    )
                            )
                        }
                        // Dome arc silhouette inside mini-card
                        val arcColor = colors.goldMotifColor
                        val primaryInk = colors.primary
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cx = w * 0.5f
                            val cy = h * 0.72f
                            val rx = w * 0.38f
                            val ry = h * 0.45f
                            drawArc(
                                color = arcColor.copy(alpha = 0.55f),
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(cx - rx, cy - ry),
                                size = androidx.compose.ui.geometry.Size(rx * 2, ry * 2),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            val spire = Path().apply {
                                moveTo(cx, cy - ry - 6.dp.toPx())
                                lineTo(cx - 3.dp.toPx(), cy - ry)
                                lineTo(cx + 3.dp.toPx(), cy - ry)
                                close()
                            }
                            drawPath(spire, primaryInk)
                        }

                        Column(
                            Modifier.fillMaxSize().padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "02:14:08",
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Surface(
                                    shape = spec.shapes.badgeShape,
                                    color = colors.primaryContainer,
                                    border = if (isGlass) BorderStroke(0.8.dp, colors.goldMotifColor.copy(alpha = 0.6f)) else null
                                ) {
                                    Text(
                                        text = if (isGlass) "CAM" else "KLASİK",
                                        color = colors.onPrimaryContainer,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            // Mini preview of the theme's custom button silhouette
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(spec.shapes.buttonShape)
                                        .background(Brush.horizontalGradient(colors.cardGradient))
                                        .border(spec.buttonBorder, spec.shapes.buttonShape)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Buton",
                                        color = colors.buttonTextColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    spec.colorOptions.take(4).forEach { opt ->
                                        Box(
                                            Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(opt.primaryPreview)
                                                .border(0.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (selected) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primary,
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, "Seçili", tint = colors.onPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(
                    theme.turkishName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    theme.featureTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun ThemeSelectorDialog(
    currentTheme: AppTheme,
    onThemeSelect: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
    viewModel: PrayerViewModel? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize()) {
            ThemeGalleryScreen(currentTheme, onThemeSelect, onDismiss, viewModel)
        }
    }
}
