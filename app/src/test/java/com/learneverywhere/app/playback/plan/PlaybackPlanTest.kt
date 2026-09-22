package com.learneverywhere.app.playback.plan

import com.learneverywhere.app.data.Language
import com.learneverywhere.app.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlaybackPlanTest {
    private val wordA = PlaybackWord("a", Language.DE, "дім", "Haus", "Zuhause", "Das ist ein Haus.")
    private val wordB = PlaybackWord("b", Language.DE, "кіт", "Katze", null, "Die Katze schläft.")

    @Test fun `standard single translation has only specified gaps`() {
        val events = PlaybackPlan.create(listOf(wordB), AppSettings())

        assertEquals(listOf(
            Speak("кіт", SpeechLanguage.UK, "b"),
            Silence(3_000, "b"),
            Speak("Katze", SpeechLanguage.DE, "b"),
            Silence(2_000, "b"),
            Speak("Katze", SpeechLanguage.DE, "b"),
        ), events)
    }

    @Test fun `repeat boundaries and example omit extra repeat pauses`() {
        val settings = AppSettings(
            ukrainianRepeats = 2, ukrainianRepeatPauseSeconds = 1,
            beforeTranslationSeconds = 3, translationRepeats = 2,
            translationRepeatPauseSeconds = 4, beforeExampleSeconds = 5,
            afterWordSeconds = 6, includeExample = true,
        )
        val events = PlaybackPlan.create(listOf(wordA), settings)

        assertEquals(listOf(
            Speak("дім", SpeechLanguage.UK, "a"), Silence(1_000, "a"), Speak("дім", SpeechLanguage.UK, "a"),
            Silence(3_000, "a"),
            Speak("Haus", SpeechLanguage.DE, "a"), Silence(4_000, "a"), Speak("Zuhause", SpeechLanguage.DE, "a"),
            Silence(4_000, "a"),
            Speak("Haus", SpeechLanguage.DE, "a"), Silence(4_000, "a"), Speak("Zuhause", SpeechLanguage.DE, "a"),
            Silence(5_000, "a"), Speak("Das ist ein Haus.", SpeechLanguage.DE, "a"),
        ), events)
    }

    @Test fun `non loop omits final after-word silence while loop includes it`() {
        val base = AppSettings(afterWordSeconds = 6)
        assertEquals(Speak("Katze", SpeechLanguage.DE, "b"), PlaybackPlan.create(listOf(wordB), base).last())
        assertEquals(Silence(6_000, "b"), PlaybackPlan.create(listOf(wordB), base.copy(loop = true)).last())
    }

    @Test fun `shuffle is a seeded permutation and avoids loop boundary duplicate`() {
        val words = listOf(wordA, wordB, wordA.copy(id = "c", ukrainian = "сад"))
        val settings = AppSettings(shuffle = true, loop = true)
        val first = PlaybackPlan.order(words, settings, randomSeed = 17, previousWordId = "a")
        val again = PlaybackPlan.order(words, settings, randomSeed = 17, previousWordId = "a")

        assertEquals(first, again)
        assertEquals(setOf("a", "b", "c"), first.map { it.id }.toSet())
        assertNotEquals("a", first.first().id)
    }
}
