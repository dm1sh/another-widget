package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.tommasoberlose.anotherwidget.R
import java.util.Locale

object LanguageHelper {

    const val SYSTEM_DEFAULT_TAG = ""

    val SUPPORTED_LANGUAGE_TAGS: List<String> = listOf(
        "en",
        "da",
        "de",
        "es",
        "fr",
        "id",
        "it",
        "pl",
        "pt",
        "ru",
        "sk",
        "uk",
        "zh-CN"
    )

    fun getCurrentLanguageTag(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (appLocales.isEmpty) return SYSTEM_DEFAULT_TAG
        val current = appLocales[0] ?: return SYSTEM_DEFAULT_TAG
        val tag = current.toLanguageTag()
        // Normalize legacy "in" to "id" and regional Chinese variants
        return when {
            tag.equals("in", ignoreCase = true) || tag.startsWith("id", ignoreCase = true) -> "id"
            tag.startsWith("zh", ignoreCase = true) -> "zh-CN"
            else -> SUPPORTED_LANGUAGE_TAGS.firstOrNull {
                it.equals(tag, ignoreCase = true) || it.equals(current.language, ignoreCase = true)
            } ?: tag
        }
    }

    fun setAppLanguage(tag: String) {
        val localeList = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getDisplayLabel(context: Context, tag: String): String {
        if (tag.isBlank()) {
            return context.getString(R.string.settings_subtitle_dark_theme_follow_system)
        }
        val locale = Locale.forLanguageTag(tag)
        val nativeName = locale.getDisplayName(locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        val localizedName = locale.getDisplayName(Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        return if (nativeName.equals(localizedName, ignoreCase = true)) {
            nativeName
        } else {
            "$nativeName ($localizedName)"
        }
    }
}
