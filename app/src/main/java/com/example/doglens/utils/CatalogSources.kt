package com.example.doglens.utils

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.widget.LinearLayout
import android.widget.TextView
import java.text.NumberFormat
import java.util.Locale

internal fun LinearLayout.addCatalogSources(sources: Map<String, String>, heading: String? = "Nguồn tham khảo") {
    if (sources.isEmpty()) return
    val links = SpannableStringBuilder(heading.orEmpty()).apply {
        if (!heading.isNullOrEmpty()) {
            setSpan(StyleSpan(Typeface.BOLD), 0, heading.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        sources.forEach { (label, url) ->
            if (isNotEmpty()) append('\n')
            val start = length
            append(label)
            setSpan(URLSpan(url), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    addView(TextView(context).apply {
        text = links
        textSize = 14f
        setTextColor(Color.rgb(16, 36, 94))
        val density = resources.displayMetrics.density
        val padding = (12 * density).toInt()
        setPadding(0, padding, 0, padding)
        setLineSpacing(6 * density, 1f)
        movementMethod = LinkMovementMethod.getInstance()
    })
}

internal fun catalogMedian(value: Double, unit: String): String =
    "${NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value)} $unit (trung vị)"
