package com.learneverywhere.app.playback

import androidx.media3.common.Player

enum class PlaybackControlFailure {
    AUDIO_FOCUS_DENIED,
    CONTROLLER_CONNECTION_FAILED,
    SERVICE_START_FAILED,
}

internal interface PlaybackRuntimeActions {
    fun requestAppend()
    fun pruneConsumed(currentMediaItemIndex: Int): Set<String>
    fun seekTo(mediaItemIndex: Int)
    fun preparePlayer()
    fun play(): Boolean
    fun completeLesson()
    fun protectCache(paths: Set<String>)
    fun stopFailedPlayback()
    fun publishError(state: PlaybackUiState.Error)
    fun settleForeground()
}

/**
 * Owns the service's queue-boundary transitions. Cache protection keeps queued and
 * in-flight files as separate leases until a prepared batch is handed to ExoPlayer.
 */
internal class PlaybackRuntimeCoordinator(
    private val actions: PlaybackRuntimeActions,
) {
    private var dictionaryId: String? = null
    private var queuedPaths: Set<String> = emptySet()
    private var inFlightPaths: Set<String> = emptySet()
    private var appendInFlight = false
    private var resumeAfterAppend = false

    fun beginSession(dictionaryId: String) {
        this.dictionaryId = dictionaryId
        queuedPaths = emptySet()
        inFlightPaths = emptySet()
        appendInFlight = false
        resumeAfterAppend = false
        publishProtection()
    }

    fun beginAppend(): Boolean {
        if (appendInFlight) return false
        appendInFlight = true
        inFlightPaths = emptySet()
        publishProtection()
        return true
    }

    fun onPreparedPaths(paths: Set<String>) {
        check(appendInFlight)
        inFlightPaths = paths.toSet()
        publishProtection()
    }

    fun onBatchReady(
        firstAppendedMediaItemIndex: Int,
        queuedPaths: Set<String>,
        startPlayback: Boolean,
    ) {
        check(appendInFlight)
        this.queuedPaths = queuedPaths.toSet()
        inFlightPaths = emptySet()
        appendInFlight = false
        val shouldStart = startPlayback || resumeAfterAppend
        resumeAfterAppend = false
        publishProtection()
        if (shouldStart) {
            actions.seekTo(firstAppendedMediaItemIndex)
            actions.preparePlayer()
            if (actions.play()) actions.settleForeground()
        }
    }

    fun onBatchFailed(error: PlaybackError) {
        appendInFlight = false
        inFlightPaths = emptySet()
        fail(error)
    }

    fun cancelAppend() {
        if (!appendInFlight) return
        appendInFlight = false
        inFlightPaths = emptySet()
        resumeAfterAppend = false
        publishProtection()
    }

    fun onMediaItemTransition(currentMediaItemIndex: Int) {
        queuedPaths = actions.pruneConsumed(currentMediaItemIndex)
        publishProtection()
    }

    fun onPlaybackStateChanged(playbackState: Int, hasRemainingEvents: Boolean) {
        if (playbackState != Player.STATE_ENDED) return
        if (!hasRemainingEvents) {
            actions.completeLesson()
        } else if (appendInFlight) {
            resumeAfterAppend = true
        } else {
            actions.requestAppend()
        }
    }

    fun resume() {
        actions.play()
    }

    fun onControlFailure(failure: PlaybackControlFailure) {
        fail(
            error = PlaybackError.SYNTHESIS_FAILED,
            controlFailure = failure,
        )
    }

    fun fail(
        error: PlaybackError,
        controlFailure: PlaybackControlFailure? = null,
    ) {
        queuedPaths = emptySet()
        inFlightPaths = emptySet()
        appendInFlight = false
        resumeAfterAppend = false
        publishProtection()
        actions.stopFailedPlayback()
        actions.publishError(PlaybackUiState.Error(error, dictionaryId, controlFailure))
        actions.settleForeground()
    }

    fun release() {
        dictionaryId = null
        queuedPaths = emptySet()
        inFlightPaths = emptySet()
        appendInFlight = false
        resumeAfterAppend = false
        publishProtection()
    }

    private fun publishProtection() {
        actions.protectCache(queuedPaths + inFlightPaths)
    }
}
