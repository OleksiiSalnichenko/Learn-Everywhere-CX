package com.learneverywhere.app.ui.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learneverywhere.app.R
import com.learneverywhere.app.data.*
import com.learneverywhere.app.transfer.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private enum class LibraryDialog { CREATE, RENAME, DELETE_DICTIONARY, DELETE_WORD, EDIT_WORD }

/** The list and detail view share the same language tab and stay on this screen. */
@Composable fun LibraryScreen(repository: DictionaryRepository, mainLanguage: Language? = Language.DE,
    onPlay: (String) -> Unit = {}, onDeleteDictionary: suspend (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transfer = remember(repository, context) { DictionaryTransfer(repository) { number -> context.getString(R.string.library_import_suffix, number) } }
    val exportStore = remember(context) { PendingExportStore(context.filesDir.resolve("pending-exports")) }
    val languages = if (mainLanguage == Language.EN) listOf(Language.EN, Language.DE) else listOf(Language.DE, Language.EN)
    var language by rememberSaveable(mainLanguage) { mutableStateOf(languages.first()) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedWordId by rememberSaveable(selectedId) { mutableStateOf<String?>(null) }
    var visibleWordCount by rememberSaveable(selectedId) { mutableIntStateOf(100) }
    var dialog by remember { mutableStateOf<LibraryDialog?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var edit by remember { mutableStateOf(WordContent("", "", null, "")) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var importPreview by remember { mutableStateOf<ImportPreview.Valid?>(null) }
    var pendingExportName by rememberSaveable { mutableStateOf<String?>(null) }
    val dictionaries by remember(repository, language) { repository.observeDictionaries(language) }.collectAsStateWithLifecycle(emptyList())
    val selected = dictionaries.firstOrNull { it.dictionary.id == selectedId }
    val words by remember(repository, selectedId, visibleWordCount) {
        selectedId?.let { repository.observeWords(it, visibleWordCount) } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(emptyList())
    val selectedWord = words.firstOrNull { it.id == selectedWordId }
    val default = dictionaries.firstOrNull { it.isDefault }
    val generalError = stringResource(R.string.library_operation_error)
    val invalidName = stringResource(R.string.library_invalid_name)
    val duplicateName = stringResource(R.string.library_duplicate_name)
    val invalidWord = stringResource(R.string.library_invalid_word)
    fun reason(exception: Exception): String = when ((exception as? RepositoryException)?.reason) {
        RepositoryException.Reason.INVALID_NAME -> invalidName
        RepositoryException.Reason.DUPLICATE_NAME -> duplicateName
        RepositoryException.Reason.INVALID_WORD -> invalidWord
        RepositoryException.Reason.MISSING_DICTIONARY -> context.getString(R.string.library_json_missing_dictionary)
        else -> (exception as? TransferException)?.let { context.getString(transferMessage(it.problem)) } ?: generalError
    }
    fun work(action: suspend () -> Unit) {
        scope.launch {
            busy = true
            try { error = null; action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (exception: Exception) { error = reason(exception) }
            finally { busy = false }
        }
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) work {
            val result = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { transfer.previewImport(it) }
                    ?: ImportPreview.Invalid(ImportIssue("file", TransferProblem.FILE_READ))
            }
            when (result) {
                is ImportPreview.Valid -> importPreview = result
                is ImportPreview.Invalid -> error = context.getString(R.string.library_issue_at, result.issue.location, context.getString(transferMessage(result.issue.problem)))
            }
        }
    }
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        val name = pendingExportName
        pendingExportName = null
        when {
            uri == null -> if (name != null) scope.launch(Dispatchers.IO) { exportStore.delete(name) }
            name == null -> error = context.getString(R.string.library_export_missing)
            else -> work {
                try {
                    withContext(Dispatchers.IO) {
                        val prepared = exportStore.open(name) ?: throw TransferException(TransferProblem.MISSING_PENDING)
                        prepared.use { input ->
                            context.contentResolver.openOutputStream(uri, "wt")?.use { output -> input.copyTo(output); output.flush() }
                                ?: throw TransferException(TransferProblem.FILE_READ)
                        }
                    }
                } finally { withContext(Dispatchers.IO) { exportStore.delete(name) } }
            }
        }
    }
    fun startExport(ids: List<String>?, fileName: String) = work {
        val name = withContext(Dispatchers.IO) {
            val output = ByteArrayOutputStream()
            transfer.export(ids, output)
            exportStore.prepare(output.toByteArray())
        }
        pendingExportName = name
        try { createFile.launch(fileName) }
        catch (problem: Exception) {
            pendingExportName = null
            withContext(Dispatchers.IO) { exportStore.delete(name) }
            throw problem
        }
    }
    BackHandler(selectedId != null || dialog != null || importPreview != null) {
        when {
            importPreview != null -> importPreview = null
            dialog != null -> dialog = null
            else -> { selectedId = null; selectedWordId = null }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        PrimaryTabRow(selectedTabIndex = languages.indexOf(language)) {
            languages.forEach { item ->
                val description = stringResource(if (item == Language.DE) R.string.library_german else R.string.library_english)
                Tab(selected = language == item, onClick = { language = item; selectedId = null; selectedWordId = null },
                    modifier = Modifier.semantics { contentDescription = description },
                    text = { Text(if (item == Language.DE) "🇩🇪" else "🇬🇧", style = MaterialTheme.typography.headlineSmall) })
            }
        }
        Spacer(Modifier.height(16.dp))
        default?.let { current ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val openDescription = stringResource(R.string.library_open_default, current.dictionary.name)
                    Column(
                        Modifier.weight(1f)
                            .clickable { selectedId = current.dictionary.id; selectedWordId = null }
                            .semantics { contentDescription = openDescription }
                    ) {
                        Text(stringResource(R.string.library_default), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(current.dictionary.name, style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.library_words, current.wordCount))
                    }
                    FilledIconButton(onClick = { onPlay(current.dictionary.id) }, enabled = current.wordCount > 0, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Outlined.PlayArrow, stringResource(R.string.library_play, current.dictionary.name))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (selected != null) {
            val selectedWordCount = selected.wordCount
            TextButton(onClick = { selectedId = null; selectedWordId = null }) { Icon(Icons.Outlined.ArrowBack, null); Text(stringResource(R.string.nav_back)) }
            Text(selected.dictionary.name, style = MaterialTheme.typography.headlineSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LibraryAction(Icons.Outlined.DeleteForever, R.string.library_delete_dictionary, !busy) { dialog = LibraryDialog.DELETE_DICTIONARY }
                LibraryAction(Icons.Outlined.Edit, R.string.library_rename, !busy) { name = selected.dictionary.name; dialog = LibraryDialog.RENAME }
                LibraryAction(Icons.Outlined.Delete, R.string.library_delete_selected, selectedWord != null && !busy) { dialog = LibraryDialog.DELETE_WORD }
                LibraryAction(Icons.Outlined.Create, R.string.library_edit_selected, selectedWord != null && !busy) {
                    selectedWord?.let { edit = it.content; dialog = LibraryDialog.EDIT_WORD }
                }
            }
            TextButton(onClick = {
                startExport(listOf(selected.dictionary.id), "dictionary.json")
            }, enabled = !busy) {
                Icon(Icons.Outlined.UploadFile, null); Text(stringResource(R.string.library_export_one))
            }
            if (words.isEmpty()) Text(stringResource(R.string.library_details_empty), Modifier.padding(vertical = 24.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(words, key = { it.id }) { word ->
                    val chosen = selectedWordId == word.id
                    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .semantics { this.selected = chosen }
                        .clickable { selectedWordId = if (chosen) null else word.id },
                        colors = CardDefaults.cardColors(containerColor = if (chosen) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(word.content.ukrainian, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                if (chosen) Icon(Icons.Outlined.CheckCircle, stringResource(R.string.library_word_selected))
                            }
                            Text(stringResource(R.string.library_translation_numbered, 1, word.content.translation1))
                            word.content.translation2?.let { Text(stringResource(R.string.library_translation_numbered, 2, it)) }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Text(word.content.example, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (words.size < selectedWordCount) item {
                    TextButton(onClick = { visibleWordCount = (visibleWordCount.toLong() + 100).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() }) {
                        Text(stringResource(R.string.library_load_more))
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { name = ""; dialog = LibraryDialog.CREATE }, enabled = !busy) { Icon(Icons.Outlined.Add, null); Text(stringResource(R.string.library_create)) }
                LibraryAction(Icons.Outlined.Download, R.string.library_import, !busy) { openFile.launch(arrayOf("application/json", "text/plain", "*/*")) }
                LibraryAction(Icons.Outlined.UploadFile, R.string.library_export_all, !busy) {
                    startExport(null, "learn-everywhere.json")
                }
            }
            if (dictionaries.isEmpty()) {
                Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 24.dp))
                Text(stringResource(R.string.library_empty_hint), Modifier.padding(vertical = 12.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(dictionaries.filterNot { it.isDefault }, key = { it.dictionary.id }) { item ->
                    OutlinedCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { selectedId = item.dictionary.id }.padding(16.dp)) {
                                Text(item.dictionary.name, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.library_words, item.wordCount))
                            }
                            val description = stringResource(R.string.library_make_default, item.dictionary.name)
                            RadioButton(selected = item.isDefault, onClick = { work { repository.setDefault(item.dictionary.id) } },
                                modifier = Modifier.padding(end = 8.dp).semantics { contentDescription = description })
                        }
                    }
                }
            }
        }
    }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text(stringResource(R.string.library_error_title)) },
        text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text(stringResource(R.string.library_ok)) } }) }
    when (dialog) {
        LibraryDialog.CREATE, LibraryDialog.RENAME -> {
            val creating = dialog == LibraryDialog.CREATE
            AlertDialog(onDismissRequest = { if (!busy) dialog = null }, title = { Text(stringResource(if (creating) R.string.library_create else R.string.library_rename)) },
                text = { OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.library_name)) }, singleLine = true, enabled = !busy) },
                confirmButton = { TextButton(enabled = !busy && name.trim().length in 1..60, onClick = {
                    work { if (creating) repository.create(language, name) else selected?.let { repository.rename(it.dictionary.id, name) }; dialog = null }
                }) { Text(stringResource(R.string.library_ok)) } },
                dismissButton = { TextButton(enabled = !busy, onClick = { dialog = null }) { Text(stringResource(R.string.library_cancel)) } })
        }
        LibraryDialog.DELETE_DICTIONARY -> selected?.let { item ->
            AlertDialog(onDismissRequest = { if (!busy) dialog = null }, title = { Text(stringResource(R.string.library_delete_dictionary)) },
                text = { Text(stringResource(R.string.library_confirm_dictionary, item.dictionary.name, item.wordCount)) },
                confirmButton = { TextButton(enabled = !busy, onClick = { work {
                    onDeleteDictionary(item.dictionary.id); repository.deleteDictionary(item.dictionary.id)
                    selectedId = null; selectedWordId = null; dialog = null
                } }) { Text(stringResource(R.string.library_delete)) } },
                dismissButton = { TextButton(enabled = !busy, onClick = { dialog = null }) { Text(stringResource(R.string.library_cancel)) } })
        }
        LibraryDialog.DELETE_WORD -> selectedWord?.let { word ->
            AlertDialog(onDismissRequest = { if (!busy) dialog = null }, title = { Text(stringResource(R.string.library_delete_selected)) },
                text = { Text(stringResource(R.string.library_confirm_word, word.content.ukrainian)) },
                confirmButton = { TextButton(enabled = !busy, onClick = { work { repository.deleteWord(word.id); selectedWordId = null; dialog = null } }) {
                    Text(stringResource(R.string.library_delete)) } },
                dismissButton = { TextButton(enabled = !busy, onClick = { dialog = null }) { Text(stringResource(R.string.library_cancel)) } })
        }
        LibraryDialog.EDIT_WORD -> selectedWord?.let { word ->
            AlertDialog(onDismissRequest = { if (!busy) dialog = null }, title = { Text(stringResource(R.string.library_edit_selected)) },
                text = { Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(edit.ukrainian, { edit = edit.copy(ukrainian = it) }, label = { Text(stringResource(R.string.library_ukrainian)) }, enabled = !busy)
                    OutlinedTextField(edit.translation1, { edit = edit.copy(translation1 = it) }, label = { Text(stringResource(R.string.library_translation1)) }, enabled = !busy)
                    OutlinedTextField(edit.translation2.orEmpty(), { edit = edit.copy(translation2 = it) }, label = { Text(stringResource(R.string.library_translation2)) }, enabled = !busy)
                    OutlinedTextField(edit.example, { edit = edit.copy(example = it) }, label = { Text(stringResource(R.string.library_example)) }, enabled = !busy)
                } }, confirmButton = { TextButton(enabled = !busy && validWord(edit), onClick = { work { repository.updateWord(word.id, edit); dialog = null } }) {
                    Text(stringResource(R.string.library_ok)) } },
                dismissButton = { TextButton(enabled = !busy, onClick = { dialog = null }) { Text(stringResource(R.string.library_cancel)) } })
        }
        null -> Unit
    }
    importPreview?.let { preview -> AlertDialog(onDismissRequest = { if (!busy) importPreview = null }, title = { Text(stringResource(R.string.library_import_preview)) },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.library_import_count, preview.dictionaries.size, preview.wordCount))
            if (preview.renamed.isNotEmpty()) { Text(stringResource(R.string.library_import_copies), style = MaterialTheme.typography.titleSmall); preview.renamed.forEach { Text(it) } }
        } }, confirmButton = { TextButton(enabled = !busy, onClick = { work { transfer.commitImport(preview); importPreview = null } }) {
            Text(stringResource(R.string.library_import_confirm)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { importPreview = null }) { Text(stringResource(R.string.library_cancel)) } }) }
}

