package com.example.finx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.finx.ui.navigation.Route
import com.example.finx.ui.screens.ComparisonScreen
import com.example.finx.ui.screens.DashboardScreen
import com.example.finx.ui.screens.InsightsScreen
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
    
    val myEntryProvider = entryProvider<NavKey> {
        entry<Route.Dashboard> { DashboardScreen() }
        entry<Route.Comparison> { ComparisonScreen() }
        entry<Route.Insights> { InsightsScreen() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "FinX",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.0).sp,
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
                }
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
                    
                    val direction = if (items.indexOfFirst { it.route == target } > 
                                      items.indexOfFirst { it.route == initial }) 1 else -1
                    
                    val enter = slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetX = { it * direction / 4 }
                    ) + fadeIn(animationSpec = tween(220)) + scaleIn(
                        animationSpec = tween(260),
                        initialScale = 0.985f
                    )
                    val exit = slideOutHorizontally(
                        animationSpec = tween(180),
                        targetOffsetX = { -it * direction / 8 }
                    ) + fadeOut(animationSpec = tween(160))
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
        }
    }
}

@Composable
fun FinXModernNavigationBar(
    currentRoute: Route,
    onNavigate: (Route) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val interactionSource = remember { MutableInteractionSource() }
                val animatedWeight by animateFloatAsState(
                    targetValue = if (isSelected) 1.5f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "WeightAnimation"
                )

                Column(
                    modifier = Modifier
                        .weight(animatedWeight)
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
                        modifier = Modifier.size(28.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary
                    )
                    AnimatedVisibility(visible = isSelected) {
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
        }
    }
}

private val items = listOf(
    NavigationItem("Dashboard", Route.Dashboard, Icons.Rounded.Dashboard),
    NavigationItem("Comparison", Route.Comparison, Icons.AutoMirrored.Rounded.CompareArrows),
    NavigationItem("Insights", Route.Insights, Icons.Rounded.Analytics)
)

private data class NavigationItem(
    val label: String,
    val route: Route,
    val icon: ImageVector
)
