package com.example.doglens.ui.article

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.doglens.R
import com.example.doglens.utils.addCatalogSources
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ArticleDetailFragment : Fragment(R.layout.fragment_article_detail) {
    private val viewModel: ArticleDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btnBack).setOnClickListener { parentFragmentManager.popBackStack() }
        val bookmarkButton = view.findViewById<ImageButton>(R.id.btnBookmark)
        val articleId = requireArguments().getString(ARG_ARTICLE_ID).orEmpty()
        val loadStatus = view.findViewById<TextView>(R.id.articleLoadStatus)
        loadStatus.setOnClickListener { viewModel.load(articleId, retry = true) }
        bookmarkButton.setOnClickListener { viewModel.toggleBookmark() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    bookmarkButton.isEnabled = state.article != null && !state.isSaving
                    if (state.article != null) state.error?.let {
                        viewModel.clearError()
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                    bookmarkButton.setImageResource(
                        if (state.isBookmarked) R.drawable.ic_article_bookmark_saved
                        else R.drawable.ic_article_bookmark_outline
                    )
                    val article = state.article
                    view.findViewById<View>(R.id.articleScroll).visibility = if (article == null) View.GONE else View.VISIBLE
                    loadStatus.visibility = if (article == null) View.VISIBLE else View.GONE
                    loadStatus.text = if (state.isLoading) "Đang tải bài viết…" else "Không thể tải bài viết. Thử lại"
                    loadStatus.isClickable = !state.isLoading
                    if (article == null) return@collect
                    val image = view.findViewById<ImageView>(R.id.imgHero)
                    when {
                        article.localImageRes != null -> image.setImageResource(article.localImageRes)
                        !article.imageUrl.isNullOrBlank() -> Glide.with(this@ArticleDetailFragment).load(article.imageUrl).centerCrop().into(image)
                    }
                    view.findViewById<TextView>(R.id.tvCategory).apply {
                        text = state.category?.name.orEmpty()
                        state.category?.let { backgroundTintList = ColorStateList.valueOf(Color.parseColor(it.colorHex)) }
                    }
                    view.findViewById<TextView>(R.id.tvTitle).text = article.title
                    view.findViewById<TextView>(R.id.tvDate).text = article.publishedAt
                    view.findViewById<TextView>(R.id.tvViews).text =
                        if (article.viewCount > 0) "${article.viewCount} lượt xem" else "— lượt xem"
                    view.findViewById<TextView>(R.id.tvReadTime).text = "${article.readMinutes} phút đọc"
                    view.findViewById<TextView>(R.id.tvSummary).text = article.summary
                    renderSections(view, state.sections)
                    view.findViewById<LinearLayout>(R.id.sectionsContainer).addCatalogSources(article.sources)
                }
            }
        }
        viewModel.load(articleId)
    }

    private fun renderSections(view: View, sections: List<com.example.doglens.data.model.ArticleSection>) {
        val toc = view.findViewById<LinearLayout>(R.id.tocContainer)
        val body = view.findViewById<LinearLayout>(R.id.sectionsContainer)
        val scroll = view.findViewById<ScrollView>(R.id.articleScroll)
        toc.removeAllViews(); body.removeAllViews()
        sections.forEachIndexed { index, section ->
            val sectionView = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(12), 0, dp(12))
                addView(TextView(requireContext()).apply {
                    text = "${index + 1}. ${section.heading}"
                    textSize = 19f
                    setTextColor(Color.parseColor("#10245E"))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                addView(TextView(requireContext()).apply {
                    text = section.body
                    textSize = 15f
                    setTextColor(Color.parseColor("#667895"))
                    setLineSpacing(dp(4).toFloat(), 1f)
                    setPadding(0, dp(8), 0, 0)
                })
            }
            body.addView(sectionView)

            val tocRow = LinearLayout(requireContext()).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                background = android.util.TypedValue().let {
                    val out = android.util.TypedValue()
                    requireContext().theme.resolveAttribute(
                        android.R.attr.selectableItemBackground,
                        out,
                        true
                    )
                    androidx.core.content.ContextCompat.getDrawable(requireContext(), out.resourceId)
                }
                setPadding(dp(4), dp(5), dp(2), dp(5))
                setOnClickListener {
                    sectionView.post { smoothScrollToView(scroll, sectionView) }
                }
            }
            tocRow.addView(TextView(requireContext()).apply {
                text = "${index + 1}.  ${section.heading}"
                textSize = 14f
                setTextColor(Color.parseColor("#667895"))
            }, LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
            })
            tocRow.addView(ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_chevron_right)
                imageTintList = ColorStateList.valueOf(Color.parseColor("#667895"))
                contentDescription = "Đi đến ${section.heading}"
            }, LinearLayout.LayoutParams(dp(30), dp(30)))
            toc.addView(tocRow, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ))
        }
    }

    private fun smoothScrollToView(scrollView: ScrollView, target: View) {
        var y = 0
        var current: View? = target
        while (current != null && current !== scrollView) {
            y += current.top
            current = current.parent as? View
        }
        scrollView.smoothScrollTo(0, (y - dp(12)).coerceAtLeast(0))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val ARG_ARTICLE_ID = "arg_article_id"
        fun newInstance(articleId: String) = ArticleDetailFragment().apply {
            arguments = bundleOf(ARG_ARTICLE_ID to articleId)
        }
    }
}
