package com.example.doglens.ui.camera

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.doglens.MainActivity
import com.example.doglens.R
import com.example.doglens.ui.recognition.RecognitionFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class ImagePreviewFragment : Fragment(R.layout.fragment_image_preview) {

    private enum class CropMode(val ratio: Float?) {
        FREE(null),
        SQUARE(1f),
        LANDSCAPE(4f / 3f),
        PORTRAIT(3f / 4f)
    }

    private lateinit var previewImage: ImageView
    private lateinit var cropViewport: View
    private lateinit var continueButton: View

    private var cropMode = CropMode.FREE
    private var rotationDegrees = 0
    private var isProcessing = false

    private val imageUri: Uri?
        get() = arguments
            ?.getString(ARG_IMAGE_URI)
            ?.let(Uri::parse)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uri = imageUri
        if (uri == null) {
            Toast.makeText(requireContext(), "Không tìm thấy ảnh", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        previewImage = view.findViewById(R.id.imgPreview)
        cropViewport = view.findViewById(R.id.cropViewport)
        continueButton = view.findViewById(R.id.btnContinue)

        Glide.with(this)
            .load(uri)
            .into(previewImage)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        bindCropButton(view, R.id.toolFree, CropMode.FREE)
        bindCropButton(view, R.id.toolSquare, CropMode.SQUARE)
        bindCropButton(view, R.id.toolLandscape, CropMode.LANDSCAPE)
        bindCropButton(view, R.id.toolPortrait, CropMode.PORTRAIT)

        view.findViewById<View>(R.id.toolRotate).setOnClickListener {
            rotationDegrees = (rotationDegrees + 90) % 360
            previewImage.animate()
                .rotation(rotationDegrees.toFloat())
                .setDuration(220)
                .start()
        }

        continueButton.setOnClickListener {
            exportAdjustedImage(uri)
        }

        updateSelectedTool(view)
    }

    private fun bindCropButton(view: View, id: Int, mode: CropMode) {
        view.findViewById<View>(id).setOnClickListener {
            cropMode = mode
            updateViewportRatio()
            updateSelectedTool(view)
        }
    }

    private fun updateViewportRatio() {
        val params = cropViewport.layoutParams as ConstraintLayout.LayoutParams
        params.dimensionRatio = when (cropMode) {
            CropMode.FREE -> "3:4"
            CropMode.SQUARE -> "1:1"
            CropMode.LANDSCAPE -> "4:3"
            CropMode.PORTRAIT -> "3:4"
        }
        cropViewport.layoutParams = params
        previewImage.scaleType = if (cropMode == CropMode.FREE) {
            ImageView.ScaleType.FIT_CENTER
        } else {
            ImageView.ScaleType.CENTER_CROP
        }
    }

    private fun updateSelectedTool(root: View) {
        val tools = listOf(
            Triple(R.id.toolFree, R.id.iconFree, R.id.labelFree),
            Triple(R.id.toolSquare, R.id.iconSquare, R.id.labelSquare),
            Triple(R.id.toolLandscape, R.id.iconLandscape, R.id.labelLandscape),
            Triple(R.id.toolPortrait, R.id.iconPortrait, R.id.labelPortrait)
        )

        val selectedId = when (cropMode) {
            CropMode.FREE -> R.id.toolFree
            CropMode.SQUARE -> R.id.toolSquare
            CropMode.LANDSCAPE -> R.id.toolLandscape
            CropMode.PORTRAIT -> R.id.toolPortrait
        }

        tools.forEach { (toolId, iconId, labelId) ->
            val selected = toolId == selectedId
            root.findViewById<View>(toolId).setBackgroundResource(
                if (selected) R.drawable.bg_preview_tool_selected
                else R.drawable.bg_preview_tool_default
            )
            root.findViewById<ImageView>(iconId).imageTintList =
                ColorStateList.valueOf(Color.parseColor(if (selected) "#FFFFFF" else "#10245E"))
            root.findViewById<TextView>(labelId).setTextColor(
                Color.parseColor(if (selected) "#168FF2" else "#7885A6")
            )
        }
    }

    private fun exportAdjustedImage(sourceUri: Uri) {
        if (isProcessing) return
        if (cropMode == CropMode.FREE && rotationDegrees == 0) {
            (requireActivity() as MainActivity).replaceScreen(
                RecognitionFragment.newInstance(sourceUri),
                R.color.recognition_background
            )
            return
        }
        isProcessing = true
        continueButton.isEnabled = false
        continueButton.alpha = 0.65f

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val exportedUri = withContext(Dispatchers.IO) {
                    val source = decodeSampledBitmap(sourceUri)
                    val rotated = rotateBitmap(source, rotationDegrees)
                    val cropped = centerCrop(rotated, cropMode.ratio)

                    val directory = File(requireContext().cacheDir, "images").apply { mkdirs() }
                    val output = File.createTempFile("adjusted_dog_", ".jpg", directory)
                    FileOutputStream(output).use { stream ->
                        cropped.compress(Bitmap.CompressFormat.JPEG, 94, stream)
                    }

                    if (cropped !== rotated) cropped.recycle()
                    if (rotated !== source) rotated.recycle()
                    source.recycle()

                    Uri.fromFile(output)
                }

                (requireActivity() as MainActivity).replaceScreen(
                    RecognitionFragment.newInstance(exportedUri),
                    R.color.recognition_background
                )
            } catch (exception: Exception) {
                isProcessing = false
                continueButton.isEnabled = true
                continueButton.alpha = 1f
                Toast.makeText(
                    requireContext(),
                    "Không thể xử lý ảnh: ${exception.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun decodeSampledBitmap(uri: Uri): Bitmap {
        val resolver = requireContext().contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val largest = max(info.size.width, info.size.height)
                if (largest > 2048) decoder.setTargetSampleSize((largest / 2048f).toInt() + 1)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: error("Không đọc được ảnh")
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun centerCrop(bitmap: Bitmap, ratio: Float?): Bitmap {
        if (ratio == null) return bitmap

        val currentRatio = bitmap.width.toFloat() / bitmap.height
        val cropWidth: Int
        val cropHeight: Int

        if (currentRatio > ratio) {
            cropHeight = bitmap.height
            cropWidth = (cropHeight * ratio).toInt()
        } else {
            cropWidth = bitmap.width
            cropHeight = (cropWidth / ratio).toInt()
        }

        val x = (bitmap.width - cropWidth) / 2
        val y = (bitmap.height - cropHeight) / 2
        return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)
    }

    companion object {
        private const val ARG_IMAGE_URI = "arg_image_uri"

        fun newInstance(uri: Uri): ImagePreviewFragment {
            return ImagePreviewFragment().apply {
                arguments = bundleOf(
                    ARG_IMAGE_URI to uri.toString()
                )
            }
        }
    }
}
