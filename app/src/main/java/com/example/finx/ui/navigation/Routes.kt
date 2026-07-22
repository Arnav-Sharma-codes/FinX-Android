package com.example.finx.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Dashboard : Route

    @Serializable
    data object Comparison : Route

    @Serializable
    data object Insights : Route

    @Serializable
    data class StockDetails(val symbol: String) : Route
}
