package com.learneverywhere.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DictionaryRepositoryTest {
    private lateinit var db: DictionaryDatabase
    private lateinit var repository: DictionaryRepository
    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DictionaryDatabase::class.java).build()
        repository = RoomDictionaryRepository(db)
    }
    @After fun close() { db.close() }
    @Test fun oneDefaultPerLanguageAndSelectionDoesNotCrossLanguages() = runBlocking {
        val first = repository.create(Language.DE, "First")
        val second = repository.create(Language.DE, "Second")
        val english = repository.create(Language.EN, "English")
        assertEquals(first.id, repository.getDefault(Language.DE)?.id)
        repository.setDefault(second.id)
        assertEquals(listOf(second.id, first.id), repository.observeDictionaries(Language.DE).first().map { it.dictionary.id })
        assertEquals(1, repository.observeDictionaries(Language.DE).first().count { it.isDefault })
        assertEquals(english.id, repository.getDefault(Language.EN)?.id)
    }
    @Test fun confirmedDraftIsIdempotentAndReconfirmsChangedDestination() = runBlocking {
        val content = WordContent("кіт", "Katze", null, "Die Katze schläft.")
        val result = repository.saveWord(Language.DE, null, content, "draft-1", "My German words") as SaveWordResult.Saved
        assertEquals("кіт", result.word.content.ukrainian)
        assertEquals(result.word.dictionaryId, repository.getDefault(Language.DE)?.id)
        val next = repository.create(Language.DE, "Travel")
        repository.setDefault(next.id)
        val retry = repository.saveWord(Language.DE, result.word.dictionaryId, content, "draft-1", "My German words") as SaveWordResult.Saved
        assertEquals(result.word.id, retry.word.id)
        val changed = repository.saveWord(Language.DE, result.word.dictionaryId, content, "draft-2", "My German words") as SaveWordResult.DestinationChanged
        assertEquals(next.id, changed.dictionary?.id)
        assertTrue(repository.getWords(next.id).isEmpty())
        repository.saveWord(Language.DE, next.id, content, "draft-2", "My German words")
        assertEquals(1, repository.getWords(next.id).size)
    }
    @Test fun rejectedBatchRollsBackAndMissingSelectionKeepsDefault() = runBlocking {
        val original = repository.create(Language.DE, "Original")
        val content = WordContent("дім", "Haus", null, "Das Haus ist groß.")
        try {
            repository.importDictionaries(listOf(ImportDictionary(Language.EN, "Travel", listOf(content)), ImportDictionary(Language.EN, "Broken", listOf(content.copy(example = " ")))))
            fail("Invalid batch must fail")
        } catch (error: RepositoryException) { assertEquals(RepositoryException.Reason.INVALID_WORD, error.reason) }
        assertTrue(repository.observeDictionaries(Language.EN).first().isEmpty())
        assertNull(repository.getDefault(Language.EN))
        try { repository.setDefault("missing"); fail("Missing dictionary must fail") }
        catch (error: RepositoryException) { assertEquals(RepositoryException.Reason.MISSING_DICTIONARY, error.reason) }
        assertEquals(original.id, repository.getDefault(Language.DE)?.id)
        repository.importDictionaries(listOf(ImportDictionary(Language.EN, "Travel", listOf(content))))
        assertEquals(1, repository.observeDictionaries(Language.EN).first().single().wordCount)
    }
    @Test fun editsDeletionAndRepairPersistAcrossDatabaseReopen() = runBlocking {
        db.close()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val filename = "repository-persistence-test.db"
        context.deleteDatabase(filename)
        db = DictionaryDatabase.open(context, filename)
        repository = RoomDictionaryRepository(db)
        val first = repository.create(Language.DE, "First")
        val second = repository.create(Language.DE, "Second")
        val english = repository.create(Language.EN, "English")
        val word = (repository.saveWord(Language.DE, first.id, WordContent("дім", "Haus", null, "Das Haus ist groß."), "persist", "My German words") as SaveWordResult.Saved).word
        repository.rename(first.id, "Renamed")
        repository.updateWord(word.id, word.content.copy(ukrainian = "будинок"))
        db.close()
        db = DictionaryDatabase.open(context, filename)
        repository = RoomDictionaryRepository(db)
        repository.repairDefaults()
        assertEquals("Renamed", repository.getDictionary(first.id)?.name)
        assertEquals("будинок", repository.getWords(first.id).single().content.ukrainian)
        assertEquals(first.id, repository.getDefault(Language.DE)?.id)
        repository.deleteWord(word.id)
        assertTrue(repository.getWords(first.id).isEmpty())
        repository.saveWord(Language.DE, first.id, word.content, "cascade", "My German words")
        repository.deleteDictionary(first.id)
        assertTrue(repository.getWords(first.id).isEmpty())
        assertEquals(second.id, repository.getDefault(Language.DE)?.id)
        repository.deleteDictionary(second.id)
        assertNull(repository.getDefault(Language.DE))
        assertEquals(english.id, repository.getDefault(Language.EN)?.id)
        context.deleteDatabase(filename)
        Unit
    }
    @Test fun firstConfirmedSaveUsesBoundarySuppliedLocalizedName() = runBlocking {
        repository.saveWord(Language.DE, null, WordContent("дім", "Haus", null, "Das Haus ist groß."), "localized", "Мої німецькі слова")
        assertEquals("Мої німецькі слова", repository.getDefault(Language.DE)?.name)
    }
    @Test fun missingDefaultIsRepairedBeforeConfirmWithoutCreatingExtraCollection() = runBlocking {
        val original = repository.create(Language.DE, "Existing")
        // Simulate legacy data with dictionaries but a missing default row.
        db.openHelper.writableDatabase.execSQL("DELETE FROM language_defaults")
        val content = WordContent("дім", "Haus", null, "Das Haus ist groß.")
        val result = repository.saveWord(Language.DE, null, content, "repair", "My German words")
        assertTrue(result is SaveWordResult.DestinationChanged)
        assertEquals(original.id, repository.getDefault(Language.DE)?.id)
        assertEquals(listOf(original.id), repository.observeDictionaries(Language.DE).first().map { it.dictionary.id })
        assertTrue(repository.getWords(original.id).isEmpty())
        val confirmed = repository.saveWord(Language.DE, original.id, content, "repair", "My German words") as SaveWordResult.Saved
        assertEquals(original.id, confirmed.word.dictionaryId)
    }
    @Test fun importHonorsFlagOnlyWhenLanguageHasNoExistingDefault() = runBlocking {
        val imported = repository.importDictionaries(listOf(
            ImportDictionary(Language.DE, "First", emptyList()),
            ImportDictionary(Language.DE, "Selected", emptyList(), isDefault = true),
        ))
        assertEquals(imported[1].id, repository.getDefault(Language.DE)?.id)
        repository.importDictionaries(listOf(ImportDictionary(Language.DE, "Later", emptyList(), isDefault = true)))
        assertEquals(imported[1].id, repository.getDefault(Language.DE)?.id)
        val english = repository.importDictionaries(listOf(
            ImportDictionary(Language.EN, "English first", emptyList()),
            ImportDictionary(Language.EN, "English second", emptyList()),
        ))
        assertEquals(english[0].id, repository.getDefault(Language.EN)?.id)
    }
    @Test fun transferSnapshotIncludesCompleteOrderedWordsAndKeepsItsOriginalValues() = runBlocking {
        val english = repository.create(Language.EN, "English")
        val contents = (1..105).map { WordContent("слово $it", "Wort $it", null, "Das ist Wort $it.") }
        val german = repository.importDictionaries(listOf(ImportDictionary(Language.DE, "German", contents))).single()
        val other = repository.create(Language.DE, "Other")
        repository.setDefault(other.id)
        val snapshot = repository.getTransferSnapshot()
        assertEquals(listOf(german.id, other.id, english.id), snapshot.map { it.dictionary.id })
        assertEquals((1..105).map { "Wort $it" }, snapshot[0].words.map { it.content.translation1 })
        assertEquals(listOf(false, true, true), snapshot.map { it.isDefault })
        repository.rename(german.id, "Changed")
        repository.updateWord(snapshot[0].words[0].id, contents[0].copy(translation1 = "Changed"))
        repository.setDefault(german.id)
        assertEquals("German", snapshot[0].dictionary.name)
        assertEquals("Wort 1", snapshot[0].words[0].content.translation1)
        assertFalse(snapshot[0].isDefault)
        assertEquals(listOf(german.id, english.id), repository.getTransferSnapshot(listOf(english.id, german.id, german.id)).map { it.dictionary.id })
        assertTrue(repository.getTransferSnapshot(emptyList()).isEmpty())
        // Export overlapping atomic imports must see each complete batch, never half a batch.
        val writer = async(kotlinx.coroutines.Dispatchers.Default) {
            repeat(8) { batch ->
                repository.importDictionaries(listOf(
                    ImportDictionary(Language.EN, "batch$batch:a", listOf(contents[0])),
                    ImportDictionary(Language.EN, "batch$batch:b", listOf(contents[1])),
                ))
            }
        }
        repeat(16) {
            val batches = repository.getTransferSnapshot().filter { it.dictionary.name.startsWith("batch") }
            batches.groupBy { it.dictionary.name.substringBefore(':') }.values.forEach { pair ->
                assertEquals(2, pair.size)
                assertTrue(pair.all { it.words.size == 1 })
            }
        }
        writer.await()
        try { repository.getTransferSnapshot(listOf(german.id, "missing")); fail("No partial export for missing ID") }
        catch (error: RepositoryException) { assertEquals(RepositoryException.Reason.MISSING_DICTIONARY, error.reason) }
    }
}
