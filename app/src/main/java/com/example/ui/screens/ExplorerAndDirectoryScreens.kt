package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.UiState
import com.example.data.model.AccessType
import com.example.data.model.CustomCategory
import com.example.data.model.SortOrder
import com.example.data.model.SourceType
import com.example.data.model.WebsiteResource
import com.example.ui.components.WebsiteIntelligenceCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.TacticalCyan

@Composable
fun ExplorerScreen(
    websites: List<WebsiteResource>,
    categories: List<String>,
    compareIds: List<String>,
    onVisit: (WebsiteResource) -> Unit,
    onToggleSave: (WebsiteResource) -> Unit,
    onAddToCollection: (WebsiteResource) -> Unit,
    onToggleCompare: (WebsiteResource) -> Unit,
    onAskAi: (WebsiteResource) -> Unit,
    onVerifyLive: (WebsiteResource) -> Unit,
    onNavigateToFreeTools: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Featured") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var sortOrder by remember { mutableStateOf(SortOrder.RELEVANCE) }

    val displayed = remember(websites, selectedTab, selectedCategory, sortOrder) {
        val base = when (selectedTab) {
            "Curated" -> websites.filter { it.sourceEnum == SourceType.CURATED }
            "AI Recommended" -> websites.filter { it.recommendationScore >= 85 }
            "Open Source" -> websites.filter { it.openSource }
            "Free" -> websites.filter { it.accessEnum == AccessType.FREE }
            else -> websites
        }.filter { site ->
            selectedCategory == null || site.categories.any { it.equals(selectedCategory, ignoreCase = true) }
        }
        when (sortOrder) {
            SortOrder.RELEVANCE -> base.sortedByDescending { it.recommendationScore }
            SortOrder.RECENTLY_ADDED -> base.sortedByDescending { it.createdAt?.seconds ?: 0L }
            SortOrder.CATEGORY -> base.sortedBy { it.categories.firstOrNull().orEmpty() }
            SortOrder.NAME -> base.sortedBy { it.name.lowercase() }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Website Explorer", style = MaterialTheme.typography.headlineSmall)
                    OutlinedButton(onClick = onNavigateToFreeTools) {
                        Text("Free Alternatives")
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Featured", "AI Recommended", "Curated", "Open Source", "Free").forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortOrder.entries.forEach { order ->
                        FilterChip(
                            selected = sortOrder == order,
                            onClick = { sortOrder = order },
                            label = { Text(order.label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All Categories") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        }

        items(displayed, key = { it.id }) { site ->
            WebsiteIntelligenceCard(
                website = site,
                isCompared = site.id in compareIds,
                onVisit = { onVisit(site) },
                onToggleSave = { onToggleSave(site) },
                onAddToCollection = { onAddToCollection(site) },
                onToggleCompare = { onToggleCompare(site) },
                onAskAi = { onAskAi(site) },
                onVerifyLive = { onVerifyLive(site) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    allCategories: List<String>,
    customCategories: List<CustomCategory>,
    websites: List<WebsiteResource>,
    selectedCategory: String?,
    compareIds: List<String>,
    onSelectCategory: (String?) -> Unit,
    onCreateCustomCategory: (String, String) -> Unit,
    onDeleteCustomCategory: (String) -> Unit,
    onVisit: (WebsiteResource) -> Unit,
    onToggleSave: (WebsiteResource) -> Unit,
    onAddToCollection: (WebsiteResource) -> Unit,
    onToggleCompare: (WebsiteResource) -> Unit,
    onAskAi: (WebsiteResource) -> Unit,
    onVerifyLive: (WebsiteResource) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    val matchingSites = remember(websites, selectedCategory) {
        if (selectedCategory == null) emptyList()
        else websites.filter { s -> s.categories.any { it.equals(selectedCategory, ignoreCase = true) } }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Intelligence Categories", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${allCategories.size} categories across ${websites.size} resources",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.testTag("create_category_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New")
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allCategories.forEach { cat ->
                    val count = websites.count { s -> s.categories.any { it.equals(cat, ignoreCase = true) } }
                    val customObj = customCategories.firstOrNull { it.name.equals(cat, ignoreCase = true) }
                    val selected = selectedCategory == cat
                    Surface(
                        color = if (selected) ElectricBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (selected) ElectricBlue else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectCategory(if (selected) null else cat) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(cat, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(6.dp))
                            Text("$count", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                            if (customObj != null) {
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onDeleteCustomCategory(customObj.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete category",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedCategory != null) {
            item {
                Text(
                    text = "$selectedCategory (${matchingSites.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (matchingSites.isEmpty()) {
                item {
                    Text(
                        "No websites categorized under $selectedCategory yet. Use AI Search to discover resources in this domain.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(matchingSites, key = { it.id }) { site ->
                    WebsiteIntelligenceCard(
                        website = site,
                        isCompared = site.id in compareIds,
                        onVisit = { onVisit(site) },
                        onToggleSave = { onToggleSave(site) },
                        onAddToCollection = { onAddToCollection(site) },
                        onToggleCompare = { onToggleCompare(site) },
                        onAskAi = { onAskAi(site) },
                        onVerifyLive = { onVerifyLive(site) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        var catName by remember { mutableStateOf("") }
        var catDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Custom Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("Category Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = catDesc,
                        onValueChange = { catDesc = it },
                        label = { Text("Description (optional)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            onCreateCustomCategory(catName, catDesc)
                            showCreateDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun FreeToolsScreen(
    websites: List<WebsiteResource>,
    categories: List<String>,
    freeAltState: UiState<List<WebsiteResource>>,
    compareIds: List<String>,
    onDiscoverFreeAlternatives: (String) -> Unit,
    onVisit: (WebsiteResource) -> Unit,
    onToggleSave: (WebsiteResource) -> Unit,
    onAddToCollection: (WebsiteResource) -> Unit,
    onToggleCompare: (WebsiteResource) -> Unit,
    onAskAi: (WebsiteResource) -> Unit,
    onVerifyLive: (WebsiteResource) -> Unit
) {
    var paidToolQuery by remember { mutableStateOf("") }
    var includeLimited by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val freeSites = remember(websites, includeLimited, selectedCategory) {
        websites.filter { site ->
            val accessOk = site.accessEnum == AccessType.FREE ||
                (includeLimited && site.accessEnum == AccessType.LIMITED_ACCESS)
            val catOk = selectedCategory == null ||
                site.categories.any { it.equals(selectedCategory, ignoreCase = true) }
            accessOk && catOk
        }.sortedByDescending {
            (if (it.accessEnum == AccessType.FREE) 20 else 0) + it.recommendationScore
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                color = EmeraldFree.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmeraldFree.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("FREE TOOLS & ALTERNATIVES FINDER", style = MaterialTheme.typography.labelMedium, color = EmeraldFree)
                    Text(
                        "Paid trials are never labeled as completely free. Limited-access tiers explicitly display their caps.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = paidToolQuery,
                            onValueChange = { paidToolQuery = it },
                            placeholder = { Text("Paid tool to replace (e.g. Maltego, Ahrefs)...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onDiscoverFreeAlternatives(paidToolQuery) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldFree)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "Find free alternatives")
                        }
                    }
                }
            }
        }

        if (freeAltState is UiState.Loading) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Discovering free open-source alternatives...")
                }
            }
        } else if (freeAltState is UiState.Success) {
            item {
                Text(
                    "AI-Discovered Free Alternatives (${freeAltState.data.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TacticalCyan
                )
            }
            items(freeAltState.data, key = { "alt_${it.id}" }) { site ->
                WebsiteIntelligenceCard(
                    website = site,
                    isCompared = site.id in compareIds,
                    onVisit = { onVisit(site) },
                    onToggleSave = { onToggleSave(site) },
                    onAddToCollection = { onAddToCollection(site) },
                    onToggleCompare = { onToggleCompare(site) },
                    onAskAi = { onAskAi(site) },
                    onVerifyLive = { onVerifyLive(site) }
                )
            }
        } else if (freeAltState is UiState.Error) {
            item {
                Text(freeAltState.message, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !includeLimited,
                    onClick = { includeLimited = !includeLimited },
                    label = { Text("100% Free Only") }
                )
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All Categories") }
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        item {
            Text("Directory (${freeSites.size})", style = MaterialTheme.typography.titleMedium)
        }

        items(freeSites, key = { it.id }) { site ->
            WebsiteIntelligenceCard(
                website = site,
                isCompared = site.id in compareIds,
                onVisit = { onVisit(site) },
                onToggleSave = { onToggleSave(site) },
                onAddToCollection = { onAddToCollection(site) },
                onToggleCompare = { onToggleCompare(site) },
                onAskAi = { onAskAi(site) },
                onVerifyLive = { onVerifyLive(site) }
            )
        }
    }
}

@Composable
fun SavedWebsitesScreen(
    websites: List<WebsiteResource>,
    addUrlInProgress: Boolean,
    compareIds: List<String>,
    onAddCustomUrl: (String) -> Unit,
    onUpdateMetadata: (WebsiteResource, String, String, String, String) -> Unit,
    onDeleteWebsite: (WebsiteResource) -> Unit,
    onVisit: (WebsiteResource) -> Unit,
    onToggleSave: (WebsiteResource) -> Unit,
    onAddToCollection: (WebsiteResource) -> Unit,
    onToggleCompare: (WebsiteResource) -> Unit,
    onAskAi: (WebsiteResource) -> Unit,
    onVerifyLive: (WebsiteResource) -> Unit,
    onExport: (List<WebsiteResource>, Boolean) -> Unit
) {
    var libraryFilter by remember { mutableStateOf("") }
    var showSavedOnly by remember { mutableStateOf(true) }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var editingSite by remember { mutableStateOf<WebsiteResource?>(null) }

    val filtered = remember(websites, libraryFilter, showSavedOnly) {
        websites.filter { site ->
            val savedOk = !showSavedOnly || site.savedByUser
            val q = libraryFilter.trim().lowercase()
            val qOk = q.isBlank() ||
                site.name.lowercase().contains(q) ||
                site.domain.lowercase().contains(q) ||
                site.notes.lowercase().contains(q) ||
                site.tags.any { it.lowercase().contains(q) } ||
                site.categories.any { it.lowercase().contains(q) }
            savedOk && qOk
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Website Library", style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onExport(filtered, true) }) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("JSON")
                        }
                        Button(
                            onClick = { showAddUrlDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.testTag("add_custom_url_button")
                        ) {
                            Icon(Icons.Filled.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add URL")
                        }
                    }
                }

                if (addUrlInProgress) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Probing URL & indexing with Gemini...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedTextField(
                    value = libraryFilter,
                    onValueChange = { libraryFilter = it },
                    placeholder = { Text("Search saved websites, notes, tags, or domains...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = showSavedOnly,
                        onClick = { showSavedOnly = true },
                        label = { Text("User Saved (${websites.count { it.savedByUser }})") }
                    )
                    FilterChip(
                        selected = !showSavedOnly,
                        onClick = { showSavedOnly = false },
                        label = { Text("All Discovered (${websites.size})") }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showSavedOnly) {
                            "No bookmarked websites yet. Tap the bookmark icon on any website card or switch to 'All Discovered'."
                        } else {
                            "No websites match \"$libraryFilter\"."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { site ->
                WebsiteIntelligenceCard(
                    website = site,
                    isCompared = site.id in compareIds,
                    onVisit = { onVisit(site) },
                    onToggleSave = { onToggleSave(site) },
                    onAddToCollection = { onAddToCollection(site) },
                    onToggleCompare = { onToggleCompare(site) },
                    onAskAi = { onAskAi(site) },
                    onVerifyLive = { onVerifyLive(site) },
                    onEditNotes = { editingSite = site },
                    onDelete = { onDeleteWebsite(site) }
                )
            }
        }
    }

    if (showAddUrlDialog) {
        var urlInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddUrlDialog = false },
            title = { Text("Index Website URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "IntelAtlas will probe live HTTP reachability, extract metadata, and auto-categorize the resource.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("https://example.org") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            onAddCustomUrl(urlInput)
                            showAddUrlDialog = false
                        }
                    }
                ) { Text("Verify & Index") }
            },
            dismissButton = {
                TextButton(onClick = { showAddUrlDialog = false }) { Text("Cancel") }
            }
        )
    }

    editingSite?.let { site ->
        var desc by remember(site) { mutableStateOf(site.description) }
        var notes by remember(site) { mutableStateOf(site.notes) }
        var tagsCsv by remember(site) { mutableStateOf(site.tags.joinToString(", ")) }
        var catsCsv by remember(site) { mutableStateOf(site.categories.joinToString(", ")) }

        AlertDialog(
            onDismissRequest = { editingSite = null },
            title = { Text("Edit ${site.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        maxLines = 3
                    )
                    OutlinedTextField(
                        value = catsCsv,
                        onValueChange = { catsCsv = it },
                        label = { Text("Categories (comma-separated)") }
                    )
                    OutlinedTextField(
                        value = tagsCsv,
                        onValueChange = { tagsCsv = it },
                        label = { Text("Tags (comma-separated)") }
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Personal Intelligence Notes") },
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateMetadata(site, notes, tagsCsv, catsCsv, desc)
                        editingSite = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingSite = null }) { Text("Cancel") }
            }
        )
    }
}
