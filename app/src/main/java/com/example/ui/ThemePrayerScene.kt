package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Timings
import com.example.ui.theme.*
import java.util.Date

private data class ScenePrayer(val name: String, val time: String, val next: Boolean, val icon: ImageVector)

@Composable
fun ThemePrayerScene(
    timings: Timings, remainingTime: String, currentAndNext: Pair<String?, String?>?,
    progress: Float, date: Date = Date()
) {
    val spec = LocalThemeSpec.current
    val strings = LocalAppStrings.current
    if (spec.id == AppTheme.CLASSIC_EMERALD.specId) {
        HeroStyleGokKubbe(timings, remainingTime, currentAndNext, progress, strings)
        return
    }
    Column(Modifier.fillMaxWidth().testTag("composition-DOME")) {
        // Pin the original dome geometry and border; only its ink follows the palette.
        CompositionLocalProvider(LocalAppTheme provides AppTheme.CLASSIC_EMERALD) {
            HeroStyleGokKubbe(timings, remainingTime, currentAndNext, progress, strings, showPrayerTimes = false)
        }
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
                    Row(Modifier.fillMaxWidth().heightIn(min = if (style == ScheduleStyle.COMPACT) 44.dp else 56.dp)
                        .background(if (p.next) c.primaryContainer else Color.Transparent),
                        verticalAlignment = Alignment.CenterVertically) {
                        when (style) {
                            ScheduleStyle.TIMELINE -> Box(Modifier.width(32.dp).height(52.dp), contentAlignment = Alignment.Center) {
                                Box(Modifier.width(1.dp).fillMaxHeight().background(c.outlineVariant))
                                Surface(Modifier.size(if (p.next) 12.dp else 7.dp), CircleShape, color = if (p.next) c.primary else c.outline) {}
                            }
                            ScheduleStyle.COMPACT -> Text("0${i + 1}", Modifier.width(32.dp).padding(start = 6.dp),
                                color = c.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            else -> Icon(p.icon, null, Modifier.padding(horizontal = 10.dp).size(20.dp), tint = c.secondary)
                        }
                        Text(p.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                            color = if (p.next) c.onPrimaryContainer else c.onBackground)
                        Text(p.time, Modifier.padding(horizontal = 12.dp), fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                            color = if (p.next) c.onPrimaryContainer else c.primary)
                    }
                    if (style == ScheduleStyle.LINES && i < prayers.lastIndex) HorizontalDivider(color = c.outlineVariant)
                }
            }
            ScheduleStyle.ALMANAC -> {
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("VAKİTLER", style = MaterialTheme.typography.labelLarge, color = c.primary)
                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(20.dp), tint = c.secondary)
                }
                HorizontalDivider(color = c.primary, thickness = 2.dp)
                prayers.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        pair.forEach { p ->
                            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                                Text(p.name, style = MaterialTheme.typography.bodyMedium)
                                Text(p.time, fontSize = 28.sp, color = if (p.next) c.secondary else c.primary, fontWeight = FontWeight.SemiBold)
                                if (p.next) Box(Modifier.width(28.dp).height(3.dp).background(c.secondary))
                            }
                        }
                    }
                    HorizontalDivider(color = c.outlineVariant)
                }
            }
            else -> {
                val columns = if (style in listOf(ScheduleStyle.PAIRED, ScheduleStyle.PANELS)) 2 else 3
                prayers.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (style == ScheduleStyle.RIBBON) 2.dp else 8.dp)) {
                        row.forEach { p ->
                            val shape = when (style) {
                                ScheduleStyle.ARCADES -> RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                                ScheduleStyle.MEDALLIONS -> CircleShape
                                ScheduleStyle.RIBBON -> RoundedCornerShape(0.dp)
                                ScheduleStyle.TILES -> RoundedCornerShape(4.dp)
                                else -> spec.shapes.cardShape
                            }
                            val background = if (p.next) c.primary else if (style == ScheduleStyle.RIBBON) c.surfaceVariant else c.surface
                            val ink = if (p.next) c.onPrimary else c.onSurface
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = shape, color = background,
                                border = if (style == ScheduleStyle.RIBBON) null else BorderStroke(1.dp, if (p.next) c.secondary else c.outlineVariant)
                            ) {
                                if (style == ScheduleStyle.PANELS) {
                                    Row(Modifier.heightIn(min = 100.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.width(4.dp).height(64.dp).background(c.secondary))
                                        Column(Modifier.weight(1f).padding(12.dp)) {
                                            Text(p.name, style = MaterialTheme.typography.bodyMedium, color = ink)
                                            Text(p.time, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = ink)
                                        }
                                        Icon(p.icon, null, Modifier.padding(end = 10.dp).size(18.dp), tint = ink.copy(alpha = .6f))
                                    }
                                } else {
                                    Column(
                                        Modifier.heightIn(min = if (style == ScheduleStyle.ARCADES) 136.dp else 112.dp).padding(horizontal = 4.dp, vertical = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                                    ) {
                                        Icon(p.icon, null, Modifier.size(20.dp), tint = if (p.next) c.onPrimary else c.secondary)
                                        Text(p.name, style = MaterialTheme.typography.labelMedium, color = ink, textAlign = TextAlign.Center)
                                        Text(p.time, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = ink, letterSpacing = 0.sp)
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
