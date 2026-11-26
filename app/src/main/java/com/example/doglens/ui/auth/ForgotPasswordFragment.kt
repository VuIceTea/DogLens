package com.example.doglens.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.doglens.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val email = view.findViewById<TextInputEditText>(R.id.etResetEmail)
        if (savedInstanceState == null && email.text.isNullOrEmpty()) {
            email.setText(arguments?.getString(ARG_EMAIL).orEmpty())
        }
        val resend = view.findViewById<TextView>(R.id.btnResend)
        view.findViewById<View>(R.id.btnBack).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.btnBackToLogin).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.btnBackToLoginSuccess).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.btnSendReset).setOnClickListener { viewModel.send(email.text?.toString().orEmpty()) }
        resend.setOnClickListener { viewModel.resend() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    view.findViewById<TextInputLayout>(R.id.layoutResetEmail).error = state.emailError
                    view.findViewById<View>(R.id.resetFormContent).visibility = if (state.sent) View.GONE else View.VISIBLE
                    view.findViewById<View>(R.id.resetSuccessContent).visibility = if (state.sent) View.VISIBLE else View.GONE
                    view.findViewById<View>(R.id.imgForgotDog).visibility = if (state.sent) View.GONE else View.VISIBLE
                    view.findViewById<TextView>(R.id.tvSentEmail).text = state.email
                    view.findViewById<View>(R.id.btnSendReset).isEnabled = !state.isLoading
                    email.isEnabled = !state.isLoading
                    resend.isEnabled = !state.isLoading && state.resendSeconds == 0
                    resend.text = when {
                        state.isLoading -> "Đang gửi lại…"
                        state.resendSeconds > 0 -> getString(R.string.reset_resend_wait, state.resendSeconds)
                        else -> getString(R.string.reset_resend)
                    }
                    view.findViewById<View>(R.id.tvSendReset).visibility = if (state.isLoading) View.INVISIBLE else View.VISIBLE
                    view.findViewById<ProgressBar>(R.id.progressSendReset).visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    state.message?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show(); viewModel.consumeMessage() }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val background = ContextCompat.getColor(requireContext(), R.color.forgot_password_background)
        requireActivity().window.statusBarColor = background
        requireActivity().findViewById<View>(R.id.main)?.setBackgroundColor(background)
    }

    companion object {
        private const val ARG_EMAIL = "reset_email"

        fun newInstance(email: String) = ForgotPasswordFragment().apply {
            arguments = Bundle().apply { putString(ARG_EMAIL, email) }
        }
    }
}
