package com.tommasoberlose.anotherwidget

import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.helpers.BackupManager
import com.tommasoberlose.anotherwidget.helpers.LocationCandidates
import com.tommasoberlose.anotherwidget.helpers.LocationChangePolicy
import com.tommasoberlose.anotherwidget.helpers.LocationSample
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.TimeZoneSelectorViewModel
import me.everything.providers.android.calendar.Calendar
import me.everything.providers.android.calendar.Event as ProviderEvent
import me.everything.providers.core.Entity
import net.idik.lib.slimadapter.SlimAdapter
import net.idik.lib.slimadapter.SlimAdapterEx
import net.idik.lib.slimadapter.register
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class HelpersUnitTest {

    @Test
    fun locationCandidates_selectsFreshAccurateFixAndRejectsOutliers() {
        val staleGps = LocationSample("gps", 52.3676, 4.9041, 10f, ageMs = 45 * 60_000L)
        val ultraCoarse = LocationSample("network", 52.3676, 4.9041, 5_000f, ageMs = 10_000L)
        val zeroZero = LocationSample("passive", 0.0, 0.0, 5f, ageMs = 1_000L)
        val recentWifi = LocationSample("network", 52.3702, 4.8952, 45f, ageMs = 60_000L)
        val olderCell = LocationSample("network", 52.3700, 4.8900, 600f, ageMs = 10 * 60_000L)

        val selected = LocationCandidates.selectBest(
            samples = listOf(staleGps, ultraCoarse, zeroZero, olderCell, recentWifi),
            maxAgeMs = Constants.LOCATION_LAST_KNOWN_MAX_AGE_MS,
            maxAccuracyMeters = Constants.LOCATION_LAST_KNOWN_MAX_ACCURACY_M
        )
        assertEquals(recentWifi, selected)
    }

    @Test
    fun locationChangePolicy_reusesCacheWhenStationaryAndInvalidatesOnMove() {
        val now = 1_700_000_000_000L
        val cachedAt = now - 5 * 60_000L

        assertTrue(
            LocationChangePolicy.shouldReuseCachedWeather(
                forceRefresh = false,
                hasCachedWeatherIcon = true,
                hasActiveError = false,
                cachedLat = "52.3676",
                cachedLon = "4.9041",
                currentLat = "52.3680",
                currentLon = "4.9050",
                cachedProvider = Constants.WeatherProvider.YR.rawValue,
                currentProvider = Constants.WeatherProvider.YR.rawValue,
                cachedTimestampMs = cachedAt,
                nowMs = now,
                refreshIntervalMs = 60 * 60_000L,
                movementThresholdMeters = Constants.WEATHER_LOCATION_CHANGE_THRESHOLD_M
            )
        )

        // Moved ~35 km (Amsterdam -> Utrecht): must fetch fresh weather
        assertFalse(
            LocationChangePolicy.shouldReuseCachedWeather(
                forceRefresh = false,
                hasCachedWeatherIcon = true,
                hasActiveError = false,
                cachedLat = "52.3676",
                cachedLon = "4.9041",
                currentLat = "52.0907",
                currentLon = "5.1214",
                cachedProvider = Constants.WeatherProvider.YR.rawValue,
                currentProvider = Constants.WeatherProvider.YR.rawValue,
                cachedTimestampMs = cachedAt,
                nowMs = now,
                refreshIntervalMs = 60 * 60_000L,
                movementThresholdMeters = Constants.WEATHER_LOCATION_CHANGE_THRESHOLD_M
            )
        )

        // Force refresh always bypasses cache
        assertFalse(
            LocationChangePolicy.shouldReuseCachedWeather(
                forceRefresh = true,
                hasCachedWeatherIcon = true,
                hasActiveError = false,
                cachedLat = "52.3676",
                cachedLon = "4.9041",
                currentLat = "52.3676",
                currentLon = "4.9041",
                cachedProvider = Constants.WeatherProvider.YR.rawValue,
                currentProvider = Constants.WeatherProvider.YR.rawValue,
                cachedTimestampMs = cachedAt,
                nowMs = now,
                refreshIntervalMs = 60 * 60_000L,
                movementThresholdMeters = Constants.WEATHER_LOCATION_CHANGE_THRESHOLD_M
            )
        )
    }

    @Test
    fun timeZoneSelector_loadsAndFiltersOfflineSystemZones() {
        val zones = TimeZoneSelectorViewModel.loadSystemTimeZones(1_700_000_000_000L, Locale.ENGLISH)
        assertTrue(zones.isNotEmpty())
        assertTrue(zones.any { it.id == "Europe/Amsterdam" })
        assertFalse(zones.any { it.id.startsWith("Etc/") })

        val filtered = TimeZoneSelectorViewModel.filterEntries(zones, "amsterdam", Locale.ENGLISH)
        assertTrue(filtered.any { it.id == "Europe/Amsterdam" })
    }

    @Test
    fun backupManager_roundTripsSettingsRespectsApiKeyToggleAndRejectsTransientState() {
        val raw = mapOf(
            "widgetAlign" to Constants.WidgetAlign.TOP_CENTER.rawValue,
            "PREF_TEXT_COLOR" to "#AABBCC",
            "textGlobalAlpha" to "E6",
            "PREF_TEXT_COLOR_INVALID_TEST" to "nope",
            "nextAlarmWindow" to Constants.NextAlarmWindow.THREE_HOURS.rawValue,
            "PREF_WEATHER_PROVIDER_API_KEY" to "secret-owm-key",
            "lastNotificationTitle" to "Private chat message",
            "PREF_WEATHER_TEMP" to 21.5f
        )

        val jsonWithoutKeys = BackupManager.serializeToJson(
            rawPreferences = raw,
            includeApiKeys = false,
            appVersion = "2.4.0 (142)",
            appLanguageTag = "ru",
            exportedAtMs = 1_700_000_000_000L
        )
        assertFalse(jsonWithoutKeys.contains("secret-owm-key"))
        assertFalse(jsonWithoutKeys.contains("Private chat message"))

        val parsedNoKeys = BackupManager.parseBackupJson(jsonWithoutKeys, restoreApiKeys = true)
        assertTrue(parsedNoKeys is BackupManager.ParseResult.Valid)
        val summaryNoKeys = (parsedNoKeys as BackupManager.ParseResult.Valid).summary
        assertEquals(Constants.WidgetAlign.TOP_CENTER.rawValue, summaryNoKeys.values["widgetAlign"])
        assertEquals("#AABBCC", summaryNoKeys.values["PREF_TEXT_COLOR"])
        assertEquals(Constants.NextAlarmWindow.THREE_HOURS.rawValue, summaryNoKeys.values["nextAlarmWindow"])
        assertEquals("ru", summaryNoKeys.appLanguageTag)
        assertFalse(summaryNoKeys.values.containsKey("PREF_WEATHER_PROVIDER_API_KEY"))
        assertFalse(summaryNoKeys.values.containsKey("lastNotificationTitle"))

        val jsonWithKeys = BackupManager.serializeToJson(
            rawPreferences = raw,
            includeApiKeys = true,
            appVersion = "2.4.0 (142)",
            appLanguageTag = "en",
            exportedAtMs = 1_700_000_000_000L
        )
        val parsedWithKeys = BackupManager.parseBackupJson(jsonWithKeys, restoreApiKeys = true)
        assertTrue(parsedWithKeys is BackupManager.ParseResult.Valid)
        assertEquals(
            "secret-owm-key",
            (parsedWithKeys as BackupManager.ParseResult.Valid).summary.values["PREF_WEATHER_PROVIDER_API_KEY"]
        )

        val invalid = BackupManager.parseBackupJson("{\"format\":\"wrong\"}", restoreApiKeys = true)
        assertTrue(invalid is BackupManager.ParseResult.Invalid)
    }

    @Test
    fun enums_fromIntNeverReturnsNullOnOutOfRangeValues() {
        assertEquals(Constants.NextAlarmWindow.ALWAYS, Constants.NextAlarmWindow.fromInt(99999))
        assertEquals(Constants.NextAlarmWindow.SIX_HOURS, Constants.NextAlarmWindow.fromInt(360))
        assertEquals(Constants.WeatherProvider.OPEN_WEATHER, Constants.WeatherProvider.fromInt(-99))
        assertEquals(Constants.GlanceNotificationTimer.ONE_MINUTE, Constants.GlanceNotificationTimer.fromInt(99))
    }

    @Test
    fun slimAdapter_registersKotlinSamLambdasWithoutReflectionCrash() {
        val adapter = SlimAdapter.create()
            .register<Constants.WeatherProvider>(101) { _, _ -> }
            .register<String>(102) { _, _ -> }
            .register<Int>(103) { _, _ -> }

        assertTrue(adapter.hasRegisteredCreator(Constants.WeatherProvider.YR.javaClass))
        assertTrue(adapter.hasRegisteredCreator("provider".javaClass))
        assertTrue(adapter.hasRegisteredCreator((42 as Any).javaClass))
        assertTrue(adapter.hasRegisteredCreator(Int::class.java))
        assertFalse(adapter.hasRegisteredCreator(Double::class.javaObjectType))

        val exAdapter = SlimAdapterEx.create()
            .register<String>(201) { _, _ -> }
        assertTrue(exAdapter.hasRegisteredCreator("app".javaClass))
    }

    @Test
    fun calendarProviderEntities_exposeMappedColumnsAndSafeFieldAccess() {
        val calCols = Entity.getColumns(Calendar::class.java).toList()
        assertTrue(calCols.contains("_id"))
        assertTrue(calCols.contains("calendar_displayName"))

        val calWriteCols = Entity.getWriteColumns(Calendar::class.java).toList()
        assertTrue(calWriteCols.contains("calendar_displayName"))

        val ev = ProviderEvent().apply {
            id = 77L
            title = "Standup"
        }
        assertEquals(77L, Entity.getId(ev))
        assertEquals("Standup", Entity.getColumnValue("title", ev))
    }
}
