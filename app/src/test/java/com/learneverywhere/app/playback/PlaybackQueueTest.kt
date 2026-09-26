package com.learneverywhere.app.playback

import androidx.media3.common.Player
import java.lang.reflect.Proxy
import com.learneverywhere.app.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import com.google.common.util.concurrent.SettableFuture
import com.learneverywhere.app.playback.plan.Silence
import com.learneverywhere.app.playback.plan.Speak
import com.learneverywhere.app.playback.plan.SpeechLanguage

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlaybackQueueTest {
    @Test fun emptyWordsProduceAnActionableEmptyResult() {
        assertEquals(PlaybackQueueResult.Empty, PlaybackQueue.create(emptyList(), AppSettings(), 7L))
    }

    @Test fun preparationStopsAtFailedSynthesisAndPreservesOrder() = runTest {
        val events = listOf(
            Speak("один", SpeechLanguage.UK, "w1"),
            Silence(3000, "w1"),
            Speak("eins", SpeechLanguage.DE, "w1"),
            Speak("sentinel", SpeechLanguage.DE, "w2"),
        )
        val seen = mutableListOf<String>()
        val result = PlaybackBatchPreparer.prepare(events, 0, 8, prepareEvent = { event ->
            val label = if (event is Speak) event.text else "silence"
            seen += label
            if (label == "eins") PreparedMedia.Failure(PreparationProblem.SYNTHESIS_FAILED)
            else PreparedMedia.Ready("/$label.wav")
        })
        assertEquals(listOf("один", "silence", "eins"), seen)
        assertTrue(result is PreparedBatch.Failed)
        assertEquals(2, (result as PreparedBatch.Failed).eventIndex)
        assertEquals(PreparationProblem.SYNTHESIS_FAILED, result.problem)
    }

    @Test fun cancelledAndThrownSynthesisRequestsReleaseCallbackAndTemporaryFile() = runTest {
        val requests = SynthesisRequestRegistry(timeoutMillis = 10_000)
        val cancelledFile = java.nio.file.Files.createTempFile("cancelled-synthesis", ".tmp").toFile()
        var cancelledId = ""
        val job = launch { requests.run(cancelledFile) { id -> cancelledId = id; true } }
        runCurrent()
        job.cancelAndJoin()
        assertFalse(cancelledFile.exists())
        assertFalse(requests.complete(cancelledId, true))

        val thrownFile = java.nio.file.Files.createTempFile("thrown-synthesis", ".tmp").toFile()
        var thrownId = ""
        val failure = runCatching {
            requests.run(thrownFile) { id -> thrownId = id; error("synthesis crashed") }
        }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertFalse(thrownFile.exists())
        assertFalse(requests.complete(thrownId, true))
    }

    @Test fun endedWindowAppendsAndSeeksToFirstNewItemBeforeResuming() {
        val harness = RuntimeHarness()
        val runtime = PlaybackRuntimeCoordinator(harness)
        runtime.beginSession("dictionary")
        harness.installQueued("/old.wav")
        assertTrue(runtime.beginAppend())
        runtime.onPreparedPaths(setOf("/old.wav"))
        runtime.onBatchReady(
            firstAppendedMediaItemIndex = 0,
            queuedPaths = setOf("/old.wav"),
            startPlayback = false,
        )
        harness.transportCalls.clear()

        runtime.onPlaybackStateChanged(Player.STATE_ENDED, hasRemainingEvents = true)

        assertEquals(listOf("append"), harness.transportCalls)
        assertTrue(runtime.beginAppend())
        runtime.onPreparedPaths(setOf("/new-1.wav", "/new-2.wav"))
        runtime.onBatchReady(
            firstAppendedMediaItemIndex = 1,
            queuedPaths = setOf("/old.wav", "/new-1.wav", "/new-2.wav"),
            startPlayback = true,
        )

        assertEquals(listOf("append", "seek:1", "prepare", "play", "settle"), harness.transportCalls)
    }

    @Test fun pruningCannotReleaseInFlightFilesBeforeHandoffOrRelease() {
        val harness = RuntimeHarness()
        val runtime = PlaybackRuntimeCoordinator(harness)
        runtime.beginSession("dictionary")
        harness.installQueued("/queued-1.wav", "/queued-2.wav")
        assertTrue(runtime.beginAppend())
        runtime.onPreparedPaths(setOf("/queued-1.wav", "/queued-2.wav"))
        runtime.onBatchReady(
            firstAppendedMediaItemIndex = 0,
            queuedPaths = setOf("/queued-1.wav", "/queued-2.wav"),
            startPlayback = false,
        )
        assertTrue(runtime.beginAppend())
        runtime.onPreparedPaths(setOf("/in-flight.wav"))
        assertEquals(
            setOf("/queued-1.wav", "/queued-2.wav", "/in-flight.wav"),
            harness.protected.last(),
        )

        runtime.onMediaItemTransition(currentMediaItemIndex = 1)
        assertEquals(setOf("/queued-2.wav", "/in-flight.wav"), harness.protected.last())

        runtime.onBatchReady(
            firstAppendedMediaItemIndex = 1,
            queuedPaths = setOf("/queued-2.wav", "/in-flight.wav"),
            startPlayback = false,
        )
        assertEquals(setOf("/queued-2.wav", "/in-flight.wav"), harness.protected.last())

        runtime.release()
        assertEquals(emptySet<String>(), harness.protected.last())
    }

    @Test fun focusDeniedPublishesTypedFailureWithRetryContextAndSettlesForeground() {
        val harness = RuntimeHarness()
        lateinit var runtime: PlaybackRuntimeCoordinator
        val transport = PlaybackTransport(
            playAction = { false },
            pauseAction = {},
            release = {},
            playRejected = { runtime.onControlFailure(PlaybackControlFailure.AUDIO_FOCUS_DENIED) },
        )
        harness.playAction = transport::play
        runtime = PlaybackRuntimeCoordinator(harness)
        runtime.beginSession("retry-dictionary")

        runtime.resume()

        assertEquals(
            PlaybackUiState.Error(
                PlaybackError.SYNTHESIS_FAILED,
                "retry-dictionary",
                PlaybackControlFailure.AUDIO_FOCUS_DENIED,
            ),
            harness.failure,
        )
        assertEquals(listOf("stop-failed", "settle"), harness.transportCalls)
        assertEquals(emptySet<String>(), harness.protected.last())
    }

    @Test fun controllerConnectionFailureIsTypedAndKeepsDictionaryContext() = runTest {
        var reported: PlaybackUiState.Error? = null
        val connection = ControllerConnection(timeoutMillis = 1) { failure, dictionaryId ->
            reported = PlaybackUiState.Error(PlaybackError.SYNTHESIS_FAILED, dictionaryId, failure)
        }
        val timedOut = SettableFuture.create<String>()
        connection.execute(timedOut, "dictionary", command = {}, release = {})
        assertTrue(timedOut.isCancelled)
        assertEquals(
            PlaybackUiState.Error(
                PlaybackError.SYNTHESIS_FAILED,
                "dictionary",
                PlaybackControlFailure.CONTROLLER_CONNECTION_FAILED,
            ),
            reported,
        )

        val cancelled = SettableFuture.create<String>()
        val cancellableConnection = ControllerConnection(timeoutMillis = 10_000) { _, _ ->
            error("cancelled connection must not report a failure")
        }
        val job = launch { cancellableConnection.execute(cancelled, "dictionary", command = {}, release = {}) }
        runCurrent()
        job.cancelAndJoin()
        assertTrue(cancelled.isCancelled)
    }

    @Test fun stopReleasesResourcesAndCannotAutoResume() {
        val calls = mutableListOf<String>()
        val rawCalls = mutableListOf<String>()
        val rawPlayer = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
        ) { _, method, _ -> rawCalls += method.name; null } as Player
        val transport = PlaybackTransport(
            playAction = { calls += "play"; rawPlayer.play(); true },
            pauseAction = { calls += "pause"; rawPlayer.pause() },
            release = { calls += "release"; rawPlayer.stop() },
        )
        val lockscreenPlayer = TransportPlayer(rawPlayer, transport)
        lockscreenPlayer.play()
        lockscreenPlayer.pause()
        lockscreenPlayer.setPlayWhenReady(true)
        lockscreenPlayer.stop()
        lockscreenPlayer.play()
        lockscreenPlayer.setPlayWhenReady(true)
        lockscreenPlayer.pause()
        lockscreenPlayer.stop()
        assertEquals(listOf("play", "pause", "play", "release"), calls)
        assertEquals(listOf("play", "pause", "play", "stop"), rawCalls)
        assertEquals(TransportState.STOPPED, transport.state)
    }

    private class RuntimeHarness : PlaybackRuntimeActions {
        val transportCalls = mutableListOf<String>()
        val protected = mutableListOf<Set<String>>()
        var failure: PlaybackUiState.Error? = null
        var playAction: () -> Boolean = {
            transportCalls += "play"
            true
        }
        private var queued: Set<String> = emptySet()

        fun installQueued(vararg paths: String) {
            queued = paths.toSet()
        }

        override fun requestAppend() { transportCalls += "append" }
        override fun pruneConsumed(currentMediaItemIndex: Int): Set<String> {
            queued = queued.drop(currentMediaItemIndex.coerceAtLeast(0)).toSet()
            return queued
        }
        override fun seekTo(mediaItemIndex: Int) { transportCalls += "seek:$mediaItemIndex" }
        override fun preparePlayer() { transportCalls += "prepare" }
        override fun play(): Boolean = playAction()
        override fun completeLesson() { transportCalls += "complete" }
        override fun protectCache(paths: Set<String>) { protected += paths }
        override fun stopFailedPlayback() { transportCalls += "stop-failed" }
        override fun publishError(state: PlaybackUiState.Error) { failure = state }
        override fun settleForeground() { transportCalls += "settle" }
    }
}
