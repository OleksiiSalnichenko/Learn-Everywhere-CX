package com.learneverywhere.app.translation

import com.learneverywhere.app.data.Language

enum class SourceLanguage(val code: String) { UK("uk"), DE("de"), EN("en") }
data class SourceCandidate(val language: SourceLanguage, val confidence: Double)

/** Model output is a suggestion. WordIntake validates it again before preview. */
data class TranslationResult(
    val sourceCandidates: List<SourceCandidate>,
    val detectedSource: SourceLanguage?,
    val normalizedInput: String,
    val ukrainian: String,
    val foreignMeanings: List<String>,
    val example: String,
    val targetLanguage: Language? = null,
)

interface TranslationProvider {
    suspend fun translate(text: String, sourceHint: SourceLanguage?, targetHint: Language?): TranslationResult
}

class TranslationException(val reason: Reason) : Exception(reason.name) {
    enum class Reason { NOT_CONFIGURED, QUOTA, NETWORK, INVALID_INPUT, UNSUPPORTED_LANGUAGE, INVALID_CONTENT, UNAVAILABLE }
}
