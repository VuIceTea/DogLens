package com.example.doglens.ui.dog

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.doglens.R
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DogDetailFragment : Fragment() {
    private val repo get() = AppContainer.dogRepository
    private val favoriteRepo get() = AppContainer.favoriteRepository
    private var dogId = ""
    private var dog: Dog? = null
    private var favorite = false
    private var images: List<DogImage> = emptyList()
    private var imageIndex = 0
    private lateinit var hero: ImageView
    private lateinit var dots: LinearLayout
    private lateinit var favoriteTop: ImageButton
    private lateinit var favoriteBottom: TextView
    private lateinit var body: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dogId = requireArguments().getString(ARG_DOG_ID).orEmpty()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View = buildUi()

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        viewLifecycleOwner.lifecycleScope.launch {
            val selected = repo.getDogById(dogId)
            if (selected == null) {
                Toast.makeText(requireContext(), "Không tìm thấy giống chó", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                return@launch
            }
            dog = selected
            images = repo.getDogImages(dogId)
            favorite = favoriteRepo.isFavorite(dogId)
            renderDog(selected)
            renderTraits(repo.getTraits(), repo.getDogTraits(dogId))
            renderCare(repo.getCareTips(), repo.getDogCareTips(dogId))
            renderScores(selected)
            updateFavorite()
        }
    }

    private fun buildUi(): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248, 250, 253))
        }
        val toolbar = LinearLayout(requireContext()).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(12), dp(8))
        }
        toolbar.addView(ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_arrow_back)
            background = round("#FFFFFF", 50)
            contentDescription = "Quay lại"
            setOnClickListener { parentFragmentManager.popBackStack() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        toolbar.addView(txt("Chi tiết giống chó", 19, "#101D46", true).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, dp(44), 1f))
        favoriteTop = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_heart_outline)
            background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            contentDescription = "Yêu thích"
            setOnClickListener { toggleFavorite() }
        }
        toolbar.addView(favoriteTop, LinearLayout.LayoutParams(dp(42), dp(44)))
        toolbar.addView(ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_detail_share)
            background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            contentDescription = "Chia sẻ"
            setOnClickListener { shareDog() }
        }, LinearLayout.LayoutParams(dp(42), dp(44)))
        root.addView(toolbar)

        val scroll = ScrollView(requireContext()).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        body = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(4), dp(18), dp(18))
        }
        hero = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = round("#EAF1F8", 22)
            clipToOutline = true
        }
        body.addView(hero, LinearLayout.LayoutParams(-1, dp(255)))
        dots = LinearLayout(requireContext()).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }
        body.addView(dots)
        val title = LinearLayout(requireContext()).apply {
            gravity = Gravity.CENTER_VERTICAL
            tag = "title"
        }
        title.addView(txt("", 26, "#101D46", true), LinearLayout.LayoutParams(0, -2, 1f))
        title.addView(txt("🔥 Phổ biến", 13, "#F05B26", true).apply {
            background = round("#FFF0E8", 24)
            setPadding(dp(10), dp(6), dp(10), dp(6))
        })
        body.addView(title)
        val rating = LinearLayout(requireContext()).apply { tag = "rating"; gravity = Gravity.CENTER_VERTICAL }
        rating.addView(txt("★", 22, "#FFB800", true))
        rating.addView(txt("  —  |  Chưa có lượt yêu thích", 14, "#71809B", false))
        body.addView(rating)
        body.addView(txt("", 14, "#667895", false).apply {
            tag = "description"
            setPadding(dp(2), dp(8), dp(2), dp(4))
        })
        body.addView(section("Đặc điểm nổi bật"))
        body.addView(LinearLayout(requireContext()).apply { tag = "traits"; orientation = LinearLayout.HORIZONTAL })
        body.addView(section("Thông tin cơ bản"))
        body.addView(LinearLayout(requireContext()).apply {
            tag = "info"; orientation = LinearLayout.VERTICAL
            background = round("#FFFFFF", 18); setPadding(dp(8), dp(4), dp(8), dp(4))
        })
        body.addView(section("Mức độ phù hợp"))
        body.addView(LinearLayout(requireContext()).apply { tag = "scores"; orientation = LinearLayout.VERTICAL })
        body.addView(section("Chăm sóc"))
        body.addView(LinearLayout(requireContext()).apply { tag = "care"; orientation = LinearLayout.HORIZONTAL })
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val footer = LinearLayout(requireContext()).apply {
            gravity = Gravity.CENTER; setPadding(dp(14), dp(10), dp(14), dp(14)); setBackgroundColor(Color.WHITE)
        }
        favoriteBottom = txt("♥  Lưu vào yêu thích", 15, "#FFFFFF", true).apply {
            gravity = Gravity.CENTER; background = round("#1684F8", 30); setOnClickListener { toggleFavorite() }
        }
        footer.addView(favoriteBottom, LinearLayout.LayoutParams(0, dp(56), 1f).apply { marginEnd = dp(8) })
        footer.addView(txt("▢  Xem giống tương tự", 14, "#10234A", true).apply {
            gravity = Gravity.CENTER; background = round("#EEF4FC", 30)
            setOnClickListener { Toast.makeText(requireContext(), "Sẽ bổ sung danh sách giống tương tự", Toast.LENGTH_SHORT).show() }
        }, LinearLayout.LayoutParams(0, dp(56), 1f).apply { marginStart = dp(4) })
        root.addView(footer)
        return root
    }

    private fun renderDog(item: Dog) {
        (body.findViewWithTag<View>("title") as LinearLayout).let {
            (it.getChildAt(0) as TextView).text = item.name
        }
        val rating = body.findViewWithTag<LinearLayout>("rating")
        (rating.getChildAt(0) as TextView).text = "★  " + (item.rating?.let { String.format(Locale.US, "%.1f", it) } ?: "—")
        (rating.getChildAt(1) as TextView).text = "  |  " + (item.favoriteCount?.let {
            "Hơn " + NumberFormat.getInstance(Locale("vi", "VN")).format(it) + " người yêu thích"
        } ?: "Chưa có lượt yêu thích")
        body.findViewWithTag<TextView>("description").text = item.summary ?: item.description

        if (images.isNotEmpty()) {
            showImage(0)
            hero.setOnClickListener { showImage((imageIndex + 1) % images.size) }
        } else {
            item.localImageRes?.let { hero.setImageResource(it) }
            if (!item.imageUrl.isNullOrBlank()) Glide.with(this).load(item.imageUrl).centerCrop().into(hero)
            dots.visibility = View.GONE
        }
        val info = body.findViewWithTag<LinearLayout>("info")
        val first = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val second = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        info.addView(first); info.addView(second)
        addInfo(first, R.drawable.ic_detail_scale, "Cân nặng", range(item.minWeightKg, item.maxWeightKg, "kg"))
        addInfo(first, R.drawable.ic_detail_ruler, "Chiều cao", range(item.minHeightCm, item.maxHeightCm, "cm"))
        addInfo(second, R.drawable.ic_detail_clock, "Tuổi thọ", range(item.minLifespanYears, item.maxLifespanYears, "năm"))
        addInfo(second, R.drawable.ic_detail_globe, "Nguồn gốc", item.origin ?: "Chưa cập nhật")
    }

    private fun renderTraits(all: List<Trait>, links: List<DogTrait>) {
        val box = body.findViewWithTag<LinearLayout>("traits")
        box.removeAllViews()
        val colors = listOf("#EAF4FF", "#F4EEFF", "#FFF0F3", "#EAF9F1")
        val ink = listOf("#1684F8", "#8653E8", "#F04468", "#139A55")
        links.mapNotNull { link -> all.firstOrNull { it.id == link.traitId }?.let { Pair(it, link) } }
            .take(4).forEachIndexed { i, pair ->
                val trait = pair.first; val link = pair.second
                val tile = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                    background = round(colors[i], 18); setPadding(dp(6), dp(10), dp(6), dp(8))
                }
                val symbol = when (trait.iconKey) {
                    "brain" -> "🧠"; "heart" -> "♥"; "run" -> "🐕"; "leaf" -> "🍃"; "star" -> "★"; else -> "🐾"
                }
                tile.addView(txt(symbol, 23, ink[i], true).apply {
                    gravity = Gravity.CENTER; background = round("#FFFFFF", 50)
                    setPadding(dp(10), dp(7), dp(10), dp(7))
                })
                tile.addView(txt(trait.name, 12, "#10234A", true).apply {
                    gravity = Gravity.CENTER; setPadding(0, dp(7), 0, dp(2))
                })
                tile.addView(txt(link.description.orEmpty(), 10, "#71809B", false).apply { gravity = Gravity.CENTER })
                box.addView(tile, LinearLayout.LayoutParams(0, dp(132), 1f).apply { marginEnd = dp(7) })
            }
    }

    private fun renderScores(item: Dog) {
        val box = body.findViewWithTag<LinearLayout>("scores")
        box.removeAllViews()
        val data = listOf(
            Triple("Gia đình", R.drawable.ic_detail_family, item.familyScore),
            Triple("Trẻ em", R.drawable.ic_detail_child, item.kidsScore),
            Triple("Huấn luyện", R.drawable.ic_detail_school, item.trainingScore),
            Triple("Căn hộ", R.drawable.ic_detail_apartment, item.apartmentScore)
        )
        val left = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val right = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        data.forEachIndexed { index, entry ->
            val row = LinearLayout(requireContext()).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(5), dp(8), dp(5)) }
            row.addView(ImageView(requireContext()).apply { setImageResource(entry.second) },
                LinearLayout.LayoutParams(dp(30), dp(30)))
            val column = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            column.addView(txt(entry.first, 12, "#10234A", false))
            column.addView(ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 5; progress = entry.third ?: 0
                progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1684F8"))
                progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E5ECF5"))
            }, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(4) })
            row.addView(column, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(8) })
            row.addView(txt((entry.third ?: 0).toString() + "/5", 11, "#71809B", false))
            (if (index % 2 == 0) left else right).addView(row)
        }
        box.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(left, LinearLayout.LayoutParams(0, -2, 1f))
            addView(right, LinearLayout.LayoutParams(0, -2, 1f))
        })
    }

    private fun renderCare(all: List<CareTip>, links: List<DogCareTip>) {
        val box = body.findViewWithTag<LinearLayout>("care")
        box.removeAllViews()
        links.mapNotNull { link -> all.firstOrNull { it.id == link.careTipId }?.let { Pair(it, link) } }
            .take(3).forEach { pair ->
                val tip = pair.first; val link = pair.second
                val tile = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL; background = round(tip.colorHex, 16)
                    setPadding(dp(9), dp(10), dp(8), dp(8))
                }
                val symbol = when (tip.iconKey) {
                    "brush" -> "✦"; "run" -> "🐕"; "heart" -> "♥"; "brain" -> "🧠"; "sun" -> "☀"; else -> "✧"
                }
                tile.addView(txt(symbol, 21, "#1684F8", true))
                tile.addView(txt(tip.name, 11, "#10234A", true).apply { setPadding(0, dp(5), 0, dp(3)) })
                tile.addView(txt(link.description.orEmpty(), 9, "#71809B", false))
                box.addView(tile, LinearLayout.LayoutParams(0, dp(126), 1f).apply { marginEnd = dp(7) })
            }
    }

    private fun addInfo(parent: LinearLayout, icon: Int, title: String, value: String) {
        val cell = LinearLayout(requireContext()).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(12), dp(4), dp(12)) }
        cell.addView(ImageView(requireContext()).apply {
            setImageResource(icon); background = round("#E5F1FF", 50); setPadding(dp(9))
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        val labels = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(txt(title, 12, "#71809B", false))
        labels.addView(txt(value, 14, "#10234A", true))
        cell.addView(labels, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(8) })
        parent.addView(cell, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun showImage(index: Int) {
        if (images.isEmpty()) return
        imageIndex = index.coerceIn(images.indices)
        val image = images[imageIndex]
        if (!image.imageUrl.isNullOrBlank()) Glide.with(this).load(image.imageUrl).centerCrop().into(hero)
        else image.localImageRes?.let { hero.setImageResource(it) }
        dots.removeAllViews()
        images.forEachIndexed { i, _ ->
            dots.addView(View(requireContext()).apply {
                background = round(if (i == imageIndex) "#1684F8" else "#D6DFEA", 20)
            }, LinearLayout.LayoutParams(if (i == imageIndex) dp(22) else dp(8), dp(8)).apply { marginEnd = dp(6) })
        }
    }

    private fun toggleFavorite() {
        val item = dog ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            if (favorite) favoriteRepo.removeFavorite(item.id) else favoriteRepo.addFavorite(item.id)
            favorite = !favorite
            updateFavorite()
        }
    }

    private fun updateFavorite() {
        favoriteTop.setImageResource(if (favorite) R.drawable.ic_heart_favorite else R.drawable.ic_heart_outline)
        favoriteBottom.text = if (favorite) "♥  Đã lưu yêu thích" else "♥  Lưu vào yêu thích"
    }

    private fun shareDog() {
        val item = dog ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Tìm hiểu giống chó " + item.name + " trên DogLens!")
        }
        startActivity(Intent.createChooser(intent, "Chia sẻ giống chó"))
    }

    private fun section(value: String) = txt(value, 19, "#101D46", true).apply { setPadding(0, dp(16), 0, dp(9)) }
    private fun txt(value: String, size: Int, color: String, bold: Boolean) = TextView(requireContext()).apply {
        text = value; textSize = size.toFloat(); setTextColor(Color.parseColor(color))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun round(color: String, radius: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(color)); cornerRadius = dp(radius).toFloat()
    }
    private fun range(min: Int?, max: Int?, unit: String) =
        if (min != null && max != null) "$min – $max$unit" else "Chưa cập nhật"
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ARG_DOG_ID = "dog_id"
        fun newInstance(id: String) = DogDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_DOG_ID, id) }
        }
    }
}