package com.example.doglens.ui.recognition

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.doglens.MainActivity
import com.example.doglens.R
import com.example.doglens.utils.applyNavigationBarPadding
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RecognitionFragment : Fragment(R.layout.fragment_recognition) {

    // Trạng thái của từng bước (chỉ dùng cho UI)
    private enum class StepState { DONE, ACTIVE, PENDING }

    // 1 bước hiển thị. doneAt = % mà bước này được coi là xong
    private class Step(
        @DrawableRes val iconRes: Int,
        val title: String,
        val subTitle: String,
        val doneAt: Int
    )

    // Các view của 1 dòng bước
    private class StepViews(
        val tile: View,
        val icon: ImageView,
        val done: View,
        val active: View,
        val pending: View
    )

    private val steps = listOf(
        Step(R.drawable.ic_step_image, "Đang xử lý hình ảnh", "Tối ưu chất lượng ảnh", 35),
        Step(R.drawable.ic_step_search, "Phân tích đặc điểm", "Nhận diện qua AI", 70),
        Step(R.drawable.ic_step_doc, "So sánh với cơ sở dữ liệu", "Đối chiếu danh mục giống chó", 92),
        Step(R.drawable.ic_step_shield, "Hoàn tất nhận diện", "Hiển thị kết quả", 100)
    )

    private val viewModel: RecognitionViewModel by viewModels()
    private val stepViews = mutableListOf<StepViews>()

    private lateinit var progressRing: CircularProgressIndicator
    private lateinit var tvPercent: TextView

    private var navigated = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUri = Uri.parse(requireArguments().getString(ARG_URI))

        view.applyNavigationBarPadding()
        progressRing = view.findViewById(R.id.progressRing)
        tvPercent = view.findViewById(R.id.tvPercent)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack() // ViewModel bị huỷ -> tiến trình dừng
        }

        Glide.with(this).load(imageUri).centerCrop().into(view.findViewById(R.id.imgPhoto))
        // ImageView thường không chạy GIF -> dùng Glide.asGif()
        Glide.with(this).asGif().load(R.drawable.corgi_magnifier)
            .into(view.findViewById(R.id.imgCorgi))

        buildSteps(view.findViewById(R.id.stepsContainer))
        startBubbleTyping(view.findViewById(R.id.tvBubble))

        viewModel.start(imageUri)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is RecognitionUiState.Running -> render(state.percent)
                        is RecognitionUiState.Success -> {
                            render(100)
                            delay(500)
                            goToResult(state.recognitionId)
                        }

                        is RecognitionUiState.Failed -> goToError(imageUri, state.dogDetected)
                        is RecognitionUiState.Unavailable -> {
                            if (!navigated) {
                                navigated = true
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                                parentFragmentManager.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stepViews.clear()
    }

    private fun buildSteps(container: LinearLayout) {
        container.removeAllViews()
        stepViews.clear()
        val inflater = LayoutInflater.from(requireContext())

        steps.forEachIndexed { index, step ->
            val row = inflater.inflate(R.layout.item_identify_step, container, false)
            row.findViewById<TextView>(R.id.stepTitle).text = step.title
            row.findViewById<TextView>(R.id.stepSubTitle).text = step.subTitle

            val icon = row.findViewById<ImageView>(R.id.stepIcon)
            icon.setImageResource(step.iconRes)

            row.findViewById<View>(R.id.stepDivider).visibility =
                if (index == steps.lastIndex) View.GONE else View.VISIBLE

            container.addView(row)
            stepViews += StepViews(
                tile = row.findViewById(R.id.stepTile),
                icon = icon,
                done = row.findViewById(R.id.statusDone),
                active = row.findViewById(R.id.statusActive),
                pending = row.findViewById(R.id.statusPending)
            )
        }
    }

    private fun render(percent: Int) {
        progressRing.setProgressCompat(percent, true)
        tvPercent.text = "$percent%"

        val blue = ContextCompat.getColor(requireContext(), R.color.identify_blue)
        val grey = ContextCompat.getColor(requireContext(), R.color.identify_icon_pending)

        // bước đầu tiên chưa xong là bước đang chạy (-1 = xong hết)
        val activeIndex = steps.indexOfFirst { percent < it.doneAt }

        stepViews.forEachIndexed { index, v ->
            val state = when {
                activeIndex == -1 || index < activeIndex -> StepState.DONE
                index == activeIndex -> StepState.ACTIVE
                else -> StepState.PENDING
            }
            v.done.visibility = if (state == StepState.DONE) View.VISIBLE else View.GONE
            v.active.visibility = if (state == StepState.ACTIVE) View.VISIBLE else View.GONE
            v.pending.visibility = if (state == StepState.PENDING) View.VISIBLE else View.GONE

            val isPending = state == StepState.PENDING
            v.tile.setBackgroundResource(
                if (isPending) R.drawable.bg_identify_tile_pending else R.drawable.bg_identify_tile_active
            )
            v.icon.imageTintList =
                ColorStateList.valueOf(if (isPending) grey else blue) // icon gốc màu đen
        }
    }

    // Chữ hiện dần; phần chưa hiện để trong suốt nên bong bóng không bị giật kích thước
    private fun startBubbleTyping(tv: TextView) {
        val text = "Đang tìm hiểu\nngười bạn 4 chân\ncủa bạn..."
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                for (i in 0..text.length) {
                    val spannable = SpannableString(text)
                    if (i < text.length) {
                        spannable.setSpan(
                            ForegroundColorSpan(Color.TRANSPARENT), i, text.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    tv.text = spannable
                    delay(45)
                }
                delay(1500)
            }
        }
    }

    private fun goToResult(recognitionId: String) {
        if (navigated) return
        navigated = true
        (requireActivity() as MainActivity).replaceScreen(ResultFragment.newInstance(recognitionId))
    }

    private fun goToError(imageUri: Uri, dogDetected: Boolean) {
        if (navigated) return
        navigated = true
        (requireActivity() as MainActivity).replaceScreen(
            RecognitionErrorFragment.newInstance(imageUri, dogDetected)
        )
    }

    companion object {
        private const val ARG_URI = "arg_uri"

        fun newInstance(uri: Uri) = RecognitionFragment().apply {
            arguments = bundleOf(ARG_URI to uri.toString())
        }
    }
}
