package uz.nodirbek.flashcardsapp.domain.usecase

import uz.nodirbek.flashcardsapp.domain.model.Card
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RateCardUseCase {
    operator fun invoke(card: Card, quality: Int, todayDate: String = getTodayDate()): Card {
        return when (quality) {
            0 -> rateAgain(card, todayDate)
            1 -> rateHard(card, todayDate)
            2 -> rateGood(card, todayDate)
            3 -> rateEasy(card, todayDate)
            else -> card
        }
    }

    private fun rateAgain(card: Card, todayDate: String): Card {
        return card.copy(
            reps = 0,
            interval = 0,
            ease = maxOf(1.3f, card.ease - 0.2f),
            dueDate = todayDate,
            lastReviewed = todayDate
        )
    }

    private fun rateHard(card: Card, todayDate: String): Card {
        val newReps = card.reps + 1
        val newInterval = when (newReps) {
            1 -> 1
            2 -> 3
            else -> (card.interval * card.ease).toInt()
        }
        val adjustedInterval = (newInterval * 0.8f).toInt().coerceAtLeast(1)
        val newDueDate = addDays(todayDate, adjustedInterval)

        return card.copy(
            reps = newReps,
            interval = adjustedInterval,
            ease = maxOf(1.3f, card.ease - 0.15f),
            dueDate = newDueDate,
            lastReviewed = todayDate
        )
    }

    private fun rateGood(card: Card, todayDate: String): Card {
        val newReps = card.reps + 1
        val newInterval = when (newReps) {
            1 -> 1
            2 -> 3
            else -> (card.interval * card.ease).toInt()
        }
        val newDueDate = addDays(todayDate, newInterval)

        return card.copy(
            reps = newReps,
            interval = newInterval,
            dueDate = newDueDate,
            lastReviewed = todayDate
        )
    }

    private fun rateEasy(card: Card, todayDate: String): Card {
        val newReps = card.reps + 1
        val newInterval = when (newReps) {
            1 -> 1
            2 -> 3
            else -> (card.interval * card.ease).toInt()
        }
        val easyInterval = (newInterval * 1.3f).toInt()
        val newDueDate = addDays(todayDate, easyInterval)

        return card.copy(
            reps = newReps,
            interval = easyInterval,
            ease = card.ease + 0.15f,
            dueDate = newDueDate,
            lastReviewed = todayDate
        )
    }

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
