package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.ai.ApiKeyManager
import com.example.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToChat: (Long) -> Unit
) {
    val chats by viewModel.chats.collectAsState(initial = emptyList())
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val context = LocalContext.current

    var showPasteDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            var name = "Chat Export"
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && nameIndex != -1) {
                    name = c.getString(nameIndex)
                }
            }
            viewModel.uploadChat(it, name)
        }
    }

    if (showApiKeyDialog) {
        var selectedProvider by remember { mutableStateOf(ApiKeyManager.getProvider(context)) }
        var apiKeyInput by remember { mutableStateOf(ApiKeyManager.getApiKey(context)) }
        var isVisible by remember { mutableStateOf(false) }

        val maskedKey = ApiKeyManager.getMaskedApiKey(context)

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

    if (showPasteDialog) {
        var pasteText by remember { mutableStateOf("") }
        var pasteTitle by remember { mutableStateOf("") }
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Paste Chat Export") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pasteTitle,
                        onValueChange = { pasteTitle = it },
                        label = { Text("Chat Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        label = { Text("Paste content here") },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        maxLines = 10
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            val text = clipboardManager.getText()?.text
                            if (!text.isNullOrEmpty()) {
                                pasteText = text
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Paste from Clipboard")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showPasteDialog = false
                    viewModel.uploadChatFromText(pasteText, pasteTitle.ifEmpty { "Pasted Chat" })
                }) {
                    Text("Upload")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    var showFabMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChatMind", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.Key, contentDescription = "API Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showFabMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Upload Chat")
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Upload .txt File") },
                        onClick = {
                            showFabMenu = false
                            launcher.launch("text/plain")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Paste Chat Content") },
                        onClick = {
                            showFabMenu = false
                            showPasteDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Load Sample Chat") },
                        onClick = {
                            showFabMenu = false
                            viewModel.uploadSampleChat()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("AI Provider Settings") },
                        onClick = {
                            showFabMenu = false
                            showApiKeyDialog = true
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uploadProgress)
                }
            }
        } else if (chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No chats uploaded yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap the + button to upload a .txt file", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("or paste your chat export.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val activeProvider = ApiKeyManager.getProvider(context).uppercase()
                        Text("AI Provider: $activeProvider (${ApiKeyManager.getMaskedApiKey(context)})")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                items(chats) { chat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToChat(chat.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = chat.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(chat.uploadedAt))
                            Text(
                                text = "Uploaded $dateStr • ${chat.messagesCount} messages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            if (!chat.summary.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = chat.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
