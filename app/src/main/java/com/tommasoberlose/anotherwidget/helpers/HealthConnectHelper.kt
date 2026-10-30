package com.tommasoberlose.anotherwidget.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object HealthConnectHelper {
    private const val STEPS_UPDATE_REQUEST_CODE = 24955
    private const val STEPS_REFRESH_INTERVAL_MS = 30 * 60 * 1000L

    val READ_STEPS_PERMISSION: String by lazy {
        HealthPermission.getReadPermission(StepsRecord::class)
    }

    val PERMISSIONS: Set<String> by lazy {
        setOf(READ_STEPS_PERMISSION)
    }

    fun isAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun hasStepsPermission(context: Context): Boolean {
        if (!isAvailable(context)) return false
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            granted.contains(READ_STEPS_PERMISSION)
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun syncDailySteps(context: Context): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !Preferences.showDailySteps || !isAvailable(context)) {
            Preferences.googleFitSteps = -1L
            cancelPeriodicSync(context)
            return -1L
        }
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.contains(READ_STEPS_PERMISSION)) {
                Preferences.showDailySteps = false
                Preferences.googleFitSteps = -1L
                cancelPeriodicSync(context)
                return -1L
            }
            val zone = ZoneId.systemDefault()
            val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            val now = Instant.now()
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            val totalSteps = response[StepsRecord.COUNT_TOTAL] ?: 0L
            Preferences.googleFitSteps = totalSteps
            schedulePeriodicSync(context)
            totalSteps
        } catch (_: Throwable) {
            Preferences.googleFitSteps
        }
    }

    fun updateDailySteps(context: Context) {
        if (!Preferences.showDailySteps || !isAvailable(context)) {
            cancelPeriodicSync(context)
            return
        }
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val before = Preferences.googleFitSteps
            val after = syncDailySteps(appContext)
            if (before != after) {
                MainWidget.updateWidget(appContext)
            }
        }
    }

    fun schedulePeriodicSync(context: Context) {
        if (!Preferences.showDailySteps || !isAvailable(context)) {
            cancelPeriodicSync(context)
            return
        }
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            STEPS_UPDATE_REQUEST_CODE,
            Intent(context, UpdatesReceiver::class.java).apply {
                action = Actions.ACTION_UPDATE_STEPS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + STEPS_REFRESH_INTERVAL_MS,
            STEPS_REFRESH_INTERVAL_MS,
            pi
        )
    }

    fun cancelPeriodicSync(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            STEPS_UPDATE_REQUEST_CODE,
            Intent(context, UpdatesReceiver::class.java).apply {
                action = Actions.ACTION_UPDATE_STEPS
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }
}
