package com.example.data.repository

import com.example.data.db.dao.ChatDao
import com.example.data.db.entities.Chat
import com.example.data.db.entities.Decision
import com.example.data.db.entities.Message
import com.example.data.db.entities.Task
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allChats: Flow<List<Chat>> = chatDao.getAllChats()

    fun getChatById(chatId: Long): Flow<Chat?> = chatDao.getChatById(chatId)
    
    suspend fun getChatByIdSync(chatId: Long): Chat? = chatDao.getChatByIdSync(chatId)

    suspend fun insertChat(chat: Chat): Long = chatDao.insertChat(chat)

    suspend fun updateChat(chat: Chat) = chatDao.updateChat(chat)

    suspend fun deleteChatById(chatId: Long) = chatDao.deleteChatById(chatId)

    suspend fun insertMessages(messages: List<Message>) = chatDao.insertMessages(messages)

    fun getMessagesForChat(chatId: Long): Flow<List<Message>> = chatDao.getMessagesForChat(chatId)
    
    suspend fun getMessagesForChatSync(chatId: Long): List<Message> = chatDao.getMessagesForChatSync(chatId)

    suspend fun insertTasks(tasks: List<Task>) = chatDao.insertTasks(tasks)

    fun getTasksForChat(chatId: Long): Flow<List<Task>> = chatDao.getTasksForChat(chatId)

    suspend fun insertDecisions(decisions: List<Decision>) = chatDao.insertDecisions(decisions)

    fun getDecisionsForChat(chatId: Long): Flow<List<Decision>> = chatDao.getDecisionsForChat(chatId)
}
