package com.example.finx.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuoteResponse(
    @Json(name = "c") val currentPrice: Double,
    @Json(name = "d") val change: Double,
    @Json(name = "dp") val percentChange: Double,
    @Json(name = "h") val highPrice: Double,
    @Json(name = "l") val lowPrice: Double,
    @Json(name = "o") val openPrice: Double,
    @Json(name = "pc") val previousClose: Double,
    @Json(name = "t") val timestamp: Long
)
