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
            delay(2200)
            navigateToMain()
        }
    }

    private fun startAnimations() {
        val cardLogo = findViewById<View>(R.id.cardLogo)
        val layoutBrandText = findViewById<View>(R.id.layoutBrandText)
        val layoutSplashFooter = findViewById<View>(R.id.layoutSplashFooter)

        val ivPawTopLeft = findViewById<View>(R.id.ivPawTopLeft)
        val ivPawTopRight = findViewById<View>(R.id.ivPawTopRight)
        val ivPawBottomLeft = findViewById<View>(R.id.ivPawBottomLeft)
        val ivPawBottomRight = findViewById<View>(R.id.ivPawBottomRight)
        val ivDecoHeart = findViewById<View>(R.id.ivDecoHeart)

        listOf(ivPawTopLeft, ivPawTopRight, ivPawBottomLeft, ivPawBottomRight, ivDecoHeart).forEach { view ->
            val targetAlpha = view.alpha
            view.alpha = 0f
            view.animate()
                .alpha(targetAlpha)
                .setDuration(800)
                .setStartDelay(150)
                .start()
        }

        cardLogo.scaleX = 0.6f
        cardLogo.scaleY = 0.6f
        cardLogo.alpha = 0f
        cardLogo.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        layoutBrandText.alpha = 0f
        layoutBrandText.translationY = 40f
        layoutBrandText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setStartDelay(250)
            .setInterpolator(DecelerateInterpolator())
            .start()

        layoutSplashFooter.alpha = 0f
        layoutSplashFooter.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(450)
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

