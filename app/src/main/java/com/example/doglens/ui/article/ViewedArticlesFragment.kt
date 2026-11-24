package com.example.doglens.ui.article

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.MainActivity
import com.example.doglens.R
import kotlinx.coroutines.launch

class ViewedArticlesFragment : Fragment(R.layout.fragment_viewed_articles) {
    private val viewModel: ViewedArticlesViewModel by viewModels()
    private lateinit var adapter: ViewedArticlesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ViewedArticlesAdapter { row ->
            (requireActivity() as MainActivity).openScreen(
                ArticleDetailFragment.newInstance(row.article.id),
                R.color.explore_background
            )
        }
        view.findViewById<RecyclerView>(R.id.rvArticles).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ViewedArticlesFragment.adapter
        }
        view.findViewById<View>(R.id.btnBack).setOnClickListener { parentFragmentManager.popBackStack() }
        bindFilter(view, R.id.filterAll, null)
        bindFilter(view, R.id.filterCare, "care")
        bindFilter(view, R.id.filterHealth, "health")
        bindFilter(view, R.id.filterTraining, "training")
        bindFilter(view, R.id.filterNutrition, "nutrition")
        view.findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                viewModel.setQuery(s?.toString().orEmpty())
        })
        observe(view)
        viewModel.load()
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
            R.id.filterNutrition to "nutrition"
        ).forEach { (id, category) ->
            view.findViewById<TextView>(id).apply {
                val active = selected == category
                setBackgroundResource(if (active) R.drawable.bg_suggested_filter_selected else R.drawable.bg_suggested_filter_default)
                setTextColor(Color.parseColor(if (active) "#FFFFFF" else "#667895"))
            }
        }
    }
}
