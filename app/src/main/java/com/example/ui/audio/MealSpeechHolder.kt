package com.example.ui.audio

import android.content.Context

/**
 * Process-scoped owner for the single [MealSpeechEngine] instance.
 *
 * Why this exists: the engine used to be created with `remember { MealSpeechEngine(context) }`
 * inside `QuranReadingScreen` and released by that screen's `DisposableEffect`. That
 * meant every visit to the Quran module constructed a **new TextToSpeech engine
 * synchronously during composition**. The constructor binds to the system TTS
 * service, which routinely takes hundreds of milliseconds and happens before the
 * first frame is drawn — so the screen appeared to freeze on entry. Repeated
 * entries paid the cost again because the engine had just been torn down.
 *
 * One engine, created once and reused:
 *  - the entry freeze disappears after the first creation,
 *  - the engine is warmed during app start, so even that first cost lands outside
 *    the Quran screen,
 *  - narration state survives navigating away and back, which is what a reader
 *    expects when they open a tool and close it.
 */
internal object MealSpeechHolder {

    private val lock = Any()
    private var engine: MealSpeechEngine? = null

    /** Returns the shared engine, creating it on first use. */
    fun get(context: Context): MealSpeechEngine = synchronized(lock) {
        engine ?: MealSpeechEngine(context.applicationContext).also { engine = it }
    }

    /**
     * Warms the engine before it is needed.
     *
     * Call this from an app-level effect (not a composable body) so the expensive
     * service binding overlaps with something the user is already looking at.
     */
    fun warmUp(context: Context) {
        get(context)
    }

    /**
     * Releases the engine. Only for genuine teardown — never on screen exit,
     * because rebuilding it is exactly what caused the stall.
     */
    fun release() {
        synchronized(lock) {
            engine?.release()
            engine = null
        }
    }
}
