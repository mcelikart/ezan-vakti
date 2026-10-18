package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DailyPrayer
import com.example.data.PrayerLessonStep
import com.example.data.PrayerLessons
import com.example.data.PrayerPosture
import com.example.data.PrayerRecitation
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.LocalPatternIntensity
import com.example.ui.theme.LocalSelectedBackground
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.drawCustomBackground
import com.example.ui.theme.navBarBottomPadding
import java.util.Locale
import kotlin.math.roundToInt

private const val PREFS = "prayer_learning_v1"

private data class PostureVisualMeta(
    val postureTitle: String,
    val gazeTarget: String,
    val maleCheckpoint: String,
    val femaleCheckpoint: String,
    val keyPointBadge: String
)

private fun PrayerPosture.visualMeta(): PostureVisualMeta = when (this) {
    PrayerPosture.INTENTION -> PostureVisualMeta(
        postureTitle = "Kalben Niyet & Kıbleye Yöneliş",
        gazeTarget = "Bakış: Secde edilecek nokta",
        maleCheckpoint = "Kıbleye dönülür, ayaklar yaklaşık 4 parmak açık ve paralel tutulur; kalben namaza niyet edilir.",
        femaleCheckpoint = "Kıbleye dönülür, ayak bilekleri birbirine yakın tutulur; kalben hangi namazın kılınacağına niyet edilir.",
        keyPointBadge = "Kıbleye Tam Yöneliş"
    )
    PrayerPosture.TAKBIR -> PostureVisualMeta(
        postureTitle = "İftitah Tekbiri (Allâhü Ekber)",
        gazeTarget = "Bakış: Secde edilecek nokta",
        maleCheckpoint = "Avuç içleri kıbleye dönük, başparmaklar kulak memelerine değecek şekilde eller kaldırılır.",
        femaleCheckpoint = "Avuç içleri kıbleye dönük, parmak uçları omuz/göğüs hizasına gelecek şekilde eller kaldırılır.",
        keyPointBadge = "Avuç İçleri Kıbleye"
    )
    PrayerPosture.STANDING -> PostureVisualMeta(
        postureTitle = "Kıyam (Ayakta Okuma Duruşu)",
        gazeTarget = "Bakış: Secde edilecek nokta",
        maleCheckpoint = "Sağ el sol elin bileği üzerine konularak göbek altında bağlanır; dik ve vakur durulur.",
        femaleCheckpoint = "Sağ el sol elin üzerine konularak göğüs üstünde bağlanır; omuzlar sakin tutulur.",
        keyPointBadge = "El Bağlama & Kıraat"
    )
    PrayerPosture.BOWING -> PostureVisualMeta(
        postureTitle = "Rükû (Eğilme Duruşu)",
        gazeTarget = "Bakış: Ayak sırtları / uçları",
        maleCheckpoint = "Sırt ve baş dümdüz (90°) olacak şekilde eğilinir; parmaklar açık olarak dizler kavranır.",
        femaleCheckpoint = "Sırt hafif meyilli (~70°) olacak şekilde eğilinir; parmaklar kapalı olarak dizlerin üzerine konur.",
        keyPointBadge = "3x Sübhâne Rabbiyel Azîm"
    )
    PrayerPosture.UPRIGHT -> PostureVisualMeta(
        postureTitle = "Kavme (Rükûdan Doğrulma)",
        gazeTarget = "Bakış: Secde edilecek nokta",
        maleCheckpoint = "Rükûdan tam dik duruma gelinir, eller yanlara salınır ve bir tesbih miktarı sakin beklenir.",
        femaleCheckpoint = "Rükûdan tam dik duruma gelinir, eller yanlara bırakılır ve acele etmeden secdeye inilir.",
        keyPointBadge = "Tam Dik Doğruluş"
    )
    PrayerPosture.PROSTRATION -> PostureVisualMeta(
        postureTitle = "Secde (Yere Kapanış - 7 Uzuv)",
        gazeTarget = "Bakış: Burun ucu kenarları",
        maleCheckpoint = "Alın+burun, iki avuç, iki diz ve kıbleye bükülü ayak parmakları yerdedir; dirsekler yerden kalkıktır.",
        femaleCheckpoint = "Alın+burun yere konur; kollar yanlara bitişik ve karın uyluklara yakın, toplu vaziyette secde edilir.",
        keyPointBadge = "3x Sübhâne Rabbiyel A'lâ"
    )
    PrayerPosture.SITTING -> PostureVisualMeta(
        postureTitle = "Ka'de / Celse (Oturuş Duruşu)",
        gazeTarget = "Bakış: Kucak ve ellerin üzeri",
        maleCheckpoint = "Sol ayak yatırılarak üzerine oturulur, sağ ayak parmakları kıbleye bükülü dik tutulur.",
        femaleCheckpoint = "Ayaklar sağ tarafa çıkarılarak sol kalça üzerine (teverrük) oturulur; eller uyluklar üzerindedir.",
        keyPointBadge = "Tahiyyat Oturuşu"
    )
    PrayerPosture.GREETING -> PostureVisualMeta(
        postureTitle = "Selâm (Sağa ve Sola Selâm)",
        gazeTarget = "Bakış: Sağ ve sol omuz uçları",
        maleCheckpoint = "Önce baş sağ omuza çevrilerek 'Esselâmü aleyküm ve rahmetullah', ardından sol omuza çevrilerek selam verilir.",
        femaleCheckpoint = "Önce sağ omuz ucuna, sonra sol omuz ucuna bakılarak 'Esselâmü aleyküm ve rahmetullah' denir.",
        keyPointBadge = "Önce Sağ, Sonra Sol"
    )
}

