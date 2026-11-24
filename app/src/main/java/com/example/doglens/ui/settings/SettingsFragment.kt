package com.example.doglens.ui.settings

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.doglens.R
import com.example.doglens.MainActivity
import com.example.doglens.ui.auth.ChangePasswordFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btnBack).setOnClickListener { parentFragmentManager.popBackStack() }

        bindRow(view, R.id.rowLanguage, R.drawable.ic_globe, "Ngôn ngữ", "Tiếng Việt", "Tiếng Việt") {
            AlertDialog.Builder(requireContext()).setTitle("Ngôn ngữ")
                .setSingleChoiceItems(arrayOf("Tiếng Việt", "English"), 0, null)
                .setPositiveButton("Xong", null).show()
        }
        bindToggle(view, R.id.rowTheme, R.drawable.ic_settings_moon, "Giao diện", "Sử dụng giao diện sáng", viewModel::setDarkMode)
        bindToggle(view, R.id.rowRecognitionNotification, R.drawable.ic_notification, "Thông báo nhận diện", "Nhận kết quả và cập nhật nhận diện", viewModel::setRecognitionNotifications)
        bindToggle(view, R.id.rowArticleNotification, R.drawable.ic_article_bookmark_outline, "Bài viết và gợi ý", "Nội dung mới phù hợp với bạn", viewModel::setArticleNotifications)
        bindRow(view, R.id.rowPermissions, R.drawable.ic_settings_shield, "Quyền ứng dụng", "Camera và thư viện ảnh") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${requireContext().packageName}")))
        }
        bindRow(view, R.id.rowPassword, R.drawable.ic_lock, "Đổi mật khẩu", "Cập nhật mật khẩu tài khoản") {
            (requireActivity() as MainActivity).openScreen(ChangePasswordFragment(), R.color.explore_background)
        }
        bindRow(view, R.id.rowDeleteAccount, R.drawable.ic_settings_delete, "Xóa tài khoản", "Xóa vĩnh viễn dữ liệu của bạn") {
            AlertDialog.Builder(requireContext()).setTitle("Xóa tài khoản?")
                .setMessage("Hành động này sẽ xóa vĩnh viễn dữ liệu của bạn.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa") { _, _ -> Toast.makeText(requireContext(), "Chưa kết nối chức năng xóa tài khoản", Toast.LENGTH_SHORT).show() }
                .show()
        }
        bindRow(view, R.id.rowTerms, R.drawable.ic_settings_document, "Điều khoản sử dụng", null) { showInfo("Điều khoản sử dụng") }
        bindRow(view, R.id.rowPrivacy, R.drawable.ic_settings_privacy, "Chính sách riêng tư", null) { showInfo("Chính sách riêng tư") }
        bindRow(view, R.id.rowVersion, R.drawable.ic_settings_info, "Phiên bản ứng dụng", null, "1.0.0", false, null)

        styleIcon(view, R.id.rowLanguage, R.drawable.bg_notification_icon_blue, "#168BFF")
        styleIcon(view, R.id.rowTheme, R.drawable.bg_notification_icon_purple, "#8B5CF6")
        styleIcon(view, R.id.rowRecognitionNotification, R.drawable.bg_menu_icon_orange, "#E97808")
        styleIcon(view, R.id.rowArticleNotification, R.drawable.bg_notification_icon_pink, "#EC4899")
        styleIcon(view, R.id.rowPermissions, R.drawable.bg_settings_icon_teal, "#0EA5A4")
        styleIcon(view, R.id.rowPassword, R.drawable.bg_notification_icon_blue, "#2563EB")
        styleIcon(view, R.id.rowDeleteAccount, R.drawable.bg_menu_icon_pink, "#EF4444")
        styleIcon(view, R.id.rowTerms, R.drawable.bg_settings_icon_indigo, "#6366F1")
        styleIcon(view, R.id.rowPrivacy, R.drawable.bg_settings_icon_green, "#16A66A")
        styleIcon(view, R.id.rowVersion, R.drawable.bg_menu_icon_gray, "#64748B")

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    toggle(view, R.id.rowTheme).isChecked = state.darkMode
                    toggle(view, R.id.rowRecognitionNotification).isChecked = state.recognitionNotifications
                    toggle(view, R.id.rowArticleNotification).isChecked = state.articleNotifications
                }
            }
        }
    }

    private fun bindRow(view: View, id: Int, icon: Int, title: String, subtitle: String?, value: String? = null, showChevron: Boolean = true, click: (() -> Unit)?) {
        val row = view.findViewById<View>(id)
        row.findViewById<ImageView>(R.id.imgSettingIcon).setImageResource(icon)
        row.findViewById<TextView>(R.id.tvSettingTitle).text = title
        row.findViewById<TextView>(R.id.tvSettingSubtitle).apply { text = subtitle; visibility = if (subtitle.isNullOrBlank()) View.GONE else View.VISIBLE }
        row.findViewById<TextView>(R.id.tvSettingValue).apply { text = value; visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE }
        row.findViewById<View>(R.id.imgChevron).visibility = if (showChevron) View.VISIBLE else View.GONE
        row.setOnClickListener { click?.invoke() }
    }

    private fun bindToggle(view: View, id: Int, icon: Int, title: String, subtitle: String, changed: (Boolean) -> Unit) {
        val row = view.findViewById<View>(id)
        row.findViewById<ImageView>(R.id.imgSettingIcon).setImageResource(icon)
        row.findViewById<TextView>(R.id.tvSettingTitle).text = title
        row.findViewById<TextView>(R.id.tvSettingSubtitle).text = subtitle
        toggle(view, id).setOnCheckedChangeListener { _, checked -> changed(checked) }
    }

    private fun toggle(view: View, rowId: Int): SwitchMaterial = view.findViewById<View>(rowId).findViewById(R.id.settingSwitch)

    private fun styleIcon(view: View, rowId: Int, backgroundRes: Int, tint: String) {
        val row = view.findViewById<View>(rowId)
        row.findViewById<View>(R.id.settingIconContainer).setBackgroundResource(backgroundRes)
        row.findViewById<ImageView>(R.id.imgSettingIcon).imageTintList =
            ColorStateList.valueOf(Color.parseColor(tint))
    }

    private fun showInfo(title: String) {
        AlertDialog.Builder(requireContext()).setTitle(title).setMessage("Nội dung đang được cập nhật.").setPositiveButton("Đóng", null).show()
    }
}
