package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AIController
import com.example.data.ai.AnalysisResult
import com.example.data.db.entities.Decision
import com.example.data.db.entities.Message
import com.example.data.db.entities.Task
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatDetailsViewModel(
    private val chatId: Long,
    private val repository: ChatRepository,
    application: Application
) : AndroidViewModel(application) {

    private val aiController = AIController(application.applicationContext)

    val chat = repository.getChatById(chatId)
    val tasks = repository.getTasksForChat(chatId)
    val decisions = repository.getDecisionsForChat(chatId)
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _aiChatHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val aiChatHistory: StateFlow<List<Pair<String, String>>> = _aiChatHistory.asStateFlow()
    
    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getMessagesForChat(chatId).collect {
                _messages.value = it
            }
        }
    }

    fun sendChatMessage(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _aiChatHistory.value = _aiChatHistory.value + (query to "")
            _isAiThinking.value = true
            
            val response = aiController.chatWithData(_messages.value, query, _aiChatHistory.value.dropLast(1))
            
            val newList = _aiChatHistory.value.toMutableList()
            if (newList.isNotEmpty()) {
                val lastIndex = newList.lastIndex
                newList[lastIndex] = newList[lastIndex].first to response
            }
            _aiChatHistory.value = newList
            _isAiThinking.value = false
        }
    }

    fun reanalyzeChat() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisError.value = null
            
            val currentChat = repository.getChatByIdSync(chatId)
            val currentMessages = repository.getMessagesForChatSync(chatId)
            
            if (currentChat != null && currentMessages.isNotEmpty()) {
                when (val result = aiController.analyzeChat(currentMessages)) {
                    is AnalysisResult.Success -> {
                        val analysis = result.data
                        val updatedChat = currentChat.copy(
                            summary = analysis.summary,
                            topics = analysis.topics.joinToString(", ")
                        )
                        repository.updateChat(updatedChat)
                        
                        if (analysis.tasks.isNotEmpty()) {
                            repository.insertTasks(analysis.tasks.map { Task(chatId = chatId, description = it.description, assignee = it.assignee) })
                        }
                        if (analysis.decisions.isNotEmpty()) {
                            repository.insertDecisions(analysis.decisions.map { Decision(chatId = chatId, description = it) })
                        }
                    }
                    is AnalysisResult.Error -> {
                        _analysisError.value = result.message
                    }
                }
            } else {
                _analysisError.value = "No messages found in this chat to analyze."
            }
            _isAnalyzing.value = false
        }
    }
}

class ChatDetailsViewModelFactory(
    private val chatId: Long,
    private val repository: ChatRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatDetailsViewModel(chatId, repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
