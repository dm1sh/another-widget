package com.tommasoberlose.anotherwidget.ui.viewmodels.tabs

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.tommasoberlose.anotherwidget.global.Preferences
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

data class TimeZoneEntry(
    val id: String,
    val label: String,
    val subtitle: String,
    val offsetMinutes: Int,
    val searchTokens: String
)

class TimeZoneSelectorViewModel(application: Application) : AndroidViewModel(application) {

    val entries: MutableLiveData<List<TimeZoneEntry>> = MutableLiveData(emptyList())
    val locationInput: MutableLiveData<String> = MutableLiveData("")

    companion object {
        private val EXCLUDED_PREFIXES = listOf("Etc/", "SystemV/", "US/", "Canada/", "Brazil/", "Chile/", "Mexico/")

        fun loadSystemTimeZones(nowMs: Long = System.currentTimeMillis(), locale: Locale = Locale.getDefault()): List<TimeZoneEntry> {
            return TimeZone.getAvailableIDs()
                .asSequence()
                .filter { it.contains('/') }
                .filter { id -> EXCLUDED_PREFIXES.none { id.startsWith(it) } }
                .filter { !it.startsWith("posix/") && !it.startsWith("right/") }
                .map { id ->
                    val tz = TimeZone.getTimeZone(id)
                    val offsetMs = tz.getOffset(nowMs)
                    val offsetMin = offsetMs / 60_000
                    val sign = if (offsetMin >= 0) "+" else "-"
                    val absMin = abs(offsetMin)
                    val offsetStr = String.format(Locale.ROOT, "UTC%s%02d:%02d", sign, absMin / 60, absMin % 60)

                    val rawCity = id.substringAfterLast('/').replace('_', ' ')
                    val localizedCity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        runCatching {
                            android.icu.text.TimeZoneNames.getInstance(locale)
                                .getExemplarLocationName(id)
                        }.getOrNull()?.takeIf { it.isNotBlank() } ?: rawCity
                    } else {
                        rawCity
                    }
                    val displayName = tz.getDisplayName(tz.inDaylightTime(java.util.Date(nowMs)), TimeZone.LONG, locale)
                    val subtitle = "$offsetStr · $id · $displayName"
                    val searchTokens = buildString {
                        append(localizedCity.lowercase(locale)).append(' ')
                        append(rawCity.lowercase(Locale.ROOT)).append(' ')
                        append(id.replace('_', ' ').lowercase(Locale.ROOT)).append(' ')
                        append(displayName.lowercase(locale)).append(' ')
                        append(offsetStr.lowercase(Locale.ROOT))
                    }
                    TimeZoneEntry(
                        id = id,
                        label = localizedCity,
                        subtitle = subtitle,
                        offsetMinutes = offsetMin,
                        searchTokens = searchTokens
                    )
                }
                .sortedWith(compareBy<TimeZoneEntry> { it.label.lowercase(locale) }.thenBy { it.offsetMinutes })
                .toList()
        }

        fun filterEntries(all: List<TimeZoneEntry>, query: String?, locale: Locale = Locale.getDefault()): List<TimeZoneEntry> {
            val trimmed = query?.trim().orEmpty()
            if (trimmed.isEmpty()) return all
            val parts = trimmed.lowercase(locale).split(Regex("\\s+")).filter { it.isNotEmpty() }
            return all.filter { entry ->
                parts.all { token -> entry.searchTokens.contains(token) }
            }
        }
    }
}
