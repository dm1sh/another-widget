package com.tommasoberlose.anotherwidget.ui.activities.tabs

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityTimeZoneSelectorBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.TimeZoneEntry
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.TimeZoneSelectorViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.idik.lib.slimadapter.SlimAdapter
import net.idik.lib.slimadapter.register

class TimeZoneSelectorActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: TimeZoneSelectorViewModel
    private lateinit var binding: ActivityTimeZoneSelectorBinding
    private var allEntries: List<TimeZoneEntry> = emptyList()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[TimeZoneSelectorViewModel::class.java]
        binding = ActivityTimeZoneSelectorBinding.inflate(layoutInflater)

        binding.listView.setHasFixedSize(true)
        binding.listView.layoutManager = LinearLayoutManager(this)

        adapter = SlimAdapter.create()
        adapter
            .register<String>(R.layout.custom_location_item) { _, injector ->
                injector
                    .text(R.id.text, getString(R.string.no_time_zone_label))
                    .clicked(R.id.item) {
                        clearSelectedTimeZone()
                    }
                    .clicked(R.id.text) {
                        clearSelectedTimeZone()
                    }
            }
            .register<TimeZoneEntry>(R.layout.custom_location_item) { item, injector ->
                val text = "${item.label} (${item.subtitle})"
                injector
                    .text(R.id.text, text)
                    .clicked(R.id.item) {
                        selectTimeZone(item)
                    }
                    .clicked(R.id.text) {
                        selectTimeZone(item)
                    }
            }
            .attachTo(binding.listView)

        setupListener()
        subscribeUi(binding, viewModel)

        binding.loader.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val loaded = TimeZoneSelectorViewModel.loadSystemTimeZones()
            withContext(Dispatchers.Main) {
                allEntries = loaded
                viewModel.entries.value = TimeZoneSelectorViewModel.filterEntries(loaded, viewModel.locationInput.value)
                binding.loader.visibility = View.INVISIBLE
            }
        }

        binding.location.requestFocus()
        setContentView(binding.root)
    }

    private fun clearSelectedTimeZone() {
        Preferences.bulk {
            altTimezoneId = ""
            altTimezoneLabel = ""
        }
        MainWidget.updateWidget(this)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun selectTimeZone(item: TimeZoneEntry) {
        Preferences.bulk {
            altTimezoneId = item.id
            altTimezoneLabel = item.label
        }
        MainWidget.updateWidget(this)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun subscribeUi(binding: ActivityTimeZoneSelectorBinding, viewModel: TimeZoneSelectorViewModel) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.entries.observe(this) { list ->
            adapter.updateData(listOf("Default") + list)
            binding.loader.visibility = View.INVISIBLE
        }

        viewModel.locationInput.observe(this) { query ->
            binding.clearSearch.isVisible = !query.isNullOrBlank()
            if (allEntries.isEmpty()) return@observe
            searchJob?.cancel()
            searchJob = lifecycleScope.launch(Dispatchers.Default) {
                delay(100)
                val filtered = TimeZoneSelectorViewModel.filterEntries(allEntries, query)
                withContext(Dispatchers.Main) {
                    viewModel.entries.value = filtered
                }
            }
        }
    }

    private fun setupListener() {
        binding.actionBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.clearSearch.setOnClickListener {
            viewModel.locationInput.value = ""
        }
    }
}
