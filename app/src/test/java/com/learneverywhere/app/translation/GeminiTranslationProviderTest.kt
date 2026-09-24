package com.learneverywhere.app.translation

import com.learneverywhere.app.data.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeminiTranslationProviderTest {
    @Test fun parsesStrictTypedTranslation() {
        val result = parseTranslation("""
            {
              "sourceCandidates": [{"language":"uk","confidence":0.98}],
              "detectedSource":"uk",
              "targetLanguage":"de",
              "normalizedInput":"дім",
              "ukrainian":"дім",
              "foreignMeanings":["Haus"],
              "example":"Das Haus ist warm."
            }
        """.trimIndent())

        assertEquals(listOf(SourceCandidate(SourceLanguage.UK, 0.98)), result.sourceCandidates)
        assertEquals(SourceLanguage.UK, result.detectedSource)
        assertEquals(Language.DE, result.targetLanguage)
        assertEquals("дім", result.normalizedInput)
        assertEquals("дім", result.ukrainian)
        assertEquals(listOf("Haus"), result.foreignMeanings)
        assertEquals("Das Haus ist warm.", result.example)
    }

    @Test fun rejectsQuotedNumericConfidence() {
        assertInvalid(validJson().replace("\"confidence\":0.98", "\"confidence\":\"0.98\""))
    }

    @Test fun rejectsNumericTextFields() {
        assertInvalid(validJson().replace("\"normalizedInput\":\"house\"", "\"normalizedInput\":42"))
    }

    @Test fun rejectsMalformedJson() {
        assertInvalid("{not-json")
    }

    @Test fun reportsUnsupportedLanguageDistinctly() {
        val problem = assertThrows(TranslationException::class.java) {
            parseTranslation(validJson().replace("\"detectedSource\":\"en\"", "\"detectedSource\":\"unsupported\""))
        }
        assertEquals(TranslationException.Reason.UNSUPPORTED_LANGUAGE, problem.reason)
    }

    private fun assertInvalid(raw: String) {
        val problem = assertThrows(TranslationException::class.java) { parseTranslation(raw) }
        assertEquals(TranslationException.Reason.INVALID_CONTENT, problem.reason)
    }

    private fun validJson() = """
        {
          "sourceCandidates": [{"language":"en","confidence":0.98}],
          "detectedSource":"en",
          "targetLanguage":"en",
          "normalizedInput":"house",
          "ukrainian":"дім",
          "foreignMeanings":["house"],
          "example":"The house is warm."
        }
    """.trimIndent()
}
