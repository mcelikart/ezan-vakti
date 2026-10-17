package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
internal fun HeritageHomeHeader(city: String, locale: Locale, onLocation: () -> Unit) {
    val style = LocalThemeSpec.current.headerStyle
    val c = MaterialTheme.colorScheme
    val center = style == HomeHeaderStyle.MIHRAB || style == HomeHeaderStyle.MASTHEAD
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start) {
        if (style == HomeHeaderStyle.MASTHEAD) HorizontalDivider(color = c.secondary, thickness = 2.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .then(if (style == HomeHeaderStyle.MIHRAB) Modifier.background(c.surface, RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 4.dp, bottomEnd = 4.dp)) else Modifier)
                .clickable(onClick = onLocation).padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (style == HomeHeaderStyle.SIDEBAR) {
                Box(Modifier.width(4.dp).height(40.dp).background(c.secondary))
                Spacer(Modifier.width(12.dp))
            } else {
                Icon(Icons.Default.LocationOn, null, Modifier.size(20.dp), tint = c.secondary)
                Spacer(Modifier.width(8.dp))
            }
            Text(city, Modifier.weight(1f), style = if (style == HomeHeaderStyle.COMPACT) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                color = c.primary, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ArrowDropDown, "Konum değiştir", tint = c.primary)
        }
        HeritageDate(locale, center)
        if (style == HomeHeaderStyle.RULE) {
            HorizontalDivider(color = c.primary)
            Spacer(Modifier.height(1.dp))
            HorizontalDivider(color = c.outlineVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeritageDate(locale: Locale, center: Boolean) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1000 - now % 1000) }
    }
    val date = remember(locale) { SimpleDateFormat("d MMMM yyyy, EEEE", locale) }
    val time = remember(locale) { SimpleDateFormat("HH:mm:ss", locale) }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = if (center) Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally) else Arrangement.SpaceBetween) {
        Text(date.format(now), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(time.format(now), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
