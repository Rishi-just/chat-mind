package com.example.data.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

object ApiKeyManager {
    private const val PREFS_NAME = "chatmind_prefs"
    private const val KEY_PROVIDER = "selected_provider"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    private const val KEY_GROQ_API_KEY = "groq_api_key"
    private const val KEY_OPENAI_API_KEY = "openai_api_key"

    const val PROVIDER_GEMINI = "gemini"
    const val PROVIDER_GROQ = "groq"
    const val PROVIDER_OPENAI = "openai"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getProvider(context: Context): String {
        return getPrefs(context).getString(KEY_PROVIDER, PROVIDER_GEMINI) ?: PROVIDER_GEMINI
    }

    fun saveProvider(context: Context, provider: String) {
        getPrefs(context).edit().putString(KEY_PROVIDER, provider).apply()
    }

    fun getApiKey(context: Context): String {
        return when (getProvider(context)) {
            PROVIDER_GROQ -> getGroqApiKey(context)
            PROVIDER_OPENAI -> getOpenAIApiKey(context)
            else -> getGeminiApiKey(context)
        }
    }

    fun getGeminiApiKey(context: Context): String {
        val savedKey = getPrefs(context).getString(KEY_GEMINI_API_KEY, "") ?: ""
        if (savedKey.isNotBlank()) return savedKey.trim()
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") return buildKey.trim()
        return ""
    }

    fun saveGeminiApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    fun getGroqApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_GROQ_API_KEY, "")?.trim() ?: ""
    }

    fun saveGroqApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_GROQ_API_KEY, apiKey.trim()).apply()
    }

    fun getOpenAIApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_OPENAI_API_KEY, "")?.trim() ?: ""
    }

    fun saveOpenAIApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_OPENAI_API_KEY, apiKey.trim()).apply()
    }

    fun saveApiKey(context: Context, apiKey: String) {
        when (getProvider(context)) {
            PROVIDER_GROQ -> saveGroqApiKey(context, apiKey)
            PROVIDER_OPENAI -> saveOpenAIApiKey(context, apiKey)
            else -> saveGeminiApiKey(context, apiKey)
        }
    }

    fun hasValidApiKey(context: Context): Boolean {
        return getApiKey(context).isNotEmpty()
    }

    fun getMaskedApiKey(context: Context): String {
        val key = getApiKey(context)
        if (key.isEmpty()) return "Not configured"
        if (key.length <= 4) return "••••"
        return "••••••••" + key.takeLast(4)
    }
}
