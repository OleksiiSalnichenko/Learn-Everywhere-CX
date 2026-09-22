package com.learneverywhere.app.transfer

import com.learneverywhere.app.data.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale

enum class TransferProblem { INVALID_JSON, INVALID_TYPE, UNKNOWN_VERSION, EMPTY, TOO_LARGE,
    TOO_MANY_DICTIONARIES, TOO_MANY_WORDS, INVALID_LANGUAGE, INVALID_NAME, MULTIPLE_DEFAULTS,
    INVALID_UKRAINIAN, INVALID_TRANSLATION1, INVALID_TRANSLATION2, INVALID_EXAMPLE,
    TOO_MANY_COPIES, FILE_READ, MISSING_DICTIONARY, NO_SELECTION, MISSING_PENDING }
data class ImportIssue(val location: String, val problem: TransferProblem)
class TransferException(val problem: TransferProblem) : IllegalArgumentException(problem.name)
sealed interface ImportPreview {
    data class Valid(val dictionaries: List<ImportDictionary>, val wordCount: Int, val renamed: List<String>) : ImportPreview
    data class Invalid(val issue: ImportIssue) : ImportPreview
}
data class ImportSummary(val dictionaryCount: Int, val wordCount: Int)

/** The transfer format deliberately excludes Room IDs and timestamps. */
class DictionaryTransfer(private val repository: DictionaryRepository, private val copySuffix: (Int) -> String) {
    companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
        const val MAX_DICTIONARIES = 100
        const val MAX_WORDS = 10_000
    }

    suspend fun previewImport(stream: InputStream): ImportPreview {
        return try {
            val bytes = readLimited(stream)
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            val source = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            val root = Json.parseToJsonElement(source).objectAt("file")
            val version = root.intAt("schemaVersion", "file")
            if (version != 1) invalid("schemaVersion", TransferProblem.UNKNOWN_VERSION)
            val rawDictionaries = root.arrayAt("dictionaries", "file")
            if (rawDictionaries.isEmpty()) invalid("dictionaries", TransferProblem.EMPTY)
            if (rawDictionaries.size > MAX_DICTIONARIES) invalid("dictionaries", TransferProblem.TOO_MANY_DICTIONARIES)
            val current = Language.entries.associateWith { language ->
                repository.observeDictionaries(language).first().map { key(it.dictionary.name) }.toMutableSet()
            }
            val defaults = mutableSetOf<Language>()
            val copies = mutableListOf<String>()
            var count = 0
            val parsed = rawDictionaries.mapIndexed { index, raw ->
                val location = "dictionaries[${index + 1}]"
                val item = raw.objectAt(location)
                val language = when (item.stringAt("language", location)) {
                    "de" -> Language.DE
                    "en" -> Language.EN
                    else -> invalid("$location.language", TransferProblem.INVALID_LANGUAGE)
                }
                val originalName = item.stringAt("name", location).trim()
                if (originalName.length !in 1..60) invalid("$location.name", TransferProblem.INVALID_NAME)
                val isDefault = item.booleanAt("isDefault", location)
                if (isDefault && !defaults.add(language)) invalid("$location.isDefault", TransferProblem.MULTIPLE_DEFAULTS)
                val rawWords = item.arrayAt("words", location)
                count += rawWords.size
                if (count > MAX_WORDS) invalid("$location.words", TransferProblem.TOO_MANY_WORDS)
                val words = rawWords.mapIndexed { wordIndex, word ->
                    val path = "$location.words[${wordIndex + 1}]"
                    val fields = word.objectAt(path)
                    val ukrainian = fields.stringAt("ukrainian", path).trim()
                    val first = fields.stringAt("translation1", path).trim()
                    val second = fields.optionalStringAt("translation2", path)?.trim()
                    val example = fields.stringAt("example", path).trim()
                    if (ukrainian.length !in 1..120) invalid("$path.ukrainian", TransferProblem.INVALID_UKRAINIAN)
                    if (first.length !in 1..120) invalid("$path.translation1", TransferProblem.INVALID_TRANSLATION1)
                    if (second != null && (second.length !in 1..120 || key(second) == key(first)))
                        invalid("$path.translation2", TransferProblem.INVALID_TRANSLATION2)
                    if (example.length !in 1..500) invalid("$path.example", TransferProblem.INVALID_EXAMPLE)
                    WordContent(ukrainian, first, second, example)
                }
                val names = current.getValue(language)
                val name = resolveName(originalName, names)
                if (name != originalName) copies += "$originalName → $name"
                names += key(name)
                ImportDictionary(language, name, words, isDefault)
            }
            ImportPreview.Valid(parsed, count, copies)
        } catch (error: InvalidFile) {
            ImportPreview.Invalid(ImportIssue(error.location, error.problem))
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            ImportPreview.Invalid(ImportIssue("file", if (error is kotlinx.serialization.SerializationException || error is java.nio.charset.CharacterCodingException) TransferProblem.INVALID_JSON else TransferProblem.FILE_READ))
        }
    }

    /** Repository imports the entire batch in one Room transaction. */
    suspend fun commitImport(preview: ImportPreview.Valid): ImportSummary {
        repository.importDictionaries(preview.dictionaries)
        return ImportSummary(preview.dictionaries.size, preview.wordCount)
    }

    suspend fun export(ids: List<String>?, stream: OutputStream) {
        if (ids != null && (ids.isEmpty() || ids.distinct().size != ids.size)) throw TransferException(TransferProblem.NO_SELECTION)
        val snapshot = repository.getTransferSnapshot(ids)
        if (snapshot.isEmpty()) throw TransferException(TransferProblem.NO_SELECTION)
        if (snapshot.size > MAX_DICTIONARIES) throw TransferException(TransferProblem.TOO_MANY_DICTIONARIES)
        var wordCount = 0
        val dictionaries = snapshot.map { item ->
            val dictionary = item.dictionary
            val words = item.words
            wordCount += words.size
            if (wordCount > MAX_WORDS) throw TransferException(TransferProblem.TOO_MANY_WORDS)
            buildJsonObject {
                put("language", dictionary.language.code)
                put("name", dictionary.name)
                put("isDefault", item.isDefault)
                put("words", buildJsonArray {
                    words.forEach { word ->
                        add(buildJsonObject {
                            put("ukrainian", word.content.ukrainian)
                            put("translation1", word.content.translation1)
                            put("translation2", word.content.translation2?.let(::JsonPrimitive) ?: JsonNull)
                            put("example", word.content.example)
                        })
                    }
                })
            }
        }
        val json = buildJsonObject { put("schemaVersion", 1); put("dictionaries", JsonArray(dictionaries)) }
        val bytes = Json.encodeToString(JsonElement.serializer(), json).toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MAX_BYTES) throw TransferException(TransferProblem.TOO_LARGE)
        stream.write(bytes)
        stream.flush()
    }

    private fun readLimited(stream: InputStream): ByteArray {
        val result = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val length = stream.read(chunk)
            if (length < 0) break
            if (result.size() + length > MAX_BYTES) invalid("file", TransferProblem.TOO_LARGE)
            result.write(chunk, 0, length)
        }
        return result.toByteArray()
    }

    private fun resolveName(name: String, used: Set<String>): String {
        if (key(name) !in used) return name
        for (number in 2..10_000) {
            val suffix = copySuffix(number)
            if (suffix.length !in 1..59) invalid("name", TransferProblem.TOO_MANY_COPIES)
            val candidate = name.take(60 - suffix.length).trimEnd() + suffix
            if (key(candidate) !in used) return candidate
        }
        invalid("name", TransferProblem.TOO_MANY_COPIES)
    }

    private fun key(text: String) = Normalizer.normalize(text.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)
    private fun invalid(location: String, problem: TransferProblem): Nothing = throw InvalidFile(location, problem)
    private class InvalidFile(val location: String, val problem: TransferProblem) : IllegalArgumentException(problem.name)
    private fun JsonElement.objectAt(location: String): JsonObject = this as? JsonObject ?: invalid(location, TransferProblem.INVALID_TYPE)
    private fun JsonObject.arrayAt(field: String, location: String): JsonArray = this[field] as? JsonArray ?: invalid("$location.$field", TransferProblem.INVALID_TYPE)
    private fun JsonObject.stringAt(field: String, location: String): String = (this[field] as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?: invalid("$location.$field", TransferProblem.INVALID_TYPE)
    private fun JsonObject.optionalStringAt(field: String, location: String): String? {
        val value = this[field] ?: return null
        if (value is JsonNull) return null
        return (value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: invalid("$location.$field", TransferProblem.INVALID_TYPE)
    }
    private fun JsonObject.booleanAt(field: String, location: String): Boolean {
        val value = this[field] ?: return false
        return (value as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: invalid("$location.$field", TransferProblem.INVALID_TYPE)
    }
    private fun JsonObject.intAt(field: String, location: String): Int = (this[field] as? JsonPrimitive)
        ?.takeUnless { it.isString }?.intOrNull ?: invalid("$location.$field", TransferProblem.INVALID_TYPE)
}
