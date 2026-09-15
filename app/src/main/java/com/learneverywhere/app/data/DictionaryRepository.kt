package com.learneverywhere.app.data

import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction
import java.util.UUID
import java.util.Locale
import java.text.Normalizer
import com.learneverywhere.app.data.RepositoryException.Reason

interface DictionaryRepository {
    fun observeDictionaries(language: Language): Flow<List<DictionarySummary>>
    fun observeWords(dictionaryId: String, limit: Int = 100, offset: Int = 0): Flow<List<WordEntry>>
    suspend fun getDictionary(id: String): Dictionary?
    suspend fun getDefault(language: Language): Dictionary?
    suspend fun getWords(dictionaryId: String): List<WordEntry>
    suspend fun getTransferSnapshot(ids: List<String>? = null): List<DictionaryExportSnapshot>
    suspend fun create(language: Language, name: String): Dictionary
    suspend fun rename(id: String, name: String)
    suspend fun setDefault(id: String)
    suspend fun saveWord(language: Language, expectedDictionaryId: String?, content: WordContent, draftId: String, automaticDictionaryName: String): SaveWordResult
    suspend fun updateWord(id: String, content: WordContent)
    suspend fun deleteWord(id: String)
    suspend fun deleteDictionary(id: String)
    suspend fun repairDefaults()
    suspend fun importDictionaries(items: List<ImportDictionary>): List<Dictionary>
}

