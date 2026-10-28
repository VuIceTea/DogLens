
package com.example.doglens.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.R
import com.example.doglens.data.adapter.HomeFeatureAdapter
import com.example.doglens.data.adapter.PopularDogAdapter
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.HomeFeature

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupHomeFeatures(view)
        setupPopularDogs(view)
    }

    // Hiển thị các tính năng trên trang chủ
    private fun setupHomeFeatures(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(
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

        val adapter = HomeFeatureAdapter(features) { feature ->
            Toast.makeText(
                requireContext(),
                "Bạn chọn: ${feature.title}",
                Toast.LENGTH_SHORT
            ).show()
        }

        recyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false
    }

    // Hiển thị các giống chó phổ biến
    private fun setupPopularDogs(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(
            R.id.rvHomePopular
        )

        val popularDogs = listOf(
            Dog(
                id = "golden_retriever",
                name = "Golden Retriever",
                description = "Thân thiện, thông minh",
                localImageRes = R.drawable.golden_retriever,
                backgroundColorHex = "#E3E2E2",
                sortOrder = 1
            ),
            Dog(
                id = "corgi",
                name = "Corgi",
                description = "Năng động, đáng yêu",
                localImageRes = R.drawable.corgi,
                backgroundColorHex = "#E3E2E2",
                sortOrder = 2
            ),
            Dog(
                id = "husky",
                name = "Husky",
                description = "Mạnh mẽ, năng động",
                localImageRes = R.drawable.siberian_husky,
                backgroundColorHex = "#E3E2E2",
                sortOrder = 3
            ),
            Dog(
                id = "poodle",
                name = "Poodle",
                description = "Thông minh, dễ huấn luyện",
                localImageRes = R.drawable.poodle,
                backgroundColorHex = "#E3E2E2",
                sortOrder = 4
            ),
            Dog(
                id = "french_bulldog",
                name = "French Bulldog",
                description = "Điềm tĩnh, tình cảm",
                localImageRes = R.drawable.french_bulldog,
                backgroundColorHex = "#E3E2E2",
                sortOrder = 5
            )
        )

        val popularAdapter = PopularDogAdapter(
            popularDogs.sortedBy { it.sortOrder }
        )

        recyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        recyclerView.adapter = popularAdapter
        recyclerView.isNestedScrollingEnabled = false
    }

    private fun String.toColorInt(): Int {
        return android.graphics.Color.parseColor(this)
    }
}