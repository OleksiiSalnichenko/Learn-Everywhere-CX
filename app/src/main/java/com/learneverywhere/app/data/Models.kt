package com.learneverywhere.app.data

import androidx.room.*

/** Only learning languages belong in dictionaries; Ukrainian is the translation language. */
enum class Language(val code: String) { DE("de"), EN("en") }

@Entity(tableName = "dictionaries", indices = [Index(value = ["id", "language"], unique = true), Index(value = ["language", "nameKey"], unique = true)])
data class Dictionary(
    @PrimaryKey val id: String,
    val language: Language,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val nameKey: String = name.trim().lowercase(java.util.Locale.ROOT),
)

data class WordContent(val ukrainian: String, val translation1: String, val translation2: String? = null, val example: String)

@Entity(tableName = "words", foreignKeys = [ForeignKey(entity = Dictionary::class, parentColumns = ["id"], childColumns = ["dictionaryId"], onDelete = ForeignKey.CASCADE)], indices = [Index("dictionaryId"), Index(value = ["draftId"], unique = true)])
data class WordEntry(
    @PrimaryKey val id: String,
    val dictionaryId: String,
    @Embedded val content: WordContent,
    val insertionOrder: Long,
    val draftId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "language_defaults", foreignKeys = [ForeignKey(entity = Dictionary::class, parentColumns = ["id", "language"], childColumns = ["dictionaryId", "language"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["dictionaryId", "language"], unique = true)])
data class LanguageDefault(@PrimaryKey val language: Language, val dictionaryId: String)

data class DictionarySummary(@Embedded val dictionary: Dictionary, val wordCount: Int, val isDefault: Boolean)
data class ImportDictionary(val language: Language, val name: String, val words: List<WordContent>, val isDefault: Boolean = false)

sealed interface SaveWordResult {
    data class Saved(val word: WordEntry) : SaveWordResult
    data class DestinationChanged(val dictionary: Dictionary?) : SaveWordResult
}
class RepositoryException(val reason: Reason) : IllegalArgumentException(reason.name) {
    enum class Reason { INVALID_NAME, DUPLICATE_NAME, INVALID_WORD, MISSING_DICTIONARY, MISSING_WORD }
}
class LanguageConverters {
    @TypeConverter fun encode(language: Language): String = language.code
    @TypeConverter fun decode(value: String): Language = Language.entries.first { it.code == value }
}

/** Immutable export data read together at one database version. */
data class DictionaryExportSnapshot(val dictionary: Dictionary, val words: List<WordEntry>, val isDefault: Boolean)
