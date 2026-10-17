package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.navBarBottomPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerLearningScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prayer_learning_v1", Context.MODE_PRIVATE) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(1) }
    val prayer = DailyPrayer.entries.firstOrNull { it.name == selected }
    if (prayer != null) {
        BackHandler { selected = null }
        key(prayer) {
            PrayerLessonScreen(
                prayer = prayer,
                initialStep = prefs.getInt(prayer.name, 0),
                onProgress = { prefs.edit().putInt(prayer.name, it).apply() },
                onBack = { selected = null }
            )
        }
        return
    }
    Scaffold(
        modifier = Modifier.padding(bottom = navBarBottomPadding()),
        topBar = { TopAppBar(title = { Text("Namaz Öğreniyorum") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
        }) }
    ) { inset ->
        Column(Modifier.fillMaxSize().padding(inset)) {
            TabRow(selectedTabIndex = tab) {
                listOf("Hazırlık", "Namazlar", "Dualar").forEachIndexed { i, title ->
                    Tab(tab == i, { tab = i }, text = { Text(title) })
                }
            }
            LazyColumn(Modifier.fillMaxSize().testTag("prayer-learning-list"),
                contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Text(PrayerLessons.SCOPE, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                when (tab) {
                    0 -> {
                        items(PrayerLessons.preparation) { (title, body) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(title, style = MaterialTheme.typography.titleMedium)
                                Text(body, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        item { HorizontalDivider(); Spacer(Modifier.height(16.dp)); Text("Abdest sırası", style = MaterialTheme.typography.titleLarge) }
                        items(PrayerLessons.ablution.size) { i ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${i + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(PrayerLessons.ablution[i], style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        item { Text("Sağlık nedeniyle ayakta duramama, su kullanamama veya başka özel durumlarda kişisel durumunuzu bir din görevlisine danışın.", style = MaterialTheme.typography.bodySmall) }
                        item { SourceLink("Diyanet • Abdest", PrayerLessons.ABLUTION_SOURCE) }
                    }
                    1 -> {
                        item { Text(PrayerLessons.NOTE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(DailyPrayer.entries) { p ->
                            val total = remember(p) { PrayerLessons.steps(p).size }
                            val saved = prefs.getInt(p.name, 0).coerceIn(0, total)
                            Surface(
                                Modifier.fillMaxWidth().clickable { selected = p.name }.testTag("learn-${p.name}"),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Surface(Modifier.size(44.dp), CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                        Box(contentAlignment = Alignment.Center) { Text("${p.rakats}", fontWeight = FontWeight.Bold, fontSize = 22.sp) }
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(p.title, style = MaterialTheme.typography.titleMedium)
                                        Text("${p.rakats} rekât farz", style = MaterialTheme.typography.bodySmall)
                                        if (saved > 0) Text(if (saved == total) "Ders tamamlandı" else "Kaldığın yer: ${saved + 1}. adım",
                                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Icon(if (saved == total) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward, null,
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        item { SourceLink("Diyanet • Namazın kılınışı", PrayerLessons.SOURCE) }
                    }
                    else -> {
                        item { Text("Latin harfleri telaffuzu yaklaşık gösterir. Doğru okuyuşu bir öğreticiden dinleyerek öğrenin.", style = MaterialTheme.typography.bodySmall) }
                        items(PrayerLessons.recitations.values.toList(), key = { it.id }) { RecitationBlock(it) }
                        item { SourceLink("Diyanet • Namaz ilmihali", PrayerLessons.SOURCE) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerLessonScreen(prayer: DailyPrayer, initialStep: Int, onProgress: (Int) -> Unit, onBack: () -> Unit) {
    val steps = remember(prayer) { PrayerLessons.steps(prayer) }
    var index by rememberSaveable(prayer.name) { mutableIntStateOf(initialStep.coerceIn(0, steps.size)) }
    val done = index == steps.size
    val scroll = rememberScrollState()
    LaunchedEffect(index) { onProgress(index); scroll.scrollTo(0) }
    Scaffold(
        modifier = Modifier.padding(bottom = navBarBottomPadding()),
        topBar = {
            TopAppBar(title = { Text("${prayer.title} • ${prayer.rakats} rekât farz", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Namazlara dön") } },
                actions = { IconButton(onClick = { index = 0 }) { Icon(Icons.Default.RestartAlt, "Dersi baştan başlat") } })
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { index-- }, enabled = index > 0, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Önceki")
                }
                Button(onClick = { if (done) onBack() else index++ }, modifier = Modifier.weight(1f).testTag("lesson-next")) {
                    Text(if (done) "Namazlar" else if (index == steps.lastIndex) "Tamamla" else "Sonraki")
                    Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
    ) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(scroll).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            LinearProgressIndicator(progress = { index.toFloat() / steps.size }, modifier = Modifier.fillMaxWidth())
            if (done) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Ders tamamlandı", style = MaterialTheme.typography.headlineMedium)
                Text("${prayer.title} namazının farzına ait adımları inceledin. İhtiyaç duyduğunda bu dersi yeniden açabilirsin.")
                Text("Bu kayıt, öğrenme ilerlemesidir; namaz kılındığına dair kayıt değildir.", style = MaterialTheme.typography.bodySmall)
            } else {
                val step = steps[index]
                Text("ADIM ${index + 1} / ${steps.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(prayer.rakats) { r ->
                        val active = step.rakat == r + 1
                        Surface(Modifier.weight(1f), color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
                            Text("${r + 1}. rekât", Modifier.padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Text(step.title, Modifier.testTag("lesson-step-title"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(step.instruction, style = MaterialTheme.typography.bodyLarge)
                step.recitations.forEach { id -> RecitationBlock(PrayerLessons.recitations.getValue(id)) }
                if (step.recitations.isEmpty()) {
                    Text("Hareketler arasında bedenin sakinleşmesine zaman tanıyın.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RecitationBlock(recitation: PrayerRecitation) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(recitation.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(recitation.arabic, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 26.sp, lineHeight = 44.sp, textDirection = TextDirection.Rtl,
            textAlign = androidx.compose.ui.text.style.TextAlign.Right))
        Text(recitation.reading, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SourceLink(label: String, url: String) {
    val uri = LocalUriHandler.current
    val context = LocalContext.current
    TextButton(onClick = { runCatching { uri.openUri(url) }.onFailure {
        Toast.makeText(context, "Bağlantı açılamadı.", Toast.LENGTH_SHORT).show()
    } }) { Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(label) }
}
