package com.example.doglens.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.doglens.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ChangePasswordFragment : Fragment(R.layout.fragment_change_password) {
    private val viewModel: ChangePasswordViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val current = view.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val newPassword = view.findViewById<TextInputEditText>(R.id.etNewPassword)
        val confirmation = view.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        view.findViewById<View>(R.id.btnBack).setOnClickListener { parentFragmentManager.popBackStack() }
        listOf(current, newPassword, confirmation).forEach { it.doAfterTextChanged { viewModel.clearFieldErrors() } }
        view.findViewById<View>(R.id.btnUpdatePassword).setOnClickListener {
            viewModel.updatePassword(current.text?.toString().orEmpty(), newPassword.text?.toString().orEmpty(), confirmation.text?.toString().orEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    view.findViewById<TextInputLayout>(R.id.layoutCurrentPassword).error = state.currentPasswordError
                    view.findViewById<TextInputLayout>(R.id.layoutNewPassword).error = state.newPasswordError
                    view.findViewById<TextInputLayout>(R.id.layoutConfirmPassword).error = state.confirmPasswordError
                    view.findViewById<View>(R.id.socialAccountNotice).visibility = if (state.isSocialAccount) View.VISIBLE else View.GONE
                    view.findViewById<View>(R.id.passwordForm).visibility = if (state.isSocialAccount) View.GONE else View.VISIBLE
                    view.findViewById<View>(R.id.btnUpdatePassword).isEnabled = !state.isLoading
                    view.findViewById<TextView>(R.id.tvUpdatePassword).visibility = if (state.isLoading) View.INVISIBLE else View.VISIBLE
                    view.findViewById<ProgressBar>(R.id.progressUpdatePassword).visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    state.message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        val success = state.success
                        viewModel.consumeMessage()
                        if (success) parentFragmentManager.popBackStack()
                    }
                }
            }
        }
    }
}
