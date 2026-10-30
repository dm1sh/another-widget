package com.tommasoberlose.anotherwidget.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver

object BatteryHelper {
    private const val BATTERY_POLL_REQUEST_CODE = 24954
    private const val BATTERY_POLL_INTERVAL_MS = 2 * 60 * 1000L
    private const val MIN_DYNAMIC_UPDATE_INTERVAL_MS = 45 * 1000L

    fun updateBatteryInfo(context: Context) {
        val level = getBatteryLevel(context)
        val charging = isChargingLive(context)
        if (level in 0..100) {
            Preferences.isBatteryLevelLow = level <= 15
            Preferences.lastBatteryLevel = level
        }
        Preferences.isCharging = charging
        if (Preferences.showBatteryCharging && (charging || Preferences.isBatteryLevelLow)) {
            schedulePoll(context)
        } else {
            cancelPoll(context)
        }
    }

    fun onDynamicBatteryChanged(context: Context): Boolean {
        val now = System.currentTimeMillis()
        val level = getBatteryLevel(context)
        val charging = isChargingLive(context)
        val chargingChanged = charging != Preferences.isCharging
        val levelChanged = level in 0..100 && level != Preferences.lastBatteryLevel
        if (!chargingChanged && !levelChanged) return false
        if (!chargingChanged && now - Preferences.lastBatteryUpdateMs < MIN_DYNAMIC_UPDATE_INTERVAL_MS) {
            return false
        }
        Preferences.lastBatteryUpdateMs = now
        updateBatteryInfo(context)
        return true
    }

    private fun isChargingLive(context: Context): Boolean {
        try {
            val bm = context.getSystemService(BATTERY_SERVICE) as? BatteryManager
            if (bm != null && bm.isCharging) return true
        } catch (_: Exception) {
        }
        return try {
            val sticky = context.applicationContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL &&
                (sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        } catch (_: Exception) {
            Preferences.isCharging
        }
    }

    fun getBatteryLevel(context: Context): Int {
        try {
            val bm = context.getSystemService(BATTERY_SERVICE) as? BatteryManager
            val prop = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (prop in 0..100) return prop
        } catch (_: Exception) {
        }
        return try {
            val sticky = context.applicationContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100) / scale else 100
        } catch (_: Exception) {
            100
        }
    }

    fun schedulePoll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            BATTERY_POLL_REQUEST_CODE,
            Intent(context, UpdatesReceiver::class.java).apply {
                action = Actions.ACTION_TIME_UPDATE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.set(AlarmManager.RTC, System.currentTimeMillis() + BATTERY_POLL_INTERVAL_MS, pi)
    }

    fun cancelPoll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            BATTERY_POLL_REQUEST_CODE,
            Intent(context, UpdatesReceiver::class.java).apply {
                action = Actions.ACTION_TIME_UPDATE
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }
}
