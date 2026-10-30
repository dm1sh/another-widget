package com.tommasoberlose.anotherwidget.helpers

import kotlin.math.*

object LocationChangePolicy {

    /**
     * Pure JVM Haversine distance in meters between two WGS84 coordinates.
     */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusM = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1.0 - a))
        return earthRadiusM * c
    }

    /**
     * Determines whether we can skip a redundant network weather fetch and reuse the cached
     * weather reading when neither the provider, refresh interval, nor user location (> threshold)
     * has changed.
     */
    fun shouldReuseCachedWeather(
        forceRefresh: Boolean,
        hasCachedWeatherIcon: Boolean,
        hasActiveError: Boolean,
        cachedLat: String,
        cachedLon: String,
        currentLat: String,
        currentLon: String,
        cachedProvider: Int,
        currentProvider: Int,
        cachedTimestampMs: Long,
        nowMs: Long,
        refreshIntervalMs: Long,
        movementThresholdMeters: Float
    ): Boolean {
        if (forceRefresh) return false
        if (!hasCachedWeatherIcon || hasActiveError) return false
        if (cachedProvider != currentProvider) return false
        if (cachedTimestampMs <= 0L || nowMs < cachedTimestampMs) return false
        // Allow a 90-second scheduling jitter buffer so periodic WorkManager triggers at the boundary still refresh
        val effectiveTtl = (refreshIntervalMs - 90_000L).coerceAtLeast(60_000L)
        if (nowMs - cachedTimestampMs >= effectiveTtl) return false

        val lat1 = cachedLat.toDoubleOrNull() ?: return false
        val lon1 = cachedLon.toDoubleOrNull() ?: return false
        val lat2 = currentLat.toDoubleOrNull() ?: return false
        val lon2 = currentLon.toDoubleOrNull() ?: return false

        val moved = distanceMeters(lat1, lon1, lat2, lon2)
        return moved < movementThresholdMeters
    }
}
