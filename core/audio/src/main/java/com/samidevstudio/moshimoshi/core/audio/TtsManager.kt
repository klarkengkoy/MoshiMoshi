package com.samidevstudio.moshimoshi.core.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.JAPANESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Japanese language is not supported")
            } else {
                isReady = true
                selectMaleVoice()
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    private fun selectMaleVoice() {
        tts?.voices?.let { voices ->
            // Try to find a male voice for Japanese
            val maleVoice = voices.find { voice ->
                voice.locale.language == "ja" && 
                (voice.name.contains("male", ignoreCase = true) || voice.name.contains("guy", ignoreCase = true))
            } ?: voices.find { it.locale.language == "ja" } // Fallback to any Japanese voice
            
            maleVoice?.let {
                tts?.voice = it
            }
        }
    }

    fun speak(text: String) {
        if (isReady) {
            // Simple detection: if contains Japanese characters, use Japanese, else English
            val hasJapanese = text.any { it.code in 0x3040..0x309F || it.code in 0x30A0..0x30FF || it.code in 0x4E00..0x9FFF }
            if (hasJapanese) {
                tts?.setLanguage(Locale.JAPANESE)
                selectMaleVoice()
            } else {
                tts?.setLanguage(Locale.ENGLISH)
            }
            
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutDown() {
        tts?.stop()
        tts?.shutdown()
    }
}