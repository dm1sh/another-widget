package com.tommasoberlose.anotherwidget.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.os.ConfigurationCompat
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import java.util.*


object GreetingsHelper {
    private const val MORNING_TIME = 36
    private const val MORNING_TIME_END = 37
    private const val EVENING_TIME = 38
    private const val NIGHT_TIME = 39

    private const val PERIOD_NONE = 0
    private const val PERIOD_MORNING = 1
    private const val PERIOD_EVENING = 2
    private const val PERIOD_NIGHT = 3

    fun toggleGreetings(context: Context) {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val now = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.HOUR_OF_DAY, 0)
            }
            if (Preferences.showGreetings) {
                listOf(
                    MORNING_TIME to 5,
                    MORNING_TIME_END to 9,
                    EVENING_TIME to 19,
                    NIGHT_TIME to 22
                ).forEach { (reqCode, hourOfDay) ->
                    val triggerCal = (now.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DATE, 1)
                        }
                    }
                    setRepeating(
                        AlarmManager.RTC,
                        triggerCal.timeInMillis,
                        1000L * 60 * 60 * 24,
                        PendingIntent.getBroadcast(
                            context,
                            reqCode,
                            Intent(context, UpdatesReceiver::class.java).apply {
                                action = Actions.ACTION_UPDATE_GREETINGS
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            } else {
                listOf(MORNING_TIME, MORNING_TIME_END, EVENING_TIME, NIGHT_TIME).forEach {
                    cancel(
                        PendingIntent.getBroadcast(
                            context,
                            it,
                            Intent(context, UpdatesReceiver::class.java).apply {
                                action = Actions.ACTION_UPDATE_GREETINGS
                            },
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }
        }
    }

    fun showGreetings(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour < 9 || hour >= 19
    }

    private fun currentPeriod(): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 5..8 -> PERIOD_MORNING
            hour in 19..21 -> PERIOD_EVENING
            hour >= 22 || hour < 5 -> PERIOD_NIGHT
            else -> PERIOD_NONE
        }
    }

    fun clearCachedGreeting() {
        Preferences.greetingPeriod = PERIOD_NONE
        Preferences.greetingText = ""
    }

    /**
     * Returns a stable greeting string within the current time-of-day window and locale so
     * unrelated widget redraws (battery percentage updates, calendar/weather ticks) do not
     * reshuffle the random greeting every minute.
     */
    fun getRandomString(context: Context, forceRefresh: Boolean = false): String {
        val period = currentPeriod()
        if (period == PERIOD_NONE) {
            if (Preferences.greetingPeriod != PERIOD_NONE) {
                clearCachedGreeting()
            }
            return ""
        }
        val localeTag = ConfigurationCompat.getLocales(context.resources.configuration)[0]?.toLanguageTag()
            ?: Locale.getDefault().toLanguageTag()

        if (!forceRefresh &&
            Preferences.greetingPeriod == period &&
            Preferences.greetingLocale == localeTag &&
            Preferences.greetingText.isNotBlank()
        ) {
            return Preferences.greetingText
        }

        val array = when (period) {
            PERIOD_MORNING -> context.resources.getStringArray(R.array.morning_greetings)
            PERIOD_EVENING -> context.resources.getStringArray(R.array.evening_greetings)
            PERIOD_NIGHT -> context.resources.getStringArray(R.array.night_greetings)
            else -> emptyArray()
        }
        val chosen = if (array.isNotEmpty()) array[Random().nextInt(array.size)] else ""
        Preferences.greetingPeriod = period
        Preferences.greetingLocale = localeTag
        Preferences.greetingText = chosen
        return chosen
    }
}
