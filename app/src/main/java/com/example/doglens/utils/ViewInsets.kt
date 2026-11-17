package com.example.doglens.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

// Ở màn phụ bottom nav bị ẩn -> chừa chỗ cho thanh điều hướng hệ thống
fun View.applyNavigationBarPadding() {
    val basePadding = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        v.updatePadding(bottom = basePadding + nav.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
