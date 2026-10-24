package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HijriDate
import com.example.data.Timings
import com.example.ui.theme.*
import java.util.Date

private data class ScenePrayer(val name: String, val time: String, val next: Boolean, val icon: ImageVector)

@Composable
fun ThemePrayerScene(
    timings: Timings, remainingTime: String, currentAndNext: Pair<String?, String?>?,
    progress: Float, date: Date = Date(), todayHijri: HijriDate? = null
) {
    val spec = LocalThemeSpec.current
    val strings = LocalAppStrings.current
    if (spec.id == AppTheme.CLASSIC_EMERALD.specId || spec.scheduleStyle == ScheduleStyle.CLASSIC) {
        Box(Modifier.fillMaxWidth().testTag("composition-DOME")) {
            HeroStyleGokKubbe(timings, remainingTime, currentAndNext, progress, strings, todayHijri = todayHijri)
        }
        return
    }
    Column(Modifier.fillMaxWidth().testTag("composition-DOME")) {
        HeroStyleGokKubbe(timings, remainingTime, currentAndNext, progress, strings, showPrayerTimes = false, todayHijri = todayHijri)
        val keys = listOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")
        val names = listOf(strings.fajr, strings.sunrise, strings.dhuhr, strings.asr, strings.maghrib, strings.isha)
        val times = listOf(timings.Fajr, timings.Sunrise, timings.Dhuhr, timings.Asr, timings.Maghrib, timings.Isha)
        val icons = listOf(Icons.Default.NightsStay, Icons.Default.WbTwilight, Icons.Default.WbSunny,
            Icons.Default.WbCloudy, Icons.Default.WbTwilight, Icons.Default.DarkMode)
        val prayers = keys.indices.map { ScenePrayer(names[it], times[it].take(5), keys[it] == currentAndNext?.second, icons[it]) }
        key(spec.scheduleStyle) { PrayerSchedule(prayers, spec.scheduleStyle) }
    }
}

@Composable
private fun BlurredGlassPrayerBox(
    shape: Shape = RoundedCornerShape(22.dp),
    isNext: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val c = MaterialTheme.colorScheme
    val isDark = LocalThemeIsDark.current
    val uniformShape = if (shape == CircleShape) CircleShape else RoundedCornerShape(22.dp)
    val borderBrush = if (isNext) {
        Brush.linearGradient(listOf(c.secondary, c.primary, c.secondary))
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (isDark) 0.56f else 0.84f),
                c.primary.copy(alpha = 0.52f),
                Color.White.copy(alpha = if (isDark) 0.28f else 0.56f)
            )
        )
    }

    Box(
        modifier = modifier
            .frostedGlassBackdrop(
                shape = uniformShape,
                tintColor = c.primary,
                tintAlpha = if (isNext) 0.78f else (if (isDark) 0.20f else 0.14f),
                surfaceVeilAlpha = if (isNext) 0.26f else (if (isDark) 0.36f else 0.44f)
            )
            .border(BorderStroke(if (isNext) 1.8.dp else 1.3.dp, borderBrush), uniformShape),
        content = content
    )
}

@Composable
private fun PrayerSchedule(prayers: List<ScenePrayer>, style: ScheduleStyle) {
    val c = MaterialTheme.colorScheme
    val spec = LocalThemeSpec.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = spec.metrics.screenGutter, vertical = 16.dp)
            .testTag("schedule-$style"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (style) {
            ScheduleStyle.LINES, ScheduleStyle.TIMELINE, ScheduleStyle.COMPACT -> {
                prayers.forEachIndexed { i, p ->
                    BlurredGlassPrayerBox(
                        shape = RoundedCornerShape(22.dp),
                        isNext = p.next,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = if (style == ScheduleStyle.COMPACT) 50.dp else 58.dp)
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (style) {
                                ScheduleStyle.TIMELINE -> Box(Modifier.width(32.dp).height(52.dp), contentAlignment = Alignment.Center) {
                                    Box(Modifier.width(1.dp).fillMaxHeight().background(c.outlineVariant))
                                    Surface(Modifier.size(if (p.next) 12.dp else 7.dp), CircleShape, color = if (p.next) c.onPrimary else c.primary) {}
                                }
                                ScheduleStyle.COMPACT -> Text("0${i + 1}", Modifier.width(32.dp).padding(start = 6.dp),
                                    color = if (p.next) c.onPrimary.copy(alpha = 0.85f) else c.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                                else -> Icon(p.icon, null, Modifier.padding(horizontal = 10.dp).size(22.dp), tint = if (p.next) c.onPrimary else c.secondary)
                            }
                            Text(p.name, Modifier.weight(1f), fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (p.next) c.onPrimary else c.onSurface)
                            Text(p.time, Modifier.padding(horizontal = 12.dp), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                                color = if (p.next) c.onPrimary else c.primary)
                        }
                    }
                }
            }
            ScheduleStyle.ALMANAC -> {
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("VAKİTLER", style = MaterialTheme.typography.labelLarge, color = c.primary)
                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(20.dp), tint = c.secondary)
                }
                HorizontalDivider(color = c.primary, thickness = 2.dp)
                prayers.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { p ->
                            BlurredGlassPrayerBox(
                                shape = RoundedCornerShape(22.dp),
                                isNext = p.next,
                                modifier = Modifier.weight(1f)
                            ) {
                                val ink = if (p.next) c.onPrimary else c.onSurface
                                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                                    Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ink)
                                    Text(p.time, fontSize = 28.sp, color = if (p.next) c.onPrimary else c.primary, fontWeight = FontWeight.ExtraBold)
                                    if (p.next) Box(Modifier.width(28.dp).height(3.dp).background(c.onPrimary))
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                val columns = if (style in listOf(ScheduleStyle.PAIRED, ScheduleStyle.PANELS)) 2 else 3
                prayers.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { p ->
                            val shape = when (style) {
                                ScheduleStyle.MEDALLIONS -> CircleShape
                                else -> RoundedCornerShape(22.dp)
                            }
                            val ink = if (p.next) c.onPrimary else c.onSurface
                            BlurredGlassPrayerBox(
                                shape = shape,
                                isNext = p.next,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (style == ScheduleStyle.PANELS) {
                                    Row(Modifier.fillMaxWidth().heightIn(min = 104.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.width(4.dp).height(64.dp).background(if (p.next) c.onPrimary else c.secondary))
                                        Column(Modifier.weight(1f).padding(12.dp)) {
                                            Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ink)
                                            Text(p.time, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = ink)
                                        }
                                        Icon(p.icon, null, Modifier.padding(end = 10.dp).size(20.dp), tint = ink.copy(alpha = .6f))
                                    }
                                } else {
                                    Column(
                                        Modifier.fillMaxWidth().heightIn(min = if (style == ScheduleStyle.ARCADES) 136.dp else 116.dp).padding(horizontal = 6.dp, vertical = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
                                    ) {
                                        Icon(p.icon, null, Modifier.size(22.dp), tint = if (p.next) c.onPrimary else c.secondary)
                                        Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ink, textAlign = TextAlign.Center)
                                        Text(p.time, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ink, letterSpacing = 0.sp)
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

