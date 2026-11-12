package com.example.doglens.ui.auth

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.doglens.R

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ĐỒNG BỘ MÀU NỀN FULL LÊN TẬN STATUS BAR
        val skyBlue = ContextCompat.getColor(requireContext(), R.color.home_background)
        requireActivity().window.statusBarColor = skyBlue
        requireActivity().findViewById<View>(R.id.main)?.setBackgroundColor(skyBlue)

        // Hàm ẩn/hiện mật khẩu
        setUpPasswordToggle(view)

        // Bấm Chưa có tài khoản? Đăng ký
        view.findViewById<TextView>(R.id.tvGoToRegister).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left).replace(
                    R.id.fragmentContainer,
                    RegisterFragment()
                ).addToBackStack(null).commit()
        }
    }

    private fun setUpPasswordToggle(view: View) {
        val etPassword = view.findViewById<EditText>(R.id.etLoginPassword)
        val btnToggle = view.findViewById<ImageButton>(R.id.btnImgTogglePassword)
        var isVisible = false

        btnToggle.setOnClickListener {
            isVisible = !isVisible
            if (isVisible) {
                // Hiện mật khẩu & đổi sang icon mắt mở
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                btnToggle.setImageResource(R.drawable.ic_visibility)
            } else {
                // Ẩn mật khẩu & đổi sang icon mắt gạch chéo
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnToggle.setImageResource(R.drawable.ic_visibility_off)
            }
            // Đưa con trỏ văn bản về cuối dòng
            etPassword.setSelection(etPassword.text.length)
        }
    }
}