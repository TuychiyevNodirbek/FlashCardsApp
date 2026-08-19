package uz.nodirbek.flashcardsapp.tts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO Фаза 6: реализовать через AVSpeechSynthesizer при сборке на Mac.
actual class TtsManager {
    actual fun speak(text: String, lang: String, speed: Float) {}
    actual fun shutdown() {}
}

@Composable
actual fun rememberTtsManager(): TtsManager = remember { TtsManager() }
