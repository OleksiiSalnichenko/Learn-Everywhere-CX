package com.learneverywhere.app.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.learneverywhere.app.AppContainer
import com.learneverywhere.app.MainActivity
import com.learneverywhere.app.R
import com.learneverywhere.app.playback.plan.PlaybackEvent
import com.learneverywhere.app.playback.plan.Silence
import com.learneverywhere.app.playback.plan.Speak
import com.learneverywhere.app.playback.plan.PlaybackWord
import com.learneverywhere.app.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

enum class PlaybackPhase { PREPARING, UKRAINIAN, TRANSLATION, EXAMPLE, SILENCE }
sealed interface PlaybackUiState {
    data object Stopped : PlaybackUiState
    data class Preparing(val dictionaryId: String) : PlaybackUiState
    data class Active(val word: PlaybackWord, val phase: PlaybackPhase, val index: Int, val total: Int, val paused: Boolean, val showCard: Boolean) : PlaybackUiState
    data class Error(val message: PlaybackError, val dictionaryId: String?) : PlaybackUiState
}
enum class PlaybackError { EMPTY_DICTIONARY, NOT_DEFAULT, MISSING_VOICE, SYNTHESIS_FAILED, STORAGE_FAILED, DICTIONARY_MISSING }

object PlaybackStateStore {
    private val mutable = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Stopped)
    val state: StateFlow<PlaybackUiState> = mutable.asStateFlow()
    internal fun set(value: PlaybackUiState) { mutable.value = value }
}

