package com.learneverywhere.app.translation

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.Firebase
import com.learneverywhere.app.data.Language
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*

class GeminiTranslationProvider(private val context: Context) : TranslationProvider {
    companion object { const val MODEL = "gemini-3.5-flash-lite" }

    override suspend fun translate(text: String, sourceHint: SourceLanguage?, targetHint: Language?): TranslationResult {
        if (FirebaseApp.getApps(context).isEmpty()) throw TranslationException(TranslationException.Reason.NOT_CONFIGURED)
        try {
        val candidateSchema = Schema.obj(mapOf(
            "language" to Schema.enumeration(listOf("uk", "de", "en")),
            "confidence" to Schema.double(),
        ))
        val schema = Schema.obj(mapOf(
            "sourceCandidates" to Schema.array(candidateSchema),
            "detectedSource" to Schema.enumeration(listOf("uk", "de", "en", "ambiguous")),
            "targetLanguage" to Schema.enumeration(listOf("de", "en")),
            "normalizedInput" to Schema.string(),
            "ukrainian" to Schema.string(),
            "foreignMeanings" to Schema.array(Schema.string()),
            "example" to Schema.string(),
        ))
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
        // The input is data, not a command; the response cannot directly mutate local storage.
        val prompt = """Return only the JSON schema fields. Classify this input as Ukrainian, German or English, listing plausible source candidates with confidence 0..1. If ambiguous, detectedSource='ambiguous'. Honor a source hint when present. The target hint selects German or English only when the source is Ukrainian; otherwise targetLanguage must match the detected foreign source. Ukrainian input: provide the most natural foreign equivalent and optionally one distinct second equivalent. German/English input: preserve the normalized foreign input as first meaning; add a second only if a natural synonym shares the Ukrainian meaning. Provide one short grammatical example in targetLanguage. No invented second meaning. Source hint: ${sourceHint?.code ?: "none"}. Target hint: ${targetHint?.code ?: "none"}. Input (quoted JSON string): ${Json.encodeToString(JsonPrimitive.serializer(), JsonPrimitive(text))}"""
            val raw = withTimeout(20_000) { model.generateContent(prompt).text }
                ?: throw TranslationException(TranslationException.Reason.INVALID_CONTENT)
            return parseTranslation(raw)
        } catch (timeout: TimeoutCancellationException) {
            throw TranslationException(TranslationException.Reason.NETWORK)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (problem: TranslationException) {
            throw problem
        } catch (problem: Exception) {
            val message = problem.message.orEmpty().lowercase()
            val reason = when {
                "quota" in message || "429" in message || "resource_exhausted" in message -> TranslationException.Reason.QUOTA
                "network" in message || "timeout" in message || "unavailable" in message || "connect" in message -> TranslationException.Reason.NETWORK
                else -> TranslationException.Reason.UNAVAILABLE
            }
            throw TranslationException(reason)
        }
    }
}

/** Parse model data independently of the Firebase response schema. */
fun parseTranslation(raw: String): TranslationResult {
    try {
        val obj = Json.parseToJsonElement(raw) as? JsonObject ?: throw IllegalArgumentException()
        fun required(name: String): String = (obj[name] as? JsonPrimitive)
            ?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException()
        val candidates = (obj["sourceCandidates"] as? JsonArray)?.map {
            val item = it as? JsonObject ?: throw IllegalArgumentException()
            val languageValue = (item["language"] as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content
                ?: throw IllegalArgumentException()
            val language = SourceLanguage.entries.firstOrNull { code -> code.code == languageValue } ?: throw IllegalArgumentException()
            val confidencePrimitive = (item["confidence"] as? JsonPrimitive)?.takeIf { primitive -> !primitive.isString }
                ?: throw IllegalArgumentException()
            val confidence = confidencePrimitive.doubleOrNull ?: throw IllegalArgumentException()
            if (confidence !in 0.0..1.0) throw IllegalArgumentException()
            SourceCandidate(language, confidence)
        } ?: throw IllegalArgumentException()
        if (candidates.isEmpty() || candidates.size > 3 || candidates.map { it.language }.distinct().size != candidates.size) throw IllegalArgumentException()
        val detected = required("detectedSource").let { value ->
            if (value == "ambiguous") null else SourceLanguage.entries.firstOrNull { it.code == value } ?: throw IllegalArgumentException()
        }
        val target = Language.entries.firstOrNull { it.code == required("targetLanguage") } ?: throw IllegalArgumentException()
        val meanings = (obj["foreignMeanings"] as? JsonArray)?.map {
            (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content?.trim()
                ?.takeIf { value -> value.isNotEmpty() } ?: throw IllegalArgumentException()
        } ?: throw IllegalArgumentException()
        if (meanings.size !in 1..2) throw IllegalArgumentException()
        return TranslationResult(candidates, detected, required("normalizedInput"), required("ukrainian"), meanings, required("example"), target)
    } catch (problem: Exception) {
        throw TranslationException(TranslationException.Reason.INVALID_CONTENT)
    }
}
