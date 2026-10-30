package com.tommasoberlose.anotherwidget.components

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.GlanceProviderSettingsLayoutBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.*
import com.tommasoberlose.anotherwidget.ui.activities.tabs.AppNotificationsFilterActivity
import com.tommasoberlose.anotherwidget.ui.activities.tabs.MediaInfoFormatActivity
import com.tommasoberlose.anotherwidget.ui.activities.tabs.MusicPlayersFilterActivity
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus

class GlanceSettingsDialog(
    val context: Activity,
    val provider: Constants.GlanceProviderId,
    private val requestHealthConnectPermission: (() -> Unit)? = null,
    private val statusCallback: (() -> Unit)?
) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    private var binding: GlanceProviderSettingsLayoutBinding =
        GlanceProviderSettingsLayoutBinding.inflate(LayoutInflater.from(context))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun dismiss() {
        scope.cancel()
        super.dismiss()
    }

    override fun show() {
        // Reset row visibilities before configuring for the selected provider
        binding.warningContainer.isVisible = false
        binding.header.isVisible = true
        binding.header.text = context.getString(R.string.preferences_header)
        binding.divider.isVisible = true
        binding.actionFilterMusicPlayers.isVisible = false
        binding.actionChangeMediaInfoFormat.isVisible = false
        binding.actionNextAlarmWindow.isVisible = false
        binding.alarmSetByContainer.isVisible = false
        binding.actionFilterNotificationsApp.isVisible = false
        binding.actionChangeNotificationTimer.isVisible = false
        binding.actionToggleGoogleFit.isVisible = false

        /* TITLE */
        binding.title.text = when (provider) {
            Constants.GlanceProviderId.PLAYING_SONG -> context.getString(R.string.settings_show_music_title)
            Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> context.getString(R.string.settings_show_next_alarm_title)
            Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> context.getString(R.string.settings_low_battery_level_title)
            Constants.GlanceProviderId.CUSTOM_INFO -> context.getString(R.string.settings_custom_notes_title)
            Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> context.getString(R.string.settings_daily_steps_title)
            Constants.GlanceProviderId.NOTIFICATIONS -> context.getString(R.string.settings_show_notifications_title)
            Constants.GlanceProviderId.GREETINGS -> context.getString(R.string.settings_show_greetings_title)
            Constants.GlanceProviderId.EVENTS -> context.getString(R.string.settings_show_events_as_glance_provider_title)
            Constants.GlanceProviderId.WEATHER -> context.getString(R.string.settings_show_weather_as_glance_provider_title)
        }

        /* SUBTITLE */
        binding.subtitle.text = when (provider) {
            Constants.GlanceProviderId.PLAYING_SONG -> context.getString(R.string.settings_show_music_subtitle)
            Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> context.getString(R.string.settings_show_next_alarm_subtitle)
            Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> context.getString(R.string.settings_low_battery_level_subtitle)
            Constants.GlanceProviderId.CUSTOM_INFO -> ""
            Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> context.getString(R.string.settings_daily_steps_subtitle)
            Constants.GlanceProviderId.NOTIFICATIONS -> context.getString(R.string.settings_show_notifications_subtitle)
            Constants.GlanceProviderId.GREETINGS -> context.getString(R.string.settings_show_greetings_subtitle)
            Constants.GlanceProviderId.EVENTS -> context.getString(R.string.settings_show_events_as_glance_provider_subtitle)
            Constants.GlanceProviderId.WEATHER -> context.getString(R.string.settings_show_weather_as_glance_provider_subtitle)
        }

        /* SONG */
        if (provider == Constants.GlanceProviderId.PLAYING_SONG) {
            binding.actionFilterMusicPlayers.isVisible = true
            binding.actionChangeMediaInfoFormat.isVisible = true
            binding.actionFilterMusicPlayers.setOnClickListener {
                dismiss()
                context.startActivity(Intent(context, MusicPlayersFilterActivity::class.java))
            }
            binding.actionChangeMediaInfoFormat.setOnClickListener {
                dismiss()
                context.startActivity(Intent(context, MediaInfoFormatActivity::class.java))
            }
            checkNotificationPermission()
        }

        /* ALARM */
        if (provider == Constants.GlanceProviderId.NEXT_CLOCK_ALARM) {
            binding.actionNextAlarmWindow.isVisible = true
            val currentWindow = Constants.NextAlarmWindow.fromInt(Preferences.nextAlarmWindow)
            binding.nextAlarmWindowLabel.text =
                context.getString(SettingsStringHelper.getNextAlarmWindowString(currentWindow))
            binding.actionNextAlarmWindow.setOnClickListener {
                val menu = BottomSheetMenu<Int>(
                    context,
                    header = context.getString(R.string.settings_next_alarm_window_title)
                ).setSelectedValue(Preferences.nextAlarmWindow)
                Constants.NextAlarmWindow.values().forEach { w ->
                    menu.addItem(context.getString(SettingsStringHelper.getNextAlarmWindowString(w)), w.rawValue)
                }
                menu.addOnSelectItemListener { value ->
                    Preferences.nextAlarmWindow = value
                    MainWidget.updateWidget(context)
                    statusCallback?.invoke()
                    this.show()
                }.show()
            }
            binding.warningContainer.isVisible = false
            checkNextAlarm()
        }

        /* BATTERY INFO */
        if (provider == Constants.GlanceProviderId.BATTERY_LEVEL_LOW) {
            binding.warningContainer.isVisible = false
            binding.header.isVisible = false
            binding.divider.isVisible = false
        }

        /* HEALTH CONNECT DAILY STEPS */
        if (provider == Constants.GlanceProviderId.GOOGLE_FIT_STEPS) {
            binding.actionToggleGoogleFit.isVisible = true
            binding.warningContainer.isVisible = false
            refreshHealthConnectUi()
        }

        /* NOTIFICATIONS */
        if (provider == Constants.GlanceProviderId.NOTIFICATIONS) {
            binding.actionFilterNotificationsApp.isVisible = true
            binding.actionChangeNotificationTimer.isVisible = true
            checkLastNotificationsPermission()
            val stringArray = context.resources.getStringArray(R.array.glance_notifications_timeout)
            binding.actionFilterNotificationsApp.setOnClickListener {
                dismiss()
                context.startActivity(Intent(context, AppNotificationsFilterActivity::class.java))
            }
            val timerIdx = Preferences.hideNotificationAfter.coerceIn(0, stringArray.lastIndex)
            binding.notificationTimerLabel.text = stringArray[timerIdx]
            binding.actionChangeNotificationTimer.setOnClickListener {
                val dialog = BottomSheetMenu<Int>(
                    context,
                    header = context.getString(R.string.glance_notification_hide_timeout_title)
                ).setSelectedValue(Preferences.hideNotificationAfter)
                Constants.GlanceNotificationTimer.values().forEachIndexed { index, timeout ->
                    dialog.addItem(stringArray[index], timeout.rawValue)
                }
                dialog.addOnSelectItemListener { value ->
                    Preferences.hideNotificationAfter = value
                    this.show()
                }.show()
            }
        }

        /* GREETINGS */
        if (provider == Constants.GlanceProviderId.GREETINGS) {
            binding.warningContainer.isVisible = false
            binding.header.isVisible = false
            binding.divider.isVisible = false
        }

        /* EVENTS */
        if (provider == Constants.GlanceProviderId.EVENTS) {
            binding.header.isVisible = false
            binding.divider.isVisible = false
            checkCalendarConfig()
        }

        /* WEATHER */
        if (provider == Constants.GlanceProviderId.WEATHER) {
            binding.header.isVisible = false
            binding.divider.isVisible = false
            checkWeatherConfig()
        }

        /* TOGGLE */
        binding.providerSwitch.setCheckedImmediatelyNoEvent(
            when (provider) {
                Constants.GlanceProviderId.PLAYING_SONG -> Preferences.showMusic
                Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> Preferences.showNextAlarm
                Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> Preferences.showBatteryCharging
                Constants.GlanceProviderId.CUSTOM_INFO -> true
                Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> Preferences.showDailySteps
                Constants.GlanceProviderId.NOTIFICATIONS -> Preferences.showNotifications
                Constants.GlanceProviderId.GREETINGS -> Preferences.showGreetings
                Constants.GlanceProviderId.EVENTS -> Preferences.showEventsAsGlanceProvider
                Constants.GlanceProviderId.WEATHER -> Preferences.showWeatherAsGlanceProvider
            }
        )

        var job: Job? = null

        binding.providerSwitch.setOnCheckedChangeListener { _, isChecked ->
            job?.cancel()
            job = scope.launch(Dispatchers.IO) {
                delay(200)
                withContext(Dispatchers.Main) {
                    when (provider) {
                        Constants.GlanceProviderId.PLAYING_SONG -> {
                            Preferences.showMusic = isChecked
                            checkNotificationPermission()
                        }
                        Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> {
                            Preferences.showNextAlarm = isChecked
                            checkNextAlarm()
                            if (!isChecked) {
                                AlarmHelper.clearTimeout(context)
                            }
                        }
                        Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> {
                            Preferences.showBatteryCharging = isChecked
                            BatteryHelper.updateBatteryInfo(context)
                            MainWidget.updateWidget(context)
                        }
                        Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> {
                            if (isChecked) {
                                val granted = withContext(Dispatchers.IO) {
                                    HealthConnectHelper.hasStepsPermission(context)
                                }
                                if (granted) {
                                    Preferences.showDailySteps = true
                                    HealthConnectHelper.updateDailySteps(context)
                                    refreshHealthConnectUi()
                                } else {
                                    requestHealthConnectPermission?.invoke()
                                }
                            } else {
                                Preferences.showDailySteps = false
                                Preferences.googleFitSteps = -1L
                                HealthConnectHelper.cancelPeriodicSync(context)
                                MainWidget.updateWidget(context)
                                refreshHealthConnectUi()
                            }
                        }
                        Constants.GlanceProviderId.NOTIFICATIONS -> {
                            Preferences.showNotifications = isChecked
                            checkLastNotificationsPermission()
                            if (!isChecked) {
                                ActiveNotificationsHelper.clearLastNotification(context)
                            }
                        }
                        Constants.GlanceProviderId.GREETINGS -> {
                            Preferences.showGreetings = isChecked
                            GreetingsHelper.toggleGreetings(context)
                        }
                        Constants.GlanceProviderId.EVENTS -> {
                            Preferences.showEventsAsGlanceProvider = isChecked
                            if (isChecked) {
                                com.tommasoberlose.anotherwidget.db.EventRepository(context).run {
                                    resetNextEventData()
                                    close()
                                }
                            }
                        }
                        Constants.GlanceProviderId.WEATHER -> {
                            Preferences.showWeatherAsGlanceProvider = isChecked
                        }
                        Constants.GlanceProviderId.CUSTOM_INFO -> {
                        }
                    }
                    statusCallback?.invoke()
                }
            }
        }

        setContentView(binding.root)
        behavior.run {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
        super.show()
    }

    fun refreshHealthConnectUi() {
        scope.launch {
            val granted = withContext(Dispatchers.IO) {
                HealthConnectHelper.hasStepsPermission(context)
            }
            binding.googleFitStatusLabel.text = if (granted) {
                if (Preferences.googleFitSteps >= 0) {
                    "%s (%s)".format(
                        context.getString(R.string.health_connect_connected),
                        context.getString(R.string.daily_steps_counter).format(Preferences.googleFitSteps)
                    )
                } else {
                    context.getString(R.string.health_connect_connected)
                }
            } else {
                context.getString(R.string.health_connect_not_connected)
            }
            binding.actionConnectToGoogleFit.isVisible = !granted
            binding.actionDisconnectToGoogleFit.isVisible = granted
            binding.actionDisconnectToGoogleFit.text = context.getString(R.string.action_manage)

            binding.actionConnectToGoogleFit.setOnClickListener {
                requestHealthConnectPermission?.invoke()
            }
            binding.actionDisconnectToGoogleFit.setOnClickListener {
                val manageIntent = IntentHelper.getFitIntent(context)
                if (manageIntent.action != null || manageIntent.component != null || manageIntent.`package` != null) {
                    try {
                        context.startActivity(manageIntent)
                    } catch (_: Exception) {
                    }
                }
            }
            binding.actionToggleGoogleFit.setOnClickListener {
                if (!granted) {
                    requestHealthConnectPermission?.invoke()
                } else {
                    val manageIntent = IntentHelper.getFitIntent(context)
                    try {
                        context.startActivity(manageIntent)
                    } catch (_: Exception) {
                    }
                }
            }
            binding.providerSwitch.setCheckedImmediatelyNoEvent(Preferences.showDailySteps && granted)
            statusCallback?.invoke()
        }
    }

    private fun checkNextAlarm() {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val alarm = nextAlarmClock
            if (alarm != null && alarm.showIntent != null) {
                val pm = context.packageManager as PackageManager
                val appNameOrPackage = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(alarm.showIntent?.creatorPackage ?: "", 0))
                } catch (_: Exception) {
                    alarm.showIntent?.creatorPackage ?: ""
                }
                binding.alarmSetByTitle.text =
                    context.getString(R.string.settings_show_next_alarm_app_title).format(appNameOrPackage)
                binding.alarmSetBySubtitle.text =
                    if (AlarmHelper.isAlarmProbablyWrong(context)) {
                        context.getString(R.string.settings_show_next_alarm_app_subtitle_wrong)
                    } else {
                        context.getString(R.string.settings_show_next_alarm_app_subtitle_correct)
                    }
                binding.alarmSetByContainer.isVisible = true
            } else {
                binding.alarmSetByContainer.isVisible = false
            }
            binding.header.isVisible = true
            binding.divider.isVisible = true
        }
        statusCallback?.invoke()
    }

    private fun checkCalendarConfig() {
        if (!Preferences.showEvents || !context.checkGrantedPermission(Manifest.permission.READ_CALENDAR)) {
            binding.warningContainer.isVisible = true
            binding.warningTitle.text = context.getString(R.string.settings_show_events_as_glance_provider_error)
            binding.warningContainer.setOnClickListener {
                dismiss()
                EventBus.getDefault().post(MainFragment.ChangeTabEvent(1))
            }
        } else {
            binding.warningContainer.isVisible = false
        }
    }

    private fun checkWeatherConfig() {
        if (!Preferences.showWeather ||
            (Preferences.weatherProviderError != "" && Preferences.weatherProviderError != "-") ||
            Preferences.weatherProviderLocationError != ""
        ) {
            binding.warningContainer.isVisible = true
            binding.warningTitle.text = context.getString(R.string.settings_show_weather_as_glance_provider_error)
            binding.warningContainer.setOnClickListener {
                dismiss()
                EventBus.getDefault().post(MainFragment.ChangeTabEvent(1))
            }
        } else {
            binding.warningContainer.isVisible = false
        }
    }

    private fun checkNotificationPermission() {
        when {
            ActiveNotificationsHelper.checkNotificationAccess(context) -> {
                binding.warningContainer.isVisible = false
                MediaPlayerHelper.updatePlayingMediaInfo(context)
            }
            Preferences.showMusic -> {
                binding.warningContainer.isVisible = true
                binding.warningTitle.text = context.getString(R.string.settings_request_notification_access)
                binding.warningContainer.setOnClickListener {
                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
            }
            else -> {
                binding.warningContainer.isVisible = false
            }
        }
        statusCallback?.invoke()
    }

    private fun checkLastNotificationsPermission() {
        when {
            ActiveNotificationsHelper.checkNotificationAccess(context) -> {
                binding.warningContainer.isVisible = false
            }
            Preferences.showNotifications -> {
                binding.warningContainer.isVisible = true
                binding.warningTitle.text = context.getString(R.string.settings_request_last_notification_access)
                binding.warningContainer.setOnClickListener {
                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
            }
            else -> {
                binding.warningContainer.isVisible = false
            }
        }
        statusCallback?.invoke()
    }
}
