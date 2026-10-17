package com.example.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.APP_BACKGROUND_ITEMS
import com.example.ui.theme.AppBackgroundItem
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.LocalPatternIntensity
import com.example.ui.theme.drawCustomBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundGalleryScreen(
    currentBgId: Int,
    currentTheme: AppTheme = LocalAppTheme.current,
    patternIntensity: Float = LocalPatternIntensity.current,
    onPatternIntensityChange: (Float) -> Unit = {},
    onBackgroundSelect: (Int) -> Unit,
    onBack: () -> Unit
) {
    val allBackgrounds = remember { APP_BACKGROUND_ITEMS }

    val initialIndex = remember(currentBgId) {
        val found = allBackgrounds.indexOfFirst { it.id == currentBgId }
        if (found >= 0) found else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (allBackgrounds.size - 1).coerceAtLeast(0)),
        pageCount = { allBackgrounds.size }
    )
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kağıt Dokuları & Arkaplanlar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${allBackgrounds.size} • Yana kaydırarak tam ekran inceleyin",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    val activeBg = allBackgrounds.getOrNull(pagerState.currentPage)
                    val isCurrent = activeBg?.id == currentBgId
                    if (isCurrent) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Aktif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive Full-Screen Swipeable Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                pageSpacing = 14.dp
            ) { page ->
                val bgItem = allBackgrounds[page]
                val isSelectedBg = bgItem.id == currentBgId

                BackgroundCardFullPreview(
                    item = bgItem,
                    pageIndex = page + 1,
                    totalPages = allBackgrounds.size,
                    theme = currentTheme,
                    intensity = patternIntensity,
                    isActive = isSelectedBg,
                    onApply = {
                        onBackgroundSelect(bgItem.id)
                    }
                )
            }

            // Pattern Intensity Slider Control Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Desen Şiddeti & Belirginliği",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "%${(patternIntensity * 100).toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Interactive Slider
                    Slider(
                        value = patternIntensity,
                        onValueChange = onPatternIntensityChange,
                        valueRange = 0.2f..2.5f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                    )

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val presets = listOf(
                            "Hafif" to 0.5f,
                            "Dengeli" to 1.0f,
                            "Belirgin" to 1.5f,
                            "Yoğun" to 2.0f
                        )
                        presets.forEach { (label, presetValue) ->
                            val isSelectedPreset = kotlin.math.abs(patternIntensity - presetValue) < 0.12f
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelectedPreset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelectedPreset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .clickable { onPatternIntensityChange(presetValue) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelectedPreset) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelectedPreset) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation & Thumbnails Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dot Page Indicators
                Row(
                    modifier = Modifier.padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val visibleDotsCount = 9
                    val startDot = (pagerState.currentPage - visibleDotsCount / 2).coerceIn(0, (allBackgrounds.size - visibleDotsCount).coerceAtLeast(0))
                    val endDot = (startDot + visibleDotsCount).coerceAtMost(allBackgrounds.size)

                    for (index in startDot until endDot) {
                        val isPageActive = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (isPageActive) 22.dp else 7.dp,
                            label = "dotWidth"
                        )
                        val dotColor = if (isPageActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        }

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(6.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(dotColor)
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                        )
                    }
                }

                // Horizontal Thumbnails Bar with Visible Light Procedural Vector Textures
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    itemsIndexed(allBackgrounds) { index, item ->
                        val isPage = pagerState.currentPage == index
                        val isApplied = item.id == currentBgId
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = item.primaryColor,
                            border = if (isPage) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .width(96.dp)
                                .height(52.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawCustomBackground(bgId = item.id, theme = currentTheme, isDark = false, intensity = patternIntensity)
                                    .padding(6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color.Black.copy(alpha = 0.7f)
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 8.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (isApplied) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.White.copy(alpha = 0.85f)
                                    ) {
                                        Text(
                                            text = item.name,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackgroundGalleryDialog(
    currentBgId: Int,
    currentTheme: AppTheme = LocalAppTheme.current,
    patternIntensity: Float = LocalPatternIntensity.current,
    onPatternIntensityChange: (Float) -> Unit = {},
    onBackgroundSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BackgroundGalleryScreen(
                currentBgId = currentBgId,
                currentTheme = currentTheme,
                patternIntensity = patternIntensity,
                onPatternIntensityChange = onPatternIntensityChange,
                onBackgroundSelect = {
                    onBackgroundSelect(it)
                },
                onBack = onDismiss
            )
        }
    }
}

@Composable
private fun BackgroundCardFullPreview(
    item: AppBackgroundItem,
    pageIndex: Int,
    totalPages: Int,
    theme: AppTheme,
    intensity: Float = 1.0f,
    isActive: Boolean,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(12.dp, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isActive) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info & Swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "$pageIndex / $totalPages",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = item.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Color Swatches
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(item.primaryColor)
                            .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(item.secondaryColor)
                            .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // FULL LIVE WALLPAPER CANVAS WITH REALISTIC FLOATING APP WIDGETS
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = item.primaryColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawCustomBackground(bgId = item.id, theme = theme, isDark = false, intensity = intensity)
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Floating Location & Clock Widget (High-Contrast Clean Surface)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.88f),
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "İstanbul, Türkiye",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "14 Safer 1448",
                                        fontSize = 9.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F4C3A)
                                ) {
                                    Text(
                                        text = "14:28:45",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD54F),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Center Visual Callout / Texture Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.88f),
                                shadowElevation = 3.dp,
                                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Açık Tonlu Kağıt Dokusu",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }

                        // Bottom Floating Prayer Next Card Widget
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.90f),
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "SIRADAKİ VAKİT",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "İkindi • 16:48",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F4C3A).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFF0F4C3A).copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "01:42:15",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F4C3A),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action / Apply Button
            if (isActive) {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        disabledContentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("✓ Seçili Arka Plan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bu Arka Planı Uygula", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
