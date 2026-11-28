package com.example.doglens.data.model

import kotlin.math.roundToInt

internal fun Dog.hasComparableSize(): Boolean =
    referenceWeight() != null || midpoint(minHeightCm, maxHeightCm) != null

internal fun Dog.sizeSimilarityTo(other: Dog): Int? {
    val matches = listOfNotNull(
        proportionalMatch(referenceWeight(), other.referenceWeight()),
        proportionalMatch(midpoint(minHeightCm, maxHeightCm), midpoint(other.minHeightCm, other.maxHeightCm))
    )
    return matches.takeIf { it.isNotEmpty() }?.average()?.times(100)?.roundToInt()
}

private fun Dog.referenceWeight() = weightMedianKg ?: midpoint(minWeightKg, maxWeightKg) ?: weightReferenceKg

private fun midpoint(min: Int?, max: Int?): Double? =
    if (min != null && max != null && min > 0 && max >= min) (min.toDouble() + max) / 2 else null

private fun proportionalMatch(a: Double?, b: Double?): Double? =
    if (a != null && b != null && a > 0 && b > 0) minOf(a, b) / maxOf(a, b) else null
