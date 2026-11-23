package com.example.data.remote

import com.example.data.model.AccessType
import com.example.data.model.AiMessage
import com.example.data.model.BuiltInCategories
import com.example.data.model.RegistrationRequirement
import com.example.data.model.SearchFilterState
import com.example.data.model.SkillLevel
import com.example.data.model.SourceType
import com.example.data.model.VerificationStatus
import com.example.data.model.WebSearchSnippet
import com.example.data.model.WebsiteResource
import com.example.data.model.domainToSlug
import com.example.data.model.extractDomain
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class GeminiOsintService {

    private val discoveryItemSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Official website or tool name"),
            "url" to Schema.string(description = "Official HTTPS URL to the primary domain"),
            "description" to Schema.string(description = "Concise factual description of what the website does"),
            "purpose" to Schema.string(description = "Primary intelligence or research purpose"),
            "categories" to Schema.array(
                items = Schema.string(),
                description = "1 to 3 relevant categories from: ${BuiltInCategories.all.joinToString(", ")}"
            ),
            "keyFeatures" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 key functional capabilities"
            ),
            "accessType" to Schema.enumeration(
                values = listOf("FREE", "LIMITED_ACCESS", "PAID"),
                description = "Use LIMITED_ACCESS for freemium/free-tier with caps, FREE only if core tool is completely free, PAID if no free tier"
            ),
            "pricingModel" to Schema.string(description = "Verified pricing model summary or 'Pricing unverified — check official site'"),
            "freeTierLimitations" to Schema.string(description = "Specific free-tier caps or 'None verified'"),
            "registrationRequired" to Schema.enumeration(
                values = listOf("NONE", "OPTIONAL", "REQUIRED", "UNVERIFIED"),
                description = "Whether account signup is required"
            ),
            "openSource" to Schema.boolean(description = "True if core software/data is open source"),
            "aiPowered" to Schema.boolean(description = "True if platform uses AI/ML capabilities"),
            "skillLevel" to Schema.enumeration(
                values = listOf("BEGINNER", "INTERMEDIATE", "PROFESSIONAL", "ALL")
            ),
            "recommendationScore" to Schema.integer(description = "Transparent relevance & usefulness score from 50 to 99"),
            "recommendationLabel" to Schema.string(description = "Ranking role such as 'Best Overall', 'Best Free Alternative', 'Best for Professionals', or 'Best for Beginners'"),
            "whyRecommended" to Schema.string(description = "Clear explanation of why this site matches the query"),
            "advantages" to Schema.array(items = Schema.string(), description = "2 to 3 concrete advantages"),
            "disadvantages" to Schema.array(items = Schema.string(), description = "1 to 3 honest limitations or drawbacks"),
            "whoShouldUse" to Schema.string(description = "Target analyst/user profile"),
            "verifiedVsEstimatedNote" to Schema.string(description = "Distinguish known official facts vs AI-estimated attributes")
        )
    )

    private val discoveryResponseSchema = Schema.obj(
        properties = mapOf(
            "searchIntentSummary" to Schema.string(description = "Brief interpretation of user intent and identified categories"),
            "websites" to Schema.array(
                items = discoveryItemSchema,
                description = "Ranked list of 5 to 7 real, official, non-duplicate websites"
            )
        )
    )

    private val enrichmentSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(),
            "description" to Schema.string(),
            "purpose" to Schema.string(),
            "categories" to Schema.array(items = Schema.string()),
            "keyFeatures" to Schema.array(items = Schema.string()),
            "accessType" to Schema.enumeration(listOf("FREE", "LIMITED_ACCESS", "PAID")),
            "pricingModel" to Schema.string(),
            "freeTierLimitations" to Schema.string(),
            "registrationRequired" to Schema.enumeration(listOf("NONE", "OPTIONAL", "REQUIRED", "UNVERIFIED")),
            "openSource" to Schema.boolean(),
            "aiPowered" to Schema.boolean(),
            "skillLevel" to Schema.enumeration(listOf("BEGINNER", "INTERMEDIATE", "PROFESSIONAL", "ALL")),
            "whyRecommended" to Schema.string(),
            "advantages" to Schema.array(items = Schema.string()),
            "disadvantages" to Schema.array(items = Schema.string()),
            "whoShouldUse" to Schema.string()
        )
    )

    private fun structuredModel(modelName: String, schema: Schema) =
        Firebase.ai.generativeModel(
            modelName = sanitizeModelName(modelName),
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )

    private val chatModelDefault by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private fun chatModel(modelName: String) =
        if (sanitizeModelName(modelName) == "gemini-3.8-flash") {
            chatModelDefault
        } else {
            Firebase.ai.generativeModel(modelName = sanitizeModelName(modelName))
        }

    suspend fun discoverWebsites(
        userId: String,
        query: String,
        filters: SearchFilterState,
        liveSnippets: List<WebSearchSnippet>,
        modelName: String
    ): Pair<String, List<WebsiteResource>> {
        val snippetsContext = if (liveSnippets.isNotEmpty()) {
            liveSnippets.joinToString("\n") {
                "- [${it.provider}] ${it.title} (${it.url}): ${it.snippet}"
            }
        } else {
            "No external search API keys configured; rely strictly on well-established official primary domains."
        }

        val prompt = """
            You are IntelAtlas, an OSINT Website Discovery & Intelligence Engine.
            User Query: "$query"
            Active Filters: ${filters.summaryText()}
            
            External Live Search Evidence:
            $snippetsContext
            
            STRICT ACCURACY RULES:
            1. NEVER invent or hallucinate website URLs. Only return well-known, real, official primary domain URLs (or URLs from the live search evidence above).
            2. Do NOT fabricate exact dollar prices, user counts, or fake ratings. If pricing varies or cannot be verified, state "Verify current pricing on official site" and label uncertain details explicitly in verifiedVsEstimatedNote.
            3. NEVER label a paid service as FREE just because it has a time-limited trial; classify freemium/capped tiers as LIMITED_ACCESS.
            4. Deduplicate domains so every website has a unique primary domain.
            5. Rank the strongest 5 to 7 matches and assign clear recommendationLabel values (e.g., "Best Overall", "Best Free Alternative", "Best for Professionals", "Best for Beginners").
        """.trimIndent()

        val response = withTimeout(60_000L) {
            structuredModel(modelName, discoveryResponseSchema).generateContent(prompt)
        }
        val rawJson = response.text ?: throw IllegalStateException("AI discovery returned an empty response.")
        val root = JSONObject(rawJson)
        val intentSummary = root.optString("searchIntentSummary", "AI analysis for \"$query\"")
        val sitesArray = root.optJSONArray("websites")
            ?: throw IllegalStateException("AI discovery response missing websites array.")

        val seenDomains = mutableSetOf<String>()
        val results = mutableListOf<WebsiteResource>()
        for (i in 0 until sitesArray.length()) {
            val obj = sitesArray.optJSONObject(i) ?: continue
            val rawUrl = obj.optString("url").trim()
            if (rawUrl.isBlank()) continue
            val normalizedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                rawUrl
            } else {
                "https://$rawUrl"
            }
            val domain = extractDomain(normalizedUrl)
            if (domain.isBlank() || !seenDomains.add(domain)) continue

            val categories = jsonStringList(obj, "categories").ifEmpty { listOf("Research") }.take(8)
            val keyFeatures = jsonStringList(obj, "keyFeatures").take(12)
            val advantages = jsonStringList(obj, "advantages").take(8)
            val disadvantages = jsonStringList(obj, "disadvantages").take(8)

            results.add(
                WebsiteResource(
                    id = domainToSlug(domain),
                    userId = userId,
                    name = obj.optString("name", domain).take(130),
                    url = normalizedUrl.take(490),
                    domain = domain.take(190),
                    description = obj.optString("description").take(1450),
                    purpose = obj.optString("purpose").take(490),
                    categories = categories,
                    keyFeatures = keyFeatures,
                    accessType = AccessType.fromRaw(obj.optString("accessType")).wireValue,
                    pricingModel = obj.optString("pricingModel", "Verify on official site").take(290),
                    freeTierLimitations = obj.optString("freeTierLimitations", "Verify on official site").take(490),
                    registrationRequired = RegistrationRequirement.fromRaw(obj.optString("registrationRequired")).wireValue,
                    openSource = obj.optBoolean("openSource", false),
                    aiPowered = obj.optBoolean("aiPowered", false),
                    skillLevel = SkillLevel.fromRaw(obj.optString("skillLevel")).wireValue,
                    recommendationScore = obj.optInt("recommendationScore", 75).coerceIn(0, 100),
                    recommendationLabel = obj.optString("recommendationLabel", "Recommended").take(115),
                    whyRecommended = obj.optString("whyRecommended").take(980),
                    advantages = advantages,
                    disadvantages = disadvantages,
                    whoShouldUse = obj.optString("whoShouldUse").take(490),
                    verifiedFacts = obj.optString("verifiedVsEstimatedNote", "AI-evaluated; pending live HTTP probe.").take(590),
                    sourceType = SourceType.AI_DISCOVERED.wireValue,
                    savedByUser = false,
                    notes = "",
                    tags = categories.map { it.lowercase() }.take(5),
                    verificationStatus = VerificationStatus.UNVERIFIED.wireValue,
                    httpStatusCode = 0,
                    lastCheckedAt = "Pending live probe",
                    visitCount = 0
                )
            )
        }
        return Pair(intentSummary, results)
    }

    suspend fun findFreeAlternatives(
        userId: String,
        paidToolOrNeed: String,
        modelName: String
    ): Pair<String, List<WebsiteResource>> {
        val query = "Best genuinely free or open-source alternatives to $paidToolOrNeed with honest free-tier limitations"
        return discoverWebsites(
            userId = userId,
            query = query,
            filters = SearchFilterState(accessTypes = setOf(AccessType.FREE, AccessType.LIMITED_ACCESS)),
            liveSnippets = emptyList(),
            modelName = modelName
        )
    }

    suspend fun enrichWebsiteUrl(
        userId: String,
        url: String,
        probe: DomainProbeResult,
        modelName: String
    ): WebsiteResource {
        val domain = extractDomain(probe.resolvedUrl.ifBlank { url })
        val prompt = """
            Analyze this verified website for an OSINT & Research Website Intelligence Library:
            URL: ${probe.resolvedUrl}
            Domain: $domain
            HTTP Probe Status: ${probe.httpStatusCode} (${probe.status.displayLabel})
            Observed HTML Title: ${probe.pageTitle.ifBlank { "Unavailable" }}
            
            Provide factual classification, honest accessType (FREE / LIMITED_ACCESS / PAID), key features, and target audience.
            Do NOT invent unverified prices or fake user numbers.
        """.trimIndent()

        val response = withTimeout(45_000L) {
            structuredModel(modelName, enrichmentSchema).generateContent(prompt)
        }
        val obj = JSONObject(response.text ?: "{}")
        val categories = jsonStringList(obj, "categories").ifEmpty { listOf("Research") }.take(8)
        return WebsiteResource(
            id = domainToSlug(domain),
            userId = userId,
            name = obj.optString("name", domain).ifBlank { domain }.take(130),
            url = probe.resolvedUrl.take(490),
            domain = domain.take(190),
            description = obj.optString("description", probe.pageTitle).take(1450),
            purpose = obj.optString("purpose", "User-added web resource").take(490),
            categories = categories,
            keyFeatures = jsonStringList(obj, "keyFeatures").take(12),
            accessType = AccessType.fromRaw(obj.optString("accessType")).wireValue,
            pricingModel = obj.optString("pricingModel", "Check official domain").take(290),
            freeTierLimitations = obj.optString("freeTierLimitations", "Check official domain").take(490),
            registrationRequired = RegistrationRequirement.fromRaw(obj.optString("registrationRequired")).wireValue,
            openSource = obj.optBoolean("openSource", false),
            aiPowered = obj.optBoolean("aiPowered", false),
            skillLevel = SkillLevel.fromRaw(obj.optString("skillLevel")).wireValue,
            recommendationScore = 80,
            recommendationLabel = "User Indexed",
            whyRecommended = obj.optString("whyRecommended", "Added and verified by user.").take(980),
            advantages = jsonStringList(obj, "advantages").take(8),
            disadvantages = jsonStringList(obj, "disadvantages").take(8),
            whoShouldUse = obj.optString("whoShouldUse", "Researchers & analysts").take(490),
            verifiedFacts = probe.factSummary.take(590),
            sourceType = SourceType.USER_ADDED.wireValue,
            savedByUser = true,
            notes = "",
            tags = categories.map { it.lowercase() }.take(5),
            verificationStatus = probe.status.wireValue,
            httpStatusCode = probe.httpStatusCode,
            lastCheckedAt = probe.checkedAtIso,
            visitCount = 0
        )
    }

    suspend fun compareWebsites(
        websites: List<WebsiteResource>,
        userGoal: String,
        modelName: String
    ): String {
        val siteSummaries = websites.joinToString("\n\n") { site ->
            """
            [${site.name}] (${site.url})
            - Access: ${site.accessEnum.displayLabel} | Pricing: ${site.pricingModel}
            - Free Tier Limits: ${site.freeTierLimitations}
            - Open Source: ${site.openSource} | AI-Powered: ${site.aiPowered} | Registration: ${site.registrationEnum.displayLabel}
            - Skill Level: ${site.skillEnum.displayLabel} | Score: ${site.recommendationScore}/100
            - Purpose: ${site.purpose}
            - Key Features: ${site.keyFeatures.joinToString(", ")}
            - Verified Info: ${site.verifiedFacts}
            """.trimIndent()
        }
        val prompt = """
            You are IntelAtlas Comparison Analyst.
            User's comparison goal: "${userGoal.ifBlank { "General OSINT / productivity evaluation" }}"
            
            Websites under evaluation:
            $siteSummaries
            
            Write a crisp, structured intelligence comparison:
            1. Executive Recommendation (which tool wins for which user need).
            2. Cost & Free-Tier Reality Check (do not confuse trials with free tools).
            3. Key Trade-offs (ease of use vs depth, open source vs managed, registration).
            4. Final Verdict.
            Keep facts grounded in the entries above and explicitly flag anything that requires checking the live site.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            chatModel(modelName).generateContent(prompt)
        }
        return response.text?.takeIf { it.isNotBlank() }
            ?: "Unable to generate comparison summary."
    }

    fun streamAiAsk(
        question: String,
        contextSummary: String,
        history: List<AiMessage>,
        modelName: String
    ): Flow<String> {
        val systemInstruction = """
            You are IntelAtlas AI Ask, an expert OSINT & Web Intelligence Research Assistant.
            Current Workspace Context:
            $contextSummary
            
            Rules:
            - Be accurate, concise, structured, and helpful.
            - Distinguish verified facts (HTTP probe status, explicit domain features) from estimates.
            - Never invent fake URLs, user counts, or exact prices you don't know.
            - Directly reference the websites in context when the user asks "which of these", "compare", or "find an alternative".
        """.trimIndent()

        val chatHistory: List<Content> = buildList {
            add(content("user") { text("System context:\n$systemInstruction") })
            add(content("model") { text("Understood. I have the current IntelAtlas workspace context ready.") })
            history.takeLast(10).forEach { msg ->
                val role = if (msg.role == "model") "model" else "user"
                add(content(role) { text(msg.content) })
            }
        }

        val chat = chatModel(modelName).startChat(history = chatHistory)
        return chat.sendMessageStream(question).map { chunk -> chunk.text.orEmpty() }
    }

    private fun jsonStringList(obj: JSONObject, key: String): List<String> {
        val arr = obj.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length())
            .mapNotNull { idx -> arr.optString(idx).trim().takeIf { it.isNotBlank() } }
    }

    private fun sanitizeModelName(modelName: String): String = when (modelName) {
        "gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon" -> modelName
        else -> "gemini-3.8-flash"
    }
}
