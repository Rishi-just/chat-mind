package com.example.data.parser

import com.example.data.db.entities.Message
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

object WhatsAppParser {
    // Basic regex to match standard WhatsApp export format:
    // [Date, Time] Sender: Message
    // or
    // Date, Time - Sender: Message
    private val regex1 = Pattern.compile("^\\s*\\[?(\\d{1,4}[-/.]\\d{1,2}[-/.]\\d{1,4})[,\\s]+(\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:[aApP][mM])?)\\]?\\s*[-:]?\\s*([^:]+):\\s*(.*)\$", Pattern.DOTALL)

    fun parse(chatId: Long, text: String): List<Message> {
        val messages = mutableListOf<Message>()
        val lines = text.split("\n")
        
        val formatters = listOf(
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US),
            SimpleDateFormat("MM/dd/yy hh:mm a", Locale.US),
            SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US),
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US),
            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US),
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
        )

        var currentSender = ""
        var currentTimestamp = 0L
        var currentContent = StringBuilder()

        for (line in lines) {
            val matcher = regex1.matcher(line)
            if (matcher.find()) {
                // Save previous message if exists
                if (currentSender.isNotEmpty() && currentContent.isNotEmpty()) {
                    messages.add(
                        Message(
                            chatId = chatId,
                            sender = currentSender,
                            timestamp = currentTimestamp,
                            content = currentContent.toString().trim()
                        )
                    )
                }

                val dateStr = matcher.group(1)?.trim() ?: ""
                val timeStr = matcher.group(2)?.trim() ?: ""
                currentSender = matcher.group(3)?.trim() ?: ""
                val msgContent = matcher.group(4)?.trim() ?: ""
                
                currentContent = StringBuilder(msgContent)
                
                val dateTimeStr = "$dateStr $timeStr"
                currentTimestamp = 0L
                for (formatter in formatters) {
                    try {
                        val parsedDate = formatter.parse(dateTimeStr)
                        if (parsedDate != null) {
                            currentTimestamp = parsedDate.time
                            break
                        }
                    } catch (e: Exception) {
                        // ignore and try next
                    }
                }
            } else {
                // Multiline message, append to current content
                if (currentContent.isNotEmpty()) {
                    currentContent.append("\n").append(line)
                }
            }
        }
        
        // Add the last message
        if (currentSender.isNotEmpty() && currentContent.isNotEmpty()) {
            messages.add(
                Message(
                    chatId = chatId,
                    sender = currentSender,
                    timestamp = currentTimestamp,
                    content = currentContent.toString().trim()
                )
            )
        }

        return messages
    }
}
