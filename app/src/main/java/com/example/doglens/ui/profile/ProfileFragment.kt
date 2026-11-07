package com.example.doglens.ui.profile

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.R
import com.example.doglens.data.adapter.ProfileStatsAdapter
import com.example.doglens.data.mock.MockProfileRepository
import com.example.doglens.data.repository.ProfileRepository
import kotlinx.coroutines.launch
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.widget.FrameLayout
import com.example.doglens.data.model.User
import com.example.doglens.data.model.profile.ProfileStats

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private val profileRepository: ProfileRepository = MockProfileRepository()
    private lateinit var imgUserAvatar: ImageView

    // Chọn ảnh từ thư viện
    private val pickAvatarLauncher =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->

            if (uri != null) {
                imgUserAvatar.setImageURI(uri)
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupAvatar(view)
        setupProfileMenu(view)
        loadProfile(view)
    }

    // AVATAR
    private fun setupAvatar(view: View) {
        imgUserAvatar = view.findViewById(R.id.imgUserAvatar)

        val avatarContainer = view.findViewById<View>(R.id.avatarContainer)

        val imgChangeAvatar = view.findViewById<ImageView>(R.id.imgChangeAvatar)

        // Bấm vào avatar
        avatarContainer.setOnClickListener {
            openImagePicker()
        }

        // Bấm vào icon camera
        imgChangeAvatar.setOnClickListener {
            openImagePicker()
        }
    }

    private fun openImagePicker() {

        pickAvatarLauncher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    // LOAD PROFILE
    private fun loadProfile(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val profile = profileRepository.getProfile()
                // Hiển thị thông tin User
                displayUserInfo(view, profile.user)
                // Hiển thị thống kê
                setupStats(view, profile.stats)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // DISPLAY USER
    private fun displayUserInfo(view: View, user: User) {
        view.findViewById<TextView>(R.id.txtName).text = user.fullName
        view.findViewById<TextView>(R.id.txtEmail).text = user.email

        val membershipText = when (user.membership) {
            "FREE" -> "Thành viên miễn phí"
            "PREMIUM" -> "Thành viên Premium"
            else -> user.membership
        }

        view.findViewById<TextView>(R.id.txtMembership).text = membershipText
    }

    // DISPLAY PROFILE STATS
    private fun setupStats(view: View, profileStats: ProfileStats) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvProfileStats)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        addStatDividers(recyclerView)

        val stats = listOf(
            ProfileStatsAdapter.StatItem(
                icon = R.drawable.ic_stat_gallery,
                value = profileStats.saveDogs,
                label = "Giống chó đã lưu",
                iconBackgroundColor = "#D9ECFF",
                iconTintColor = "#1683F8"
            ),
            ProfileStatsAdapter.StatItem(
                icon = R.drawable.ic_stat_history,
                value = profileStats.recognitionCount,
                label = "Lượt nhận diện",
                iconBackgroundColor = "#D5F2E3",
                iconTintColor = "#16B978"
            ),
            ProfileStatsAdapter.StatItem(
                icon = R.drawable.ic_stat_heart_outline,
                value = profileStats.viewedArticles,
                label = "Bài viết đã xem",
                iconBackgroundColor = "#FFE9ED",
                iconTintColor = "#FF4564"
            )
        )
        recyclerView.adapter = ProfileStatsAdapter(stats)
    }

    private fun addStatDividers(recyclerView: RecyclerView) {

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5EAF2")
            strokeWidth = 1.dp
        }

        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun onDraw(
                    canvas: Canvas,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    super.onDraw(canvas, parent, state)
                    for (i in 0 until parent.childCount) {

                        val child = parent.getChildAt(i)

                        val params = child.layoutParams as GridLayoutManager.LayoutParams

                        if (params.spanIndex < 2) {

                            val x = child.right.toFloat()

                            val top = child.top + 16.dp
                            val bottom = child.bottom - 16.dp

                            canvas.drawLine(
                                x,
                                top.toFloat(),
                                x,
                                bottom.toFloat(),
                                dividerPaint
                            )
                        }
                    }
                }
            }
        )
    }

    private val Int.dp: Float get() = this * resources.displayMetrics.density

    private fun setupProfileMenu(view: View) {
        fun bindMenu(
            id: Int,
            icon: Int,
            title: String,
            description: String,
            background: Int,
            tint: String
        ) {
            val item = view.findViewById<View>(id)

            val iconContainer = item.findViewById<FrameLayout>(R.id.menuIconContainer)

            val imgIcon = item.findViewById<ImageView>(R.id.imgMenuIcon)

            item.findViewById<TextView>(R.id.txtMenuTitle).text = title

            item.findViewById<TextView>(R.id.txtMenuDescription).text = description

            iconContainer.setBackgroundResource(background)
            imgIcon.setImageResource(icon)
            if (icon == R.drawable.ic_history) {
                imgIcon.imageTintList = null
            } else {
                imgIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(tint))
            }
        }

        bindMenu(
            R.id.menuFavorite,
            R.drawable.ic_heart_favorite,
            "Giống chó yêu thích",
            "Xem lại danh sách giống chó bạn đã lưu",
            R.drawable.bg_menu_icon_pink,
            "#FF4564"
        )
        bindMenu(
            R.id.menuHistory,
            R.drawable.ic_history,
            "Lịch sử nhận diện",
            "Hình ảnh và kết quả đã nhận diện",
            R.drawable.bg_menu_icon_blue,
            "#1683F8"
        )
        bindMenu(
            R.id.menuViewedArticles,
            R.drawable.ic_viewed_articles,
            "Bài viết đã xem",
            "Các bài viết trong mục Khám phá",
            R.drawable.bg_menu_icon_purple,
            "#A855F7"
        )
        bindMenu(
            R.id.menuPersonalInfo,
            R.drawable.ic_person_info,
            "Thông tin cá nhân",
            "Quản lý thông tin tài khoản",
            R.drawable.bg_menu_icon_blue,
            "#1683F8"
        )
        bindMenu(
            R.id.menuNotification,
            R.drawable.ic_notifications,
            "Thông báo",
            "Quản lý thông báo từ ứng dụng",
            R.drawable.bg_menu_icon_orange,
            "#F59E0B"
        )
        bindMenu(
            R.id.menuSettings,
            R.drawable.ic_setting,
            "Cài đặt",
            "Ngôn ngữ, giao diện, quyền riêng tư...",
            R.drawable.bg_menu_icon_gray,
            "#64748B"
        )
        bindMenu(
            R.id.menuLogout,
            R.drawable.ic_logout,
            "Đăng xuất",
            "Thoát khỏi tài khoản hiện tại",
            R.drawable.bg_menu_icon_pink,
            "#FF4564"
        )
    }
}
