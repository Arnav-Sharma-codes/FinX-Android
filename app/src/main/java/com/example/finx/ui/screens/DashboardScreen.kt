package com.example.finx.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
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
import com.example.finx.data.model.*
import com.example.finx.data.memory.UserProfile
import com.example.finx.data.orchestrator.DailyBrief
import com.example.finx.data.orchestrator.OrchestratedAiResult
import com.example.finx.data.model.DashboardInsight
import com.example.finx.di.NetworkModule
import com.example.finx.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToStock: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val repository        = NetworkModule.marketRepository
    val orchestrator      = NetworkModule.aiOrchestrator
    val briefEngine       = NetworkModule.dailyBriefEngine
    val dashboardEngine   = NetworkModule.dashboardIntelligenceEngine
    val userMemoryStore   = NetworkModule.userMemoryStore
    val watchlistSymbols  = NetworkModule.watchlistSymbols
    val coroutineScope    = rememberCoroutineScope()
    val context           = LocalContext.current

    var watchlistQuotes    by remember { mutableStateOf<Map<String, QuoteResponse>>(emptyMap()) }
    var isLoadingWatchlist by remember { mutableStateOf(false) }
    var watchlistAiResult  by remember { mutableStateOf<OrchestratedAiResult?>(null) }
    var isAnalyzingWatchlist by remember { mutableStateOf(false) }
    var stockSearchQuery   by remember { mutableStateOf("") }
    
    var dailyBrief         by remember { mutableStateOf<DailyBrief?>(null) }
    var isLoadingBrief     by remember { mutableStateOf(false) }
    
    var dashboardInsights by remember { mutableStateOf<List<DashboardInsight>>(emptyList()) }
    var isLoadingInsights by remember { mutableStateOf(false) }
    
    var userProfile        by remember { mutableStateOf(UserProfile()) }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val speakBrief = { text: String ->
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    val analyzeWatchlist = { quotes: Map<String, QuoteResponse> ->
        if (quotes.isNotEmpty()) {
            isAnalyzingWatchlist = true
            coroutineScope.launch {
                watchlistAiResult = orchestrator.analyzeWatchlist(quotes)
                isAnalyzingWatchlist = false
            }
        }
    }

    val refreshDashboard = {
        isLoadingBrief = true
        isLoadingInsights = true
        coroutineScope.launch {
            val symbols = watchlistSymbols.toList()
            dailyBrief = briefEngine.getDailyBrief(symbols)
            isLoadingBrief = false
            
            dashboardInsights = dashboardEngine.getInsights(symbols)
            isLoadingInsights = false
        }
    }

    val fetchWatchlist = {
        isLoadingWatchlist = true
        coroutineScope.launch {
            val updatedMap = mutableMapOf<String, QuoteResponse>()
            val symbols = watchlistSymbols.toList()
            symbols.forEach { sym ->
                repository.getQuote(sym).fold(
                    onSuccess = { updatedMap[sym] = it },
                    onFailure = { /* ignore */ }
                )
            }
            watchlistQuotes = updatedMap
            isLoadingWatchlist = false
            analyzeWatchlist(updatedMap)
        }
    }

    LaunchedEffect(Unit) {
        userProfile = userMemoryStore.loadProfile()
        fetchWatchlist()
        refreshDashboard()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            MeshGradientBackground()
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
            ) {
                item {
                    GreetingSection(
                        userProfile = userProfile,
                        onProfileClick = onNavigateToProfile
                    )
                }

                item {
                    AnalystMorningBriefHero(
                        profile = userProfile,
                        brief = dailyBrief,
                        aiResult = watchlistAiResult,
                        isLoading = isLoadingBrief || isAnalyzingWatchlist,
                        onRefresh = { refreshDashboard(); fetchWatchlist() },
                        onSpeak = { text -> speakBrief(text) },
                        modifier = Modifier.fadeInSlideIn(delay = 100)
                    )
                }

                item {
                    StockLookupField(
                        value = stockSearchQuery,
                        onValueChange = { stockSearchQuery = it },
                        onSymbolSelected = { result ->
                            if (!watchlistSymbols.contains(result.symbol)) {
                                watchlistSymbols.add(result.symbol)
                            }
                            coroutineScope.launch { userMemoryStore.recordSymbolView(result.symbol) }
                            fetchWatchlist()
                        },
                        label = "Search company or ticker",
                        modifier = Modifier.fadeInSlideIn(delay = 200)
                    )
                }

                item {
                    MarketInsightsSection(
                        insights = dashboardInsights, 
                        isLoading = isLoadingInsights,
                        modifier = Modifier.fadeInSlideIn(delay = 300)
                    )
                }

                item {
                    OrchestratedDailyBriefCard(
                        brief = dailyBrief,
                        aiResult = watchlistAiResult,
                        isLoading = isAnalyzingWatchlist,
                        onRefresh = { fetchWatchlist() },
                        modifier = Modifier.fadeInSlideIn(delay = 400)
                    )
                }

                item {
                    TodayOpportunitiesSection(
                        brief = dailyBrief,
                        modifier = Modifier.fadeInSlideIn(delay = 500)
                    )
                }

                item {
                    WatchlistHeader(
                        onRefresh = { fetchWatchlist() },
                        modifier = Modifier.fadeInSlideIn(delay = 600)
                    )
                }

                if (isLoadingWatchlist && watchlistQuotes.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = PrimaryIndigo)
                        }
                    }
                } else {
                    items(watchlistSymbols.toList()) { sym ->
                        val quote = watchlistQuotes[sym]
                        if (quote != null) {
                            Surface(
                                onClick = { onNavigateToStock(sym) }, 
                                color = Color.Transparent,
                                modifier = Modifier.fadeInSlideIn(delay = 700)
                            ) {
                                WatchlistCardPremium(symbol = sym, quote = quote)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeshGradientBackground() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .graphicsLayer(alpha = 0.5f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(PrimaryIndigo.copy(alpha = 0.3f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(200f, 100f),
                        radius = 600f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(TertiaryViolet.copy(alpha = 0.2f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(1000f, 400f),
                        radius = 800f
                    )
                )
        )
    }
}

@Composable
fun GreetingSection(userProfile: UserProfile, onProfileClick: () -> Unit = {}) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) { in 0..11 -> "Good Morning"; in 12..16 -> "Good Afternoon"; else -> "Good Evening" }

    val infiniteTransition = rememberInfiniteTransition(label = "NudgePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        if (!userProfile.isComplete) {
            Surface(
                onClick = onProfileClick,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .graphicsLayer(alpha = pulseAlpha),
                color = PrimaryIndigo.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.2f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Set up your professional profile to unlock AI alpha",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = greeting, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextSecondary)
                Text(
                    text = if (userProfile.fullName.isNotBlank()) userProfile.fullName.split(" ").first() else "Market Intelligence",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-1).sp
                )
            }

            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GlassWhite.copy(alpha = 0.7f))
            ) {
                Icon(Icons.Rounded.Person, contentDescription = "User Profile", tint = PrimaryIndigo, modifier = Modifier.size(23.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalystMorningBriefHero(
    profile: UserProfile,
    brief: DailyBrief?,
    aiResult: OrchestratedAiResult?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSpeak: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val mood = brief?.marketMood ?: aiResult?.outlook ?: "Neutral"
    val confidence = aiResult?.confidenceScore ?: if (brief != null) 74 else 0
    
    val summaryText = aiResult?.executiveSummary?.takeIf { it.isNotBlank() }
        ?: brief?.marketOverview?.takeIf { it.isNotBlank() }
        ?: "FinX is gathering market context, watchlist movement, news, analyst sentiment, and technical signals for your morning read."

    val evidence = buildList {
        brief?.topWinners?.firstOrNull()?.let { add("${it.symbol} ${formatSignedPercent(it.percentChange)}") }
        brief?.sectorRotation?.takeIf { it.isNotBlank() }?.let { add(it) }
        aiResult?.technicalSignals?.takeIf { it.isNotBlank() }?.let { add("Technicals: $it") }
        aiResult?.fundamentalSignals?.takeIf { it.isNotBlank() }?.let { add("Fundamentals: $it") }
        aiResult?.importantNews?.firstOrNull()?.let { add("News: $it") }
    }.take(5)
    val opportunities = (aiResult?.opportunities.orEmpty() + brief?.personalRecommendations.orEmpty() + listOfNotNull(brief?.aiOpportunity))
        .filter { it.isNotBlank() }
        .distinct()
        .take(3)
    val risks = (aiResult?.risks.orEmpty() + brief?.topLosers.orEmpty().map { "${it.symbol} is under pressure at ${formatSignedPercent(it.percentChange)}" })
        .filter { it.isNotBlank() }
        .distinct()
        .take(3)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .smoothPress(0.99f)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFF171922).copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(PrimaryIndigo, TertiaryViolet))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Morning Brief", style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("Market Brief", style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh brief", tint = PrimaryIndigo)
                }
                IconButton(onClick = { onSpeak(summaryText) }) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Listen to brief", tint = PrimaryIndigo)
                }
            }

            if (isLoading && brief == null && aiResult == null) {
                AnalystBriefSkeleton()
            } else {
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnalystEvidenceChip("Mood", mood, moodColor(mood))
                    if (confidence > 0) AnalystEvidenceChip("Confidence", "$confidence%", PrimaryIndigo)
                }

                if (evidence.isNotEmpty()) {
                    AnalystBriefSection(
                        title = "Why This Matters",
                        icon = Icons.Rounded.FactCheck,
                        color = PrimaryIndigo,
                        items = evidence
                    )
                }

                AnalystSignalStrip(
                    opportunities = opportunities.ifEmpty { listOf("Wait for stronger confirmation before adding risk.") },
                    risks = risks.ifEmpty { listOf("No major watchlist risk detected.") }
                )

                val action = aiResult?.suggestedActions?.firstOrNull()
                    ?: brief?.personalRecommendations?.firstOrNull()
                    ?: "Review your watchlist before adding risk; prioritize names with strong fundamentals and confirmed momentum."
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Final AI Verdict", style = MaterialTheme.typography.labelSmall, color = PrimaryIndigo, fontWeight = FontWeight.ExtraBold)
                        Text(action, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold, lineHeight = 19.sp)
                        Text(
                            "Tailored analysis for your profile and watchlist activity.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalystBriefSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == 3) 0.7f else 1f)
                    .height(if (index == 0) 22.dp else 14.dp)
                    .clip(CircleShape)
                    .background(GlassWhite.copy(alpha = 0.18f))
            )
        }
    }
}

