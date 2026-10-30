package com.tommasoberlose.anotherwidget.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import androidx.health.connect.client.HealthConnectClient
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import java.util.*


object IntentHelper {

    const val DEFAULT_OPTION = ""
    const val DO_NOTHING_OPTION = "DO_NOTHING"
    const val REFRESH_WIDGET_OPTION = "REFRESH_WIDGET"

    fun getPendingIntent(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent {
        return if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK == Intent.FLAG_ACTIVITY_NEW_TASK)
            PendingIntent.getActivity(context, requestCode, intent, flags)
        else
            PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    fun getWidgetUpdateIntent(context: Context): Intent {
        val widgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, MainWidget::class.java)
        val widgetIds = widgetManager.getAppWidgetIds(widgetComponent)
        return Intent(context, MainWidget::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
    }

    private fun getWidgetRefreshIntent(context: Context): Intent {
        return Intent(context, UpdatesReceiver::class.java).apply {
            action = Actions.ACTION_REFRESH
        }
    }

    fun getGoogleMapsIntentFromAddress(context: Context, address: String): Intent {
        val gmmIntentUri: Uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

        return if (mapIntent.resolveActivity(context.packageManager) != null) {
            mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            val map = "https://www.openstreetmap.org/search?query=${Uri.encode(address)}"
            Intent(Intent.ACTION_VIEW, Uri.parse(map)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getWeatherIntent(context: Context): Intent {
        return when (Preferences.weatherAppPackage) {
            DEFAULT_OPTION -> {
                Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    data = Uri.parse("dynact://velour/weather/ProxyActivity")
                    component = ComponentName(
                        "com.google.android.googlequicksearchbox",
                        "com.google.android.apps.gsa.velour.DynamicActivityTrampoline"
                    )
                    setClassName(
                        "com.google.android.googlequicksearchbox",
                        "com.google.android.apps.gsa.velour.DynamicActivityTrampoline"
                    )
                }
            }
            DO_NOTHING_OPTION -> {
                Intent()
            }
            REFRESH_WIDGET_OPTION -> {
                getWidgetRefreshIntent(context)
            }
            else -> {
                val pm: PackageManager = context.packageManager
                try {
                    pm.getLaunchIntentForPackage(Preferences.weatherAppPackage)!!.apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                } catch (e: Exception) {
                    Intent()
                }
            }
        }
    }

    fun getCalendarIntent(context: Context, time: Long? = null): Intent {
        val calendarUri = CalendarContract.CONTENT_URI
            .buildUpon()
            .appendPath("time")
            .appendPath((time ?: Calendar.getInstance().timeInMillis).toString())
            .build()
        val pm: PackageManager = context.packageManager
        return when (Preferences.calendarAppPackage) {
            DEFAULT_OPTION -> {
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    data = calendarUri
                }
                if (viewIntent.resolveActivity(pm) != null) {
                    viewIntent
                } else {
                    val categoryIntent = Intent.makeMainSelectorActivity(
                        Intent.ACTION_MAIN,
                        Intent.CATEGORY_APP_CALENDAR
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (categoryIntent.resolveActivity(pm) != null) categoryIntent else Intent()
                }
            }
            DO_NOTHING_OPTION -> {
                Intent()
            }
            REFRESH_WIDGET_OPTION -> {
                getWidgetRefreshIntent(context)
            }
            else -> {
                try {
                    val targetedViewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setPackage(Preferences.calendarAppPackage)
                        data = calendarUri
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (targetedViewIntent.resolveActivity(pm) != null) {
                        targetedViewIntent
                    } else {
                        pm.getLaunchIntentForPackage(Preferences.calendarAppPackage)!!.apply {
                            addCategory(Intent.CATEGORY_LAUNCHER)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    }
                } catch (e: Exception) {
                    Intent()
                }
            }
        }
    }

    fun getEventIntent(context: Context, e: Event, forceEventDetails: Boolean = false): Intent {
        if (!forceEventDetails) {
            if (Preferences.calendarAppPackage == DO_NOTHING_OPTION) return Intent()
            if (Preferences.calendarAppPackage == REFRESH_WIDGET_OPTION) return getWidgetRefreshIntent(context)
        }
        return when (Preferences.openEventDetails || forceEventDetails) {
            true -> {
                val uri = ContentUris.withAppendedId(Events.CONTENT_URI, e.eventID)
                val eventIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = uri
                    flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    if (Preferences.calendarAppPackage != DEFAULT_OPTION &&
                        Preferences.calendarAppPackage != DO_NOTHING_OPTION &&
                        Preferences.calendarAppPackage != REFRESH_WIDGET_OPTION
                    ) {
                        setPackage(Preferences.calendarAppPackage)
                    }
                    if (!e.allDay) {
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, e.startDate)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, e.endDate)
                    } else {
                        val start = Calendar.getInstance().apply {
                            timeInMillis = e.startDate
                        }
                        val end = Calendar.getInstance().apply {
                            timeInMillis = e.endDate
                        }
                        putExtra(
                            CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                            start.timeInMillis + start.timeZone.getOffset(start.timeInMillis)
                        )
                        putExtra(
                            CalendarContract.EXTRA_EVENT_END_TIME,
                            end.timeInMillis + end.timeZone.getOffset(end.timeInMillis)
                        )
                        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, 1)
                    }
                }
                if (eventIntent.resolveActivity(context.packageManager) != null) {
                    eventIntent
                } else {
                    getCalendarIntent(context, e.startDate)
                }
            }
            false -> {
                getCalendarIntent(context, e.startDate)
            }
        }
    }

    fun getClockIntent(context: Context): Intent {
        return when (Preferences.clockAppPackage) {
            DEFAULT_OPTION -> {
                Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            DO_NOTHING_OPTION -> {
                Intent()
            }
            REFRESH_WIDGET_OPTION -> {
                getWidgetRefreshIntent(context)
            }
            else -> {
                val pm: PackageManager = context.packageManager
                try {
                    pm.getLaunchIntentForPackage(Preferences.clockAppPackage)!!.apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                } catch (e: Exception) {
                    Intent()
                }
            }
        }
    }

    fun getBatteryIntent(): Intent {
        return Intent(Intent.ACTION_POWER_USAGE_SUMMARY).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun getMusicIntent(context: Context): Intent {
        return when (Preferences.mediaPlayerPackage) {
            DO_NOTHING_OPTION -> {
                Intent()
            }
            else -> {
                val pm: PackageManager = context.packageManager
                try {
                    pm.getLaunchIntentForPackage(Preferences.mediaPlayerPackage)!!.apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                } catch (e: Exception) {
                    Intent()
                }
            }
        }
    }

    fun getFitIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hcIntent = HealthConnectClient.getHealthConnectManageDataIntent(context).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (hcIntent.resolveActivity(context.packageManager) != null) {
                    return hcIntent
                }
            } catch (_: Exception) {
            }
        }
        return Intent()
    }

    fun getNotificationIntent(context: Context): Intent {
        val pm: PackageManager = context.packageManager
        return try {
            pm.getLaunchIntentForPackage(Preferences.lastNotificationPackage)!!.apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            Intent()
        }
    }
}
