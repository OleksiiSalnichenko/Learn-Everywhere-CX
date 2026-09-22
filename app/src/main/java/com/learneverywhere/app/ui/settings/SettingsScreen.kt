package com.learneverywhere.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.learneverywhere.app.R
import com.learneverywhere.app.data.Language
import com.learneverywhere.app.settings.*

private data class Choice<T>(val value: T, @StringRes val label: Int)

@Composable fun SettingsScreen(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { Text(stringResource(R.string.settings_general), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp)) }
        item { ChoiceRow(R.string.settings_main_language, mainLanguageChoices(), settings.mainLanguage) { value -> onChange { it.copy(mainLanguage = value) } } }
        item { ChoiceRow(R.string.settings_interface_language, interfaceLanguageChoices(), settings.interfaceLanguage) { value -> onChange { it.copy(interfaceLanguage = value) } } }
        item { ChoiceRow(R.string.settings_theme, themeChoices(), settings.themeMode) { value -> onChange { it.copy(themeMode = value) } } }
        item { HorizontalDivider(Modifier.padding(vertical = 12.dp)) }
        item { Text(stringResource(R.string.settings_playback), style = MaterialTheme.typography.titleLarge) }
        item { Text(stringResource(R.string.settings_next_session_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { ToggleRow(R.string.settings_loop, settings.loop) { value -> onChange { it.copy(loop = value) } } }
        item { ToggleRow(R.string.settings_shuffle, settings.shuffle) { value -> onChange { it.copy(shuffle = value) } } }
        item { NumberRow(R.string.settings_ukrainian_repeats, R.string.settings_times, settings.ukrainianRepeats) { value -> onChange { it.copy(ukrainianRepeats = value) } } }
        item { NumberRow(R.string.settings_ukrainian_repeat_pause, R.string.settings_seconds, settings.ukrainianRepeatPauseSeconds) { value -> onChange { it.copy(ukrainianRepeatPauseSeconds = value) } } }
        item { NumberRow(R.string.settings_before_translation, R.string.settings_seconds, settings.beforeTranslationSeconds) { value -> onChange { it.copy(beforeTranslationSeconds = value) } } }
        item { NumberRow(R.string.settings_translation_repeats, R.string.settings_times, settings.translationRepeats) { value -> onChange { it.copy(translationRepeats = value) } } }
        item { NumberRow(R.string.settings_translation_repeat_pause, R.string.settings_seconds, settings.translationRepeatPauseSeconds) { value -> onChange { it.copy(translationRepeatPauseSeconds = value) } } }
        item { NumberRow(R.string.settings_before_example, R.string.settings_seconds, settings.beforeExampleSeconds) { value -> onChange { it.copy(beforeExampleSeconds = value) } } }
        item { NumberRow(R.string.settings_after_word, R.string.settings_seconds, settings.afterWordSeconds) { value -> onChange { it.copy(afterWordSeconds = value) } } }
        item { ToggleRow(R.string.settings_include_example, settings.includeExample) { value -> onChange { it.copy(includeExample = value) } } }
        item { ToggleRow(R.string.settings_show_card, settings.showCard) { value -> onChange { it.copy(showCard = value) } } }
        item { TextButton(onClick = onBack, modifier = Modifier.padding(vertical = 16.dp)) { Text(stringResource(R.string.nav_back)) } }
    }
}

@Composable private fun NumberRow(@StringRes label: Int, @StringRes unit: Int, selected: Int, onSelect: (Int) -> Unit) =
    ChoiceRow(label, (1..6).map { Choice(it, unit) }, selected, valueLabel = { value, labelId -> stringResource(labelId, value) }, onSelect = onSelect)

@Composable private fun ToggleRow(@StringRes label: Int, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Checkbox(checked, onChecked)
    }
}

@Composable private fun <T> ChoiceRow(@StringRes label: Int, choices: List<Choice<T>>, selected: T,
    valueLabel: @Composable (T, Int) -> String = { _, labelId -> stringResource(labelId) }, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = choices.first { it.value == selected }
    Row(Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(stringResource(label), style = MaterialTheme.typography.bodyLarge); Text(valueLabel(current.value, current.label), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(stringResource(label)) },
        text = { Column { choices.forEach { choice -> TextButton(onClick = { onSelect(choice.value); open = false }, modifier = Modifier.fillMaxWidth()) { Text(valueLabel(choice.value, choice.label)) } } } },
        confirmButton = {}, dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.settings_cancel)) } })
}

private fun mainLanguageChoices(): List<Choice<Language?>> = listOf(Choice(null, R.string.settings_none), Choice(Language.DE, R.string.settings_german), Choice(Language.EN, R.string.settings_english))
private fun interfaceLanguageChoices() = listOf(Choice(InterfaceLanguage.ENGLISH, R.string.settings_english), Choice(InterfaceLanguage.UKRAINIAN, R.string.settings_ukrainian), Choice(InterfaceLanguage.GERMAN, R.string.settings_german))
private fun themeChoices() = listOf(Choice(ThemeMode.SYSTEM, R.string.settings_theme_system), Choice(ThemeMode.LIGHT, R.string.settings_theme_light), Choice(ThemeMode.DARK, R.string.settings_theme_dark))
