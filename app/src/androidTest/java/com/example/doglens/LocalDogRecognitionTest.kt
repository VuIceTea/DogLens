package com.example.doglens

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.doglens.data.local.LocalDogRecognition
import com.example.doglens.data.local.decodeDogPhoto
import com.example.doglens.data.repository.RecognitionFailedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.InputStream

class LocalDogRecognitionTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val recognition = LocalDogRecognition(context)

    @Test
    fun recognizesGoldenRetrieverFromImageUri() {
        context.resources.openRawResource(R.drawable.img_golden_retriever).use { photo ->
            assertBreed(photo, ".png", "golden_retriever")
        }
    }

    @Test
    fun recognizesLabradorFromOutdoorPhoto() {
        val fixtures = InstrumentationRegistry.getInstrumentation().context.assets
        fixtures.open("labrador_on_beach.jpg").use { photo ->
            assertBreed(photo, ".jpg", "labrador_retriever", fromContentProvider = true)
        }
    }

    private fun assertBreed(
        input: InputStream,
        suffix: String,
        expectedDogId: String,
        fromContentProvider: Boolean = false
    ) {
        val images = File(context.cacheDir, "images").apply { mkdirs() }
        val image = File.createTempFile("doglens-test-", suffix, images)
        try {
            image.outputStream().use { output -> input.copyTo(output) }
            val uri = if (fromContentProvider) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", image)
            } else {
                Uri.fromFile(image)
            }
            val photo = decodeDogPhoto(context, uri)
            try {
                val (dogId, confidence) = recognition.recognize(photo) {}
                assertEquals(expectedDogId, dogId)
                assertTrue(confidence in 65..99)
            } finally {
                photo.recycle()
            }
        } finally {
            image.delete()
        }
    }

    @Test
    fun rejectsImageWithNoDog() {
        val photo = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        photo.eraseColor(Color.GRAY)
        try {
            assertThrows(RecognitionFailedException::class.java) { recognition.recognize(photo) {} }
        } finally {
            photo.recycle()
        }
    }
}
