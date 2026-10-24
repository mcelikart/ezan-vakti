package com.example.data

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.LongBuffer
import kotlin.math.sqrt

/** The bundled multilingual E5 retrieval model: masked mean pooling and L2 norm. */
internal class QuranSentenceEncoder(
    private val model: ByteBuffer,
    vocabularyBytes: ByteArray,
    normalizerBytes: ByteArray
) : Closeable {
    private val environment = OrtEnvironment.getEnvironment()
    private val tokenizer = XlmRobertaTokenizer(vocabularyBytes, normalizerBytes)
    private val options = OrtSession.SessionOptions().apply {
        setIntraOpNumThreads(2)
        setInterOpNumThreads(1)
    }
    private val session = try {
        environment.createSession(model, options)
    } catch (error: Throwable) {
        options.close()
        throw error
    }

    fun encode(text: String): FloatArray {
        // E5 was trained with these English task prefixes, also for Turkish input.
        val ids = tokenizer.encode("query: $text", MAX_TOKENS)
        val shape = longArrayOf(1L, ids.size.toLong())
        val inputs = linkedMapOf<String, OnnxTensor>()
        try {
            inputs["input_ids"] = OnnxTensor.createTensor(environment, LongBuffer.wrap(ids), shape)
            inputs["attention_mask"] = OnnxTensor.createTensor(environment, LongBuffer.wrap(LongArray(ids.size) { 1L }), shape)
            inputs["token_type_ids"] = OnnxTensor.createTensor(environment, LongBuffer.wrap(LongArray(ids.size)), shape)
            session.run(inputs).use { result ->
                val states = (result[0].value as Array<*>)[0] as Array<*>
                val pooled = DoubleArray(DIMENSIONS)
                states.forEach { state ->
                    val vector = state as FloatArray
                    for (i in pooled.indices) pooled[i] += vector[i].toDouble() / ids.size
                }
                val norm = sqrt(pooled.sumOf { it * it }).coerceAtLeast(1e-12)
                return FloatArray(DIMENSIONS) { (pooled[it] / norm).toFloat() }
            }
        } finally {
            inputs.values.forEach { it.close() }
        }
    }

    override fun close() {
        try { session.close() } finally { options.close() }
    }

    companion object {
        const val DIMENSIONS = 384
        const val MAX_TOKENS = 128
    }
}
