package com.keepereye.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.keepereye.app.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animateAndNavigate()
    }

    private fun animateAndNavigate() {
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
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }, 2500)
    }
}