@Composable
fun AnalystEvidenceChip(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(value, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun AnalystBriefSection(title: String, icon: ImageVector, color: Color, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
        }
        items.take(2).forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.padding(top = 7.dp).size(5.dp).background(color.copy(alpha = 0.9f), CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(item, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun AnalystSignalStrip(opportunities: List<String>, risks: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.055f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalystSignalLine("Opportunity", opportunities.firstOrNull().orEmpty(), Color(0xFF4ADE80))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            AnalystSignalLine("Risk", risks.firstOrNull().orEmpty(), Color(0xFFF87171))
        }
    }
}

@Composable
fun AnalystSignalLine(title: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.padding(top = 4.dp).size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary.copy(alpha = 0.9f), lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

fun moodColor(mood: String): Color = when {
    mood.contains("bull", ignoreCase = true) -> Color(0xFF4ADE80)
    mood.contains("bear", ignoreCase = true) -> Color(0xFFF87171)
    else -> Color(0xFFFBBF24)
}

fun formatSignedPercent(value: Double): String =
    "${if (value >= 0) "+" else ""}${String.format(Locale.getDefault(), "%.1f", value)}%"

@Composable
fun MarketInsightsSection(insights: List<DashboardInsight>, isLoading: Boolean, modifier: Modifier = Modifier) {
    var rotationIndex by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(insights) {
        if (insights.size > 2) {
            while (true) {
                kotlinx.coroutines.delay(10000)
                rotationIndex = (rotationIndex + 2) % (if (insights.size % 2 == 0) insights.size else insights.size + 1)
            }
        }
    }

    val displayInsights = remember(insights, rotationIndex) {
        if (insights.isEmpty()) emptyList()
        else {
            val first = insights.getOrNull(rotationIndex % insights.size)
            val second = insights.getOrNull((rotationIndex + 1) % insights.size)
            listOfNotNull(first, second)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth().height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(2) { index ->
            val insight = displayInsights.getOrNull(index)
            
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = if (isLoading && insights.isEmpty()) null else insight,
                    transitionSpec = {
                        if (initialState == null) {
                            (fadeIn(animationSpec = tween(1000, easing = EaseOutBack)) + 
                             slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(1000, easing = EaseOutBack))).togetherWith(
                             fadeOut(animationSpec = tween(500)))
                        } else {
                            (fadeIn(animationSpec = tween(800, easing = EaseInOutCubic)) + 
                             scaleIn(initialScale = 0.95f, animationSpec = tween(800))).togetherWith(
                             fadeOut(animationSpec = tween(600, easing = EaseInOutCubic)) + 
                             scaleOut(targetScale = 1.05f, animationSpec = tween(600)))
                        }
                    },
                    label = "SmoothInsightTransition"
                ) { targetInsight ->
                    if (targetInsight == null) {
                        ShimmerInsightCard()
                    } else {
                        ModernInsightCard(insight = targetInsight)
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerInsightCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "SubtleShimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        color = MidnightSurface.copy(alpha = alpha),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(GlassWhite.copy(0.1f)))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.5f).height(10.dp).clip(CircleShape).background(GlassWhite.copy(0.05f)))
                Box(modifier = Modifier.fillMaxWidth(0.9f).height(20.dp).clip(CircleShape).background(GlassWhite.copy(0.1f)))
                Box(modifier = Modifier.fillMaxWidth(0.7f).height(10.dp).clip(CircleShape).background(GlassWhite.copy(0.05f)))
            }
        }
    }
}

@Composable
fun ModernInsightCard(
    insight: DashboardInsight
) {
    val color = try { Color(android.graphics.Color.parseColor(insight.colorHex)) } catch (_: Exception) { PrimaryIndigo }
    val icon = when (insight.iconType) {
        "COMMODITY" -> Icons.Rounded.Diamond
        "TECHNICAL" -> Icons.Rounded.BarChart
        "NEWS"      -> Icons.Rounded.Public
        "FOREX"     -> Icons.Rounded.CurrencyExchange
        "ALERT"     -> Icons.Rounded.Notifications
        "TREND"     -> Icons.AutoMirrored.Rounded.TrendingUp
        else        -> Icons.Rounded.AutoAwesome
    }

    val isAlert = insight.iconType == "ALERT" || insight.priority >= 90
    val infiniteTransition = if (isAlert) rememberInfiniteTransition(label = "ProfessionalPulse") else null
    val glowAlpha by if (infiniteTransition != null) {
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
            label = "Glow"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Surface(
        modifier = Modifier.fillMaxSize().smoothPress(0.985f),
        shape = RoundedCornerShape(28.dp),
        color = MidnightSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isAlert) color.copy(alpha = 0.5f + glowAlpha) else GlassBorder
        )
    ) {
        if (isAlert) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.05f + glowAlpha/4), Color.Transparent),
                radius = 400f
            )))
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(insight.title, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(insight.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, letterSpacing = (-0.5).sp)
            }
            Text(insight.subtitle, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.85f), fontWeight = FontWeight.ExtraBold, lineHeight = 14.sp, maxLines = 2)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrchestratedDailyBriefCard(
    brief: DailyBrief?,
    aiResult: OrchestratedAiResult?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val infiniteTransition = rememberInfiniteTransition(label = "Glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "GlowAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .smoothPress(0.985f)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isLoading) PrimaryIndigo.copy(alpha = glowAlpha) else GlassBorder)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(PrimaryIndigo, TertiaryViolet))),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("AI Daily Brief", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text(brief?.date ?: "Orchestrated Intelligence", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
                Row {
                    IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = PrimaryIndigo) }
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = TextSecondary)
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = PrimaryIndigo, trackColor = GlassWhite)
                Text("Synthesizing market intelligence...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            } else if (isExpanded) {
                brief?.let { b ->
                    Surface(color = PrimaryIndigo.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.15f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(b.marketMoodEmoji, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Market Mood: ${b.marketMood}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo)
                                Text(b.marketOverview, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 18.sp)
                            }
                        }
                    }
                }

                aiResult?.let { r ->
                    if (r.executiveSummary.isNotBlank()) {
                        Surface(color = GlassWhite, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Psychology, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("INSTITUTIONAL ANALYSIS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo)
                                    Spacer(Modifier.weight(1f))
                                    Text("${r.confidenceScore}% Confidence", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(r.executiveSummary, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } ?: run {
                    if (brief == null) {
                        Text("Add stocks to your watchlist for personalized AI intelligence.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 22.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TodayOpportunitiesSection(brief: DailyBrief? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Today's Opportunities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val winner = brief?.topWinners?.firstOrNull()
            OpportunityCard(
                modifier = Modifier.weight(1f),
                sector = winner?.let { "${it.symbol} +${String.format("%.1f", it.percentChange)}%" } ?: "Tech",
                trend = if (winner != null) "Top Gainer" else "+2.4%",
                icon = Icons.Rounded.TrendingUp,
                color = Color(0xFF4ADE80)
            )
            OpportunityCard(
                modifier = Modifier.weight(1f),
                sector = brief?.aiOpportunity?.take(20)?.plus("…") ?: "Energy",
                trend = "AI Spotted",
                icon = Icons.Rounded.AutoAwesome,
                color = PrimaryIndigo
            )
        }
    }
}

@Composable
fun OpportunityCard(modifier: Modifier, sector: String, trend: String, icon: ImageVector, color: Color) {
    Surface(
        modifier = modifier.smoothPress(0.97f),
        shape = RoundedCornerShape(24.dp),
        color = MidnightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = color)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(sector, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(trend, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4ADE80), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun WatchlistHeader(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Watchlist Summary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .clip(CircleShape)
                .background(GlassWhite)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun WatchlistCardPremium(
    symbol: String,
    quote: QuoteResponse
) {
    val isPositive = quote.change >= 0
    val trendColor = if (isPositive) Color(0xFF4ADE80) else Color(0xFFF87171)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .smoothPress(0.975f),
        shape = RoundedCornerShape(24.dp),
        color = MidnightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassWhite),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    symbol.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Stock • NASDAQ", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${String.format(Locale.getDefault(), "%.2f", quote.currentPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "${if (isPositive) "+" else ""}${String.format(Locale.getDefault(), "%.2f", quote.percentChange)}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = trendColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
