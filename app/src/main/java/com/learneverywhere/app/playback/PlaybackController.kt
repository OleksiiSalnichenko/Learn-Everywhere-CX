package com.learneverywhere.app.playback

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.StateFlow

class PlaybackController(private val context: Context) {
    fun observeState(): StateFlow<PlaybackUiState> = PlaybackStateStore.state

    fun play(defaultDictionaryId: String) {
        val intent = Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_PLAY_DICTIONARY)
            .putExtra(PlaybackService.EXTRA_DICTIONARY_ID, defaultDictionaryId)
        ContextCompat.startForegroundService(context, intent)
    }

    fun retry() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_RETRY),
        )
    }

    fun pause() = send(PlaybackService.ACTION_PAUSE)
    fun resume() = send(PlaybackService.ACTION_RESUME)

    fun stop() = send(PlaybackService.ACTION_STOP)

    private fun send(action: String) {
        context.startService(Intent(context, PlaybackService::class.java).setAction(action))
    }
}
