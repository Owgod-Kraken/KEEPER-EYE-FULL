package com.keepereye.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.keepereye.app.databinding.ActivityHistoryBinding
import com.keepereye.app.history.HistoryAdapter
import com.keepereye.app.history.HistoryRepository
import com.keepereye.app.tts.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyRepository: HistoryRepository
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var adapter: HistoryAdapter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        historyRepository = HistoryRepository(this)
        ttsManager = TextToSpeechManager(this)

        setupUI()
        loadHistory()
    }

    private fun setupUI() {
        adapter = HistoryAdapter { text ->
            ttsManager.speak(text)
        }

        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnClearHistory.setOnClickListener {
            scope.launch {
                withContext(Dispatchers.IO) {
                    historyRepository.clearHistory()
                }
                adapter.submitList(emptyList())
                binding.tvEmptyHistory.visibility = View.VISIBLE
                binding.recyclerHistory.visibility = View.GONE
            }
        }
    }

    private fun loadHistory() {
        scope.launch {
            val items = withContext(Dispatchers.IO) {
                historyRepository.getHistory()
            }
            if (items.isEmpty()) {
                binding.tvEmptyHistory.visibility = View.VISIBLE
                binding.recyclerHistory.visibility = View.GONE
            } else {
                binding.tvEmptyHistory.visibility = View.GONE
                binding.recyclerHistory.visibility = View.VISIBLE
                adapter.submitList(items)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        scope.cancel()
    }
}
