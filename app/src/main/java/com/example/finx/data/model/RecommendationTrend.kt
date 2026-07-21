package com.example.finx.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecommendationTrend(
    @Json(name = "buy") val buy: Int,
    @Json(name = "hold") val hold: Int,
    @Json(name = "period") val period: String,
    @Json(name = "sell") val sell: Int,
    @Json(name = "strongBuy") val strongBuy: Int,
    @Json(name = "strongSell") val strongSell: Int,
    @Json(name = "symbol") val symbol: String
)
