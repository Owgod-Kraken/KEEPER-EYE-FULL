package com.keepereye.app

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.keepereye.app.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var prefs: SharedPreferences

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        prefs.edit().putBoolean(PREF_PERMISSIONS_REQUESTED, true).apply()
        navigateToMain()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        animateAndCheckPermissions()
    }

    private fun animateAndCheckPermissions() {
        val logoAlpha = ObjectAnimator.ofFloat(binding.ivSplashLogo, "alpha", 0f, 1f).apply {
            duration = 800
        }
        val logoScale = ObjectAnimator.ofFloat(binding.ivSplashLogo, "scaleX", 0.5f, 1f).apply {
            duration = 800
            interpolator = OvershootInterpolator(1.5f)
        }
        val logoScaleY = ObjectAnimator.ofFloat(binding.ivSplashLogo, "scaleY", 0.5f, 1f).apply {
            duration = 800
            interpolator = OvershootInterpolator(1.5f)
        }

        val nameAlpha = ObjectAnimator.ofFloat(binding.tvSplashName, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 400
        }
        val nameTranslate = ObjectAnimator.ofFloat(binding.tvSplashName, "translationY", 40f, 0f).apply {
            duration = 600
            startDelay = 400
        }

        val subtitleAlpha = ObjectAnimator.ofFloat(binding.tvSplashSubtitle, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 600
        }

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(logoAlpha, logoScale, logoScaleY, nameAlpha, nameTranslate, subtitleAlpha)
        animatorSet.start()

        binding.root.postDelayed({
            if (allPermissionsGranted()) {
                navigateToMain()
            } else {
                requestAllPermissions()
            }
        }, 2000)
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestAllPermissions() {
        val needed = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            navigateToMain()
        } else {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    companion object {
        private const val PREFS_NAME = "keeper_eye_prefs"
        private const val PREF_PERMISSIONS_REQUESTED = "permissions_requested"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}
