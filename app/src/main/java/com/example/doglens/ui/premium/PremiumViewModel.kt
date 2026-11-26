package com.example.doglens.ui.premium

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

enum class PremiumPlan(val amount: Int, val days: Int) {
    MONTH(49_000, 30),
    YEAR(399_000, 365)
}

data class PremiumCheckoutState(
    val plan: PremiumPlan = PremiumPlan.YEAR,
    val loading: Boolean = false,
    val status: String = "none",
    val premiumUntil: Long = 0,
    val resumeUrl: Uri? = null,
    val openUrl: Uri? = null,
    val error: String? = null
)

class PremiumViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(
        PremiumCheckoutState(plan = saved.get<PremiumPlan>("plan") ?: PremiumPlan.YEAR)
    )
    val state = mutableState.asStateFlow()
    private val auth = FirebaseAuth.getInstance()

    fun select(plan: PremiumPlan) {
        if (state.value.loading || state.value.status == "pending" || state.value.plan == plan) return
        saved["plan"] = plan
        saved.remove<String>("requestId")
        mutableState.value = state.value.copy(plan = plan, error = null)
    }

    fun checkout() {
        val current = state.value
        if (current.loading || current.status == "review") return
        current.resumeUrl?.let {
            mutableState.value = current.copy(openUrl = it, error = null)
            return
        }
        runRequest {
            val requestId = saved.get<String>("requestId") ?: UUID.randomUUID().toString()
            saved["requestId"] = requestId
            val result = api("/checkout", JSONObject()
                .put("plan", current.plan.name.lowercase())
                .put("expectedAmount", current.plan.amount)
                .put("requestId", requestId))
            val checkoutUrl = paymentUri(result.getString("paymentUrl"))
            saved["orderId"] = result.getString("orderId")
            mutableState.value = state.value.copy(status = "pending", resumeUrl = checkoutUrl, openUrl = checkoutUrl)
        }
    }

    fun refresh() = runRequest {
        val orderId = saved.get<String>("orderId")
        val result = try {
            api(if (orderId == null) "/status" else "/status?orderId=$orderId")
        } catch (error: PaymentError) {
            if (orderId == null || error.code != "404") throw error
            saved.remove<String>("orderId")
            api("/status")
        }
        val status = result.getString("status")
        val pendingPlan = if (status == "pending") {
            PremiumPlan.entries.firstOrNull { it.name.equals(result.optString("plan"), ignoreCase = true) }
        } else null
        if (pendingPlan != null) saved["plan"] = pendingPlan
        saved["orderId"] = result.optString("orderId").takeIf { !result.isNull("orderId") }
        if (status == "paid" || status == "failed" || status == "expired" || status == "review") {
            saved.remove<String>("requestId")
        }
        mutableState.value = state.value.copy(
            status = status,
            plan = pendingPlan ?: state.value.plan,
            premiumUntil = result.optLong("premiumUntil"),
            resumeUrl = if (result.isNull("paymentUrl")) null else paymentUri(result.getString("paymentUrl"))
        )
    }

    fun consumeOpenUrl() {
        mutableState.value = state.value.copy(openUrl = null)
    }

    private fun runRequest(action: suspend () -> Unit) {
        if (state.value.loading) return
        val uid = auth.currentUser?.uid
        if (uid == null) {
            mutableState.value = PremiumCheckoutState(plan = state.value.plan, error = "Vui lòng đăng nhập để thanh toán.")
            return
        }
        if (saved.get<String>("uid") != uid) {
            saved["uid"] = uid
            saved.remove<String>("orderId")
            saved.remove<String>("requestId")
        }
        mutableState.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val errorCode = (error as? PaymentError)?.code
                if (errorCode == "ORDER_PENDING" || errorCode == "ORDER_CLOSED") {
                    saved.remove<String>("orderId")
                    saved.remove<String>("requestId")
                }
                val message = when (errorCode) {
                    "PRICE_CHANGED" -> "Giá đã thay đổi. Vui lòng cập nhật phiên bản DogLens."
                    "PROFILE_REQUIRED" -> "Hồ sơ chưa được đồng bộ. Hãy đăng nhập lại."
                    "401" -> "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại."
                    "ORDER_PENDING" -> "Bạn có giao dịch đang chờ. Bấm Kiểm tra giao dịch để tiếp tục."
                    "ORDER_CLOSED" -> "Đơn đã được xử lý hoặc hết hạn. Hãy kiểm tra giao dịch."
                    else -> "Chưa kết nối được dịch vụ thanh toán Sandbox. Vui lòng thử lại."
                }
                mutableState.value = state.value.copy(error = message)
            } finally {
                mutableState.value = state.value.copy(loading = false)
            }
        }
    }

    private suspend fun api(path: String, payload: JSONObject? = null): JSONObject {
        val user = auth.currentUser ?: throw PaymentError("401")
        val token = user.getIdToken(false).await().token ?: throw PaymentError("401")
        val project = checkNotNull(FirebaseApp.getInstance().options.projectId)
        return withContext(Dispatchers.IO) {
            val connection = URL("https://asia-southeast1-$project.cloudfunctions.net/premiumPayments$path")
                .openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 10_000
                connection.readTimeout = 12_000
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/json")
                if (payload != null) {
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                }
                val code = connection.responseCode
                if (code !in 200..299) {
                    val body = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    val error = runCatching { JSONObject(body.orEmpty()).optString("error") }.getOrNull()
                    throw PaymentError(error?.takeIf(String::isNotBlank) ?: code.toString())
                }
                JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun paymentUri(url: String): Uri = Uri.parse(url).also {
        require(it.scheme == "https" && it.authority == "sandbox.vnpayment.vn" &&
            it.path == "/paymentv2/vpcpay.html")
    }

    private class PaymentError(val code: String) : Exception()
}
