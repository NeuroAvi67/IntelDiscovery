package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.UiState
import com.example.data.local.LocalIntegrationSecrets
import com.example.data.model.IntegrationCardModel
import com.example.data.model.IntegrationStatus
import com.example.data.model.ResearchCollection
import com.example.data.model.SearchHistoryEntry
import com.example.data.model.SearchProviderId
import com.example.data.model.UserPreferences
import com.example.data.model.WebsiteResource
import com.example.ui.components.SiteFavicon
import com.example.ui.theme.AmberLimited
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.RosePaid
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.VioletAi

@Composable
fun HistoryScreen(
    history: List<SearchHistoryEntry>,
    visitedWebsites: List<WebsiteResource>,
    onRepeatSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearAll: () -> Unit,
    onVisitWebsite: (WebsiteResource) -> Unit
) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(history, filter) {
        val q = filter.trim().lowercase()
        if (q.isBlank()) history
        else history.filter { it.query.lowercase().contains(q) || it.filtersSummary.lowercase().contains(q) }
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
                Text("Search & Visit History", style = MaterialTheme.typography.headlineSmall)
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearAll, modifier = Modifier.testTag("clear_history_button")) {
                        Icon(Icons.Filled.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = { Text("Search previous queries...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (visitedWebsites.isNotEmpty()) {
            item {
                Text("Visited Websites", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    visitedWebsites.sortedByDescending { it.visitCount }.take(10).forEach { site ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable { onVisitWebsite(site) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SiteFavicon(site.faviconUrl(), site.name, Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("${site.name} (${site.visitCount})", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    "No search history yet. Run an AI Search to record queries and discovered results.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(filtered, key = { it.id }) { entry ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(entry.query, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${entry.resultCount} sites • ${entry.searchSource} • ${entry.filtersSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onRepeatSearch(entry.query) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Repeat search", tint = ElectricBlue)
                        }
                        IconButton(onClick = { onDeleteHistory(entry.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete history item", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionsScreen(
    collections: List<ResearchCollection>,
    allWebsites: List<WebsiteResource>,
    onCreateCollection: (String, String, String) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onRemoveWebsiteFromCollection: (ResearchCollection, String) -> Unit,
    onVisitWebsite: (WebsiteResource) -> Unit,
    onExportCollection: (ResearchCollection, List<WebsiteResource>, Boolean) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

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
                    Text("Research Collections", style = MaterialTheme.typography.headlineSmall)
                    Text("Organize, export, and share intelligence dossiers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.testTag("create_collection_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New")
                }
            }
        }

        if (collections.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No collections created yet. Create a dossier or tap the folder icon on any website card.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(collections, key = { it.id }) { col ->
                val members = remember(col.websiteIds, allWebsites) {
                    allWebsites.filter { it.id in col.websiteIds }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(col.name, style = MaterialTheme.typography.titleMedium)
                                if (col.description.isNotBlank()) {
                                    Text(col.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row {
                                IconButton(onClick = { onExportCollection(col, members, true) }) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share collection JSON", tint = TacticalCyan)
                                }
                                IconButton(onClick = { onDeleteCollection(col.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete collection", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        if (members.isEmpty()) {
                            Text("Empty collection — add websites from Search or Explorer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            members.forEach { site ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onVisitWebsite(site) }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SiteFavicon(site.faviconUrl(), site.name, Modifier.size(26.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(site.name, style = MaterialTheme.typography.titleSmall)
                                        Text(site.domain, style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                                    }
                                    IconButton(onClick = { onRemoveWebsiteFromCollection(col, site.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove from collection", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Research Collection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Collection Name") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        onCreateCollection(name, desc, "#3B82F6")
                        showCreateDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ComparisonScreen(
    allWebsites: List<WebsiteResource>,
    compareIds: List<String>,
    comparisonState: UiState<String>,
    onToggleCompare: (String) -> Unit,
    onClearCompare: () -> Unit,
    onGenerateComparison: (List<WebsiteResource>, String) -> Unit
) {
    val selected = remember(allWebsites, compareIds) {
        compareIds.mapNotNull { id -> allWebsites.firstOrNull { it.id == id } }
    }
    var goal by remember { mutableStateOf("") }

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
                    Text("Comparison Engine", style = MaterialTheme.typography.headlineSmall)
                    Text("Select 2–4 websites to compare side-by-side", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (selected.isNotEmpty()) {
                    TextButton(onClick = onClearCompare) { Text("Clear") }
                }
            }
        }

        // Quick selector strip
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allWebsites.take(15).forEach { site ->
                    FilterChip(
                        selected = site.id in compareIds,
                        onClick = { onToggleCompare(site.id) },
                        label = { Text(site.name) }
                    )
                }
            }
        }

        if (selected.size < 2) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Pick at least 2 websites above (or tap the Compare button on any website card) to build the intelligence comparison matrix.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            // Side-by-side horizontally scrollable comparison table
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)) {
                        CompareRow(
                            label = "Website",
                            values = selected.map { "${it.name}\n(${it.domain})" },
                            isHeader = true
                        )
                        CompareRow("Score", selected.map { "${it.recommendationScore}/100" })
                        CompareRow("Pricing", selected.map { "${it.accessEnum.displayLabel}\n${it.pricingModel}" })
                        CompareRow("Free Tier", selected.map { it.freeTierLimitations.ifBlank { "Check site" } })
                        CompareRow("AI Features", selected.map { if (it.aiPowered) "Yes" else "No" })
                        CompareRow("Functionality", selected.map { it.purpose.ifBlank { it.description }.take(110) })
                        CompareRow("Registration", selected.map { it.registrationEnum.displayLabel })
                        CompareRow("Open Source", selected.map { if (it.openSource) "Yes" else "No" })
                        CompareRow("Limitations", selected.map { it.disadvantages.joinToString("; ").ifBlank { "None noted" } })
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("Your specific requirement (optional)") },
                    placeholder = { Text("e.g. Zero budget, beginner-friendly, no registration...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onGenerateComparison(selected, goal) },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletAi),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_ai_comparison_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Generate AI Comparison Summary")
                }
            }

            when (comparisonState) {
                is UiState.Loading -> {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Synthesizing comparative brief...")
                        }
                    }
                }
                is UiState.Success -> {
                    item {
                        Surface(
                            color = VioletAi.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, VioletAi.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("AI COMPARISON VERDICT", style = MaterialTheme.typography.labelMedium, color = TacticalCyan)
                                Text(comparisonState.data, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    item { Text(comparisonState.message, color = MaterialTheme.colorScheme.error) }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun CompareRow(label: String, values: List<String>, isHeader: Boolean = false) {
    Column {
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TacticalCyan,
                modifier = Modifier.width(105.dp).padding(end = 8.dp)
            )
            values.forEach { v ->
                Text(
                    text = v,
                    style = if (isHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(165.dp).padding(end = 12.dp)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun IntegrationsScreen(
    integrations: List<IntegrationCardModel>,
    secrets: LocalIntegrationSecrets,
    testingProvider: SearchProviderId?,
    onSaveAndTestProvider: (SearchProviderId, String) -> Unit,
    onDisconnectProvider: (SearchProviderId) -> Unit
) {
    var editingProvider by remember { mutableStateOf<SearchProviderId?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Integrations Management", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Connect optional live web search APIs. API keys are stored locally on device and never hardcoded in source.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(integrations, key = { it.id }) { item ->
            val statusColor = when (item.status) {
                IntegrationStatus.BUILT_IN_ACTIVE, IntegrationStatus.CONNECTED -> EmeraldFree
                IntegrationStatus.CONFIGURATION_REQUIRED -> AmberLimited
                IntegrationStatus.CONNECTION_ERROR -> RosePaid
                IntegrationStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val providerEnum = SearchProviderId.entries.firstOrNull { it.name == item.id }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                            Spacer(Modifier.width(6.dp))
                            Text(item.status.label, style = MaterialTheme.typography.labelSmall, color = statusColor)
                        }
                    }

                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (item.lastTestMessage.isNotBlank()) {
                        Text(
                            item.lastTestMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!item.isBuiltIn && providerEnum != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { editingProvider = providerEnum },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                            ) {
                                Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (item.keyConfigured) "Update & Test Key" else "Configure Key")
                            }
                            if (item.keyConfigured) {
                                OutlinedButton(onClick = { onDisconnectProvider(providerEnum) }) {
                                    Text("Disconnect")
                                }
                            }
                            if (testingProvider == providerEnum) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    editingProvider?.let { provider ->
        var keyInput by remember(provider) { mutableStateOf(secrets.keyFor(provider)) }
        AlertDialog(
            onDismissRequest = { editingProvider = null },
            title = { Text("Configure ${provider.displayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter your ${provider.displayName} key. IntelAtlas will run a live verification probe before saving.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveAndTestProvider(provider, keyInput)
                    editingProvider = null
                }) { Text("Save & Test") }
            },
            dismissButton = {
                TextButton(onClick = { editingProvider = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsScreen(
    userEmail: String,
    userName: String,
    preferences: UserPreferences,
    onUpdatePreferences: ((UserPreferences) -> UserPreferences) -> Unit,
    onExportAllJson: () -> Unit,
    onExportAllCsv: () -> Unit,
    onSignOut: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Settings & Workspace", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ACCOUNT PROFILE", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                    Text(userName.ifBlank { "OSINT Researcher" }, style = MaterialTheme.typography.titleMedium)
                    Text(userEmail.ifBlank { "Google Authenticated" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dark OSINT Theme", style = MaterialTheme.typography.titleSmall)
                            Text("Toggle between Obsidian Dark and Intel Daylight", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = preferences.darkTheme,
                            onCheckedChange = { checked -> onUpdatePreferences { it.copy(darkTheme = checked) } }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Save Discovered Sites", style = MaterialTheme.typography.titleSmall)
                            Text("Store AI Search discoveries in your searchable library", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = preferences.autoSaveDiscovered,
                            onCheckedChange = { checked -> onUpdatePreferences { it.copy(autoSaveDiscovered = checked) } }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Probe Live HTTP Links", style = MaterialTheme.typography.titleSmall)
                            Text("Verify domain reachability automatically during AI Search", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = preferences.autoVerifyLinks,
                            onCheckedChange = { checked -> onUpdatePreferences { it.copy(autoVerifyLinks = checked) } }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Preferred Gemini Model", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("gemini-3.8-flash", "gemini-3.5-flash-lite").forEach { model ->
                                FilterChip(
                                    selected = preferences.preferredModel == model,
                                    onClick = { onUpdatePreferences { it.copy(preferredModel = model) } },
                                    label = { Text(model) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("DATA PORTABILITY", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onExportAllJson, modifier = Modifier.weight(1f)) {
                            Text("Export JSON")
                        }
                        OutlinedButton(onClick = onExportAllCsv, modifier = Modifier.weight(1f)) {
                            Text("Export CSV")
                        }
                    }
                }
            }
        }
    }
}
