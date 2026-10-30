package com.tommasoberlose.anotherwidget.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.text.format.DateFormat
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.utils.setExactIfCanSchedule
import java.text.SimpleDateFormat
import java.util.*

object AlarmHelper {

    fun getNextAlarm(context: Context): String = with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
        val alarm = nextAlarmClock ?: run {
            clearTimeout(context)
            return ""
        }
        val nowMs = System.currentTimeMillis()
        if (alarm.triggerTime <= nowMs) {
            clearTimeout(context)
            return ""
        }
        if (!isValidClockAlarm(context, alarm)) {
            clearTimeout(context)
            return ""
        }

        val window = Constants.NextAlarmWindow.fromInt(Preferences.nextAlarmWindow)
        if (window != Constants.NextAlarmWindow.ALWAYS) {
            val windowMs = window.rawValue * 60 * 1000L
            val diffMs = alarm.triggerTime - nowMs
            if (diffMs > windowMs) {
                // Schedule an update when the alarm enters the configured lookahead window
                setTimeout(context, alarm.triggerTime - windowMs + 1_000L)
                return ""
            }
        }

        setTimeout(context, alarm.triggerTime)
        return "%s %s".format(
            SimpleDateFormat("EEE", Locale.getDefault()).format(alarm.triggerTime),
            DateFormat.getTimeFormat(context).format(Date(alarm.triggerTime))
        )
    }

    /**
     * Verifies whether the AlarmClockInfo comes from an actual clock/alarm application
     * rather than a task automation tool, reminder app, or sleep tracker abusing setAlarmClock().
     */
    fun isValidClockAlarm(context: Context, alarm: AlarmManager.AlarmClockInfo): Boolean {
        val creatorPkg = alarm.showIntent?.creatorPackage ?: return true
        // If user explicitly configured this package as their clock app, always trust it
        if (Preferences.clockAppPackage.isNotBlank() &&
            Preferences.clockAppPackage != IntentHelper.DO_NOTHING_OPTION &&
            Preferences.clockAppPackage != IntentHelper.REFRESH_WIDGET_OPTION &&
            Preferences.clockAppPackage == creatorPkg
        ) {
            return true
        }
        val clockPackages = resolveClockPackages(context)
        // If package visibility returns no clock packages at all, fail open rather than hiding real alarms
        if (clockPackages.isEmpty()) return true
        return clockPackages.contains(creatorPkg)
    }

    private fun resolveClockPackages(context: Context): Set<String> {
        val pm = context.packageManager
        val result = mutableSetOf<String>()
        listOf(AlarmClock.ACTION_SHOW_ALARMS, AlarmClock.ACTION_SET_ALARM).forEach { action ->
            try {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(Intent(action), PackageManager.MATCH_ALL).forEach { info ->
                    info.activityInfo?.packageName?.let { result.add(it) }
                }
            } catch (_: Exception) {
            }
        }
        return result
    }

    fun isAlarmProbablyWrong(context: Context): Boolean {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val alarm = nextAlarmClock ?: return false
            val diff = alarm.triggerTime - System.currentTimeMillis()
            // Flag suspiciously fake alarms (e.g. Tasker/Xiaomi persistent 1-minute rolling triggers from non-clock apps)
            return diff in 1..(5 * 60 * 1000L) && !isValidClockAlarm(context, alarm)
        }
    }

    private fun setTimeout(context: Context, trigger: Long) {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val intent = Intent(context, UpdatesReceiver::class.java).apply {
                action = Actions.ACTION_ALARM_UPDATE
            }
            setExactIfCanSchedule(
                AlarmManager.RTC,
                trigger,
                PendingIntent.getBroadcast(
                    context,
                    ALARM_UPDATE_ID,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
    }

    fun clearTimeout(context: Context) {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val intent = Intent(context, UpdatesReceiver::class.java).apply {
                action = Actions.ACTION_ALARM_UPDATE
            }
            cancel(PendingIntent.getBroadcast(context, ALARM_UPDATE_ID, intent, PendingIntent.FLAG_IMMUTABLE))
        }
    }

    private const val ALARM_UPDATE_ID = 24953
}
