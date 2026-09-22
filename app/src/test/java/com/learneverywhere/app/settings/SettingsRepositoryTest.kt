package com.learneverywhere.app.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.learneverywhere.app.data.Language
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun `empty store exposes documented defaults`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val repository = repository(scope)

        assertEquals(AppSettings(), repository.observeSettings().first())
        scope.cancel()
    }

    @Test fun `every setting survives repository recreation`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val file = temporaryFolder.newFile("settings.preferences_pb")
        val first = DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
        val expected = AppSettings(
            mainLanguage = null, interfaceLanguage = InterfaceLanguage.UKRAINIAN,
            loop = true, shuffle = true, ukrainianRepeats = 6,
            ukrainianRepeatPauseSeconds = 1, beforeTranslationSeconds = 6,
            translationRepeats = 1, translationRepeatPauseSeconds = 6,
            beforeExampleSeconds = 1, afterWordSeconds = 6,
            includeExample = true, showCard = false, themeMode = ThemeMode.DARK,
        )

        assertEquals(SaveSettingsResult.Saved, first.update(expected))
        assertEquals(expected, first.observeSettings().first())
        scope.cancel()

        val secondScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val restored = DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = secondScope) { file })
        assertEquals(expected, restored.observeSettings().first())
        secondScope.cancel()
    }

    @Test fun `concurrent field changes merge against the latest persisted snapshot`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val file = temporaryFolder.newFile("concurrent.preferences_pb")
        val repository = DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })

        val results = listOf(
            async { repository.update { it.copy(loop = true) } },
            async { repository.update { it.copy(shuffle = true) } },
        ).awaitAll()

        assertEquals(listOf(SaveSettingsResult.Saved, SaveSettingsResult.Saved), results)
        scope.cancel()

        val restoredScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val restored = DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = restoredScope) { file })
        assertEquals(AppSettings().copy(loop = true, shuffle = true), restored.observeSettings().first())
        restoredScope.cancel()
    }

    private fun repository(scope: CoroutineScope): SettingsRepository = DataStoreSettingsRepository(
        PreferenceDataStoreFactory.create(scope = scope) { temporaryFolder.newFile("empty.preferences_pb") },
    )
}
