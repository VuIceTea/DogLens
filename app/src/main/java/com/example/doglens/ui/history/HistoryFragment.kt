package com.example.doglens.ui.history

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
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
import com.example.doglens.data.adapter.HistoryAdapter
import com.example.doglens.data.model.RecognitionHistoryItem
import com.example.doglens.ui.recognition.ResultFragment
import com.example.doglens.utils.applyNavigationBarPadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private val viewModel: HistoryViewModel by viewModels()

    private lateinit var adapter: HistoryAdapter
    private lateinit var filterButtons: Map<HistoryFilter, MaterialButton>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBack).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.btnClearAll).setOnClickListener { confirmClearAll() }
        view.findViewById<View>(R.id.btnIdentifyNow).setOnClickListener {
            (requireActivity() as MainActivity).goToHome() // về Trang chủ để chọn ảnh
        }

        setupList(view)
        setupSearch(view)
        setupFilters(view)

        viewModel.refresh()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(view, it) }
            }
        }
    }

    private fun setupList(view: View) {
        adapter = HistoryAdapter(
            onItemClick = { item ->
                (requireActivity() as MainActivity).openScreen(ResultFragment.newInstance(item.result.id))
            },
            onFavoriteClick = { item ->
                viewModel.toggleFavorite(item)
                val msg = if (item.isFavorite) "Đã bỏ ${item.dog.name} khỏi yêu thích"
                else "Đã lưu ${item.dog.name} vào yêu thích"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { item -> confirmDelete(item) }
        )

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvHistory)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.applyNavigationBarPadding()
    }

    private fun setupSearch(view: View) {
        view.findViewById<EditText>(R.id.edtSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.setQuery(s?.toString().orEmpty())
            }
        })
    }

    private fun setupFilters(view: View) {
        filterButtons = mapOf(
            HistoryFilter.ALL to view.findViewById(R.id.filterAll),
            HistoryFilter.TODAY to view.findViewById(R.id.filterToday),
            HistoryFilter.LAST_7_DAYS to view.findViewById(R.id.filter7Days),
            HistoryFilter.LAST_30_DAYS to view.findViewById(R.id.filter30Days)
        )
        filterButtons.forEach { (filter, button) ->
            button.setOnClickListener { viewModel.setFilter(filter) }
        }
    }

    private fun render(view: View, state: HistoryUiState) {
        state.error?.let {
            viewModel.clearError()
            android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
        }
        adapter.submitList(state.rows)

        // Nút lọc đang chọn: nền xanh chữ trắng; còn lại nền xám chữ xám
        filterButtons.forEach { (filter, button) ->
            val selected = filter == state.filter
            button.backgroundTintList = ColorStateList.valueOf(
                Color.parseColor(if (selected) "#1585FD" else "#E9EFF7")
            )
            button.setTextColor(Color.parseColor(if (selected) "#FFFFFF" else "#526581"))
        }

        val btnClearAll = view.findViewById<View>(R.id.btnClearAll)
        btnClearAll.isEnabled = state.totalCount > 0
        btnClearAll.alpha = if (state.totalCount > 0) 1f else 0.35f

        // Trạng thái trống
        val isEmpty = !state.loading && state.rows.isEmpty()
        view.findViewById<View>(R.id.emptyState).visibility = if (isEmpty) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.rvHistory).visibility = if (isEmpty) View.GONE else View.VISIBLE

        val noHistoryAtAll = state.totalCount == 0
        view.findViewById<TextView>(R.id.tvEmptyTitle).text =
            if (noHistoryAtAll) "Chưa có lịch sử nhận diện" else "Không tìm thấy kết quả"
        view.findViewById<TextView>(R.id.tvEmptyMessage).text =
            if (noHistoryAtAll) "Chụp hoặc tải ảnh chú chó lên để AI nhận diện giống chó nhé!"
            else "Thử đổi bộ lọc thời gian hoặc từ khoá tìm kiếm."
        view.findViewById<View>(R.id.btnIdentifyNow).visibility =
            if (noHistoryAtAll) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(item: RecognitionHistoryItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xoá kết quả này?")
            .setMessage("Kết quả nhận diện ${item.dog.name} sẽ bị xoá khỏi lịch sử.")
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Xoá") { _, _ -> viewModel.delete(item) }
            .show()
    }

    private fun confirmClearAll() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xoá toàn bộ lịch sử?")
            .setMessage("Tất cả kết quả nhận diện sẽ bị xoá và không thể khôi phục.")
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Xoá tất cả") { _, _ -> viewModel.clearAll() }
            .show()
    }
}
