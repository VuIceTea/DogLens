
package com.example.doglens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cho phép giao diện vẽ phía sau system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Icon trên thanh hệ thống có màu tối
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

        val root = findViewById<android.view.View>(
            R.id.main
        )

        // Root chỉ xử lý inset trái và phải.
        // Không thêm paddingBottom vào toàn bộ màn hình.

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        // Dành chỗ an toàn cho Bottom Navigation
        val originalBottomPadding =
            bottomNavigation.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(
            bottomNavigation
        ) { view, insets ->

            val bars = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars()
            )

            view.updatePadding(
                bottom = originalBottomPadding + bars.bottom
            )

            insets
        }

        // Chỉ hiển thị Home lần đầu
        if (savedInstanceState == null) {
            showFragment(HomeFragment())
        }

        // Xử lý chuyển màn hình
        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    showFragment(HomeFragment())
                    true
                }

//                R.id.nav_explore -> {
//                    showFragment(ExploreFragment())
//                    true
//                }
//
//                R.id.nav_favorite -> {
//                    showFragment(FavoriteFragment())
//                    true
//                }
//
//                R.id.nav_profile -> {
//                    showFragment(ProfileFragment())
//                    true
//                }

                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
