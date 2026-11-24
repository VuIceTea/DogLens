package com.example.doglens.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.doglens.MainActivity
import com.example.doglens.R
import com.example.doglens.ui.recognition.RecognitionFragment
import java.io.File

class CameraFragment : Fragment(R.layout.fragment_camera) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode = ImageCapture.FLASH_MODE_AUTO
    private var isTakingPhoto = false

    private lateinit var previewView: PreviewView
    private lateinit var btnFlash: ImageButton

    private val requestCameraPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Bạn cần cấp quyền camera để chụp ảnh",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val pickImage =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                openImagePreview(uri)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previewView = view.findViewById(R.id.previewView)
        btnFlash = view.findViewById(R.id.btnFlash)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btnTakePhoto).setOnClickListener {
            takePhoto()
        }

        view.findViewById<View>(R.id.btnSwitchCamera).setOnClickListener {
            switchCamera()
        }

        btnFlash.setOnClickListener {
            changeFlashMode()
        }

        view.findViewById<View>(R.id.btnGallery).setOnClickListener {
            openGallery()
        }

        view.findViewById<View>(R.id.btnPreviewGallery).setOnClickListener {
            openGallery()
        }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        val permission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        )

        if (permission == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val providerFuture =
            ProcessCameraProvider.getInstance(requireContext())

        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder()
                    .build()
                    .also { cameraPreview ->
                        cameraPreview.setSurfaceProvider(
                            previewView.surfaceProvider
                        )
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                    )
                    .setFlashMode(flashMode)
                    .build()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                provider.unbindAll()

                camera = provider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                updateFlashAvailability()
            } catch (exception: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Không thể mở camera: ${exception.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        if (isTakingPhoto) return

        val capture = imageCapture ?: return

        isTakingPhoto = true

        previewView.display?.rotation?.let { rotation ->
            capture.targetRotation = rotation
        }

        val imageDirectory = File(
            requireContext().cacheDir,
            "images"
        ).apply {
            mkdirs()
        }

        val imageFile = File.createTempFile(
            "captured_dog_",
            ".jpg",
            imageDirectory
        )

        val outputOptions =
            ImageCapture.OutputFileOptions
                .Builder(imageFile)
                .build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {
                    isTakingPhoto = false

                    val imageUri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        imageFile
                    )

                    openImagePreview(imageUri)
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {
                    isTakingPhoto = false

                    Toast.makeText(
                        requireContext(),
                        "Không thể chụp ảnh: ${exception.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun switchCamera() {
        val nextLensFacing =
            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }

        val selector = CameraSelector.Builder()
            .requireLensFacing(nextLensFacing)
            .build()

        val hasCamera = try {
            cameraProvider?.hasCamera(selector) == true
        } catch (_: Exception) {
            false
        }

        if (!hasCamera) {
            Toast.makeText(
                requireContext(),
                "Thiết bị không có camera này",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lensFacing = nextLensFacing
        startCamera()
    }

    private fun changeFlashMode() {
        if (camera?.cameraInfo?.hasFlashUnit() != true) {
            Toast.makeText(
                requireContext(),
                "Camera này không hỗ trợ đèn flash",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO ->
                ImageCapture.FLASH_MODE_ON

            ImageCapture.FLASH_MODE_ON ->
                ImageCapture.FLASH_MODE_OFF

            else ->
                ImageCapture.FLASH_MODE_AUTO
        }

        imageCapture?.flashMode = flashMode
        updateFlashButton()
    }

    private fun updateFlashAvailability() {
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true

        btnFlash.isEnabled = hasFlash
        btnFlash.alpha = if (hasFlash) 1f else 0.4f

        updateFlashButton()
    }

    private fun updateFlashButton() {
        btnFlash.contentDescription = when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> "Flash tự động"
            ImageCapture.FLASH_MODE_ON -> "Bật flash"
            else -> "Tắt flash"
        }

        btnFlash.alpha = when {
            camera?.cameraInfo?.hasFlashUnit() != true -> 0.4f
            flashMode == ImageCapture.FLASH_MODE_OFF -> 0.55f
            else -> 1f
        }
    }

    private fun openGallery() {
        pickImage.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    private fun openRecognition(uri: Uri) {
        cameraProvider?.unbindAll()

        (requireActivity() as MainActivity).replaceScreen(
            RecognitionFragment.newInstance(uri),
            R.color.recognition_background
        )
    }

    private fun openImagePreview(uri: Uri) {
        cameraProvider?.unbindAll()

        (requireActivity() as MainActivity).replaceScreen(
            ImagePreviewFragment.newInstance(uri),
            R.color.recognition_background
        )
    }

    override fun onDestroyView() {
        cameraProvider?.unbindAll()
        camera = null
        imageCapture = null
        cameraProvider = null

        super.onDestroyView()
    }
}
