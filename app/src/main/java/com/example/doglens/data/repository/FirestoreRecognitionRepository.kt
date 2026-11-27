package com.example.doglens.data.repository

import android.content.Context
import android.net.Uri
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.RecognitionResult
import com.example.doglens.data.local.LocalDogRecognition
import com.example.doglens.data.local.decodeDogPhoto
import com.example.doglens.data.local.encodeRecognitionJpeg
import com.example.doglens.data.remote.DogRecognitionApi
import com.example.doglens.utils.TimeUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

internal class FirestoreRecognitionRepository(
    private val dogs: DogRepository,
    private val account: FirestoreAccountData = FirestoreAccountData(),
    private val api: DogRecognitionApi = DogRecognitionApi(),
    private val context: Context = FirebaseApp.getInstance().applicationContext,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : RecognitionRepository {
    private val localRecognition by lazy { LocalDogRecognition(context) }

    override suspend fun recognize(imageUri: Uri, onProgress: (Int) -> Unit): RecognitionResult {
        val user = auth.currentUser ?: error("Vui lòng đăng nhập để nhận diện và lưu kết quả.")
        val history = account.requireCollection("recognitions")
        val record = history.document()
        onProgress(10)
        val photo = withContext(Dispatchers.IO) { decodeDogPhoto(context, imageUri) }
        val prediction: Pair<String, Int>
        val bytes: ByteArray
        try {
            onProgress(38)
            if (api.isConfigured) {
                bytes = withContext(Dispatchers.IO) { photo.encodeRecognitionJpeg() }
                val token = user.getIdToken(false).await().token ?: error("Phiên đăng nhập đã hết hạn.")
                prediction = api.recognize(bytes, "image/jpeg", token)
            } else {
                prediction = withContext(Dispatchers.Default) {
                    localRecognition.recognize(photo, onProgress)
                }
                bytes = withContext(Dispatchers.IO) { photo.encodeRecognitionJpeg() }
            }
        } finally {
            photo.recycle()
        }
        val (dogId, confidence) = prediction
        val dog = dogs.getDogById(dogId) ?: error("Giống chó này chưa có trong danh mục.")
        check(auth.currentUser?.uid == user.uid) { "Phiên đăng nhập đã thay đổi." }
        onProgress(80)
        val image = photoFile(user.uid, record.id)
        withContext(Dispatchers.IO) {
            image.parentFile?.mkdirs()
            image.writeBytes(bytes)
        }

        val notification = checkNotNull(history.parent).collection("notifications").document(record.id)
        account.db.batch()
            .set(record, mapOf("dogId" to dogId, "confidence" to confidence, "createdAt" to FieldValue.serverTimestamp()))
            .set(notification, mapOf(
                "type" to "RECOGNITION",
                "title" to "Đã nhận diện thành công",
                "message" to "${dog.name} · $confidence% độ tin cậy",
                "createdAt" to FieldValue.serverTimestamp(),
                "isRead" to false
            )).commit().await()
        onProgress(100)
        return RecognitionResult(record.id, user.uid, dogId, confidence, Uri.fromFile(image).toString(), TimeUtils.nowIso())
    }

    override suspend fun getHistory(): List<RecognitionResult> {
        val history = account.collection("recognitions") ?: return emptyList()
        val documents = history.orderBy("createdAt", Query.Direction.DESCENDING).get().await().documents
        if (documents.isEmpty()) return emptyList()
        val byId = dogs.getDogs().associateBy { it.id }
        return documents.map { it.toResult(byId[it.string("dogId")]) }
    }

    override suspend fun getById(id: String): RecognitionResult? {
        if (id.isBlank()) return null
        val history = account.collection("recognitions") ?: return null
        val record = history.document(id).get().await()
        if (!record.exists()) return null
        return record.toResult(dogs.getDogById(record.string("dogId")))
    }

    override suspend fun delete(id: String) {
        val history = account.requireCollection("recognitions")
        history.document(id).delete().await()
        withContext(Dispatchers.IO) { photoFile(checkNotNull(history.parent).id, id).delete() }
    }

    override suspend fun clearHistory() {
        val history = account.requireCollection("recognitions")
        val uid = checkNotNull(history.parent).id
        while (true) {
            val documents = history.limit(400).get(com.google.firebase.firestore.Source.SERVER).await().documents
            if (documents.isEmpty()) break
            val batch = account.db.batch()
            documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            withContext(Dispatchers.IO) { documents.forEach { photoFile(uid, it.id).delete() } }
        }
    }

    private fun DocumentSnapshot.toResult(dog: Dog?): RecognitionResult {
        val uid = checkNotNull(reference.parent.parent).id
        val photo = photoFile(uid, id)
        val image = when {
            photo.isFile -> Uri.fromFile(photo).toString()
            !dog?.imageUrl.isNullOrBlank() -> dog?.imageUrl.orEmpty()
            dog?.localImageRes != null -> "android.resource://${context.packageName}/${dog.localImageRes}"
            else -> ""
        }
        return RecognitionResult(id, uid, string("dogId"), int("confidence") ?: 0, image,
            TimeUtils.toIso(getTimestamp("createdAt")?.toDate()?.time ?: 0L))
    }

    private fun photoFile(uid: String, id: String): File {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,100}"))) { "Mã ảnh không hợp lệ." }
        return File(context.noBackupFilesDir, "recognitions/$uid/$id")
    }

}
