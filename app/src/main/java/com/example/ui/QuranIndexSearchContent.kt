package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranAiSearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
internal fun QuranIndexSearchContent(
    onBack: () -> Unit,
    onOpenVerse: (surahId: Int, verseNumber: Int) -> Unit,
    search: suspend (String) -> List<QuranAiSearchResult>
) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<QuranAiSearchResult>?>(null) }
    var searchFailed by remember { mutableStateOf(false) }
    var expandedArabic by remember { mutableStateOf(emptySet<String>()) }
    var expandedNotes by remember { mutableStateOf(emptySet<String>()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose { searchJob?.cancel() }
    }

    fun submitSearch() {
        val submitted = query.trim()
        if (submitted.isBlank() || searching) return
        searchJob?.cancel()
        query = submitted
        searching = true
        results = null
        searchFailed = false
        expandedArabic = emptySet()
        expandedNotes = emptySet()
        keyboard?.hide()
        searchJob = scope.launch {
            try {
                val found = search(submitted)
                ensureActive()
                results = found
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ensureActive()
                searchFailed = true
            } finally {
                // A cancelled request must not clear a newer request's state.
                if (isActive) searching = false
            }
        }
    }

    fun changeQuery(value: String) {
        searchJob?.cancel()
        query = value
        searching = false
        results = null
        searchFailed = false
        expandedArabic = emptySet()
        expandedNotes = emptySet()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
    SubScreenScaffold(
        title = "Kur'an Fihristi",
        onBack = {
            searchJob?.cancel()
            onBack()
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                .testTag("quran-index-screen")
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = ::changeQuery,
                modifier = Modifier.fillMaxWidth().testTag("quran-index-query"),
                label = { Text("Konu veya soru") },
                placeholder = { Text("Örn. geçim sıkıntısı") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                trailingIcon = {
                    Row {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { changeQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Aramayı temizle")
                            }
                        }
                        IconButton(
                            onClick = ::submitSearch,
                            enabled = query.isNotBlank() && !searching,
                            modifier = Modifier.testTag("quran-index-submit")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Ayet ara")
                        }
                    }
                }
            )
            Spacer(Modifier.height(20.dp))

            val visibleResults = results
            when {
                searching -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().testTag("quran-index-loading"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Aranıyor…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                searchFailed -> {
                    Column(modifier = Modifier.testTag("quran-index-error")) {
                        Text(
                            "Arama tamamlanamadı.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = ::submitSearch) { Text("Tekrar dene") }
                    }
                }
                visibleResults == null -> {
                    Text(
                        "Aramak istediğiniz konuyu yazın.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                visibleResults.isEmpty() -> {
                    Text(
                        "İlgili ayet bulunamadı. Konuyu farklı ifade edin.",
                        modifier = Modifier.testTag("quran-index-empty"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("quran-index-results"),
                        contentPadding = PaddingValues(
                            bottom = com.example.ui.theme.navBarBottomPadding(extra = 24.dp)
                        )
                    ) {
                        item {
                            Text(
                                "${visibleResults.size} ayet",
                                modifier = Modifier.padding(bottom = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(visibleResults, key = { "${it.surahId}:${it.verseNumber}" }) { verse ->
                            val key = "${verse.surahId}:${verse.verseNumber}"
                            QuranIndexVerseRow(
                                verse = verse,
                                arabicExpanded = key in expandedArabic,
                                notesExpanded = key in expandedNotes,
                                onToggleArabic = {
                                    expandedArabic = if (key in expandedArabic) expandedArabic - key else expandedArabic + key
                                },
                                onToggleNotes = {
                                    expandedNotes = if (key in expandedNotes) expandedNotes - key else expandedNotes + key
                                },
                                onOpenVerse = { onOpenVerse(verse.surahId, verse.verseNumberInt) }
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuranIndexVerseRow(
    verse: QuranAiSearchResult,
    arabicExpanded: Boolean,
    notesExpanded: Boolean,
    onToggleArabic: () -> Unit,
    onToggleNotes: () -> Unit,
    onOpenVerse: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var menuExpanded by remember(verse.surahId, verse.verseNumber) { mutableStateOf(false) }
    val reference = "${verse.surahName}, ${verse.verseNumber}. ayet"
    val verseText = remember(verse) {
        buildString {
            if (verse.arabic.isNotBlank()) appendLine(verse.arabic).appendLine()
            appendLine(verse.translation)
            append("(${verse.surahName}, ${verse.verseNumber}. ayet)")
            if (verse.aiTafsirInsight.isNotBlank()) {
                appendLine().appendLine().appendLine("Meal notları:")
                append(verse.aiTafsirInsight)
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp)) {
        Text(
            reference,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            verse.translation,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 26.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (arabicExpanded && verse.arabic.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                verse.arabic,
                modifier = Modifier.fillMaxWidth().testTag("quran-index-arabic-${verse.surahId}-${verse.verseNumberInt}"),
                fontSize = 25.sp,
                lineHeight = 42.sp,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (notesExpanded && verse.aiTafsirInsight.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Meal notları",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                verse.aiTafsirInsight,
                modifier = Modifier.testTag("quran-index-notes-${verse.surahId}-${verse.verseNumberInt}"),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onOpenVerse, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text("Kur'an'da aç")
            }
            if (verse.arabic.isNotBlank()) {
                TextButton(onClick = onToggleArabic, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Arapça")
                    Icon(
                        if (arabicExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (arabicExpanded) "Arapçayı gizle" else "Arapçayı göster",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (verse.aiTafsirInsight.isNotBlank()) {
                TextButton(onClick = onToggleNotes, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Notlar")
                    Icon(
                        if (notesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (notesExpanded) "Meal notlarını gizle" else "Meal notlarını göster",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Ayet işlemleri", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Kopyala") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            clipboard.setText(AnnotatedString(verseText))
                            Toast.makeText(context, "Ayet kopyalandı", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Paylaş") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, verseText)
                            }
                            runCatching { context.startActivity(Intent.createChooser(share, "Ayet paylaş")) }
                        }
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}