private fun validWord(word: WordContent): Boolean {
    val first = word.translation1.trim()
    val second = word.translation2?.trim().orEmpty()
    return word.ukrainian.trim().length in 1..120 && first.length in 1..120 &&
        (second.isEmpty() || (second.length <= 120 && !second.equals(first, ignoreCase = true))) && word.example.trim().length in 1..500
}

private fun transferMessage(problem: TransferProblem): Int = when (problem) {
    TransferProblem.INVALID_JSON -> R.string.library_json_invalid
    TransferProblem.INVALID_TYPE -> R.string.library_json_type
    TransferProblem.UNKNOWN_VERSION -> R.string.library_json_version
    TransferProblem.EMPTY -> R.string.library_json_empty
    TransferProblem.TOO_LARGE -> R.string.library_json_bytes
    TransferProblem.TOO_MANY_DICTIONARIES -> R.string.library_json_dictionaries
    TransferProblem.TOO_MANY_WORDS -> R.string.library_json_words
    TransferProblem.INVALID_LANGUAGE -> R.string.library_json_language
    TransferProblem.INVALID_NAME -> R.string.library_invalid_name
    TransferProblem.MULTIPLE_DEFAULTS -> R.string.library_json_defaults
    TransferProblem.INVALID_UKRAINIAN, TransferProblem.INVALID_TRANSLATION1, TransferProblem.INVALID_TRANSLATION2 -> R.string.library_json_translation
    TransferProblem.INVALID_EXAMPLE -> R.string.library_json_example
    TransferProblem.TOO_MANY_COPIES -> R.string.library_json_copies_limit
    TransferProblem.FILE_READ -> R.string.library_json_io
    TransferProblem.MISSING_DICTIONARY -> R.string.library_json_missing_dictionary
    TransferProblem.NO_SELECTION -> R.string.library_json_selection
    TransferProblem.MISSING_PENDING -> R.string.library_export_missing
}

@Composable private fun LibraryAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int, enabled: Boolean, action: () -> Unit) {
    IconButton(onClick = action, enabled = enabled) { Icon(icon, stringResource(label)) }
}
