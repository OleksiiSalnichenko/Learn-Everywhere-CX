package com.learneverywhere.app.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

internal class ControllerConnection(
    private val timeoutMillis: Long,
    private val reportFailure: (PlaybackControlFailure, String?) -> Unit,
) {
    suspend fun <T> execute(
        future: ListenableFuture<T>,
        dictionaryId: String?,
        command: (T) -> Unit,
        release: (T) -> Unit,
    ) {
        val controller = try {
            await(future)
        } catch (_: TimeoutCancellationException) {
            reportFailure(PlaybackControlFailure.CONTROLLER_CONNECTION_FAILED, dictionaryId)
            return
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            reportFailure(PlaybackControlFailure.CONTROLLER_CONNECTION_FAILED, dictionaryId)
            return
        }
        try {
            command(controller)
        } catch (_: Exception) {
            reportFailure(PlaybackControlFailure.CONTROLLER_CONNECTION_FAILED, dictionaryId)
        } finally {
            release(controller)
        }
    }

    private suspend fun <T> await(future: ListenableFuture<T>): T = withTimeout(timeoutMillis) {
        suspendCancellableCoroutine { continuation ->
            future.addListener({
                try {
                    continuation.resume(future.get())
                } catch (failure: Exception) {
                    continuation.resumeWithException(failure.cause ?: failure)
                }
            }, DIRECT_EXECUTOR)
            continuation.invokeOnCancellation { future.cancel(true) }
        }
    }

    private companion object {
        val DIRECT_EXECUTOR = Executor { command -> command.run() }
    }
}

class PlaybackController(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connection = ControllerConnection(CONNECTION_TIMEOUT_MS, ::publishControlFailure)

    fun observeState(): StateFlow<PlaybackUiState> = PlaybackStateStore.state

    fun play(defaultDictionaryId: String) {
        startForeground(
            Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_PLAY_DICTIONARY)
                .putExtra(PlaybackService.EXTRA_DICTIONARY_ID, defaultDictionaryId),
            defaultDictionaryId,
        )
    }

    fun retry() = startForeground(
        Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_RETRY),
        currentDictionaryId(),
    )

    fun pause() = withController(MediaController::pause)
    fun resume() = withController(MediaController::play)
    fun stop() = withController(MediaController::stop)

    private fun startForeground(intent: Intent, id: String?) {
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) {
            publishControlFailure(PlaybackControlFailure.SERVICE_START_FAILED, id)
        }
    }

    private fun withController(command: (MediaController) -> Unit) {
        val dictionaryId = currentDictionaryId()
        scope.launch {
            val future = MediaController.Builder(
                context,
                SessionToken(context, ComponentName(context, PlaybackService::class.java)),
            ).buildAsync()
            connection.execute(future, dictionaryId, command, MediaController::release)
        }
    }

    private fun publishControlFailure(failure: PlaybackControlFailure, dictionaryId: String?) {
        PlaybackStateStore.set(
            PlaybackUiState.Error(
                message = PlaybackError.SYNTHESIS_FAILED,
                dictionaryId = dictionaryId,
                controlFailure = failure,
            ),
        )
    }

    private fun currentDictionaryId(): String? = when (val state = PlaybackStateStore.state.value) {
        is PlaybackUiState.Preparing -> state.dictionaryId
        is PlaybackUiState.Active -> state.dictionaryId
        is PlaybackUiState.Error -> state.dictionaryId
        else -> null
    }

    private companion object {
        const val CONNECTION_TIMEOUT_MS = 5_000L
    }
}
