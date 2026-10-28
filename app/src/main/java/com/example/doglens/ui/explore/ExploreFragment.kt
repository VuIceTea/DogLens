package com.example.doglens.ui.explore

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.R
import com.example.doglens.data.adapter.ExploreCategoryAdaper
import com.example.doglens.data.adapter.ExploreFeatureTopicAdapter
import com.example.doglens.data.adapter.ExploreDogUseCaseAdapter
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.FeaturedTopic
import com.example.doglens.data.model.DogUseCase
import com.example.doglens.data.repository.ExploreRepository

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private val repository by lazy { ExploreRepository() }

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
        renderLists()
        initializeSearch(view)
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
            "Danh mục"
        )

        setupSectionHeader(
            view.findViewById(R.id.headerFeaturedTopics),
            "Chủ đề nổi bật"
        )

        setupSectionHeader(
            view.findViewById(R.id.headerDogUseCases),
            "Theo nhóm công dụng"
        )
    }

    private fun setupSectionHeader(
        header: View,
        title: String
    ) {
        header.findViewById<TextView>(R.id.txtSectionTitle).text = title

        header.findViewById<TextView>(R.id.btnAllCategories).setOnClickListener {
            showMessage("Chức năng xem tất cả sẽ được bổ sung sau")
        }
    }

    // Hiển thị dữ liệu từ Repository
    private fun renderLists() {
        categoryAdapter.submitList(
            repository.getCategories()
        )

        featuredTopicAdapter.submitList(
            repository.getFeaturedTopics()
        )

        dogUseCaseAdapter.submitList(
            repository.getDogUseCases()
        )
    }

    // Xử lý tìm kiếm
    private fun initializeSearch(view: View) {
        val searchInput = view.findViewById<EditText>(R.id.txtSearch)

        searchInput.setOnEditorActionListener { input, actionId, event ->
            val searchAction = actionId == EditorInfo.IME_ACTION_SEARCH

            val enterPressed =
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP

            if (searchAction || enterPressed) {
                val keyword = input.text.toString().trim()

                if (keyword.isNotEmpty()) {
                    showMessage("Tìm kiếm: $keyword")
                }
                true
            } else {
                false
            }
        }
    }

    // Nhấn vào danh mục giống chó
    private fun onCategoryClicked(dog: Dog) {
        showMessage("Danh mục: ${dog.name}")
    }

    // Nhấn vào chủ đề nổi bật
    private fun onFeaturedTopicClicked(topic: FeaturedTopic) {
        showMessage(
            "Chủ đề: ${topic.title.replace("\n", " ")}"
        )
    }

    // Nhấn vào nhóm công dụng
    private fun onDogUseCaseClicked(useCase: DogUseCase) {
        showMessage("Nhóm: ${useCase.name}")
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}