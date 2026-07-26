package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.ai.ApiKeyManager
import com.example.ui.viewmodels.ChatDetailsViewModel
import com.example.data.db.entities.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailsScreen(
    viewModel: ChatDetailsViewModel,
    onBack: () -> Unit
) {
    val chat by viewModel.chat.collectAsState(initial = null)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Timeline", "Tasks", "Decisions", "AI Chat")
    val context = LocalContext.current
    var showApiKeyDialog by remember { mutableStateOf(false) }

    if (showApiKeyDialog) {
        var selectedProvider by remember { mutableStateOf(ApiKeyManager.getProvider(context)) }
        var apiKeyInput by remember { mutableStateOf(ApiKeyManager.getApiKey(context)) }
        var isVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("AI Provider & Key Settings") },
            text = {
                Column {
                    Text("Select AI Provider:", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = selectedProvider == ApiKeyManager.PROVIDER_GEMINI,
                            onClick = {
                                selectedProvider = ApiKeyManager.PROVIDER_GEMINI
                                apiKeyInput = ApiKeyManager.getGeminiApiKey(context)
                            },
                            label = { Text("Gemini") }
                        )
                        FilterChip(
                            selected = selectedProvider == ApiKeyManager.PROVIDER_GROQ,
                            onClick = {
                                selectedProvider = ApiKeyManager.PROVIDER_GROQ
                                apiKeyInput = ApiKeyManager.getGroqApiKey(context)
                            },
                            label = { Text("Groq (Free)") }
                        )
                        FilterChip(
                            selected = selectedProvider == ApiKeyManager.PROVIDER_OPENAI,
                            onClick = {
                                selectedProvider = ApiKeyManager.PROVIDER_OPENAI
                                apiKeyInput = ApiKeyManager.getOpenAIApiKey(context)
                            },
                            label = { Text("OpenAI") }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val linkText = when (selectedProvider) {
                        ApiKeyManager.PROVIDER_GROQ -> "Get free API Key at console.groq.com"
                        ApiKeyManager.PROVIDER_OPENAI -> "Get API Key at platform.openai.com or openrouter.ai"
                        else -> "Get free API Key at ai.google.dev"
                    }

                    val hintText = when (selectedProvider) {
                        ApiKeyManager.PROVIDER_GROQ -> "gsk_..."
                        ApiKeyManager.PROVIDER_OPENAI -> "sk-... or sk-or-..."
                        else -> "AIzaSy..."
                    }

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("${selectedProvider.uppercase()} API Key") },
                        placeholder = { Text(hintText) },
                        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isVisible = !isVisible }) {
                                Icon(
                                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isVisible) "Hide Key" else "Show Key"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        linkText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    ApiKeyManager.saveProvider(context, selectedProvider)
                    ApiKeyManager.saveApiKey(context, apiKeyInput)
                    showApiKeyDialog = false
                }) {
                    Text("Save Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chat?.title ?: "Loading...", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.Key, contentDescription = "AI Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTabIndex) {
                    0 -> OverviewTab(viewModel)
                    1 -> TimelineTab(viewModel)
                    2 -> TasksTab(viewModel)
                    3 -> DecisionsTab(viewModel)
                    4 -> AiChatTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun OverviewTab(viewModel: ChatDetailsViewModel) {
    val chat by viewModel.chat.collectAsState(initial = null)
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisError by viewModel.analysisError.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        analysisError?.let { err ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = err,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        chat?.let { c ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Executive Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                OutlinedButton(onClick = { viewModel.reanalyzeChat() }) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analyze AI", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(c.summary ?: "No summary available.")
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Topics Discussed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        val topics = c.topics?.split(",")?.map { it.trim() } ?: emptyList()
                        if (topics.isEmpty()) {
                            Text("No topics identified.")
                        } else {
                            topics.forEach { topic ->
                                Text("• $topic")
                            }
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Conversation Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total Messages: ${c.messagesCount}")
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineTab(viewModel: ChatDetailsViewModel) {
    val messages by viewModel.messages.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }
        if (messages.isEmpty()) {
            item { Text("No messages available.") }
        }
        items(messages.take(100)) { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(msg.sender, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text(msg.content)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun TasksTab(viewModel: ChatDetailsViewModel) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (tasks.isEmpty()) {
            item { Text("No tasks identified.") }
        }
        items(tasks) { task ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(task.description, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Assignee: ${task.assignee ?: "Unassigned"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun DecisionsTab(viewModel: ChatDetailsViewModel) {
    val decisions by viewModel.decisions.collectAsState(initial = emptyList())
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (decisions.isEmpty()) {
            item { Text("No decisions identified.") }
        }
        items(decisions) { decision ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("•", modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text(decision.description)
                }
            }
        }
    }
}

@Composable
fun AiChatTab(viewModel: ChatDetailsViewModel) {
    val history by viewModel.aiChatHistory.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    var input by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            reverseLayout = true
        ) {
            if (isThinking) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("AI is thinking...", modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
            
            items(history.reversed()) { pair ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (pair.second.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                                modifier = Modifier.padding(end = 32.dp)
                            ) {
                                Text(pair.second, modifier = Modifier.padding(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 0.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                            modifier = Modifier.padding(start = 32.dp)
                        ) {
                            Text(pair.first, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
            
            if (history.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Ask me anything about this conversation!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }
        
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            placeholder = { Text("Ask a question...") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            trailingIcon = {
                IconButton(
                    onClick = {
                        viewModel.sendChatMessage(input)
                        input = ""
                    },
                    enabled = !isThinking && input.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
