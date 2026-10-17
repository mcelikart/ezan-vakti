package com.example.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Turkish meal narration.
 *
 * Why this replaces the previous inline TextToSpeech usage: that version
 * preferred voices that *required* a network connection, spoke an entire verse
 * in one `speak()` call (long text gets truncated by several engines), offered
 * no pause/resume, only implemented the deprecated `onError(utteranceId)`, and
 * kept talking after the app went to the background.
 *
 * Design notes:
 *  - Voices are ranked offline-first. A network voice is a liability: it fails
 *    on a plane, costs latency, and can drop mid-verse.
 *  - Narration is split into sentence chunks and queued with QUEUE_ADD, so a
 *    long verse neither truncates nor needs one giant allocation, and each chunk
 *    reports back so the UI can follow along.
 *  - A single spring of state is exposed as StateFlow so Compose can render
 *    transport state without any ad-hoc booleans.
 *  - Narration pauses when the process loses foreground, because reading a
 *    Quran translation aloud from the background is never what the user meant.
 */
class MealSpeechEngine(context: Context) {

    enum class State { IDLE, PREPARING, PLAYING, PAUSED, FINISHED, UNAVAILABLE, ERROR }

    data class Status(
        val state: State = State.IDLE,
        /** Index of the chunk currently being spoken, for UI highlighting. */
        val chunkIndex: Int = -1,
        val chunkCount: Int = 0,
        val message: String? = null
    )

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    private var engine: TextToSpeech? = null
    private var ready = false
    private var shuttingDown = false

    /** Chunks of the current narration, in order. */
    private var chunks: List<String> = emptyList()
    private var nextChunk = 0
    private var pausedAtChunk = 0
    private var resumeFromPause = false
    private var sessionToken = 0L
    private val speaking = AtomicBoolean(false)

