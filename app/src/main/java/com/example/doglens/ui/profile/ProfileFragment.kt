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
import com.bumptech.glide.Glide
import com.example.doglens.MainActivity
import com.example.doglens.data.model.User
import com.example.doglens.data.model.profile.ProfileStats
import com.example.doglens.ui.auth.LoginFragment
import com.example.doglens.ui.history.HistoryFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import android.widget.Toast

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private val profileRepository: ProfileRepository = MockProfileRepository()
    private lateinit var imgUserAvatar: ImageView
    private val userViewModel: UserViewModel by viewModels()

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

        observeUser(view)
        setupAvatar(view)
        setupProfileMenu(view)
        loadProfile(view)
    }


    private fun observeUser(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                userViewModel.uiState.collect { state ->

                    if (state.isLoading) {
                    }

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

    private fun displayUser(view: View, user: User) {

        val txtName = view.findViewById<TextView>(R.id.txtName)
        val txtEmail = view.findViewById<TextView>(R.id.txtEmail)
        val txtMembership = view.findViewById<TextView>(R.id.txtMembership)
        val imgAvatar = view.findViewById<ImageView>(R.id.imgUserAvatar)

        txtName.text = user.fullName
        txtEmail.text = user.email

        // Chuyển mã membership thành nội dung hiển thị
        txtMembership.text = when (user.membership) {
            "FREE" -> "Thành viên miễn phí"
            "PREMIUM" -> "Thành viên Premium"
            else -> user.membership
        }

        val avatarUrl = if (
            user.hasCustomAvatar &&
            !user.customAvatarUrl.isNullOrBlank()
        ) {
            user.customAvatarUrl
        } else {
            user.avatarUrl
        }

        Glide.with(this)
            .load(avatarUrl)
            .placeholder(R.drawable.avatar_golden_retriever)
            .error(R.drawable.avatar_golden_retriever)
            .circleCrop()
            .into(imgAvatar)
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
        fun openEditProfile() {
            (requireActivity() as MainActivity).openScreen(
                EditProfileFragment(),
                R.color.explore_background
            )
        }

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

        // Mở màn Lịch sử nhận diện
        view.findViewById<View>(R.id.menuHistory).setOnClickListener {
            (requireActivity() as MainActivity).openScreen(
                HistoryFragment(),
                R.color.explore_background
            )
        }

        // Mở màn Chỉnh sửa thông tin cá nhân
        view.findViewById<View>(R.id.menuPersonalInfo).setOnClickListener {
            openEditProfile()
        }

        // Cho phép bấm trực tiếp vào khung hồ sơ phía trên để chỉnh sửa.
        view.findViewById<View>(R.id.userProfileCard).setOnClickListener {
            openEditProfile()
        }

        // Bắt sự kiện bấm vào nút Đăng xuất
        val menuLogout = view.findViewById<View>(R.id.menuLogout)
        menuLogout.setOnClickListener {

            // Đăng xuất Firebase
            FirebaseAuth.getInstance().signOut()

            // Ẩn thanh Bottom Navigation của MainActivity
            activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.GONE

            // Chuyển sang màn hình LoginFragment
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragmentContainer, LoginFragment())
                .commit()
        }
    }
}