class RoomDictionaryRepository(private val database: DictionaryDatabase) : DictionaryRepository {
    private val dao get() = database.dao()
    private fun key(value: String) = Normalizer.normalize(value.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)
    private fun validName(name: String): String = name.trim().also {
        if (it.length !in 1..60) throw RepositoryException(Reason.INVALID_NAME)
    }
    override fun observeDictionaries(language: Language) = dao.observeDictionaries(language)
    override fun observeWords(dictionaryId: String, limit: Int, offset: Int): Flow<List<WordEntry>> {
        require(limit > 0 && offset >= 0)
        return dao.observeWords(dictionaryId, limit, offset)
    }
    override suspend fun getDictionary(id: String) = dao.dictionary(id)
    override suspend fun getDefault(language: Language) = dao.defaultDictionary(language)
    override suspend fun getWords(dictionaryId: String) = dao.words(dictionaryId)
    override suspend fun getTransferSnapshot(ids: List<String>?): List<DictionaryExportSnapshot> = database.withTransaction {
        // All reads share one database version, including names, defaults and complete word lists.
        val dictionaries = Language.entries.flatMap { dao.dictionaries(it) }
        val selectedIds = ids?.toSet()
        if (selectedIds != null && !dictionaries.map { it.id }.toSet().containsAll(selectedIds)) {
            throw RepositoryException(Reason.MISSING_DICTIONARY)
        }
        val defaults = Language.entries.associateWith { dao.defaultDictionary(it)?.id }
        dictionaries.filter { selectedIds == null || it.id in selectedIds }.map { dictionary ->
            DictionaryExportSnapshot(dictionary, dao.words(dictionary.id), defaults[dictionary.language] == dictionary.id)
        }
    }
    override suspend fun create(language: Language, name: String): Dictionary = database.withTransaction {
        val clean = validName(name)
        if (dao.dictionaries(language).any { it.nameKey == key(clean) }) throw RepositoryException(Reason.DUPLICATE_NAME)
        val now = System.currentTimeMillis()
        val dictionary = Dictionary(UUID.randomUUID().toString(), language, clean, now, now, key(clean))
        dao.insert(dictionary)
        if (dao.defaultDictionary(language) == null) dao.selectDefault(LanguageDefault(language, dictionary.id))
        dictionary
    }
    override suspend fun rename(id: String, name: String): Unit = database.withTransaction {
        val dictionary = dao.dictionary(id) ?: throw RepositoryException(Reason.MISSING_DICTIONARY)
        val clean = validName(name)
        if (dao.dictionaries(dictionary.language).any { it.id != id && it.nameKey == key(clean) }) throw RepositoryException(Reason.DUPLICATE_NAME)
        dao.update(dictionary.copy(name = clean, nameKey = key(clean), updatedAt = System.currentTimeMillis()))
    }
    override suspend fun setDefault(id: String): Unit = database.withTransaction {
        val dictionary = dao.dictionary(id) ?: throw RepositoryException(Reason.MISSING_DICTIONARY)
        dao.selectDefault(LanguageDefault(dictionary.language, id))
    }
    override suspend fun saveWord(language: Language, expectedDictionaryId: String?, content: WordContent, draftId: String, automaticDictionaryName: String): SaveWordResult = database.withTransaction {
        require(draftId.isNotBlank())
        // Retrying a confirmed draft must not create a second word, even after changing default.
        dao.draftWord(draftId)?.let { return@withTransaction SaveWordResult.Saved(it) }
        val clean = validContent(content)
        // Older data may lack the default row; reuse the oldest existing collection.
        // A repaired destination still requires confirmation when the preview expected none.
        repairDefault(language)
        val current = dao.defaultDictionary(language)
        if (current?.id != expectedDictionaryId) return@withTransaction SaveWordResult.DestinationChanged(current)
        val destination = current ?: create(language, automaticDictionaryName)
        SaveWordResult.Saved(insertWord(destination.id, clean, draftId))
    }
    private suspend fun insertWord(dictionaryId: String, content: WordContent, draftId: String? = null): WordEntry {
        val now = System.currentTimeMillis()
        val word = WordEntry(UUID.randomUUID().toString(), dictionaryId, content, dao.nextOrder(dictionaryId), draftId, now, now)
        dao.insert(word)
        return word
    }
    private fun validContent(content: WordContent): WordContent {
        val clean = content.copy(ukrainian = content.ukrainian.trim(), translation1 = content.translation1.trim(), translation2 = content.translation2?.trim()?.takeIf { it.isNotEmpty() }, example = content.example.trim())
        if (clean.ukrainian.length !in 1..120 || clean.translation1.length !in 1..120 || clean.example.length !in 1..500 ||
            (clean.translation2 != null && (clean.translation2.length > 120 || key(clean.translation2) == key(clean.translation1)))) {
            throw RepositoryException(Reason.INVALID_WORD)
        }
        return clean
    }
    override suspend fun updateWord(id: String, content: WordContent): Unit = database.withTransaction {
        val word = dao.word(id) ?: throw RepositoryException(Reason.MISSING_WORD)
        dao.update(word.copy(content = validContent(content), updatedAt = System.currentTimeMillis()))
    }
    override suspend fun deleteWord(id: String): Unit = database.withTransaction {
        if (dao.word(id) == null) throw RepositoryException(Reason.MISSING_WORD)
        dao.deleteWord(id)
    }
    override suspend fun deleteDictionary(id: String): Unit = database.withTransaction {
        val dictionary = dao.dictionary(id) ?: throw RepositoryException(Reason.MISSING_DICTIONARY)
        dao.deleteDictionary(id)
        repairDefault(dictionary.language)
    }
    override suspend fun repairDefaults(): Unit = database.withTransaction {
        Language.entries.forEach { repairDefault(it) }
    }
    private suspend fun repairDefault(language: Language) {
        if (dao.defaultDictionary(language) == null) {
            dao.dictionaries(language).firstOrNull()?.let { dao.selectDefault(LanguageDefault(language, it.id)) }
        }
    }
    override suspend fun importDictionaries(items: List<ImportDictionary>): List<Dictionary> = database.withTransaction {
        val previousDefaults = items.map { it.language }.distinct().associateWith { dao.defaultDictionary(it) }
        val imported = items.map { item ->
            val dictionary = create(item.language, item.name)
            item.words.forEach { insertWord(dictionary.id, validContent(it)) }
            dictionary
        }
        // A file's selection applies only when it cannot replace the user's existing default.
        previousDefaults.forEach { (language, previous) ->
            if (previous == null) {
                val candidates = items.indices.filter { items[it].language == language }
                val selectedIndex = candidates.firstOrNull { items[it].isDefault } ?: candidates.first()
                dao.selectDefault(LanguageDefault(language, imported[selectedIndex].id))
            }
        }
        imported
    }
}
