package com.learneverywhere.app.playback

import com.learneverywhere.app.playback.plan.PlaybackEvent

enum class PreparationProblem { MISSING_VOICE, SYNTHESIS_FAILED, STORAGE_FAILED }

sealed interface PreparedMedia {
    data class Ready(val path: String) : PreparedMedia
    data class Failure(val problem: PreparationProblem) : PreparedMedia
}

data class PreparedSegment(val eventIndex: Int, val event: PlaybackEvent, val path: String)

sealed interface PreparedBatch {
    data class Ready(val segments: List<PreparedSegment>, val nextEventIndex: Int) : PreparedBatch
    data class Failed(val eventIndex: Int, val problem: PreparationProblem) : PreparedBatch
}

object PlaybackBatchPreparer {
    suspend fun prepare(
        events: List<PlaybackEvent>,
        startIndex: Int,
        maximumEvents: Int,
        prepareEvent: suspend (PlaybackEvent) -> PreparedMedia,
    ): PreparedBatch {
        require(startIndex in 0..events.size)
        require(maximumEvents > 0)
        val end = (startIndex + maximumEvents).coerceAtMost(events.size)
        val prepared = ArrayList<PreparedSegment>(end - startIndex)
        for (index in startIndex until end) {
            when (val item = prepareEvent(events[index])) {
                is PreparedMedia.Ready -> prepared += PreparedSegment(index, events[index], item.path)
                is PreparedMedia.Failure -> return PreparedBatch.Failed(index, item.problem)
            }
        }
        return PreparedBatch.Ready(prepared, end)
    }
}
