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

class RegisterFragment : Fragment(R.layout.fragment_register) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ĐỒNG BỘ MÀU NỀN FULL LÊN TẬN STATUS BAR
        val skyBlue = ContextCompat.getColor(requireContext(), R.color.home_background)
        requireActivity().window.statusBarColor = skyBlue
        requireActivity().findViewById<View>(R.id.main)?.setBackgroundColor(skyBlue)

        // 1. Cài đặt ẩn/hiện cho ô Mật khẩu
        setUpToggle(editText = view.findViewById(R.id.etLoginPassword), button = view.findViewById(R.id.btnImgTogglePassword))

        // 2. Cài đặt ẩn/hiện cho ô Xác nhận mật khẩu
        setUpToggle(editText = view.findViewById(R.id.etRegisterConfirmPassword), button = view.findViewById(R.id.btnImgToggleConfirmPassword))

        // Bấm Đã có tài khoản? Đăng nhập
        view.findViewById<TextView>(R.id.tvGoToLogin).setOnClickListener {
            // Quay trở lại LoginFragment
            parentFragmentManager.popBackStack()
        }
    }

    private fun setUpToggle(editText: EditText, button: ImageButton) {
        var isVisible = false
        button.setOnClickListener {
            isVisible = !isVisible
            if(isVisible) {
                editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                button.setImageResource(R.drawable.ic_visibility)
            } else {
                editText.transformationMethod = PasswordTransformationMethod.getInstance()
                button.setImageResource(R.drawable.ic_visibility_off)
            }
            editText.setSelection(editText.text.length)
        }
    }
}