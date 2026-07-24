package com.example.finx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.finx.ui.navigation.Route
import com.example.finx.ui.screens.*
import com.example.finx.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinXTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val backStack = rememberNavBackStack(Route.Dashboard)
    var showSearchOverlay by remember { mutableStateOf(false) }
    
    val myEntryProvider = entryProvider<NavKey> {
        entry<Route.Dashboard> { 
            DashboardScreen(
                onNavigateToStock = { backStack.add(Route.StockDetails(it)) },
                onNavigateToProfile = { backStack.clear(); backStack.add(Route.Profile) }
            ) 
        }
        entry<Route.Comparison> { ComparisonScreen() }
        entry<Route.Insights> { InsightsScreen() }
        entry<Route.Profile> { ProfileScreen() }
        entry<Route.StockDetails> { 
            val r = it as Route.StockDetails
            StockDetailsScreen(symbol = r.symbol, onBack = { backStack.removeAt(backStack.size - 1) }) 
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "FinX",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            FinXModernNavigationBar(
                currentRoute = backStack.last() as Route,
                onNavigate = { route ->
                    if (backStack.last() != route) {
                        backStack.clear()
                        backStack.add(route)
                    }
                },
                onAddClick = { showSearchOverlay = true }
            )
        },
        containerColor = MidnightBackground
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MidnightBackground, MidnightSurface)
                )
            )
        ) {
            AnimatedContent(
                targetState = backStack.last(),
                transitionSpec = {
                    val target = targetState as Route
                    val initial = initialState as Route
                    
                    val targetIndex = items.indexOfFirst { it.route == target }
                    val initialIndex = items.indexOfFirst { it.route == initial }
                    
                    val direction = if (targetIndex > initialIndex) 1 else -1
                    
                    val enter = slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetX = { it * direction / 4 }
                    ) + fadeIn(animationSpec = tween(300)) + scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialScale = 0.92f
                    )
                    val exit = slideOutHorizontally(
                        animationSpec = tween(250, easing = FastOutSlowInEasing),
                        targetOffsetX = { -it * direction / 8 }
                    ) + fadeOut(animationSpec = tween(200)) + scaleOut(
                        animationSpec = tween(250),
                        targetScale = 0.95f
                    )
                    enter.togetherWith(exit)
                },
                label = "ScreenTransition",
                modifier = Modifier.fillMaxSize()
            ) { targetRoute: NavKey ->
                NavDisplay(
                    backStack = listOf(targetRoute),
                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                    entryProvider = myEntryProvider
                )
            }

            // Quick Search Overlay
            if (showSearchOverlay) {
                QuickSearchOverlay(
                    onDismiss = { showSearchOverlay = false },
                    onSymbolSelected = { symbol ->
                        showSearchOverlay = false
                        backStack.add(Route.StockDetails(symbol))
                    }
                )
            }
        }
    }
}

@Composable
fun QuickSearchOverlay(
    onDismiss: () -> Unit,
    onSymbolSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(onClick = onDismiss, indication = null, interactionSource = remember { MutableInteractionSource() })
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {}, // Prevent dismiss when clicking the content
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StockLookupField(
                value = query,
                onValueChange = { query = it },
                onSymbolSelected = { onSymbolSelected(it.symbol) },
                label = "Search company or ticker",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun FinXModernNavigationBar(
    currentRoute: Route,
    onNavigate: (Route) -> Unit,
    onAddClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF202127).copy(alpha = 0.94f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left items
            items.take(2).forEach { item ->
                NavigationTabItem(item, currentRoute == item.route, onNavigate)
            }
            
            // Center Add Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PrimaryIndigo, TertiaryViolet)))
                    .smoothPress(0.92f)
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            
            // Right items
            items.drop(2).forEach { item ->
                NavigationTabItem(item, currentRoute == item.route, onNavigate)
            }
        }
    }
}

@Composable
private fun NavigationTabItem(
    item: NavigationItem,
    isSelected: Boolean,
    onNavigate: (Route) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "IconScale"
    )

    Column(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .smoothPress(pressedScale = 0.92f, interactionSource = interactionSource)
            .selectable(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                indication = null,
                interactionSource = interactionSource
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            modifier = Modifier.size(26.dp).graphicsLayer(scaleX = animatedScale, scaleY = animatedScale),
            tint = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary
        )
        AnimatedVisibility(
            visible = isSelected,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            Text(
                item.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private val items = listOf(
    NavigationItem("Dashboard", Route.Dashboard, Icons.Rounded.Dashboard),
    NavigationItem("Comparison", Route.Comparison, Icons.AutoMirrored.Rounded.CompareArrows),
    NavigationItem("Insights", Route.Insights, Icons.Rounded.Analytics),
    NavigationItem("Profile", Route.Profile, Icons.Rounded.Person)
)

private data class NavigationItem(
    val label: String,
    val route: Route,
    val icon: ImageVector
)