@Composable
fun PrayerLearningScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    val theme = LocalAppTheme.current
    val selectedBgId = LocalSelectedBackground.current
    val patternIntensity = LocalPatternIntensity.current
    val isDark = LocalThemeIsDark.current

    var activePrayer by remember { mutableStateOf<DailyPrayer?>(null) }
    var isFemalePosture by remember { mutableStateOf(false) }
    var stepTick by remember { mutableIntStateOf(0) }

    // TTS Support for listening to recitations and step guidance
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var speakingId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale("tr", "TR")
            }
        }
        tts = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    fun stepIndexFor(prayer: DailyPrayer): Int = prefs.getInt("step_${prayer.name}", 0)
    fun saveStepIndex(prayer: DailyPrayer, index: Int) {
        prefs.edit().putInt("step_${prayer.name}", index).commit()
        stepTick++
    }

    BackHandler {
        if (activePrayer != null) {
            tts?.stop()
            speakingId = null
            activePrayer = null
        } else {
            onBack()
        }
    }

    val bottomReserve = navBarBottomPadding(16.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawCustomBackground(selectedBgId, theme, isDark, patternIntensity)
            .statusBarsPadding()
    ) {
        val currentPrayer = activePrayer
        if (currentPrayer == null) {
            PrayerLearningCatalog(
                stepTick = stepTick,
                stepIndexFor = ::stepIndexFor,
                isFemalePosture = isFemalePosture,
                onToggleGender = { isFemalePosture = !isFemalePosture },
                onSelectPrayer = { activePrayer = it },
                onBack = onBack,
                bottomPadding = bottomReserve
            )
        } else {
            val steps = remember(currentPrayer) { PrayerLessons.steps(currentPrayer) }
            val currentIdx = remember(currentPrayer, stepTick) { stepIndexFor(currentPrayer) }

            PrayerLessonInteractiveSession(
                prayer = currentPrayer,
                steps = steps,
                stepIndex = currentIdx,
                isFemalePosture = isFemalePosture,
                onToggleGender = { isFemalePosture = !isFemalePosture },
                speakingId = speakingId,
                onSpeakText = { id, text ->
                    val engine = tts ?: return@PrayerLessonInteractiveSession
                    if (speakingId == id) {
                        engine.stop()
                        speakingId = null
                    } else {
                        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
                        speakingId = id
                    }
                },
                onUpdateIndex = { nextIdx ->
                    tts?.stop()
                    speakingId = null
                    saveStepIndex(currentPrayer, nextIdx)
                },
                onReturnCatalog = {
                    tts?.stop()
                    speakingId = null
                    activePrayer = null
                },
                bottomPadding = bottomReserve
            )
        }
    }
}

/* ============================================================================
   1. CATALOG VIEW: VISUAL POSTURE ATLAS + 5 DAILY PRAYERS + VISUAL ABDEST
   ========================================================================== */

