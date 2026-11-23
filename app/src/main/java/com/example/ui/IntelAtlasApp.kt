package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.UiState
import com.example.data.model.ResearchCollection
import com.example.data.model.WebsiteResource
import com.example.ui.components.AiAskModalSheet
import com.example.ui.components.AiAskWorkspaceContent
import com.example.ui.screens.AiSearchScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.ComparisonScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExplorerScreen
import com.example.ui.screens.FreeToolsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.IntegrationsScreen
import com.example.ui.screens.SavedWebsitesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AmberLimited
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.IntelAtlasTheme
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.VioletAi
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntelAtlasApp(
    user: FirebaseUser,
    viewModel: IntelAtlasViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val websitesState by viewModel.websitesState.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConvId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val secrets by viewModel.integrationSecrets.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val intentSummary by viewModel.searchIntentSummary.collectAsStateWithLifecycle()
    val pipelineStage by viewModel.searchPipelineStage.collectAsStateWithLifecycle()
    val aiAskStreaming by viewModel.aiAskStreaming.collectAsStateWithLifecycle()
    val aiContextLabel by viewModel.aiContextLabel.collectAsStateWithLifecycle()
    val aiFocusedWebsites by viewModel.aiFocusedWebsites.collectAsStateWithLifecycle()
    val compareIds by viewModel.compareIds.collectAsStateWithLifecycle()
    val comparisonState by viewModel.comparisonState.collectAsStateWithLifecycle()
    val freeAltState by viewModel.freeAltState.collectAsStateWithLifecycle()
    val addUrlInProgress by viewModel.addUrlInProgress.collectAsStateWithLifecycle()
    val testingProvider by viewModel.testingProvider.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()

    val websites = remember(websitesState) {
        (websitesState as? UiState.Success)?.data ?: emptyList()
    }
    val stats = remember(websites, searchHistory, customCategories) {
        viewModel.computeStats(websites, searchHistory, customCategories)
    }
    val allCategories = remember(customCategories) {
        viewModel.allAvailableCategories(customCategories)
    }
    val integrationsList = remember(secrets, preferences.preferredModel) {
        viewModel.buildIntegrationsList(secrets)
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteStr = backStackEntry?.destination?.route ?: AppRoute.DASHBOARD.route
    val currentAppRoute = AppRoute.entries.firstOrNull { it.route == currentRouteStr } ?: AppRoute.DASHBOARD

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var showAiSheet by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var collectionPickerWebsite by remember { mutableStateOf<WebsiteResource?>(null) }
    var isSidebarCollapsed by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val navigateTo: (AppRoute) -> Unit = { route ->
        viewModel.setAiContext(route.label)
        navController.navigate(route.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        scope.launch { drawerState.close() }
    }

    val openWebsiteExternal: (WebsiteResource) -> Unit = { site ->
        viewModel.recordVisit(site)
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(site.url))
            context.startActivity(intent)
        }.onFailure {
            scope.launch { snackbarHostState.showSnackbar("Unable to open ${site.url}") }
        }
    }

    val shareExportText: (String, String) -> Unit = { title, payload ->
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, payload)
        }
        context.startActivity(Intent.createChooser(sendIntent, title))
    }

    val onAskAiAboutSite: (WebsiteResource) -> Unit = { site ->
        viewModel.setAiContext("Website: ${site.name}", listOf(site))
        showAiSheet = true
    }

    IntelAtlasTheme(darkTheme = preferences.darkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 840.dp

            val sidebarContent: @Composable () -> Unit = {
                SidebarNavigationContent(
                    currentRoute = currentAppRoute,
                    userName = user.displayName ?: "Analyst",
                    userEmail = user.email ?: "",
                    compareCount = compareIds.size,
                    searchApiConnected = secrets.hasAnyConfiguredSearchApi(),
                    aiModel = preferences.preferredModel,
                    onNavigate = navigateTo
                )
            }

            val mainScaffold: @Composable () -> Unit = {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            navigationIcon = {
                                if (!isWideScreen) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("open_drawer_button")
                                    ) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Open navigation menu")
                                    }
                                } else {
                                    IconButton(
                                        onClick = { isSidebarCollapsed = !isSidebarCollapsed },
                                        modifier = Modifier.testTag("toggle_sidebar_button")
                                    ) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Toggle sidebar")
                                    }
                                }
                            },
                            title = {
                                Column {
                                    Text(currentAppRoute.label, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "INTELATLAS • ${stats.totalDiscovered} INDEXED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TacticalCyan
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { navigateTo(AppRoute.AI_SEARCH) },
                                    modifier = Modifier.testTag("topbar_search_button")
                                ) {
                                    Icon(Icons.Filled.Search, contentDescription = "Global AI Search")
                                }
                                IconButton(
                                    onClick = { navigateTo(AppRoute.COMPARE) },
                                    modifier = Modifier.testTag("topbar_compare_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (compareIds.isNotEmpty()) {
                                                Badge { Text("${compareIds.size}") }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.CompareArrows, contentDescription = "Compare tools")
                                    }
                                }
                                IconButton(
                                    onClick = { showNotificationsDialog = true },
                                    modifier = Modifier.testTag("topbar_notifications_button")
                                ) {
                                    Icon(Icons.Filled.Notifications, contentDescription = "System & API Status")
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.updatePreferences { it.copy(darkTheme = !it.darkTheme) }
                                    },
                                    modifier = Modifier.testTag("topbar_theme_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (preferences.darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                        contentDescription = "Toggle Light or Dark Theme"
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (!isWideScreen) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                val quickRoutes = listOf(
                                    AppRoute.DASHBOARD to Icons.Filled.Dashboard,
                                    AppRoute.AI_SEARCH to Icons.Filled.Search,
                                    AppRoute.EXPLORER to Icons.Filled.Explore,
                                    AppRoute.SAVED to Icons.Filled.Bookmark
                                )
                                quickRoutes.forEach { (route, icon) ->
                                    NavigationBarItem(
                                        selected = currentAppRoute == route,
                                        onClick = { navigateTo(route) },
                                        icon = { Icon(icon, contentDescription = route.label) },
                                        label = { Text(route.label.substringBefore(" ")) }
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (currentAppRoute != AppRoute.AI_ASK) {
                            FloatingActionButton(
                                onClick = {
                                    viewModel.setAiContext(currentAppRoute.label)
                                    showAiSheet = true
                                },
                                containerColor = VioletAi,
                                contentColor = Color.White,
                                modifier = Modifier.testTag("floating_ai_ask_fab")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = "Open AI Ask Assistant")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(innerPadding)
                    ) {
                        if (websitesState is UiState.Loading && websites.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = TacticalCyan)
                            }
                        } else if (websitesState is UiState.Error && websites.isEmpty()) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (websitesState as UiState.Error).message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            NavHost(
                                navController = navController,
                                startDestination = AppRoute.DASHBOARD.route,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(AppRoute.DASHBOARD.route) {
                                    DashboardScreen(
                                        userDisplayName = user.displayName ?: "Analyst",
                                        stats = stats,
                                        websites = websites,
                                        collections = collections,
                                        compareIds = compareIds,
                                        onQuickAiSearch = { q ->
                                            navigateTo(AppRoute.AI_SEARCH)
                                            viewModel.executeAiSearch(q)
                                        },
                                        onNavigate = navigateTo,
                                        onSelectCategory = { cat ->
                                            viewModel.selectCategoryFilter(cat)
                                            navigateTo(AppRoute.CATEGORIES)
                                        },
                                        onVisitWebsite = openWebsiteExternal,
                                        onToggleSave = viewModel::toggleSaveWebsite,
                                        onAddToCollection = { collectionPickerWebsite = it },
                                        onToggleCompare = { viewModel.toggleCompareWebsite(it.id) },
                                        onAskAiAbout = onAskAiAboutSite,
                                        onVerifyLive = viewModel::verifyWebsiteLive
                                    )
                                }
                                composable(AppRoute.AI_SEARCH.route) {
                                    AiSearchScreen(
                                        query = searchQuery,
                                        filterState = filterState,
                                        searchState = searchState,
                                        intentSummary = intentSummary,
                                        pipelineStage = pipelineStage,
                                        categories = allCategories,
                                        compareIds = compareIds,
                                        onQueryChange = viewModel::updateSearchQuery,
                                        onExecuteSearch = viewModel::executeAiSearch,
                                        onUpdateFilter = viewModel::updateFilterState,
                                        applyFilters = viewModel::applyFilters,
                                        onVisitWebsite = openWebsiteExternal,
                                        onToggleSave = viewModel::toggleSaveWebsite,
                                        onAddToCollection = { collectionPickerWebsite = it },
                                        onToggleCompare = { viewModel.toggleCompareWebsite(it.id) },
                                        onAskAiAbout = onAskAiAboutSite,
                                        onVerifyLive = viewModel::verifyWebsiteLive,
                                        onExportResults = { list, asJson ->
                                            val data = if (asJson) viewModel.exportToJson(list, "AI Search Results")
                                            else viewModel.exportToCsv(list)
                                            shareExportText("IntelAtlas Search Export", data)
                                        }
                                    )
                                }
                                composable(AppRoute.AI_ASK.route) {
                                    AiAskWorkspaceContent(
                                        contextLabel = aiContextLabel,
                                        focusedWebsites = aiFocusedWebsites,
                                        conversations = conversations,
                                        activeConversationId = activeConvId,
                                        messages = activeMessages,
                                        streamingState = aiAskStreaming,
                                        onSendQuestion = { q -> viewModel.sendAiAskMessage(q, websites) },
                                        onSelectConversation = viewModel::selectConversation,
                                        onNewConversation = viewModel::startNewConversation,
                                        onDeleteConversation = viewModel::deleteConversation,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                composable(AppRoute.EXPLORER.route) {
                                    ExplorerScreen(
                                        websites = websites,
                                        categories = allCategories,
                                        compareIds = compareIds,
                                        onVisit = openWebsiteExternal,
                                        onToggleSave = viewModel::toggleSaveWebsite,
                                        onAddToCollection = { collectionPickerWebsite = it },
                                        onToggleCompare = { viewModel.toggleCompareWebsite(it.id) },
                                        onAskAi = onAskAiAboutSite,
                                        onVerifyLive = viewModel::verifyWebsiteLive,
                                        onNavigateToFreeTools = { navigateTo(AppRoute.FREE_TOOLS) }
                                    )
                                }
                                composable(AppRoute.CATEGORIES.route) {
                                    CategoriesScreen(
                                        allCategories = allCategories,
                                        customCategories = customCategories,
                                        websites = websites,
                                        selectedCategory = selectedCategoryFilter,
                                        compareIds = compareIds,
                                        onSelectCategory = viewModel::selectCategoryFilter,
                                        onCreateCustomCategory = viewModel::createCustomCategory,
                                        onDeleteCustomCategory = viewModel::deleteCustomCategory,
                                        onVisit = openWebsiteExternal,
                                        onToggleSave = viewModel::toggleSaveWebsite,
                                        onAddToCollection = { collectionPickerWebsite = it },
                                        onToggleCompare = { viewModel.toggleCompareWebsite(it.id) },
                                        onAskAi = onAskAiAboutSite,
                                        onVerifyLive = viewModel::verifyWebsiteLive
                                    )
                                }
                                composable(AppRoute.FREE_TOOLS.route) {
                                    FreeToolsScreen(
                                        websites = websites,
                                        categories = allCategories,
                                        freeAltState = freeAltState,
                                        compareIds = compareIds,
                                        onDiscoverFreeAlternatives = viewModel::discoverFreeAlternatives,
                                        onVisit = openWebsiteExternal,
                                        onToggleSave = viewModel::toggleSaveWebsite,
                                        onAddToCollection = { collectionPickerWebsite = it },
                                        onToggleCompare = { viewModel.toggleCompareWebsite(it.id) },
                                        onAskAi = onAskAiAboutSite,
                                        onVerifyLive = viewModel::verifyWebsiteLive
                                    )
                                }
                                composable(AppRoute.SAVED.route) {
                                    SavedWebsitesScreen(
                                        websites = websites,
                                        addUrlInProgress = addUrlInProgress,
                                        compareIds = compareIds,
                                        onAddCustomUrl = viewModel::addCustomWebsiteByUrl,
                                        onUpdateMetadata = viewModel::updateWebsiteNotesAndTags,
                                        onDeleteWebsite = viewModel::deleteWebsite,
                                        onVisit = openWebsiteExternal,
                                        onToggleSave = viewModel::toggleSaveWebsite,
                                        onAddToCollection = { collectionPickerWebsite = it },
                                        onToggleCompare = { viewModel.toggleCompareWebsite(it.id) },
                                        onAskAi = onAskAiAboutSite,
                                        onVerifyLive = viewModel::verifyWebsiteLive,
                                        onExport = { list, asJson ->
                                            val data = if (asJson) viewModel.exportToJson(list, "IntelAtlas Library")
                                            else viewModel.exportToCsv(list)
                                            shareExportText("IntelAtlas Library Export", data)
                                        }
                                    )
                                }
                                composable(AppRoute.HISTORY.route) {
                                    HistoryScreen(
                                        history = searchHistory,
                                        visitedWebsites = websites.filter { it.visitCount > 0 },
                                        onRepeatSearch = { q ->
                                            navigateTo(AppRoute.AI_SEARCH)
                                            viewModel.executeAiSearch(q)
                                        },
                                        onDeleteHistory = viewModel::deleteHistoryEntry,
                                        onClearAll = viewModel::clearAllHistory,
                                        onVisitWebsite = openWebsiteExternal
                                    )
                                }
                                composable(AppRoute.INTEGRATIONS.route) {
                                    IntegrationsScreen(
                                        integrations = integrationsList,
                                        secrets = secrets,
                                        testingProvider = testingProvider,
                                        onSaveAndTestProvider = viewModel::saveAndTestIntegration,
                                        onDisconnectProvider = viewModel::disconnectIntegration
                                    )
                                }
                                composable(AppRoute.COLLECTIONS.route) {
                                    CollectionsScreen(
                                        collections = collections,
                                        allWebsites = websites,
                                        onCreateCollection = viewModel::createCollection,
                                        onDeleteCollection = viewModel::deleteCollection,
                                        onRemoveWebsiteFromCollection = viewModel::toggleWebsiteInCollection,
                                        onVisitWebsite = openWebsiteExternal,
                                        onExportCollection = { col, members, asJson ->
                                            val data = if (asJson) viewModel.exportToJson(members, col.name)
                                            else viewModel.exportToCsv(members)
                                            shareExportText("Collection: ${col.name}", data)
                                        }
                                    )
                                }
                                composable(AppRoute.COMPARE.route) {
                                    ComparisonScreen(
                                        allWebsites = websites,
                                        compareIds = compareIds,
                                        comparisonState = comparisonState,
                                        onToggleCompare = viewModel::toggleCompareWebsite,
                                        onClearCompare = viewModel::clearCompare,
                                        onGenerateComparison = viewModel::generateAiComparison
                                    )
                                }
                                composable(AppRoute.SETTINGS.route) {
                                    SettingsScreen(
                                        userEmail = user.email.orEmpty(),
                                        userName = user.displayName.orEmpty(),
                                        preferences = preferences,
                                        onUpdatePreferences = viewModel::updatePreferences,
                                        onExportAllJson = {
                                            shareExportText(
                                                "IntelAtlas Full JSON Export",
                                                viewModel.exportToJson(websites, "IntelAtlas Full Library")
                                            )
                                        },
                                        onExportAllCsv = {
                                            shareExportText(
                                                "IntelAtlas Full CSV Export",
                                                viewModel.exportToCsv(websites)
                                            )
                                        },
                                        onSignOut = {
                                            signOutUser(credentialManager, scope)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (!isSidebarCollapsed) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .width(270.dp)
                                .fillMaxHeight()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            sidebarContent()
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        mainScaffold()
                    }
                }
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.width(290.dp)
                        ) {
                            sidebarContent()
                        }
                    }
                ) {
                    mainScaffold()
                }
            }

            if (showAiSheet) {
                AiAskModalSheet(
                    contextLabel = aiContextLabel,
                    focusedWebsites = aiFocusedWebsites,
                    conversations = conversations,
                    activeConversationId = activeConvId,
                    messages = activeMessages,
                    streamingState = aiAskStreaming,
                    onDismiss = { showAiSheet = false },
                    onSendQuestion = { q -> viewModel.sendAiAskMessage(q, websites) },
                    onSelectConversation = viewModel::selectConversation,
                    onNewConversation = viewModel::startNewConversation,
                    onDeleteConversation = viewModel::deleteConversation
                )
            }

            collectionPickerWebsite?.let { site ->
                CollectionPickerDialog(
                    website = site,
                    collections = collections,
                    onToggleInCollection = { col -> viewModel.toggleWebsiteInCollection(col, site.id) },
                    onCreateNewCollection = { name ->
                        viewModel.createCollection(name, "OSINT Dossier", "#3B82F6", site.id)
                    },
                    onDismiss = { collectionPickerWebsite = null }
                )
            }

            if (showNotificationsDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationsDialog = false },
                    title = { Text("Intelligence System Status") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("AI Engine: Firebase AI Logic (${preferences.preferredModel}) — Active", style = MaterialTheme.typography.bodySmall)
                            Text("Database: Cloud Firestore Synced (${stats.totalDiscovered} websites)", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Web Search APIs: " + if (secrets.hasAnyConfiguredSearchApi()) "Configured & Active" else "Optional (configure in Integrations)",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text("HTTP Reachability Probe: Ready (${stats.verifiedCount} verified)", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showNotificationsDialog = false }) { Text("Close") }
                    }
                )
            }
        }
    }
}

