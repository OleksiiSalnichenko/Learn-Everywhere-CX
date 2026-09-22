package com.learneverywhere.app.transfer

import com.learneverywhere.app.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class DictionaryTransferTest {
    private fun transfer(repository: DictionaryRepository) = DictionaryTransfer(repository) { number -> " (import $number)" }
    @Test fun `empty dictionary file is rejected before commit`() = runBlocking {
        val transfer = transfer(MemoryRepository())
        val preview = transfer.previewImport(ByteArrayInputStream("""{"schemaVersion":1,"dictionaries":[]}""".toByteArray()))
        assertTrue(preview is ImportPreview.Invalid)
    }

    @Test fun `present blank second translation is an invalid field`() = runBlocking {
        val source = """{"schemaVersion":1,"dictionaries":[{"language":"de","name":"Test","words":[{"ukrainian":"кіт","translation1":"die Katze","translation2":" ","example":"Die Katze schläft."}]}]}"""
        val preview = transfer(MemoryRepository()).previewImport(ByteArrayInputStream(source.toByteArray()))
        assertEquals("dictionaries[1].words[1].translation2", (preview as ImportPreview.Invalid).issue.location)
    }

    @Test fun `export refuses more than ten thousand words before writing`() = runBlocking {
        val repository = MemoryRepository()
        val dictionary = Dictionary("large", Language.DE, "Large", 0, 0)
        repository.dictionaries += dictionary
        repository.words[dictionary.id] = List(10_001) { order ->
            WordEntry("word-$order", dictionary.id, WordContent("слово", "Wort", null, "Ein Wort."), order.toLong(), null, 0, 0)
        }
        val output = ByteArrayOutputStream()
        try {
            transfer(repository).export(listOf(dictionary.id), output)
            fail("Export should reject a file the importer cannot accept")
        } catch (expected: IllegalArgumentException) {
            assertEquals(0, output.size())
        }
    }

    @Test fun `malformed, unknown version and oversized files do not reach commit`() = runBlocking {
        val transfer = transfer(MemoryRepository())
        val malformed = transfer.previewImport(ByteArrayInputStream("""{"schemaVersion":"1","dictionaries":[{}]}""".toByteArray()))
        assertEquals("file.schemaVersion", (malformed as ImportPreview.Invalid).issue.location)
        val unknown = transfer.previewImport(ByteArrayInputStream("""{"schemaVersion":2,"dictionaries":[{}]}""".toByteArray()))
        assertEquals("schemaVersion", (unknown as ImportPreview.Invalid).issue.location)
        val tooLarge = ByteArrayInputStream(ByteArray(DictionaryTransfer.MAX_BYTES + 1) { ' '.code.toByte() })
        val oversized = transfer.previewImport(tooLarge)
        assertEquals("file", (oversized as ImportPreview.Invalid).issue.location)
    }

    @Test fun `repeated import creates visible numbered copies without overwriting`() = runBlocking {
        val repository = MemoryRepository()
        val transfer = transfer(repository)
        val file = """{"schemaVersion":1,"dictionaries":[{"language":"de","name":"Reise","words":[{"ukrainian":"подорож","translation1":"die Reise","example":"Die Reise beginnt heute."}]}]}""".toByteArray()
        transfer.commitImport(transfer.previewImport(ByteArrayInputStream(file)) as ImportPreview.Valid)
        val second = transfer.previewImport(ByteArrayInputStream(file)) as ImportPreview.Valid
        assertEquals("Reise (import 2)", second.dictionaries.single().name)
        assertEquals(listOf("Reise → Reise (import 2)"), second.renamed)
        transfer.commitImport(second)
        val third = transfer.previewImport(ByteArrayInputStream(file)) as ImportPreview.Valid
        assertEquals("Reise (import 3)", third.dictionaries.single().name)
        assertEquals(listOf("Reise → Reise (import 3)"), third.renamed)
        transfer.commitImport(third)
        assertEquals(listOf("Reise", "Reise (import 2)", "Reise (import 3)"), repository.dictionaries.map { it.name })
        assertEquals(3, repository.words.values.sumOf { it.size })
    }

    @Test fun `preview accepts nullable and missing optional translation, then round trips in order`() = runBlocking {
        val repository = MemoryRepository()
        val transfer = transfer(repository)
        val file = """{"schemaVersion":1,"dictionaries":[{"language":"de","name":"Reise","isDefault":true,"words":[{"ukrainian":"кіт","translation1":"die Katze","translation2":null,"example":"Die Katze schläft."},{"ukrainian":"дім","translation1":"das Haus","example":"Das Haus ist groß."}]}]}"""
        val preview = transfer.previewImport(ByteArrayInputStream(file.toByteArray())) as ImportPreview.Valid
        assertEquals(2, preview.wordCount)
        transfer.commitImport(preview)
        val output = ByteArrayOutputStream()
        transfer.export(repository.dictionaries.map { it.id }, output)
        val parsed = transfer.previewImport(ByteArrayInputStream(output.toByteArray())) as ImportPreview.Valid
        assertEquals(listOf("кіт", "дім"), parsed.dictionaries.single().words.map { it.ukrainian })
        assertNull(parsed.dictionaries.single().words.first().translation2)
        assertNull(parsed.dictionaries.single().words.last().translation2)
    }

    private class MemoryRepository : DictionaryRepository {
        val dictionaries = mutableListOf<Dictionary>()
        val words = mutableMapOf<String, List<WordEntry>>()
        override fun observeDictionaries(language: Language): Flow<List<DictionarySummary>> = flowOf(
            dictionaries.filter { it.language == language }.map { dictionary ->
                DictionarySummary(dictionary, words[dictionary.id].orEmpty().size, dictionary.id == dictionaries.firstOrNull { it.language == language }?.id)
            }
        )
        override fun observeWords(dictionaryId: String, limit: Int, offset: Int): Flow<List<WordEntry>> = flowOf(emptyList())
        override suspend fun getDictionary(id: String) = dictionaries.find { it.id == id }
        override suspend fun getDefault(language: Language) = dictionaries.firstOrNull { it.language == language }
        override suspend fun getWords(dictionaryId: String) = words[dictionaryId].orEmpty()
        override suspend fun getTransferSnapshot(ids: List<String>?): List<DictionaryExportSnapshot> =
            dictionaries.filter { ids == null || it.id in ids }.map { dictionary ->
                DictionaryExportSnapshot(dictionary, words[dictionary.id].orEmpty(), dictionaries.firstOrNull { it.language == dictionary.language }?.id == dictionary.id)
            }
        override suspend fun create(language: Language, name: String): Dictionary = error("unused")
        override suspend fun rename(id: String, name: String) = error("unused")
        override suspend fun setDefault(id: String) = Unit
        override suspend fun saveWord(language: Language, expectedDictionaryId: String?, content: WordContent, draftId: String, automaticDictionaryName: String): SaveWordResult = error("unused")
        override suspend fun updateWord(id: String, content: WordContent) = error("unused")
        override suspend fun deleteWord(id: String) = error("unused")
        override suspend fun deleteDictionary(id: String) = error("unused")
        override suspend fun repairDefaults() = Unit
        override suspend fun importDictionaries(items: List<ImportDictionary>): List<Dictionary> {
            val start = dictionaries.size
            return items.mapIndexed { index, item ->
                val dictionary = Dictionary("${start + index}", item.language, item.name, 0, 0)
                dictionaries += dictionary
                words[dictionary.id] = item.words.mapIndexed { order, content -> WordEntry("$index-$order", dictionary.id, content, order.toLong(), null, 0, 0) }
                dictionary
            }
        }
    }
}