    /** Preferred narration speed; 1.0 is the engine default. */
    var speechRate: Float = 0.92f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            engine?.setSpeechRate(field)
        }

    init {
        start()
    }

    private fun start() {
        _status.value = Status(state = State.PREPARING)
        engine = TextToSpeech(appContext) { status ->
            val tts = engine ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS || shuttingDown) {
                _status.value = Status(state = State.UNAVAILABLE, message = "Türkçe ses motoru bulunamadı.")
                return@TextToSpeech
            }
            configure(tts)
        }
    }

    private fun configure(tts: TextToSpeech) {
        val result = tts.setLanguage(Locale.forLanguageTag("tr-TR"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            _status.value = Status(
                state = State.UNAVAILABLE,
                message = "Cihazda Türkçe metin okuma verisi yok. Ayarlar > Sistem > Metin okuma bölümünden yükleyebilirsiniz."
            )
            return
        }

        selectBestTurkishVoice(tts)

        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        tts.setSpeechRate(speechRate)
        tts.setPitch(basePitch)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                mainHandler.post { handleChunkDone(utteranceId.orEmpty()) }
            }

            @Deprecated("Kept for API < 21 engines; modern engines use onError(String, Int)")
            override fun onError(utteranceId: String?) {
                mainHandler.post { handleChunkError(utteranceId.orEmpty(), errorCode = -1) }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post { handleChunkError(utteranceId.orEmpty(), errorCode) }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                mainHandler.post {
                    if (interrupted) {
                        speaking.set(false)
                    }
                }
            }
        })

        ready = true
        // A pending request made before the engine finished loading.
        if (resumeFromPause) {
            resumeFromPause = false
            speakFrom(pausedAtChunk)
        } else if (_status.value.state == State.PREPARING) {
            _status.value = Status(state = State.IDLE)
        }
    }

    /**
     * Offline voices first, then quality, then latency.
     *
     * The old ordering preferred `isNetworkConnectionRequired` voices, which is
     * backwards: those are exactly the ones that fail without a connection.
     */
    private fun selectBestTurkishVoice(tts: TextToSpeech) {
        val voices = runCatching { tts.voices }.getOrNull().orEmpty()
        val turkish = voices.filter { it.locale.language.equals("tr", ignoreCase = true) }
        val best = turkish.maxWithOrNull(
            compareBy<Voice> { if (it.isNetworkConnectionRequired) 1 else 0 }
                .thenBy { it.quality }
                .thenBy { if (it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true) 1 else 0 }
                .thenBy { it.latency }
        ) ?: return
        runCatching { tts.voice = best }
    }

    /**
     * Speaks [text], splitting it into sentence-sized chunks.
     *
     * @param onFinished invoked on the main thread when the last chunk completes
     */
    fun speak(text: String, onFinished: () -> Unit = {}) {
        val prepared = normalise(text)
        if (prepared.isBlank()) {
            _status.value = Status(state = State.ERROR, message = "Bu ayetin meal metni bulunamadı.")
            return
        }
        if (!ready) {
            // Queue the work: configure() will pick it up once the engine loads.
            chunks = splitIntoChunks(prepared)
            nextChunk = 0
            pausedAtChunk = 0
            resumeFromPause = true
            _status.value = Status(state = State.PREPARING, chunkCount = chunks.size)
            sessionToken++
            onFinished()
            return
        }
        chunks = splitIntoChunks(prepared)
        nextChunk = 0
        pausedAtChunk = 0
        sessionToken++
        speakFrom(0)
    }

    private fun speakFrom(index: Int) {
        val tts = engine ?: return
        if (!ready || index >= chunks.size) {
            finish()
            return
        }
        nextChunk = index
        _status.value = Status(state = State.PLAYING, chunkIndex = index, chunkCount = chunks.size)
        speaking.set(true)
        val id = utteranceId(index)
        val chunk = chunks[index]
        // Per-chunk prosody: engines apply one rate/pitch per utterance, so
        // shaping each chunk is what produces sentence-level intonation instead
        // of one flat monotone delivery across the whole verse.
        val prosody = prosodyFor(chunk, index)
        tts.setSpeechRate(prosody.rate)
        tts.setPitch(prosody.pitch)
        val result = tts.speak(
            chunk,
            TextToSpeech.QUEUE_FLUSH,
            Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            },
            id
        )
        if (result != TextToSpeech.SUCCESS) {
            handleChunkError(id, -2)
        }
    }

    private data class Prosody(val rate: Float, val pitch: Float)

    /**
     * Natural-reading prosody for one chunk.
     *
     * Rules, chosen to match how a person reads a translation aloud rather than
     * how a machine announces it:
     *  - A chunk that closes a sentence falls slightly in rate and pitch, which
     *    is the audible "landing" at the end of a thought.
     *  - A chunk that opens a quotation lifts in pitch, marking a new voice or
     *    a quoted phrase.
     *  - Long chunks ease off a little; short ones carry full speed.
     *  - Punctuation alone does most of the pausing; the gap between utterances
     *    is derived from it in [pauseAfter].
     */
    private fun prosodyFor(chunk: String, index: Int): Prosody {
        val last = chunk.trimEnd().lastOrNull()
        val endsSentence = last != null && last in ".!?…"
        val endsClause = last != null && last in ",;:"
        val isLong = chunk.length > 140

        var rate = speechRate
        var pitch = basePitch
        if (endsSentence) {
            rate *= 0.97f
            pitch *= 0.985f
        } else if (endsClause) {
            rate *= 0.99f
        }
        if (isLong) rate *= 0.98f
        if (chunk.startsWith('“') || chunk.startsWith('"')) pitch *= 1.03f

        return Prosody(
            rate = rate.coerceIn(0.4f, 2.0f),
            pitch = pitch.coerceIn(0.6f, 1.8f)
        )
    }

    /** Warmth baseline: slightly above neutral reads as a person, not a robot. */
    private val basePitch: Float
        get() = 1.04f

    private fun handleChunkDone(utteranceId: String) {
        if (!speaking.get()) return
        val index = chunkIndexOf(utteranceId) ?: return
        if (index + 1 < chunks.size) {
            // The inter-sentence breath is applied here rather than through a
            // TTS silence key: those keys are not part of the public API surface
            // across all engine versions, whereas a real gap between utterances
            // works on every engine and is what a reader actually does.
            val pause = pauseAfter(chunks[index])
            if (pause <= 0L) {
                speakFrom(index + 1)
            } else {
                mainHandler.postDelayed({ speakFrom(index + 1) }, pause)
            }
        } else {
            finish()
        }
    }

    /** Pause length implied by the punctuation a chunk ends on. */
    private fun pauseAfter(chunk: String): Long {
        val last = chunk.trimEnd().lastOrNull() ?: return 0L
        return when (last) {
            '…' -> 620L
            '.', '!', '?' -> 460L
            ',', ';', ':' -> 220L
            else -> 140L
        }
    }

    private fun handleChunkError(utteranceId: String, errorCode: Int) {
        if (!speaking.get()) return
        val index = chunkIndexOf(utteranceId)
        if (index == null) return
        speaking.set(false)
        engine?.stop()
        val message = when (errorCode) {
            TextToSpeech.ERROR_NETWORK, TextToSpeech.ERROR_NETWORK_TIMEOUT ->
                "Seslendirme için internet bağlantısı gerekiyor."
            TextToSpeech.ERROR_NOT_INSTALLED_YET ->
                "Türkçe ses dosyası henüz yüklenmedi. Ayarlar > Sistem > Metin okuma bölümünden yükleyebilirsiniz."
            TextToSpeech.ERROR_SYNTHESIS -> "Türkçe seslendirme sırasında bir hata oluştu."
            TextToSpeech.ERROR_SERVICE -> "Metin okuma servisi yanıt vermiyor."
            TextToSpeech.ERROR_OUTPUT, TextToSpeech.ERROR_INVALID_REQUEST ->
                "Türkçe seslendirme başlatılamadı."
            else -> "Türkçe seslendirme başlatılamadı."
        }
        _status.value = Status(
            state = State.ERROR,
            chunkIndex = index,
            chunkCount = chunks.size,
            message = message
        )
    }

    fun pause() {
        val tts = engine ?: return
        if (!ready || _status.value.state != State.PLAYING) return
        // Cancel the pending gap first, otherwise a queued resume fires after
        // the pause and the narration restarts itself.
        mainHandler.removeCallbacksAndMessages(null)
        tts.stop()
        speaking.set(false)
        pausedAtChunk = nextChunk
        _status.value = _status.value.copy(state = State.PAUSED)
    }

    fun resume() {
        if (_status.value.state != State.PAUSED) return
        if (!ready) {
            resumeFromPause = true
            return
        }
        speakFrom(pausedAtChunk)
    }

    fun stop() {
        mainHandler.removeCallbacksAndMessages(null)
        engine?.stop()
        speaking.set(false)
        chunks = emptyList()
        nextChunk = 0
        pausedAtChunk = 0
        sessionToken++
        _status.value = Status(state = State.IDLE)
    }

    /** Called when the app leaves the foreground. */
    fun onBackgrounded() {
        if (_status.value.state == State.PLAYING) pause()
    }

    private fun finish() {
        speaking.set(false)
        _status.value = Status(state = State.FINISHED, chunkIndex = chunks.lastIndex, chunkCount = chunks.size)
        chunks = emptyList()
        nextChunk = 0
    }

    private fun utteranceId(index: Int) = "meal-chunk-$index-${sessionToken}"

    private fun chunkIndexOf(utteranceId: String): Int? =
        utteranceId.removePrefix("meal-chunk-").substringBefore('-').toIntOrNull()

    fun release() {
        shuttingDown = true
        ready = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching {
            engine?.stop()
            engine?.shutdown()
        }
        engine = null
        _status.value = Status(state = State.IDLE)
    }

    companion object {
        /**
         * Removes Quranic orthographic marks that Turkish engines either skip or
         * mispronounce, and normalises whitespace and punctuation.
         */
        fun normalise(raw: String): String {
            val withoutTashkeel = raw
                // Arabic vowel marks, sukun, shadda, tanwin, dagger alif, pause marks.
                .replace(Regex("[\\u064B-\\u0652\\u0670\\u06D6-\\u06ED\\u0640]"), "")
                .replace(Regex("[\\u06DF\\u06E0\\u06E5\\u06E6]"), "")
            return withoutTashkeel
                .replace(Regex("\\s+"), " ")
                .replace(Regex("\\s+([,.;:!?])"), "$1")
                .replace(Regex("([,.;:!?])(?=\\S)"), "$1 ")
                .trim()
        }

        /**
         * Splits narration text into engine-safe chunks.
         *
         * A single `speak()` call for a whole verse is unreliable: several
         * engines cap utterance length and silently truncate, and a long
         * utterance cannot be interrupted mid-sentence. Sentences are the right
         * unit, with oversized sentences broken on clause boundaries.
         *
         * The limit is deliberately lower than the engine's hard cap (around
         * 180 rather than 220 characters): shorter utterances give the engine
         * room to reset its intonation at every sentence boundary, which is what
         * makes the result read like a person rather than one long drone.
         */
        fun splitIntoChunks(text: String, maxChars: Int = 180): List<String> {
            if (text.isBlank()) return emptyList()
            if (text.length <= maxChars) return listOf(text)

            val sentences = text.split(Regex("(?<=[.!?…])\\s+")).filter { it.isNotBlank() }
            val chunks = ArrayList<String>()
            val current = StringBuilder()

            fun flush() {
                if (current.isNotBlank()) {
                    chunks += current.toString().trim()
                    current.setLength(0)
                }
            }

            for (sentence in sentences) {
                if (sentence.length > maxChars) {
                    flush()
                    // Break an oversized sentence on commas, then on spaces.
                    var piece = StringBuilder()
                    for (clause in sentence.split(Regex("(?<=,)\\s+"))) {
                        if (piece.isNotEmpty() && piece.length + clause.length + 1 > maxChars) {
                            chunks += piece.toString().trim()
                            piece = StringBuilder()
                        }
                        if (piece.isNotEmpty()) piece.append(' ')
                        piece.append(clause)
                    }
                    if (piece.isNotBlank()) chunks += piece.toString().trim()
                    continue
                }
                // Never merge two sentences into one chunk: the boundary is what
                // gives the reader a breath and lets intonation fall.
                if (current.isNotBlank()) flush()
                current.append(sentence)
            }
            flush()
            return chunks.filter { it.isNotBlank() }
        }
    }
}
