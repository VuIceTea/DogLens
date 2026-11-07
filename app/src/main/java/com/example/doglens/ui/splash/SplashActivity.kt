package com.example.doglens.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.doglens.MainActivity
import com.example.doglens.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContentView(R.layout.activity_splash)

        val root = findViewById<View>(R.id.splashRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, statusBar.top, 0, navBar.bottom)
            insets
        }

        startAnimations()

        lifecycleScope.launch {
            delay(2000)
            navigateToMain()
        }
    }

    private fun startAnimations() {
        val ivSplashLogo = findViewById<View>(R.id.ivSplashLogo)
        val layoutBrandText = findViewById<View>(R.id.layoutBrandText)
        val layoutSplashFooter = findViewById<View>(R.id.layoutSplashFooter)

        ivSplashLogo.scaleX = 0.65f
        ivSplashLogo.scaleY = 0.65f
        ivSplashLogo.alpha = 0f
        ivSplashLogo.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(750)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        layoutBrandText.alpha = 0f
        layoutBrandText.translationY = 35f
        layoutBrandText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(650)
            .setStartDelay(200)
            .setInterpolator(DecelerateInterpolator())
            .start()

        layoutSplashFooter.alpha = 0f
        layoutSplashFooter.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(350)
            .start()
    }

    private fun navigateToMain() {
        if (isFinishing || isDestroyed) return

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        finish()
    }
}
