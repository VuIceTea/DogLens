package com.example.doglens.ui.explore

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doglens.R
import com.example.doglens.MainActivity
import com.example.doglens.data.AppContainer
import com.example.doglens.ui.dog.DogDetailFragment
import com.example.doglens.data.adapter.ExploreCategoryAdaper
import com.example.doglens.data.adapter.ExploreFeatureTopicAdapter
import com.example.doglens.data.adapter.ExploreDogUseCaseAdapter
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.FeaturedTopic
import com.example.doglens.data.model.DogUseCase
import com.example.doglens.data.repository.ExploreRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
        renderLists(view)
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
            showDogChoices(title) { true }
        }
    }

    // Hiển thị dữ liệu từ Repository
    private fun renderLists(view: View) = loadCatalog {
        coroutineScope {
            val categories = async(Dispatchers.IO) { repository.getCategories() }
            val topics = async(Dispatchers.IO) { repository.getFeaturedTopics() }
            val useCases = async(Dispatchers.IO) { repository.getDogUseCases() }
            val bannerData = async(Dispatchers.IO) { repository.getBanner() }
            categoryAdapter.submitList(categories.await())
            featuredTopicAdapter.submitList(topics.await())
            dogUseCaseAdapter.submitList(useCases.await())
            bannerData.await()?.let { banner ->
                val image = view.findViewById<ImageView>(R.id.imgExploreBanner)
                image.contentDescription = banner.title
                val imageSource = banner.imageUrl ?: banner.localImageRes
                if (imageSource != null) Glide.with(this@ExploreFragment).load(imageSource).into(image)
                image.setOnClickListener { showDogChoices(banner.actionText) { true } }
            }
        }
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
                    showDogChoices("Kết quả tìm kiếm") {
                        it.name.contains(keyword, ignoreCase = true) || it.description.contains(keyword, ignoreCase = true)
                    }
                }
                true
            } else {
                false
            }
        }
    }

    // Nhấn vào danh mục giống chó
    private fun onCategoryClicked(dog: Dog) {
        val size = when (dog.id) {
            "large_dog" -> "large"
            "medium_dog" -> "medium"
            "small_dog" -> "small"
            else -> null
        }
        showDogChoices(dog.name) { candidate ->
            if (size != null) candidate.sizeCategoryId == size else candidate.id in dog.exploreDogIds
        }
    }

    // Nhấn vào chủ đề nổi bật
    private fun onFeaturedTopicClicked(topic: FeaturedTopic) {
        showDogChoices(topic.title) { it.id in topic.dogIds }
    }

    // Nhấn vào nhóm công dụng
    private fun onDogUseCaseClicked(useCase: DogUseCase) {
        showDogChoices(useCase.name) { it.id in useCase.dogIds }
    }

    private fun showDogChoices(title: String, predicate: (Dog) -> Boolean) = loadCatalog {
        val dogs = AppContainer.dogRepository.getDogs().filter(predicate)
        if (dogs.isEmpty()) {
            showMessage("Chưa có giống chó phù hợp.")
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setItems(dogs.map { it.name }.toTypedArray()) { _, index ->
                    (requireActivity() as MainActivity).openScreen(DogDetailFragment.newInstance(dogs[index].id))
                }
                .show()
        }
    }

    private fun loadCatalog(action: suspend () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showMessage("Chưa tải được dữ liệu Khám phá.")
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
