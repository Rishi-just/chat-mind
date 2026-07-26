package com.example.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.ai.AIController
import com.example.data.db.AppDatabase
import com.example.data.db.entities.Chat
import com.example.data.db.entities.Decision
import com.example.data.db.entities.Task
import com.example.data.parser.WhatsAppParser
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "chatmind-db"
    ).fallbackToDestructiveMigration().build()

    val repository = ChatRepository(database.chatDao())
    private val aiController = AIController(application.applicationContext)

    val chats = repository.allChats

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()
    
    private val _uploadProgress = MutableStateFlow("")
    val uploadProgress: StateFlow<String> = _uploadProgress.asStateFlow()

    fun uploadChat(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = "Reading file..."
            
            try {
                val content = StringBuilder()
                val isZip = fileName.endsWith(".zip", ignoreCase = true)
                
                if (isZip) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                        java.util.zip.ZipInputStream(inputStream).use { zipStream ->
                            var entry = zipStream.nextEntry
                            while (entry != null) {
                                if (!entry.isDirectory && entry.name.endsWith(".txt", ignoreCase = true)) {
                                    val reader = java.io.BufferedReader(java.io.InputStreamReader(zipStream))
                                    var line: String? = reader.readLine()
                                    while (line != null) {
                                        content.append(line).append("\n")
                                        line = reader.readLine()
                                    }
                                    break
                                }
                                entry = zipStream.nextEntry
                            }
                        }
                    }
                } else {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                        java.io.BufferedReader(java.io.InputStreamReader(inputStream)).use { reader ->
                            var line: String? = reader.readLine()
                            while (line != null) {
                                content.append(line).append("\n")
                                line = reader.readLine()
                            }
                        }
                    }
                }
                processChatContent(content.toString(), fileName)
            } catch (e: Exception) {
                e.printStackTrace()
                _uploadProgress.value = "Error: ${e.message}"
                kotlinx.coroutines.delay(1000)
                _isUploading.value = false
            }
        }
    }
    
    fun uploadChatFromText(content: String, title: String) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                processChatContent(content, title)
            } catch (e: Exception) {
                e.printStackTrace()
                _uploadProgress.value = "Error: ${e.message}"
                kotlinx.coroutines.delay(1000)
                _isUploading.value = false
            }
        }
    }
    
    fun uploadSampleChat() {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                _uploadProgress.value = "Reading sample file..."
                val content = getApplication<Application>().assets.open("sample_chat.txt").bufferedReader().use { it.readText() }
                processChatContent(content, "Sample Chat")
            } catch (e: Exception) {
                e.printStackTrace()
                _uploadProgress.value = "Error: ${e.message}"
                kotlinx.coroutines.delay(1000)
                _isUploading.value = false
            }
        }
    }
    
    private suspend fun processChatContent(content: String, fileName: String) {
        _uploadProgress.value = "Parsing messages..."
        val chat = Chat(title = fileName.replace(".txt", ""), fileName = fileName)
        val chatId = repository.insertChat(chat)
        
        val messages = WhatsAppParser.parse(chatId, content)
        
        // Update chat with message count
        val updatedChat = chat.copy(id = chatId, messagesCount = messages.size)
        repository.updateChat(updatedChat)
        
        // Save messages in batches to avoid large transactions
        val batchSize = 1000
        messages.chunked(batchSize).forEachIndexed { index, batch ->
            _uploadProgress.value = "Saving messages (${index * batchSize}/${messages.size})..."
            repository.insertMessages(batch)
        }

        _uploadProgress.value = "Generating AI Analysis..."
        when (val result = aiController.analyzeChat(messages)) {
            is com.example.data.ai.AnalysisResult.Success -> {
                val analysis = result.data
                val finalChat = updatedChat.copy(
                    summary = analysis.summary,
                    topics = analysis.topics.joinToString(", ")
                )
                repository.updateChat(finalChat)
                
                if (analysis.tasks.isNotEmpty()) {
                    repository.insertTasks(analysis.tasks.map { Task(chatId = chatId, description = it.description, assignee = it.assignee) })
                }
                if (analysis.decisions.isNotEmpty()) {
                    repository.insertDecisions(analysis.decisions.map { Decision(chatId = chatId, description = it) })
                }
            }
            is com.example.data.ai.AnalysisResult.Error -> {
                val senders = messages.map { it.sender }.distinct().filter { it.isNotBlank() }
                val sendersStr = if (senders.isNotEmpty()) senders.take(3).joinToString(", ") else "Participants"
                val fallbackSummary = "Chat export with $sendersStr containing ${messages.size} messages. Tap 'Analyze AI' to generate AI analysis."
                val finalChat = updatedChat.copy(
                    summary = fallbackSummary,
                    topics = "General Conversation"
                )
                repository.updateChat(finalChat)
            }
        }
        
        _uploadProgress.value = "Upload complete!"
        kotlinx.coroutines.delay(1000)
        _isUploading.value = false
    }
}
