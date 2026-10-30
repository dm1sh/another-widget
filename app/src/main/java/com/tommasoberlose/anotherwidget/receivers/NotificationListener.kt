package com.tommasoberlose.anotherwidget.receivers

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.session.MediaSession
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.ActiveNotificationsHelper
import com.tommasoberlose.anotherwidget.helpers.BatteryHelper
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.setExactIfCanSchedule
import java.util.*

class NotificationListener : NotificationListenerService() {

    private var batteryChangedReceiver: BroadcastReceiver? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaPlayerHelper.updatePlayingMediaInfo(this)
        pruneStaleCachedNotification()
        registerBatteryChangedReceiver()
    }

    override fun onListenerDisconnected() {
        unregisterBatteryChangedReceiver()
        MediaPlayerHelper.updatePlayingMediaInfo(this)
        ActiveNotificationsHelper.clearLastNotification(this)
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        unregisterBatteryChangedReceiver()
        super.onDestroy()
    }

    private fun pruneStaleCachedNotification() {
        if (Preferences.lastNotificationId == -1 && Preferences.lastNotificationPackage.isBlank()) {
            return
        }
        val stillPresent = try {
            activeNotifications?.any {
                it.id == Preferences.lastNotificationId && it.packageName == Preferences.lastNotificationPackage
            } == true
        } catch (_: Exception) {
            false
        }
        if (!stillPresent) {
            ActiveNotificationsHelper.clearLastNotification(this)
        }
    }

    private fun registerBatteryChangedReceiver() {
        if (batteryChangedReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED && Preferences.showBatteryCharging) {
                    if (BatteryHelper.onDynamicBatteryChanged(context)) {
                        MainWidget.updateWidget(context)
                    }
                }
            }
        }
        try {
            ContextCompat.registerReceiver(
                this,
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            batteryChangedReceiver = receiver
        } catch (_: Exception) {
        }
    }

    private fun unregisterBatteryChangedReceiver() {
        val receiver = batteryChangedReceiver ?: return
        batteryChangedReceiver = null
        try {
            unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.notification?.extras?.let { bundle ->
            @Suppress("DEPRECATION")
            val mediaToken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
            } else {
                bundle.getParcelable(Notification.EXTRA_MEDIA_SESSION)
            }
            if (mediaToken != null) {
                if (Preferences.showMusic) {
                    MediaPlayerHelper.updatePlayingMediaInfo(this)
                }
            } else {
                val flags = sbn.notification.flags
                val isGroupHeader = flags and Notification.FLAG_GROUP_SUMMARY != 0
                val isOngoing = flags and Notification.FLAG_ONGOING_EVENT != 0
                val isForegroundService = flags and Notification.FLAG_FOREGROUND_SERVICE != 0
                val isNoClear = flags and Notification.FLAG_NO_CLEAR != 0
                val isSystemPackage = sbn.packageName == "android" || sbn.packageName.contains("com.android.systemui")

                var title = bundle.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
                if ((sbn.packageName == "com.whatsapp" || sbn.packageName == "com.whatsapp.w4b") &&
                    title.startsWith("WhatsApp: ", ignoreCase = true)
                ) {
                    title = title.substring("WhatsApp: ".length).trim()
                }

                if (Preferences.showNotifications &&
                    title.isNotEmpty() &&
                    !isGroupHeader &&
                    !isOngoing &&
                    !isForegroundService &&
                    !isNoClear &&
                    !isSystemPackage &&
                    ActiveNotificationsHelper.isAppAccepted(sbn.packageName)
                ) {
                    Preferences.lastNotificationId = sbn.id
                    Preferences.lastNotificationTitle = title
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            Preferences.lastNotificationIcon = sbn.notification.smallIcon.resId
                        } else {
                            @Suppress("DEPRECATION")
                            Preferences.lastNotificationIcon = sbn.notification.icon
                        }
                    } catch (_: Exception) {
                        Preferences.lastNotificationIcon = 0
                    }
                    Preferences.lastNotificationPackage = sbn.packageName
                    MainWidget.updateWidget(this)
                    setTimeout(this)
                }
            }
        }

        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (Preferences.showMusic) {
            MediaPlayerHelper.updatePlayingMediaInfo(this)
        }
        sbn?.let {
            if (Preferences.showNotifications &&
                sbn.id == Preferences.lastNotificationId &&
                sbn.packageName == Preferences.lastNotificationPackage
            ) {
                ActiveNotificationsHelper.clearLastNotification(this)
            }
        }
        super.onNotificationRemoved(sbn)
    }

    private fun setTimeout(context: Context) {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val intent = Intent(context, UpdatesReceiver::class.java).apply {
                action = Actions.ACTION_CLEAR_NOTIFICATION
            }
            val timeoutPref = Constants.GlanceNotificationTimer.fromInt(Preferences.hideNotificationAfter)
            if (timeoutPref != Constants.GlanceNotificationTimer.WHEN_DISMISSED) {
                setExactIfCanSchedule(
                    AlarmManager.RTC,
                    Calendar.getInstance().timeInMillis + when (timeoutPref) {
                        Constants.GlanceNotificationTimer.HALF_MINUTE -> 30 * 1000L
                        Constants.GlanceNotificationTimer.ONE_MINUTE -> 60 * 1000L
                        Constants.GlanceNotificationTimer.FIVE_MINUTES -> 5 * 60 * 1000L
                        Constants.GlanceNotificationTimer.TEN_MINUTES -> 10 * 60 * 1000L
                        Constants.GlanceNotificationTimer.FIFTEEN_MINUTES -> 15 * 60 * 1000L
                        Constants.GlanceNotificationTimer.WHEN_DISMISSED -> 60 * 1000L
                    },
                    PendingIntent.getBroadcast(
                        context,
                        5,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        }
    }

    companion object {
        fun clearTimeout(context: Context) {
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                val intent = Intent(context, UpdatesReceiver::class.java).apply {
                    action = Actions.ACTION_CLEAR_NOTIFICATION
                }
                cancel(PendingIntent.getBroadcast(context, 5, intent, PendingIntent.FLAG_IMMUTABLE))
            }
        }
    }
}
