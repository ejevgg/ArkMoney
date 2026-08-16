package com.arkulz.arkmoney

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, LIGHT, DARK;
    companion object { fun from(value: String?) = entries.firstOrNull { it.name == value } ?: SYSTEM }
}

@Composable
fun ArkMoneyTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFFA4D5AA), secondary = Color(0xFFB8CCB9), tertiary = Color(0xFFFFB77C))
        else -> lightColorScheme(primary = Color(0xFF315C3B), secondary = Color(0xFF536A57), tertiary = Color(0xFF8B5000))
    }
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        @Suppress("DEPRECATION")
        window.navigationBarColor = scheme.surfaceContainer.toArgb()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
