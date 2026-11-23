package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.UiState
import com.example.data.local.IntegrationPreferencesStore
import com.example.data.local.LocalIntegrationSecrets
import com.example.data.model.AccessType
import com.example.data.model.AiConversation
import com.example.data.model.AiMessage
import com.example.data.model.BuiltInCategories
import com.example.data.model.CustomCategory
import com.example.data.model.IntegrationCardModel
import com.example.data.model.IntegrationStatus
import com.example.data.model.RegistrationRequirement
import com.example.data.model.ResearchCollection
import com.example.data.model.SearchFilterState
import com.example.data.model.SearchHistoryEntry
import com.example.data.model.SearchProviderId
import com.example.data.model.SkillLevel
import com.example.data.model.SortOrder
import com.example.data.model.UserPreferences
import com.example.data.model.VerificationStatus
import com.example.data.model.WebsiteResource
import com.example.data.remote.GeminiOsintService
import com.example.data.remote.WebProbeService
import com.example.data.repository.OsintRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class AppRoute(val route: String, val label: String) {
    DASHBOARD("dashboard", "Dashboard"),
    AI_SEARCH("ai_search", "AI Search"),
    AI_ASK("ai_ask", "AI Ask"),
    EXPLORER("explorer", "Website Explorer"),
    CATEGORIES("categories", "Categories"),
    FREE_TOOLS("free_tools", "Free Tools"),
    SAVED("saved", "Saved Websites"),
    HISTORY("history", "History"),
    INTEGRATIONS("integrations", "Integrations"),
    COLLECTIONS("collections", "Collections"),
    COMPARE("compare", "Compare"),
    SETTINGS("settings", "Settings")
}

data class DiscoveryStats(
    val totalDiscovered: Int = 0,
    val savedCount: Int = 0,
    val freeCount: Int = 0,
    val limitedCount: Int = 0,
    val paidCount: Int = 0,
    val openSourceCount: Int = 0,
    val verifiedCount: Int = 0,
    val categoriesExplored: Int = 0,
    val searchesPerformed: Int = 0,
    val topCategories: List<Pair<String, Int>> = emptyList()
)

