package com.learneverywhere.app.playback.plan

import com.learneverywhere.app.data.Language
import com.learneverywhere.app.settings.AppSettings
import kotlin.random.Random

enum class SpeechLanguage { UK, DE, EN }
enum class PlaybackEventPhase { UKRAINIAN, TRANSLATION, EXAMPLE, SILENCE }

sealed interface PlaybackEvent {
    val wordId: String
    val phase: PlaybackEventPhase
}
data class Speak(
    val text: String,
    val language: SpeechLanguage,
    override val wordId: String,
    override val phase: PlaybackEventPhase = if (language == SpeechLanguage.UK) PlaybackEventPhase.UKRAINIAN else PlaybackEventPhase.TRANSLATION,
) : PlaybackEvent
data class Silence(val durationMs: Long, override val wordId: String) : PlaybackEvent {
    override val phase = PlaybackEventPhase.SILENCE
}

data class PlaybackWord(
    val id: String,
    val language: Language,
    val ukrainian: String,
    val translation1: String,
    val translation2: String?,
    val example: String,
)

object PlaybackPlan {
    fun create(
        words: List<PlaybackWord>,
        settings: AppSettings,
        randomSeed: Long = 0,
        previousWordId: String? = null,
    ): List<PlaybackEvent> {
        val ordered = order(words, settings, randomSeed, previousWordId)
        return buildList {
            ordered.forEachIndexed { index, word ->
                addWord(word, settings)
                if (index < ordered.lastIndex || settings.loop) add(Silence(settings.afterWordSeconds * 1_000L, word.id))
            }
        }
    }

    fun order(words: List<PlaybackWord>, settings: AppSettings, randomSeed: Long = 0, previousWordId: String? = null): List<PlaybackWord> {
        if (!settings.shuffle || words.size < 2) return words.toList()
        val shuffled = words.shuffled(Random(randomSeed)).toMutableList()
        if (settings.loop && shuffled.first().id == previousWordId) {
            val swap = shuffled.indexOfFirst { it.id != previousWordId }
            if (swap > 0) { val first = shuffled[0]; shuffled[0] = shuffled[swap]; shuffled[swap] = first }
        }
        return shuffled
    }

    private fun MutableList<PlaybackEvent>.addWord(word: PlaybackWord, settings: AppSettings) {
        val learningLanguage = if (word.language == Language.DE) SpeechLanguage.DE else SpeechLanguage.EN
        repeat(settings.ukrainianRepeats) { repeatIndex ->
            add(Speak(word.ukrainian, SpeechLanguage.UK, word.id))
            if (repeatIndex < settings.ukrainianRepeats - 1) add(Silence(settings.ukrainianRepeatPauseSeconds * 1_000L, word.id))
        }
        add(Silence(settings.beforeTranslationSeconds * 1_000L, word.id))
        val translations = listOfNotNull(word.translation1, word.translation2?.takeIf { it.isNotBlank() })
        repeat(settings.translationRepeats) { cycle ->
            translations.forEachIndexed { translationIndex, text ->
                add(Speak(text, learningLanguage, word.id))
                val anotherTranslation = translationIndex < translations.lastIndex
                val anotherCycle = cycle < settings.translationRepeats - 1
                if (anotherTranslation || anotherCycle) add(Silence(settings.translationRepeatPauseSeconds * 1_000L, word.id))
            }
        }
        if (settings.includeExample) {
            add(Silence(settings.beforeExampleSeconds * 1_000L, word.id))
            add(Speak(word.example, learningLanguage, word.id, PlaybackEventPhase.EXAMPLE))
        }
    }
}
