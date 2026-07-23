package uz.nodirbek.flashcardsapp.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Thin wrapper over Android TextToSpeech driven by the language/speed set in Settings. */
class TtsManager(context: Context) {

    private var ready = false
    private val tts = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
    }

    fun speak(text: String, lang: String, speed: Float) {
        if (!ready || text.isBlank()) return
        tts.language = localeFor(lang)
        tts.setSpeechRate(speed)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "flashcard_tts")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    private fun localeFor(lang: String): Locale = when (lang) {
        "en" -> Locale.US
        "en-gb" -> Locale.UK
        "ru" -> Locale("ru", "RU")
        "de" -> Locale.GERMANY
        else -> Locale.US
    }
}
