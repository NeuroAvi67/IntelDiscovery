package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.net.URI
import java.util.Locale

enum class AccessType(val wireValue: String, val displayLabel: String) {
    FREE("FREE", "FREE"),
    LIMITED_ACCESS("LIMITED_ACCESS", "LIMITED ACCESS"),
    PAID("PAID", "PAID");

    companion object {
        fun fromRaw(raw: String?): AccessType =
            entries.firstOrNull { it.wireValue.equals(raw?.trim(), ignoreCase = true) } ?: LIMITED_ACCESS
    }
}

enum class RegistrationRequirement(val wireValue: String, val displayLabel: String) {
    NONE("NONE", "No Registration"),
    OPTIONAL("OPTIONAL", "Optional Account"),
    REQUIRED("REQUIRED", "Account Required"),
    UNVERIFIED("UNVERIFIED", "Unverified");

    companion object {
        fun fromRaw(raw: String?): RegistrationRequirement =
            entries.firstOrNull { it.wireValue.equals(raw?.trim(), ignoreCase = true) } ?: UNVERIFIED
    }
}

enum class SkillLevel(val wireValue: String, val displayLabel: String) {
    BEGINNER("BEGINNER", "Beginner Friendly"),
    INTERMEDIATE("INTERMEDIATE", "Intermediate"),
    PROFESSIONAL("PROFESSIONAL", "Professional"),
    ALL("ALL", "All Levels");

    companion object {
        fun fromRaw(raw: String?): SkillLevel =
            entries.firstOrNull { it.wireValue.equals(raw?.trim(), ignoreCase = true) } ?: ALL
    }
}

enum class SourceType(val wireValue: String, val displayLabel: String) {
    CURATED("CURATED", "Curated Directory"),
    AI_DISCOVERED("AI_DISCOVERED", "AI Discovered"),
    USER_ADDED("USER_ADDED", "User Added");

    companion object {
        fun fromRaw(raw: String?): SourceType =
            entries.firstOrNull { it.wireValue.equals(raw?.trim(), ignoreCase = true) } ?: AI_DISCOVERED
    }
}

enum class VerificationStatus(val wireValue: String, val displayLabel: String) {
    VERIFIED("VERIFIED", "Reachable"),
    UNREACHABLE("UNREACHABLE", "Unreachable"),
    UNVERIFIED("UNVERIFIED", "Pending Check");

    companion object {
        fun fromRaw(raw: String?): VerificationStatus =
            entries.firstOrNull { it.wireValue.equals(raw?.trim(), ignoreCase = true) } ?: UNVERIFIED
    }
}

@Keep
@IgnoreExtraProperties
data class WebsiteResource(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val url: String = "",
    val domain: String = "",
    val description: String = "",
    val purpose: String = "",
    val categories: List<String> = emptyList(),
    val keyFeatures: List<String> = emptyList(),
    val accessType: String = AccessType.LIMITED_ACCESS.wireValue,
    val pricingModel: String = "",
    val freeTierLimitations: String = "",
    val registrationRequired: String = RegistrationRequirement.UNVERIFIED.wireValue,
    val openSource: Boolean = false,
    val aiPowered: Boolean = false,
    val skillLevel: String = SkillLevel.ALL.wireValue,
    val recommendationScore: Int = 75,
    val recommendationLabel: String = "",
    val whyRecommended: String = "",
    val advantages: List<String> = emptyList(),
    val disadvantages: List<String> = emptyList(),
    val whoShouldUse: String = "",
    val verifiedFacts: String = "",
    val sourceType: String = SourceType.AI_DISCOVERED.wireValue,
    val savedByUser: Boolean = false,
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val verificationStatus: String = VerificationStatus.UNVERIFIED.wireValue,
    val httpStatusCode: Int = 0,
    val lastCheckedAt: String = "",
    val visitCount: Int = 0,
    val lastVisitedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val accessEnum: AccessType get() = AccessType.fromRaw(accessType)
    val registrationEnum: RegistrationRequirement get() = RegistrationRequirement.fromRaw(registrationRequired)
    val skillEnum: SkillLevel get() = SkillLevel.fromRaw(skillLevel)
    val sourceEnum: SourceType get() = SourceType.fromRaw(sourceType)
    val verificationEnum: VerificationStatus get() = VerificationStatus.fromRaw(verificationStatus)

    fun faviconUrl(): String =
        "https://www.google.com/s2/favicons?domain=${domain.ifBlank { extractDomain(url) }}&sz=128"
}

