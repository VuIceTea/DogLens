package com.example.doglens.ui.auth

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.doglens.MainActivity
import com.example.doglens.R
import kotlinx.coroutines.launch

class RegisterFragment : Fragment(R.layout.fragment_register) {
    private val emailAuth: EmailAuthViewModel by viewModels()

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
        bindRegistration(view)
    }

    private fun bindRegistration(view: View) {
        val name = view.findViewById<EditText>(R.id.etRegisterFullName)
        val email = view.findViewById<EditText>(R.id.etRegisterEmail)
        val password = view.findViewById<EditText>(R.id.etLoginPassword)
        val confirm = view.findViewById<EditText>(R.id.etRegisterConfirmPassword)
        val submit = view.findViewById<TextView>(R.id.btnRegister)
        val label = submit.text

        submit.setOnClickListener {
            val fullName = name.text.toString().trim()
            val address = email.text.toString().trim()
            val secret = password.text.toString()
            when {
                fullName.isEmpty() -> name.error = "Nhập họ và tên"
                !Patterns.EMAIL_ADDRESS.matcher(address).matches() -> email.error = "Nhập email hợp lệ"
                secret.length < 8 || !secret.any(Char::isUpperCase) || !secret.any(Char::isDigit) ->
                    password.error = "Cần ít nhất 8 ký tự, có chữ hoa và chữ số"
                secret != confirm.text.toString() -> confirm.error = "Mật khẩu xác nhận không khớp"
                else -> emailAuth.register(fullName, address, secret)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                emailAuth.state.collect { state ->
                    submit.isEnabled = !state.loading
                    submit.text = if (state.loading) "Đang tạo tài khoản…" else label
                    view.findViewById<View>(R.id.tvGoToLogin).isEnabled = !state.loading
                    state.message?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
                    if (state.completed || state.message != null) emailAuth.consumeOutcome()
                    if (state.completed) (requireActivity() as MainActivity).goToHome()
                }
            }
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
