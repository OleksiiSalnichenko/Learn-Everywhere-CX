package com.learneverywhere.app.playback

import androidx.media3.common.Player
import java.lang.reflect.Proxy
import com.learneverywhere.app.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest
import com.learneverywhere.app.playback.plan.Silence
import com.learneverywhere.app.playback.plan.Speak
import com.learneverywhere.app.playback.plan.SpeechLanguage

class PlaybackQueueTest {
    @Test fun emptyWordsProduceAnActionableEmptyResult() {
        assertEquals(PlaybackQueueResult.Empty, PlaybackQueue.create(emptyList(), AppSettings(), 7L))
    }

    @Test fun preparationStopsAtFailedSynthesisAndPreservesOrder() = runTest {
        val events = listOf(Speak("один", SpeechLanguage.UK, "w1"), Silence(3000, "w1"), Speak("eins", SpeechLanguage.DE, "w1"))
        val seen = mutableListOf<String>()
        val result = PlaybackBatchPreparer.prepare(events, 0, 8) { event ->
            val label = if (event is Speak) event.text else "silence"
            seen += label
            if (label == "eins") PreparedMedia.Failure(PreparationProblem.SYNTHESIS_FAILED)
            else PreparedMedia.Ready("/$label.wav")
        }
        assertEquals(listOf("один", "silence", "eins"), seen)
        assertTrue(result is PreparedBatch.Failed)
        assertEquals(2, (result as PreparedBatch.Failed).eventIndex)
    }

    @Test fun stopReleasesResourcesAndCannotAutoResume() {
        val calls = mutableListOf<String>()
        val rawCalls = mutableListOf<String>()
        val rawPlayer = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
        ) { _, method, _ -> rawCalls += method.name; null } as Player
        val transport = PlaybackTransport(
            playAction = { calls += "play"; rawPlayer.play() },
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
}
