package com.example.doglens.ui.recognition

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.example.doglens.ui.camera.ImagePreviewFragment
import com.bumptech.glide.Glide
import com.example.doglens.MainActivity
import com.example.doglens.R
import com.example.doglens.utils.applyNavigationBarPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class RecognitionErrorFragment : Fragment(R.layout.fragment_recognition_error) {

    // 1 dòng nội dung tĩnh trên màn (chỉ UI)
    private class Row(
        @DrawableRes val icon: Int,
        val title: String,
        val subtitle: String,
        val detail: String? = null // có detail -> hiện mũi tên, bấm mở hộp thoại
    )

    private val reasons = listOf(
        Row(
            R.drawable.ic_error_image, "Ảnh bị mờ",
            "Ảnh chưa đủ rõ nét để nhận diện đặc điểm của chú chó.",
            "Giữ chắc điện thoại, chạm vào chú chó trên màn hình để lấy nét trước khi chụp. Tránh chụp lúc chó đang chạy nhảy."
        ),
        Row(
            R.drawable.ic_error_dog, "Không thấy rõ toàn thân hoặc khuôn mặt chó",
            "Chó có thể bị che khuất, cắt mất một phần cơ thể hoặc không nhìn thẳng vào camera.",
            "Chụp sao cho thấy rõ mặt, tai và thân chó. Tránh để người, đồ vật che mất chó."
        ),
        Row(
            R.drawable.ic_error_sun, "Ánh sáng chưa đủ",
            "Ảnh quá tối hoặc thiếu sáng có thể khiến AI khó nhận diện.",
            "Chụp ngoài trời hoặc gần cửa sổ. Tránh ngược sáng và đèn flash quá gắt."
        )
    )

    private val tips = listOf(
        Row(
            R.drawable.ic_error_frame,
            "Đưa chú chó vào khung",
            "Chụp rõ toàn thân hoặc khuôn mặt chú chó."
        ),
        Row(
            R.drawable.ic_error_sun,
            "Chụp ở nơi đủ sáng",
            "Nên chụp ngoài trời hoặc nơi có ánh sáng tốt."
        ),
        Row(
            R.drawable.ic_sheet_camera,
            "Tránh rung tay khi chụp",
            "Giữ điện thoại ổn định để ảnh rõ nét hơn."
        )
    )

    private var cameraUri: Uri? = null

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = cameraUri
            if (success && uri != null) retry(uri)
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) retry(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraUri = savedInstanceState?.getString(STATE_CAMERA_URI)?.let { Uri.parse(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        cameraUri?.let { outState.putString(STATE_CAMERA_URI, it.toString()) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.bottomActions).applyNavigationBarPadding()

        val imageUri = Uri.parse(requireArguments().getString(ARG_URI))
        Glide.with(this).load(imageUri).centerCrop().into(view.findViewById(R.id.imgPhoto))

        view.findViewById<View>(R.id.btnBack)
            .setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.btnGoHome)
            .setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.btnHelp).setOnClickListener { showHelpDialog() }

        addRows(view.findViewById(R.id.reasonsContainer), reasons)
        addRows(view.findViewById(R.id.tipsContainer), tips)

        view.findViewById<View>(R.id.btnRetake).setOnClickListener { launchCamera() }
        view.findViewById<View>(R.id.btnPickOther).setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun addRows(container: LinearLayout, rows: List<Row>) {
        val inflater = LayoutInflater.from(requireContext())
        rows.forEach { row ->
            val item = inflater.inflate(R.layout.item_error_row, container, false)
            item.findViewById<ImageView>(R.id.rowIcon).setImageResource(row.icon)
            item.findViewById<TextView>(R.id.rowTitle).text = row.title
            item.findViewById<TextView>(R.id.rowSubtitle).text = row.subtitle

            val chevron = item.findViewById<View>(R.id.rowChevron)
            val detail = row.detail
            if (detail != null) {
                chevron.visibility = View.VISIBLE
                item.setOnClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(row.title)
                        .setMessage(detail)
                        .setPositiveButton("Đã hiểu", null)
                        .show()
                }
            } else {
                chevron.visibility = View.GONE
                item.isClickable = false
                item.foreground = null
            }
            container.addView(item)
        }
    }

    private fun showHelpDialog() {
        val message = tips.joinToString("\n\n") { "• ${it.title}: ${it.subtitle}" } +
                "\n\n• Mỗi ảnh chỉ nên có 1 chú chó."
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cách chụp để AI nhận diện tốt")
            .setMessage(message)
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun launchCamera() {
        try {
            val dir = File(requireContext().cacheDir, "images").apply { mkdirs() }
            val file = File.createTempFile("dog_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.fileprovider", file
            )
            cameraUri = uri
            takePicture.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Không mở được camera: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun retry(uri: Uri) {
        (requireActivity() as MainActivity).replaceScreen(
            ImagePreviewFragment.newInstance(uri), R.color.recognition_background
        )
    }

    companion object {
        private const val ARG_URI = "arg_uri"
        private const val STATE_CAMERA_URI = "state_camera_uri"

        fun newInstance(uri: Uri) = RecognitionErrorFragment().apply {
            arguments = bundleOf(ARG_URI to uri.toString())
        }
    }
}
