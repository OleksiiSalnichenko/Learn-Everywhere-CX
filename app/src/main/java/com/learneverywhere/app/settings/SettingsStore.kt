package com.learneverywhere.app.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

fun settingsRepository(context: Context): SettingsRepository =
    DataStoreSettingsRepository(context.applicationContext.settingsDataStore)
