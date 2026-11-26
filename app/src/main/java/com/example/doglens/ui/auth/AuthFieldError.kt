package com.example.doglens.ui.auth

import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged

internal fun TextView.showFieldError(message: String?) {
    text = message
    isVisible = message != null
}

internal fun TextView.clearErrorOnEdit(input: EditText) {
    input.doAfterTextChanged { showFieldError(null) }
}