class PlaybackService : MediaSessionService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession
    private lateinit var preparer: AndroidAudioPreparer
    private lateinit var cache: AudioFileCache
    private lateinit var transport: PlaybackTransport
    private var preparation: Job? = null
    private var events: List<PlaybackEvent> = emptyList()
    private var wordsById: Map<String, PlaybackWord> = emptyMap()
    private var orderedWordIds: List<String> = emptyList()
    private var settings = AppSettings()
    private var dictionaryId: String? = null
    private var nextEventIndex = 0
    private var appending = false
    private var seed = System.currentTimeMillis()
    private var lastWordId: String? = null
    private var destroying = false
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private val focusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener { change -> if (change < AudioManager.AUDIOFOCUS_GAIN) pauseForInterruption() }
            .build()
    }
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { pauseForInterruption() }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), false)
            addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { updateCurrent(mediaItem); maybeAppend() }
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (::transport.isInitialized) transport.syncPlayerState(isPlaying)
                    updatePauseState(!isPlaying)
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) onQueueEnded()
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) { fail(PlaybackError.SYNTHESIS_FAILED) }
            })
        }
        transport = PlaybackTransport(
            playAction = player::play,
            pauseAction = player::pause,
            release = ::onTransportStopped,
        )
        session = MediaSession.Builder(this, TransportPlayer(player, transport)).build()
        cache = AudioFileCache(File(cacheDir, "speech"))
        preparer = AndroidAudioPreparer(this, cache)
        ContextCompat.registerReceiver(this, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = session

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_DICTIONARY -> intent.getStringExtra(EXTRA_DICTIONARY_ID)?.let { startDictionary(it) }
            ACTION_RETRY -> dictionaryId?.let { startDictionary(it) }
            ACTION_PAUSE -> transport.pause()
            ACTION_RESUME -> transport.play()
            ACTION_STOP -> stopSession()
            else -> return super.onStartCommand(intent, flags, startId)
        }
        return START_NOT_STICKY
    }

    private fun startDictionary(id: String) {
        dictionaryId = id
        player.pause()
        player.clearMediaItems()
        cache.protect(emptySet())
        PlaybackStateStore.set(PlaybackUiState.Preparing(id))
        showPreparingNotification()
        preparation?.cancel()
        appending = false
        preparation = scope.launch {
            val repository = AppContainer.repository(applicationContext)
            val dictionary = repository.getDictionary(id)
            if (dictionary == null) return@launch fail(PlaybackError.DICTIONARY_MISSING, id)
            if (repository.getDefault(dictionary.language)?.id != id) return@launch fail(PlaybackError.NOT_DEFAULT, id)
            val wordEntries = repository.getWords(id)
            val settingsSnapshot = AppContainer.settings(applicationContext).observeSettings().first()
            when (val queue = PlaybackQueue.create(wordEntries, dictionary.language, settingsSnapshot, seed)) {
                PlaybackQueueResult.Empty -> fail(PlaybackError.EMPTY_DICTIONARY, id)
                is PlaybackQueueResult.Ready -> {
                    dictionaryId = id; settings = settingsSnapshot; wordsById = queue.words.associateBy { it.id }
                    events = queue.events
                    orderedWordIds = queue.events.map { it.wordId }.distinct()
                    nextEventIndex = 0; player.clearMediaItems(); cache.protect(emptySet())
                    PlaybackStateStore.set(PlaybackUiState.Preparing(id))
                    appendBatch(startPlayback = true)
                }
            }
        }
    }

    private suspend fun appendBatch(startPlayback: Boolean) {
        if (appending || nextEventIndex >= events.size) return
        appending = true
        when (val batch = PlaybackBatchPreparer.prepare(events, nextEventIndex, PREFETCH_EVENTS) { preparer.prepare(it) }) {
            is PreparedBatch.Failed -> { appending = false; fail(batch.problem.toPlaybackError()); return }
            is PreparedBatch.Ready -> {
                val items = batch.segments.map { segment ->
                    MediaItem.Builder().setMediaId(segment.eventIndex.toString()).setUri(Uri.fromFile(File(segment.path)))
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(wordsById[segment.event.wordId]?.ukrainian).build()).build()
                }
                player.addMediaItems(items)
                cache.protect((0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).localConfiguration?.uri?.path?.let(::File) }.toSet())
                nextEventIndex = batch.nextEventIndex
                appending = false
                if (startPlayback) {
                    player.prepare()
                    if (audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) transport.play()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        }
    }

    private fun maybeAppend() {
        if (player.mediaItemCount - player.currentMediaItemIndex <= 4 && nextEventIndex < events.size && !appending)
            preparation = scope.launch { appendBatch(startPlayback = false) }
    }

    private fun updateCurrent(item: MediaItem?) {
        val index = item?.mediaId?.toIntOrNull() ?: return
        val event = events.getOrNull(index) ?: return
        lastWordId = event.wordId
        val word = wordsById[event.wordId] ?: return
        val phase = when (event) {
            is Silence -> PlaybackPhase.SILENCE
            is Speak -> when {
                event.text == word.ukrainian -> PlaybackPhase.UKRAINIAN
                event.text == word.example && settings.includeExample -> PlaybackPhase.EXAMPLE
                else -> PlaybackPhase.TRANSLATION
            }
        }
        PlaybackStateStore.set(PlaybackUiState.Active(word, phase, orderedWordIds.indexOf(word.id) + 1,
            orderedWordIds.size, !player.isPlaying, settings.showCard))
    }

    private fun updatePauseState(paused: Boolean) {
        val current = PlaybackStateStore.state.value
        if (current is PlaybackUiState.Active) PlaybackStateStore.set(current.copy(paused = paused))
    }

    private fun pauseForInterruption() { if (::transport.isInitialized) transport.pause() }

    private fun onQueueEnded() {
        if (!settings.loop) { stopSession(); return }
        val snapshot = wordsById.values.toList()
        seed++
        when (val queue = PlaybackQueue.create(snapshot, settings, seed)) {
            PlaybackQueueResult.Empty -> fail(PlaybackError.EMPTY_DICTIONARY)
            is PlaybackQueueResult.Ready -> {
                events = com.learneverywhere.app.playback.plan.PlaybackPlan.create(snapshot, settings, seed, lastWordId)
                orderedWordIds = events.map { it.wordId }.distinct(); nextEventIndex = 0; player.clearMediaItems(); cache.protect(emptySet())
                preparation = scope.launch { appendBatch(startPlayback = true) }
            }
        }
    }

    private fun fail(error: PlaybackError, id: String? = dictionaryId) {
        transport.pause(); player.clearMediaItems(); cache.protect(emptySet())
        audioManager.abandonAudioFocusRequest(focusRequest)
        PlaybackStateStore.set(PlaybackUiState.Error(error, id)); stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun stopSession() {
        if (::transport.isInitialized) transport.stop()
    }

    private fun onTransportStopped() {
        stopPlaybackResources()
        events = emptyList(); wordsById = emptyMap(); dictionaryId = null
        PlaybackStateStore.set(PlaybackUiState.Stopped)
        if (!destroying) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    }

    private fun stopPlaybackResources() {
        preparation?.cancel(); preparation = null; appending = false
        if (::cache.isInitialized) cache.protect(emptySet())
        if (::player.isInitialized) { player.stop(); player.clearMediaItems() }
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    private fun destroyPlaybackResources() {
        if (::transport.isInitialized) transport.stop() else stopPlaybackResources()
        if (::session.isInitialized) session.release()
        if (::player.isInitialized) player.release()
        if (::preparer.isInitialized) preparer.close()
    }

    private fun showPreparingNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.playback_channel), NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_book_sound)
            .setContentTitle(getString(R.string.playback_preparing)).setContentIntent(open).setOngoing(true).build()
        startForeground(PREPARING_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        destroying = true
        unregisterReceiver(noisyReceiver)
        scope.cancel()
        destroyPlaybackResources()
        PlaybackStateStore.set(PlaybackUiState.Stopped)
        super.onDestroy()
    }

    private fun PreparationProblem.toPlaybackError() = when (this) {
        PreparationProblem.MISSING_VOICE -> PlaybackError.MISSING_VOICE
        PreparationProblem.SYNTHESIS_FAILED -> PlaybackError.SYNTHESIS_FAILED
        PreparationProblem.STORAGE_FAILED -> PlaybackError.STORAGE_FAILED
    }

    companion object {
        const val ACTION_PLAY_DICTIONARY = "com.learneverywhere.app.PLAY_DICTIONARY"
        const val ACTION_RETRY = "com.learneverywhere.app.RETRY_PLAYBACK"
        const val ACTION_PAUSE = "com.learneverywhere.app.PAUSE_PLAYBACK"
        const val ACTION_RESUME = "com.learneverywhere.app.RESUME_PLAYBACK"
        const val ACTION_STOP = "com.learneverywhere.app.STOP_PLAYBACK"
        const val EXTRA_DICTIONARY_ID = "dictionary_id"
        private const val PREFETCH_EVENTS = 12
        private const val CHANNEL_ID = "playback"
        private const val PREPARING_NOTIFICATION_ID = 1042
    }
}
