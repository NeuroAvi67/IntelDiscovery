package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.core.UiState
import com.example.data.model.AccessType
import com.example.data.model.SearchFilterState
import com.example.data.model.SortOrder
import com.example.data.model.WebsiteResource
import com.example.ui.components.WebsiteIntelligenceCard
import com.example.ui.theme.AmberLimited
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.RosePaid
import com.example.ui.theme.TacticalCyan

@Composable
fun AiSearchScreen(
    query: String,
    filterState: SearchFilterState,
    searchState: UiState<List<WebsiteResource>>,
    intentSummary: String,
    pipelineStage: String,
    categories: List<String>,
    compareIds: List<String>,
    onQueryChange: (String) -> Unit,
    onExecuteSearch: (String?) -> Unit,
    onUpdateFilter: ((SearchFilterState) -> SearchFilterState) -> Unit,
    applyFilters: (List<WebsiteResource>, SearchFilterState) -> List<WebsiteResource>,
    onVisitWebsite: (WebsiteResource) -> Unit,
    onToggleSave: (WebsiteResource) -> Unit,
    onAddToCollection: (WebsiteResource) -> Unit,
    onToggleCompare: (WebsiteResource) -> Unit,
    onAskAiAbout: (WebsiteResource) -> Unit,
    onVerifyLive: (WebsiteResource) -> Unit,
    onExportResults: (List<WebsiteResource>, Boolean) -> Unit
) {
    var showAdvancedFilters by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Header & Operators
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Search websites, datasets, AI tools, OSINT...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onExecuteSearch(null) }),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_search_query_input")
                    )

                    Button(
                        onClick = { onExecuteSearch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("ai_search_submit_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Search")
                    }
                }

                // Operator & Quick Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = showAdvancedFilters || filterState.activeCount() > 0,
                        onClick = { showAdvancedFilters = !showAdvancedFilters },
                        leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = {
                            val cnt = filterState.activeCount()
                            Text(if (cnt > 0) "Filters ($cnt)" else "Filters")
                        }
                    )
                    FilterChip(
                        selected = AccessType.FREE in filterState.accessTypes,
                        onClick = {
                            onUpdateFilter { f ->
                                val next = f.accessTypes.toMutableSet()
                                if (!next.add(AccessType.FREE)) next.remove(AccessType.FREE)
                                f.copy(accessTypes = next)
                            }
                        },
                        label = { Text("Free", color = EmeraldFree) }
                    )
                    FilterChip(
                        selected = AccessType.LIMITED_ACCESS in filterState.accessTypes,
                        onClick = {
                            onUpdateFilter { f ->
                                val next = f.accessTypes.toMutableSet()
                                if (!next.add(AccessType.LIMITED_ACCESS)) next.remove(AccessType.LIMITED_ACCESS)
                                f.copy(accessTypes = next)
                            }
                        },
                        label = { Text("Limited Access", color = AmberLimited) }
                    )
                    FilterChip(
                        selected = AccessType.PAID in filterState.accessTypes,
                        onClick = {
                            onUpdateFilter { f ->
                                val next = f.accessTypes.toMutableSet()
                                if (!next.add(AccessType.PAID)) next.remove(AccessType.PAID)
                                f.copy(accessTypes = next)
                            }
                        },
                        label = { Text("Paid", color = RosePaid) }
                    )
                    FilterChip(
                        selected = filterState.openSourceOnly,
                        onClick = { onUpdateFilter { it.copy(openSourceOnly = !it.openSourceOnly) } },
                        label = { Text("Open Source") }
                    )
                    FilterChip(
                        selected = filterState.aiPoweredOnly,
                        onClick = { onUpdateFilter { it.copy(aiPoweredOnly = !it.aiPoweredOnly) } },
                        label = { Text("AI-Powered") }
                    )
                    FilterChip(
                        selected = filterState.noRegistrationOnly,
                        onClick = { onUpdateFilter { it.copy(noRegistrationOnly = !it.noRegistrationOnly) } },
                        label = { Text("No Registration") }
                    )
                }

                if (showAdvancedFilters) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = filterState.beginnerFriendlyOnly,
                                onClick = { onUpdateFilter { it.copy(beginnerFriendlyOnly = !it.beginnerFriendlyOnly) } },
                                label = { Text("Beginner Friendly") }
                            )
                            FilterChip(
                                selected = filterState.professionalOnly,
                                onClick = { onUpdateFilter { it.copy(professionalOnly = !it.professionalOnly) } },
                                label = { Text("Professional") }
                            )
                            FilterChip(
                                selected = filterState.recentlyDiscoveredOnly,
                                onClick = { onUpdateFilter { it.copy(recentlyDiscoveredOnly = !it.recentlyDiscoveredOnly) } },
                                label = { Text("Recently Discovered") }
                            )
                            SortOrder.entries.forEach { order ->
                                FilterChip(
                                    selected = filterState.sortOrder == order,
                                    onClick = { onUpdateFilter { it.copy(sortOrder = order) } },
                                    label = { Text("Sort: ${order.label}") }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = filterState.selectedCategory == null,
                                onClick = { onUpdateFilter { it.copy(selectedCategory = null) } },
                                label = { Text("All Categories") }
                            )
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = filterState.selectedCategory == cat,
                                    onClick = {
                                        onUpdateFilter {
                                            it.copy(selectedCategory = if (it.selectedCategory == cat) null else cat)
                                        }
                                    },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("free:", "oss:", "no-login:", "ai:").forEach { op ->
                                AssistChip(
                                    onClick = {
                                        if (op !in query) onQueryChange("$op $query".trim())
                                    },
                                    label = { Text("Op $op", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Results Pane
        when (searchState) {
            is UiState.Idle -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("AI DISCOVERY ENGINE READY", style = MaterialTheme.typography.labelMedium, color = TacticalCyan)
                                Text(
                                    "Describe what you need in natural language. IntelAtlas analyzes intent, queries configured search APIs + Gemini 3.8 Flash, probes live HTTP reachability, ranks top picks, and saves discoveries automatically.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text("TRY AN INTELLIGENCE QUERY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                listOf(
                                    "Find the best free websites for analysing public government datasets",
                                    "Free cybersecurity learning platforms with hands-on labs",
                                    "Best OSINT tools for researching companies and corporate registries",
                                    "Free open-source alternatives to expensive design software"
                                ).forEach { sample ->
                                    OutlinedButton(
                                        onClick = { onExecuteSearch(sample) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(sample, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is UiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(24.dp))
                    CircularProgressIndicator(color = TacticalCyan)
                    Text(
                        text = pipelineStage.ifBlank { "Discovering & verifying intelligence sources..." },
                        style = MaterialTheme.typography.titleSmall,
                        color = TacticalCyan
                    )
                    // Skeleton Loader Cards
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    RoundedCornerShape(16.dp)
                                )
                        )
                    }
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Search Error", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                            Text(searchState.message, style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { onExecuteSearch(null) }) { Text("Retry Search") }
                        }
                    }
                }
            }
            is UiState.Success -> {
                val filtered = remember(searchState.data, filterState) {
                    applyFilters(searchState.data, filterState)
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Surface(
                            color = ElectricBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ElectricBlue.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${filtered.size} RANKED RECOMMENDATIONS",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TacticalCyan
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        AssistChip(
                                            onClick = { onExportResults(filtered, true) },
                                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                            label = { Text("JSON") }
                                        )
                                        AssistChip(
                                            onClick = { onExportResults(filtered, false) },
                                            label = { Text("CSV") }
                                        )
                                    }
                                }
                                if (intentSummary.isNotBlank()) {
                                    Text(intentSummary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "No results match the active filters. Try clearing one or more filter chips.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { site ->
                            WebsiteIntelligenceCard(
                                website = site,
                                isCompared = site.id in compareIds,
                                onVisit = { onVisitWebsite(site) },
                                onToggleSave = { onToggleSave(site) },
                                onAddToCollection = { onAddToCollection(site) },
                                onToggleCompare = { onToggleCompare(site) },
                                onAskAi = { onAskAiAbout(site) },
                                onVerifyLive = { onVerifyLive(site) }
                            )
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}
