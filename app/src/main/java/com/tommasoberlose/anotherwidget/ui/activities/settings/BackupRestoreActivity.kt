package com.tommasoberlose.anotherwidget.ui.activities.settings

import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityBackupRestoreBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.BackupManager
import com.tommasoberlose.anotherwidget.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackupRestoreBinding

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            if (uri != null) {
                exportBackupToUri(uri)
            }
        }

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                inspectAndConfirmRestore(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeApiKeysToggle.setCheckedImmediatelyNoEvent(Preferences.backupIncludeApiKeys)

        binding.actionBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.actionIncludeApiKeys.setOnClickListener {
            binding.includeApiKeysToggle.isChecked = !binding.includeApiKeysToggle.isChecked
        }

        binding.includeApiKeysToggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.backupIncludeApiKeys = isChecked
        }

        binding.actionExportBackup.setOnClickListener {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
            createDocumentLauncher.launch("another-widget-backup-$stamp.json")
        }

        binding.actionImportBackup.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
    }

    private fun exportBackupToUri(uri: Uri) {
        val includeKeys = Preferences.backupIncludeApiKeys
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val json = BackupManager.exportToJson(this@BackupRestoreActivity, includeKeys)
                contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Unable to open output stream")
            }
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val msg = getString(R.string.backup_export_success)
                    binding.backupStatusText.isVisible = true
                    binding.backupStatusText.text = msg
                    toast(msg)
                } else {
                    val msg = getString(R.string.backup_export_error)
                    binding.backupStatusText.isVisible = true
                    binding.backupStatusText.text = msg
                    toast(msg)
                }
            }
        }
    }

    private fun inspectAndConfirmRestore(uri: Uri) {
        val restoreKeys = Preferences.backupIncludeApiKeys
        lifecycleScope.launch(Dispatchers.IO) {
            val textResult = runCatching {
                contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
            }
            val parsed = textResult.fold(
                onSuccess = { BackupManager.parseBackupJson(it, restoreKeys) },
                onFailure = { BackupManager.ParseResult.Invalid(it.message ?: "Read error") }
            )
            withContext(Dispatchers.Main) {
                when (parsed) {
                    is BackupManager.ParseResult.Invalid -> {
                        val msg = getString(R.string.backup_restore_invalid_error)
                        binding.backupStatusText.isVisible = true
                        binding.backupStatusText.text = msg
                        toast(msg)
                    }
                    is BackupManager.ParseResult.Valid -> {
                        val summary = parsed.summary
                        val dateText = if (summary.exportedAtMs > 0L) {
                            DateFormat.getMediumDateFormat(this@BackupRestoreActivity).format(Date(summary.exportedAtMs))
                        } else {
                            "-"
                        }
                        val confirmMessage = getString(R.string.backup_restore_confirm_message).format(
                            summary.values.size,
                            dateText
                        )
                        MaterialAlertDialogBuilder(this@BackupRestoreActivity)
                            .setTitle(R.string.backup_restore_action_title)
                            .setMessage(confirmMessage)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.action_restore) { _, _ ->
                                BackupManager.applyBackup(this@BackupRestoreActivity, summary)
                                val msg = getString(R.string.backup_restore_success).format(summary.values.size)
                                binding.backupStatusText.isVisible = true
                                binding.backupStatusText.text = msg
                                toast(msg)
                            }
                            .show()
                    }
                }
            }
        }
    }
}
