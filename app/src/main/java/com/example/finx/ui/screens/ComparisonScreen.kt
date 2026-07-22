package com.example.finx.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finx.data.model.QuoteResponse
import com.example.finx.data.orchestrator.OrchestratedAiResult
import com.example.finx.di.NetworkModule
import com.example.finx.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun ComparisonScreen() {
    val repository = NetworkModule.marketRepository
    val aiOrchestrator = NetworkModule.aiOrchestrator
    val coroutineScope = rememberCoroutineScope()

    var symbolA by remember { mutableStateOf("AAPL") }
    var symbolB by remember { mutableStateOf("MSFT") }

    var quoteA by remember { mutableStateOf<QuoteResponse?>(null) }
    var quoteB by remember { mutableStateOf<QuoteResponse?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var isAnalyzingComparison by remember { mutableStateOf(false) }
    var comparisonAiResult by remember { mutableStateOf<OrchestratedAiResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val compareButtonInteraction = remember { MutableInteractionSource() }

    val performComparison = {
        if (symbolA.isNotBlank() && symbolB.isNotBlank()) {
            isLoading = true
            comparisonAiResult = null
            errorMessage = null
            coroutineScope.launch {
                val cleanSymbolA = repository.resolveBestSymbol(symbolA)
                val cleanSymbolB = repository.resolveBestSymbol(symbolB)
                symbolA = cleanSymbolA
                symbolB = cleanSymbolB
                val resA = repository.getQuote(cleanSymbolA)
                val resB = repository.getQuote(cleanSymbolB)

                var successA = false
                var successB = false
                var latestQuoteA: QuoteResponse? = null
                var latestQuoteB: QuoteResponse? = null

                resA.fold(
                    onSuccess = {
                        quoteA = it
                        latestQuoteA = it
                        successA = true
                    },
                    onFailure = {}
                )

                resB.fold(
                    onSuccess = {
                        quoteB = it
                        latestQuoteB = it
                        successB = true
                    },
                    onFailure = {}
                )

                if (!successA || !successB) {
                    errorMessage = "Failed to fetch quotes."
                }
                isLoading = false

                if (successA && successB && latestQuoteA != null && latestQuoteB != null) {
                    isAnalyzingComparison = true
                    comparisonAiResult = aiOrchestrator.compareStocks(
                        cleanSymbolA,
                        latestQuoteA,
                        cleanSymbolB,
                        latestQuoteB
                    )
                    isAnalyzingComparison = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        performComparison()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Asset Comparison",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            letterSpacing = (-1).sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StockLookupField(
                value = symbolA,
                onValueChange = { symbolA = it },
                onSymbolSelected = { symbolA = it.symbol },
                label = "Primary Asset",
                modifier = Modifier.fillMaxWidth()
            )
            StockLookupField(
                value = symbolB,
                onValueChange = { symbolB = it },
                onSymbolSelected = { symbolB = it.symbol },
                label = "Comparison Asset",
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { performComparison() },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .smoothPress(0.985f, compareButtonInteraction),
            interactionSource = compareButtonInteraction,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Analyze Relativity", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        val qA = quoteA
        val qB = quoteB

        AnimatedContent(
            targetState = if (isLoading) "loading" else if (qA != null && qB != null) "result" else "empty",
            transitionSpec = {
                fadeIn(animationSpec = tween(500)).togetherWith(fadeOut(animationSpec = tween(400)))
            },
            label = "ComparisonTransition"
        ) { state ->
            when (state) {
                "loading" -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp, color = PrimaryIndigo)
                    }
                }
                "result" -> {
                    if (qA != null && qB != null) {
                        Column {
                            PremiumComparisonTable(
                                symA = symbolA.uppercase(),
                                quoteA = qA,
                                symB = symbolB.uppercase(),
                                quoteB = qB
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            ComparisonAiPremiumPanel(
                                result = comparisonAiResult,
                                isLoading = isAnalyzingComparison
                            )
                        }
                    }
                }
                else -> {}
            }
        }
        
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun ComparisonAiPremiumPanel(
    result: OrchestratedAiResult?,
    isLoading: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AnalysisGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "Glow"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = RoundedCornerShape(32.dp),
        color = MidnightSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isLoading) PrimaryIndigo.copy(alpha = glowAlpha * 3) else GlassBorder
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryIndigo.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp).align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    "AI Relativity Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = PrimaryIndigo,
                    trackColor = GlassWhite
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Computing comparative intelligence...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            } else if (result != null) {
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.22f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Should I invest?", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo)
                        Text(
                            result.suggestedActions.firstOrNull()
                                ?: result.executiveSummary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            lineHeight = 23.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(result.outlook, style = MaterialTheme.typography.labelLarge, color = moodColor(result.outlook), fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("${result.confidenceScore}% confidence", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                ComparisonDeepRow("Current Situation", result.executiveSummary)
                ComparisonDeepRow("Technical Outlook", result.technicalSignals.ifBlank { "No decisive technical edge yet." })
                ComparisonDeepRow("Fundamental Read", result.fundamentalSignals.ifBlank { "Fundamental context is being inferred from available market data." })
                result.bullishFactors.take(3).forEach { ComparisonDeepRow("Why It Could Work", it) }
                result.bearishFactors.take(3).forEach { ComparisonDeepRow("What Could Go Wrong", it) }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    color = GlassWhite,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("GENERATED FROM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Technical indicators, fundamental signals, market sentiment, analyst consensus, recent news, and multi-model AI validation.", style = MaterialTheme.typography.bodySmall, color = TextPrimary, lineHeight = 19.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Confidence: ${result.confidenceScore}% • ${result.modelsLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        result.conflictNote?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonDeepRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
fun PremiumComparisonTable(
    symA: String,
    quoteA: QuoteResponse,
    symB: String,
    quoteB: QuoteResponse
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MidnightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Metric", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextSecondary)
                Text(symA, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo)
                Text(symB, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = SecondaryTeal)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = GlassBorder)
            
            PremiumMetricRow("Price", quoteA.currentPrice, quoteB.currentPrice, "$%.2f")
            PremiumMetricRow("Change %", quoteA.percentChange, quoteB.percentChange, "%.2f%%")
            PremiumMetricRow("High", quoteA.highPrice, quoteB.highPrice, "$%.2f")
            PremiumMetricRow("Low", quoteA.lowPrice, quoteB.lowPrice, "$%.2f")
        }
    }
}

@Composable
fun PremiumMetricRow(label: String, valA: Double, valB: Double, format: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(String.format(Locale.getDefault(), format, valA), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(String.format(Locale.getDefault(), format, valB), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
