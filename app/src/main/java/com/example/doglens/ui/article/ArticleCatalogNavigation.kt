package com.example.doglens.ui.article

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.doglens.MainActivity
import com.example.doglens.R
import com.example.doglens.data.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal fun Fragment.openArticleCatalog() {
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            val articles = AppContainer.articleRepository.getArticles()
            if (articles.isEmpty()) {
                Toast.makeText(requireContext(), "Danh mục chưa có bài viết.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Khám phá bài viết")
                .setItems(articles.map { it.title }.toTypedArray()) { _, position ->
                    (requireActivity() as MainActivity).openScreen(
                        ArticleDetailFragment.newInstance(articles[position].id), R.color.explore_background
                    )
                }.show()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Chưa tải được danh mục bài viết.", Toast.LENGTH_SHORT).show()
        }
    }
}
