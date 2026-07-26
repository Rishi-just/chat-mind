package com.example.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String
)

@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val response_format: ResponseFormat? = null,
    val temperature: Float = 0.2f
)

@Serializable
data class OpenAiChoice(
    val message: OpenAiMessage? = null
)

@Serializable
data class OpenAiResponse(
    val choices: List<OpenAiChoice>? = null,
    val error: OpenAiErrorDetail? = null
)

@Serializable
data class OpenAiErrorDetail(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

interface OpenAiApiService {
    @POST
    suspend fun chatCompletions(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse
}
