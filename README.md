# 🧠 ChatMind - AI WhatsApp & Chat Export Analyzer

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Built with AI](https://img.shields.io/badge/Built%20with-AI%20Intelligence-purple.svg)](#-built-with-ai-intelligence)

**ChatMind** is an Android application that parses WhatsApp chat exports (`.txt` / `.zip`) and transforms long, cluttered conversations into structured **Executive Summaries**, **Topics**, **Actionable Tasks**, and **Key Decisions** using Multi-Provider AI (Google Gemini, Groq AI Free Llama 3.3, and OpenAI).

> ✨ **Made with AI Intelligence**: Built collaboratively with Google DeepMind's Antigravity Agentic AI Assistant.

---

## ✨ Features

- 📲 **Native Share Sheet Integration**: Export any chat from WhatsApp ("Export Chat" -> "Without Media") and share directly to **ChatMind**!
- ⚡ **Multi-Provider AI Engine**:
  - **Google Gemini** (`gemini-1.5-flash`)
  - **Groq AI (Free Llama 3.3 70B)** — 100% free, ultra-fast 500+ tokens/sec
  - **OpenAI / OpenRouter** (`gpt-4o-mini`, `deepseek-chat`)
- 🔒 **Privacy & Key Security**: In-app password masking (`••••••••••••`) with visibility toggle for API keys.
- 📋 **Executive Summaries & Topics**: Automated structured insights extracted directly from chat transcripts.
- ✅ **Task & Decision Extractor**: Automatically identifies assignees, action items, and key decisions.
- 💬 **Interactive AI Chat with Data**: Ask any question about your chat history and receive context-aware answers.
- 📦 **Zip & TXT Export Parser**: Supports multi-line messages, date formats, and zip archive extraction.

---

## 📸 Usage

1. Open WhatsApp -> Tap **⋮** -> **More** -> **Export Chat**.
2. Select **ChatMind** from your phone's share menu.
3. Tap **Analyze AI** to generate your instant summary, tasks, and key insights!

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Database**: Room Database (SQLite)
- **Network**: Retrofit 2 + OkHttp 4 + KotlinX Serialization
- **Architecture**: MVVM (Model-View-ViewModel) + Coroutines & Flow

---

## 🤖 Built with AI Intelligence

This application was engineered, debugged, and optimized using **Antigravity AI** (Google DeepMind). All features—ranging from custom REST API payload serialization, Room database schema design, to Android Share Sheet intent handling—were crafted through pair programming with AI intelligence.

---

## 🚀 Building from Source

1. Clone this repository:
   ```bash
   git clone https://github.com/rishi-just/ChatMind.git
   ```
2. Open the project in Android Studio.
3. Build & run on your Android device (`./gradlew assembleDebug`).

---

## 📄 License

Distributed under the MIT License.
