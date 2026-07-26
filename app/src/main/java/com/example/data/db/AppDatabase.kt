package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.db.dao.ChatDao
import com.example.data.db.entities.Chat
import com.example.data.db.entities.Decision
import com.example.data.db.entities.Message
import com.example.data.db.entities.Task

@Database(
    entities = [Chat::class, Message::class, Task::class, Decision::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
