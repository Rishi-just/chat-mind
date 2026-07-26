package com.example.data.ai

import android.content.Context
import com.example.data.db.entities.Message
import com.example.data.network.Content
import com.example.data.network.GenerateContentRequest
import com.example.data.network.GenerationConfig
import com.example.data.network.OpenAiMessage
import com.example.data.network.OpenAiRequest
import com.example.data.network.Part
import com.example.data.network.ResponseFormat
import com.example.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import retrofit2.HttpException

@Serializable
data class ChatAnalysisResult(
    val summary: String,
    val topics: List<String> = emptyList(),
    val tasks: List<ExtractedTask> = emptyList(),
    val decisions: List<String> = emptyList()
)

@Serializable
data class ExtractedTask(
    val description: String,
    val assignee: String = "Unassigned"
)

sealed class AnalysisResult {
    data class Success(val data: ChatAnalysisResult) : AnalysisResult()
    data class Error(val message: String) : AnalysisResult()
}

class AIController(private val context: Context) {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
        coerceInputValues = true
    }

    suspend fun analyzeChat(messages: List<Message>): AnalysisResult = withContext(Dispatchers.IO) {
        val provider = ApiKeyManager.getProvider(context)
        val apiKey = ApiKeyManager.getApiKey(context)

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AnalysisResult.Error("API Key for ${provider.uppercase()} is not configured. Tap the Key icon to set your API Key.")
        }

        val messagesToSend = if (messages.size > 300) {
            messages.takeLast(300)
        } else {
            messages
        }

        var conversationText = messagesToSend.joinToString("\n") { 
            "${it.sender}: ${it.content}"
        }

        if (conversationText.length > 25000) {
            conversationText = conversationText.takeLast(25000)
        }

        val prompt = """
            Analyze the following conversation and output JSON with these exact fields:
            - summary: string (concise executive summary)
            - topics: array of strings (main discussion topics)
            - tasks: array of objects with 'description' and 'assignee' strings
            - decisions: array of strings (key decisions made)

            Conversation:
            $conversationText
        """.trimIndent()

        when (provider) {
            ApiKeyManager.PROVIDER_GROQ -> {
                return@withContext analyzeWithOpenAiCompatible(
                    apiKey = apiKey,
                    url = "https://api.groq.com/openai/v1/chat/completions",
                    model = "llama-3.3-70b-versatile",
                    prompt = prompt
                )
            }
            ApiKeyManager.PROVIDER_OPENAI -> {
                val url = if (apiKey.startsWith("sk-or-")) {
                    "https://openrouter.ai/api/v1/chat/completions"
                } else {
                    "https://api.openai.com/v1/chat/completions"
                }
                val model = if (apiKey.startsWith("sk-or-")) "deepseek/deepseek-chat" else "gpt-4o-mini"
                return@withContext analyzeWithOpenAiCompatible(
                    apiKey = apiKey,
                    url = url,
                    model = model,
                    prompt = prompt
                )
            }
            else -> {
                return@withContext analyzeWithGemini(apiKey, prompt)
            }
        }
    }

    private suspend fun analyzeWithGemini(apiKey: String, prompt: String): AnalysisResult = withContext(Dispatchers.IO) {
        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("summary") { put("type", "STRING") }
                putJsonObject("topics") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("tasks") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("description") { put("type", "STRING") }
                            putJsonObject("assignee") { put("type", "STRING") }
                        }
                    }
                }
                putJsonObject("decisions") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(role = "user", parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = schema,
                temperature = 0.2f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            
            if (!jsonText.isNullOrBlank()) {
                val cleanJson = jsonText.trim()
                    .removePrefix("```json")
                    .removePrefix("```JSON")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                    
                val parsed = json.decodeFromString<ChatAnalysisResult>(cleanJson)
                return@withContext AnalysisResult.Success(parsed)
            } else {
                return@withContext AnalysisResult.Error("Gemini returned an empty response. Please try again.")
            }
        } catch (e: HttpException) {
            e.printStackTrace()
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            val errorMsg = when (e.code()) {
                400 -> if (errorBody.contains("API_KEY_INVALID")) "Invalid Gemini API Key. Check your key from ai.google.dev or switch to Groq AI." else "Gemini API Error (400): $errorBody"
                401, 403 -> "Gemini Key Error (${e.code()}): Invalid key or permission denied."
                429 -> "Gemini Quota Exceeded (429): Free rate limit reached. Switch provider to Groq AI or wait 1 min."
                else -> "Gemini API Error (${e.code()}): ${e.message()}"
            }
            return@withContext AnalysisResult.Error(errorMsg)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext AnalysisResult.Error("Failed to parse AI response: ${e.localizedMessage ?: e.message}")
        }
    }

    private suspend fun analyzeWithOpenAiCompatible(
        apiKey: String,
        url: String,
        model: String,
        prompt: String
    ): AnalysisResult = withContext(Dispatchers.IO) {
        val request = OpenAiRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(
                    role = "system",
                    content = "You are a JSON assistant. Output valid JSON only with keys: summary (string), topics (list of strings), tasks (list of objects with description and assignee strings), decisions (list of strings)."
                ),
                OpenAiMessage(role = "user", content = prompt)
            ),
            response_format = ResponseFormat(type = "json_object"),
            temperature = 0.2f
        )

        try {
            val response = RetrofitClient.openAiService.chatCompletions(
                url = url,
                authHeader = "Bearer $apiKey",
                request = request
            )
            
            val jsonText = response.choices?.firstOrNull()?.message?.content
            if (!jsonText.isNullOrBlank()) {
                val cleanJson = jsonText.trim()
                    .removePrefix("```json")
                    .removePrefix("```JSON")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                val parsed = json.decodeFromString<ChatAnalysisResult>(cleanJson)
                return@withContext AnalysisResult.Success(parsed)
            } else if (response.error != null) {
                return@withContext AnalysisResult.Error("API Error: ${response.error.message ?: "Unknown error"}")
            } else {
                return@withContext AnalysisResult.Error("API returned an empty response. Please try again.")
            }
        } catch (e: HttpException) {
            e.printStackTrace()
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            return@withContext AnalysisResult.Error("API Error (${e.code()}): ${if (errorBody.isNotBlank()) errorBody else e.message()}")
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext AnalysisResult.Error("Failed to parse response: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun chatWithData(messages: List<Message>, userQuery: String, history: List<Pair<String, String>>): String = withContext(Dispatchers.IO) {
        val provider = ApiKeyManager.getProvider(context)
        val apiKey = ApiKeyManager.getApiKey(context)

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key for ${provider.uppercase()} is not configured. Please tap the Key icon to set your API Key."
        }
        
        val contextMessages = if (messages.size > 200) messages.takeLast(200) else messages
        var conversationText = contextMessages.joinToString("\n") { "${it.sender}: ${it.content}" }
        if (conversationText.length > 20000) {
            conversationText = conversationText.takeLast(20000)
        }
        
        val sysInstruction = """
            You are a helpful AI assistant analyzing a chat export. 
            You must only answer questions based on the provided conversation text.
            If the answer is not in the text, say "I don't have enough information from the chat to answer that."
            
            Conversation Context:
            $conversationText
        """.trimIndent()

        when (provider) {
            ApiKeyManager.PROVIDER_GROQ -> {
                return@withContext chatWithOpenAiCompatible(
                    apiKey = apiKey,
                    url = "https://api.groq.com/openai/v1/chat/completions",
                    model = "llama-3.3-70b-versatile",
                    sysInstruction = sysInstruction,
                    userQuery = userQuery,
                    history = history
                )
            }
            ApiKeyManager.PROVIDER_OPENAI -> {
                val url = if (apiKey.startsWith("sk-or-")) "https://openrouter.ai/api/v1/chat/completions" else "https://api.openai.com/v1/chat/completions"
                val model = if (apiKey.startsWith("sk-or-")) "deepseek/deepseek-chat" else "gpt-4o-mini"
                return@withContext chatWithOpenAiCompatible(
                    apiKey = apiKey,
                    url = url,
                    model = model,
                    sysInstruction = sysInstruction,
                    userQuery = userQuery,
                    history = history
                )
            }
            else -> {
                return@withContext chatWithGemini(apiKey, sysInstruction, userQuery, history)
            }
        }
    }

    private suspend fun chatWithGemini(apiKey: String, sysInstruction: String, userQuery: String, history: List<Pair<String, String>>): String = withContext(Dispatchers.IO) {
        val contents = mutableListOf<Content>()
        
        for ((q, a) in history) {
            contents.add(Content(role = "user", parts = listOf(Part(text = q))))
            if (a.isNotEmpty()) {
                contents.add(Content(role = "model", parts = listOf(Part(text = a))))
            }
        }
        
        contents.add(Content(role = "user", parts = listOf(Part(text = userQuery))))

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(role = "user", parts = listOf(Part(text = sysInstruction))),
            generationConfig = GenerationConfig(temperature = 0.3f)
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            return@withContext reply ?: "I could not generate an answer."
        } catch (e: HttpException) {
            e.printStackTrace()
            val errorBody = e.response()?.errorBody()?.string()
            if (e.code() == 400 || e.code() == 401 || e.code() == 403) {
                return@withContext "Gemini Key Error (${e.code()}): Invalid API key."
            }
            if (e.code() == 429) {
                return@withContext "Gemini Quota Exceeded (429): Rate limit reached."
            }
            if (!errorBody.isNullOrEmpty()) {
                return@withContext "Gemini Error (${e.code()}): $errorBody"
            }
            return@withContext "Gemini Error (${e.code()}): ${e.message()}"
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "An error occurred: ${e.localizedMessage ?: e.message}"
        }
    }

    private suspend fun chatWithOpenAiCompatible(
        apiKey: String,
        url: String,
        model: String,
        sysInstruction: String,
        userQuery: String,
        history: List<Pair<String, String>>
    ): String = withContext(Dispatchers.IO) {
        val openAiMessages = mutableListOf<OpenAiMessage>()
        openAiMessages.add(OpenAiMessage(role = "system", content = sysInstruction))
        
        for ((q, a) in history) {
            openAiMessages.add(OpenAiMessage(role = "user", content = q))
            if (a.isNotEmpty()) {
                openAiMessages.add(OpenAiMessage(role = "assistant", content = a))
            }
        }
        
        openAiMessages.add(OpenAiMessage(role = "user", content = userQuery))

        val request = OpenAiRequest(
            model = model,
            messages = openAiMessages,
            temperature = 0.3f
        )

        try {
            val response = RetrofitClient.openAiService.chatCompletions(
                url = url,
                authHeader = "Bearer $apiKey",
                request = request
            )
            val reply = response.choices?.firstOrNull()?.message?.content
            return@withContext reply ?: "I could not generate an answer."
        } catch (e: HttpException) {
            e.printStackTrace()
            val errorBody = e.response()?.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                return@withContext "API Error (${e.code()}): $errorBody"
            }
            return@withContext "API Error (${e.code()}): ${e.message()}"
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "An error occurred: ${e.localizedMessage ?: e.message}"
        }
    }
}
