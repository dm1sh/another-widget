package com.tommasoberlose.anotherwidget.network.repository

import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.network.api.ApiServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class MeteoblueRepository {

    private val apiService: ApiServices.MeteoblueService = Retrofit.Builder()
        .baseUrl(BASE_URL_METEOBLUE)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(NetworkResponseAdapterFactory())
        .client(RepositoryHelper.getOkHttpClient())
        .build()
        .create(ApiServices.MeteoblueService::class.java)

    suspend fun getWeather() = apiService.getWeather(
        apiKey = Preferences.weatherProviderApiMeteoblue.trim(),
        lat = Preferences.customLocationLat,
        lon = Preferences.customLocationLon,
        temperatureUnit = if (Preferences.weatherTempUnit == "F") "F" else "C",
        timeZone = "UTC",
        forecastDays = 1
    )

    data class ParsedWeather(
        val temperature: Float,
        val temperatureUnit: String,
        val iconCode: String
    )

    companion object {
        private const val BASE_URL_METEOBLUE = "https://my.meteoblue.com/"
        private val ISO_OFFSET_REGEX = Regex("""^(\d{4}-?\d{2}-?\d{2}[T ]\d{2}:\d{2}(?::\d{2})?)([+-])(\d{2}):?(\d{2})$""")

        fun extractErrorMessage(body: Map<*, *>?): String? {
            if (body == null) return null
            val msg = body["error_message"]?.toString()?.trim()
            if (!msg.isNullOrEmpty()) return msg
            val fallback = body["message"]?.toString()?.trim()
            return fallback?.takeIf { it.isNotEmpty() }
        }

        fun parseWeatherResponse(
            body: Map<*, *>,
            requestedUnit: String,
            nowEpochMs: Long = System.currentTimeMillis()
        ): ParsedWeather? {
            if (body["error"] == true) {
                return null
            }

            val targetUnit = if (requestedUnit.equals("F", ignoreCase = true)) "F" else "C"
            val unitsMap = body["units"] as? Map<*, *>
            val responseUnit = unitsMap?.get("temperature")?.toString()?.trim()?.uppercase(Locale.US)
                ?.takeIf { it.isNotEmpty() } ?: targetUnit
            val metadataMap = body["metadata"] as? Map<*, *>
            val utcOffsetHours = (metadataMap?.get("utc_timeoffset") as? Number)?.toDouble() ?: 0.0

            val data1h = body["data_1h"] as? Map<*, *>
            if (data1h != null) {
                val temps = data1h["temperature"] as? List<*>
                if (!temps.isNullOrEmpty()) {
                    val times = data1h["time"] as? List<*>
                    val pictocodes = data1h["pictocode"] as? List<*>
                    val daylights = data1h["isdaylight"] as? List<*>

                    var bestIdx = -1
                    var bestDiff = Long.MAX_VALUE
                    var firstValidIdx = -1

                    for (i in temps.indices) {
                        if (temps[i] !is Number) continue
                        if (firstValidIdx == -1) firstValidIdx = i
                        val slotMs = parseMeteoblueTimeEpochMs(times?.getOrNull(i), utcOffsetHours)
                        if (slotMs != null) {
                            val diff = abs(slotMs - nowEpochMs)
                            if (diff < bestDiff) {
                                bestDiff = diff
                                bestIdx = i
                            }
                        }
                    }

                    val chosenIdx = if (bestIdx >= 0) bestIdx else firstValidIdx
                    if (chosenIdx >= 0) {
                        val rawTemp = (temps[chosenIdx] as Number).toDouble()
                        val convertedTemp = convertTemperature(rawTemp, responseUnit, targetUnit)
                        val pictocode = (pictocodes?.getOrNull(chosenIdx) as? Number)?.toInt()
                        val isDaylight = ((daylights?.getOrNull(chosenIdx) as? Number)?.toInt() ?: 1) != 0
                        val icon = WeatherHelper.getMeteoblueIcon(pictocode, isDaylight)
                        return ParsedWeather(
                            temperature = convertedTemp.toFloat(),
                            temperatureUnit = targetUnit,
                            iconCode = icon
                        )
                    }
                }
            }

            val dataCurrent = body["data_current"] as? Map<*, *>
            if (dataCurrent != null) {
                val rawTemp = (dataCurrent["temperature"] as? Number)?.toDouble() ?: return null
                val convertedTemp = convertTemperature(rawTemp, responseUnit, targetUnit)
                val isDaylight = ((dataCurrent["isdaylight"] as? Number)?.toInt() ?: 1) != 0
                val detailedCode = (dataCurrent["pictocode_detailed"] as? Number)?.toInt()
                val summaryCode = (dataCurrent["pictocode"] as? Number)?.toInt()
                val icon = if (detailedCode != null) {
                    WeatherHelper.getMeteoblueIcon(detailedCode, isDaylight)
                } else {
                    WeatherHelper.getMeteoblueDailyIcon(summaryCode, isDaylight)
                }
                return ParsedWeather(
                    temperature = convertedTemp.toFloat(),
                    temperatureUnit = targetUnit,
                    iconCode = icon
                )
            }

            return null
        }

        fun convertTemperature(value: Double, fromUnit: String, toUnit: String): Double {
            val from = fromUnit.uppercase(Locale.US)
            val to = toUnit.uppercase(Locale.US)
            if (from == to) return value
            val celsius = when (from) {
                "F" -> (value - 32.0) * 5.0 / 9.0
                "K" -> value - 273.15
                else -> value
            }
            return when (to) {
                "F" -> celsius * 9.0 / 5.0 + 32.0
                "K" -> celsius + 273.15
                else -> celsius
            }
        }

        fun parseMeteoblueTimeEpochMs(rawTime: Any?, utcOffsetHours: Double = 0.0): Long? {
            when (rawTime) {
                null -> return null
                is Number -> {
                    val v = rawTime.toLong()
                    return when {
                        v >= 100_000_000_000L -> v
                        v > 0L -> v * 1000L
                        else -> null
                    }
                }
                is String -> {
                    val trimmed = rawTime.trim()
                    if (trimmed.isEmpty()) return null
                    trimmed.toLongOrNull()?.let { numeric ->
                        return when {
                            numeric >= 100_000_000_000L -> numeric
                            numeric > 0L -> numeric * 1000L
                            else -> null
                        }
                    }

                    var normalized = trimmed
                    var explicitOffsetMs: Long? = null
                    if (normalized.endsWith("Z", ignoreCase = true)) {
                        normalized = normalized.dropLast(1)
                        explicitOffsetMs = 0L
                    } else {
                        val match = ISO_OFFSET_REGEX.matchEntire(normalized)
                        if (match != null) {
                            normalized = match.groupValues[1]
                            val sign = if (match.groupValues[2] == "-") -1L else 1L
                            val hours = match.groupValues[3].toLongOrNull() ?: 0L
                            val minutes = match.groupValues[4].toLongOrNull() ?: 0L
                            explicitOffsetMs = sign * ((hours * 60L + minutes) * 60_000L)
                        }
                    }
                    normalized = normalized.replace('T', ' ')

                    val patterns = arrayOf(
                        "yyyy-MM-dd HH:mm:ss",
                        "yyyy-MM-dd HH:mm",
                        "yyyyMMdd HH:mm:ss",
                        "yyyyMMdd HH:mm"
                    )
                    for (pattern in patterns) {
                        try {
                            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                                isLenient = false
                            }
                            val parsed = sdf.parse(normalized)
                            if (parsed != null) {
                                val offsetMs = explicitOffsetMs ?: (utcOffsetHours * 3_600_000.0).toLong()
                                return parsed.time - offsetMs
                            }
                        } catch (_: Exception) {
                            // Try next format
                        }
                    }
                    return null
                }
                else -> return null
            }
        }
    }
}
