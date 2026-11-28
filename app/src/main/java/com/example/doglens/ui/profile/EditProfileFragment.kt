package com.example.doglens.ui.profile

import android.os.Bundle
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.doglens.R
import com.example.doglens.utils.bindPullRefresh
import com.example.doglens.MainActivity
import com.example.doglens.ui.auth.ChangePasswordFragment
import com.example.doglens.data.model.User
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private val viewModel: EditProfileViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Thiết lập tiêu đề các nhóm
        setupSectionTitles(view)

        // 2. Thiết lập tên và icon từng dòng
        setupRows(view)
        removeLastRowDividers(view)

        // 3. Lắng nghe dữ liệu từ Firestore
        observeUser(view)
        bindPullRefresh(view.findViewById(R.id.pullRefresh)) { viewModel.refresh() }

        // 4. Nút quay lại
        view.findViewById<View>(R.id.btnBack)
            .setOnClickListener {
                parentFragmentManager.popBackStack()
            }

        view.findViewById<View>(R.id.rowChangePassword).setOnClickListener {
            (requireActivity() as MainActivity).openScreen(
                ChangePasswordFragment(),
                R.color.explore_background
            )
        }
    }

    private fun removeLastRowDividers(view: View) {

        // Item cuối của Thông tin cơ bản
        view.findViewById<View>(R.id.rowGender)
            .setBackgroundResource(R.drawable.bg_profile_row_last)

        // Item cuối của Thông tin liên hệ
        view.findViewById<View>(R.id.rowAddress)
            .setBackgroundResource(R.drawable.bg_profile_row_last)
    }


    // THIẾT LẬP TIÊU ĐỀ
    private fun setupSectionTitles(view: View) {

        fun setTitle(sectionId: Int, title: String) {
            val section = view.findViewById<View>(sectionId)

            section.findViewById<TextView>(R.id.sectionTitle)
                .text = title
        }

        setTitle(
            R.id.basicSectionTitle,
            "Thông tin cơ bản"
        )

        setTitle(
            R.id.contactSectionTitle,
            "Thông tin liên hệ"
        )

        setTitle(
            R.id.securitySectionTitle,
            "Bảo mật"
        )
    }

    // THIẾT LẬP ICON VÀ LABEL
    private fun setupRows(view: View) {

        fun setupRow(
            rowId: Int,
            iconRes: Int,
            label: String
        ) {
            val row = view.findViewById<View>(rowId)

            val iconView = row.findViewById<ImageView>(R.id.rowIcon)

            iconView.setImageResource(iconRes)
            row.findViewById<TextView>(R.id.rowLabel).text = label

            // Hai icon này có màu gốc khác bộ icon hồ sơ, nên áp dụng cùng màu xanh.
            iconView.imageTintList = if (
                rowId == R.id.rowEmail || rowId == R.id.rowPhone
            ) {
                ColorStateList.valueOf(Color.parseColor("#168FF2"))
            } else {
                null
            }
        }

        setupRow(
            R.id.rowFullName,
            R.drawable.ic_person_info,
            "Họ và tên"
        )

        setupRow(
            R.id.rowBirthDate,
            R.drawable.ic_calendar,
            "Ngày sinh"
        )

        setupRow(
            R.id.rowGender,
            R.drawable.ic_gender,
            "Giới tính"
        )

        setupRow(
            R.id.rowEmail,
            R.drawable.ic_email,
            "Địa chỉ email"
        )

        setupRow(
            R.id.rowPhone,
            R.drawable.ic_phone,
            "Số điện thoại"
        )

        setupRow(
            R.id.rowAddress,
            R.drawable.ic_location,
            "Địa chỉ"
        )

        // Email chỉ được xem, không chỉnh sửa trực tiếp
        val emailRow = view.findViewById<View>(R.id.rowEmail)

        // Email của tài khoản Google/Facebook chỉ dùng để hiển thị.
        // Ẩn toàn bộ action thay vì hiện khóa và thông báo "Không thể sửa".
        emailRow.findViewById<View>(R.id.rowActionContainer)
            .visibility = View.GONE
        emailRow.isEnabled = false
        emailRow.isClickable = false
        emailRow.isFocusable = false
    }

    // QUAN SÁT DỮ LIỆU
    private fun observeUser(view: View) {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.uiState.collect { state ->

                    state.user?.let { user ->
                        displayUser(view, user)
                    }

                    state.error?.let { message ->
                        Toast.makeText(
                            requireContext(),
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // HIỂN THỊ THÔNG TIN NGƯỜI DÙNG
    private fun displayUser(view: View, user: User) {
        fun setValue(rowId: Int, value: String) {

            val row = view.findViewById<View>(rowId)

            val textView = row.findViewById<TextView>(
                R.id.rowValue
            )

            if (value.isBlank()) {
                textView.text = "Chưa cập nhật"
                textView.setTextColor(
                    android.graphics.Color.parseColor("#A2AAC4")
                )

            } else {
                textView.text = value
                textView.setTextColor(
                    android.graphics.Color.parseColor("#10245E")
                )
            }
        }

        setValue(
            R.id.rowFullName,
            user.fullName
        )

        setValue(
            R.id.rowBirthDate,
            user.birthDate
        )

        setValue(
            R.id.rowGender,
            user.gender
        )

        setValue(
            R.id.rowEmail,
            user.email
        )

        setValue(
            R.id.rowPhone,
            user.phoneNumber
        )

        setValue(
            R.id.rowAddress,
            user.address
        )

        // Hiển thị ảnh đại diện
        val avatarUrl = if (
            user.hasCustomAvatar &&
            !user.customAvatarUrl.isNullOrBlank()
        ) {
            user.customAvatarUrl
        } else {
            user.avatarUrl
        }

        val imgAvatar = view.findViewById<ImageView>(
            R.id.imgEditAvatar
        )

        Glide.with(this)
            .load(avatarUrl)
            .placeholder(R.drawable.avatar_golden_retriever)
            .error(R.drawable.avatar_golden_retriever)
            .circleCrop()
            .into(imgAvatar)
    }
}
