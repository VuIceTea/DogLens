package com.example.doglens.ui.auth

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.doglens.R
import com.example.doglens.ui.home.HomeFragment
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ĐỒNG BỘ MÀU NỀN FULL LÊN TẬN STATUS BAR
        val skyBlue = ContextCompat.getColor(requireContext(), R.color.home_background)
        requireActivity().window.statusBarColor = skyBlue
        requireActivity().findViewById<View>(R.id.main)?.setBackgroundColor(skyBlue)

        // Hàm ẩn/hiện mật khẩu
        setUpPasswordToggle(view)

        // Thiết lập đăng nhập Google
        setUpGoogleSignIn(view)

        // Bấm Chưa có tài khoản? Đăng ký
        view.findViewById<TextView>(R.id.tvGoToRegister).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left).replace(
                    R.id.fragmentContainer, RegisterFragment()
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

    private fun setUpGoogleSignIn(view: View) {
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(requireContext())
        view.findViewById<View>(R.id.btnGoogleLogin).setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Tạo yêu cầu đăng nhập Google
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setAutoSelectEnabled(false)
                    .build()

                // 2. Tạo Credential Request
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // 3. Hiển thị hộp chọn tài khoản Google
                val result = credentialManager.getCredential(
                    context = requireContext(),
                    request = request
                )

                // 4. Nhận Google Credential
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    // 5. Lấy Google ID Token
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    // 6. Đăng nhập Firebase bằng Google ID Token
                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    Toast.makeText(
                        requireContext(), "Không thể lấy thông tin tài khoản Google",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: GetCredentialException) {
                Log.e("GoogleSignIn", "Credential error", e)

                Toast.makeText(
                    requireContext(),
                    "Không thể chọn tài khoản Google: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "Sign in error", e)

                Toast.makeText(
                    requireContext(),
                    "Đăng nhập Google thất bại: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d("GoogleSignIn", "Login successful")
                    Log.d("GoogleSignIn", "UID: ${user?.uid}")
                    Log.d("GoogleSignIn", "Email: ${user?.email}")

                    Toast.makeText(
                        requireContext(),
                        "Xin chào ${user?.displayName}",
                        Toast.LENGTH_LONG
                    ).show()
                    navigateToHome()
                } else {
                    Log.e(
                        "GoogleSignIn",
                        "Firebase authentication failed",
                        task.exception
                    )

                    Toast.makeText(
                        requireContext(),
                        "Firebase xác thực thất bại: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun navigateToHome() {
        // Hiện lại thanh Bottom Navigation
        requireActivity()
            .findViewById<View>(R.id.bottomNavigation)
            .visibility = View.VISIBLE

        // Chuyển sang HomeFragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()
    }
}