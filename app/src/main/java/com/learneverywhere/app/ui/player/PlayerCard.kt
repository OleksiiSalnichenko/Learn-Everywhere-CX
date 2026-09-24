package com.learneverywhere.app.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.learneverywhere.app.R
import com.learneverywhere.app.playback.*

@Composable
fun PlayerCard(state: PlaybackUiState, showCard: Boolean, onPause: () -> Unit, onResume: () -> Unit, onStop: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    if (state == PlaybackUiState.Stopped) return
    Card(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), elevation = CardDefaults.cardElevation(6.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state) {
                is PlaybackUiState.Preparing -> Text(stringResource(R.string.playback_preparing), style = MaterialTheme.typography.titleMedium)
                is PlaybackUiState.Active -> {
                    Text(stringResource(R.string.playback_progress, state.index, state.total), style = MaterialTheme.typography.labelMedium)
                    if (showCard) {
                        Text(state.word.ukrainian, style = MaterialTheme.typography.headlineSmall)
                        Text(state.word.translation1)
                        state.word.translation2?.let { Text(it) }
                        Text(state.word.example, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(when (state.phase) {
                            PlaybackPhase.PREPARING -> R.string.playback_preparing
                            PlaybackPhase.UKRAINIAN -> R.string.playback_phase_ukrainian
                            PlaybackPhase.TRANSLATION -> R.string.playback_phase_translation
                            PlaybackPhase.EXAMPLE -> R.string.playback_phase_example
                            PlaybackPhase.SILENCE -> R.string.playback_phase_pause
                        }), color = MaterialTheme.colorScheme.primary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(onClick = if (state.paused) onResume else onPause) {
                            Icon(if (state.paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                stringResource(if (state.paused) R.string.playback_resume else R.string.playback_pause))
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedIconButton(onClick = onStop) { Icon(Icons.Outlined.Stop, stringResource(R.string.playback_stop)) }
                    }
                }
                is PlaybackUiState.Error -> {
                    Text(stringResource(R.string.playback_error_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(stringResource(when (state.message) {
                        PlaybackError.EMPTY_DICTIONARY -> R.string.playback_error_empty
                        PlaybackError.NOT_DEFAULT -> R.string.playback_error_default
                        PlaybackError.MISSING_VOICE -> R.string.playback_error_voice
                        PlaybackError.SYNTHESIS_FAILED -> R.string.playback_error_synthesis
                        PlaybackError.STORAGE_FAILED -> R.string.playback_error_storage
                        PlaybackError.DICTIONARY_MISSING -> R.string.playback_error_missing
                    }))
                    Row { Button(onClick = onRetry, enabled = state.dictionaryId != null) { Text(stringResource(R.string.playback_retry)) }; TextButton(onClick = onStop) { Text(stringResource(R.string.playback_stop)) } }
                }
                PlaybackUiState.Stopped -> Unit
            }
        }
    }
}
