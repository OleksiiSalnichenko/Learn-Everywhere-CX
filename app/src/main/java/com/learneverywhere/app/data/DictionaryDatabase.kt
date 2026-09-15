package com.learneverywhere.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DictionaryDao {
    @Query("SELECT d.*, (SELECT COUNT(*) FROM words w WHERE w.dictionaryId=d.id) AS wordCount, EXISTS(SELECT 1 FROM language_defaults f WHERE f.dictionaryId=d.id) AS isDefault FROM dictionaries d WHERE language=:language ORDER BY isDefault DESC, createdAt, id")
    fun observeDictionaries(language: Language): Flow<List<DictionarySummary>>
    @Query("SELECT * FROM words WHERE dictionaryId=:id ORDER BY insertionOrder, id LIMIT :limit OFFSET :offset")
    fun observeWords(id: String, limit: Int, offset: Int): Flow<List<WordEntry>>
    @Query("SELECT * FROM dictionaries WHERE id=:id") suspend fun dictionary(id: String): Dictionary?
    @Query("SELECT * FROM dictionaries WHERE language=:language ORDER BY createdAt, id") suspend fun dictionaries(language: Language): List<Dictionary>
    @Query("SELECT d.* FROM dictionaries d JOIN language_defaults f ON d.id=f.dictionaryId WHERE f.language=:language") suspend fun defaultDictionary(language: Language): Dictionary?
    @Query("SELECT * FROM words WHERE id=:id") suspend fun word(id: String): WordEntry?
    @Query("SELECT * FROM words WHERE draftId=:id") suspend fun draftWord(id: String): WordEntry?
    @Query("SELECT * FROM words WHERE dictionaryId=:id ORDER BY insertionOrder, id") suspend fun words(id: String): List<WordEntry>
    @Query("SELECT COALESCE(MAX(insertionOrder), -1) + 1 FROM words WHERE dictionaryId=:id") suspend fun nextOrder(id: String): Long
    @Insert suspend fun insert(dictionary: Dictionary)
    @Insert suspend fun insert(word: WordEntry)
    @Upsert suspend fun selectDefault(value: LanguageDefault)
    @Update suspend fun update(dictionary: Dictionary)
    @Update suspend fun update(word: WordEntry)
    @Query("DELETE FROM dictionaries WHERE id=:id") suspend fun deleteDictionary(id: String)
    @Query("DELETE FROM words WHERE id=:id") suspend fun deleteWord(id: String)
}

@Database(entities = [Dictionary::class, WordEntry::class, LanguageDefault::class], version = 1, exportSchema = true)
@TypeConverters(LanguageConverters::class)
abstract class DictionaryDatabase : RoomDatabase() {
    internal abstract fun dao(): DictionaryDao
    companion object {
        fun open(context: Context, name: String = "learn-everywhere.db"): DictionaryDatabase =
            Room.databaseBuilder(context.applicationContext, DictionaryDatabase::class.java, name).build()
    }
}
