package com.learneverywhere.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.learneverywhere.app.data.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import kotlinx.coroutines.CancellationException

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun update(settings: AppSettings): SaveSettingsResult
    suspend fun update(transform: (AppSettings) -> AppSettings): SaveSettingsResult
}

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettings> = store.data
        .catch { if (it is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw it }
        .map(::decode)

    override suspend fun update(settings: AppSettings): SaveSettingsResult = save { settings }

    override suspend fun update(transform: (AppSettings) -> AppSettings): SaveSettingsResult = save(transform)

    private suspend fun save(transform: (AppSettings) -> AppSettings): SaveSettingsResult = try {
        store.edit { values -> write(values, transform(decode(values))) }
        SaveSettingsResult.Saved
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        SaveSettingsResult.Failure(error)
    }

    private fun write(values: androidx.datastore.preferences.core.MutablePreferences, settings: AppSettings) {
        values[Keys.mainLanguage] = settings.mainLanguage?.code ?: "none"
        values[Keys.interfaceLanguage] = settings.interfaceLanguage.languageTag
        values[Keys.loop] = settings.loop
        values[Keys.shuffle] = settings.shuffle
        values[Keys.ukrainianRepeats] = settings.ukrainianRepeats
        values[Keys.ukrainianRepeatPause] = settings.ukrainianRepeatPauseSeconds
        values[Keys.beforeTranslation] = settings.beforeTranslationSeconds
        values[Keys.translationRepeats] = settings.translationRepeats
        values[Keys.translationRepeatPause] = settings.translationRepeatPauseSeconds
        values[Keys.beforeExample] = settings.beforeExampleSeconds
        values[Keys.afterWord] = settings.afterWordSeconds
        values[Keys.includeExample] = settings.includeExample
        values[Keys.showCard] = settings.showCard
        values[Keys.themeMode] = settings.themeMode.name.lowercase()
    }

    private fun decode(values: Preferences): AppSettings = AppSettings(
        mainLanguage = when (values[Keys.mainLanguage]) { "none" -> null; "en" -> Language.EN; else -> Language.DE },
        interfaceLanguage = InterfaceLanguage.entries.firstOrNull { it.languageTag == values[Keys.interfaceLanguage] } ?: InterfaceLanguage.ENGLISH,
        loop = values[Keys.loop] ?: false,
        shuffle = values[Keys.shuffle] ?: false,
        ukrainianRepeats = bounded(values[Keys.ukrainianRepeats], 1),
        ukrainianRepeatPauseSeconds = bounded(values[Keys.ukrainianRepeatPause], 2),
        beforeTranslationSeconds = bounded(values[Keys.beforeTranslation], 3),
        translationRepeats = bounded(values[Keys.translationRepeats], 2),
        translationRepeatPauseSeconds = bounded(values[Keys.translationRepeatPause], 2),
        beforeExampleSeconds = bounded(values[Keys.beforeExample], 2),
        afterWordSeconds = bounded(values[Keys.afterWord], 2),
        includeExample = values[Keys.includeExample] ?: false,
        showCard = values[Keys.showCard] ?: true,
        themeMode = ThemeMode.entries.firstOrNull { it.name.equals(values[Keys.themeMode], true) } ?: ThemeMode.SYSTEM,
    )

    private fun bounded(value: Int?, default: Int) = value?.takeIf { it in 1..6 } ?: default

    private object Keys {
        val mainLanguage = stringPreferencesKey("main_language")
        val interfaceLanguage = stringPreferencesKey("interface_language")
        val loop = booleanPreferencesKey("loop")
        val shuffle = booleanPreferencesKey("shuffle")
        val ukrainianRepeats = intPreferencesKey("ukrainian_repeats")
        val ukrainianRepeatPause = intPreferencesKey("ukrainian_repeat_pause_seconds")
        val beforeTranslation = intPreferencesKey("before_translation_seconds")
        val translationRepeats = intPreferencesKey("translation_repeats")
        val translationRepeatPause = intPreferencesKey("translation_repeat_pause_seconds")
        val beforeExample = intPreferencesKey("before_example_seconds")
        val afterWord = intPreferencesKey("after_word_seconds")
        val includeExample = booleanPreferencesKey("include_example")
        val showCard = booleanPreferencesKey("show_card")
        val themeMode = stringPreferencesKey("theme_mode")
    }
}
