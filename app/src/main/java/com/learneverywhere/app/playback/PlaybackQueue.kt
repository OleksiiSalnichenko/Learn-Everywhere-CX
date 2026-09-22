package com.learneverywhere.app.playback

import com.learneverywhere.app.data.WordEntry
import com.learneverywhere.app.playback.plan.PlaybackEvent
import com.learneverywhere.app.playback.plan.PlaybackPlan
import com.learneverywhere.app.playback.plan.PlaybackWord
import com.learneverywhere.app.settings.AppSettings
import com.learneverywhere.app.data.Language

sealed interface PlaybackQueueResult {
    data object Empty : PlaybackQueueResult
    data class Ready(val events: List<PlaybackEvent>, val words: List<PlaybackWord>) : PlaybackQueueResult
}

object PlaybackQueue {
    fun create(words: List<WordEntry>, language: Language, settings: AppSettings, randomSeed: Long): PlaybackQueueResult {
        if (words.isEmpty()) return PlaybackQueueResult.Empty
        val snapshot = words.map { word ->
            PlaybackWord(word.id, language, word.content.ukrainian, word.content.translation1,
                word.content.translation2, word.content.example)
        }
        return PlaybackQueueResult.Ready(PlaybackPlan.create(snapshot, settings, randomSeed), snapshot)
    }

    fun create(words: List<PlaybackWord>, settings: AppSettings, randomSeed: Long): PlaybackQueueResult {
        if (words.isEmpty()) return PlaybackQueueResult.Empty
        return PlaybackQueueResult.Ready(PlaybackPlan.create(words, settings, randomSeed), words.toList())
    }
}
