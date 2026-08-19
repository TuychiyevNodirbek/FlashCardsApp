package uz.nodirbek.flashcardsapp.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Thin wrapper over Android TextToSpeech driven by the language/speed set in Settings. */
actual class TtsManager(context: Context) {

    private var ready = false
    private val tts = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
    }

    actual fun speak(text: String, lang: String, speed: Float) {
        if (!ready || text.isBlank()) return
        tts.language = localeFor(lang)
        tts.setSpeechRate(speed)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "flashcard_tts")
    }

    actual fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    private fun localeFor(lang: String): Locale = when (lang) {
        "en" -> Locale.US
        "en-gb" -> Locale.UK
        "ru" -> Locale("ru", "RU")
        "de" -> Locale.GERMANY
        "es" -> Locale("es", "ES")
        "fr" -> Locale.FRANCE
        "it" -> Locale.ITALY
        "pt" -> Locale("pt", "PT")
        "zh" -> Locale.SIMPLIFIED_CHINESE
        "ja" -> Locale.JAPAN
        "ko" -> Locale.KOREA
        "ar" -> Locale("ar", "SA")
        "tr" -> Locale("tr", "TR")
        "la" -> Locale("la", "VA")
        else -> Locale.US
    }
}

@Composable
actual fun rememberTtsManager(): TtsManager {
    val context = LocalContext.current
    return remember { TtsManager(context) }
}
