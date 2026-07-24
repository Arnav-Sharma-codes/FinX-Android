package com.example.finx.data.memory

/**
 * Persistent user profile that personalizes every AI response.
 * Stored via UserMemoryStore (DataStore-backed JSON).
 */
data class UserProfile(
    val fullName: String = "",
    val age: Int = 0,
    val country: String = "US",
    val preferredCurrency: String = "USD",
    val avatarUrl: String? = null,
    val joinedDate: Long = System.currentTimeMillis(),
    val membershipStatus: String = "Free",
    
    // Investment Profile
    val investmentStyle: String = "Growth",           // Growth | Value | Income | Speculation | Dividend | Long-term
    val riskTolerance: String = "Medium",             // Low | Medium | High
    val investmentHorizon: String = "Mid-term",       // Short-term | Mid-term | Long-term
    val investmentGoals: List<String> = listOf("Retirement", "Wealth Growth"),
    val preferredSectors: List<String> = listOf("Technology", "Finance"),
    
    // AI Preferences
    val preferredAiProvider: String = "Auto",         // Auto | Gemini | Groq | OpenRouter
    val aiResponseLength: String = "Balanced",        // Quick | Balanced | Detailed
    val aiExplanationStyle: String = "Professional",  // Beginner | Intermediate | Professional
    val enableDailyBrief: Boolean = true,
    val enablePersonalizedRecs: Boolean = true,
    val enableMarketAlerts: Boolean = true,
    
    // Activity Tracking
    val recentlyViewed: List<String> = emptyList(),   // last 10 symbols
    val frequentlySearched: Map<String, Int> = emptyMap(), // symbol → count
    val recentAiSummaries: List<String> = emptyList(), // last 5 AI responses
    val analysesCount: Int = 0,
    val newsReadCount: Int = 0,
    val daysActive: Int = 1,
    
    // Gamification
    val achievements: List<String> = emptyList(),
    val hasSeenWelcome: Boolean = false
) {
    val isComplete: Boolean get() = fullName.isNotBlank() && age > 0

    /** Build a compact personalization string to inject into every AI prompt. */
    fun toPromptContext(): String {
        val top = frequentlySearched.entries.sortedByDescending { it.value }.take(3).map { it.key }
        return buildString {
            append("USER PROFILE:\n")
            if (fullName.isNotBlank()) append("  Name: $fullName\n")
            if (age > 0) append("  Age: $age\n")
            append("  Investment style: $investmentStyle\n")
            append("  Risk tolerance: $riskTolerance\n")
            append("  Time horizon: $investmentHorizon\n")
            append("  Goals: ${investmentGoals.joinToString(", ")}\n")
            append("  AI Detail: $aiResponseLength, Style: $aiExplanationStyle\n")
            append("  Preferred sectors: ${preferredSectors.joinToString(", ")}\n")
            append("  Country: $country\n")
            if (recentlyViewed.isNotEmpty()) append("  Recently viewed: ${recentlyViewed.take(5).joinToString(", ")}\n")
            if (top.isNotEmpty()) append("  Frequently searches: ${top.joinToString(", ")}\n")
            append("Tailor response specifically for this user profile.")
        }
    }

    fun isDefault(): Boolean = preferredSectors == listOf("Technology", "Finance") &&
        riskTolerance == "Medium" && investmentStyle == "Growth"
}
