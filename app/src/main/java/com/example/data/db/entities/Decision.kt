package com.example.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "decisions",
    foreignKeys = [
        ForeignKey(
            entity = Chat::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("chatId"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chatId"])]
)
data class Decision(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val description: String
)
