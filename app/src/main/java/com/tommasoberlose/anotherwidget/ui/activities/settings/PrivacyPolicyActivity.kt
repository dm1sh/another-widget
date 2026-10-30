package com.tommasoberlose.anotherwidget.ui.activities.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommasoberlose.anotherwidget.R

/**
 * Displays the local privacy rationale required by Android Health Connect for
 * ACTION_SHOW_PERMISSIONS_RATIONALE and Android 14+ VIEW_PERMISSION_USAGE.
 */
class PrivacyPolicyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_privacy_policy_title)
            .setMessage(R.string.health_connect_privacy_rationale)
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
