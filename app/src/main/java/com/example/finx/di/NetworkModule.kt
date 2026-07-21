package com.example.finx.di

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.finx.data.memory.UserMemoryStore
import com.example.finx.data.orchestrator.AiOrchestrator
import com.example.finx.data.orchestrator.ContextEngine
import com.example.finx.data.orchestrator.DailyBriefEngine
import com.example.finx.data.orchestrator.NewsClusterer
import com.example.finx.data.remote.*
import com.example.finx.data.repository.AlphaVantageRepository
import com.example.finx.data.repository.MarketRepository
import com.example.finx.data.service.UnifiedAiService
import com.example.finx.data.service.UnifiedNewsService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // ─── Base URLs ────────────────────────────────────────────────────────────
    private const val FINNHUB_BASE_URL      = "https://finnhub.io/api/v1/"
    private const val ALPHA_VANTAGE_BASE_URL = "https://www.alphavantage.co/"
    private const val NEWS_API_BASE_URL      = "https://newsapi.org/v2/"
    private const val GEMINI_BASE_URL        = "https://generativelanguage.googleapis.com/"
    private const val GROQ_BASE_URL          = "https://api.groq.com/"
    private const val OPENROUTER_BASE_URL    = "https://openrouter.ai/"
    private const val GNEWS_BASE_URL         = "https://gnews.io/"
    private const val FMP_BASE_URL           = "https://financialmodelingprep.com/api/"
    private const val FIRECRAWL_BASE_URL     = "https://api.firecrawl.dev/"
    private const val EXA_BASE_URL           = "https://api.exa.ai/"
    private const val MASSIVE_BASE_URL       = "https://api.massive.com/"

    // ─── Application Context (set by FinXApp) ────────────────────────────────
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // ─── HTTP / Moshi ─────────────────────────────────────────────────────────
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    // ─── API Services ─────────────────────────────────────────────────────────
    val apiService:            FinnhubApiService     by lazy { retrofit(FINNHUB_BASE_URL).create(FinnhubApiService::class.java) }
    val newsApiService:        NewsApiService        by lazy { retrofit(NEWS_API_BASE_URL).create(NewsApiService::class.java) }
    val alphaVantageApiService: AlphaVantageApiService by lazy { retrofit(ALPHA_VANTAGE_BASE_URL).create(AlphaVantageApiService::class.java) }
    val geminiApiService:      GeminiApiService      by lazy { retrofit(GEMINI_BASE_URL).create(GeminiApiService::class.java) }
    val groqApiService:        GroqApiService        by lazy { retrofit(GROQ_BASE_URL).create(GroqApiService::class.java) }
    val openRouterApiService:  OpenRouterApiService  by lazy { retrofit(OPENROUTER_BASE_URL).create(OpenRouterApiService::class.java) }
    val gNewsApiService:       GNewsApiService       by lazy { retrofit(GNEWS_BASE_URL).create(GNewsApiService::class.java) }
    val fmpApiService:         FmpApiService         by lazy { retrofit(FMP_BASE_URL).create(FmpApiService::class.java) }
    val firecrawlApiService:   FirecrawlApiService   by lazy { retrofit(FIRECRAWL_BASE_URL).create(FirecrawlApiService::class.java) }
    val exaApiService:         ExaApiService         by lazy { retrofit(EXA_BASE_URL).create(ExaApiService::class.java) }
    val massiveApiService:     MassiveApiService     by lazy { retrofit(MASSIVE_BASE_URL).create(MassiveApiService::class.java) }

    // ─── Repositories ─────────────────────────────────────────────────────────
    val alphaVantageRepository: AlphaVantageRepository by lazy { AlphaVantageRepository(alphaVantageApiService) }

    // ─── Memory Layer ─────────────────────────────────────────────────────────
    val userMemoryStore: UserMemoryStore by lazy { UserMemoryStore(appContext) }

    // ─── News Layer ───────────────────────────────────────────────────────────
    val unifiedNewsService: UnifiedNewsService by lazy {
        UnifiedNewsService(newsApiService, gNewsApiService, fmpApiService, firecrawlApiService, exaApiService, massiveApiService)
    }

    // ─── Legacy AI Service (kept for backward compat in InsightsScreen) ───────
    val unifiedAiService: UnifiedAiService by lazy {
        UnifiedAiService(geminiApiService, groqApiService, openRouterApiService, apiService, alphaVantageRepository, unifiedNewsService)
    }

    // ─── Orchestrator Layer ───────────────────────────────────────────────────
    val contextEngine: ContextEngine by lazy {
        ContextEngine(apiService, alphaVantageRepository, unifiedNewsService, userMemoryStore)
    }

    val aiOrchestrator: AiOrchestrator by lazy {
        AiOrchestrator(contextEngine, geminiApiService, groqApiService, openRouterApiService, userMemoryStore)
    }

    val dailyBriefEngine: DailyBriefEngine by lazy {
        DailyBriefEngine(groqApiService, apiService, unifiedNewsService, userMemoryStore)
    }

    val dashboardIntelligenceEngine: com.example.finx.data.orchestrator.DashboardIntelligenceEngine by lazy {
        com.example.finx.data.orchestrator.DashboardIntelligenceEngine(groqApiService, marketRepository, userMemoryStore)
    }

    // NewsClusterer is a stateless object — accessed directly via NewsClusterer.cluster(...)

    // ─── Market Repository ────────────────────────────────────────────────────
    val marketRepository: MarketRepository by lazy {
        MarketRepository(apiService, unifiedNewsService, unifiedAiService, alphaVantageRepository, massiveApiService)
    }

    // ─── Watchlist (in-memory) ────────────────────────────────────────────────
    val watchlistSymbols = mutableStateListOf("AAPL", "GOOGL", "MSFT", "TSLA", "AMZN")

    // ─── Shared UI State (Persistence across tab switches) ────────────────────
    var dashboardInsights = mutableStateListOf<com.example.finx.data.model.DashboardInsight>()
    var isInsightsLoading by mutableStateOf(false)
}
