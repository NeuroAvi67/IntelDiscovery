package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AccessType
import com.example.data.model.ResearchCollection
import com.example.data.model.WebsiteResource
import com.example.ui.AppRoute
import com.example.ui.DiscoveryStats
import com.example.ui.components.AccessDistributionChartCard
import com.example.ui.components.CategoryBreakdownChartCard
import com.example.ui.components.SiteFavicon
import com.example.ui.components.WebsiteIntelligenceCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.VioletAi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    userDisplayName: String,
    stats: DiscoveryStats,
    websites: List<WebsiteResource>,
    collections: List<ResearchCollection>,
    compareIds: List<String>,
    onQuickAiSearch: (String) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSelectCategory: (String) -> Unit,
    onVisitWebsite: (WebsiteResource) -> Unit,
    onToggleSave: (WebsiteResource) -> Unit,
    onAddToCollection: (WebsiteResource) -> Unit,
    onToggleCompare: (WebsiteResource) -> Unit,
    onAskAiAbout: (WebsiteResource) -> Unit,
    onVerifyLive: (WebsiteResource) -> Unit
) {
    var quickQuery by remember { mutableStateOf("") }
    val recommended = remember(websites) {
        websites.sortedByDescending { it.recommendationScore }.take(3)
    }
    val freeHighlight = remember(websites) {
        websites.filter { it.accessEnum == AccessType.FREE }.take(5)
    }
    val recentlyVisited = remember(websites) {
        websites.filter { it.visitCount > 0 }.sortedByDescending { it.visitCount }.take(4)
    }
    val popularCategories = remember(stats) {
        stats.topCategories.ifEmpty {
            listOf("OSINT" to 4, "Cybersecurity" to 3, "Government Data" to 2, "Artificial Intelligence" to 2)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Hero Welcome & Global AI Search Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_osint_hero),
                        contentDescription = "IntelAtlas Intelligence Workspace",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xB8070D19), Color(0xF2070D19))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "WELCOME BACK, ${userDisplayName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TacticalCyan
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "OSINT Website Intelligence Hub",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White
                                )
                            }
                            Surface(
                                color = EmeraldFree.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.border(1.dp, EmeraldFree.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            ) {
                                Text(
                                    text = "AI ONLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldFree,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = quickQuery,
                            onValueChange = { quickQuery = it },
                            placeholder = {
                                Text("Ask AI to discover websites (e.g. free government datasets)...")
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = TacticalCyan)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (quickQuery.isNotBlank()) onQuickAiSearch(quickQuery)
                            }),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xCC0D1526), RoundedCornerShape(14.dp))
                                .testTag("dashboard_ai_search_input")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (quickQuery.isNotBlank()) onQuickAiSearch(quickQuery)
                                    else onNavigate(AppRoute.AI_SEARCH)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("dashboard_ai_search_button")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Run AI Search")
                            }
                            TextButton(onClick = { onNavigate(AppRoute.FREE_TOOLS) }) {
                                Text("Free Tools", color = TacticalCyan)
                            }
                        }

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Best free tools for researching companies",
                                "Free websites for analyzing government datasets",
                                "Open-source cybersecurity learning platforms",
                                "AI tools for video generation"
                            ).forEach { suggestion ->
                                AssistChip(
                                    onClick = { onQuickAiSearch(suggestion) },
                                    label = { Text(suggestion, style = MaterialTheme.typography.bodySmall, color = Color.White) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // KPI Discovery Statistics Row
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatKpiCard("Discovered", "${stats.totalDiscovered}", Icons.Filled.Public, ElectricBlue)
                StatKpiCard("Saved", "${stats.savedCount}", Icons.Filled.Bookmark, EmeraldFree)
                StatKpiCard("Free Tools", "${stats.freeCount}", Icons.Filled.Verified, TacticalCyan)
                StatKpiCard("Categories", "${stats.categoriesExplored}", Icons.Filled.Category, VioletAi)
                StatKpiCard("Searches", "${stats.searchesPerformed}", Icons.Filled.History, ElectricBlue)
            }
        }

        // Analytics Charts (Responsive 2-column on wide screens, stacked on phones)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 680.dp) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AccessDistributionChartCard(stats = stats, modifier = Modifier.weight(1f))
                        CategoryBreakdownChartCard(
                            topCategories = stats.topCategories,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccessDistributionChartCard(stats = stats, modifier = Modifier.fillMaxWidth())
                        CategoryBreakdownChartCard(
                            topCategories = stats.topCategories,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Popular Categories Quick Access
        item {
            SectionHeader(
                title = "Popular Intelligence Categories",
                actionLabel = "All Categories",
                onAction = { onNavigate(AppRoute.CATEGORIES) }
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                popularCategories.forEach { (cat, count) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { onSelectCategory(cat) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(cat, style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalCyan
                            )
                        }
                    }
                }
            }
        }

        // Free Tools Spotlight
        item {
            SectionHeader(
                title = "Verified Free Tools",
                actionLabel = "Directory",
                onAction = { onNavigate(AppRoute.FREE_TOOLS) }
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                freeHighlight.forEach { site ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .width(210.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .clickable { onVisitWebsite(site) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SiteFavicon(site.faviconUrl(), site.name, Modifier.size(30.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(site.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text("FREE", style = MaterialTheme.typography.labelSmall, color = EmeraldFree)
                                }
                            }
                            Text(
                                text = site.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // Recently Visited Resources
        if (recentlyVisited.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recently Visited Resources",
                    actionLabel = "History",
                    onAction = { onNavigate(AppRoute.HISTORY) }
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    recentlyVisited.forEach { site ->
                        AssistChip(
                            onClick = { onVisitWebsite(site) },
                            label = { Text("${site.name} (${site.visitCount}x)") }
                        )
                    }
                }
            }
        }

        // Saved Collections Preview
        item {
            SectionHeader(
                title = "Saved Collections",
                actionLabel = "Manage",
                onAction = { onNavigate(AppRoute.COLLECTIONS) }
            )
            Spacer(Modifier.height(8.dp))
            if (collections.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AppRoute.COLLECTIONS) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.FolderSpecial, contentDescription = null, tint = TacticalCyan)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Create research collections to group OSINT tools and export dossiers.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    collections.forEach { col ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(180.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                .clickable { onNavigate(AppRoute.COLLECTIONS) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(col.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Text(
                                    "${col.websiteIds.size} websites",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TacticalCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Recommended Websites
        item {
            SectionHeader(
                title = "Top Recommended Websites",
                actionLabel = "Explore All",
                onAction = { onNavigate(AppRoute.EXPLORER) }
            )
        }

        items(recommended, key = { it.id }) { site ->
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

@Composable
private fun StatKpiCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .width(126.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = TacticalCyan)
            }
        }
    }
}
