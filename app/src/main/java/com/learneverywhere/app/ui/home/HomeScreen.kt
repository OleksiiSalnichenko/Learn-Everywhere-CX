package com.learneverywhere.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.learneverywhere.app.R
import com.learneverywhere.app.data.DictionaryRepository
import com.learneverywhere.app.data.Language

@Composable fun HomeScreen(repository: DictionaryRepository, mainLanguage: Language?, automaticDictionaryName: (Language) -> String) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(Icons.Outlined.Mic, null, Modifier.size(120.dp).padding(28.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.shell_home), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.shell_translation), style = MaterialTheme.typography.bodyLarge)
    }
}