@Composable
private fun PrayerLearningCatalog(
    stepTick: Int,
    stepIndexFor: (DailyPrayer) -> Int,
    isFemalePosture: Boolean,
    onToggleGender: () -> Unit,
    onSelectPrayer: (DailyPrayer) -> Unit,
    onBack: () -> Unit,
    bottomPadding: Dp
) {
    var previewPosture by remember { mutableStateOf(PrayerPosture.STANDING) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("prayer-learning-catalog"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column {
                        Text(
                            text = "Namaz Öğreniyorum",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = PrayerLessons.SCOPE,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Erkek / Kadın Görsel Duruş Seçici
                Surface(
                    onClick = onToggleGender,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("catalog-gender-toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isFemalePosture) Icons.Default.Woman else Icons.Default.Man,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isFemalePosture) "Kadın Duruşu" else "Erkek Duruşu",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Section 1: 5 Vakit Farz Namaz Dersleri (Placed at the top so tests & users immediately reach them!)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Adım Adım Vakit Namazları",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Derse başlamak için dokunun",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        items(DailyPrayer.entries, key = { it.name }) { prayer ->
            val totalSteps = remember(prayer) { PrayerLessons.steps(prayer).size }
            val savedIdx = remember(prayer, stepTick) { stepIndexFor(prayer) }
            val isCompleted = savedIdx >= totalSteps
            val isInProgress = savedIdx in 1 until totalSteps
            val progressFraction = (savedIdx.toFloat() / totalSteps.coerceAtLeast(1)).coerceIn(0f, 1f)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (isCompleted) 1.6.dp else 1.dp,
                    color = if (isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectPrayer(prayer) }
                    .testTag("learn-${prayer.name}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Visual Rakat Counter Badge
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${prayer.rakats}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCompleted) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Rekât",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${prayer.title} Namazı",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isCompleted) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "✓ Tamamlandı",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (isInProgress) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "Adım ${savedIdx + 1}/$totalSteps",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${prayer.rakats} rekât farz • $totalSteps görsel adım ve okunuş",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Visual Rakat Step Pills + Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (r in 1..prayer.rakats) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$r. Rekât",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (savedIdx > 0) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }

        // Section 2: Interactive Visual Posture Atlas Card
        item(key = "posture-atlas") {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth().testTag("posture-atlas")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Görsel Namaz Duruşları Şeması",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Her rükündaki el, ayak, sırt açısı ve bakış noktasını incelemek için bir duruş seçin:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val postureChoices = remember {
                        listOf(
                            PrayerPosture.TAKBIR to "Tekbir",
                            PrayerPosture.STANDING to "Kıyam",
                            PrayerPosture.BOWING to "Rükû",
                            PrayerPosture.UPRIGHT to "Kavme",
                            PrayerPosture.PROSTRATION to "Secde",
                            PrayerPosture.SITTING to "Oturuş",
                            PrayerPosture.GREETING to "Selâm"
                        )
                    }

                    LazyRow(
                        modifier = Modifier.testTag("posture-atlas-tabs"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(postureChoices) { (posture, label) ->
                            val selected = previewPosture == posture
                            FilterChip(
                                modifier = Modifier.testTag("atlas-pose-${posture.name}"),
                                selected = selected,
                                onClick = { previewPosture = posture },
                                label = {
                                    Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                }
                            )
                        }
                    }

                    VisualPostureCard(
                        posture = previewPosture,
                        isFemale = isFemalePosture
                    )
                }
            }
        }

        // Section 3: Visual Abdest & Namaza Hazırlık Rehberi
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Görsellerle Abdest Alınışı",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sırasıyla 7 adımda abdestin sünnet ve farzları",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    PrayerLessons.ablution.forEachIndexed { index, text ->
                        val isFarzStep = index in listOf(2, 3, 4, 6)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                1.dp,
                                if (isFarzStep) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AbdestVisualBadge(stepIndex = index, isFarz = isFarzStep)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}. Adım",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isFarzStep) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = if (isFarzStep) "FARZ" else "SÜNNET",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isFarzStep) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Namaza Hazırlık Şartları (Hadesten Taharet, Necasetten Taharet, Setr-i Avret, İstikbal-i Kıble, Vakit, Niyet)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Namaza Hazırlık Şartları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PrayerLessons.preparation.forEach { (title, desc) ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp)
                            )
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        text = PrayerLessons.NOTE,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/* ============================================================================
   2. INTERACTIVE STEP-BY-STEP PRAYER LESSON SESSION
   ========================================================================== */

@Composable
private fun PrayerLessonInteractiveSession(
    prayer: DailyPrayer,
    steps: List<PrayerLessonStep>,
    stepIndex: Int,
    isFemalePosture: Boolean,
    onToggleGender: () -> Unit,
    speakingId: String?,
    onSpeakText: (String, String) -> Unit,
    onUpdateIndex: (Int) -> Unit,
    onReturnCatalog: () -> Unit,
    bottomPadding: Dp
) {
    val isFinished = stepIndex >= steps.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = bottomPadding)
    ) {
        // Top Lesson Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onReturnCatalog) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Namazlara dön"
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${prayer.title} Namazı (${prayer.rakats} Rekât Farz)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isFinished) "Tüm adımlar tamamlandı"
                    else "Adım ${stepIndex + 1} / ${steps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Erkek / Kadın Duruş Toggle
                IconButton(
                    onClick = onToggleGender,
                    modifier = Modifier.testTag("lesson-gender-toggle")
                ) {
                    Icon(
                        imageVector = if (isFemalePosture) Icons.Default.Woman else Icons.Default.Man,
                        contentDescription = if (isFemalePosture) "Kadın Duruşu" else "Erkek Duruşu",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onUpdateIndex(0) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Dersi baştan başlat"
                    )
                }
            }
        }

        if (isFinished) {
            // Completion Screen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        Text(
                            text = "Ders tamamlandı",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${prayer.title} namazının ${prayer.rakats} rekâtlık farz kılınışını baştan sona görsel şemalar ve dualarla tamamladınız. Allah kabul etsin.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onUpdateIndex(0) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Tekrar Et")
                            }

                            Button(
                                onClick = onReturnCatalog,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("lesson-next"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Derslere Dön")
                            }
                        }
                    }
                }
            }
        } else {
            val safeIdx = stepIndex.coerceIn(0, steps.lastIndex)
            val currentStep = steps[safeIdx]
            val progress = (safeIdx + 1).toFloat() / steps.size.toFloat()

            // Visual Rakat & Step Timeline Strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                itemsIndexed(steps) { idx, item ->
                    val isCurrent = idx == safeIdx
                    val isDone = idx < safeIdx
                    Surface(
                        onClick = { onUpdateIndex(idx) },
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isDone -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = "${idx + 1}. ${item.title}",
                            fontSize = 11.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.onPrimary
                                isDone -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(5.dp)
                    .clip(CircleShape)
            )

            // Main Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "step_content"
            ) { step ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    // 1. Step Header + Visual Posture Illustration Card
                    item {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = if (step.rakat > 0) "${step.rakat}. Rekât" else "Başlangıç",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        onClick = onToggleGender,
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                    ) {
                                        Text(
                                            text = if (isFemalePosture) "Kadın Duruşu (Değiştir)" else "Erkek Duruşu (Değiştir)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Step Title (Tested by PrayerLearningScreenshotTest!)
                                Text(
                                    text = step.title,
                                    modifier = Modifier.testTag("lesson-step-title"),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Visual Posture Diagram
                                VisualPostureCard(
                                    posture = step.posture,
                                    isFemale = isFemalePosture
                                )

                                // Step Instruction
                                Text(
                                    text = step.instruction,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 23.sp
                                )
                            }
                        }
                    }

                    // 2. Recitation Cards (Arabic + Reading + TTS Audio Listen)
                    val stepRecitations: List<PrayerRecitation> = step.recitations.mapNotNull { PrayerLessons.recitations[it] }
                    items(stepRecitations, key = { it.id }) { rec ->
                        val isPlayingThis = speakingId == rec.id
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rec.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    FilledTonalButton(
                                        onClick = { onSpeakText(rec.id, "${rec.title}. ${rec.reading}") },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingThis) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Sesli Dinle",
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (isPlayingThis) "Durdur" else "Dinle",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Arabic script
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = rec.arabic,
                                        fontSize = 20.sp,
                                        lineHeight = 32.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.End,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    )
                                }

                                // Turkish Pronunciation
                                Text(
                                    text = rec.reading,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Bottom Navigation Row ("Önceki" / "Sonraki" with testTag("lesson-next"))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { if (safeIdx > 0) onUpdateIndex(safeIdx - 1) },
                    enabled = safeIdx > 0,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("lesson-prev"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Önceki")
                }

                Button(
                    onClick = { onUpdateIndex(safeIdx + 1) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("lesson-next"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (safeIdx == steps.lastIndex) "Dersi Bitir" else "Sonraki")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

/* ============================================================================
   3. ILLUSTRATED POSTURE CARD & ACCESSIBLE ZOOM VIEW
   ========================================================================== */

@Composable
private fun VisualPostureCard(
    posture: PrayerPosture,
    isFemale: Boolean
) {
    val meta = remember(posture) { posture.visualMeta() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    var showZoomDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.30f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The same packaged artwork is used in lessons, the atlas, and the enlarged view.
            Box(
                modifier = Modifier
                    .width(164.dp)
                    .height(192.dp)
                    .testTag("posture-image-frame")
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF6F2E8))
                    .border(1.dp, primaryColor.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                    .clickable(onClickLabel = "Görseli büyüt") { showZoomDialog = true },
                contentAlignment = Alignment.Center
            ) {
                PrayerPostureIllustration(
                    posture = posture,
                    isFemale = isFemale,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color(0xFFF6F2E8).copy(alpha = 0.94f),
                    shape = RoundedCornerShape(bottomStart = 10.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "← Kıble",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF24554A),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    color = Color(0xFFF6F2E8).copy(alpha = 0.94f),
                    shape = RoundedCornerShape(topEnd = 10.dp),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "Büyüt",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF24554A),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            // Right: Anatomical & Fiqh Guidance Checkpoints
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = primaryColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = if (isFemale) "Hanım Cemaat" else "Erkek Cemaat",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = meta.postureTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryColor
                )
                Text(
                    text = if (isFemale) meta.femaleCheckpoint else meta.maleCheckpoint,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurface,
                    lineHeight = 18.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaryColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "👁️ ${meta.gazeTarget}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = secondaryColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "✦ ${meta.keyPointBadge}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }

    if (showZoomDialog) {
        AlertDialog(
            onDismissRequest = { showZoomDialog = false },
            title = {
                Text(
                    text = "${meta.postureTitle} (${if (isFemale) "Kadın" else "Erkek"})",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp)
                            .testTag("posture-zoom")
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFF6F2E8))
                            .border(1.dp, primaryColor.copy(alpha = 0.18f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        PrayerPostureIllustration(
                            posture = posture,
                            isFemale = isFemale,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = if (isFemale) meta.femaleCheckpoint else meta.maleCheckpoint,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "👁️ Bakış Noktası: ${meta.gazeTarget}\n✦ Ölçü: ${meta.keyPointBadge}",
                        style = MaterialTheme.typography.labelMedium,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showZoomDialog = false }) {
                    Text("Kapat", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/** Each bundled illustration pairs the male pose on the left with the female pose on the right. */
@DrawableRes
private fun PrayerPosture.illustrationResource(): Int = when (this) {
    PrayerPosture.INTENTION, PrayerPosture.UPRIGHT -> R.drawable.prayer_pose_upright
    PrayerPosture.TAKBIR -> R.drawable.prayer_pose_takbir
    PrayerPosture.STANDING -> R.drawable.prayer_pose_standing
    PrayerPosture.BOWING -> R.drawable.prayer_pose_bowing
    PrayerPosture.PROSTRATION -> R.drawable.prayer_pose_prostration
    PrayerPosture.SITTING -> R.drawable.prayer_pose_sitting
    PrayerPosture.GREETING -> R.drawable.prayer_pose_greeting
}

@Composable
private fun PrayerPostureIllustration(
    posture: PrayerPosture,
    isFemale: Boolean,
    modifier: Modifier = Modifier
) {
    val artwork = ImageBitmap.imageResource(posture.illustrationResource())
    val meta = remember(posture) { posture.visualMeta() }
    val description = "${if (isFemale) "Kadın" else "Erkek"} duruşu: ${meta.postureTitle}. " +
        if (isFemale) meta.femaleCheckpoint else meta.maleCheckpoint

    Canvas(
        modifier = modifier
            .testTag("posture-artwork-${posture.name}-${if (isFemale) "female" else "male"}")
            .semantics { contentDescription = description }
    ) {
        val sourceWidth = artwork.width / 2
        val sourceSize = IntSize(sourceWidth, artwork.height)
        val scale = minOf(size.width / sourceWidth, size.height / artwork.height)
        val destinationSize = IntSize(
            (sourceWidth * scale).roundToInt(),
            (artwork.height * scale).roundToInt()
        )
        drawImage(
            image = artwork,
            srcOffset = IntOffset(if (isFemale) sourceWidth else 0, 0),
            srcSize = sourceSize,
            dstOffset = IntOffset(
                ((size.width - destinationSize.width) / 2f).roundToInt(),
                ((size.height - destinationSize.height) / 2f).roundToInt()
            ),
            dstSize = destinationSize,
            filterQuality = FilterQuality.High
        )
    }
}

@Composable
private fun AbdestVisualBadge(stepIndex: Int, isFarz: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val waterBlue = Color(0xFF0284C7)
    val waterLight = Color(0xFF38BDF8)
    val skinLight = Color(0xFFF7D6BF)
    val skinMid = Color(0xFFE7B898)
    val skinOutline = Color(0xFFB47B56)
    val gold = Color(0xFFF59E0B)

    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    if (isFarz) {
                        listOf(Color(0xFF0D2E26), Color(0xFF16483C))
                    } else {
                        listOf(Color(0xFF0F293A), Color(0xFF163E56))
                    }
                )
            )
            .border(
                1.3.dp,
                if (isFarz) gold.copy(alpha = 0.65f) else waterLight.copy(alpha = 0.50f),
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(7.dp)) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f
            val skinBrush = Brush.linearGradient(listOf(skinLight, skinMid))

            // Helper: Draw a sculpted water droplet
            fun drawDroplet(x: Float, y: Float, r: Float) {
                val drop = Path().apply {
                    moveTo(x, y - r * 1.4f)
                    quadraticTo(x + r, y, x, y + r)
                    quadraticTo(x - r, y, x, y - r * 1.4f)
                    close()
                }
                drawPath(drop, waterLight)
            }

            drawDroplet(w * 0.84f, h * 0.18f, 2.6.dp.toPx())
            drawDroplet(w * 0.70f, h * 0.11f, 2.0.dp.toPx())

            when (stepIndex) {
                0 -> {
                    // Step 1: Washing Both Hands up to Wrists (Sculpted Palms & Fingers)
                    for (side in listOf(-1f, 1f)) {
                        val hx = cx + side * w * 0.21f
                        val hy = cy + h * 0.08f
                        drawRoundRect(
                            brush = skinBrush,
                            topLeft = Offset(hx - w * 0.14f, hy - h * 0.16f),
                            size = Size(w * 0.28f, h * 0.38f),
                            cornerRadius = CornerRadius(5.dp.toPx())
                        )
                        // 4 sculpted fingers on each hand
                        for (f in -1..2) {
                            val fx = hx + f * w * 0.055f - w * 0.025f
                            drawLine(
                                color = skinLight,
                                start = Offset(fx, hy - h * 0.14f),
                                end = Offset(fx, hy - h * 0.34f),
                                strokeWidth = 2.4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    // Flowing water wave across wrists
                    val wave = Path().apply {
                        moveTo(w * 0.10f, h * 0.84f)
                        quadraticTo(w * 0.35f, h * 0.74f, cx, h * 0.84f)
                        quadraticTo(w * 0.65f, h * 0.94f, w * 0.90f, h * 0.84f)
                    }
                    drawPath(wave, waterLight, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
                }
                1 -> {
                    // Step 2: Rinsing Mouth & Nose (Cupped Hand Lifting Water Stream)
                    val cuppedHand = Path().apply {
                        moveTo(w * 0.16f, h * 0.62f)
                        cubicTo(w * 0.25f, h * 0.84f, w * 0.75f, h * 0.84f, w * 0.84f, h * 0.60f)
                        quadraticTo(w * 0.62f, h * 0.68f, w * 0.16f, h * 0.62f)
                        close()
                    }
                    drawPath(cuppedHand, brush = skinBrush)
                    drawPath(cuppedHand, skinOutline, style = Stroke(width = 1.dp.toPx()))
                    // Pooled water in palm + upward rinsing arc
                    drawOval(waterLight, Offset(w * 0.30f, h * 0.54f), Size(w * 0.40f, h * 0.14f))
                    val stream = Path().apply {
                        moveTo(cx, h * 0.54f)
                        quadraticTo(cx + w * 0.14f, h * 0.32f, cx, h * 0.18f)
                    }
                    drawPath(stream, waterLight, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
                }
                2 -> {
                    // Step 3: Washing Face (FARZ) - Serene Sculpted Face Contour + Water Halo
                    drawOval(
                        brush = skinBrush,
                        topLeft = Offset(w * 0.22f, h * 0.12f),
                        size = Size(w * 0.56f, h * 0.74f)
                    )
                    drawOval(
                        color = gold,
                        topLeft = Offset(w * 0.22f, h * 0.12f),
                        size = Size(w * 0.56f, h * 0.74f),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                    // Gentle water waves framing cheeks
                    drawArc(
                        color = waterLight,
                        startAngle = 110f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(w * 0.12f, h * 0.16f),
                        size = Size(w * 0.76f, h * 0.68f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = waterLight,
                        startAngle = -70f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(w * 0.12f, h * 0.16f),
                        size = Size(w * 0.76f, h * 0.68f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                3 -> {
                    // Step 4: Washing Forearms Including Elbows (FARZ)
                    val armPath = Path().apply {
                        moveTo(w * 0.14f, h * 0.56f)
                        lineTo(w * 0.48f, h * 0.76f)
                        lineTo(w * 0.82f, h * 0.22f)
                    }
                    drawPath(armPath, skinMid, style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(armPath, skinLight, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    // Highlighted Elbow Joint Ring (Dirsek Dahil)
                    drawCircle(gold.copy(alpha = 0.35f), 7.5.dp.toPx(), Offset(w * 0.48f, h * 0.76f))
                    drawCircle(gold, 3.8.dp.toPx(), Offset(w * 0.48f, h * 0.76f))
                }
                4 -> {
                    // Step 5: Wiping (Mesh) 1/4 of the Head (FARZ)
                    drawOval(
                        brush = skinBrush,
                        topLeft = Offset(w * 0.22f, h * 0.22f),
                        size = Size(w * 0.56f, h * 0.66f)
                    )
                    val meshCrown = Path().apply {
                        moveTo(w * 0.18f, h * 0.44f)
                        quadraticTo(cx, h * 0.04f, w * 0.82f, h * 0.44f)
                    }
                    drawPath(meshCrown, waterLight, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(meshCrown, gold, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
                }
                5 -> {
                    // Step 6: Wiping Ears & Neck (SÜNNET)
                    val earPath = Path().apply {
                        moveTo(cx - w * 0.06f, h * 0.22f)
                        cubicTo(cx + w * 0.28f, h * 0.10f, cx + w * 0.30f, h * 0.62f, cx + w * 0.04f, h * 0.78f)
                        quadraticTo(cx - w * 0.10f, h * 0.82f, cx - w * 0.08f, h * 0.64f)
                    }
                    drawPath(earPath, skinLight, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(earPath, waterLight, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                }
                else -> {
                    // Step 7: Washing Feet Including Ankles (FARZ)
                    val footPath = Path().apply {
                        moveTo(w * 0.26f, h * 0.16f)
                        lineTo(w * 0.44f, h * 0.16f)
                        lineTo(w * 0.46f, h * 0.58f)
                        cubicTo(w * 0.60f, h * 0.62f, w * 0.84f, h * 0.66f, w * 0.86f, h * 0.76f)
                        quadraticTo(w * 0.86f, h * 0.86f, w * 0.70f, h * 0.86f)
                        lineTo(w * 0.24f, h * 0.86f)
                        quadraticTo(w * 0.18f, h * 0.86f, w * 0.20f, h * 0.72f)
                        close()
                    }
                    drawPath(footPath, brush = skinBrush)
                    drawPath(footPath, gold, style = Stroke(width = 1.4.dp.toPx(), join = StrokeJoin.Round))
                    // Highlighted Ankle Bone (Topuk / Aşık Kemiği)
                    drawCircle(gold.copy(alpha = 0.35f), 7.dp.toPx(), Offset(w * 0.35f, h * 0.66f))
                    drawCircle(gold, 3.5.dp.toPx(), Offset(w * 0.35f, h * 0.66f))
                }
            }
        }
    }
}


