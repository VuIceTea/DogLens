package com.example.doglens.data.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.example.doglens.R
import com.example.doglens.data.model.HomeFeature

//HomeFeatureAdapter có nhiệm vụ đưa dữ liệu vào từng thẻ
class HomeFeatureAdapter(
    // Thêm danh sách dữ liệu vào Adapter
    private  val features: List<HomeFeature>, // Adapter nhận một danh sách các đối tượng HomeFeature
    private val onItemClick: (HomeFeature) -> Unit // onItemClick là một hàm mà HomeFragment truyền vào để Adapter gọi khi người dùng nhấn một item
) : RecyclerView.Adapter<HomeFeatureAdapter.ViewHolder>() { // HomeFeatureAdapter.ViewHolder: Là class vừa tự tạo bên trong Adapter. ViewHolder là class nằm bên trong HomeFeatureAdapter

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) { // RecyclerView.ViewHolder(view): Truyền giao diện đó cho class cha
        val image: ImageView = view.findViewById(R.id.imgFeature);
        val title: TextView = view.findViewById(R.id.txtFeatureTitle);
        val description: TextView = view.findViewById(R.id.txtFeatureDesc);
    }


    // Tạo giao diện của 1 item từ item_home_features và đưa giao diện vừa tạo vào ViewHolder
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context) // LayoutInflater dùng để biến bản thiết kế xml thành các đối tượng View thực tế
            .inflate(R.layout.item_home_feature, parent, false) // inflate tạo View từ bản thiết kế
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val feature = features[position] // Lấy một tính năng tại vị trí position

        holder.image.setImageResource(feature.image) // Đưa hình ảnh của tính năng lên ImageView
        holder.title.text = feature.title // Đưa tên lên TextView
        holder.description.text = feature.description // Đưa mô tả lên TextView

        // Đổi màu nền theo từng item
        val background = GradientDrawable().apply {
            setColor(feature.backgroundColor)
            cornerRadius = 16f * holder.itemView.resources.displayMetrics.density
        }
        holder.itemView.background = background //holder.itemView là toàn bộ giao diện của một item

        holder.itemView.setOnClickListener { // holder.itemView là toàn bộ giao diện của một thẻ
            onItemClick(feature)
        }
    }

    // Có bao nhiêu phần tử trong danh sách thì hàm này trả về bấy nhiêu
    override fun getItemCount(): Int {
        return features.size
    }
}