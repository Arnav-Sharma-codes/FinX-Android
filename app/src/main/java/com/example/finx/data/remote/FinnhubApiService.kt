package com.example.finx.data.remote

import com.example.finx.data.model.NewsItem
import com.example.finx.data.model.QuoteResponse
import com.example.finx.data.model.RecommendationTrend
import com.example.finx.data.model.SymbolSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubApiService {
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): QuoteResponse

    @GET("news")
    suspend fun getGeneralNews(
        @Query("category") category: String = "general",
        @Query("token") apiKey: String
    ): List<NewsItem>

    @GET("stock/recommendation")
    suspend fun getRecommendationTrends(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): List<RecommendationTrend>

    @GET("search")
    suspend fun searchSymbols(
        @Query("q") query: String,
        @Query("token") apiKey: String
    ): SymbolSearchResponse
}
