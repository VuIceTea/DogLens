package com.example.doglens

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import com.example.doglens.ui.auth.ForgotPasswordFragment
import com.example.doglens.ui.auth.LoginFragment
import com.example.doglens.ui.auth.RegisterFragment
import com.example.doglens.ui.home.HomeFragment
import com.example.doglens.ui.explore.ExploreFragment
import com.example.doglens.ui.favorite.FavoriteFragment
import com.example.doglens.ui.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var currentTabId: Int = R.id.nav_home
    private var currentColor: Int = 0
    private var colorAnimator: ValueAnimator? = null

    @ColorRes
    private var screenBackgroundRes: Int = R.color.identify_background

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var root: View

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

        bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.setItemActiveIndicatorEnabled(false)
        root = findViewById(R.id.main)

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

        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager,
                    fragment: Fragment,
                    view: View,
                    savedInstanceState: Bundle?
                ) {
                    if (fragment.id == R.id.fragmentContainer) updateBottomNavigation(fragment)
                }
            },
            false
        )
        updateBottomNavigation(supportFragmentManager.findFragmentById(R.id.fragmentContainer))

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

        supportFragmentManager.addOnBackStackChangedListener {
            val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            val isTab = updateBottomNavigation(current)
            val background = when (current) {
                is LoginFragment, is RegisterFragment -> R.color.home_background
                is ForgotPasswordFragment -> R.color.forgot_password_background
                else -> if (isTab) tabBackground(currentTabId) else screenBackgroundRes
            }
            applyBackground(background, animate = isTab)
        }
    }

    private fun updateBottomNavigation(fragment: Fragment?): Boolean {
        val tabId = when (fragment) {
            is HomeFragment -> R.id.nav_home
            is ExploreFragment -> R.id.nav_explore
            is FavoriteFragment -> R.id.nav_favorite
            is ProfileFragment -> R.id.nav_profile
            else -> null
        }
        if (tabId != null) {
            currentTabId = tabId
            bottomNavigation.menu.findItem(tabId).isChecked = true
        }
        bottomNavigation.visibility = if (tabId != null) View.VISIBLE else View.GONE
        return tabId != null
    }

    private fun getTabIndex(itemId: Int): Int = when (itemId) {
        R.id.nav_home -> 0
        R.id.nav_explore -> 1
        R.id.nav_favorite -> 2
        R.id.nav_profile -> 3
        else -> 0
    }

    @ColorRes
    private fun tabBackground(tabId: Int): Int =
        if (tabId == R.id.nav_home) R.color.home_background else R.color.explore_background

    private fun applyBackground(@ColorRes colorRes: Int, animate: Boolean) {
        val targetColor = ContextCompat.getColor(this, colorRes)

        colorAnimator?.cancel()
        colorAnimator = null

        if (animate && currentColor != targetColor) {
            colorAnimator =
                ValueAnimator.ofObject(ArgbEvaluator(), currentColor, targetColor).apply {
                    duration = 250
                    addUpdateListener { animator ->
                        val animatedColor = animator.animatedValue as Int
                        root.setBackgroundColor(animatedColor)
                        window.statusBarColor = animatedColor
                    }
                    start()
                }
        } else {
            root.setBackgroundColor(targetColor)
            window.statusBarColor = targetColor
        }
        currentColor = targetColor
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
        applyBackground(backgroundColor, animate)

        val transaction = supportFragmentManager.beginTransaction()
        if (animate) {
            if (isMovingRight) {
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
            } else {
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        }
        transaction.replace(R.id.fragmentContainer, fragment).commit()
    }

    // Mở màn phụ (Đang nhận diện, Kết quả, Lỗi, Chi tiết...): ẩn bottom nav, có Back
    fun openScreen(fragment: Fragment, @ColorRes backgroundRes: Int = R.color.identify_background) {
        screenBackgroundRes = backgroundRes
        bottomNavigation.visibility = View.GONE
        applyBackground(backgroundRes, animate = false)

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,  // mở
                R.anim.slide_in_left, R.anim.slide_out_right   // Back
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    // Thay màn phụ hiện tại (Đang nhận diện -> Kết quả: Back ở Kết quả về thẳng Trang chủ)
    fun replaceScreen(
        fragment: Fragment,
        @ColorRes backgroundRes: Int = R.color.identify_background
    ) {
        supportFragmentManager.popBackStack()
        openScreen(fragment, backgroundRes)
    }

    fun goToHome() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        showFragment(HomeFragment(), animate = false)
    }
}
