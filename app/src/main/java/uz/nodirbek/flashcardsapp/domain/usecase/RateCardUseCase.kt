package uz.nodirbek.flashcardsapp.domain.usecase

import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.usecase.algorithm.FsrsAlgorithm
import uz.nodirbek.flashcardsapp.domain.usecase.algorithm.SpacedRepetitionAlgorithm
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RateCardUseCase {
    operator fun invoke(
        card: Card,
        quality: Int,
        todayDate: String = getTodayDate(),
        algorithm: SpacedRepetitionAlgorithm = FsrsAlgorithm()
    ): Card = algorithm.rate(card, quality, todayDate)

    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd"
        private val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)

        fun getTodayDate(): String {
            return LocalDate.now().format(formatter)
        }

        fun addDays(dateStr: String, days: Int): String {
            val date = LocalDate.parse(dateStr, formatter)
            return date.plusDays(days.toLong()).format(formatter)
        }
    }
}
