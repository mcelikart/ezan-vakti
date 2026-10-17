package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

enum class AssistantMode(
    val title: String,
    val subtitle: String,
    val icon: String,
    val systemPrompt: String,
    val welcomeMessage: String
) {
    GENERAL(
        title = "Dini Rehber",
        subtitle = "Kur'an ve Sünnet Işığında Q&A",
        icon = "🕌",
        systemPrompt = "Sen İslami konularda rehberlik eden, son derece saygılı, güvenilir ve hikmetli bir uzmansın. Ehli Sünnet vel Cemaat inancına uygun, Kur'an-ı Kerim ayetleri ve sahih hadisler ışığında net cevaplar verirsin. Ayet ve hadis aktarırken mutlaka sure/ayet numarası ve hadis kaynağını (Buhari, Müslim vb.) açıkça belirtirsin. Tartışmalı konularda farklı mezhep görüşlerini nezaketle aktarır, ihtilafa düşürmeden yapıcı bir dil kullanırsın. Dini olmayan konularda saygıyla 'Ben bir İslami rehber asistanıyım, bu konuda size yardımcı olamam' dersin. Uzun yanıtları paragraflara ve maddelere bölerek okunması kolay hale getirirsin.",
        welcomeMessage = "Selamun Aleyküm. Dini sorularınızı Kur'an-ı Kerim ve sahih hadisler ışığında yanıtlamak için hizmetinizdeyim. Aklınıza takılan herhangi bir konuyu sorabilirsiniz."
    ),
    DREAM(
        title = "Rüya Tabiri",
        subtitle = "İslam Alimlerinin Usulü",
        icon = "🌙",
        systemPrompt = "Sen İslami rüya tabiri konusunda uzmanlaşmış bir rehbersin. İbn Sirin, Nablusi, İmam Cafer-i Sadık ve Seyyid Süleyman gibi muteber İslam alimlerinin rüya tabiri esaslarına dayanarak değerlendirme yaparsın.\nYanıt düzenin:\n1. Rüyadaki temel sembollerin İslami anlamları.\n2. Rüyayı hayra yoracak yapıcı ve ferahlatıcı değerlendirme.\n3. Rüyaların gayb bilgisi olmadığını, nihai takdirin Allah'a ait olduğunu ve hayırlı dua ile sadaka tavsiyesini belirten mesaj.",
        welcomeMessage = "Gördüğünüz rüyayı detaylarıyla paylaşın; İbn Sirin ve Nablusi gibi muteber İslami kaynaklar ışığında hayra yorarak tabir edelim. (Rüyaların gaybı bildirmediğini, hayra yormanızın esas olduğunu unutmayın.)"
    ),
    TAFSIR(
        title = "Ayet & Hadis",
        subtitle = "Derinlemesine İnceleme",
        icon = "📖",
        systemPrompt = "Sen Kur'an ayetleri ve hadis-i şerifler üzerine derinlemesine tefsir ve analiz yapan bir uzmansın. Bir ayet veya hadis sorulduğunda:\n1. Arapça metni ve Türkçe mealini sun.\n2. Nüzul/vurud sebebi ve tarihsel bağlamını anlat.\n3. Muteber tefsirlerden (Elmalılı Hamdi Yazır, İbn Kesir, Diyanet) özet hikmetler aktar.\n4. Günlük hayatımıza çıkarılabilecek ameli ve ahlaki dersleri maddeler halinde sırala.",
        welcomeMessage = "İncelemek istediğiniz ayet-i kerimeyi veya hadis-i şerifi yazın; kelime anlamları, nüzul sebebi ve hikmetleriyle detaylıca ele alalım."
    ),
    GROWTH(
        title = "Manevi Program",
        subtitle = "Kişisel İbadet & Zikir Planı",
        icon = "🌱",
        systemPrompt = "Sen kişiye özel manevi gelişim ve ibadet rehberisin. Kullanıcının ruh hali, zaman imkanı veya hedeflerine uygun günlük/haftalık zikir, Kur'an okuma, nafile namaz ve ahlaki gelişim programı oluşturursun. Yanıtlarını son derece motive edici, uygulanabilir ve düzenli maddeler halinde sunarsın.",
        welcomeMessage = "Manevi hayatınızı düzenlemek, zikir veya okuma alışkanlığı kazanmak için hedeflerinizi belirtin; size özel dengeli ve sürdürülebilir bir ibadet programı hazırlayayım."
    )
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val mode: AssistantMode = AssistantMode.GENERAL
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var selectedMode by remember { mutableStateOf(AssistantMode.GENERAL) }
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = AssistantMode.GENERAL.welcomeMessage,
                    isUser = false,
                    mode = AssistantMode.GENERAL
                )
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isLiveVoiceMode by remember { mutableStateOf(false) }
    var showKeyDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var currentApiKey by remember { mutableStateOf(GeminiApiClient.getApiKey(context)) }

    // Audio & Speech States
    var isHandsFreeMode by remember { mutableStateOf(false) }
    var selectedVoicePersona by remember { mutableStateOf("bilge") } // "bilge", "sakin", "rahime"
    var voiceAmplitude by remember { mutableStateOf(0f) }
    var currentlySpeakingMsgId by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Speech & Text-To-Speech Instances
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var wasLastInputVoice by remember { mutableStateOf(false) }

    fun applyVoicePersona(ttsInstance: TextToSpeech) {
        when (selectedVoicePersona) {
            "bilge" -> {
                ttsInstance.setPitch(0.82f)
                ttsInstance.setSpeechRate(0.88f)
            }
            "sakin" -> {
                ttsInstance.setPitch(1.0f)
                ttsInstance.setSpeechRate(1.0f)
            }
            "rahime" -> {
                ttsInstance.setPitch(1.18f)
                ttsInstance.setSpeechRate(1.05f)
            }
        }
    }

    fun stopTts() {
        tts?.stop()
        currentlySpeakingMsgId = null
    }

    fun speakText(msgId: String, textToSpeak: String) {
        if (!isTtsReady || tts == null) {
            Toast.makeText(context, "Ses motoru hazır değil.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentlySpeakingMsgId == msgId) {
            stopTts()
            return
        }

        stopTts()
        tts?.let { ttsInstance ->
            applyVoicePersona(ttsInstance)
            val cleanText = textToSpeak.replace(Regex("[#*_~]"), "").take(1000)
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, msgId)
            ttsInstance.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, msgId)
            currentlySpeakingMsgId = msgId
        }
    }

    fun sendMessage(text: String, isVoice: Boolean = wasLastInputVoice) {
        if (text.isBlank()) return

        stopTts()

        val userMsg = ChatMessage(text = text, isUser = true, mode = selectedMode)
        val loadingMsg = ChatMessage(text = "", isUser = false, isLoading = true, mode = selectedMode)
        messages = messages + listOf(userMsg, loadingMsg)
        inputText = ""
        focusManager.clearFocus()

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            try {
                val historyParts = messages
                    .filter { !it.isLoading && !it.isError }
                    .takeLast(8)
                    .joinToString("\n") {
                        if (it.isUser) "Kullanıcı: ${it.text}" else "Asistan: ${it.text}"
                    }

                val prompt = "$historyParts\nKullanıcı: $text\nAsistan:"

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = selectedMode.systemPrompt))),
                    generationConfig = GenerationConfig(temperature = 0.7f)
                )

                var responseText: String? = null
                try {
                    val key = GeminiApiClient.getApiKey(context)
                    if (GeminiApiClient.isApiKeyValid(key)) {
                        val response = withContext(Dispatchers.IO) {
                            GeminiApiClient.service.generateContent(key, request)
                        }
                        responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (responseText.isNull_or_blank_or_error()) {
                    delay(400) // Realistic typing feel
                    responseText = generateIslamicKnowledgeFallbackResponse(text, selectedMode)
                }

                val newAiMsg = ChatMessage(text = responseText!!, isUser = false, mode = selectedMode)
                messages = messages.dropLast(1) + newAiMsg

                // Auto read aloud if queried by voice or in live voice mode
                if (isTtsReady && (isVoice || isLiveVoiceMode)) {
                    speakText(newAiMsg.id, responseText!!)
                }
                wasLastInputVoice = false

            } catch (e: Exception) {
                val fallbackText = generateIslamicKnowledgeFallbackResponse(text, selectedMode)
                messages = messages.dropLast(1) + ChatMessage(text = fallbackText, isUser = false, mode = selectedMode)
                wasLastInputVoice = false
            }
            coroutineScope.launch {
                delay(100)
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    fun startListening() {
        if (recordAudioPermissionState.status.isGranted) {
            stopTts()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            }
            speechRecognizer?.startListening(intent)
            isListening = true
        } else {
            recordAudioPermissionState.launchPermissionRequest()
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    // Initialize TTS
    DisposableEffect(context) {
        var textToSpeechInstance: TextToSpeech? = null
        textToSpeechInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeechInstance?.language = Locale.forLanguageTag("tr-TR")
                isTtsReady = true
            }
        }
        textToSpeechInstance.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                currentlySpeakingMsgId = utteranceId
            }
            override fun onDone(utteranceId: String?) {
                currentlySpeakingMsgId = null
                if (isLiveVoiceMode && isHandsFreeMode) {
                    coroutineScope.launch {
                        delay(400)
                        startListening()
                    }
                }
            }
            override fun onError(utteranceId: String?) {
                currentlySpeakingMsgId = null
            }
        })
        tts = textToSpeechInstance
        onDispose {
            textToSpeechInstance.stop()
            textToSpeechInstance.shutdown()
        }
    }

    // Initialize Speech Recognizer
    DisposableEffect(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f).coerceIn(0f, 12f) / 12f)
                        voiceAmplitude = normalized
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { isListening = false }
                    override fun onError(error: Int) { isListening = false }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedText = matches[0]
                            wasLastInputVoice = true
                            sendMessage(recognizedText)
                        }
                        isListening = false
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    // Handle Mode Change - Insert welcome message if switching mode
    fun changeMode(newMode: AssistantMode) {
        if (selectedMode == newMode) return
        selectedMode = newMode
        stopTts()
        val welcomeMsg = ChatMessage(text = newMode.welcomeMessage, isUser = false, mode = newMode)
        messages = messages + welcomeMsg
        coroutineScope.launch {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (isLiveVoiceMode) {
        LiveVoiceAssistantScreen(
            isListening = isListening,
            isSpeaking = currentlySpeakingMsgId != null,
            voiceAmplitude = voiceAmplitude,
            isHandsFreeMode = isHandsFreeMode,
            selectedVoicePersona = selectedVoicePersona,
            onHandsFreeToggle = { isHandsFreeMode = !isHandsFreeMode },
            onPersonaChange = { selectedVoicePersona = it },
            messages = messages,
            onClose = {
                isLiveVoiceMode = false
                stopTts()
                stopListening()
            },
            onMicClick = {
                if (isListening || currentlySpeakingMsgId != null) {
                    stopTts()
                    stopListening()
                } else {
                    startListening()
                }
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = selectedMode.icon, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Rehber Asistan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Gemini 3.5",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = selectedMode.title + " • " + selectedMode.subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { isLiveVoiceMode = true }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Canlı Sesli Sohbet",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Temizle",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.islamic_pattern_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.03f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp)
            ) {
                // Modern Mode Selector Tabs Bar
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(AssistantMode.values()) { mode ->
                        val isSelected = selectedMode == mode
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable { changeMode(mode) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = mode.icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = mode.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Messages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(
                            message = msg,
                            currentlySpeakingId = currentlySpeakingMsgId,
                            onSpeakClick = { speakText(msg.id, msg.text) }
                        )
                    }
                }

                // Quick Mode Prompt Suggestions (shows relevant presets)
                val suggestions = when (selectedMode) {
                    AssistantMode.GENERAL -> listOf(
                        "Günün ayeti ve meali nedir?",
                        "Kaza namazı nasıl hesaplanır?",
                        "Gıybetin İslam'daki yeri nedir?",
                        "Yolculukta namaz nasıl kılınır?"
                    )
                    AssistantMode.DREAM -> listOf(
                        "Rüyada berrak su görmek ne anlama gelir?",
                        "Rüyada Kabe'yi ve camiyi görmek",
                        "Rüyada uçmak veya yüksekte olmak",
                        "Rüyada yeşil bahçe görmek"
                    )
                    AssistantMode.TAFSIR -> listOf(
                        "Bakara Suresi 153. Ayet tefsiri",
                        "İhlas Suresinin hikmetleri",
                        "Ameller niyetlere göredir hadis tefsiri",
                        "Ayet-el Kürsi'nin sırları"
                    )
                    AssistantMode.GROWTH -> listOf(
                        "Günlük 15 dakikalık zikir ve vird programı",
                        "Sabah ve akşam okunacak dualar",
                        "Haftalık Kur'an okuma takvimi",
                        "Namaz bilincini artırma tavsiyeleri"
                    )
                }

                AnimatedVisibility(
                    visible = messages.size <= 3,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Önerilen Sorular",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(suggestions) { promptText ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    modifier = Modifier.clickable { sendMessage(promptText, false) }
                                ) {
                                    Text(
                                        text = promptText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Input Controls Box
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, top = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic Button
                        val infiniteTransition = rememberInfiniteTransition()
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = if (isListening) 1.25f else 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(46.dp)
                        ) {
                            if (isListening) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (isListening) {
                                        stopListening()
                                    } else {
                                        startListening()
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Sesle Sor",
                                    tint = if (isListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    text = when (selectedMode) {
                                        AssistantMode.GENERAL -> "Dini sorunuzu yazın..."
                                        AssistantMode.DREAM -> "Gördüğünüz rüyayı anlatın..."
                                        AssistantMode.TAFSIR -> "Ayet veya hadis yazın..."
                                        AssistantMode.GROWTH -> "Manevi hedefinizi yazın..."
                                    },
                                    fontSize = 13.sp
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendMessage(inputText, false) })
                        )

                        AnimatedVisibility(
                            visible = inputText.isNotBlank(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            IconButton(
                                onClick = { sendMessage(inputText, false) },
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(42.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Gönder",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Sohbeti Temizle") },
            text = { Text("Tüm mesaj geçmişi silinecektir. Devam etmek istiyor musunuz?") },
            confirmButton = {
                Button(
                    onClick = {
                        stopTts()
                        messages = listOf(
                            ChatMessage(
                                text = selectedMode.welcomeMessage,
                                isUser = false,
                                mode = selectedMode
                            )
                        )
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Temizle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

fun containsArabicText(text: String): Boolean {
    return text.any { it.code in 0x0600..0x06FF || it.code in 0x0750..0x077F || it.code in 0x08A0..0x08FF }
}

@Composable
fun FormattedChatMessage(text: String, textColor: Color, isUser: Boolean) {
    val lines = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { line ->
            if (line.trim().isBlank()) return@forEach

            val isArabic = containsArabicText(line)
            if (isArabic) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = line.trim(),
                        color = if (isUser) textColor else Color(0xFF00695C),
                        fontSize = 22.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else {
                val trimmed = line.trim()
                if (trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*")) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = trimmed.removePrefix("•").removePrefix("-").removePrefix("*").trim(),
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = trimmed,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = if (containsArabicText(text)) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    currentlySpeakingId: String?,
    onSpeakClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (message.isUser) {
        RoundedCornerShape(22.dp, 22.dp, 4.dp, 22.dp)
    } else {
        RoundedCornerShape(22.dp, 22.dp, 22.dp, 4.dp)
    }

    val isSpeakingThis = currentlySpeakingId == message.id

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        contentAlignment = alignment
    ) {
        if (message.isLoading) {
            Surface(
                shape = shape,
                color = bgColor,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Hikmetle düşünüyor...", color = textColor.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = textColor,
                        strokeWidth = 2.dp
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 330.dp)
            ) {
                var displayedText by remember { mutableStateOf(if (message.isUser || message.isError) message.text else "") }

                LaunchedEffect(message.text) {
                    if (!message.isUser && !message.isError && displayedText.isEmpty()) {
                        message.text.forEachIndexed { index, _ ->
                            displayedText = message.text.substring(0, index + 1)
                            delay(8)
                        }
                    } else {
                        displayedText = message.text
                    }
                }

                Surface(
                    shape = shape,
                    color = if (message.isError) MaterialTheme.colorScheme.errorContainer else bgColor,
                    border = if (!message.isUser && !message.isError) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)) else null,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        FormattedChatMessage(
                            text = displayedText,
                            textColor = if (message.isError) MaterialTheme.colorScheme.onErrorContainer else textColor,
                            isUser = message.isUser
                        )

                        // Action Buttons on AI Messages
                        if (!message.isUser && !message.isError && displayedText.length == message.text.length) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = message.mode.icon + " " + message.mode.title,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor.copy(alpha = 0.5f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Speak Button
                                    IconButton(
                                        onClick = onSpeakClick,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeakingThis) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = "Dinle",
                                            tint = if (isSpeakingThis) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Copy Button
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(message.text))
                                            Toast.makeText(context, "Kopyalandı", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Kopyala",
                                            tint = textColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Share Button
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "${message.text}\n\n— Rehber Asistan'dan paylaşıldı")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Yanıtı Paylaş"))
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Paylaş",
                                            tint = textColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
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
fun LiveVoiceAssistantScreen(
    isListening: Boolean,
    isSpeaking: Boolean,
    voiceAmplitude: Float,
    isHandsFreeMode: Boolean,
    selectedVoicePersona: String,
    onHandsFreeToggle: () -> Unit,
    onPersonaChange: (String) -> Unit,
    messages: List<ChatMessage>,
    onClose: () -> Unit,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()

    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (isListening) 1.35f else if (isSpeaking) 1.18f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val lastAssistantMsg = messages.lastOrNull { !it.isUser && !it.isLoading && !it.isError }?.text
        ?: "Sizi dinliyorum, dini sorunuzu veya rüyanızı aktarabilirsiniz..."

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D12))
    ) {
        Image(
            painter = painterResource(id = R.drawable.islamic_pattern_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.04f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.15f),
                            Color.Transparent,
                            tertiaryColor.copy(alpha = 0.15f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onHandsFreeToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isHandsFreeMode) primaryColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f),
                            CircleShape
                        )
                        .border(1.dp, if (isHandsFreeMode) primaryColor else Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isHandsFreeMode) Icons.Default.Hearing else Icons.Default.Mic,
                        contentDescription = "Eller Serbest",
                        tint = if (isHandsFreeMode) primaryColor else Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CANLI SESLİ SOHBET",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = if (isHandsFreeMode) "Eller Serbest Modu" else "Dokun-Konuş Modu",
                        color = primaryColor.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.06f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Color.White
                    )
                }
            }

            // Persona Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "bilge" to "Bilge Rehber 👴",
                    "sakin" to "Sakin Muallim 🕊️",
                    "rahime" to "Rahime Hanım 👩‍🏫"
                ).forEach { (id, label) ->
                    val isSelected = selectedVoicePersona == id
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) primaryColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) primaryColor else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onPersonaChange(id) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Central Glowing Visualizer Core
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Outer Blur Aura
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(pulseScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    if (isListening) Color(0xFFE53935).copy(alpha = 0.28f)
                                    else if (isSpeaking) primaryColor.copy(alpha = 0.35f)
                                    else primaryColor.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Intermediate Orbit Ring
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(if (isListening) pulseScale * 0.9f else 1.0f)
                        .border(
                            2.dp,
                            Brush.sweepGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.2f),
                                    secondaryColor.copy(alpha = 0.7f),
                                    primaryColor.copy(alpha = 0.2f)
                                )
                            ),
                            CircleShape
                        )
                )

                // Central Interactive Orb
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (isListening) {
                                    listOf(Color(0xFFE53935), Color(0xFFFF8A80))
                                } else {
                                    listOf(primaryColor, secondaryColor)
                                }
                            )
                        )
                        .clickable { onMicClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.AutoAwesome,
                        contentDescription = "Ses",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Multi-Wave Equalizer Canvas
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val midY = height / 2

                    val baseAmplitude = if (isListening) {
                        (voiceAmplitude * 35.dp.toPx()).coerceAtLeast(3.dp.toPx())
                    } else if (isSpeaking) {
                        (18.dp.toPx() * (0.6f + 0.4f * sin(phaseShift * 2.5f)))
                    } else {
                        4.dp.toPx()
                    }

                    for (i in 0..2) {
                        val path = Path()
                        path.moveTo(0f, midY)
                        val waveColor = when (i) {
                            0 -> primaryColor.copy(alpha = 0.85f)
                            1 -> secondaryColor.copy(alpha = 0.55f)
                            else -> tertiaryColor.copy(alpha = 0.35f)
                        }
                        val waveFrequency = (0.012f + i.toFloat() * 0.006f) * (2f * Math.PI.toFloat() / 100f)
                        val speedFactor = if (i == 1) -1.2f else 1.0f

                        for (x in 0..width.toInt() step 6) {
                            val angle = x.toFloat() * waveFrequency + phaseShift * speedFactor + (i.toFloat() * Math.PI.toFloat() / 3f)
                            val y = midY + sin(angle) * (baseAmplitude * (0.4f + 0.6f * cos(angle * 0.4f)))
                            path.lineTo(x.toFloat(), y)
                        }
                        drawPath(
                            path = path,
                            color = waveColor,
                            style = Stroke(
                                width = (3.5f - (i.toFloat() * 0.8f)).dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isListening) "Sizi dinliyorum..." else if (isSpeaking) "Asistan cevap veriyor..." else "Konuşmak için ortadaki orba dokunun",
                    color = if (isListening) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Live Transcript Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                val isAr = containsArabicText(lastAssistantMsg)
                Text(
                    text = lastAssistantMsg,
                    color = Color.White,
                    fontSize = if (isAr) 22.sp else 14.sp,
                    lineHeight = if (isAr) 34.sp else 22.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun GeminiKeyDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    currentKey: String
) {
    var keyText by remember { mutableStateOf(currentKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini API Anahtarı")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Yapay Zeka Rehber Asistanının çalışabilmesi için Gemini API anahtarınızı tanımlayabilirsiniz. Anahtarınız yalnızca cihazınızda güvenli bir şekilde saklanır.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = "API anahtarınızı ai.google.dev adresinden ücretsiz olarak alabilirsiniz.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("API Anahtarı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(keyText)
                    onDismiss()
                }
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

private fun String?.isNull_or_blank_or_error(): Boolean {
    if (this.isNullOrBlank()) return true
    if (this.contains("Maalesef şu an yanıt oluşturulamadı", ignoreCase = true)) return true
    return false
}

private fun generateIslamicKnowledgeFallbackResponse(query: String, mode: AssistantMode): String {
    val q = query.lowercase(Locale.getDefault())
    return when (mode) {
        AssistantMode.DREAM -> {
            val symbol = when {
                q.contains("su") -> "Su görmek; hayat, ilim, bolluk ve helal rızka delalet eder. Berrak su huzura, bulanık su ise dikkat edilmesi gereken sıkıntılara işaret eder."
                q.contains("yılan") -> "Yılan görmek; gizli bir düşmana, nefsin arzularına veya kıskanç bir kimseye işaret eder. Yılanı yenmek sıkıntılardan kurtuluştur."
                q.contains("uçmak") -> "Uçtuğunu görmek; makam, mevki, seyahat veya manevi derecenin yükselmesine delalet eder."
                q.contains("namaz") -> "Rüyada namaz kılmak; duaların kabulüne, borçlardan ve dertlerden kurtulmaya, yüksek ahlaka delalet eder."
                q.contains("kabe") || q.contains("mekke") -> "Kabe'yi veya Mekke'yi görmek; büyük hayır, hidayet, emniyet ve huzura erişmeye delalettir."
                q.contains("ölü") || q.contains("cenaze") -> "Ölmüş birini görmek; o kişiden haber, dua beklentisi veya ömrün uzunluğuna, hayırlı akıbete işarettir."
                else -> "Gördüğünüz rüya, hayatınızdaki manevi arınma, hayırlı kapıların açılması ve içsel huzura kavuşma arzunuzun tecellisidir."
            }
            "🌙 **İslami Rüya Tabiri Değerlendirmesi**\n\n" +
            "1. **Rüyadaki Sembol ve Anlamı:**\n$symbol\n\n" +
            "2. **Hikmet & Değerlendirme:**\nİbn Sirin ve Nablusi hazretlerinin usulüne göre bu tür rüyalar hayra yorulmalı ve şükredilmelidir.\n\n" +
            "3. **Tavsiye:**\nRüyaların nihai takdiri yalnızca Allah'a aittir. Gördüğünüz rüyayı hayra yorup bir miktar sadaka vermeniz ve 'Allah'ım hakkımda hayırlı eyle' diye dua etmeniz sünnettir."
        }
        AssistantMode.TAFSIR -> {
            "📖 **İslami Tefsir ve Ayet/Hadis İncelemesi**\n\n" +
            "Sorduğunuz konuyla ilgili temel ayet-i kerime:\n" +
            "\"İnsan için ancak çalıştığının karşılığı vardır. Şüphesiz onun çalışması yakında görülecektir.\" (Necm Suresi, 39-40. Ayetler)\n\n" +
            "**Hadis-i Şerif:**\n" +
            "\"Ameller ancak niyetlere göredir; herkes için ancak niyet ettiği şey vardır.\" (Buhari, Bed'ü'l-Vahy 1; Müslim, İmare 155)\n\n" +
            "**Hikmet & Ameli Dersler:**\n" +
            "1. İbadet ve amellerimizde samimiyet (ihlas) ve doğru niyet esastır.\n" +
            "2. Her İşte Allah'ın rızası gözetilmeli, meşru yollardan ayrılmamalı ve sabırla gayret gösterilmelidir."
        }
        AssistantMode.GROWTH -> {
            "🌱 **Kişisel Manevi Gelişim ve Zikir Programı**\n\n" +
            "Gününüzü Maneviyatla Bereketlendirecek Program:\n\n" +
            "• **Sabah (Güne Başlarken):** 100 defa 'Subhanallahi ve bihamdihi, Subhanallahil Azim'\n" +
            "• **Öğle Vakti:** 33 defa 'Astağfirullah el-Azim' ve Cevşen / Kur'an tilaveti\n" +
            "• **İkindi & Akşam:** Hasbünallahu ve ni'mel vekil (Allah bize yeter)\n" +
            "• **Yatsı & Gece:** Salavat-ı Şerife, Ayet-el Kürsi ve Tebareke (Mülk) Suresi\n\n" +
            "\"Allah katında amellerin en makbulü, az da olsa devamlı olanıdır.\" (Hadis-i Şerif)"
        }
        AssistantMode.GENERAL -> {
            when {
                q.contains("namaz") -> {
                    "🕌 **Namaz ve İbadet Rehberi**\n\n" +
                    "Namaz, dinin direğidir ve müminin miracıdır. Kur'an-ı Kerim'de buyrulur:\n" +
                    "\"Şüphesiz namaz, müminlere belirli vakitlerde farz kılınmıştır.\" (Nisa Suresi, 103. Ayet)\n\n" +
                    "**Sıkça Sorulan Hususlar:**\n" +
                    "1. Namazın vaktinde kılınması efdaldir. Kaçan namazlar için kaza niyetiyle eda edilmelidir.\n" +
                    "2. Abdestin farzları ve huşu, namazın manevi lezzetini artırır."
                }
                q.contains("oruç") || q.contains("ramazan") || q.contains("kaza") -> {
                    "🌙 **Oruç ve İbadet Rehberi**\n\n" +
                    "\"Ey iman edenler! Allah'a karşı gelmekten sakınasınız diye oruç sizden öncekilere farz kılındığı gibi size de farz kılındı.\" (Bakara Suresi, 183. Ayet)\n\n" +
                    "**Önemli Bilgi:** Oruç sadece mideyi aç bırakmak değil; dili, gözü ve gönlü günahlardan koruma ibadetidir."
                }
                q.contains("dua") || q.contains("zikir") || q.contains("tesbih") -> {
                    "🤲 **Dua ve Zikir Rehberi**\n\n" +
                    "\"Rabbiniz şöyle buyurdu: Bana dua edin, duanızı kabul edeyim.\" (Mü'min Suresi, 60. Ayet)\n\n" +
                    "Duada samimiyet, helal lokma ve ihlas icabetin kapısıdır. Günlük tesbihat ve salavatlar kalbe şifa verir."
                }
                else -> {
                    "🕌 **İslami Rehber Asistanı Yanıtı**\n\n" +
                    "Sorduğunuz konuda İslami esaslar Kur'an-ı Kerim ve Sahih Sünnet ışığında değerlendirilir.\n\n" +
                    "\"Rabbimiz! Bize dünyada da iyilik ver, ahirette de iyilik ver ve bizi ateş azabından koru.\" (Bakara Suresi, 201. Ayet)\n\n" +
                    "Konuyla ilgili daha spesifik detaylar öğrenmek isterseniz sorunuzu biraz daha açabilirsiniz."
                }
            }
        }
    }
}
