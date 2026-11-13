package com.example.doglens.ui.home

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResult
import com.example.doglens.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

class PickImageBottomSheet : BottomSheetDialogFragment() {

    private var cameraUri: Uri? = null

    // Chụp ảnh bằng camera, ảnh ghi vào cameraUri
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = cameraUri
            if (success && uri != null) deliver(uri)
        }

    // Chọn ảnh từ thư viện (Photo Picker, không cần xin quyền)
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) deliver(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraUri = savedInstanceState?.getString(STATE_CAMERA_URI)?.let { Uri.parse(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        cameraUri?.let { outState.putString(STATE_CAMERA_URI, it.toString()) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pick_image_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Chừa chỗ cho thanh điều hướng hệ thống ở đáy
        val basePadding = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = basePadding + nav.bottom)
            insets
        }

        view.findViewById<View>(R.id.optionCamera).setOnClickListener { launchCamera() }

        view.findViewById<View>(R.id.optionGallery).setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        view.findViewById<View>(R.id.btnCancel).setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        // Nền container trong suốt để thấy góc bo của bg_pick_sheet
        (requireView().parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun launchCamera() {
        try {
            val dir = File(requireContext().cacheDir, "images").apply { mkdirs() }
            val file = File.createTempFile("dog_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
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

    // Trả ảnh về cho HomeFragment rồi đóng sheet
    private fun deliver(uri: Uri) {
        setFragmentResult(REQUEST_KEY, bundleOf(KEY_URI to uri.toString()))
        dismiss()
    }

    companion object {
        const val TAG = "PickImageBottomSheet"
        const val REQUEST_KEY = "pick_image_request"
        const val KEY_URI = "pick_image_uri"
        private const val STATE_CAMERA_URI = "state_camera_uri"
    }
}