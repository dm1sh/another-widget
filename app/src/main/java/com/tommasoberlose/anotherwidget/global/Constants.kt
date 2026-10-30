package com.tommasoberlose.anotherwidget.global

object Constants {
    const val RESULT_CODE_CUSTOM_LOCATION = 45
    const val RESULT_APP_NAME = "RESULT_APP_NAME"
    const val RESULT_APP_PACKAGE = "RESULT_APP_PACKAGE"

    const val CUSTOM_FONT_GOOGLE_SANS = 1
    const val CUSTOM_FONT_DOWNLOADED = 2
    const val CUSTOM_FONT_DOWNLOAD_NEW = 3

    const val LOCATION_LAST_KNOWN_MAX_AGE_MS = 30 * 60 * 1000L
    const val LOCATION_LAST_KNOWN_MAX_ACCURACY_M = 1_500f
    const val LOCATION_FRESH_MAX_ACCURACY_M = 2_500f
    const val WEATHER_LOCATION_CHANGE_THRESHOLD_M = 2_000f

    enum class ClockBottomMargin(val rawValue: Int) {
        NONE(0),
        SMALL(1),
        MEDIUM(2),
        LARGE(3)
    }

    enum class SecondRowTopMargin(val rawValue: Int) {
        NONE(0),
        SMALL(1),
        MEDIUM(2),
        LARGE(3)
    }

    enum class Dimension(val rawValue: Float) {
        NONE(0f),
        SMALL(8f),
        MEDIUM(16f),
        LARGE(24f)
    }

    enum class GlanceProviderId(val id: String) {
        PLAYING_SONG("PLAYING_SONG"),
        NEXT_CLOCK_ALARM("NEXT_CLOCK_ALARM"),
        BATTERY_LEVEL_LOW("BATTERY_LEVEL_LOW"),
        CUSTOM_INFO("CUSTOM_INFO"),
        GOOGLE_FIT_STEPS("GOOGLE_FIT_STEPS"),
        NOTIFICATIONS("NOTIFICATIONS"),
        GREETINGS("GREETINGS"),
        EVENTS("EVENTS"),
        WEATHER("WEATHER");

        companion object {
            private val map = values().associateBy(GlanceProviderId::id)
            fun from(type: String) = map[type]
        }
    }

    enum class WidgetUpdateFrequency(val rawValue: Int) {
        LOW(0),
        DEFAULT(1),
        HIGH(2)
    }

    enum class WeatherProvider(val rawValue: Int) {
        OPEN_WEATHER(0),
        WEATHER_BIT(1),
        WEATHER_API(2),
        HERE(3),
        ACCUWEATHER(4),
        WEATHER_GOV(5),
        YR(6);

        companion object {
            private val map = values().associateBy(WeatherProvider::rawValue)
            fun fromInt(type: Int): WeatherProvider = map[type] ?: OPEN_WEATHER
        }
    }

    enum class GlanceNotificationTimer(val rawValue: Int) {
        HALF_MINUTE(0),
        ONE_MINUTE(1),
        FIVE_MINUTES(2),
        TEN_MINUTES(3),
        FIFTEEN_MINUTES(4),
        WHEN_DISMISSED(5);

        companion object {
            private val map = values().associateBy(GlanceNotificationTimer::rawValue)
            fun fromInt(type: Int): GlanceNotificationTimer = map[type] ?: ONE_MINUTE
        }
    }

    enum class NextAlarmWindow(val rawValue: Int) {
        ALWAYS(0),
        THIRTY_MINUTES(30),
        ONE_HOUR(60),
        THREE_HOURS(180),
        SIX_HOURS(360),
        TWELVE_HOURS(720),
        TWENTY_FOUR_HOURS(1440);

        companion object {
            private val map = values().associateBy(NextAlarmWindow::rawValue)
            fun fromInt(value: Int): NextAlarmWindow = map[value] ?: ALWAYS
        }
    }

    enum class WeatherIconPack(val rawValue: Int) {
        DEFAULT(0),
        MINIMAL(1),
        COOL(2),
        GOOGLE_NEWS(3)
    }

    enum class WidgetAlign(val rawValue: Int) {
        LEFT(0),
        RIGHT(1),
        CENTER(2),
        TOP_LEFT(3),
        TOP_RIGHT(4),
        TOP_CENTER(5)
    }
}