class IntelAtlasViewModel(
    private val repository: OsintRepository,
    private val geminiService: GeminiOsintService,
    private val webProbeService: WebProbeService,
    private val integrationStore: IntegrationPreferencesStore,
    private val currentUserId: String
) : ViewModel() {

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    val websitesState: StateFlow<UiState<List<WebsiteResource>>> =
        repository.observeWebsites(currentUserId)
            .map<List<WebsiteResource>, UiState<List<WebsiteResource>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "observeWebsites error", root)
                emit(UiState.Error(root.message ?: "Failed to load website intelligence library"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val collections: StateFlow<List<ResearchCollection>> =
        repository.observeCollections(currentUserId)
            .catch { error ->
                if (unwrapFirestoreError(error) == null && error is CancellationException) throw error
                Log.w(TAG, "observeCollections error", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customCategories: StateFlow<List<CustomCategory>> =
        repository.observeCustomCategories(currentUserId)
            .catch { error ->
                if (unwrapFirestoreError(error) == null && error is CancellationException) throw error
                Log.w(TAG, "observeCustomCategories error", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntry>> =
        repository.observeSearchHistory(currentUserId)
            .catch { error ->
                if (unwrapFirestoreError(error) == null && error is CancellationException) throw error
                Log.w(TAG, "observeSearchHistory error", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversations: StateFlow<List<AiConversation>> =
        repository.observeConversations(currentUserId)
            .catch { error ->
                if (unwrapFirestoreError(error) == null && error is CancellationException) throw error
                Log.w(TAG, "observeConversations error", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val preferences: StateFlow<UserPreferences> =
        repository.observePreferences(currentUserId)
            .catch { error ->
                if (unwrapFirestoreError(error) == null && error is CancellationException) throw error
                Log.w(TAG, "observePreferences error", error)
                emit(UserPreferences(id = "appSettings", userId = currentUserId))
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                UserPreferences(id = "appSettings", userId = currentUserId)
            )

    val integrationSecrets: StateFlow<LocalIntegrationSecrets> =
        integrationStore.state
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalIntegrationSecrets())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<AiMessage>> =
        _activeConversationId.flatMapLatest { convId ->
            if (convId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeMessages(currentUserId, convId)
                .catch { error ->
                    if (unwrapFirestoreError(error) == null && error is CancellationException) throw error
                    emit(emptyList())
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // AI Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterState = MutableStateFlow(SearchFilterState())
    val filterState: StateFlow<SearchFilterState> = _filterState.asStateFlow()

    private val _searchState = MutableStateFlow<UiState<List<WebsiteResource>>>(UiState.Idle)
    val searchState: StateFlow<UiState<List<WebsiteResource>>> = _searchState.asStateFlow()

    private val _searchIntentSummary = MutableStateFlow("")
    val searchIntentSummary: StateFlow<String> = _searchIntentSummary.asStateFlow()

    private val _searchPipelineStage = MutableStateFlow("")
    val searchPipelineStage: StateFlow<String> = _searchPipelineStage.asStateFlow()

    // AI Ask state
    private val _aiAskStreaming = MutableStateFlow<UiState<String>>(UiState.Idle)
    val aiAskStreaming: StateFlow<UiState<String>> = _aiAskStreaming.asStateFlow()

    private val _aiContextLabel = MutableStateFlow("Global Workspace")
    val aiContextLabel: StateFlow<String> = _aiContextLabel.asStateFlow()

    private val _aiFocusedWebsites = MutableStateFlow<List<WebsiteResource>>(emptyList())
    val aiFocusedWebsites: StateFlow<List<WebsiteResource>> = _aiFocusedWebsites.asStateFlow()

    // Comparison Engine state
    private val _compareIds = MutableStateFlow<List<String>>(emptyList())
    val compareIds: StateFlow<List<String>> = _compareIds.asStateFlow()

    private val _comparisonState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val comparisonState: StateFlow<UiState<String>> = _comparisonState.asStateFlow()

    // Free alternatives AI state
    private val _freeAltState = MutableStateFlow<UiState<List<WebsiteResource>>>(UiState.Idle)
    val freeAltState: StateFlow<UiState<List<WebsiteResource>>> = _freeAltState.asStateFlow()

    // URL enrichment state
    private val _addUrlInProgress = MutableStateFlow(false)
    val addUrlInProgress: StateFlow<Boolean> = _addUrlInProgress.asStateFlow()

    // Integration testing state
    private val _testingProvider = MutableStateFlow<SearchProviderId?>(null)
    val testingProvider: StateFlow<SearchProviderId?> = _testingProvider.asStateFlow()

    // Selected category / collection drilldowns
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedCuratedDirectoryIfNeeded(currentUserId)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterState(transform: (SearchFilterState) -> SearchFilterState) {
        _filterState.value = transform(_filterState.value)
    }

    fun selectCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun executeAiSearch(overrideQuery: String? = null) {
        val query = (overrideQuery ?: _searchQuery.value).trim()
        if (query.isBlank()) {
            _toastEvents.tryEmit("Enter a research query or topic to search.")
            return
        }
        _searchQuery.value = query

        viewModelScope.launch {
            _searchState.value = UiState.Loading
            try {
                val secrets = integrationSecrets.value
                val prefs = preferences.value
                val effectiveFilters = parseOperatorsIntoFilters(query, _filterState.value)

                _searchPipelineStage.value = if (secrets.hasAnyConfiguredSearchApi()) {
                    "Querying live Search APIs & Gemini Intelligence..."
                } else {
                    "Analyzing search intent with Gemini 3.8 Flash..."
                }

                val (snippets, usedProviders) = webProbeService.searchConfiguredProviders(query, secrets)
                val (intentSummary, discovered) = geminiService.discoverWebsites(
                    userId = currentUserId,
                    query = query,
                    filters = effectiveFilters,
                    liveSnippets = snippets,
                    modelName = prefs.preferredModel
                )
                _searchIntentSummary.value = intentSummary

                // Verify top links via real HTTP probe if enabled
                val verifiedList = if (prefs.autoVerifyLinks && discovered.isNotEmpty()) {
                    _searchPipelineStage.value = "Verifying live domain reachability via HTTP probe..."
                    discovered.map { site ->
                        async {
                            val probe = webProbeService.probeUrl(site.url)
                            site.copy(
                                url = probe.resolvedUrl.ifBlank { site.url },
                                verificationStatus = probe.status.wireValue,
                                httpStatusCode = probe.httpStatusCode,
                                lastCheckedAt = probe.checkedAtIso,
                                verifiedFacts = "${probe.factSummary} | ${site.verifiedFacts}".take(590)
                            )
                        }
                    }.awaitAll()
                } else {
                    discovered
                }

                val upsertResult = repository.upsertDiscoveredWebsites(
                    userId = currentUserId,
                    websites = verifiedList,
                    autoSave = prefs.autoSaveDiscovered
                )
                val finalResults = upsertResult.getOrDefault(verifiedList)

                val sourceLabel = if (usedProviders.isNotEmpty()) {
                    "${usedProviders.joinToString("+")} + Gemini AI"
                } else {
                    "Gemini AI + HTTP Probe"
                }

                repository.recordSearchHistory(
                    userId = currentUserId,
                    query = query,
                    filtersSummary = effectiveFilters.summaryText(),
                    resultWebsiteIds = finalResults.map { it.id },
                    searchSource = sourceLabel
                )

                _searchPipelineStage.value = ""
                _searchState.value = UiState.Success(finalResults)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Search timed out", e)
                _searchPipelineStage.value = ""
                _searchState.value = UiState.Error("Search timed out after 60s. Try a more specific query.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Search failed", e)
                _searchPipelineStage.value = ""
                _searchState.value = UiState.Error(e.localizedMessage ?: "AI Search failed.")
            }
        }
    }

    fun applyFilters(items: List<WebsiteResource>, filters: SearchFilterState): List<WebsiteResource> {
        val nowMs = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        val filtered = items.filter { site ->
            val accessOk = filters.accessTypes.isEmpty() || site.accessEnum in filters.accessTypes
            val ossOk = !filters.openSourceOnly || site.openSource
            val aiOk = !filters.aiPoweredOnly || site.aiPowered
            val noRegOk = !filters.noRegistrationOnly || site.registrationEnum == RegistrationRequirement.NONE
            val beginnerOk = !filters.beginnerFriendlyOnly ||
                site.skillEnum == SkillLevel.BEGINNER || site.skillEnum == SkillLevel.ALL
            val proOk = !filters.professionalOnly || site.skillEnum == SkillLevel.PROFESSIONAL
            val catOk = filters.selectedCategory == null ||
                site.categories.any { it.equals(filters.selectedCategory, ignoreCase = true) }
            val recentOk = !filters.recentlyDiscoveredOnly ||
                ((site.createdAt?.toDate()?.time ?: nowMs) >= (nowMs - sevenDaysMs))
            accessOk && ossOk && aiOk && noRegOk && beginnerOk && proOk && catOk && recentOk
        }

        return when (filters.sortOrder) {
            SortOrder.RELEVANCE -> filtered.sortedByDescending { it.recommendationScore }
            SortOrder.RECENTLY_ADDED -> filtered.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            SortOrder.CATEGORY -> filtered.sortedBy { it.categories.firstOrNull().orEmpty() }
            SortOrder.NAME -> filtered.sortedBy { it.name.lowercase() }
        }
    }

    fun verifyWebsiteLive(website: WebsiteResource) {
        viewModelScope.launch {
            _toastEvents.tryEmit("Probing ${website.domain}...")
            val probe = webProbeService.probeUrl(website.url)
            repository.updateWebsiteVerification(currentUserId, website, probe)
                .onSuccess {
                    _toastEvents.tryEmit("${website.name}: ${probe.status.displayLabel} (HTTP ${probe.httpStatusCode})")
                }
                .onFailure {
                    _toastEvents.tryEmit("Failed to update verification status")
                }
        }
    }

    fun toggleSaveWebsite(website: WebsiteResource) {
        viewModelScope.launch {
            repository.toggleSavedWebsite(currentUserId, website)
                .onSuccess { saved ->
                    _toastEvents.tryEmit(if (saved) "Saved ${website.name} to library" else "Removed ${website.name} from saved")
                }
                .onFailure { err ->
                    _toastEvents.tryEmit(err.localizedMessage ?: "Failed to save website")
                }
        }
    }

    fun recordVisit(website: WebsiteResource) {
        viewModelScope.launch {
            repository.recordWebsiteVisit(currentUserId, website)
        }
    }

    fun updateWebsiteNotesAndTags(
        website: WebsiteResource,
        notes: String,
        tagsCsv: String,
        categoriesCsv: String,
        description: String
    ) {
        val tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val cats = categoriesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
            .ifEmpty { website.categories }
        viewModelScope.launch {
            repository.updateWebsiteMetadata(
                userId = currentUserId,
                websiteId = website.id,
                notes = notes,
                tags = tags,
                categories = cats,
                description = description
            ).onSuccess {
                _toastEvents.tryEmit("Updated intelligence notes for ${website.name}")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Failed to update notes")
            }
        }
    }

    fun deleteWebsite(website: WebsiteResource) {
        viewModelScope.launch {
            repository.deleteWebsite(currentUserId, website.id)
                .onSuccess { _toastEvents.tryEmit("Deleted ${website.name}") }
                .onFailure { _toastEvents.tryEmit("Failed to delete website") }
        }
    }

    fun addCustomWebsiteByUrl(rawUrl: String) {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            _toastEvents.tryEmit("Enter a valid domain or URL")
            return
        }
        viewModelScope.launch {
            _addUrlInProgress.value = true
            try {
                val probe = webProbeService.probeUrl(trimmed)
                val enriched = geminiService.enrichWebsiteUrl(
                    userId = currentUserId,
                    url = trimmed,
                    probe = probe,
                    modelName = preferences.value.preferredModel
                )
                repository.upsertDiscoveredWebsites(currentUserId, listOf(enriched), autoSave = true)
                    .onSuccess {
                        _toastEvents.tryEmit("Indexed ${enriched.name} (${probe.status.displayLabel})")
                    }
                    .onFailure { err ->
                        _toastEvents.tryEmit(err.localizedMessage ?: "Failed to save website")
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "addCustomWebsiteByUrl failed", e)
                _toastEvents.tryEmit(e.localizedMessage ?: "Could not analyze URL")
            } finally {
                _addUrlInProgress.value = false
            }
        }
    }

    fun discoverFreeAlternatives(paidToolName: String) {
        val trimmed = paidToolName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            _freeAltState.value = UiState.Loading
            try {
                val (_, sites) = geminiService.findFreeAlternatives(
                    userId = currentUserId,
                    paidToolOrNeed = trimmed,
                    modelName = preferences.value.preferredModel
                )
                val saved = repository.upsertDiscoveredWebsites(
                    userId = currentUserId,
                    websites = sites,
                    autoSave = preferences.value.autoSaveDiscovered
                ).getOrDefault(sites)
                _freeAltState.value = UiState.Success(saved)
            } catch (e: TimeoutCancellationException) {
                _freeAltState.value = UiState.Error("Request timed out. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "discoverFreeAlternatives failed", e)
                _freeAltState.value = UiState.Error(e.localizedMessage ?: "Failed to find alternatives.")
            }
        }
    }

    // Collections
    fun createCollection(name: String, description: String, colorHex: String, initialWebsiteId: String? = null) {
        viewModelScope.launch {
            repository.createCollection(
                userId = currentUserId,
                name = name,
                description = description,
                colorHex = colorHex,
                initialWebsiteIds = listOfNotNull(initialWebsiteId)
            ).onSuccess {
                _toastEvents.tryEmit("Created collection \"$name\"")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Failed to create collection")
            }
        }
    }

    fun toggleWebsiteInCollection(collection: ResearchCollection, websiteId: String) {
        val updated = if (websiteId in collection.websiteIds) {
            collection.websiteIds - websiteId
        } else {
            collection.websiteIds + websiteId
        }
        viewModelScope.launch {
            repository.updateCollectionWebsites(currentUserId, collection.id, updated)
                .onSuccess {
                    val added = websiteId !in collection.websiteIds
                    _toastEvents.tryEmit(if (added) "Added to ${collection.name}" else "Removed from ${collection.name}")
                }
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            repository.deleteCollection(currentUserId, collectionId)
                .onSuccess { _toastEvents.tryEmit("Collection deleted") }
        }
    }

    // Custom categories
    fun createCustomCategory(name: String, description: String) {
        viewModelScope.launch {
            repository.createCustomCategory(currentUserId, name, description)
                .onSuccess { _toastEvents.tryEmit("Added category \"$name\"") }
                .onFailure { err -> _toastEvents.tryEmit(err.localizedMessage ?: "Failed to add category") }
        }
    }

    fun deleteCustomCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCustomCategory(currentUserId, categoryId)
        }
    }

    // History
    fun deleteHistoryEntry(historyId: String) {
        viewModelScope.launch {
            repository.deleteSearchHistoryEntry(currentUserId, historyId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory(currentUserId)
                .onSuccess { _toastEvents.tryEmit("Search history cleared") }
        }
    }

    // Comparison Engine
    fun toggleCompareWebsite(websiteId: String) {
        val current = _compareIds.value
        _compareIds.value = if (websiteId in current) {
            current - websiteId
        } else if (current.size >= 4) {
            _toastEvents.tryEmit("Compare supports up to 4 websites at once")
            current
        } else {
            _toastEvents.tryEmit("Added to comparison queue (${current.size + 1}/4)")
            current + websiteId
        }
    }

    fun clearCompare() {
        _compareIds.value = emptyList()
        _comparisonState.value = UiState.Idle
    }

    fun generateAiComparison(websites: List<WebsiteResource>, goal: String) {
        if (websites.size < 2) {
            _toastEvents.tryEmit("Select at least 2 websites to compare")
            return
        }
        viewModelScope.launch {
            _comparisonState.value = UiState.Loading
            try {
                val summary = geminiService.compareWebsites(
                    websites = websites,
                    userGoal = goal,
                    modelName = preferences.value.preferredModel
                )
                _comparisonState.value = UiState.Success(summary)
            } catch (e: TimeoutCancellationException) {
                _comparisonState.value = UiState.Error("Comparison timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Comparison error", e)
                _comparisonState.value = UiState.Error(e.localizedMessage ?: "Comparison failed.")
            }
        }
    }

    // AI Ask
    fun setAiContext(label: String, focusedWebsites: List<WebsiteResource> = emptyList()) {
        _aiContextLabel.value = label
        if (focusedWebsites.isNotEmpty()) {
            _aiFocusedWebsites.value = focusedWebsites
        }
    }

    fun startNewConversation() {
        _activeConversationId.value = null
        _aiAskStreaming.value = UiState.Idle
    }

    fun selectConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        _aiAskStreaming.value = UiState.Idle
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
            }
            repository.deleteConversation(currentUserId, conversationId)
        }
    }

    fun sendAiAskMessage(question: String, allLibrarySites: List<WebsiteResource>) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _aiAskStreaming.value = UiState.Loading
            try {
                val focusSites = _aiFocusedWebsites.value.ifEmpty {
                    when (val sr = _searchState.value) {
                        is UiState.Success -> sr.data.take(6)
                        else -> allLibrarySites.take(8)
                    }
                }
                val contextText = buildString {
                    appendLine("Active Page / Focus: ${_aiContextLabel.value}")
                    if (focusSites.isNotEmpty()) {
                        appendLine("Websites in current context:")
                        focusSites.forEach { site ->
                            appendLine("- ${site.name} (${site.url}) | ${site.accessEnum.displayLabel} | OpenSource=${site.openSource} | Reg=${site.registrationEnum.displayLabel} | Score=${site.recommendationScore} | Purpose: ${site.purpose} | Limits: ${site.freeTierLimitations}")
                        }
                    }
                }

                val priorHistory = activeMessages.value
                val userSave = repository.ensureConversationAndAppendMessage(
                    userId = currentUserId,
                    existingConversationId = _activeConversationId.value,
                    title = trimmed.take(60),
                    contextLabel = _aiContextLabel.value,
                    role = "user",
                    content = trimmed,
                    contextSummary = _aiContextLabel.value
                )
                val convId = userSave.getOrThrow()
                _activeConversationId.value = convId

                val accumulated = StringBuilder()
                geminiService.streamAiAsk(
                    question = trimmed,
                    contextSummary = contextText,
                    history = priorHistory,
                    modelName = preferences.value.preferredModel
                ).collect { chunk ->
                    accumulated.append(chunk)
                    _aiAskStreaming.value = UiState.Streaming(accumulated.toString())
                }

                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotBlank()) {
                    repository.ensureConversationAndAppendMessage(
                        userId = currentUserId,
                        existingConversationId = convId,
                        title = trimmed.take(60),
                        contextLabel = _aiContextLabel.value,
                        role = "model",
                        content = finalReply,
                        contextSummary = _aiContextLabel.value
                    )
                    _aiAskStreaming.value = UiState.Idle
                } else {
                    _aiAskStreaming.value = UiState.Error("AI returned an empty reply.")
                }
            } catch (e: TimeoutCancellationException) {
                _aiAskStreaming.value = UiState.Error("AI Ask timed out. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Ask failed", e)
                _aiAskStreaming.value = UiState.Error(e.localizedMessage ?: "AI Ask failed.")
            }
        }
    }

    // Integrations
    fun saveAndTestIntegration(provider: SearchProviderId, apiKey: String) {
        viewModelScope.launch {
            _testingProvider.value = provider
            val trimmed = apiKey.trim()
            if (trimmed.isBlank()) {
                integrationStore.disconnectProvider(provider)
                _testingProvider.value = null
                _toastEvents.tryEmit("${provider.displayName} disconnected")
                return@launch
            }
            val testResult = webProbeService.testSearchProviderConnection(provider, trimmed)
            testResult.onSuccess { msg ->
                integrationStore.saveProviderConfig(
                    provider = provider,
                    apiKey = trimmed,
                    status = IntegrationStatus.CONNECTED,
                    message = msg
                )
                _toastEvents.tryEmit(msg)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Connection failed"
                integrationStore.saveProviderConfig(
                    provider = provider,
                    apiKey = trimmed,
                    status = IntegrationStatus.CONNECTION_ERROR,
                    message = msg
                )
                _toastEvents.tryEmit("${provider.displayName}: $msg")
            }
            _testingProvider.value = null
        }
    }

    fun disconnectIntegration(provider: SearchProviderId) {
        viewModelScope.launch {
            integrationStore.disconnectProvider(provider)
            _toastEvents.tryEmit("${provider.displayName} disconnected")
        }
    }

    fun buildIntegrationsList(secrets: LocalIntegrationSecrets): List<IntegrationCardModel> = listOf(
        IntegrationCardModel(
            id = "gemini_ai_logic",
            name = "Firebase AI Logic (${preferences.value.preferredModel})",
            category = "AI Model Provider",
            description = "On-device App Check attested Gemini inference for AI Search, Ranking, Summaries & AI Ask.",
            status = IntegrationStatus.BUILT_IN_ACTIVE,
            isBuiltIn = true,
            keyConfigured = true,
            lastTestMessage = "Active via Firebase App Check"
        ),
        IntegrationCardModel(
            id = "http_probe",
            name = "Live HTTP Domain Probe",
            category = "Website Metadata Service",
            description = "Verifies live HTTP status codes, redirects, and HTML page titles via OkHttp.",
            status = IntegrationStatus.BUILT_IN_ACTIVE,
            isBuiltIn = true,
            keyConfigured = true,
            lastTestMessage = "Ready for real-time URL verification"
        ),
        IntegrationCardModel(
            id = "favicon_resolver",
            name = "Domain Favicon Resolver",
            category = "Logo / Favicon Service",
            description = "Fetches authentic domain favicons without inventing synthetic logos.",
            status = IntegrationStatus.BUILT_IN_ACTIVE,
            isBuiltIn = true,
            keyConfigured = true,
            lastTestMessage = "Active"
        ),
        IntegrationCardModel(
            id = SearchProviderId.TAVILY.name,
            name = SearchProviderId.TAVILY.displayName,
            category = "Web Search API",
            description = SearchProviderId.TAVILY.description,
            status = secrets.tavilyStatus,
            isBuiltIn = false,
            keyConfigured = secrets.tavilyApiKey.isNotBlank(),
            lastTestMessage = secrets.tavilyLastMessage
        ),
        IntegrationCardModel(
            id = SearchProviderId.BRAVE.name,
            name = SearchProviderId.BRAVE.displayName,
            category = "Web Search API",
            description = SearchProviderId.BRAVE.description,
            status = secrets.braveStatus,
            isBuiltIn = false,
            keyConfigured = secrets.braveApiKey.isNotBlank(),
            lastTestMessage = secrets.braveLastMessage
        ),
        IntegrationCardModel(
            id = SearchProviderId.SERPAPI.name,
            name = SearchProviderId.SERPAPI.displayName,
            category = "Web Search API",
            description = SearchProviderId.SERPAPI.description,
            status = secrets.serpStatus,
            isBuiltIn = false,
            keyConfigured = secrets.serpApiKey.isNotBlank(),
            lastTestMessage = secrets.serpLastMessage
        )
    )

    // Preferences
    fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        val next = transform(preferences.value)
        viewModelScope.launch {
            repository.savePreferences(currentUserId, next)
                .onFailure { _toastEvents.tryEmit("Failed to save settings") }
        }
    }

    // Analytics computed strictly from actual app data
    fun computeStats(
        websites: List<WebsiteResource>,
        history: List<SearchHistoryEntry>,
        customCats: List<CustomCategory>
    ): DiscoveryStats {
        val catCounts = mutableMapOf<String, Int>()
        websites.forEach { site ->
            site.categories.forEach { cat ->
                catCounts[cat] = (catCounts[cat] ?: 0) + 1
            }
        }
        val allExplored = (catCounts.keys + customCats.map { it.name }).distinct()
        return DiscoveryStats(
            totalDiscovered = websites.size,
            savedCount = websites.count { it.savedByUser },
            freeCount = websites.count { it.accessEnum == AccessType.FREE },
            limitedCount = websites.count { it.accessEnum == AccessType.LIMITED_ACCESS },
            paidCount = websites.count { it.accessEnum == AccessType.PAID },
            openSourceCount = websites.count { it.openSource },
            verifiedCount = websites.count { it.verificationEnum == VerificationStatus.VERIFIED },
            categoriesExplored = allExplored.size,
            searchesPerformed = history.size,
            topCategories = catCounts.entries
                .sortedByDescending { it.value }
                .take(6)
                .map { it.key to it.value }
        )
    }

    fun allAvailableCategories(customCats: List<CustomCategory>): List<String> =
        (BuiltInCategories.all + customCats.map { it.name }).distinct()

    // Export helpers
    fun exportToJson(websites: List<WebsiteResource>, title: String = "IntelAtlas Export"): String {
        val arr = JSONArray()
        websites.forEach { site ->
            arr.put(
                JSONObject().apply {
                    put("name", site.name)
                    put("url", site.url)
                    put("domain", site.domain)
                    put("accessType", site.accessEnum.displayLabel)
                    put("pricingModel", site.pricingModel)
                    put("categories", JSONArray(site.categories))
                    put("openSource", site.openSource)
                    put("aiPowered", site.aiPowered)
                    put("recommendationScore", site.recommendationScore)
                    put("verificationStatus", site.verificationEnum.displayLabel)
                    put("httpStatusCode", site.httpStatusCode)
                    put("lastCheckedAt", site.lastCheckedAt)
                    put("description", site.description)
                    put("notes", site.notes)
                }
            )
        }
        return JSONObject().apply {
            put("exportTitle", title)
            put("count", websites.size)
            put("websites", arr)
        }.toString(2)
    }

    fun exportToCsv(websites: List<WebsiteResource>): String {
        val sb = StringBuilder()
        sb.appendLine("Name,Domain,URL,Access,OpenSource,AIPowered,Score,Categories,VerifiedStatus,HTTP,LastChecked")
        websites.forEach { s ->
            sb.appendLine(
                listOf(
                    csvEsc(s.name),
                    csvEsc(s.domain),
                    csvEsc(s.url),
                    csvEsc(s.accessEnum.displayLabel),
                    s.openSource.toString(),
                    s.aiPowered.toString(),
                    s.recommendationScore.toString(),
                    csvEsc(s.categories.joinToString("; ")),
                    csvEsc(s.verificationEnum.displayLabel),
                    s.httpStatusCode.toString(),
                    csvEsc(s.lastCheckedAt)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    private fun csvEsc(v: String): String = "\"${v.replace("\"", "\"\"")}\""

    private fun parseOperatorsIntoFilters(query: String, base: SearchFilterState): SearchFilterState {
        val lower = query.lowercase()
        var next = base
        if ("free:" in lower) next = next.copy(accessTypes = next.accessTypes + AccessType.FREE)
        if ("oss:" in lower || "opensource:" in lower) next = next.copy(openSourceOnly = true)
        if ("no-login:" in lower || "noreg:" in lower) next = next.copy(noRegistrationOnly = true)
        if ("ai:" in lower) next = next.copy(aiPoweredOnly = true)
        return next
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "IntelAtlasVM"
    }
}
