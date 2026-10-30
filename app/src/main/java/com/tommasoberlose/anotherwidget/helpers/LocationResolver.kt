package com.tommasoberlose.anotherwidget.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

object LocationResolver {
    private const val FUSED_PROVIDER_NAME = "fused"
    private const val FRESH_FIX_TIMEOUT_MS = 10_000L

    fun hasLocationPermission(context: Context): Boolean {
        return context.checkGrantedPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            context.checkGrantedPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    @SuppressLint("MissingPermission")
    suspend fun resolveAutomaticLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        if (!LocationManagerCompat.isLocationEnabled(lm)) {
            return selectBestLastKnown(lm)
        }

        val hasFine = context.checkGrantedPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val orderedActiveProviders = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isProviderUsable(lm, FUSED_PROVIDER_NAME)) {
                add(FUSED_PROVIDER_NAME)
            }
            if (isProviderUsable(lm, LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            if (hasFine && isProviderUsable(lm, LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
        }

        for (provider in orderedActiveProviders) {
            val fresh = requestSingleCurrentLocation(context, lm, provider)
            if (fresh != null &&
                (!fresh.hasAccuracy() || fresh.accuracy <= Constants.LOCATION_FRESH_MAX_ACCURACY_M)
            ) {
                return fresh
            }
        }

        return selectBestLastKnown(lm)
    }

    private fun isProviderUsable(lm: LocationManager, provider: String): Boolean {
        return try {
            lm.isProviderEnabled(provider)
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleCurrentLocation(
        context: Context,
        lm: LocationManager,
        provider: String
    ): Location? = withTimeoutOrNull(FRESH_FIX_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation {
                try {
                    signal.cancel()
                } catch (_: Exception) {
                }
            }
            try {
                LocationManagerCompat.getCurrentLocation(
                    lm,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(context)
                ) { location ->
                    if (cont.isActive) {
                        cont.resume(location)
                    }
                }
            } catch (_: Exception) {
                if (cont.isActive) {
                    cont.resume(null)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun selectBestLastKnown(lm: LocationManager): Location? {
        val candidateProviders = buildSet {
            addAll(runCatching { lm.allProviders }.getOrDefault(emptyList()))
            add(FUSED_PROVIDER_NAME)
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        val nowElapsedNs = SystemClock.elapsedRealtimeNanos()
        val nowWallMs = System.currentTimeMillis()
        val byLocation = mutableMapOf<LocationSample, Location>()

        for (provider in candidateProviders) {
            val loc = try {
                lm.getLastKnownLocation(provider)
            } catch (_: Exception) {
                null
            } ?: continue

            val ageMs = if (loc.elapsedRealtimeNanos > 0L && nowElapsedNs >= loc.elapsedRealtimeNanos) {
                (nowElapsedNs - loc.elapsedRealtimeNanos) / 1_000_000L
            } else if (loc.time > 0L && nowWallMs >= loc.time) {
                nowWallMs - loc.time
            } else {
                0L
            }
            val sample = LocationSample(
                provider = provider,
                latitude = loc.latitude,
                longitude = loc.longitude,
                accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
                ageMs = ageMs
            )
            byLocation[sample] = loc
        }

        val best = LocationCandidates.selectBest(
            samples = byLocation.keys,
            maxAgeMs = Constants.LOCATION_LAST_KNOWN_MAX_AGE_MS,
            maxAccuracyMeters = Constants.LOCATION_LAST_KNOWN_MAX_ACCURACY_M
        ) ?: LocationCandidates.selectBest(
            samples = byLocation.keys,
            maxAgeMs = 6 * 60 * 60_000L,
            maxAccuracyMeters = 10_000f
        )
        return best?.let { byLocation[it] }
    }

    suspend fun updateReverseGeocodedLabel(context: Context, lat: Double, lon: Double) {
        if (!Geocoder.isPresent()) return
        val label = withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val address: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    withTimeoutOrNull(4_000L) {
                        suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (cont.isActive) cont.resume(addresses.firstOrNull())
                                }

                                override fun onError(errorMessage: String?) {
                                    if (cont.isActive) cont.resume(null)
                                }
                            })
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                }
                formatAddressLabel(address)
            } catch (_: Exception) {
                ""
            }
        }
        if (label.isNotBlank()) {
            Preferences.autoLocationLabel = label
        }
    }

    fun formatAddressLabel(address: Address?): String {
        if (address == null) return ""
        val district = address.subLocality?.takeIf { it.isNotBlank() }
        val city = address.locality?.takeIf { it.isNotBlank() }
            ?: address.subAdminArea?.takeIf { it.isNotBlank() }
            ?: address.adminArea?.takeIf { it.isNotBlank() }
        return when {
            district != null && city != null && !district.equals(city, ignoreCase = true) -> "$district, $city"
            city != null -> city
            district != null -> district
            else -> address.countryName?.takeIf { it.isNotBlank() } ?: ""
        }
    }
}
