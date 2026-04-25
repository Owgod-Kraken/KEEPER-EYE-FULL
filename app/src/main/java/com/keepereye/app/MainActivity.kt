package com.keepereye.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.keepereye.app.databinding.ActivityMainBinding
import com.keepereye.app.tts.TextToSpeechManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ttsManager: TextToSpeechManager
    private var pendingAssistMode = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navigateToScan(pendingAssistMode)
        } else {
            Toast.makeText(
                this,
                getString(R.string.camera_permission_denied),
                Toast.LENGTH_LONG
            ).show()
            ttsManager.speak(getString(R.string.camera_permission_denied))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsManager = TextToSpeechManager(this)

        setupUI()
    }

    private fun setupUI() {
        binding.btnStartScan.setOnClickListener {
            pendingAssistMode = false
            checkCameraPermissionAndStart()
        }

        binding.btnAssistMode.setOnClickListener {
            pendingAssistMode = true
            checkCameraPermissionAndStart()
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.btnStartScan.contentDescription = getString(R.string.start_scan_description)
        binding.btnAssistMode.contentDescription = getString(R.string.assist_mode_description)
        binding.btnHistory.contentDescription = getString(R.string.history_description)
    }

    private fun checkCameraPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                navigateToScan(pendingAssistMode)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun navigateToScan(assistMode: Boolean) {
        val intent = Intent(this, ScanActivity::class.java).apply {
            putExtra(ScanActivity.EXTRA_ASSIST_MODE, assistMode)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}
