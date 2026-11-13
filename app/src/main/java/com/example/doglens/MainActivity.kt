package com.example.doglens

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.doglens.ui.home.HomeFragment
import com.example.doglens.ui.explore.ExploreFragment
import com.example.doglens.ui.favorite.FavoriteFragment
import com.example.doglens.ui.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var currentTabId: Int = R.id.nav_home
    private var currentColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContentView(R.layout.activity_main)

        val bottomNavigation =
            findViewById<BottomNavigationView>(
                R.id.bottomNavigation
            )

        bottomNavigation.setItemActiveIndicatorEnabled(false)

        val root = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        val originalBottomPadding = bottomNavigation.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = originalBottomPadding + bars.bottom)
            insets
        }

        currentColor = ContextCompat.getColor(this, R.color.home_background)

        if (savedInstanceState == null) {
            showFragment(HomeFragment(), animate = false)
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == currentTabId) return@setOnItemSelectedListener true

            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_explore -> ExploreFragment()
                R.id.nav_favorite -> FavoriteFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> return@setOnItemSelectedListener false
            }

            val isMovingRight = getTabIndex(item.itemId) > getTabIndex(currentTabId)
            currentTabId = item.itemId
            showFragment(fragment, isMovingRight = isMovingRight, animate = true)
            true
        }
    }

    private fun getTabIndex(itemId: Int): Int = when (itemId) {
        R.id.nav_home -> 0
        R.id.nav_explore -> 1
        R.id.nav_favorite -> 2
        R.id.nav_profile -> 3
        else -> 0
    }

    private fun showFragment(
        fragment: Fragment,
        isMovingRight: Boolean = true,
        animate: Boolean = true
    ) {
        val backgroundColor = when (fragment) {
            is HomeFragment -> R.color.home_background
            else -> R.color.explore_background
        }

        val targetColor = ContextCompat.getColor(this, backgroundColor)

        if (animate && currentColor != targetColor) {
            ValueAnimator.ofObject(ArgbEvaluator(), currentColor, targetColor).apply {
                duration = 250
                addUpdateListener { animator ->
                    val animatedColor = animator.animatedValue as Int
                    findViewById<View>(R.id.main).setBackgroundColor(animatedColor)
                    window.statusBarColor = animatedColor
                }
                start()
            }
        } else {
            findViewById<View>(R.id.main).setBackgroundColor(targetColor)
            window.statusBarColor = targetColor
        }

        currentColor = targetColor

        val transaction = supportFragmentManager.beginTransaction()
        if (animate) {
            if (isMovingRight) {
                transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            } else {
                transaction.setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
            }
        }

        transaction.replace(R.id.fragmentContainer, fragment).commit()
    }

    fun goToHome() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.visibility = View.VISIBLE

        // Tích chọn tab Trang chủ mà không kích hoạt listener
        bottomNavigation.menu.findItem(R.id.nav_home).isChecked = true
        currentTabId = R.id.nav_home

        showFragment(HomeFragment(), animate = false)
    }
}
