package com.samidevstudio.moshimoshi.core.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import java.io.File

class GeminiService(apiKey: String) {
    private val systemInstruction = content {
        text("""
            You are 'Model Desu', a Japanese Conversation Tutor who interacts like a friendly colleague.
            Tone: Natural and conversational, suitable for workplace peers. Use polite Japanese (Desu/Masu form) but avoid overly stiff or formal honorifics unless appropriate for a standard office setting.
            Rules:
            1. Actively tutor the user. If they make a mistake in grammar or vocabulary, gently correct them as a helpful teammate would, explain why if necessary, and then continue the conversation naturally.
            2. Mirror the user's language complexity: If they speak in complete Japanese sentences, respond in complete Japanese. If they use full English, you can also respond in English, or mix it with Japanese when explaining. 
            3. Always use polite Japanese (Desu/Masu form) when talking in Japanese, but keep the vibe friendly and approachable.
            4. For Japanese text, use a natural mix of Kanji, Hiragana, and Katakana. Do not use Romaji unless specifically asked.
            5. For English text, use standard Alphabet.
            6. Your goal is to help the user learn and practice as a supportive peer. Be encouraging!
        """.trimIndent())
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
        systemInstruction = systemInstruction
    )

    private var chatSession = generativeModel.startChat()

    suspend fun processAudio(audioFile: File): String? {
        val audioBytes = audioFile.readBytes()
        
        val response = chatSession.sendMessage(
            content {
                blob("audio/mp4", audioBytes)
                text("User is speaking. Please listen, tutor/correct if needed, and respond naturally as a colleague.")
            }
        )
        return response.text
    }

    fun resetConversation() {
        chatSession = generativeModel.startChat()
    }
}
