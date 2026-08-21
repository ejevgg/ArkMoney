package com.arkulz.arkmoney

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

enum class AppLanguage { SYSTEM, RUSSIAN, ENGLISH;
    fun effective(): AppLanguage = when (this) {
        SYSTEM -> if (Locale.getDefault().language.equals("ru", true)) RUSSIAN else ENGLISH
        else -> this
    }
    companion object { fun from(value: String?) = entries.firstOrNull { it.name == value } ?: SYSTEM }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.SYSTEM }

@Composable
fun tr(russian: String, english: String): String = if (LocalAppLanguage.current.effective() == AppLanguage.RUSSIAN) russian else english

fun AppLanguage.locale(): Locale = if (effective() == AppLanguage.RUSSIAN) Locale.forLanguageTag("ru-RU") else Locale.ENGLISH
