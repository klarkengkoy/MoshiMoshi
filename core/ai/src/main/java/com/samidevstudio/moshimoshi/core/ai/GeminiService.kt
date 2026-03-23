package com.samidevstudio.moshimoshi.core.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import java.io.File

class GeminiService(private val apiKey: String, initialModelName: String = "gemini-3.1-flash-lite-preview") {

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
            
            7. IMPORTANT - RESPONSE FORMAT: You MUST always provide your response in three distinct sections. Use these markers EXACTLY:
               START_NORMAL
               (Standard Japanese text here)
               END_NORMAL
               START_BASIC
               (Hiragana/Katakana only text here)
               END_BASIC
               START_ENGLISH
               (English translation here)
               END_ENGLISH

            8. TTS OPTIMIZATION: Your response will be read by a Text-to-Speech engine. In ALL sections, do NOT use emojis, emoticons, markdown (bolding/italics), or any parentheses. Keep the text clean so it sounds natural when spoken.
        """.trimIndent())
    }

    private var generativeModel = GenerativeModel(
        modelName = initialModelName,
        apiKey = apiKey,
        systemInstruction = systemInstruction
    )

    private var chatSession = generativeModel.startChat()

    fun updateModel(newModelName: String) {
        val history = chatSession.history
        
        generativeModel = GenerativeModel(
            modelName = newModelName,
            apiKey = apiKey,
            systemInstruction = systemInstruction
        )
        chatSession = generativeModel.startChat(history = history)
    }

    suspend fun processAudio(audioFile: File): String? {
        val audioBytes = audioFile.readBytes()
        
        val response = chatSession.sendMessage(
            content {
                blob("audio/mp4", audioBytes)
                text("User is speaking. Please listen, tutor/correct if needed, and respond using the START_NORMAL/END_NORMAL, START_BASIC/END_BASIC, and START_ENGLISH/END_ENGLISH format. Do NOT output anything other than these tags.")
            }
        )
        return response.text
    }

    fun resetConversation() {
        chatSession = generativeModel.startChat()
    }
}