@Composable
private fun SidebarNavigationContent(
    currentRoute: AppRoute,
    userName: String,
    userEmail: String,
    compareCount: Int,
    searchApiConnected: Boolean,
    aiModel: String,
    onNavigate: (AppRoute) -> Unit
) {
    val navItems: List<Pair<AppRoute, ImageVector>> = listOf(
        AppRoute.DASHBOARD to Icons.Filled.Dashboard,
        AppRoute.AI_SEARCH to Icons.Filled.Search,
        AppRoute.AI_ASK to Icons.Filled.QuestionAnswer,
        AppRoute.EXPLORER to Icons.Filled.Explore,
        AppRoute.CATEGORIES to Icons.Filled.Category,
        AppRoute.FREE_TOOLS to Icons.Filled.Verified,
        AppRoute.SAVED to Icons.Filled.Bookmark,
        AppRoute.HISTORY to Icons.Filled.History,
        AppRoute.INTEGRATIONS to Icons.Filled.Hub,
        AppRoute.COLLECTIONS to Icons.Filled.FolderSpecial,
        AppRoute.COMPARE to Icons.Filled.CompareArrows,
        AppRoute.SETTINGS to Icons.Filled.Settings
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElectricBlue.copy(alpha = 0.2f))
                    .border(1.dp, TacticalCyan, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Explore, contentDescription = null, tint = TacticalCyan, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("INTELATLAS", style = MaterialTheme.typography.titleMedium)
                Text("OSINT DISCOVERY", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            navItems.forEach { (route, icon) ->
                NavigationDrawerItem(
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(route.label, style = MaterialTheme.typography.bodyMedium)
                            if (route == AppRoute.COMPARE && compareCount > 0) {
                                Text("$compareCount", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                            }
                        }
                    },
                    selected = currentRoute == route,
                    onClick = { onNavigate(route) },
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp)) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = ElectricBlue.copy(alpha = 0.16f),
                        selectedIconColor = TacticalCyan,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("nav_${route.route}")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))

        // Bottom Status Telemetry + User Profile
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(userName, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        if (userEmail.isNotBlank()) {
                            Text(userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
                StatusDotLine("AI: $aiModel", EmeraldFree)
                StatusDotLine(
                    if (searchApiConnected) "Search API: Connected" else "Search API: Gemini + Probe",
                    if (searchApiConnected) EmeraldFree else AmberLimited
                )
                StatusDotLine("System: Operational", TacticalCyan)
            }
        }
    }
}

@Composable
private fun StatusDotLine(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CollectionPickerDialog(
    website: WebsiteResource,
    collections: List<ResearchCollection>,
    onToggleInCollection: (ResearchCollection) -> Unit,
    onCreateNewCollection: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${website.name} to Collection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                collections.forEach { col ->
                    val checked = website.id in col.websiteIds
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleInCollection(col) }
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { onToggleInCollection(col) })
                        Text(col.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Or create new collection") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onCreateNewCollection(newName)
                    }
                    onDismiss()
                }
            ) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
