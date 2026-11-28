package com.example.doglens.ui.dog

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.MainActivity
import com.example.doglens.R
import com.example.doglens.ui.explore.ExploreSection
import com.example.doglens.utils.wrapForPullRefresh
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch

class SuggestedDogsFragment : Fragment(R.layout.fragment_suggested_dogs) {

    private val viewModel: SuggestedDogsViewModel by viewModels()
    private lateinit var adapter: SuggestedDogsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val section = arguments?.getString(ARG_SECTION)?.let(ExploreSection::valueOf)
        val sourceId = arguments?.getString(ARG_SOURCE_DOG_ID)
        val groupId = arguments?.getString(ARG_GROUP)
        val browsingCatalog = arguments?.getString(ARG_TITLE) != null
        val useGrid = section == ExploreSection.CATEGORIES ||
            (browsingCatalog && section == null && arguments?.getString(ARG_QUERY).isNullOrBlank())
        val itemLayout = when {
            useGrid -> R.layout.item_explore_breed_tile
            section == ExploreSection.TOPICS -> R.layout.item_explore_breed_feature
            else -> R.layout.item_suggested_dog
        }
        adapter = SuggestedDogsAdapter(itemLayout) { dog ->
            (requireActivity() as MainActivity).openScreen(
                DogDetailFragment.newInstance(dog.id),
                R.color.explore_background
            )
        }

        val list = view.findViewById<RecyclerView>(R.id.rvSuggestedDogs).apply {
            layoutManager = if (useGrid) {
                val width = resources.configuration.run { screenWidthDp / fontScale }
                GridLayoutManager(requireContext(), if (width >= 720) 3 else if (width >= 300) 2 else 1)
            } else {
                LinearLayoutManager(requireContext())
            }
            adapter = this@SuggestedDogsFragment.adapter
        }
        val refresh = list.wrapForPullRefresh()
        refresh.setOnRefreshListener { viewModel.load(sourceId, section, groupId, refresh = true) }
        arguments?.getString(ARG_TITLE)?.let { title ->
            view.findViewById<TextView>(R.id.suggestedTitle).text = title
            view.findViewById<TextView>(R.id.suggestedSubtitle).text = "Chọn một giống để xem chi tiết"
            listOf(R.id.filterSize, R.id.filterTrait, R.id.filterOrigin).forEach {
                view.findViewById<View>(it).visibility = View.GONE
            }
        }

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        bindFilter(view, R.id.filterAll, SuggestedFilter.ALL)
        bindFilter(view, R.id.filterPopular, SuggestedFilter.POPULAR)
        bindFilter(view, R.id.filterSize, SuggestedFilter.SIZE)
        bindFilter(view, R.id.filterTrait, SuggestedFilter.TRAIT)
        bindFilter(view, R.id.filterOrigin, SuggestedFilter.ORIGIN)

        val search = view.findViewById<EditText>(R.id.etSearch)
        val initialQuery = viewModel.state.value.query.ifBlank { arguments?.getString(ARG_QUERY).orEmpty() }
        search.setText(initialQuery)
        if (initialQuery.isNotBlank()) viewModel.setQuery(initialQuery)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        observeState(view, refresh)
        viewModel.load(sourceId, section, groupId)
    }

    private fun bindFilter(view: View, id: Int, filter: SuggestedFilter) {
        view.findViewById<View>(id).setOnClickListener {
            viewModel.setFilter(filter)
        }
    }

    private fun observeState(view: View, refresh: SwipeRefreshLayout) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (!state.isLoading) refresh.isRefreshing = false
                    view.findViewById<ProgressBar>(R.id.progressLoading).visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE
                    adapter.submitList(state.rows)
                    view.findViewById<TextView>(R.id.tvEmpty).visibility =
                        if (!state.isLoading && state.rows.isEmpty()) View.VISIBLE else View.GONE
                    updateFilterStyles(view, state.selectedFilter)
                    state.error?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateFilterStyles(view: View, selectedFilter: SuggestedFilter) {
        val filters = listOf(
            R.id.filterAll to SuggestedFilter.ALL,
            R.id.filterPopular to SuggestedFilter.POPULAR,
            R.id.filterSize to SuggestedFilter.SIZE,
            R.id.filterTrait to SuggestedFilter.TRAIT,
            R.id.filterOrigin to SuggestedFilter.ORIGIN
        )
        filters.forEach { (id, filter) ->
            val text = view.findViewById<TextView>(id)
            val selected = selectedFilter == filter
            text.setBackgroundResource(
                if (selected) R.drawable.bg_suggested_filter_selected
                else R.drawable.bg_suggested_filter_default
            )
            text.setTextColor(Color.parseColor(if (selected) "#FFFFFF" else "#667895"))
        }
    }

    companion object {
        private const val ARG_SOURCE_DOG_ID = "arg_source_dog_id"
        private const val ARG_TITLE = "directory_title"
        private const val ARG_SECTION = "directory_section"
        private const val ARG_GROUP = "directory_group"
        private const val ARG_QUERY = "directory_query"

        fun newInstance(sourceDogId: String? = null) = SuggestedDogsFragment().apply {
            arguments = bundleOf(ARG_SOURCE_DOG_ID to sourceDogId)
        }

        internal fun directory(title: String, section: ExploreSection?, groupId: String?, query: String = "") =
            SuggestedDogsFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to title, ARG_SECTION to section?.name, ARG_GROUP to groupId,
                    ARG_QUERY to query
                )
            }
    }
}
