package com.example.finx.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finx.data.model.NewsItem
import com.example.finx.data.model.RecommendationTrend
import com.example.finx.data.model.SymbolSearchResult
import com.example.finx.data.orchestrator.OrchestratedAiResult
import com.example.finx.di.NetworkModule
import com.example.finx.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InsightsScreen() {
    val repository = NetworkModule.marketRepository
    val coroutineScope = rememberCoroutineScope()

    var trendSymbol by remember { mutableStateOf("AAPL") }
    var trendsList by remember { mutableStateOf<List<RecommendationTrend>>(emptyList()) }
    var isLoadingTrends by remember { mutableStateOf(false) }

    var newsList by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    var isLoadingNews by remember { mutableStateOf(false) }

    val fetchTrendsFor: (String) -> Unit = { rawSymbol ->
        if (rawSymbol.isNotBlank()) {
            isLoadingTrends = true
            coroutineScope.launch {
                val resolvedSymbol = repository.resolveBestSymbol(rawSymbol)
                trendSymbol = resolvedSymbol
                repository.getRecommendationTrends(resolvedSymbol).fold(
                    onSuccess = { trendsList = it },
                    onFailure = { trendsList = emptyList() }
                )
                isLoadingTrends = false
            }
        }
    }

    val fetchTrends = {
        fetchTrendsFor(trendSymbol)
    }

    val fetchNews = {
        isLoadingNews = true
        coroutineScope.launch {
            repository.getGeneralNews().fold(
                onSuccess = { newsList = it },
                onFailure = { newsList = emptyList() }
            )
            isLoadingNews = false
        }
    }

    LaunchedEffect(Unit) {
        fetchTrends()
        fetchNews()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Market Intelligence",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            RecommendationSection(
                trendSymbol = trendSymbol,
                onSymbolChange = { trendSymbol = it },
                onSearch = { fetchTrends() },
                onSymbolSelected = { result ->
                    trendSymbol = result.symbol
                    fetchTrendsFor(result.symbol)
                },
                isLoading = isLoadingTrends,
                trendsList = trendsList
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Global Intelligence",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(
                    onClick = { fetchNews() }, 
                    enabled = !isLoadingNews,
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryIndigo)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Live", fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoadingNews) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp, color = PrimaryIndigo)
                }
            }
        } else if (newsList.isEmpty()) {
            item {
                EmptyNewsState(onRefresh = { fetchNews() })
            }
        } else {
            // News is already ranked by aggregator (Recency + Quality)
            itemsIndexed(newsList, key = { _, it -> it.id }) { index, news ->
                StaggeredFadeInItem(index = index) {
                    PremiumNewsCardModern(news = news)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun StaggeredFadeInItem(index: Int, content: @Composable () -> Unit) {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 100L)
        visible.value = true
    }
    AnimatedVisibility(
        visible = visible.value,
        enter = slideInVertically(initialOffsetY = { 40 }) + fadeIn(animationSpec = tween(500)),
        content = { content() }
    )
}

@Composable
fun EmptyNewsState(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No live news loaded",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap Live to refresh market intelligence.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRefresh,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Refresh News")
            }
        }
    }
}

@Composable
fun RecommendationSection(
    trendSymbol: String,
    onSymbolChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSymbolSelected: (SymbolSearchResult) -> Unit,
    isLoading: Boolean,
    trendsList: List<RecommendationTrend>
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = GlassWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Sentiment Analysis", 
                style = MaterialTheme.typography.labelMedium, 
                color = PrimaryIndigo, 
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top) {
                StockLookupField(
                    value = trendSymbol,
                    onValueChange = onSymbolChange,
                    onSymbolSelected = onSymbolSelected,
                    label = "Enter company or symbol",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onSearch,
                    modifier = Modifier.clip(CircleShape).background(GlassWhite)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search Symbol", tint = TextPrimary)
                }
            }
            
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = PrimaryIndigo,
                    trackColor = GlassWhite
                )
            } else if (trendsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                RecommendationTrendChartModern(trend = trendsList.first())
            }
        }
    }
}

