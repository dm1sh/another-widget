package com.tommasoberlose.anotherwidget

import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.helpers.BackupManager
import com.tommasoberlose.anotherwidget.helpers.LocationCandidates
import com.tommasoberlose.anotherwidget.helpers.LocationChangePolicy
import com.tommasoberlose.anotherwidget.helpers.LocationSample
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.network.repository.MeteoblueRepository
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

    @Test
    fun meteoblue_mapsAllWeatherConditionFamiliesAndDayNight() {
        assertEquals(Constants.WeatherProvider.METEOBLUE, Constants.WeatherProvider.fromInt(7))
        assertTrue(WeatherHelper.isKeyRequired(Constants.WeatherProvider.METEOBLUE))
        assertEquals("01d", WeatherHelper.getMeteoblueIcon(1, true))
        assertEquals("01n", WeatherHelper.getMeteoblueIcon(2, false))
        assertEquals("02d", WeatherHelper.getMeteoblueIcon(7, true))
        assertEquals("03n", WeatherHelper.getMeteoblueIcon(20, false))
        assertEquals("04d", WeatherHelper.getMeteoblueIcon(22, true))
        assertEquals("50d", WeatherHelper.getMeteoblueIcon(14, true))
        assertEquals("82n", WeatherHelper.getMeteoblueIcon(16, false))
        assertEquals("10d", WeatherHelper.getMeteoblueIcon(23, true))
        assertEquals("13n", WeatherHelper.getMeteoblueIcon(24, false))
        assertEquals("11d", WeatherHelper.getMeteoblueIcon(27, true))
        assertEquals("09n", WeatherHelper.getMeteoblueIcon(30, false))
        assertEquals("81d", WeatherHelper.getMeteoblueIcon(35, true))
        assertEquals("", WeatherHelper.getMeteoblueIcon(999, true))
    }

    @Test
    fun meteoblue_parsesBasic1hClosestHourAndUnitConversion() {
        val nowMs = MeteoblueRepository.parseMeteoblueTimeEpochMs("2026-10-30 14:20", 0.0)!!
        val body = mapOf(
            "metadata" to mapOf("utc_timeoffset" to 0.0),
            "units" to mapOf("temperature" to "C"),
            "data_1h" to mapOf(
                "time" to listOf("2026-10-30 13:00", "2026-10-30 14:00", "2026-10-30 15:00"),
                "temperature" to listOf(10.0, 15.0, 12.0),
                "pictocode" to listOf(1, 23, 22),
                "isdaylight" to listOf(1, 1, 0)
            )
        )

        val parsedC = MeteoblueRepository.parseWeatherResponse(body, requestedUnit = "C", nowEpochMs = nowMs)
        assertNotNull(parsedC)
        assertEquals(15.0f, parsedC!!.temperature, 0.01f)
        assertEquals("C", parsedC.temperatureUnit)
        assertEquals("10d", parsedC.iconCode)

        val parsedF = MeteoblueRepository.parseWeatherResponse(body, requestedUnit = "F", nowEpochMs = nowMs)
        assertNotNull(parsedF)
        assertEquals(59.0f, parsedF!!.temperature, 0.01f)
        assertEquals("F", parsedF.temperatureUnit)
        assertEquals("10d", parsedF.iconCode)

        val currentFallback = mapOf(
            "units" to mapOf("temperature" to "F"),
            "data_current" to mapOf(
                "temperature" to 68.0,
                "isdaylight" to 0,
                "pictocode_detailed" to 2
            )
        )
        val parsedCurrent = MeteoblueRepository.parseWeatherResponse(currentFallback, requestedUnit = "F")
        assertNotNull(parsedCurrent)
        assertEquals(68.0f, parsedCurrent!!.temperature, 0.01f)
        assertEquals("01n", parsedCurrent.iconCode)

        val errorPayload = mapOf("error" to true, "error_message" to "Apikey was specified, but is not valid.")
        assertNull(MeteoblueRepository.parseWeatherResponse(errorPayload, requestedUnit = "C"))
        assertEquals("Apikey was specified, but is not valid.", MeteoblueRepository.extractErrorMessage(errorPayload))
    }

    @Test
    fun backupManager_supportsMeteoblueProviderAndApiKeyFiltering() {
        val raw = mapOf<String, Any?>(
            "weatherProvider" to Constants.WeatherProvider.METEOBLUE.rawValue,
            "weatherProviderApiMeteoblue" to "mb-secret-key"
        )
        val withKeys = BackupManager.serializeToJson(raw, appVersionCode = 100, appVersionName = "2.2.3", includeApiKeys = true)
        val restoredWithKeys = BackupManager.parseBackupJson(withKeys, restoreApiKeys = true) as BackupManager.ParseResult.Success
        assertEquals(Constants.WeatherProvider.METEOBLUE.rawValue, restoredWithKeys.entries["weatherProvider"])
        assertEquals("mb-secret-key", restoredWithKeys.entries["weatherProviderApiMeteoblue"])

        val withoutKeys = BackupManager.serializeToJson(raw, appVersionCode = 100, appVersionName = "2.2.3", includeApiKeys = false)
        val restoredWithoutKeys = BackupManager.parseBackupJson(withoutKeys, restoreApiKeys = true) as BackupManager.ParseResult.Success
        assertEquals(Constants.WeatherProvider.METEOBLUE.rawValue, restoredWithoutKeys.entries["weatherProvider"])
        assertFalse(restoredWithoutKeys.entries.containsKey("weatherProviderApiMeteoblue"))
    }
}