@Keep
@IgnoreExtraProperties
data class ResearchCollection(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val websiteIds: List<String> = emptyList(),
    val colorHex: String = "#3B82F6",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomCategory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SearchHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val query: String = "",
    val filtersSummary: String = "",
    val resultWebsiteIds: List<String> = emptyList(),
    val resultCount: Int = 0,
    val searchSource: String = "Gemini AI",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AiConversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val contextLabel: String = "",
    val messageCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AiMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val content: String = "",
    val contextSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPreferences(
    @DocumentId val id: String = "",
    val userId: String = "",
    val darkTheme: Boolean = true,
    val preferredModel: String = "gemini-3.8-flash",
    val autoSaveDiscovered: Boolean = true,
    val autoVerifyLinks: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class SortOrder(val label: String) {
    RELEVANCE("Relevance & Score"),
    RECENTLY_ADDED("Recently Discovered"),
    CATEGORY("Category"),
    NAME("Alphabetical")
}

data class SearchFilterState(
    val accessTypes: Set<AccessType> = emptySet(),
    val openSourceOnly: Boolean = false,
    val aiPoweredOnly: Boolean = false,
    val noRegistrationOnly: Boolean = false,
    val beginnerFriendlyOnly: Boolean = false,
    val professionalOnly: Boolean = false,
    val recentlyDiscoveredOnly: Boolean = false,
    val selectedCategory: String? = null,
    val sortOrder: SortOrder = SortOrder.RELEVANCE
) {
    fun summaryText(): String {
        val parts = mutableListOf<String>()
        if (accessTypes.isNotEmpty()) parts.add(accessTypes.joinToString("/") { it.displayLabel })
        if (openSourceOnly) parts.add("Open Source")
        if (aiPoweredOnly) parts.add("AI-Powered")
        if (noRegistrationOnly) parts.add("No Registration")
        if (beginnerFriendlyOnly) parts.add("Beginner")
        if (professionalOnly) parts.add("Professional")
        if (recentlyDiscoveredOnly) parts.add("Recent")
        selectedCategory?.let { parts.add(it) }
        return if (parts.isEmpty()) "All Sources" else parts.joinToString(" • ").take(280)
    }

    fun activeCount(): Int {
        var count = accessTypes.size
        if (openSourceOnly) count++
        if (aiPoweredOnly) count++
        if (noRegistrationOnly) count++
        if (beginnerFriendlyOnly) count++
        if (professionalOnly) count++
        if (recentlyDiscoveredOnly) count++
        if (selectedCategory != null) count++
        return count
    }
}

enum class IntegrationStatus(val label: String) {
    BUILT_IN_ACTIVE("Connected (Built-in)"),
    CONNECTED("Connected"),
    DISCONNECTED("Disconnected"),
    CONFIGURATION_REQUIRED("Configuration Required"),
    CONNECTION_ERROR("Connection Error")
}

enum class SearchProviderId(val displayName: String, val description: String, val docsUrl: String) {
    TAVILY(
        displayName = "Tavily Search API",
        description = "AI-native live web search & content extraction API for real-time domain discovery.",
        docsUrl = "https://tavily.com"
    ),
    BRAVE(
        displayName = "Brave Search API",
        description = "Independent web search index API with clean organic snippets and domain coverage.",
        docsUrl = "https://brave.com/search/api/"
    ),
    SERPAPI(
        displayName = "SerpAPI (Google Search)",
        description = "Structured search engine results API for broad web and dataset discovery.",
        docsUrl = "https://serpapi.com"
    )
}

data class IntegrationCardModel(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val status: IntegrationStatus,
    val isBuiltIn: Boolean,
    val keyConfigured: Boolean = false,
    val lastTestMessage: String = ""
)

data class WebSearchSnippet(
    val title: String,
    val url: String,
    val snippet: String,
    val provider: String
)

object BuiltInCategories {
    val all: List<String> = listOf(
        "OSINT",
        "Artificial Intelligence",
        "Cybersecurity",
        "Government Data",
        "Data Science",
        "Research",
        "Programming",
        "Web Development",
        "Education",
        "Finance",
        "Productivity",
        "Design",
        "Image Generation",
        "Video Generation",
        "Automation",
        "Business",
        "Science",
        "Technology",
        "Marketing",
        "Writing",
        "Analytics"
    )
}

fun extractDomain(rawUrl: String): String {
    return try {
        val normalized = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        val host = URI(normalized).host.orEmpty().lowercase(Locale.US)
        host.removePrefix("www.").ifBlank { rawUrl.lowercase(Locale.US) }
    } catch (_: Exception) {
        rawUrl.removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .lowercase(Locale.US)
    }
}

fun domainToSlug(domainOrUrl: String): String {
    val domain = extractDomain(domainOrUrl)
    val sanitized = domain.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
    return sanitized.ifBlank { "site_${System.currentTimeMillis()}" }.take(120)
}
