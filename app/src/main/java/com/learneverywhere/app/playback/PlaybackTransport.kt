package com.learneverywhere.app.playback
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

enum class TransportState { IDLE, PLAYING, PAUSED, STOPPED }

/** Keeps interruption and force-stop behavior independent from Android callbacks. */
class PlaybackTransport(
    private val playAction: () -> Boolean,
    private val pauseAction: () -> Unit,
    private val release: () -> Unit,
    private val playRejected: () -> Unit = {},
) {
    var state: TransportState = TransportState.IDLE
        private set

    fun play(): Boolean {
        if (state == TransportState.STOPPED) return false
        if (state == TransportState.PLAYING) return true
        if (playAction()) {
            state = TransportState.PLAYING
            return true
        }
        playRejected()
        return false
    }

    fun pause() {
        if (state == TransportState.PLAYING) { pauseAction(); state = TransportState.PAUSED }
    }

    fun syncPlayerState(isPlaying: Boolean) {
        if (state == TransportState.STOPPED) return
        state = if (isPlaying) TransportState.PLAYING
        else if (state == TransportState.IDLE) TransportState.IDLE
        else TransportState.PAUSED
    }

    fun stop() {
        if (state == TransportState.STOPPED) return
        state = TransportState.STOPPED
        release()
    }
}

/** Routes MediaSession and lockscreen commands through the same terminal transport state. */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class TransportPlayer(
    player: Player,
    private val transport: PlaybackTransport,
) : ForwardingPlayer(player) {
    override fun play() { transport.play() }
    override fun pause() = transport.pause()
    override fun stop() = transport.stop()
    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) transport.play() else transport.pause()
    }
}
