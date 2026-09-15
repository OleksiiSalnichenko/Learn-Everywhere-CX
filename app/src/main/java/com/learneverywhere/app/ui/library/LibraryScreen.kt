package com.learneverywhere.app.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learneverywhere.app.R
import com.learneverywhere.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Playback always receives the current default, even while another dictionary is open. */
@Composable fun LibraryScreen(repository: DictionaryRepository, mainLanguage: Language? = Language.DE, onPlay: (String) -> Unit = {}) {
    val languages = if (mainLanguage == Language.EN) listOf(Language.EN, Language.DE) else listOf(Language.DE, Language.EN)
    var language by rememberSaveable(mainLanguage) { mutableStateOf(languages.first()) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val dictionaries by remember(repository, language) { repository.observeDictionaries(language) }.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    BackHandler(selectedId != null) { selectedId = null }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        PrimaryTabRow(selectedTabIndex = languages.indexOf(language)) {
            languages.forEach { item ->
                val description = stringResource(if (item == Language.DE) R.string.library_german else R.string.library_english)
                Tab(selected = language == item, onClick = { language = item; selectedId = null }, modifier = Modifier.semantics { contentDescription = description }, text = { Text(if (item == Language.DE) "🇩🇪" else "🇬🇧", style = MaterialTheme.typography.headlineSmall) })
            }
        }
        Spacer(Modifier.height(20.dp))
        dictionaries.firstOrNull { it.isDefault }?.let { current ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.library_default), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(current.dictionary.name, style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.library_words, current.wordCount))
                    }
                    FilledIconButton(onClick = { onPlay(current.dictionary.id) }, enabled = current.wordCount > 0, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Outlined.PlayArrow, stringResource(R.string.library_play, current.dictionary.name))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        val selected = dictionaries.firstOrNull { it.dictionary.id == selectedId }
        if (selected != null) {
            TextButton(onClick = { selectedId = null }) { Icon(Icons.Outlined.ArrowBack, null); Text(stringResource(R.string.nav_back)) }
            Text(selected.dictionary.name, style = MaterialTheme.typography.headlineMedium)
            var visibleWordCount by rememberSaveable(selectedId) { mutableIntStateOf(100) }
            val words by remember(repository, selectedId, visibleWordCount) { repository.observeWords(selected.dictionary.id, limit = visibleWordCount) }.collectAsStateWithLifecycle(emptyList())
            if (words.isEmpty()) Text(stringResource(R.string.library_details_empty), Modifier.padding(vertical = 24.dp))
            LazyColumn { items(words, key = { it.id }) { word ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { Column(Modifier.padding(16.dp)) {
                    Text(word.content.ukrainian, style = MaterialTheme.typography.titleLarge)
                    Text("1. ${word.content.translation1}")
                    word.content.translation2?.let { Text("2. $it") }
                    Text(word.content.example, Modifier.padding(top = 12.dp))
                } }
            }
                if (words.size < selected.wordCount) item {
                    TextButton(onClick = { visibleWordCount = (visibleWordCount.toLong() + 100).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() }) { Text(stringResource(R.string.library_load_more)) }
                }
            }
        } else {
            if (dictionaries.isEmpty()) {
                Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 32.dp))
                Text(stringResource(R.string.library_empty_hint), Modifier.padding(vertical = 12.dp))
            }
            Button(onClick = { creating = true; error = false }, modifier = Modifier.padding(vertical = 12.dp)) { Icon(Icons.Outlined.Add, null); Text(stringResource(R.string.library_create)) }
            if (error && !creating) Text(stringResource(R.string.library_error), color = MaterialTheme.colorScheme.error)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(dictionaries, key = { it.dictionary.id }) { item ->
                    OutlinedCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { selectedId = item.dictionary.id }.padding(18.dp)) {
                                Text(item.dictionary.name, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.library_words, item.wordCount))
                            }
                            val description = stringResource(R.string.library_make_default, item.dictionary.name)
                            RadioButton(selected = item.isDefault, onClick = { scope.launch {
                                try { repository.setDefault(item.dictionary.id); error = false }
                                catch (cancelled: CancellationException) { throw cancelled }
                                catch (_: Exception) { error = true }
                            } }, modifier = Modifier.padding(end = 8.dp).semantics { contentDescription = description })
                        }
                    }
                }
            }
        }
    }
    if (creating) AlertDialog(onDismissRequest = { if (!saving) creating = false }, title = { Text(stringResource(R.string.library_create)) }, text = {
        Column {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.library_name)) }, singleLine = true, enabled = !saving)
            if (error) Text(stringResource(R.string.library_error), color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton(enabled = !saving && name.trim().length in 1..60, onClick = { scope.launch {
        saving = true
        try { repository.create(language, name); creating = false; name = ""; error = false }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = true }
        finally { saving = false }
    } }) { Text(stringResource(R.string.library_save)) } }, dismissButton = { TextButton(enabled = !saving, onClick = { creating = false }) { Text(stringResource(R.string.library_cancel)) } })
}
