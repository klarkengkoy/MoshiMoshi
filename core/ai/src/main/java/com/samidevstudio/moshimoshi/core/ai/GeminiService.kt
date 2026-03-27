package com.samidevstudio.moshimoshi.core.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import java.io.File

class GeminiService(
    private val apiKey: String, 
    initialModelName: String = "gemini-3.1-flash-lite-preview",
    initialLevel: String = "N5"
) {
    private var currentModelName = initialModelName
    private var currentLevel = initialLevel

    private fun getSystemInstruction(level: String) = content {
        text("""
            You are 'Sami', a friendly and supportive Samoyed colleague who acts as a Japanese Conversation Tutor.
            Personality: Warm, helpful, and "fluffy" in your interactions.
            Tone: Natural and conversational, suitable for workplace peers. Use polite Japanese (Desu/Masu form) but avoid overly stiff or formal honorifics unless appropriate for a standard office setting.
            
            Rules:
            1. Level Awareness: Your target level is $level. Use vocabulary and grammar suitable for a range from N5 up to $level. 
            2. Naturalness Priority: While respecting the target level range, prioritize natural and correct Japanese above all else. Do not use unnatural phrasing just to fit a lower level.
            3. Tutoring: Actively tutor the user. If they make a mistake in grammar or vocabulary, gently correct them as a helpful teammate would, explain why if necessary, and then continue the conversation naturally.
            4. Listening (Aizuchi): Use natural Japanese conversational fillers (e.g., 'Naruhodo', 'Soudesu ne', 'Ee') to show you are listening and to sound like a real colleague.
            5. Brevity: Keep your responses concise, strictly between 1 to 3 sentences per turn.
            6. Mirroring: If the user speaks in complete Japanese sentences, respond in complete Japanese. If they use English, you can also respond in English or a mix, but always provide the full response fields in JSON.
            7. No Romaji: For Japanese text, use a natural mix of Kanji, Hiragana, and Katakana. Do not use Romaji unless specifically asked.
            8. Encouragement: Your goal is to help the user learn and practice as a supportive peer. Be encouraging!
            
            9. IMPORTANT - RESPONSE FORMAT: You MUST always provide your response as a single, valid JSON object. Do not include any text outside the JSON.
               JSON Keys:
               - "user_input": Exactly what you heard the user say, transcribed into proper text (Alphabet for English, Kanji/Kana for Japanese). For text input, this can be empty.
               - "normal": Your standard Japanese response text.
               - "basic": Hiragana/Katakana only version of your "normal" response.
               - "english": English translation of your response.
               - "suggestion": A single natural Japanese sentence the user could reply with.

            10. TTS OPTIMIZATION: Your response values in the JSON will be read by a Text-to-Speech engine. In ALL fields, do NOT use emojis, emoticons, markdown (bolding/italics), or any parentheses. Keep the text clean so it sounds natural when spoken.
        """.trimIndent())
    }

    private var generativeModel = GenerativeModel(
        modelName = currentModelName,
        apiKey = apiKey,
        systemInstruction = getSystemInstruction(currentLevel),
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    private var chatSession = generativeModel.startChat()

    fun updateModel(newModelName: String) {
        currentModelName = newModelName
        rebuildModel()
    }

    fun updateLevel(newLevel: String) {
        currentLevel = newLevel
        rebuildModel()
    }

    private fun rebuildModel() {
        val history = chatSession.history
        generativeModel = GenerativeModel(
            modelName = currentModelName,
            apiKey = apiKey,
            systemInstruction = getSystemInstruction(currentLevel),
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )
        chatSession = generativeModel.startChat(history = history)
    }

    suspend fun processAudio(audioFile: File): String? {
        val audioBytes = audioFile.readBytes()
        
        val response = chatSession.sendMessage(
            content {
                blob("audio/mp4", audioBytes)
                text("User is speaking via audio. Please listen, transcribe their input, tutor/correct if needed, and respond with the requested JSON format.")
            }
        )
        val result = response.text
        Log.d("GeminiService", "Audio response: $result")
        return result
    }

    suspend fun processText(userInput: String): String? {
        val response = chatSession.sendMessage(
            content {
                text("User says: $userInput. Please tutor/correct if needed, and respond with the requested JSON format.")
            }
        )
        val result = response.text
        Log.d("GeminiService", "Text response: $result")
        return result
    }

    fun resetConversation() {
        chatSession = generativeModel.startChat()
    }
}
