package uz.nodirbek.flashcardsapp.tts

import androidx.compose.runtime.Composable

/** Озвучка слова на языке/скорости из настроек. Android — TextToSpeech, iOS — AVSpeechSynthesizer (TODO Фаза 6). */
expect class TtsManager {
    fun speak(text: String, lang: String, speed: Float)
    fun shutdown()
}

@Composable
expect fun rememberTtsManager(): TtsManager
