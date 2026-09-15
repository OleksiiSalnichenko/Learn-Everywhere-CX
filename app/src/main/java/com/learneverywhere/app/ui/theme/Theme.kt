package com.learneverywhere.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(primary = Color(0xFF0D7377), onPrimary = Color.White, background = Color(0xFFF7F5EF), onBackground = Color(0xFF172C2D), surface = Color(0xFFF7F5EF), onSurface = Color(0xFF172C2D), surfaceVariant = Color(0xFFE9ECE5), onSurfaceVariant = Color(0xFF405453))
private val Dark = darkColorScheme(primary = Color(0xFF4ECCA3), onPrimary = Color(0xFF102D24), background = Color(0xFF0E1415), onBackground = Color(0xFFE8EFEA), surface = Color(0xFF0E1415), onSurface = Color(0xFFE8EFEA), surfaceVariant = Color(0xFF203030), onSurfaceVariant = Color(0xFFB5CAC2))
@Composable fun LearnEverywhereTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}
