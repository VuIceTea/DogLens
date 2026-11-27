package com.example.doglens.ui.notification

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
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
import com.example.doglens.ui.article.ViewedArticlesFragment
import com.example.doglens.ui.favorite.FavoriteFragment
import com.example.doglens.ui.history.HistoryFragment
import kotlinx.coroutines.launch

class NotificationFragment : Fragment(R.layout.fragment_notification) {
    private val viewModel: NotificationViewModel by viewModels()
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationAdapter = NotificationAdapter(::openNotification)
        view.findViewById<RecyclerView>(R.id.rvNotifications).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
        view.findViewById<View>(R.id.btnBack).setOnClickListener { parentFragmentManager.popBackStack() }
        val readAll = view.findViewById<View>(R.id.btnReadAll)
        if (resources.configuration.run { screenWidthDp / fontScale < 300 }) {
            val header = view.findViewById<LinearLayout>(R.id.notificationHeader)
            val column = header.parent as LinearLayout
            header.removeView(readAll)
            column.addView(readAll, column.indexOfChild(header) + 1, LinearLayout.LayoutParams(-2, -2).apply {
                gravity = Gravity.END
            })
        }
        readAll.setOnClickListener { viewModel.markAllRead() }
        bindFilter(view, R.id.filterAll, NotificationFilter.ALL)
        bindFilter(view, R.id.filterRecognition, NotificationFilter.RECOGNITION)
        bindFilter(view, R.id.filterUpdate, NotificationFilter.UPDATE)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    state.error?.let {
                        viewModel.clearError()
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                    notificationAdapter.submitList(state.rows)
                    updateFilters(view, state.selectedFilter)
                    view.findViewById<TextView>(R.id.tvEmpty).visibility = if (state.rows.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun bindFilter(view: View, id: Int, filter: NotificationFilter) {
        view.findViewById<View>(id).setOnClickListener { viewModel.setFilter(filter) }
    }

    private fun updateFilters(view: View, selected: NotificationFilter) {
        listOf(
            R.id.filterAll to NotificationFilter.ALL,
            R.id.filterRecognition to NotificationFilter.RECOGNITION,
            R.id.filterUpdate to NotificationFilter.UPDATE
        ).forEach { (id, filter) ->
            view.findViewById<TextView>(id).apply {
                val active = selected == filter
                setBackgroundResource(if (active) R.drawable.bg_suggested_filter_selected else R.drawable.bg_suggested_filter_default)
                setTextColor(Color.parseColor(if (active) "#FFFFFF" else "#52627C"))
            }
        }
    }

    private fun openNotification(item: DogLensNotification) {
        viewModel.markRead(item.id)
        val destination = when (item.type) {
            NotificationType.RECOGNITION -> HistoryFragment()
            NotificationType.FAVORITE -> FavoriteFragment()
            NotificationType.ARTICLE -> ViewedArticlesFragment()
            NotificationType.UPDATE -> null
        }
        destination?.let {
            (requireActivity() as MainActivity).openScreen(it, R.color.explore_background)
        }
    }
}
