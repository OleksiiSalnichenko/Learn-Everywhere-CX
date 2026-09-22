package com.learneverywhere.app.ui.home

import com.learneverywhere.app.data.Language
import com.learneverywhere.app.data.WordContent
import com.learneverywhere.app.intake.WordDraft
import com.learneverywhere.app.translation.SourceLanguage
import kotlinx.serialization.json.*

// Save only the review draft so rotation restores the dialog without repeating a network call.
internal fun encodeDraft(draft: WordDraft): String = buildJsonObject {
    put("id", draft.id); put("input", draft.input); put("source", draft.source.code)
    put("language", draft.language.code); put("ukrainian", draft.content.ukrainian)
    put("translation1", draft.content.translation1)
    draft.content.translation2?.let { put("translation2", it) }
    put("example", draft.content.example)
    draft.expectedDictionaryId?.let { put("dictionaryId", it) }
    draft.dictionaryName?.let { put("dictionaryName", it) }
}.toString()

internal fun decodeDraft(value: String): WordDraft? = try {
    if (value.isBlank()) null else {
        val obj = Json.parseToJsonElement(value).jsonObject
        fun field(name: String) = obj[name]!!.jsonPrimitive.content
        WordDraft(field("id"), field("input"), SourceLanguage.entries.first { it.code == field("source") },
            Language.entries.first { it.code == field("language") },
            WordContent(field("ukrainian"), field("translation1"), obj["translation2"]?.jsonPrimitive?.content, field("example")),
            obj["dictionaryId"]?.jsonPrimitive?.content, obj["dictionaryName"]?.jsonPrimitive?.content)
    }
} catch (_: Exception) { null }
