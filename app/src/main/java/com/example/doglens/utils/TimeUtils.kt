package com.example.doglens.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {
    private const val ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    private fun isoFormat() = SimpleDateFormat(ISO_PATTERN, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Thời gian hiện tại dạng ISO-8601 (UTC), ví dụ 2026-11-15T10:20:30Z
    fun nowIso(): String = toIso(System.currentTimeMillis())

    fun toIso(millis: Long): String = isoFormat().format(Date(millis))

    // ISO -> millis. Sai định dạng trả về 0
    fun parseIso(iso: String): Long = try {
        isoFormat().parse(iso)?.time ?: 0L
    } catch (e: ParseException) {
        0L
    }

    // Ngày: "Hôm nay" / "Hôm qua" / "07/10/2026"
    fun dayLabel(dayStart: Long): String {
        val today = startOfDay(System.currentTimeMillis())
        val yesterday = startOfDay(today - 1)
        return when (dayStart) {
            today -> "Hôm nay"
            yesterday -> "Hôm qua"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dayStart))
        }
    }

    // 00:00:00 của ngày chứa millis (theo giờ máy)
    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Thời gian: Hôm nay -> "14:23", ngày khác -> "08/10/2026 16:30"
    fun timeLabel(millis: Long): String {
        val isToday = startOfDay(millis) == startOfDay(System.currentTimeMillis())
        val pattern = if (isToday) "HH:mm" else "dd/MM/yyyy HH:mm"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }
}