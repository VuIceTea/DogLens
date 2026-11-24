package com.example.doglens.ui.dog

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.doglens.MainActivity
import com.example.doglens.R
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.SimilarDogItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class SimilarDogsFragment : Fragment(R.layout.fragment_similar_dogs) {

    private val repository = AppContainer.dogRepository
    private lateinit var sourceDog: Dog
    private var allItems: List<SimilarDogItem> = emptyList()
    private var traitNames: Map<String, List<String>> = emptyMap()
    private var sameSizeOnly = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btnFilter).setOnClickListener {
            if (!::sourceDog.isInitialized) return@setOnClickListener
            sameSizeOnly = !sameSizeOnly
            renderItems(view)
        }

        loadData(view)
    }

    private fun loadData(view: View) {
        val dogId = requireArguments().getString(ARG_DOG_ID).orEmpty()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val source = repository.getDogById(dogId)
                if (source == null) {
                    Toast.makeText(requireContext(), "Không tìm thấy giống chó", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                    return@launch
                }

                sourceDog = source
                val dogs = repository.getDogs().filterNot { it.id == source.id }
                val traits = repository.getTraits().associateBy { it.id }
                traitNames = dogs.associate { dog ->
                    dog.id to repository.getDogTraits(dog.id)
                        .mapNotNull { traits[it.traitId]?.name }
                        .take(3)
                }
                allItems = dogs
                    .map { SimilarDogItem(it, calculateSimilarity(source, it)) }
                    .sortedByDescending { it.similarityPercent }

                bindSource(view, source)
                view.findViewById<ProgressBar>(R.id.progressLoading).visibility = View.GONE
                renderItems(view)
                renderSuggestions(view)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                view.findViewById<ProgressBar>(R.id.progressLoading).visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Không thể tải giống tương tự: ${exception.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun bindSource(view: View, dog: Dog) {
        view.findViewById<TextView>(R.id.tvSourceName).text = dog.name
        loadDogImage(view.findViewById(R.id.imgSourceDog), dog)
        view.findViewById<View>(R.id.sourceCard).setOnClickListener { openDetail(dog.id) }
    }

    private fun renderItems(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.similarList)
        val empty = view.findViewById<TextView>(R.id.tvEmpty)
        val filterStatus = view.findViewById<TextView>(R.id.tvFilterStatus)

        val visibleItems = if (sameSizeOnly) {
            allItems.filter { it.dog.sizeCategoryId == sourceDog.sizeCategoryId }
        } else {
            allItems
        }

        filterStatus.visibility = if (sameSizeOnly) View.VISIBLE else View.GONE
        filterStatus.text = "Đang lọc: cùng kích thước  ×"
        filterStatus.setOnClickListener {
            sameSizeOnly = false
            renderItems(view)
        }

        container.removeAllViews()
        empty.visibility = if (visibleItems.isEmpty()) View.VISIBLE else View.GONE

        visibleItems.take(5).forEach { item ->
            container.addView(createDogCard(item), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(164)
            ).apply { bottomMargin = dp(12) })
        }
    }

    private fun createDogCard(item: SimilarDogItem): View {
        val dog = item.dog
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded("#FFFFFF", 20)
            elevation = dp(2).toFloat()
            setPadding(dp(8), dp(8), dp(8), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { openDetail(dog.id) }
        }

        card.addView(ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded("#E5EEF8", 16)
            clipToOutline = true
            contentDescription = dog.name
            loadDogImage(this, dog)
        }, LinearLayout.LayoutParams(dp(108), dp(148)))

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(2), 0)
        }

        val titleRow = LinearLayout(requireContext()).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        titleRow.addView(text(dog.name, 16, "#10245E", true).apply {
            maxLines = 2
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        titleRow.addView(text("${item.similarityPercent}% tương tự", 13, "#168FF2", true).apply {
            background = rounded("#E3F2FF", 20)
            setPadding(dp(9), dp(6), dp(9), dp(6))
        })
        content.addView(titleRow)

        content.addView(text(dog.summary ?: dog.description, 12, "#667895", false).apply {
            maxLines = 2
            setPadding(0, dp(3), 0, dp(5))
        })

        val chips = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val chipColors = listOf(
            Pair("#E8F4FF", "#168FF2"),
            Pair("#F2EAFE", "#8653E8"),
            Pair("#E7F8EE", "#139A55")
        )
        traitNames[dog.id].orEmpty().forEachIndexed { index, name ->
            val colors = chipColors[index % chipColors.size]
            chips.addView(text(name, 11, colors.second, false).apply {
                background = rounded(colors.first, 18)
                setPadding(dp(8), dp(5), dp(8), dp(5))
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(5) })
        }
        content.addView(chips)

        val info = LinearLayout(requireContext()).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(7), 0, 0)
        }
        info.addView(infoValue(R.drawable.ic_detail_scale, range(dog.minWeightKg, dog.maxWeightKg, "kg")))
        info.addView(View(requireContext()).apply { setBackgroundColor(Color.parseColor("#DCE5EF")) },
            LinearLayout.LayoutParams(dp(1), dp(24)).apply { marginStart = dp(12); marginEnd = dp(12) })
        info.addView(infoValue(R.drawable.ic_detail_ruler, range(dog.minHeightCm, dog.maxHeightCm, "cm")))
        info.addView(ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_chevron_right)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#7885A6"))
        }, LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginStart = dp(6) })
        content.addView(info)

        card.addView(content, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        return card
    }

    private fun renderSuggestions(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.suggestionList)
        container.removeAllViews()
        allItems.takeLast(2).reversed().forEach { item ->
            val dog = item.dog
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded("#F0F7FF", 18)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setOnClickListener { openDetail(dog.id) }
            }
            card.addView(ImageView(requireContext()).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = rounded("#E5EEF8", 14)
                clipToOutline = true
                loadDogImage(this, dog)
            }, LinearLayout.LayoutParams(dp(78), dp(82)))
            card.addView(text(dog.name, 13, "#10245E", true).apply {
                maxLines = 3
                setPadding(dp(9), 0, dp(4), 0)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_chevron_right)
            }, LinearLayout.LayoutParams(dp(22), dp(22)))
            container.addView(card, LinearLayout.LayoutParams(0, dp(100), 1f).apply {
                marginEnd = dp(6)
                marginStart = dp(6)
            })
        }
    }

    private fun calculateSimilarity(source: Dog, candidate: Dog): Int {
        var score = 54f
        if (source.sizeCategoryId == candidate.sizeCategoryId) score += 16f
        score += closeness(source.minWeightKg, candidate.minWeightKg, 35, 5f)
        score += closeness(source.maxHeightCm, candidate.maxHeightCm, 50, 5f)
        score += scoreCloseness(source.familyScore, candidate.familyScore)
        score += scoreCloseness(source.kidsScore, candidate.kidsScore)
        score += scoreCloseness(source.trainingScore, candidate.trainingScore)
        score += scoreCloseness(source.apartmentScore, candidate.apartmentScore)
        return score.roundToInt().coerceIn(68, 96)
    }

    private fun closeness(a: Int?, b: Int?, range: Int, points: Float): Float {
        if (a == null || b == null) return 0f
        return (1f - abs(a - b).toFloat() / range).coerceIn(0f, 1f) * points
    }

    private fun scoreCloseness(a: Int?, b: Int?): Float {
        if (a == null || b == null) return 0f
        return (5 - abs(a - b)).coerceAtLeast(0) * 1.25f
    }

    private fun infoValue(icon: Int, value: String): View = LinearLayout(requireContext()).apply {
        gravity = Gravity.CENTER_VERTICAL
        addView(ImageView(requireContext()).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#168FF2"))
        }, LinearLayout.LayoutParams(dp(22), dp(22)))
        addView(text(value, 13, "#10245E", true).apply { setPadding(dp(6), 0, 0, 0) })
    }

    private fun loadDogImage(image: ImageView, dog: Dog) {
        when {
            dog.localImageRes != null -> image.setImageResource(dog.localImageRes)
            !dog.imageUrl.isNullOrBlank() -> Glide.with(this).load(dog.imageUrl).centerCrop().into(image)
        }
    }

    private fun openDetail(dogId: String) {
        (requireActivity() as MainActivity).openScreen(
            DogDetailFragment.newInstance(dogId),
            R.color.explore_background
        )
    }

    private fun text(value: String, size: Int, color: String, bold: Boolean) =
        TextView(requireContext()).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(Color.parseColor(color))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun rounded(color: String, radiusDp: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(color))
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun range(min: Int?, max: Int?, unit: String): String =
        if (min != null && max != null) "$min – $max$unit" else "Chưa cập nhật"

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val ARG_DOG_ID = "arg_dog_id"

        fun newInstance(dogId: String) = SimilarDogsFragment().apply {
            arguments = bundleOf(ARG_DOG_ID to dogId)
        }
    }
}
