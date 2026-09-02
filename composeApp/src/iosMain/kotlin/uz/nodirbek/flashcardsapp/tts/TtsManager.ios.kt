package uz.nodirbek.flashcardsapp.tts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.AVFAudio.AVSpeechUtteranceMaximumSpeechRate
import platform.AVFAudio.AVSpeechUtteranceMinimumSpeechRate

/** Thin wrapper over AVSpeechSynthesizer driven by the language/speed set in Settings. */
actual class TtsManager {

    private val synthesizer = AVSpeechSynthesizer()

    actual fun speak(text: String, lang: String, speed: Float) {
        if (text.isBlank()) return
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(localeFor(lang))
        utterance.rate = (AVSpeechUtteranceDefaultSpeechRate * speed)
            .coerceIn(AVSpeechUtteranceMinimumSpeechRate, AVSpeechUtteranceMaximumSpeechRate)
        synthesizer.speakUtterance(utterance)
    }

    actual fun shutdown() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    /** BCP-47 языковые теги, ожидаемые AVSpeechSynthesisVoice — тот же список языков, что и в Android-версии. */
    private fun localeFor(lang: String): String = when (lang) {
        "en" -> "en-US"
        "en-gb" -> "en-GB"
        "ru" -> "ru-RU"
        "de" -> "de-DE"
        "es" -> "es-ES"
        "fr" -> "fr-FR"
        "it" -> "it-IT"
        "pt" -> "pt-PT"
        "zh" -> "zh-CN"
        "ja" -> "ja-JP"
        "ko" -> "ko-KR"
        "ar" -> "ar-SA"
        "tr" -> "tr-TR"
        "la" -> "it-IT" // отдельного латинского голоса у AVSpeechSynthesizer нет
        else -> "en-US"
    }
}

@Composable
actual fun rememberTtsManager(): TtsManager = remember { TtsManager() }
