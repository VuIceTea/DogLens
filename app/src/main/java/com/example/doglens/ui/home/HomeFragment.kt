package com.example.doglens.ui.home

import com.example.doglens.utils.bindPullRefresh

import android.net.Uri
import android.os.Bundle
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.MainActivity
import com.example.doglens.R
import com.example.doglens.data.adapter.PopularDogAdapter
import com.example.doglens.data.model.HomeFeature
import com.example.doglens.ui.camera.ImagePreviewFragment
import com.example.doglens.ui.dog.DogDetailFragment
import com.example.doglens.ui.camera.CameraFragment
import com.example.doglens.ui.article.openArticleCatalog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val viewModel: HomeViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupHomeFeatures(view)
        setupPopularDogs(view)
        setupPickImage(view)
    }

    private fun setupPickImage(view: View) {
        view.findViewById<View>(R.id.btnChooseImage).setOnClickListener {
            if (parentFragmentManager.findFragmentByTag(PickImageBottomSheet.TAG) == null) {
                PickImageBottomSheet().show(parentFragmentManager, PickImageBottomSheet.TAG)
            }
        }

        setFragmentResultListener(PickImageBottomSheet.REQUEST_KEY) { _, bundle ->

            if (bundle.getBoolean(PickImageBottomSheet.KEY_OPEN_CAMERA, false)) {
                (requireActivity() as MainActivity).openScreen(
                    CameraFragment(),
                    R.color.recognition_background
                )
                return@setFragmentResultListener
            }

            val urlString =
                bundle.getString(PickImageBottomSheet.KEY_URI)
                    ?: return@setFragmentResultListener

            onImagePicked(Uri.parse(urlString))
        }
    }

    private fun onImagePicked(uri: Uri) {
        (requireActivity() as MainActivity).openScreen(
            ImagePreviewFragment.newInstance(uri), R.color.recognition_background
        )
    }

    // Hiển thị các tính năng trên trang chủ
    private fun setupHomeFeatures(view: View) {
        val featureGrid = view.findViewById<LinearLayout>(
            R.id.rvHomeFeatures
        )

        val features = listOf(
            HomeFeature(
                "identify",
                "Nhận diện giống chó",
                "Nhanh & chính xác",
                R.drawable.identify_dog,
                "#E2F1FF".toColorInt()
            ),
            HomeFeature(
                "information",
                "Tìm hiểu thông tin",
                "Đặc điểm, tính cách",
                R.drawable.learn_about_dogs,
                "#F1E5FF".toColorInt()
            ),
            HomeFeature(
                "favorite",
                "Lưu giống chó yêu thích",
                "Bộ sưu tập riêng",
                R.drawable.favorite_dogs,
                "#E3FAED".toColorInt()
            ),
            HomeFeature(
                "community",
                "Khám phá cộng đồng",
                "Chia sẻ ảnh thú cưng",
                R.drawable.dog_community,
                "#FFF1DD".toColorInt()
            )
        )

        val density = resources.displayMetrics.density
        val cardWidth = (90 * resources.configuration.fontScale.coerceIn(1f, 1.65f) * density).toInt()
        features.forEach { feature ->
            val card = layoutInflater.inflate(R.layout.item_home_feature, featureGrid, false)
            card.findViewById<ImageView>(R.id.imgFeature).setImageResource(feature.image)
            card.findViewById<TextView>(R.id.txtFeatureTitle).text = feature.title
            card.findViewById<TextView>(R.id.txtFeatureDesc).text = feature.description
            card.background = GradientDrawable().apply {
                setColor(feature.backgroundColor)
                cornerRadius = 16f * density
            }
            card.setOnClickListener {
                when (feature.id) {
                    "identify" -> (requireActivity() as MainActivity).openScreen(
                        CameraFragment(), R.color.recognition_background
                    )
                    "information" -> openArticleCatalog()
                    "favorite" -> (requireActivity() as MainActivity).selectFavoriteTab()
                    else -> Toast.makeText(requireContext(), "Bạn chọn: ${feature.title}", Toast.LENGTH_SHORT).show()
                }
            }
            val params = card.layoutParams as LinearLayout.LayoutParams
            params.width = cardWidth
            params.weight = 1f
            featureGrid.addView(card, params)
        }
    }

    // Hiển thị các giống chó phổ biến
    private fun setupPopularDogs(view: View) {
        bindPullRefresh(view.findViewById(R.id.pullRefresh)) { viewModel.refreshPopularDogs().join() }
        val recyclerView = view.findViewById<RecyclerView>(
            R.id.rvHomePopular
        )

        recyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        recyclerView.isNestedScrollingEnabled = false

        val skeleton = PopularDogSkeletonAdapter()
        val loadError = Snackbar.make(view, R.string.popular_dogs_load_error, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.popular_dogs_retry) { viewModel.retryPopularDogs() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    viewModel.popularDogs.collect { state ->
                        when (state) {
                            PopularDogsState.Loading -> {
                                loadError.dismiss()
                                recyclerView.adapter = skeleton
                            }
                            PopularDogsState.Error -> {
                                recyclerView.adapter = null
                                loadError.show()
                            }
                            is PopularDogsState.Content -> {
                                loadError.dismiss()
                                recyclerView.adapter = PopularDogAdapter(state.dogs) { dog ->
                                    (requireActivity() as MainActivity)
                                        .openScreen(DogDetailFragment.newInstance(dog.id))
                                }
                            }
                        }
                    }
                } finally {
                    loadError.dismiss()
                }
            }
        }
    }

    private fun String.toColorInt(): Int {
        return android.graphics.Color.parseColor(this)
    }
}
