package com.example.doglens.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.R
import com.example.doglens.data.adapter.HomeFeatureAdapter
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.HomeFeature
import androidx.core.graphics.toColorInt
import com.example.doglens.data.adapter.PopularDogAdapter

// HomeFragment là nơi chuẩn bị dữ liệu và hiển thị danh sách các thẻ đó
class HomeFragment : Fragment(R.layout.fragment_home) {

    //onViewCreated được gọi khi View của Fragment đã được tạo xong
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { // view: View là giao diện của fragment_home.xml đã được tạo, savedInstanceState chứa trạng thái đã lưu trước đó (nếu có)
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvHomeFeatures)

        val features = listOf<HomeFeature>(
            HomeFeature(
                "identify",
                "Nhận diện giống chó",
                "Nhanh & chính xác",
                R.drawable.identify_dog,
                Color.parseColor("#E2F1FF")
            ),
            HomeFeature(
                "information",
                "Tìm hiểu thông tin",
                "Đặc điểm, tính cách",
                R.drawable.learn_about_dogs,
                Color.parseColor("#F1E5FF")
            ),
            HomeFeature(
                "favorite",
                "Lưu giống chó yêu thích",
                "Bộ sưu tập riêng",
                R.drawable.favorite_dogs,
                Color.parseColor("#E3FAED")
            ),
            HomeFeature(
                "community",
                "Khám phá cộng đồng",
                "Chia sẻ ảnh thú cưng",
                R.drawable.dog_community,
                Color.parseColor("#FFF1DD")
            )
        )

        // Tạo Adapter
        val adapter = HomeFeatureAdapter(features) { feature ->
            Toast.makeText(requireContext(), "Bạn chọn: ${feature.title}", Toast.LENGTH_SHORT).show()
        }

        // Thiết lập hướng hiển thị RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Gắn Adapter vào RecyclerView để hiển thị dữ liệu
        recyclerView.adapter = adapter

        // Tìm id RecyclerView Popular Dog
        val recyclerViewPopularDogs = view.findViewById<RecyclerView>(R.id.rvHomePopular);

        // Khởi tạo dữ liệu popular dog
        val popularDogs = listOf<Dog>(
            Dog(
                "golden_retriever",
                "Golden Retriever",
                R.drawable.golden_retriever,
                "",
                Color.parseColor("#e3e2e2")
            ),
            Dog(
                "corgi",
                "Corgi",
                R.drawable.corgi,
                "",
                Color.parseColor("#e3e2e2")
            ),
            Dog(
                "huskt",
                "Husky",
                R.drawable.siberian_husky,
                "",
                Color.parseColor("#e3e2e2")
            ),
            Dog(
                "poodle",
                "Poodle",
                R.drawable.poodle,
                "",
                Color.parseColor("#e3e2e2")
            ),
            Dog(
                "french_bulldog",
                "French Bulldog",
                R.drawable.french_bulldog,
                "",
                Color.parseColor("#e3e2e2")
            ),
        )

        val popularAdapter = PopularDogAdapter(popularDogs)

        recyclerViewPopularDogs.layoutManager = LinearLayoutManager(requireContext(),
            LinearLayoutManager.HORIZONTAL, false)

        recyclerViewPopularDogs.adapter = popularAdapter
    }
}