package com.learneverywhere.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.learneverywhere.app.R

@Composable fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.padding(24.dp)) {
        Text(stringResource(R.string.shell_settings))
        TextButton(onClick = onBack) { Text(stringResource(R.string.nav_back)) }
    }
}
