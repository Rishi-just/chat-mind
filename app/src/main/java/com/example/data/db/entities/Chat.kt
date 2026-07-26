package com.example.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fileName: String,
    val uploadedAt: Long = System.currentTimeMillis(),
    val summary: String? = null,
    val topics: String? = null, // Comma separated topics
    val messagesCount: Int = 0
)
