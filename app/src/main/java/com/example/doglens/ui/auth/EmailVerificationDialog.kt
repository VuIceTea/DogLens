package com.example.doglens.ui.auth

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

internal fun Fragment.showEmailVerification(
    verification: PendingEmailVerification,
    buttonLabel: String,
    onContinue: () -> Unit
) {
    val message = if (verification.sent) {
        "Hãy kiểm tra hộp thư (kể cả thư rác) của ${verification.email}, mở liên kết xác minh rồi đăng nhập lại."
    } else {
        "Chưa gửi được thư xác minh đến ${verification.email}. Hãy đăng nhập lại để thử gửi thư rồi mở liên kết xác minh."
    }

    AlertDialog.Builder(requireContext())
        .setTitle("Xác minh email")
        .setMessage(message)
        .setPositiveButton(buttonLabel) { _, _ -> onContinue() }
        .show()
}
