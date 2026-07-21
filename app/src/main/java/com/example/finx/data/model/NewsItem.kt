package com.example.finx.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NewsItem(
    @Json(name = "category") val category: String,
    @Json(name = "datetime") val datetime: Long,
    @Json(name = "headline") val headline: String,
    @Json(name = "id") val id: Long,
    @Json(name = "image") val image: String,
    @Json(name = "related") val related: String,
    @Json(name = "source") val source: String,
    @Json(name = "summary") val summary: String,
    @Json(name = "url") val url: String
)
