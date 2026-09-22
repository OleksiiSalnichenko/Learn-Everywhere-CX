package com.learneverywhere.app.settings

import com.learneverywhere.app.data.Language

enum class InterfaceLanguage(val languageTag: String) { ENGLISH("en"), UKRAINIAN("uk"), GERMAN("de") }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val mainLanguage: Language? = Language.DE,
    val interfaceLanguage: InterfaceLanguage = InterfaceLanguage.ENGLISH,
    val loop: Boolean = false,
    val shuffle: Boolean = false,
    val ukrainianRepeats: Int = 1,
    val ukrainianRepeatPauseSeconds: Int = 2,
    val beforeTranslationSeconds: Int = 3,
    val translationRepeats: Int = 2,
    val translationRepeatPauseSeconds: Int = 2,
    val beforeExampleSeconds: Int = 2,
    val afterWordSeconds: Int = 2,
    val includeExample: Boolean = false,
    val showCard: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    init {
        require(listOf(ukrainianRepeats, ukrainianRepeatPauseSeconds, beforeTranslationSeconds,
            translationRepeats, translationRepeatPauseSeconds, beforeExampleSeconds,
            afterWordSeconds).all { it in 1..6 })
    }
}

sealed interface SaveSettingsResult {
    data object Saved : SaveSettingsResult
    data class Failure(val cause: Throwable) : SaveSettingsResult
}
