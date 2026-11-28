package com.example.doglens.ui.explore

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doglens.R
import com.example.doglens.MainActivity
import com.example.doglens.ui.dog.SuggestedDogsFragment
import com.example.doglens.ui.notification.NotificationFragment
import com.example.doglens.utils.bindPullRefresh
import com.example.doglens.data.adapter.ExploreCategoryAdaper
import com.example.doglens.data.adapter.ExploreFeatureTopicAdapter
import com.example.doglens.data.adapter.ExploreDogUseCaseAdapter
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.FeaturedTopic
import com.example.doglens.data.model.DogUseCase
import kotlinx.coroutines.launch

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private val viewModel: ExploreViewModel by viewModels()

    private lateinit var categoryAdapter: ExploreCategoryAdaper
    private lateinit var featuredTopicAdapter: ExploreFeatureTopicAdapter
    private lateinit var dogUseCaseAdapter: ExploreDogUseCaseAdapter

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        initializeAdapters()
        initializeRecyclerViews(view)
        initializeSectionHeaders(view)
        bindPullRefresh(view.findViewById(R.id.pullRefresh)) {
            viewModel.load().join()
            check(!viewModel.loadFailed)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.content.collect { content -> content?.let { renderLists(view, it) } }
            }
        }
        if (viewModel.content.value == null) viewLifecycleOwner.lifecycleScope.launch {
            viewModel.load().join()
            if (viewModel.loadFailed) showMessage("Chưa tải được dữ liệu Khám phá. Kéo xuống để thử lại.")
        }
        initializeSearch(view)
        view.findViewById<View>(R.id.exploreNotifications).setOnClickListener {
            openScreen(NotificationFragment())
        }
        view.findViewById<View>(R.id.imgExploreBanner).apply {
            contentDescription = "Tất cả giống chó"
            setOnClickListener { openDirectory("Tất cả giống chó") }
        }
    }

    // Khởi tạo các Adapter
    private fun initializeAdapters() {
        categoryAdapter = ExploreCategoryAdaper { dog ->
            onCategoryClicked(dog)
        }

        featuredTopicAdapter = ExploreFeatureTopicAdapter { topic ->
            onFeaturedTopicClicked(topic)
        }

        dogUseCaseAdapter = ExploreDogUseCaseAdapter { useCase ->
            onDogUseCaseClicked(useCase)
        }
    }

    // Cấu hình các RecyclerView theo chiều ngang
    private fun initializeRecyclerViews(view: View) {
        setupHorizontalRecyclerView(
            view.findViewById(R.id.rvCategoryExplore),
            categoryAdapter
        )

        setupHorizontalRecyclerView(
            view.findViewById(R.id.rvFeaturedTopics),
            featuredTopicAdapter
        )

        setupHorizontalRecyclerView(
            view.findViewById(R.id.rvDogUseCases),
            dogUseCaseAdapter
        )
    }

    private fun setupHorizontalRecyclerView(
        recyclerView: RecyclerView,
        adapter: RecyclerView.Adapter<*>
    ) {
        recyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    // Thiết lập tiêu đề các khu vực
    private fun initializeSectionHeaders(view: View) {
        setupSectionHeader(
            view.findViewById(R.id.headerCategories),
            ExploreSection.CATEGORIES
        )

        setupSectionHeader(
            view.findViewById(R.id.headerFeaturedTopics),
            ExploreSection.TOPICS
        )

        setupSectionHeader(
            view.findViewById(R.id.headerDogUseCases),
            ExploreSection.USE_CASES
        )
    }

    private fun setupSectionHeader(
        header: View,
        section: ExploreSection
    ) {
        header.findViewById<TextView>(R.id.txtSectionTitle).text = section.title

        header.findViewById<TextView>(R.id.btnAllCategories).setOnClickListener {
            openScreen(ExploreSectionFragment.newInstance(section))
        }
    }

    // Hiển thị dữ liệu từ Repository
    private fun renderLists(view: View, content: ExploreContent) {
        categoryAdapter.submitList(content.categories)
        featuredTopicAdapter.submitList(content.topics)
        dogUseCaseAdapter.submitList(content.useCases)
        content.banner?.let { banner ->
            val image = view.findViewById<ImageView>(R.id.imgExploreBanner)
            image.contentDescription = banner.title.ifBlank { "Tất cả giống chó" }
            val imageSource = banner.imageUrl ?: banner.localImageRes
            if (imageSource != null) Glide.with(this).load(imageSource).into(image)
        }
    }

    // Xử lý tìm kiếm
    private fun initializeSearch(view: View) {
        val searchInput = view.findViewById<EditText>(R.id.txtSearch)
        fun search() {
            val keyword = searchInput.text.toString().trim()
            androidx.core.view.WindowInsetsControllerCompat(requireActivity().window, view)
                .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
            openDirectory(if (keyword.isBlank()) "Tất cả giống chó" else "Kết quả tìm kiếm", query = keyword)
        }
        view.findViewById<View>(R.id.imaSearch).setOnClickListener { search() }

        searchInput.setOnEditorActionListener { _, actionId, event ->
            val searchAction = actionId == EditorInfo.IME_ACTION_SEARCH

            val enterPressed =
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP

            if (searchAction || enterPressed) {
                search()
                true
            } else {
                false
            }
        }
    }

    // Nhấn vào danh mục giống chó
    private fun onCategoryClicked(dog: Dog) {
        openDirectory(dog.name, ExploreSection.CATEGORIES, dog.id)
    }

    // Nhấn vào chủ đề nổi bật
    private fun onFeaturedTopicClicked(topic: FeaturedTopic) {
        openDirectory(topic.title, ExploreSection.TOPICS, topic.id)
    }

    // Nhấn vào nhóm công dụng
    private fun onDogUseCaseClicked(useCase: DogUseCase) {
        openDirectory(useCase.name, ExploreSection.USE_CASES, useCase.id)
    }

    private fun openDirectory(title: String, section: ExploreSection? = null, groupId: String? = null, query: String = "") =
        openScreen(SuggestedDogsFragment.directory(title, section, groupId, query))

    private fun openScreen(fragment: Fragment) =
        (requireActivity() as MainActivity).openScreen(fragment, R.color.explore_background)

    private fun showMessage(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
