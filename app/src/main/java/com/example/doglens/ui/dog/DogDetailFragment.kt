package com.example.doglens.ui.dog

import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnLayout
import com.example.doglens.MainActivity
import com.example.doglens.R
import com.example.doglens.data.model.*
import com.example.doglens.ui.favorite.whenSignedInForFavorite
import com.example.doglens.utils.addCatalogSources
import com.example.doglens.utils.catalogMedian
import com.example.doglens.utils.loadBreedPhoto
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DogDetailFragment : Fragment() {
    private val viewModel: DogDetailViewModel by viewModels()
    private var dogId = ""
    private var renderedContent: DogDetailUiState.Content? = null
    private var dog: Dog? = null
    private var favorite = false
    private var images: List<DogImage> = emptyList()
    private var imageIndex = 0
    private lateinit var hero: ImageView
    private lateinit var dots: LinearLayout
    private lateinit var favoriteTop: ImageButton
    private lateinit var favoriteBottom: TextView
    private lateinit var similarButton: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var footer: LinearLayout
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var loadStatus: TextView
    private lateinit var detailsStatus: TextView
    private val compactTextLayout: Boolean
        get() = resources.configuration.run { screenWidthDp < 350 || screenWidthDp / fontScale < 310 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dogId = requireArguments().getString(ARG_DOG_ID).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View = buildUi()

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        renderedContent = null
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val hasContent = state is DogDetailUiState.Content
                    scroll.visibility = if (hasContent) View.VISIBLE else View.GONE
                    footer.visibility = scroll.visibility
                    loadingIndicator.visibility = if (state == DogDetailUiState.Loading) View.VISIBLE else View.GONE
                    loadStatus.visibility = if (hasContent) View.GONE else View.VISIBLE
                    loadStatus.isClickable = state == DogDetailUiState.Error
                    when (state) {
                        DogDetailUiState.Loading -> loadStatus.text = "Đang tải thông tin giống chó…"
                        DogDetailUiState.NotFound -> loadStatus.text = "Không tìm thấy giống chó"
                        DogDetailUiState.Error -> loadStatus.text = "Không thể tải thông tin giống chó. Thử lại"
                        is DogDetailUiState.Content -> {
                            if (state.favoriteError) {
                                viewModel.clearFavoriteError()
                                Toast.makeText(requireContext(), R.string.favorite_update_error, Toast.LENGTH_SHORT)
                                    .show()
                            }
                            dog = state.dog
                            images = state.images
                            favorite = state.isFavorite
                            val previous = renderedContent
                            if (previous?.dog != state.dog) {
                                renderDog(state.dog)
                                renderScores(state.dog)
                            }
                            if (previous?.dog != state.dog || previous?.images != state.images) renderGallery(state.dog)
                            if (previous?.dogTraits != state.dogTraits || previous?.traits != state.traits) {
                                renderTraits(state.traits, state.dogTraits)
                            }
                            if (previous?.dogCareTips != state.dogCareTips || previous?.careTips != state.careTips) {
                                renderCare(state.careTips, state.dogCareTips)
                            }
                            renderedContent = state
                            detailsStatus.visibility = if (state.detailsLoading || state.detailsError) View.VISIBLE else View.GONE
                            detailsStatus.isClickable = !state.detailsLoading && state.detailsError
                            detailsStatus.text = if (state.detailsLoading) "Đang tải thêm thông tin…" else "Một số thông tin chưa tải được. Thử lại"
                            updateFavorite()
                            if (state.favoritePending) {
                                favoriteBottom.text = if (state.isFavorite) "Đã lưu · Chờ đồng bộ" else "Đã bỏ lưu · Chờ đồng bộ"
                            }
                        }
                    }
                }
            }
        }
        viewModel.load(dogId)
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
        toolbar.addView(
            txt("Chi tiết giống chó", 19, "#101D46", true).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, -2, 1f)
        )
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

        loadingIndicator = ProgressBar(requireContext())
        root.addView(loadingIndicator, LinearLayout.LayoutParams(dp(32), dp(32)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(36)
        })
        loadStatus = loadStatusView()
        root.addView(loadStatus)
        scroll = ScrollView(requireContext()).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            visibility = View.GONE
        }
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
        hero.doOnLayout { image ->
            image.layoutParams = image.layoutParams.apply {
                height = (image.width * 0.70f).toInt().coerceIn(dp(160), dp(340))
            }
        }
        dots = LinearLayout(requireContext()).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }
        body.addView(dots)
        body.addView(txt("Thông tin ảnh", 12, "#52627C", false).apply {
            tag = "photoCredit"
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            minHeight = dp(36)
            visibility = View.GONE
        })
        val title = LinearLayout(requireContext()).apply {
            gravity = Gravity.CENTER_VERTICAL
            tag = "title"
        }
        title.addView(txt("", 26, "#101D46", true), LinearLayout.LayoutParams(0, -2, 1f))
        title.addView(txt("Nổi bật", 13, "#F05B26", true).apply {
            background = round("#FFF0E8", 24)
            setPadding(dp(10), dp(6), dp(10), dp(6))
        })
        body.addView(title)
        val rating = LinearLayout(requireContext()).apply {
            tag = "rating"; gravity = Gravity.CENTER_VERTICAL
        }
        rating.addView(txt("★", 22, "#FFB800", true))
        rating.addView(txt("  —  |  Chưa có lượt yêu thích", 14, "#71809B", false))
        body.addView(rating)
        body.addView(txt("", 14, "#667895", false).apply {
            tag = "description"
            setPadding(dp(2), dp(8), dp(2), dp(4))
        })
        body.addView(section("Đặc điểm nổi bật").apply { tag = "traitsHeading" })
        val traitsScroll = HorizontalScrollView(requireContext()).apply {
            tag = "traitsScroll"
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val traitsBox = LinearLayout(requireContext()).apply {
            tag = "traits"
            orientation = LinearLayout.HORIZONTAL
        }
        traitsScroll.addView(traitsBox)
        body.addView(traitsScroll)
        body.addView(section("Thông tin cơ bản"))
        body.addView(LinearLayout(requireContext()).apply {
            tag = "info"; orientation = LinearLayout.VERTICAL
            background = round("#FFFFFF", 18); setPadding(dp(8), dp(4), dp(8), dp(4))
        })
        detailsStatus = loadStatusView()
        body.addView(detailsStatus)
        body.addView(section("Mức độ phù hợp").apply { tag = "scoresHeading" })
        body.addView(LinearLayout(requireContext()).apply {
            tag = "scores"; orientation = LinearLayout.VERTICAL
        })
        body.addView(section("Chăm sóc").apply { tag = "careHeading" })
        val careScroll = HorizontalScrollView(requireContext()).apply {
            tag = "careScroll"
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val careBox = LinearLayout(requireContext()).apply {
            tag = "care"
            orientation = LinearLayout.HORIZONTAL
        }
        careScroll.addView(careBox)
        body.addView(careScroll)
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        footer = LinearLayout(requireContext()).apply {
            visibility = View.GONE
            orientation = if (compactTextLayout) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER; setPadding(
            dp(14),
            dp(10),
            dp(14),
            dp(14)
        ); setBackgroundColor(Color.WHITE)
        }
        favoriteBottom = com.google.android.material.button.MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonStyle
        ).apply {
            text = "Lưu vào yêu thích"
            setTextAppearance(R.style.Widget_DogLens_Button_Primary)

            isSingleLine = false
            minHeight = dp(52)
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(10), dp(7), dp(10), dp(7))

            textSize = 13f

            setIconResource(R.drawable.ic_heart_white)
            iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
            iconPadding = dp(6)
            iconSize = dp(20)

            setOnClickListener { toggleFavorite() }
        }

        footer.addView(
            favoriteBottom,
            flexibleColumn().apply { if (!compactTextLayout) marginEnd = dp(8) }
        )
        similarButton = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = round("#EEF4FC", 30)
            minimumHeight = dp(52)
            setPadding(dp(10), dp(7), dp(10), dp(7))

            setOnClickListener {
                val currentDogId = dog?.id ?: return@setOnClickListener
                (requireActivity() as MainActivity).openScreen(
                    SimilarDogsFragment.newInstance(currentDogId),
                    R.color.explore_background
                )
            }
        }

        similarButton.addView(ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_detail_compare)
            setColorFilter(Color.parseColor("#1684F8"))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(dp(22), dp(22)))

        similarButton.addView(
            txt("Xem giống tương tự", 12, "#10234A", true).apply {
                gravity = Gravity.CENTER
                includeFontPadding = false
                setPadding(dp(5), 0, 0, 0)
            }
        )

        footer.addView(
            similarButton,
            flexibleColumn().apply { if (compactTextLayout) topMargin = dp(6) }
        )
        root.addView(footer)
        return root
    }

    private fun flexibleColumn(): LinearLayout.LayoutParams =
        if (compactTextLayout) LinearLayout.LayoutParams(-1, -2)
        else LinearLayout.LayoutParams(0, -2, 1f)

    private fun loadStatusView() = txt("", 14, "#1684F8", false).apply {
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(12), dp(16), dp(12))
        setOnClickListener { viewModel.load(dogId, retry = true) }
    }

    private fun renderDog(item: Dog) {
        (body.findViewWithTag<View>("title") as LinearLayout).let {
            (it.getChildAt(0) as TextView).text = item.name
            it.getChildAt(1).visibility = if (item.sortOrder in 1..6) View.VISIBLE else View.GONE
        }
        similarButton.visibility = if (item.hasComparableSize()) View.VISIBLE else View.GONE
        val rating = body.findViewWithTag<LinearLayout>("rating")
        rating.visibility = if (item.rating != null || item.favoriteCount != null) View.VISIBLE else View.GONE
        (rating.getChildAt(0) as TextView).text =
            "★  " + (item.rating?.let { String.format(Locale.US, "%.1f", it) } ?: "—")
        (rating.getChildAt(1) as TextView).text = "  |  " + (item.favoriteCount?.let {
            "Hơn " + NumberFormat.getInstance(Locale("vi", "VN")).format(it) + " người yêu thích"
        } ?: "")
        body.findViewWithTag<TextView>("description").text = (item.summary ?: item.description).withoutFciLabel()

        val credits = item.sources.filterKeys { it.startsWith("Ảnh") || it == "Giấy phép ảnh" }
        body.findViewWithTag<TextView>("photoCredit").apply {
            visibility = if (credits.isEmpty()) View.GONE else View.VISIBLE
            setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Thông tin ảnh")
                    .setView(ScrollView(requireContext()).apply {
                        addView(LinearLayout(requireContext()).apply {
                            setPadding(dp(20), 0, dp(20), 0)
                            addCatalogSources(credits, heading = null)
                        })
                    })
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
        val info = body.findViewWithTag<LinearLayout>("info")
        info.removeAllViews()
        val infoOrientation = if (compactTextLayout) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val first = LinearLayout(requireContext()).apply { orientation = infoOrientation }
        val second = LinearLayout(requireContext()).apply { orientation = infoOrientation }
        info.addView(first); info.addView(second)
        addInfo(
            first,
            R.drawable.ic_detail_scale,
            "Cân nặng",
            item.weightStandard ?: item.weightMedianKg?.let { catalogMedian(it, "kg") }
                ?: range(item.minWeightKg, item.maxWeightKg, "kg")
        )
        addInfo(
            first,
            R.drawable.ic_detail_ruler,
            "Chiều cao",
            item.heightStandard?.withoutFciLabel() ?: range(item.minHeightCm, item.maxHeightCm, "cm")
        )
        addInfo(
            second,
            R.drawable.ic_detail_clock,
            "Tuổi thọ",
            item.lifespanMedianYears?.let { catalogMedian(it, "năm") } ?: item.lifespanReference
                ?: range(item.minLifespanYears, item.maxLifespanYears, "năm")
        )
        addInfo(second, R.drawable.ic_detail_globe, "Nguồn gốc", item.origin?.withoutFciLabel() ?: "Chưa cập nhật")
    }

    private fun renderGallery(item: Dog) {
        if (images.isEmpty()) {
            hero.loadBreedPhoto(item)
            hero.setOnClickListener(null)
            dots.visibility = View.GONE
        } else {
            dots.visibility = View.VISIBLE
            showImage(0)
            hero.setOnClickListener { showImage((imageIndex + 1) % images.size) }
        }
    }

    private fun renderTraits(all: List<Trait>, links: List<DogTrait>) {
        val box = body.findViewWithTag<LinearLayout>("traits")
        box.removeAllViews()
        val visibility = if (links.isEmpty()) View.GONE else View.VISIBLE
        body.findViewWithTag<View>("traitsHeading").visibility = visibility
        body.findViewWithTag<View>("traitsScroll").visibility = visibility
        val colors = listOf("#EAF4FF", "#F4EEFF", "#FFF0F3", "#EAF9F1")
        links.mapNotNull { link ->
            all.firstOrNull { it.id == link.traitId }?.let { Pair(it, link) }
        }
            .forEachIndexed { i, pair ->
                val trait = pair.first;
                val link = pair.second
                val tile = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                    background = round(colors[i % colors.size], 18); setPadding(dp(6), dp(10), dp(6), dp(8))
                }
                val iconRes = when (trait.iconKey) {
                    "smile" -> R.drawable.ic_trait_smile
                    "brain" -> R.drawable.ic_trait_brain
                    "heart" -> R.drawable.ic_trait_heart_outline
                    "run" -> R.drawable.ic_trait_run
                    "leaf" -> R.drawable.ic_trait_leaf
                    else -> R.drawable.ic_trait_star
                }
                val iconBg = when (i) {
                    0 -> "#D7EBFF"
                    1 -> "#E8D9FF"
                    2 -> "#FFDCE5"
                    else -> "#D1F5E1"
                }

                tile.addView(ImageView(requireContext()).apply {
                    setImageResource(iconRes)
                    background = round(iconBg, 50)
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }, LinearLayout.LayoutParams(dp(48), dp(48)))
                tile.addView(txt(trait.name, 14, "#10234A", true).apply {
                    gravity = Gravity.CENTER; setPadding(0, dp(7), 0, dp(2))
                })
                tile.addView(txt(link.description.orEmpty().withoutFciLabel(), 13, "#71809B", false).apply {
                    gravity = Gravity.CENTER
                    setLineSpacing(dp(2).toFloat(), 1f)
                })
                box.addView(
                    tile,
                    LinearLayout.LayoutParams(dp(if (compactTextLayout) 144 else 116), -2).apply { marginEnd = dp(8) })
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
        ).filter { it.third != null }
        val visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
        body.findViewWithTag<View>("scoresHeading").visibility = visibility
        box.visibility = visibility
        if (data.isEmpty()) return
        val left = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val right = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        data.forEachIndexed { index, entry ->
            val row = LinearLayout(requireContext()).apply {
                gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(5), dp(8), dp(5))
            }
            row.addView(
                ImageView(requireContext()).apply { setImageResource(entry.second) },
                LinearLayout.LayoutParams(dp(30), dp(30))
            )
            val column =
                LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            column.addView(txt(entry.first, 12, "#10234A", false))
            column.addView(
                ProgressBar(
                    requireContext(),
                    null,
                    android.R.attr.progressBarStyleHorizontal
                ).apply {
                    max = 5; progress = entry.third ?: 0
                    progressTintList =
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#1684F8"))
                    progressBackgroundTintList =
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#E5ECF5"))
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
        val visibility = if (links.isEmpty()) View.GONE else View.VISIBLE
        body.findViewWithTag<View>("careHeading").visibility = visibility
        body.findViewWithTag<View>("careScroll").visibility = visibility
        links.mapNotNull { link ->
            all.firstOrNull { it.id == link.careTipId }?.let { Pair(it, link) }
        }
            .forEach { pair ->
                val tip = pair.first;
                val link = pair.second
                val tile = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL; background = round(tip.colorHex, 16)
                    setPadding(dp(9), dp(10), dp(8), dp(8))
                }
                val iconRes = when (tip.iconKey) {
                    "brush" -> R.drawable.ic_detail_brush
                    "run" -> R.drawable.ic_trait_run
                    "heart" -> R.drawable.ic_trait_heart_outline
                    "brain" -> R.drawable.ic_trait_brain
                    else -> R.drawable.ic_detail_flame
                }
                val cardColor = Color.parseColor(tip.colorHex)

                val hsv = FloatArray(3)
                Color.colorToHSV(cardColor, hsv)

                val backgroundHsv = hsv.copyOf()
                backgroundHsv[1] = (hsv[1] * 1.6f + 0.04f).coerceAtMost(0.35f)
                backgroundHsv[2] = (hsv[2] * 0.98f).coerceAtLeast(0f)

                val iconBackgroundColor = Color.HSVToColor(backgroundHsv)

                val iconBackground = String.format(
                    Locale.US,
                    "#%06X",
                    0xFFFFFF and iconBackgroundColor
                )

                val iconHsv = hsv.copyOf()
                iconHsv[1] = 0.85f
                iconHsv[2] = 0.72f

                val iconColor = Color.HSVToColor(iconHsv)

                tile.addView(ImageView(requireContext()).apply {
                    setImageResource(
                        if (tip.iconKey == "heart") {
                            R.drawable.ic_detail_communication
                        } else {
                            iconRes
                        }
                    )
                    background = round(iconBackground, 50)
                    setPadding(dp(9), dp(9), dp(9), dp(9))
                    setColorFilter(iconColor)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }, LinearLayout.LayoutParams(dp(42), dp(42)))

                tile.addView(txt(tip.name, 15, "#10234A", true).apply {
                    setPadding(0, dp(7), 0, dp(4))
                })

                tile.addView(txt(link.description.orEmpty(), 13, "#71809B", false).apply {
                    setLineSpacing(dp(2).toFloat(), 1f)
                })
                box.addView(tile, LinearLayout.LayoutParams(dp(if (compactTextLayout) 180 else 160), -2).apply {
                    marginEnd = dp(8)
                })
            }
    }

    private fun addInfo(parent: LinearLayout, icon: Int, title: String, value: String) {
        val cell = LinearLayout(requireContext()).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(
            dp(8), dp(12), dp(4), dp(12)
        )
        }
        cell.addView(ImageView(requireContext()).apply {
            setImageResource(icon); background = round("#E5F1FF", 50); setPadding(
            dp(9),
            dp(9),
            dp(9),
            dp(9)
        )
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        val labels = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(txt(title, 12, "#71809B", false))
        labels.addView(txt(value, 14, "#10234A", true))
        cell.addView(labels, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(8) })
        parent.addView(cell, flexibleColumn())
    }

    private fun showImage(index: Int) {
        if (images.isEmpty()) return
        imageIndex = index.coerceIn(images.indices)
        val image = images[imageIndex]
        val item = dog
        if (item != null && image.imageUrl == item.imageUrl && !image.imageUrl.isNullOrBlank()) {
            hero.loadBreedPhoto(item)
        } else {
            hero.loadBreedPhoto(image.imageUrl, image.localImageRes ?: item?.localImageRes)
        }
        dots.removeAllViews()
        images.forEachIndexed { i, _ ->
            dots.addView(
                View(requireContext()).apply {
                    background = round(if (i == imageIndex) "#1684F8" else "#D6DFEA", 20)
                },
                LinearLayout.LayoutParams(if (i == imageIndex) dp(22) else dp(8), dp(8))
                    .apply { marginEnd = dp(6) })
        }
    }

    private fun toggleFavorite() {
        if (viewModel.state.value is DogDetailUiState.Content) {
            whenSignedInForFavorite(viewModel::toggleFavorite)
        }
    }

    private fun updateFavorite() {
        favoriteTop.setImageResource(
            if (favorite) R.drawable.ic_heart_favorite
            else R.drawable.ic_heart_outline
        )
        val button = favoriteBottom as com.google.android.material.button.MaterialButton
        if (favorite) {
            button.text = "Đã lưu yêu thích"
            button.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#1684F8"))
            button.setTextColor(Color.WHITE)
            button.setIconResource(R.drawable.ic_heart_favorite)
            button.iconTint =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#FF3B5C"))
        } else {
            button.text = "Lưu vào yêu thích"
            button.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#1684F8"))
            button.setTextColor(Color.WHITE)
            button.setIconResource(R.drawable.ic_heart_white)
            button.iconTint = android.content.res.ColorStateList.valueOf(Color.WHITE)
        }
    }

    private fun shareDog() {
        val item = dog ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Tìm hiểu giống chó " + item.name + " trên DogLens!")
        }
        startActivity(Intent.createChooser(intent, "Chia sẻ giống chó"))
    }

    private fun String.withoutFciLabel(): String = replace(" (danh mục FCI)", "")
        .replace(" trong danh mục FCI", "")
        .replace("Xuất xứ theo nguồn tham khảo: ", "Xuất xứ: ")
        .replace(", mốc FCI", "")
        .replace("FCI mô tả giống ", "Giống ")
        .replace("FCI mô tả tính khí ", "Tính khí ")
        .replace("Tiêu chuẩn FCI nêu ", "Tiêu chuẩn giống nêu ")
        .replace(" được FCI mô tả", " được ghi nhận trong tiêu chuẩn giống")
        .replace("tiêu chuẩn FCI", "tiêu chuẩn giống")
        .replace("FCI", "tiêu chuẩn giống")

    private fun section(value: String) =
        txt(value, 19, "#101D46", true).apply { setPadding(0, dp(16), 0, dp(9)) }

    private fun txt(value: String, size: Int, color: String, bold: Boolean) =
        TextView(requireContext()).apply {
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