@Composable
fun PremiumNewsCardModern(news: NewsItem) {
    val aiOrchestrator = NetworkModule.aiOrchestrator
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(news.datetime * 1000))

    var isExpanded by remember { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf<OrchestratedAiResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    val cardInteraction = remember { MutableInteractionSource() }
    val themeTitle = remember(news.headline) { inferNewsTheme(news.headline) }

    LaunchedEffect(news.id) {
        if (aiResult == null && !isAnalyzing) {
            isAnalyzing = true
            aiResult = aiOrchestrator.analyzeNews(news.headline, news.summary)
            isAnalyzing = false
        }
    }

    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .smoothPress(0.985f, cardInteraction)
            .clickable(
                interactionSource = cardInteraction,
                indication = null
            ) { isExpanded = !isExpanded }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        news.source.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryIndigo,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "MARKET THEME",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryIndigo
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                themeTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                lineHeight = 25.sp,
                color = TextPrimary
            )

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    aiResult?.executiveSummary?.takeIf { it.isNotBlank() } ?: news.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 3,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                ModernAiVerdictPreview(result = aiResult, isLoading = isAnalyzing)
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(180)),
                exit = shrinkVertically(animationSpec = tween(170)) + fadeOut(animationSpec = tween(120))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        news.headline,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        aiResult?.executiveSummary?.takeIf { it.isNotBlank() } ?: news.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    if (isAnalyzing) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = PrimaryIndigo)
                        }
                    } else {
                        aiResult?.let { result ->
                            ModernAiDeepIntelligencePanel(result = result)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.url))
                                context.startActivity(intent)
                            } catch (e: Exception) { /* ignore */ }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlassWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Text("Read Full Article", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernAiVerdictPreview(result: OrchestratedAiResult?, isLoading: Boolean) {
    Surface(
        color = PrimaryIndigo.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI VERDICT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo)
                Spacer(modifier = Modifier.weight(1f))
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryIndigo)
                } else if (result != null) {
                    val confidenceColor = when {
                        result.confidenceScore >= 85 -> Color(0xFF4ADE80) // Vibrant Green
                        result.confidenceScore >= 70 -> PrimaryIndigo
                        else -> TextSecondary
                    }
                    Surface(
                        color = confidenceColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "${result.confidenceScore}% Signal",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = confidenceColor
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (result == null) {
                Text("Synthesizing institutional intelligence...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(result.outlookEmoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(result.outlook, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    result.suggestedActions.firstOrNull()
                        ?: result.executiveSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ModernAiDeepIntelligencePanel(result: OrchestratedAiResult) {
    Surface(
        color = GlassWhite,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("AI INTELLIGENCE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                Text("Confidence: ${result.confidenceScore}%", style = MaterialTheme.typography.labelSmall, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            ModernIntelligenceRow(label = "Outlook", value = result.outlook, emoji = result.outlookEmoji)
            ModernIntelligenceRow(label = "Why It Matters", value = result.executiveSummary)
            ModernIntelligenceRow(label = "Market Impact", value = result.technicalSignals.ifBlank { result.investmentOutlook })
            ModernIntelligenceRow(label = "Investor Action", value = result.suggestedActions.firstOrNull().orEmpty())
            
            if (result.importantNews.isNotEmpty()) {
                ModernIntelligenceChips(label = "Important News", items = result.importantNews)
            }
            
            ModernIntelligenceRow(label = "Opportunity", value = result.opportunities.firstOrNull().orEmpty())
            ModernIntelligenceRow(label = "Risks", value = result.risks.firstOrNull() ?: result.riskLevel)
            ModernIntelligenceRow(label = "Generated From", value = "${result.modelsLabel} plus live news context")
        }
    }
}

fun inferNewsTheme(headline: String): String {
    val upper = headline.uppercase(Locale.getDefault())
    val company = listOf("NVIDIA", "MICROSOFT", "APPLE", "TESLA", "GOOGLE", "AMAZON", "META", "BITCOIN", "OIL", "FED", "CPI")
        .firstOrNull { upper.contains(it) }
    return when {
        company != null -> "$company dominates today's market conversation"
        upper.contains("EARN") -> "Earnings expectations are reshaping sentiment"
        upper.contains("RATE") || upper.contains("INFLATION") -> "Macro policy risk is driving market positioning"
        upper.contains("AI") -> "AI infrastructure remains the market's key growth story"
        else -> "A major market story is developing"
    }
}

@Composable
fun ModernIntelligenceRow(label: String, value: String, emoji: String? = null) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (emoji != null) {
                Text(emoji, modifier = Modifier.padding(end = 6.dp))
            }
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun ModernIntelligenceChips(label: String, items: List<String>) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items.take(3).forEach { item ->
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(item, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                }
            }
        }
    }
}

@Composable
fun RecommendationTrendChartModern(trend: RecommendationTrend) {
    val total = (trend.strongBuy + trend.buy + trend.hold + trend.sell + trend.strongSell).toFloat()
    if (total == 0f) return

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
        ) {
            if (trend.strongBuy > 0) Box(Modifier.weight(trend.strongBuy / total).fillMaxHeight().background(Color(0xFF1B5E20)))
            if (trend.buy > 0) Box(Modifier.weight(trend.buy / total).fillMaxHeight().background(Color(0xFF4ADE80)))
            if (trend.hold > 0) Box(Modifier.weight(trend.hold / total).fillMaxHeight().background(Color(0xFF94A3B8)))
            if (trend.sell > 0) Box(Modifier.weight(trend.sell / total).fillMaxHeight().background(Color(0xFFFBBF24)))
            if (trend.strongSell > 0) Box(Modifier.weight(trend.strongSell / total).fillMaxHeight().background(Color(0xFFF87171)))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ModernLegendItem("Buy", Color(0xFF4ADE80))
            ModernLegendItem("Hold", Color(0xFF94A3B8))
            ModernLegendItem("Sell", Color(0xFFFBBF24))
        }
    }
}

@Composable
fun ModernLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, shape = CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
