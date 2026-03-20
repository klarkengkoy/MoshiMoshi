# Project Plan

Create a single-screen Android Activity named "MoshiMoshi" using Jetpack Compose and the com.google.ai.client.generativeai SDK. The app is a language learning "buddy" for real-time voice-to-voice conversation with code-switching support (English/Japanese). It uses the gemini-2.5-flash-native-audio-preview-12-2025 model. Key features: Record audio on button press (16-bit PCM, 16kHz), send to Gemini, display transcript, and play audio response using AudioTrack. Material Design 3, Edge-to-Edge, and adaptive icon required.

## Project Brief

# Project Brief: MoshiMoshi

## Features
- **Real-Time Voice Interaction:** Seamless voice-to-voice conversation with an AI language buddy, supporting English and Japanese code-switching.
- **Advanced Audio Processing:** Captures high-quality 16-bit PCM audio at 16kHz and utilizes `AudioTrack` for responsive playback of AI-generated speech.
- **Gemini AI Integration:** Powered by the `gemini-2.5-flash-native-audio-preview-12-2025` model for natural and context-aware language learning.
- **Live Transcripts:** Displays real-time text transcriptions of the conversation to assist with comprehension and vocabulary building.
- **Modern Material UI:** A vibrant Material Design 3 interface featuring edge-to-edge display and an adaptive app icon.

## High-Level Technical Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Concurrency:** Kotlin Coroutines & Flow
- **AI SDK:** `com.google.ai.client.generativeai`
- **Audio APIs:** `AudioRecord` (Input) and `AudioTrack` (Output)
- **Code Generation:** KSP (Kotlin Symbol Processing)

## UI Design Image
![UI Design](C:/Users/HP/MoshiMoshi/input_images/image_0.png)
Image path = C:/Users/HP/MoshiMoshi/input_images/image_0.png

## Implementation Steps

### Task_1_Setup_Audio: Add Gemini SDK dependency and implement Audio Engine for 16kHz 16-bit PCM recording and playback.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - generativeai dependency added to build.gradle.kts
  - RECORD_AUDIO permission requested and handled
  - AudioRecord captures 16kHz 16-bit PCM data correctly
  - AudioTrack initialized for 16kHz 16-bit PCM playback
- **StartTime:** 2026-03-11 22:42:05 JST

### Task_2_Gemini_Service: Integrate Gemini 2.5 Flash Native Audio model and implement the ViewModel to coordinate streaming.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Gemini client configured with gemini-2.5-flash-native-audio-preview-12-2025
  - ViewModel manages audio stream flow to/from Gemini
  - Transcript state updated from Gemini text responses
  - Audio response from Gemini successfully routed to AudioTrack

### Task_3_UI_Development: Build the MoshiMoshi UI using Jetpack Compose with Material 3 and Edge-to-Edge support.
- **Status:** PENDING
- **Acceptance Criteria:**
  - UI implemented with Jetpack Compose and Material 3
  - Edge-to-Edge display enabled
  - UI matches the design provided in C:/Users/HP/MoshiMoshi/input_images/image_0.png
  - Record button and transcript display are fully functional

### Task_4_Final_Polish_Verification: Apply final theme refinements, create an adaptive icon, and perform a full system verification.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Vibrant Material 3 color scheme implemented
  - Adaptive app icon matching the language buddy theme created
  - Application stability verified (no crashes)
  - Project builds successfully and all existing tests pass
  - Critic agent confirms alignment with user requirements and UI fidelity

