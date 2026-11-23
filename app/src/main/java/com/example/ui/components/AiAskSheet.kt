package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.UiState
import com.example.data.model.AiConversation
import com.example.data.model.AiMessage
import com.example.data.model.WebsiteResource
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.VioletAi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAskModalSheet(
    contextLabel: String,
    focusedWebsites: List<WebsiteResource>,
    conversations: List<AiConversation>,
    activeConversationId: String?,
    messages: List<AiMessage>,
    streamingState: UiState<String>,
    onDismiss: () -> Unit,
    onSendQuestion: (String) -> Unit,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AiAskWorkspaceContent(
            contextLabel = contextLabel,
            focusedWebsites = focusedWebsites,
            conversations = conversations,
            activeConversationId = activeConversationId,
            messages = messages,
            streamingState = streamingState,
            onSendQuestion = onSendQuestion,
            onSelectConversation = onSelectConversation,
            onNewConversation = onNewConversation,
            onDeleteConversation = onDeleteConversation,
            onClose = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
        )
    }
}

@Composable
fun AiAskWorkspaceContent(
    contextLabel: String,
    focusedWebsites: List<WebsiteResource>,
    conversations: List<AiConversation>,
    activeConversationId: String?,
    messages: List<AiMessage>,
    streamingState: UiState<String>,
    onSendQuestion: (String) -> Unit,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = remember(focusedWebsites) {
        if (focusedWebsites.isNotEmpty()) {
            val names = focusedWebsites.take(2).joinToString(" and ") { it.name }
            listOf(
                "Which of these websites is completely free?",
                "Compare $names",
                "Find a free open-source alternative",
                "Which tool is better for a beginner?"
            )
        } else {
            listOf(
                "Which websites in my library are completely free?",
                "Recommend top OSINT tools for domain recon",
                "Best free websites for analyzing government datasets",
                "Which AI tools require no registration?"
            )
        }
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(VioletAi.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = VioletAi)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("AI Ask Intelligence Assistant", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Context: $contextLabel" +
                            if (focusedWebsites.isNotEmpty()) " (${focusedWebsites.size} sites)" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = TacticalCyan
                    )
                }
            }
            Row {
                IconButton(
                    onClick = { showHistory = !showHistory },
                    modifier = Modifier.testTag("ai_ask_history_toggle")
                ) {
                    Icon(Icons.Filled.History, contentDescription = "Conversation history")
                }
                IconButton(
                    onClick = onNewConversation,
                    modifier = Modifier.testTag("ai_ask_new_chat")
                ) {
                    Icon(Icons.Filled.AddComment, contentDescription = "New conversation")
                }
                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("ai_ask_close")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close AI Ask")
                    }
                }
            }
        }

        // Optional persistent conversation thread selector
        if (showHistory && conversations.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("SAVED CONVERSATIONS", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        conversations.forEach { conv ->
                            FilterChip(
                                selected = conv.id == activeConversationId,
                                onClick = {
                                    onSelectConversation(conv.id)
                                    showHistory = false
                                },
                                label = { Text(conv.title.take(28)) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onDeleteConversation(conv.id) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete conversation",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Quick contextual suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { prompt ->
                AssistChip(
                    onClick = { onSendQuestion(prompt) },
                    label = { Text(prompt, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Messages area
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && streamingState is UiState.Idle) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Ask anything about discovered websites, comparisons, pricing limits, or OSINT workflows.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Conversations persist automatically in your encrypted Firestore history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isUser) {
                            ElectricBlue.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .border(
                                1.dp,
                                if (isUser) ElectricBlue.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUser) "ANALYST" else "INTELATLAS AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUser) ElectricBlue else TacticalCyan
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            when (streamingState) {
                is UiState.Loading -> {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Analyzing intelligence context...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is UiState.Streaming -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .border(1.dp, TacticalCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("INTELATLAS AI • STREAMING", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                                Spacer(Modifier.height(4.dp))
                                Text(streamingState.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = streamingState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                else -> Unit
            }
        }

        // Input bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask about tools, pricing, or alternatives...") },
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_ask_input")
            )
            Surface(
                color = ElectricBlue,
                shape = RoundedCornerShape(14.dp)
            ) {
                IconButton(
                    onClick = {
                        if (input.isNotBlank() && streamingState !is UiState.Loading && streamingState !is UiState.Streaming) {
                            val q = input
                            input = ""
                            onSendQuestion(q)
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("ai_ask_send")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send AI question",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
