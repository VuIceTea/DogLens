package com.example.doglens.ui.recognition

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
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
import com.example.doglens.utils.loadBreedPhoto
import com.example.doglens.R
import com.example.doglens.MainActivity
import com.example.doglens.ui.dog.DogDetailFragment
import com.example.doglens.ui.dog.bindBreedDescription
import com.example.doglens.ui.article.bindRelatedArticles
import com.example.doglens.ui.dog.SimilarDogsFragment
import com.example.doglens.ui.favorite.whenSignedInForFavorite
import com.example.doglens.data.model.Dog
import com.example.doglens.utils.ImageUtils
import com.example.doglens.utils.applyNavigationBarPadding
import com.example.doglens.utils.catalogMedian
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultFragment : Fragment(R.layout.fragment_result) {

    private val viewModel: ResultViewModel by viewModels()
    private var boundRecognitionId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        boundRecognitionId = null // view mới -> bind lại

        view.findViewById<View>(R.id.bottomActions).applyNavigationBarPadding()
        view.findViewById<View>(R.id.btnBack)
            .setOnClickListener { parentFragmentManager.popBackStack() }

        view.findViewById<View>(R.id.tvDogName).setOnClickListener {
            val content = viewModel.state.value as? ResultUiState.Content ?: return@setOnClickListener
            (requireActivity() as MainActivity).openScreen(DogDetailFragment.newInstance(content.dog.id))
        }

        val btnFavorite = view.findViewById<MaterialButton>(R.id.btnFavorite)
        btnFavorite.setOnClickListener {
            if (viewModel.state.value is ResultUiState.Content) {
                whenSignedInForFavorite(viewModel::toggleFavorite)
            }
        }
        view.findViewById<View>(R.id.btnShare).setOnClickListener {
            (viewModel.state.value as? ResultUiState.Content)?.let { shareResult(it) }
        }
        view.findViewById<View>(R.id.btnSeeAllSimilar).setOnClickListener {
            val content = viewModel.state.value as? ResultUiState.Content
                ?: return@setOnClickListener
            (requireActivity() as MainActivity).openScreen(
                SimilarDogsFragment.newInstance(content.dog.id),
                R.color.explore_background
            )
        }
        view.findViewById<View>(R.id.btnDownload).setOnClickListener {
            (viewModel.state.value as? ResultUiState.Content)?.let { saveImage(it.recognition.imageUrl) }
        }

        val id = requireArguments().getString(ARG_RECOGNITION_ID).orEmpty()
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.resultRefresh)
        refresh.setOnRefreshListener {
            boundRecognitionId = null
            viewModel.load(id, refresh = true)
        }
        viewModel.load(id)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state != ResultUiState.Loading) refresh.isRefreshing = false
                    when (state) {
                        ResultUiState.Loading -> Unit
                        ResultUiState.NotFound -> {
                            Toast.makeText(
                                requireContext(),
                                "Không tìm thấy kết quả",
                                Toast.LENGTH_SHORT
                            ).show()
                            parentFragmentManager.popBackStack()
                        }

                        is ResultUiState.Content -> {
                            if (state.favoriteError) {
                                viewModel.clearFavoriteError()
                                Toast.makeText(requireContext(), R.string.favorite_update_error, Toast.LENGTH_SHORT)
                                    .show()
                            }
                            if (boundRecognitionId != state.recognition.id) {
                                bindContent(view, state)
                                boundRecognitionId = state.recognition.id
                            }
                            renderFavorite(btnFavorite, state.isFavorite)
                        }
                    }
                }
            }
        }
    }

    private fun bindContent(view: View, state: ResultUiState.Content) {
        val dog = state.dog
        val confidence = state.recognition.confidence

        Glide.with(this).load(state.recognition.imageUrl).centerCrop()
            .into(view.findViewById(R.id.imgResult))

        view.findViewById<TextView>(R.id.tvDogName).text = dog.name
        view.findViewById<TextView>(R.id.tvConfidence).text = "$confidence%"
        view.findViewById<TextView>(R.id.tvAccuracyLabel).text = when {
            confidence >= 90 -> "Độ tin cậy cao"
            confidence >= 75 -> "Độ tin cậy khá"
            else -> "Độ tin cậy trung bình"
        }
        bindBreedDescription(view.findViewById(R.id.tvSummary), dog)
        bindRelatedArticles(view.findViewById(R.id.relatedArticlesSection), dog.id)

        view.findViewById<View>(R.id.resultTraitsSection).visibility =
            if (state.traits.isEmpty()) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.resultRelatedSection).visibility =
            if (state.relatedDogs.isEmpty()) View.GONE else View.VISIBLE
        bindCards(view.findViewById(R.id.gridTraits), state.traits.map { (trait, link) ->
            InfoCard(trait.name, link.description.orEmpty(), trait.colorHex, traitIcon(trait.iconKey))
        })
        view.findViewById<View>(R.id.resultCareSection).visibility =
            if (state.care.isEmpty()) View.GONE else View.VISIBLE
        bindCards(view.findViewById(R.id.gridCare), state.care.map { (tip, link) ->
            val icon = when (tip.id) {
                "grooming" -> R.drawable.ic_detail_brush
                "exercise" -> R.drawable.ic_trait_run
                "social" -> R.drawable.ic_trait_heart_outline
                "training" -> R.drawable.ic_trait_brain
                else -> R.drawable.ic_detail_paw
            }
            InfoCard(tip.name, link.description.orEmpty(), tip.colorHex, icon)
        })
        bindInfo(view, dog)
        bindRelatedDogs(view.findViewById(R.id.relatedDogsList), state.relatedDogs)
    }

    // ---------- Đặc điểm nổi bật: lưới 2 cột ----------

    private data class InfoCard(val title: String, val description: String, val color: String, @DrawableRes val icon: Int)

    private fun bindCards(grid: GridLayout, cards: List<InfoCard>) {
        grid.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        cards.forEach { card ->
            val cell = inflater.inflate(R.layout.item_result_trait, grid, false)
            cell.findViewById<View>(R.id.traitCircle).backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(card.color))
            cell.findViewById<ImageView>(R.id.traitIcon).setImageResource(card.icon)
            cell.findViewById<TextView>(R.id.traitName).text = card.title
            cell.findViewById<TextView>(R.id.traitDesc).text = card.description

            // width 0 + weight 1 -> 2 cột chia đều
            cell.layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply { width = 0 }
            grid.addView(cell)
        }
    }

    @DrawableRes
    private fun traitIcon(iconKey: String?): Int = when (iconKey) {
        "smile" -> R.drawable.ic_trait_smile
        "brain" -> R.drawable.ic_trait_brain
        "heart" -> R.drawable.ic_trait_heart_outline
        "run" -> R.drawable.ic_trait_run
        "leaf" -> R.drawable.ic_trait_leaf
        else -> R.drawable.ic_trait_star
    }

    // ---------- Thông tin cơ bản 2x2 ----------

    private fun bindInfo(view: View, dog: Dog) {
        setInfoCell(
            view, R.id.infoWeight, R.drawable.ic_weight, "Cân nặng",
            dog.weightStandard ?: dog.weightMedianKg?.let { catalogMedian(it, "kg") }
                ?: range(dog.minWeightKg, dog.maxWeightKg, "kg")
        )
        setInfoCell(
            view, R.id.infoHeight, R.drawable.ic_height_ruler, "Chiều cao",
            dog.heightStandard ?: range(dog.minHeightCm, dog.maxHeightCm, "cm")
        )
        setInfoCell(
            view, R.id.infoLifespan, R.drawable.ic_result_clock, "Tuổi thọ",
            dog.lifespanMedianYears?.let { catalogMedian(it, "năm") } ?: dog.lifespanReference
                ?: range(dog.minLifespanYears, dog.maxLifespanYears, " năm")
        )
        setInfoCell(
            view, R.id.infoCoat, R.drawable.ic_result_palette, "Màu lông",
            dog.coatColors ?: "Đang cập nhật"
        )
    }

    private fun setInfoCell(
        root: View,
        cellId: Int,
        @DrawableRes icon: Int,
        label: String,
        value: String
    ) {
        val cell = root.findViewById<View>(cellId)
        cell.findViewById<ImageView>(R.id.infoIcon).setImageResource(icon)
        cell.findViewById<TextView>(R.id.infoLabel).text = label
        cell.findViewById<TextView>(R.id.infoValue).text = value
    }

    private fun range(min: Int?, max: Int?, unit: String): String = when {
        min != null && max != null -> "$min – $max$unit"
        min != null -> "$min$unit"
        max != null -> "$max$unit"
        else -> "Đang cập nhật"
    }

    private fun bindRelatedDogs(container: LinearLayout, dogs: List<Dog>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        dogs.forEach { dog ->
            val card = inflater.inflate(R.layout.item_result_related_dog, container, false)
            val image = card.findViewById<ImageView>(R.id.imgRelatedDog)

            image.loadBreedPhoto(dog)

            card.findViewById<TextView>(R.id.tvRelatedDogName).text = dog.name
            card.findViewById<TextView>(R.id.tvRelatedDogDescription).text = dog.description
            card.setOnClickListener {
                (requireActivity() as MainActivity).openScreen(
                    DogDetailFragment.newInstance(dog.id),
                    R.color.explore_background
                )
            }
            container.addView(card)
        }
    }

    // ---------- Yêu thích / Chia sẻ / Lưu ảnh ----------

    private fun renderFavorite(button: MaterialButton, saved: Boolean) {
        val blue = ContextCompat.getColor(requireContext(), R.color.identify_blue)
        if (saved) {
            button.text = "Đã lưu vào yêu thích"
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E6F1FD"))
            button.setTextColor(blue)
            button.setIconResource(R.drawable.ic_heart_favorite)
            button.iconTint = ColorStateList.valueOf(Color.parseColor("#FF3B5C"))
        } else {
            button.text = "Lưu vào yêu thích"
            button.backgroundTintList = ColorStateList.valueOf(blue)
            button.setTextColor(Color.WHITE)
            button.setIconResource(R.drawable.ic_heart_outline)
            button.iconTint = ColorStateList.valueOf(Color.WHITE)
        }
    }

    private fun shareResult(state: ResultUiState.Content) {
        val text =
            "DogLens nhận diện: ${state.dog.name} – ${state.recognition.confidence}% độ tin cậy 🐶"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Chia sẻ kết quả"))
    }

    // Glide tải được cả content:// (mock) lẫn URL (API) -> lưu vào thư viện
    private fun saveImage(imageUrl: String) {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val bitmap = Glide.with(appContext).asBitmap().load(imageUrl).submit().get()
                    ImageUtils.saveToGallery(appContext, bitmap)
                } catch (e: Exception) {
                    false
                }
            }
            Toast.makeText(
                appContext,
                if (ok) "Đã lưu ảnh vào Pictures/DogLens" else "Không lưu được ảnh (cần Android 10 trở lên)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        private const val ARG_RECOGNITION_ID = "arg_recognition_id"

        fun newInstance(recognitionId: String) = ResultFragment().apply {
            arguments = bundleOf(ARG_RECOGNITION_ID to recognitionId)
        }
    }
}
