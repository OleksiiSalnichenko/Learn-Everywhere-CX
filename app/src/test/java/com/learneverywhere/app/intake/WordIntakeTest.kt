package com.learneverywhere.app.intake

import com.learneverywhere.app.data.*
import com.learneverywhere.app.translation.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WordIntakeTest {
    @Test fun ukrainianMainLanguageIsSentToProviderAndCheckedBeforePreview() = runTest {
        val repository = MemoryRepository()
        var requestedTarget: Language? = null
        val provider = object : TranslationProvider {
            override suspend fun translate(text: String, sourceHint: SourceLanguage?, targetHint: Language?): TranslationResult {
                requestedTarget = targetHint
                return TranslationResult(listOf(SourceCandidate(SourceLanguage.UK, 0.99)), SourceLanguage.UK,
                    "дім", "дім", listOf("house"), "The house is warm.", Language.EN)
            }
        }
        val intake = WordIntake(repository, provider, { "My German words" })
        assertEquals(PrepareResult.Failure(TranslationException.Reason.INVALID_CONTENT), intake.prepare("дім", mainLanguage = Language.DE))
        assertEquals(Language.DE, requestedTarget)
        assertNull(repository.saved)
    }

    @Test fun repeatedForeignInputRequiresDeliberateDuplicateChoice() = runTest {
        val repository = MemoryRepository()
        repository.default = Dictionary("de-default", Language.DE, "German", 1, 1)
        repository.words = listOf(WordEntry("old", "de-default", WordContent("будинок", "Haus", null, "Das Haus ist klein."), 1, null, 1, 1))
        val intake = WordIntake(repository, FakeProvider(TranslationResult(
            listOf(SourceCandidate(SourceLanguage.DE, 0.99)), SourceLanguage.DE,
            "Haus", "дім", listOf("Haus"), "Das Haus steht dort."
        )), { "My German words" })
        assertTrue(intake.prepare("Haus") is PrepareResult.Duplicate)
        assertNull(repository.saved)
    }

    @Test fun missingSentenceNeverReachesPreviewOrDatabase() = runTest {
        val repository = MemoryRepository()
        val intake = WordIntake(repository, FakeProvider(TranslationResult(
            listOf(SourceCandidate(SourceLanguage.DE, 0.99)), SourceLanguage.DE,
            "Haus", "будинок", listOf("Haus"), "Haus"
        )), { "My German words" })
        assertEquals(PrepareResult.Failure(TranslationException.Reason.INVALID_CONTENT), intake.prepare("Haus"))
        assertNull(repository.saved)
    }

    @Test fun missingLanguageCandidatesNeverProducesDraft() = runTest {
        val repository = MemoryRepository()
        val intake = WordIntake(repository, FakeProvider(TranslationResult(
            emptyList(), SourceLanguage.EN, "house", "будинок", listOf("house"), "The house is small."
        )), { "My English words" })
        assertEquals(PrepareResult.Failure(TranslationException.Reason.INVALID_CONTENT), intake.prepare("house"))
        assertNull(repository.saved)
    }

    @Test fun unexpectedProviderFailureReturnsUnavailableWithoutSaving() = runTest {
        val repository = MemoryRepository()
        val provider = object : TranslationProvider {
            override suspend fun translate(text: String, sourceHint: SourceLanguage?, targetHint: Language?): TranslationResult = throw IllegalStateException("service failed")
        }
        val intake = WordIntake(repository, provider, { "My German words" })
        val result = intake.prepare("Haus", mainLanguage = Language.DE)
        assertEquals(PrepareResult.Failure(TranslationException.Reason.UNAVAILABLE), result)
        assertNull(repository.saved)
    }

    @Test fun germanInputUsesGermanDictionaryDespiteEnglishMainLanguage() = runTest {
        val repository = MemoryRepository()
        val intake = WordIntake(repository, FakeProvider(TranslationResult(
            listOf(SourceCandidate(SourceLanguage.DE, 0.98)), SourceLanguage.DE,
            "Haus", "будинок", listOf("Haus"), "Das Haus ist klein."
        )), { "My German words" })
        val result = intake.prepare("Haus", mainLanguage = Language.EN)
        assertTrue(result is PrepareResult.Draft)
        assertEquals(Language.DE, (result as PrepareResult.Draft).draft.language)
        assertNull(repository.saved)
    }

    @Test fun ukrainianInputUsesMainLanguageWhenNoExplicitTargetExists() = runTest {
        val repository = MemoryRepository()
        var requestedTarget: Language? = null
        val provider = object : TranslationProvider {
            override suspend fun translate(text: String, sourceHint: SourceLanguage?, targetHint: Language?): TranslationResult {
                requestedTarget = targetHint
                return TranslationResult(
                    listOf(SourceCandidate(SourceLanguage.UK, 0.99)), SourceLanguage.UK,
                    "дім", "дім", listOf("Haus"), "Das Haus ist warm.", Language.DE,
                )
            }
        }
        val result = WordIntake(repository, provider) { "Deutsch" }.prepare("дім", mainLanguage = Language.DE)
        assertEquals(Language.DE, requestedTarget)
        assertEquals(Language.DE, (result as PrepareResult.Draft).draft.language)
    }

    @Test fun detectedLanguageThatContradictsCandidateRankingRequiresChoice() = runTest {
        val result = WordIntake(MemoryRepository(), FakeProvider(TranslationResult(
            listOf(SourceCandidate(SourceLanguage.EN, 0.90), SourceCandidate(SourceLanguage.DE, 0.10)),
            SourceLanguage.DE, "Haus", "будинок", listOf("Haus"), "Das Haus ist klein.", Language.DE,
        ))) { "Words" }.prepare("Haus")
        assertTrue(result is PrepareResult.NeedSource)
        assertEquals(listOf(SourceLanguage.EN, SourceLanguage.DE), (result as PrepareResult.NeedSource).candidates)
    }

    @Test fun destinationChangeRequiresSecondConfirmationOfSameDraft() = runTest {
        val repository = MemoryRepository()
        val replacement = Dictionary("new-default", Language.DE, "New German", 2, 2)
        repository.saveResults.add(SaveWordResult.DestinationChanged(replacement))
        val stored = WordEntry("word", replacement.id, WordContent("дім", "Haus", null, "Das Haus ist warm."), 1, "draft", 3, 3)
        repository.saveResults.add(SaveWordResult.Saved(stored))
        val intake = WordIntake(repository, FakeProvider(TranslationResult(
            listOf(SourceCandidate(SourceLanguage.DE, 0.99)), SourceLanguage.DE,
            "Haus", "дім", listOf("Haus"), "Das Haus ist warm.", Language.DE,
        ))) { "Words" }
        val draft = (intake.prepare("Haus") as PrepareResult.Draft).draft

        val changed = intake.confirm(draft) as ConfirmResult.DestinationChanged
        assertEquals(draft.id, changed.draft.id)
        assertEquals(replacement.id, changed.draft.expectedDictionaryId)
        assertEquals("New German", changed.draft.dictionaryName)
        assertEquals(1, repository.saveCalls)

        val saved = intake.confirm(changed.draft) as ConfirmResult.Saved
        assertEquals(stored, saved.word)
        assertEquals(2, repository.saveCalls)
    }

    private class FakeProvider(private val result: TranslationResult) : TranslationProvider {
        override suspend fun translate(text: String, sourceHint: SourceLanguage?, targetHint: Language?) = result
    }

    private class MemoryRepository : DictionaryRepository {
        var saved: WordContent? = null
        var default: Dictionary? = null
        var words: List<WordEntry> = emptyList()
        var saveCalls: Int = 0
        val saveResults = ArrayDeque<SaveWordResult>()
        override fun observeDictionaries(language: Language): Flow<List<DictionarySummary>> = flowOf(emptyList())
        override fun observeWords(dictionaryId: String, limit: Int, offset: Int): Flow<List<WordEntry>> = flowOf(emptyList())
        override suspend fun getDictionary(id: String): Dictionary? = null
        override suspend fun getDefault(language: Language): Dictionary? = default?.takeIf { it.language == language }
        override suspend fun getWords(dictionaryId: String): List<WordEntry> = words.filter { it.dictionaryId == dictionaryId }
        override suspend fun getTransferSnapshot(ids: List<String>?): List<DictionaryExportSnapshot> = emptyList()
        override suspend fun create(language: Language, name: String): Dictionary = error("not used")
        override suspend fun rename(id: String, name: String) = Unit
        override suspend fun setDefault(id: String) = Unit
        override suspend fun saveWord(language: Language, expectedDictionaryId: String?, content: WordContent, draftId: String, automaticDictionaryName: String): SaveWordResult {
            saved = content
            saveCalls += 1
            return saveResults.removeFirstOrNull() ?: error("save result not configured")
        }
        override suspend fun updateWord(id: String, content: WordContent) = Unit
        override suspend fun deleteWord(id: String) = Unit
        override suspend fun deleteDictionary(id: String) = Unit
        override suspend fun repairDefaults() = Unit
        override suspend fun importDictionaries(items: List<ImportDictionary>): List<Dictionary> = emptyList()
    }
}
