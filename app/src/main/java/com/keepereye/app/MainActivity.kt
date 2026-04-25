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
    private var pendingTarget: Class<*>? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingTarget?.let { navigateTo(it) }
        } else {
            Toast.makeText(
                this,
                getString(R.string.camera_permission_denied),
                Toast.LENGTH_LONG
            ).show()
            ttsManager.speak(getString(R.string.camera_permission_denied))
        }
        pendingTarget = null
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
            checkCameraPermissionAndStart(ScanActivity::class.java)
        }

        binding.btnObstacles.setOnClickListener {
            checkCameraPermissionAndStart(ObstacleActivity::class.java)
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.btnStartScan.contentDescription = getString(R.string.start_scan_description)
        binding.btnObstacles.contentDescription = getString(R.string.obstacle_detector_description)
        binding.btnHistory.contentDescription = getString(R.string.history_description)
    }

    private fun checkCameraPermissionAndStart(target: Class<*>) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                navigateTo(target)
            }
            else -> {
                pendingTarget = target
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun navigateTo(target: Class<*>) {
        startActivity(Intent(this, target))
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}
