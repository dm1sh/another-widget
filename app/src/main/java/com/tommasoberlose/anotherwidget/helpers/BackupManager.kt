package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tommasoberlose.anotherwidget.BuildConfig
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget

object BackupManager {

    data class BackupSummary(
        val schemaVersion: Int,
        val appVersion: String,
        val exportedAtMs: Long,
        val includesApiKeys: Boolean,
        val appLanguageTag: String,
        val values: Map<String, Any>
    )

    sealed class ParseResult {
        data class Valid(val summary: BackupSummary) : ParseResult()
        data class Invalid(val reason: String) : ParseResult()
    }

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun serializeToJson(
        rawPreferences: Map<String, *>,
        includeApiKeys: Boolean,
        appVersion: String,
        appLanguageTag: String,
        exportedAtMs: Long = System.currentTimeMillis()
    ): String {
        val root = JsonObject()
        root.addProperty("format", BackupSchema.FORMAT_ID)
        root.addProperty("schemaVersion", BackupSchema.SCHEMA_VERSION)
        root.addProperty("appVersion", appVersion)
        root.addProperty("exportedAtMs", exportedAtMs)
        root.addProperty("includesApiKeys", includeApiKeys)
        root.addProperty("appLanguageTag", appLanguageTag)

        val prefsObj = JsonObject()
        for (spec in BackupSchema.SPECS) {
            if (spec.isApiKey && !includeApiKeys) continue
            val value = rawPreferences[spec.key] ?: continue
            when (spec.type) {
                BackupSchema.ValueType.BOOLEAN -> if (value is Boolean) prefsObj.addProperty(spec.key, value)
                BackupSchema.ValueType.INT -> if (value is Number) prefsObj.addProperty(spec.key, value.toInt())
                BackupSchema.ValueType.LONG -> if (value is Number) prefsObj.addProperty(spec.key, value.toLong())
                BackupSchema.ValueType.FLOAT -> if (value is Number) {
                    val f = value.toFloat()
                    if (f.isFinite()) prefsObj.addProperty(spec.key, f)
                }
                BackupSchema.ValueType.STRING -> if (value is String) prefsObj.addProperty(spec.key, value)
            }
        }
        root.add("preferences", prefsObj)
        return gson.toJson(root)
    }

    fun parseBackupJson(jsonText: String, restoreApiKeys: Boolean): ParseResult {
        if (jsonText.isBlank()) {
            return ParseResult.Invalid("Empty backup file")
        }
        val root: JsonObject = try {
            val parsed = JsonParser.parseString(jsonText)
            if (!parsed.isJsonObject) return ParseResult.Invalid("Root JSON element must be an object")
            parsed.asJsonObject
        } catch (ex: Exception) {
            return ParseResult.Invalid(ex.message ?: "Malformed JSON")
        }

        val format = root.get("format")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
        if (format != BackupSchema.FORMAT_ID) {
            return ParseResult.Invalid("Unsupported backup format")
        }

        val schemaVersion = root.get("schemaVersion")?.takeIf { it.isJsonPrimitive }?.let {
            runCatching { it.asInt }.getOrNull()
        } ?: return ParseResult.Invalid("Missing schemaVersion")

        if (schemaVersion < 1 || schemaVersion > BackupSchema.SCHEMA_VERSION) {
            return ParseResult.Invalid("Unsupported schemaVersion: $schemaVersion")
        }

        val prefsElement = root.get("preferences")
        if (prefsElement == null || !prefsElement.isJsonObject) {
            return ParseResult.Invalid("Missing preferences object")
        }
        val prefsObj = prefsElement.asJsonObject

        val validated = linkedMapOf<String, Any>()
        for ((key, elem) in prefsObj.entrySet()) {
            val spec = BackupSchema.SPECS_BY_KEY[key] ?: continue
            if (spec.isApiKey && !restoreApiKeys) continue
            val decoded = decodeValue(spec, elem) ?: continue
            if (spec.validator == null || spec.validator.invoke(decoded)) {
                validated[key] = decoded
            }
        }

        val appVersion = root.get("appVersion")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
        val exportedAtMs = root.get("exportedAtMs")?.takeIf { it.isJsonPrimitive }?.let {
            runCatching { it.asLong }.getOrDefault(0L)
        } ?: 0L
        val includesApiKeys = root.get("includesApiKeys")?.takeIf { it.isJsonPrimitive }?.let {
            runCatching { it.asBoolean }.getOrDefault(false)
        } ?: false
        val appLanguageTag = root.get("appLanguageTag")?.takeIf { it.isJsonPrimitive }?.asString ?: ""

        return ParseResult.Valid(
            BackupSummary(
                schemaVersion = schemaVersion,
                appVersion = appVersion,
                exportedAtMs = exportedAtMs,
                includesApiKeys = includesApiKeys,
                appLanguageTag = appLanguageTag,
                values = validated
            )
        )
    }

    private fun decodeValue(spec: BackupSchema.Spec, element: JsonElement): Any? {
        if (!element.isJsonPrimitive) return null
        val prim = element.asJsonPrimitive
        return try {
            when (spec.type) {
                BackupSchema.ValueType.BOOLEAN -> if (prim.isBoolean) prim.asBoolean else null
                BackupSchema.ValueType.INT -> if (prim.isNumber) prim.asInt else null
                BackupSchema.ValueType.LONG -> if (prim.isNumber) prim.asLong else null
                BackupSchema.ValueType.FLOAT -> if (prim.isNumber) prim.asFloat.takeIf { it.isFinite() } else null
                BackupSchema.ValueType.STRING -> if (prim.isString) prim.asString else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun exportToJson(context: Context, includeApiKeys: Boolean): String {
        Kotpref.init(context)
        return serializeToJson(
            rawPreferences = Preferences.preferences.all,
            includeApiKeys = includeApiKeys,
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            appLanguageTag = LanguageHelper.getCurrentLanguageTag()
        )
    }

    fun applyBackup(context: Context, summary: BackupSummary) {
        Kotpref.init(context)
        val editor = Preferences.preferences.edit()
        for ((key, value) in summary.values) {
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
            }
        }
        // Reset weather cache timestamp so weather refreshes cleanly for restored location/provider
        editor.putLong("weatherCacheTimestamp", 0L)
        editor.commit()

        AppCompatDelegate.setDefaultNightMode(Preferences.darkThemePreference)
        if (summary.appLanguageTag.isBlank() || LanguageHelper.SUPPORTED_LANGUAGE_TAGS.contains(summary.appLanguageTag)) {
            LanguageHelper.setAppLanguage(summary.appLanguageTag)
        }

        CalendarHelper.updateEventList(context)
        GreetingsHelper.clearCachedGreeting()
        GreetingsHelper.toggleGreetings(context)
        BatteryHelper.updateBatteryInfo(context)
        HealthConnectHelper.updateDailySteps(context)
        WeatherHelper.updateWeather(context, force = true)
        MainWidget.updateWidget(context)
    }
}
