package com.example.doglens.ui.article

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.MainActivity
import com.example.doglens.R
import com.example.doglens.utils.bindPullRefresh
import com.example.doglens.utils.wrapForPullRefresh
import kotlinx.coroutines.launch

class ViewedArticlesFragment : Fragment(R.layout.fragment_viewed_articles) {
    private val viewModel: ViewedArticlesViewModel by viewModels()
    private lateinit var adapter: ViewedArticlesAdapter
    private val browsing get() = arguments?.getBoolean(ARG_BROWSE) == true
    private var coverCards = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (browsing) {
            view.findViewById<TextView>(R.id.articleListTitle).setText(R.string.article_library_title)
            view.findViewById<TextView>(R.id.articleListSubtitle).setText(
                if (arguments?.getString(ARG_BREED) == null) R.string.article_library_subtitle
                else R.string.article_breed_subtitle
            )
        }
        val list = view.findViewById<RecyclerView>(R.id.rvArticles)
        list.layoutManager = LinearLayoutManager(requireContext())
        bindPullRefresh(list.wrapForPullRefresh()) {
            viewModel.load(browsing, arguments?.getString(ARG_BREED)).join()
        }
        val preferences = requireContext().getSharedPreferences("article_library", Context.MODE_PRIVATE)
        val layoutToggle = view.findViewById<ImageButton>(R.id.articleLayoutToggle)
        fun setLayout(useCover: Boolean) {
            val manager = list.layoutManager as LinearLayoutManager
            val firstPosition = manager.findFirstVisibleItemPosition()
            coverCards = useCover
            adapter = ViewedArticlesAdapter(showViewed = !browsing, useCoverCards = coverCards) { row ->
                (requireActivity() as MainActivity).openScreen(
                    ArticleDetailFragment.newInstance(row.article.id), R.color.explore_background
                )
            }
            adapter.submitList(viewModel.state.value.rows)
            list.adapter = adapter
            if (firstPosition != RecyclerView.NO_POSITION) manager.scrollToPositionWithOffset(firstPosition, 0)
            layoutToggle.setImageResource(
                if (coverCards) R.drawable.ic_article_layout_cover else R.drawable.ic_article_layout_list
            )
            val action = getString(
                if (coverCards) R.string.article_show_compact_layout else R.string.article_show_cover_layout
            )
            layoutToggle.contentDescription = action
            androidx.appcompat.widget.TooltipCompat.setTooltipText(layoutToggle, action)
        }
        setLayout(browsing && preferences.getBoolean("cover_cards", false))
        if (browsing) {
            layoutToggle.visibility = View.VISIBLE
            layoutToggle.setOnClickListener {
                val nextLayout = !coverCards
                preferences.edit().putBoolean("cover_cards", nextLayout).apply()
                setLayout(nextLayout)
            }
        }
        view.findViewById<View>(R.id.btnBack).setOnClickListener { parentFragmentManager.popBackStack() }
        bindFilter(view, R.id.filterAll, null)
        bindFilter(view, R.id.filterCare, "care")
        bindFilter(view, R.id.filterHealth, "health")
        bindFilter(view, R.id.filterTraining, "training")
        bindFilter(view, R.id.filterNutrition, "nutrition")
        bindFilter(view, R.id.filterBreeds, "breeds")
        view.findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                viewModel.setQuery(s?.toString().orEmpty())
        })
        observe(view)
        view.findViewById<TextView>(R.id.tvEmpty).apply {
            if (!browsing) {
                text = "Chưa tìm thấy bài viết. Bấm để mở danh mục."
                setOnClickListener { openArticleCatalog() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load(browsing, arguments?.getString(ARG_BREED))
    }

    private fun bindFilter(view: View, id: Int, category: String?) {
        view.findViewById<View>(id).setOnClickListener { viewModel.setCategory(category) }
    }

    private fun observe(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    view.findViewById<ProgressBar>(R.id.progressLoading).visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    adapter.submitList(state.rows)
                    view.findViewById<TextView>(R.id.tvEmpty).visibility = if (!state.isLoading && state.rows.isEmpty()) View.VISIBLE else View.GONE
                    updateFilters(view, state.selectedCategory)
                    state.error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    private fun updateFilters(view: View, selected: String?) {
        listOf(
            R.id.filterAll to null,
            R.id.filterCare to "care",
            R.id.filterHealth to "health",
            R.id.filterTraining to "training",
            R.id.filterNutrition to "nutrition",
            R.id.filterBreeds to "breeds"
        ).forEach { (id, category) ->
            styleSelection(view.findViewById(id), selected == category)
        }
    }

    private fun styleSelection(text: TextView, active: Boolean) {
        text.isSelected = active
        text.setBackgroundResource(
            if (active) R.drawable.bg_suggested_filter_selected else R.drawable.bg_suggested_filter_default
        )
        text.setTextColor(Color.parseColor(if (active) "#FFFFFF" else "#667895"))
    }

    companion object {
        private const val ARG_BROWSE = "browse"
        private const val ARG_BREED = "breed"

        internal fun catalog(breedId: String?) = ViewedArticlesFragment().apply {
            arguments = bundleOf(ARG_BROWSE to true, ARG_BREED to breedId)
        }
    }
}
