package com.learneverywhere.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.learneverywhere.app.data.*
import com.learneverywhere.app.ui.home.HomeScreen
import com.learneverywhere.app.ui.library.LibraryScreen
import com.learneverywhere.app.ui.settings.SettingsScreen
import com.learneverywhere.app.ui.theme.LearnEverywhereTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import com.learneverywhere.app.settings.*
import com.learneverywhere.app.playback.PlaybackController
import com.learneverywhere.app.ui.player.PlayerCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = AppContainer.repository(applicationContext)
        val settingsRepository = AppContainer.settings(applicationContext)
        setContent {
            val persistedSettings by produceState<AppSettings?>(null, settingsRepository) {
                settingsRepository.observeSettings().collect { value = it }
            }
            persistedSettings?.let { settings ->
                LaunchedEffect(settings.interfaceLanguage) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(settings.interfaceLanguage.languageTag))
                }
                LaunchedEffect(settings.mainLanguage) { AppContainer.setMainLanguage(settings.mainLanguage) }
                LearnEverywhereTheme(settings.themeMode) { LearnEverywhereApp(repository, settingsRepository, settings) }
            }
        }
    }
}
object AppContainer {
    // Settings will supply its persisted value here; null represents None.
    private val selectedLanguage = MutableStateFlow<Language?>(Language.DE)
    val mainLanguage = selectedLanguage.asStateFlow()
    fun setMainLanguage(language: Language?) { selectedLanguage.value = language }
    @Volatile private var dictionaries: DictionaryRepository? = null
    @Volatile private var appSettings: SettingsRepository? = null
    fun repository(context: android.content.Context): DictionaryRepository = dictionaries ?: synchronized(this) {
        dictionaries ?: RoomDictionaryRepository(DictionaryDatabase.open(context)).also { dictionaries = it }
    }
    fun settings(context: android.content.Context): SettingsRepository = appSettings ?: synchronized(this) {
        appSettings ?: settingsRepository(context).also { appSettings = it }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LearnEverywhereApp(repository: DictionaryRepository, settingsRepository: SettingsRepository, appSettings: AppSettings) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settingsSaveError = stringResource(R.string.settings_save_error)
    val automaticDictionaryNames = mapOf(
        Language.DE to stringResource(R.string.automatic_german_dictionary),
        Language.EN to stringResource(R.string.automatic_english_dictionary),
    )
    val playbackController = remember(context) { PlaybackController(context.applicationContext) }
    val playbackState by playbackController.observeState().collectAsStateWithLifecycle()
    LaunchedEffect(repository) { repository.repairDefaults() }
    BackHandler(settings || tab != 0) { if (settings) settings = false else tab = 0 }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(if (settings) R.string.nav_settings else R.string.app_name)) }, actions = {
            if (!settings) IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Settings, stringResource(R.string.nav_settings)) }
        })
    }, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        NavigationBar {
            NavigationBarItem(selected = tab == 0 && !settings, onClick = { tab = 0; settings = false }, icon = { Icon(Icons.Outlined.Home, stringResource(R.string.nav_home)) })
            NavigationBarItem(selected = tab == 1 && !settings, onClick = { tab = 1; settings = false }, icon = { Icon(Icons.AutoMirrored.Outlined.LibraryBooks, stringResource(R.string.nav_library)) })
        }
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (settings) SettingsScreen(appSettings, onChange = { change -> scope.launch {
                if (settingsRepository.update(change) is SaveSettingsResult.Failure) snackbar.showSnackbar(settingsSaveError)
            } }) { settings = false }
            else if (tab == 0) HomeScreen(repository, appSettings.mainLanguage, automaticDictionaryName = { language -> automaticDictionaryNames.getValue(language) })
            else LibraryScreen(repository, mainLanguage = appSettings.mainLanguage, onPlay = playbackController::play, onDeleteDictionary = { playbackController.stop() })
            PlayerCard(
                state = playbackState,
                showCard = appSettings.showCard,
                onPause = playbackController::pause,
                onResume = playbackController::resume,
                onStop = playbackController::stop,
                onRetry = playbackController::retry,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
            )
        }
    }
}
