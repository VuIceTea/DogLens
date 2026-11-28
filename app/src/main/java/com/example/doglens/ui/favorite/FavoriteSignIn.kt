package com.example.doglens.ui.favorite

import androidx.fragment.app.Fragment
import com.example.doglens.MainActivity
import com.example.doglens.R
import com.example.doglens.ui.auth.LoginFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

internal fun Fragment.whenSignedInForFavorite(action: () -> Unit) {
    if (FirebaseAuth.getInstance().currentUser != null) {
        action()
        return
    }

    Snackbar.make(requireView(), R.string.favorite_sign_in_message, Snackbar.LENGTH_LONG)
        .setAction(R.string.favorite_sign_in_action) {
            (activity as? MainActivity)?.openScreen(LoginFragment(), R.color.home_background)
        }
        .show()
}
