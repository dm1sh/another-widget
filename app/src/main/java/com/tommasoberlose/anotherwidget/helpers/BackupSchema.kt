package com.tommasoberlose.anotherwidget.helpers

/**
 * Typed specification of user-configurable preferences eligible for SAF JSON Backup & Restore.
 * Transient runtime caches (current weather temp/icon, last notification, playing media title,
 * battery polling timestamps, etc.) are intentionally excluded.
 */
object BackupSchema {

    const val SCHEMA_VERSION = 1
    const val FORMAT_ID = "dm1sh.anotherwidget.backup"

    enum class ValueType {
        BOOLEAN,
        INT,
        LONG,
        FLOAT,
        STRING
    }

    data class Spec(
        val key: String,
        val type: ValueType,
        val isApiKey: Boolean = false,
        val validator: ((Any) -> Boolean)? = null
    )

    private val hexColorRegex = Regex("^#[0-9A-Fa-f]{6}$")
    private val hexAlphaRegex = Regex("^[0-9A-Fa-f]{2}$")

    private val isColor: (Any) -> Boolean = { v -> v is String && hexColorRegex.matches(v) }
    private val isAlpha: (Any) -> Boolean = { v -> v is String && hexAlphaRegex.matches(v) }

    val SPECS: List<Spec> = listOf(
        // Appearance & Theme
        Spec("darkThemePreference", ValueType.INT),
        Spec("showWallpaper", ValueType.BOOLEAN),
        Spec("showPreview", ValueType.BOOLEAN),
        Spec("showXiaomiWarning", ValueType.BOOLEAN),

        // Layout & Alignment
        Spec("widgetMargin", ValueType.FLOAT, validator = { it is Float && it in 0f..64f }),
        Spec("widgetPadding", ValueType.FLOAT, validator = { it is Float && it in 0f..64f }),
        Spec("widgetAlign", ValueType.INT, validator = { it is Int && it in 0..5 }),
        Spec("clockBottomMargin", ValueType.INT, validator = { it is Int && it in 0..3 }),
        Spec("secondRowTopMargin", ValueType.INT, validator = { it is Int && it in 0..3 }),
        Spec("showDividers", ValueType.BOOLEAN),

        // Typography & Colors
        Spec("PREF_TEXT_MAIN_SIZE", ValueType.FLOAT, validator = { it is Float && it in 8f..60f }),
        Spec("PREF_TEXT_SECOND_SIZE", ValueType.FLOAT, validator = { it is Float && it in 8f..60f }),
        Spec("PREF_TEXT_CLOCK_SIZE", ValueType.FLOAT, validator = { it is Float && it in 20f..160f }),
        Spec("PREF_TEXT_SHADOW", ValueType.INT, validator = { it is Int && it in 0..2 }),
        Spec("textShadowDark", ValueType.INT, validator = { it is Int && it in 0..2 }),
        Spec("PREF_CUSTOM_FONT", ValueType.INT, validator = { it is Int && it in 0..3 }),
        Spec("customFontFile", ValueType.STRING),
        Spec("customFontName", ValueType.STRING),
        Spec("customFontVariant", ValueType.STRING),
        Spec("dateFormat", ValueType.STRING),
        Spec("isDateCapitalize", ValueType.BOOLEAN),
        Spec("isDateUppercase", ValueType.BOOLEAN),

        Spec("PREF_TEXT_COLOR", ValueType.STRING, validator = isColor),
        Spec("textGlobalAlpha", ValueType.STRING, validator = isAlpha),
        Spec("textSecondaryColor", ValueType.STRING, validator = isColor),
        Spec("textSecondaryAlpha", ValueType.STRING, validator = isAlpha),
        Spec("backgroundCardColor", ValueType.STRING, validator = isColor),
        Spec("backgroundCardAlpha", ValueType.STRING, validator = isAlpha),
        Spec("clockTextColor", ValueType.STRING, validator = isColor),
        Spec("clockTextAlpha", ValueType.STRING, validator = isAlpha),

        Spec("textGlobalColorDark", ValueType.STRING, validator = isColor),
        Spec("textGlobalAlphaDark", ValueType.STRING, validator = isAlpha),
        Spec("textSecondaryColorDark", ValueType.STRING, validator = isColor),
        Spec("textSecondaryAlphaDark", ValueType.STRING, validator = isAlpha),
        Spec("backgroundCardColorDark", ValueType.STRING, validator = isColor),
        Spec("backgroundCardAlphaDark", ValueType.STRING, validator = isAlpha),
        Spec("clockTextColorDark", ValueType.STRING, validator = isColor),
        Spec("clockTextAlphaDark", ValueType.STRING, validator = isAlpha),

        // Calendar
        Spec("PREF_SHOW_EVENTS", ValueType.BOOLEAN),
        Spec("PREF_CALENDAR_ALL_DAY", ValueType.BOOLEAN),
        Spec("PREF_CALENDAR_FILTER", ValueType.STRING),
        Spec("PREF_SHOW_UNTIL", ValueType.INT, validator = { it is Int && it in 0..7 }),
        Spec("PREF_SHOW_DIFF_TIME", ValueType.BOOLEAN),
        Spec("PREF_SHOW_DECLINED_EVENTS", ValueType.BOOLEAN),
        Spec("showInvitedEvents", ValueType.BOOLEAN),
        Spec("showAcceptedEvents", ValueType.BOOLEAN),
        Spec("showOnlyBusyEvents", ValueType.BOOLEAN),
        Spec("PREF_SECOND_ROW_INFORMATION", ValueType.INT, validator = { it is Int && it in 0..1 }),
        Spec("PREF_SHOW_NEXT_EVENT", ValueType.BOOLEAN),
        Spec("showNextEventOnMultipleLines", ValueType.BOOLEAN),
        Spec("widgetUpdateFrequency", ValueType.INT, validator = { it is Int && it in 0..2 }),

        // Weather & Location
        Spec("PREF_SHOW_WEATHER", ValueType.BOOLEAN),
        Spec("PREF_WEATHER_TEMP_UNIT", ValueType.STRING, validator = { it == "C" || it == "F" }),
        Spec("PREF_WEATHER_REFRESH_PERIOD", ValueType.INT, validator = { it is Int && it in 0..5 }),
        Spec("weatherProvider", ValueType.INT, validator = { it is Int && it in 0..6 }),
        Spec("weatherIconPack", ValueType.INT, validator = { it is Int && it in 0..3 }),
        Spec("PREF_CUSTOM_LOCATION_LAT", ValueType.STRING),
        Spec("PREF_CUSTOM_LOCATION_LON", ValueType.STRING),
        Spec("PREF_CUSTOM_LOCATION_ADD", ValueType.STRING),

        // Optional Weather API Keys
        Spec("PREF_WEATHER_PROVIDER_API_KEY", ValueType.STRING, isApiKey = true),
        Spec("weatherProviderApiHere", ValueType.STRING, isApiKey = true),
        Spec("weatherProviderApiWeatherApi", ValueType.STRING, isApiKey = true),
        Spec("weatherProviderApiWeatherBit", ValueType.STRING, isApiKey = true),
        Spec("weatherProviderApiAccuweather", ValueType.STRING, isApiKey = true),

        // Clock
        Spec("PREF_SHOW_CLOCK", ValueType.BOOLEAN),
        Spec("showAMPMIndicator", ValueType.BOOLEAN),
        Spec("altTimezoneLabel", ValueType.STRING),
        Spec("altTimezoneId", ValueType.STRING),

        // Gestures & Target Apps
        Spec("openEventDetails", ValueType.BOOLEAN),
        Spec("PREF_CALENDAR_APP_NAME", ValueType.STRING),
        Spec("PREF_CALENDAR_APP_PACKAGE", ValueType.STRING),
        Spec("PREF_WEATHER_APP_NAME", ValueType.STRING),
        Spec("PREF_WEATHER_APP_PACKAGE", ValueType.STRING),
        Spec("PREF_EVENT_APP_NAME", ValueType.STRING),
        Spec("PREF_EVENT_APP_PACKAGE", ValueType.STRING),
        Spec("PREF_CLOCK_APP_NAME", ValueType.STRING),
        Spec("PREF_CLOCK_APP_PACKAGE", ValueType.STRING),

        // Glance
        Spec("enabledGlanceProviderOrder", ValueType.STRING),
        Spec("customNotes", ValueType.STRING),
        Spec("showNextAlarm", ValueType.BOOLEAN),
        Spec("nextAlarmWindow", ValueType.INT, validator = { it is Int && it in setOf(0, 30, 60, 180, 360, 720, 1440) }),
        Spec("showBatteryCharging", ValueType.BOOLEAN),
        Spec("showDailySteps", ValueType.BOOLEAN),
        Spec("showGreetings", ValueType.BOOLEAN),
        Spec("showNotifications", ValueType.BOOLEAN),
        Spec("hideNotificationAfter", ValueType.INT, validator = { it is Int && it in 0..5 }),
        Spec("showMusic", ValueType.BOOLEAN),
        Spec("mediaInfoFormat", ValueType.STRING),
        Spec("musicPlayersFilter", ValueType.STRING),
        Spec("appNotificationsFilter", ValueType.STRING),
        Spec("showEventsAsGlanceProvider", ValueType.BOOLEAN),
        Spec("showWeatherAsGlanceProvider", ValueType.BOOLEAN)
    )

    val SPECS_BY_KEY: Map<String, Spec> = SPECS.associateBy { it.key }
}
