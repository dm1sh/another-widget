package com.tommasoberlose.anotherwidget.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.chibatching.kotpref.Kotpref
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.LocationChangePolicy
import com.tommasoberlose.anotherwidget.helpers.LocationResolver
import com.tommasoberlose.anotherwidget.network.WeatherNetworkApi
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        Kotpref.init(context)
        val forceRefresh = inputData.getBoolean(KEY_FORCE_REFRESH, false)

        if (Preferences.customLocationAdd == "" && LocationResolver.hasLocationPermission(context)) {
            val resolved = LocationResolver.resolveAutomaticLocation(context)
            if (resolved != null) {
                Preferences.customLocationLat = resolved.latitude.toString()
                Preferences.customLocationLon = resolved.longitude.toString()
                LocationResolver.updateReverseGeocodedLabel(context, resolved.latitude, resolved.longitude)
            }
        }

        val nowMs = System.currentTimeMillis()
        val refreshIntervalMs = getRefreshIntervalMinutes() * 60_000L
        val hasError = (Preferences.weatherProviderError != "" && Preferences.weatherProviderError != "-") ||
            Preferences.weatherProviderLocationError != ""

        val canReuseCache = LocationChangePolicy.shouldReuseCachedWeather(
            forceRefresh = forceRefresh,
            hasCachedWeatherIcon = Preferences.weatherIcon.isNotBlank(),
            hasActiveError = hasError,
            cachedLat = Preferences.weatherCacheLat,
            cachedLon = Preferences.weatherCacheLon,
            currentLat = Preferences.customLocationLat,
            currentLon = Preferences.customLocationLon,
            cachedProvider = Preferences.weatherCacheProvider,
            currentProvider = Preferences.weatherProvider,
            cachedTimestampMs = Preferences.weatherCacheTimestamp,
            nowMs = nowMs,
            refreshIntervalMs = refreshIntervalMs,
            movementThresholdMeters = Constants.WEATHER_LOCATION_CHANGE_THRESHOLD_M
        )

        if (canReuseCache) {
            MainWidget.updateWidget(context)
            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
        } else {
            withContext(Dispatchers.IO) {
                WeatherNetworkApi(context).updateWeather()
            }
            if (Preferences.weatherIcon.isNotBlank() &&
                Preferences.weatherProviderError == "" &&
                Preferences.weatherProviderLocationError == ""
            ) {
                Preferences.weatherCacheTimestamp = System.currentTimeMillis()
                Preferences.weatherCacheLat = Preferences.customLocationLat
                Preferences.weatherCacheLon = Preferences.customLocationLon
                Preferences.weatherCacheProvider = Preferences.weatherProvider
            }
        }

        if (Preferences.showWeather) {
            enqueueTrigger(context)
        }
        return Result.success()
    }

    companion object {
        const val KEY_FORCE_REFRESH = "KEY_FORCE_REFRESH"

        fun getRefreshIntervalMinutes(): Long = when (Preferences.weatherRefreshPeriod) {
            0 -> 30L
            1 -> 60L
            2 -> 60L * 3
            3 -> 60L * 6
            4 -> 60L * 12
            5 -> 60L * 24
            else -> 60L
        }

        fun enqueue(context: Context, replace: Boolean = false) {
            val data = Data.Builder()
                .putBoolean(KEY_FORCE_REFRESH, replace)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "updateWeather",
                if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<WeatherWorker>()
                    .setInputData(data)
                    .build()
            )
        }

        fun enqueueTrigger(context: Context) {
            val interval = getRefreshIntervalMinutes()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "updateWeatherTrigger",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<Trigger>().setInitialDelay(
                    interval, TimeUnit.MINUTES
                ).build()
            )
        }

        fun cancelTrigger(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("updateWeatherTrigger")
        }
    }

    class Trigger(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            if (Preferences.showWeather && !isStopped) {
                enqueue(applicationContext, replace = false)
            }
            return Result.success()
        }
    }
}
