package com.example.alarm

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeSunriseAlarmEntry(onClick: () -> Unit) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val preferences = listOf(SunriseAlarmStore.PREFERENCES, "prayer_daily_times", "sunrise_alarm_runtime")
            .map { context.getSharedPreferences(it, Context.MODE_PRIVATE) }
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        preferences.forEach { it.registerOnSharedPreferenceChangeListener(listener) }
        onDispose { preferences.forEach { it.unregisterOnSharedPreferenceChangeListener(listener) } }
    }
    val next = remember(revision) {
        SunriseAlarmScheduler.pending(context).filter { it.triggerAtMillis > System.currentTimeMillis() }
            .minByOrNull { it.triggerAtMillis }?.let {
                SunriseAlarmOccurrence(it.alarmId, it.sunriseAtMillis, it.triggerAtMillis, it.sourceDate)
            } ?: SunriseAlarmPlanner.allNext(context, SunriseAlarmStore(context).list()).firstOrNull()
    }
    val subtitle = next?.let {
        "Sonraki alarm ${SimpleDateFormat("d MMM • HH:mm", Locale.forLanguageTag("tr-TR")).format(Date(it.triggerAtMillis))}"
    } ?: "Güneşten 30 dk önce • Süreyi değiştirebilirsiniz"
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("home-sunrise-alarm").clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text("Güneş Alarmı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
