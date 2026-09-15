package com.learneverywhere.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = AppContainer.repository(applicationContext)
        setContent {
            val mainLanguage by AppContainer.mainLanguage.collectAsStateWithLifecycle()
            LearnEverywhereTheme { LearnEverywhereApp(repository, mainLanguage) }
        }
    }
}
object AppContainer {
    // Settings will supply its persisted value here; null represents None.
    private val selectedLanguage = MutableStateFlow<Language?>(Language.DE)
    val mainLanguage = selectedLanguage.asStateFlow()
    fun setMainLanguage(language: Language?) { selectedLanguage.value = language }
    @Volatile private var dictionaries: DictionaryRepository? = null
    fun repository(context: android.content.Context): DictionaryRepository = dictionaries ?: synchronized(this) {
        dictionaries ?: RoomDictionaryRepository(DictionaryDatabase.open(context)).also { dictionaries = it }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LearnEverywhereApp(repository: DictionaryRepository, mainLanguage: Language? = Language.DE) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val playbackMessage = stringResource(R.string.shell_playback)
    LaunchedEffect(repository) { repository.repairDefaults() }
    BackHandler(settings || tab != 0) { if (settings) settings = false else tab = 0 }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(if (settings) R.string.nav_settings else R.string.app_name)) }, actions = {
            if (!settings) IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Settings, stringResource(R.string.nav_settings)) }
        })
    }, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        NavigationBar {
            NavigationBarItem(selected = tab == 0 && !settings, onClick = { tab = 0; settings = false }, icon = { Icon(Icons.Outlined.Home, stringResource(R.string.nav_home)) })
            NavigationBarItem(selected = tab == 1 && !settings, onClick = { tab = 1; settings = false }, icon = { Icon(Icons.Outlined.LibraryBooks, stringResource(R.string.nav_library)) })
        }
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (settings) SettingsScreen { settings = false }
            else if (tab == 0) HomeScreen(repository, mainLanguage, automaticDictionaryName = { language -> context.getString(if (language == Language.DE) R.string.automatic_german_dictionary else R.string.automatic_english_dictionary) })
            else LibraryScreen(repository, mainLanguage = mainLanguage, onPlay = { scope.launch { snackbar.showSnackbar(playbackMessage) } })
        }
    }
}
