package com.learneverywhere.app.playback

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.learneverywhere.app.playback.plan.PlaybackEvent
import com.learneverywhere.app.playback.plan.Silence
import com.learneverywhere.app.playback.plan.Speak
import com.learneverywhere.app.playback.plan.SpeechLanguage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AudioFileCache(private val directory: File, private val maximumBytes: Long = 100L * 1024 * 1024) {
    @Volatile private var protectedPaths: Set<String> = emptySet()

    init { directory.mkdirs() }

    fun file(key: String): File = File(directory, sha256(key) + ".wav")

    fun get(key: String): File? = file(key).takeIf { it.isFile }?.also { it.setLastModified(System.currentTimeMillis()) }

    fun commit(key: String, temporary: File, protected: Set<File> = emptySet()): File {
        val target = file(key)
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        trim(protected + target)
        return target
    }

    fun protect(files: Set<File>) {
        protectedPaths = files.mapTo(mutableSetOf()) { it.absolutePath }
    }

    private fun trim(protected: Set<File>) {
        val files = directory.listFiles { file -> file.extension == "wav" }?.sortedBy { it.lastModified() }.orEmpty()
        var size = files.sumOf { it.length() }
        for (file in files) if (size > maximumBytes && file !in protected && file.absolutePath !in protectedPaths) {
            val length = file.length()
            if (file.delete()) size -= length
        }
    }

    fun temporaryFile() = File(directory, ".${UUID.randomUUID()}.tmp")

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}

internal class SynthesisRequestRegistry(private val timeoutMillis: Long) {
    private val requests = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    suspend fun run(temporary: File, enqueue: (String) -> Boolean): Boolean {
        val id = UUID.randomUUID().toString()
        val completion = CompletableDeferred<Boolean>()
        requests[id] = completion
        var keepTemporary = false
        try {
            if (!enqueue(id)) return false
            keepTemporary = withTimeoutOrNull(timeoutMillis) { completion.await() } == true
            return keepTemporary
        } finally {
            requests.remove(id)?.cancel()
            if (!keepTemporary) temporary.delete()
        }
    }

    fun complete(id: String, success: Boolean): Boolean = requests[id]?.complete(success) == true

    fun cancelAll() {
        requests.values.forEach { it.cancel() }
        requests.clear()
    }
}

class AndroidAudioPreparer(context: Context, private val cache: AudioFileCache) : AutoCloseable {
    private val mutex = Mutex()
    private val requests = SynthesisRequestRegistry(TTS_TIMEOUT_MS)
    private val ready = CompletableDeferred<Boolean>()
    private val tts = TextToSpeech(context.applicationContext) { status -> ready.complete(status == TextToSpeech.SUCCESS) }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { utteranceId?.let { requests.complete(it, true) } }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { utteranceId?.let { requests.complete(it, false) } }
            override fun onError(utteranceId: String?, errorCode: Int) { utteranceId?.let { requests.complete(it, false) } }
        })
    }

    suspend fun prepare(event: PlaybackEvent): PreparedMedia = mutex.withLock {
        withContext(Dispatchers.IO) {
            when (event) {
                is Silence -> prepareSilence(event.durationMs)
                is Speak -> prepareSpeech(event)
            }
        }
    }

    private suspend fun prepareSpeech(event: Speak): PreparedMedia {
        if (withTimeoutOrNull(TTS_TIMEOUT_MS) { ready.await() } != true) return PreparedMedia.Failure(PreparationProblem.MISSING_VOICE)
        val locale = when (event.language) { SpeechLanguage.UK -> Locale("uk", "UA"); SpeechLanguage.DE -> Locale.GERMANY; SpeechLanguage.EN -> Locale.UK }
        val available = tts.isLanguageAvailable(locale)
        if (available < TextToSpeech.LANG_AVAILABLE || tts.setLanguage(locale) < TextToSpeech.LANG_AVAILABLE)
            return PreparedMedia.Failure(PreparationProblem.MISSING_VOICE)
        val voiceId = tts.voice?.name ?: locale.toLanguageTag()
        val key = "speech|${event.language}|$voiceId|1.0|${event.text}"
        cache.get(key)?.let { return PreparedMedia.Ready(it.absolutePath) }
        val temporary = cache.temporaryFile()
        val success = requests.run(temporary) { utteranceId ->
            tts.synthesizeToFile(event.text, null, temporary, utteranceId) == TextToSpeech.SUCCESS
        }
        if (!success || !temporary.isFile || temporary.length() == 0L) {
            temporary.delete()
            return PreparedMedia.Failure(PreparationProblem.SYNTHESIS_FAILED)
        }
        return try {
            PreparedMedia.Ready(cache.commit(key, temporary).absolutePath)
        } catch (_: Exception) {
            temporary.delete()
            PreparedMedia.Failure(PreparationProblem.STORAGE_FAILED)
        }
    }

    private fun prepareSilence(durationMs: Long): PreparedMedia {
        val key = "silence|$durationMs"
        cache.get(key)?.let { return PreparedMedia.Ready(it.absolutePath) }
        val temporary = cache.temporaryFile()
        return try {
            writeSilentWav(temporary, durationMs)
            PreparedMedia.Ready(cache.commit(key, temporary).absolutePath)
        } catch (_: Exception) {
            temporary.delete(); PreparedMedia.Failure(PreparationProblem.STORAGE_FAILED)
        }
    }

    private fun writeSilentWav(file: File, durationMs: Long) {
        val sampleRate = 8_000
        val dataSize = (durationMs * sampleRate / 1_000 * 2).toInt()
        FileOutputStream(file).use { output ->
            fun little(value: Int, bytes: Int) { repeat(bytes) { output.write(value shr (it * 8) and 0xff) } }
            output.write("RIFF".toByteArray()); little(36 + dataSize, 4); output.write("WAVEfmt ".toByteArray())
            little(16, 4); little(1, 2); little(1, 2); little(sampleRate, 4); little(sampleRate * 2, 4)
            little(2, 2); little(16, 2); output.write("data".toByteArray()); little(dataSize, 4)
            val zeros = ByteArray(8_192); var remaining = dataSize
            while (remaining > 0) { val count = minOf(remaining, zeros.size); output.write(zeros, 0, count); remaining -= count }
            output.fd.sync()
        }
    }

    private companion object { const val TTS_TIMEOUT_MS = 60_000L }

    override fun close() { requests.cancelAll(); tts.stop(); tts.shutdown() }
}
