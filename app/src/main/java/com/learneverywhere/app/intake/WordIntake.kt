package com.learneverywhere.app.intake

import com.learneverywhere.app.data.*
import com.learneverywhere.app.translation.*
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException

data class WordDraft(
    val id: String,
    val input: String,
    val source: SourceLanguage,
    val language: Language,
    val content: WordContent,
    val expectedDictionaryId: String?,
    val dictionaryName: String?,
)

sealed interface PrepareResult {
    data class NeedSource(val input: String, val candidates: List<SourceLanguage>) : PrepareResult
    data class NeedTarget(val input: String, val source: SourceLanguage) : PrepareResult
    data class Draft(val draft: WordDraft) : PrepareResult
    data class Duplicate(val draft: WordDraft, val existing: WordEntry) : PrepareResult
    data class Failure(val reason: TranslationException.Reason) : PrepareResult
}

sealed interface ConfirmResult {
    data class Saved(val word: WordEntry) : ConfirmResult
    data class DestinationChanged(val draft: WordDraft) : ConfirmResult
    data class Failure(val reason: RepositoryException.Reason?) : ConfirmResult
}

class WordIntake(
    private val repository: DictionaryRepository,
    private val provider: TranslationProvider,
    private val automaticDictionaryName: (Language) -> String,
) {
    suspend fun prepare(text: String, sourceHint: SourceLanguage? = null, targetLanguage: Language? = null, mainLanguage: Language? = null): PrepareResult {
        val input = Normalizer.normalize(text.trim(), Normalizer.Form.NFC)
        if (input.length !in 1..120 || input.any { it.isISOControl() }) return PrepareResult.Failure(TranslationException.Reason.INVALID_INPUT)
        val effectiveTarget = targetLanguage ?: mainLanguage
        val output = try { provider.translate(input, sourceHint, effectiveTarget) }
            catch (problem: CancellationException) { throw problem }
            catch (problem: TranslationException) { return PrepareResult.Failure(problem.reason) }
            catch (problem: Exception) { return PrepareResult.Failure(TranslationException.Reason.UNAVAILABLE) }
        if (output.sourceCandidates.isEmpty() || output.sourceCandidates.size > 3 ||
            output.sourceCandidates.any { it.confidence !in 0.0..1.0 } ||
            output.sourceCandidates.map { it.language }.distinct().size != output.sourceCandidates.size) {
            return PrepareResult.Failure(TranslationException.Reason.INVALID_CONTENT)
        }
        val possible = output.sourceCandidates.sortedByDescending { it.confidence }
        // A confident ranking and the declared result must agree. When they do not,
        // let the learner choose instead of silently filing the word in the wrong language.
        val contradictory = output.detectedSource != null && output.detectedSource != possible.firstOrNull()?.language
        val ambiguous = sourceHint == null && (contradictory || output.detectedSource == null ||
            possible.firstOrNull()?.confidence?.let { it < 0.70 } != false ||
            (possible.size > 1 && possible[0].confidence - possible[1].confidence < 0.20))
        if (ambiguous) return PrepareResult.NeedSource(input, possible.map { it.language })
        val source = sourceHint ?: output.detectedSource ?: return PrepareResult.Failure(TranslationException.Reason.INVALID_CONTENT)
        if (sourceHint != null && output.detectedSource != null && output.detectedSource != sourceHint) return PrepareResult.Failure(TranslationException.Reason.INVALID_CONTENT)
        if (source !in output.sourceCandidates.map { it.language }) return PrepareResult.Failure(TranslationException.Reason.INVALID_CONTENT)
        val destination = when (source) {
            SourceLanguage.DE -> Language.DE
            SourceLanguage.EN -> Language.EN
            SourceLanguage.UK -> targetLanguage ?: mainLanguage ?: return PrepareResult.NeedTarget(input, source)
        }
        if (source != SourceLanguage.UK && targetLanguage != null && targetLanguage != destination) return PrepareResult.Failure(TranslationException.Reason.INVALID_CONTENT)
        if (source == SourceLanguage.UK && output.targetLanguage != destination) return PrepareResult.Failure(TranslationException.Reason.INVALID_CONTENT)
        if (source != SourceLanguage.UK && output.targetLanguage != null && output.targetLanguage != destination) return PrepareResult.Failure(TranslationException.Reason.INVALID_CONTENT)
        val content = validate(output, input, source) ?: return PrepareResult.Failure(TranslationException.Reason.INVALID_CONTENT)
        return try {
            val dictionary = repository.getDefault(destination)
            val draft = WordDraft(UUID.randomUUID().toString(), input, source, destination, content, dictionary?.id, dictionary?.name ?: automaticDictionaryName(destination))
            val existing = dictionary?.let { repository.getWords(it.id).firstOrNull { word ->
                if (source == SourceLanguage.UK) key(word.content.ukrainian) == key(input)
                else key(word.content.translation1) == key(input)
            } }
            if (existing != null) PrepareResult.Duplicate(draft, existing) else PrepareResult.Draft(draft)
        } catch (problem: CancellationException) { throw problem }
          catch (problem: Exception) { PrepareResult.Failure(TranslationException.Reason.UNAVAILABLE) }
    }

    suspend fun confirm(draft: WordDraft): ConfirmResult = try {
        when (val result = repository.saveWord(draft.language, draft.expectedDictionaryId, draft.content, draft.id, automaticDictionaryName(draft.language))) {
            is SaveWordResult.Saved -> ConfirmResult.Saved(result.word)
            is SaveWordResult.DestinationChanged -> ConfirmResult.DestinationChanged(draft.copy(expectedDictionaryId = result.dictionary?.id, dictionaryName = result.dictionary?.name ?: automaticDictionaryName(draft.language)))
        }
    } catch (problem: CancellationException) { throw problem }
      catch (problem: RepositoryException) {
        ConfirmResult.Failure(problem.reason)
    } catch (problem: Exception) { ConfirmResult.Failure(null) }

    private fun validate(result: TranslationResult, input: String, source: SourceLanguage): WordContent? {
        if (result.sourceCandidates.isEmpty() || result.sourceCandidates.any { it.confidence !in 0.0..1.0 } || result.sourceCandidates.map { it.language }.distinct().size != result.sourceCandidates.size) return null
        if (result.detectedSource != null && result.detectedSource !in result.sourceCandidates.map { it.language }) return null
        if (result.normalizedInput.length !in 1..120 || result.ukrainian.length !in 1..120 || result.example.length !in 1..500 || result.foreignMeanings.size !in 1..2) return null
        if (listOf(result.normalizedInput, result.ukrainian, result.example).any { it.isBlank() || it.any(Char::isISOControl) }) return null
        val sentence = result.example.trim()
        if (sentence.split(Regex("\\s+")).size < 2 || sentence.last() !in ".!?…") return null
        if (result.foreignMeanings.any { it.length !in 1..120 || it.isBlank() || it.any(Char::isISOControl) }) return null
        if (result.foreignMeanings.size == 2 && key(result.foreignMeanings[0]) == key(result.foreignMeanings[1])) return null
        // A foreign source keeps the entered expression as meaning 1; do not silently replace it.
        if (source != SourceLanguage.UK && key(result.foreignMeanings[0]) != key(result.normalizedInput)) return null
        if (source != SourceLanguage.UK && key(result.normalizedInput) != key(input)) return null
        return WordContent(result.ukrainian.trim(), result.foreignMeanings[0].trim(), result.foreignMeanings.getOrNull(1)?.trim(), result.example.trim())
    }

    private fun key(value: String) = Normalizer.normalize(value.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)
}
