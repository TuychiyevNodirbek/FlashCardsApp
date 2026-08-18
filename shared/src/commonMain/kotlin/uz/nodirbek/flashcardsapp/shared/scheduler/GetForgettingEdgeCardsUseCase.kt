package uz.nodirbek.flashcardsapp.shared.scheduler

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import uz.nodirbek.flashcardsapp.shared.model.Card

class GetForgettingEdgeCardsUseCase {

    enum class Urgency { HIGH, MEDIUM, LOW }

    data class ForgettingEdgeCard(
        val card: Card,
        val urgency: Urgency,
        val daysUntilForgotten: Long
    )

    operator fun invoke(cards: List<Card>): List<ForgettingEdgeCard> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val tomorrowStr = today.plus(DatePeriod(days = 1)).toString()

        return cards
            .filter { card ->
                card.reps > 1 && card.interval > 1 && card.dueDate <= tomorrowStr
            }
            .map { card ->
                val dueDate = LocalDate.parse(card.dueDate)
                val daysLeft = today.daysUntil(dueDate).toLong()
                val urgency = when {
                    daysLeft <= 0 -> Urgency.HIGH
                    daysLeft == 1L -> Urgency.MEDIUM
                    else -> Urgency.LOW
                }
                ForgettingEdgeCard(card, urgency, daysLeft)
            }
            .sortedWith(compareBy({ it.urgency.ordinal }, { it.daysUntilForgotten }))
    }
}
