package com.learneverywhere.app.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.learneverywhere.app.R
import com.learneverywhere.app.data.DictionaryRepository
import com.learneverywhere.app.data.Language
import com.learneverywhere.app.intake.*
import com.learneverywhere.app.translation.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(repository: DictionaryRepository, mainLanguage: Language?, automaticDictionaryName: (Language) -> String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val provider = remember(context) { GeminiTranslationProvider(context.applicationContext) }
    val intake = remember(repository, provider, automaticDictionaryName) { WordIntake(repository, provider, automaticDictionaryName) }
    var input by rememberSaveable { mutableStateOf("") }
    var previewData by rememberSaveable { mutableStateOf("") }
    val draft = remember(previewData) { decodeDraft(previewData) }
    var picker by rememberSaveable { mutableStateOf("") }
    var voiceLanguage by rememberSaveable { mutableStateOf("auto") }
    var sourceChoiceCodes by rememberSaveable { mutableStateOf("") }
    val sourceChoices = sourceChoiceCodes.split(',').mapNotNull { code -> SourceLanguage.entries.firstOrNull { it.code == code } }.ifEmpty { SourceLanguage.entries }
    var sourceHint by rememberSaveable { mutableStateOf<SourceLanguage?>(null) }
    var targetHint by rememberSaveable { mutableStateOf<Language?>(null) }
    var busy by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var duplicate by rememberSaveable { mutableStateOf(false) }
    var destinationChanged by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<TranslationException.Reason?>(null) }
    var saveError by rememberSaveable { mutableStateOf(false) }
    var speechMessage by remember { mutableIntStateOf(0) }
    var listening by remember { mutableStateOf(false) }
    var speechSessionActive by remember { mutableStateOf(false) }
    var cancelledSpeech by remember { mutableStateOf(false) }
    var recognized by remember { mutableStateOf("") }
    val speech = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    fun prepare() {
        if (busy || saving) return
        busy = true; error = null; saveError = false; picker = ""
        scope.launch {
            try {
                when (val result = intake.prepare(input, sourceHint, targetHint, mainLanguage)) {
                    is PrepareResult.NeedSource -> {
                        sourceChoiceCodes = result.candidates.joinToString(",") { it.code }
                        picker = "source"
                    }
                    is PrepareResult.NeedTarget -> picker = "target"
                    is PrepareResult.Draft -> { previewData = encodeDraft(result.draft); duplicate = false; destinationChanged = false }
                    is PrepareResult.Duplicate -> { previewData = encodeDraft(result.draft); duplicate = true; destinationChanged = false }
                    is PrepareResult.Failure -> error = result.reason
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = TranslationException.Reason.UNAVAILABLE
            } finally {
                busy = false
            }
        }
    }

    fun confirm() {
        val current = draft ?: return
        if (saving) return
        saving = true; saveError = false
        scope.launch {
            try {
                when (val result = intake.confirm(current)) {
                    is ConfirmResult.Saved -> {
                        previewData = ""; input = ""; sourceHint = null; targetHint = null
                        duplicate = false; destinationChanged = false; speechMessage = R.string.home_saved
                    }
                    is ConfirmResult.DestinationChanged -> {
                        previewData = encodeDraft(result.draft); destinationChanged = true
                    }
                    is ConfirmResult.Failure -> saveError = true
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                saveError = true
            } finally {
                saving = false
            }
        }
    }

    DisposableEffect(speech) {
        speech?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { speechSessionActive = true; listening = true }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onError(code: Int) {
                speechSessionActive = false; listening = false
                if (!cancelledSpeech) speechMessage = if (code == SpeechRecognizer.ERROR_NO_MATCH || code == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) R.string.home_speech_none else R.string.home_speech_error
                cancelledSpeech = false
            }
            override fun onResults(results: Bundle?) {
                speechSessionActive = false; listening = false
                if (cancelledSpeech) { cancelledSpeech = false; return }
                val word = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                if (word.isEmpty()) speechMessage = R.string.home_speech_none
                else { recognized = word; speechMessage = 0 }
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose { speech?.cancel(); speech?.destroy() }
    }
    LaunchedEffect(recognized) {
        if (recognized.isNotEmpty()) {
            input = recognized
            sourceHint = SourceLanguage.entries.firstOrNull { it.code == voiceLanguage }
            targetHint = null; recognized = ""
            prepare()
        }
    }
    fun startSpeech() {
        if (speech == null) { speechMessage = R.string.home_speech_unavailable; return }
        try {
            cancelledSpeech = false; speechMessage = 0; speechSessionActive = true; listening = true
            speech.startListening(recognizerIntent(voiceLanguage))
        } catch (_: Exception) {
            speechSessionActive = false; listening = false; speechMessage = R.string.home_speech_unavailable
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startSpeech()
        else speechMessage = R.string.home_speech_permission
    }
    fun microphone() {
        if (speechSessionActive) {
            // stopListening is asynchronous. Keep the session occupied until onResults/onError,
            // so a second recognizer session cannot overlap the first one.
            if (listening) { speech?.stopListening(); listening = false }
            return
        }
        if (speech == null) { speechMessage = R.string.home_speech_unavailable; return }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSpeech()
        } else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceVariant) {
            IconButton(onClick = ::microphone, modifier = Modifier.size(124.dp), enabled = !busy && !saving) {
                Icon(if (speechSessionActive) Icons.Outlined.Stop else Icons.Outlined.Mic,
                    stringResource(if (speechSessionActive) R.string.home_stop else R.string.home_microphone),
                    Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(if (speechSessionActive) R.string.home_listening else R.string.home_hint), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        if (speechSessionActive) {
            ListeningWave()
            TextButton(onClick = {
                cancelledSpeech = true; speech?.cancel(); speechSessionActive = false
                listening = false; speechMessage = 0
            }) { Text(stringResource(R.string.home_cancel)) }
        }
        TextButton(onClick = { picker = "voice" }, enabled = !listening && !busy) {
            val name = SourceLanguage.entries.firstOrNull { it.code == voiceLanguage }?.let { stringResource(sourceString(it)) } ?: stringResource(R.string.home_voice_auto)
            Text(stringResource(R.string.home_voice_language, name))
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it; error = null; speechMessage = 0; sourceHint = null; targetHint = null },
                label = { Text(stringResource(R.string.home_input_hint)) }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { prepare() }),
                modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = ::prepare, enabled = !busy && !saving) { Text(stringResource(R.string.home_submit)) }
        }
        Spacer(Modifier.height(14.dp))
        if (busy) { CircularProgressIndicator(Modifier.size(24.dp)); Text(stringResource(R.string.home_processing)) }
        if (speechMessage != 0) Text(stringResource(speechMessage), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        error?.let { reason ->
            Text(stringResource(errorString(reason)), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            if (canRetryTranslation(reason)) TextButton(onClick = ::prepare) { Text(stringResource(R.string.home_retry)) }
        }
    }
    if (picker == "source") AlertDialog(onDismissRequest = { picker = "" }, title = { Text(stringResource(R.string.home_choose_source)) },
        text = { Column { sourceChoices.forEach { candidate -> TextButton(onClick = { sourceHint = candidate; picker = ""; prepare() }) { Text(stringResource(sourceString(candidate))) } } } },
        confirmButton = {}, dismissButton = { TextButton(onClick = { picker = "" }) { Text(stringResource(R.string.home_cancel)) } })
    if (picker == "voice") AlertDialog(onDismissRequest = { picker = "" }, title = { Text(stringResource(R.string.home_choose_voice_language)) },
        text = { Column {
            TextButton(onClick = { voiceLanguage = "auto"; picker = "" }) { Text(stringResource(R.string.home_voice_auto)) }
            SourceLanguage.entries.forEach { language -> TextButton(onClick = { voiceLanguage = language.code; picker = "" }) { Text(stringResource(sourceString(language))) } }
        } }, confirmButton = {}, dismissButton = { TextButton(onClick = { picker = "" }) { Text(stringResource(R.string.home_cancel)) } })
    if (picker == "target") AlertDialog(onDismissRequest = { picker = "" }, title = { Text(stringResource(R.string.home_choose_target)) },
        text = { Column { Language.entries.forEach { language -> TextButton(onClick = { targetHint = language; picker = ""; prepare() }) { Text(stringResource(if (language == Language.DE) R.string.home_source_de else R.string.home_source_en)) } } } },
        confirmButton = {}, dismissButton = { TextButton(onClick = { picker = "" }) { Text(stringResource(R.string.home_cancel)) } })
    if (draft != null) AlertDialog(onDismissRequest = { if (!saving) previewData = "" }, title = { Text(stringResource(R.string.home_preview)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text("${stringResource(R.string.home_ukrainian)}: ${draft.content.ukrainian}")
                Text("${stringResource(R.string.home_meaning_one)}: ${draft.content.translation1}")
                draft.content.translation2?.let { Text("${stringResource(R.string.home_meaning_two)}: $it") }
                Text("${stringResource(R.string.home_example)}: ${draft.content.example}")
                Text(stringResource(R.string.home_destination, draft.dictionaryName.orEmpty()))
                if (duplicate) Text(stringResource(R.string.home_duplicate, draft.dictionaryName.orEmpty()), color = MaterialTheme.colorScheme.error)
                if (destinationChanged) Text(stringResource(R.string.home_destination_changed, draft.dictionaryName.orEmpty()), color = MaterialTheme.colorScheme.error)
                if (saveError) Text(stringResource(R.string.home_error_save), color = MaterialTheme.colorScheme.error)
            }
        }, confirmButton = { TextButton(onClick = ::confirm, enabled = !saving) { Text(stringResource(if (saving) R.string.home_saving else R.string.home_submit)) } },
        dismissButton = { TextButton(onClick = { previewData = ""; duplicate = false; destinationChanged = false }, enabled = !saving) { Text(stringResource(R.string.home_cancel)) } })
}

@Composable
private fun ListeningWave() {
    val transition = rememberInfiniteTransition(label = "listening wave")
    Row(
        Modifier.height(34.dp).padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            val height by transition.animateFloat(
                initialValue = 7f,
                targetValue = 26f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 520, delayMillis = index * 75),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "listening bar $index",
            )
            Box(
                Modifier.width(5.dp).height(height.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

private fun recognizerIntent(voiceLanguage: String) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    val locale = when (voiceLanguage) { "uk" -> "uk-UA"; "de" -> "de-DE"; "en" -> "en-US"; else -> null }
    if (locale != null) putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
}
private fun sourceString(source: SourceLanguage) = when (source) {
    SourceLanguage.UK -> R.string.home_source_uk
    SourceLanguage.DE -> R.string.home_source_de
    SourceLanguage.EN -> R.string.home_source_en
}
private fun errorString(reason: TranslationException.Reason) = when (reason) {
    TranslationException.Reason.NOT_CONFIGURED -> R.string.home_error_config
    TranslationException.Reason.QUOTA -> R.string.home_error_quota
    TranslationException.Reason.NETWORK -> R.string.home_error_network
    TranslationException.Reason.INVALID_INPUT -> R.string.home_error_input
    TranslationException.Reason.INVALID_CONTENT -> R.string.home_error_invalid
    TranslationException.Reason.UNAVAILABLE -> R.string.home_error_unavailable
}

internal fun canRetryTranslation(reason: TranslationException.Reason): Boolean = when (reason) {
    TranslationException.Reason.INVALID_INPUT, TranslationException.Reason.NOT_CONFIGURED -> false
    TranslationException.Reason.INVALID_CONTENT,
    TranslationException.Reason.QUOTA,
    TranslationException.Reason.NETWORK,
    TranslationException.Reason.UNAVAILABLE -> true
}
