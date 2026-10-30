package com.tommasoberlose.anotherwidget.helpers

import android.content.ContentResolver
import android.content.Context
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.blockingBulk
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.NotificationListener
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget

object ActiveNotificationsHelper {
    fun showLastNotification(context: Context): Boolean {
        if (!checkNotificationAccess(context)) {
            if (Preferences.lastNotificationId != -1 ||
                Preferences.lastNotificationTitle.isNotEmpty() ||
                Preferences.lastNotificationPackage.isNotEmpty()
            ) {
                clearLastNotificationState(context)
            }
            return false
        }
        return Preferences.lastNotificationId != -1 &&
            Preferences.lastNotificationPackage.isNotBlank() &&
            Preferences.lastNotificationTitle.isNotBlank()
    }

    fun clearLastNotificationState(context: Context) {
        Kotpref.init(context)
        Preferences.blockingBulk {
            remove(Preferences::lastNotificationId)
            remove(Preferences::lastNotificationTitle)
            remove(Preferences::lastNotificationPackage)
            remove(Preferences::lastNotificationIcon)
        }
        NotificationListener.clearTimeout(context)
    }

    fun clearLastNotification(context: Context) {
        clearLastNotificationState(context)
        MainWidget.updateWidget(context)
    }

    /**
     * Resolves the notifying app's icon.
     * Returns Pair(drawable, isColoredAppIcon). When isColoredAppIcon is true, widget renderers
     * skip monochrome secondary font tinting so full-color app icons remain recognizable.
     */
    fun getLastNotificationIcon(context: Context): Pair<Drawable?, Boolean> {
        val pkg = Preferences.lastNotificationPackage
        if (pkg.isBlank()) return null to false
        try {
            val appIcon = context.packageManager.getApplicationIcon(pkg)
            return appIcon to true
        } catch (_: Exception) {
        }
        if (Preferences.lastNotificationIcon != 0) {
            try {
                val remotePackageContext = context.createPackageContext(pkg, 0)
                val smallIcon = ContextCompat.getDrawable(
                    remotePackageContext,
                    Preferences.lastNotificationIcon
                )
                return smallIcon to false
            } catch (_: Exception) {
            }
        }
        return null to false
    }

    fun checkNotificationAccess(context: Context): Boolean {
        return try {
            val contentResolver: ContentResolver = context.contentResolver
            val enabledNotificationListeners =
                Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            val packageName: String = context.packageName
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(packageName) &&
                (enabledNotificationListeners != null && enabledNotificationListeners.contains(NotificationListener::class.java.name))
        } catch (_: Exception) {
            false
        }
    }

    fun isAppAccepted(appPkg: String): Boolean {
        if (Preferences.appNotificationsFilter.isBlank()) return true
        return Preferences.appNotificationsFilter
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .contains(appPkg)
    }

    fun toggleAppFilter(appPkg: String) {
        val current = Preferences.appNotificationsFilter
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableSet()
        if (!current.add(appPkg)) {
            current.remove(appPkg)
        }
        Preferences.appNotificationsFilter = current.joinToString(separator = ",")
    }
}
