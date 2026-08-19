package uz.nodirbek.flashcardsapp.shared.scheduler

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import uz.nodirbek.flashcardsapp.shared.model.Card

class RateCardUseCase {
    operator fun invoke(
        card: Card,
        quality: Int,
        todayDate: String = getTodayDate(),
        algorithm: SpacedRepetitionAlgorithm = FsrsAlgorithm()
    ): Card = algorithm.rate(card, quality, todayDate)

    companion object {
        fun getTodayDate(): String {
            return Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        }

        fun addDays(dateStr: String, days: Int): String {
            return LocalDate.parse(dateStr).plus(DatePeriod(days = days)).toString()
        }
    }
}
