package uz.nodirbek.flashcardsapp.domain.usecase

import uz.nodirbek.flashcardsapp.domain.model.Card
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class GetForgettingEdgeCardsUseCase {

    enum class Urgency { HIGH, MEDIUM, LOW }

    data class ForgettingEdgeCard(
        val card: Card,
        val urgency: Urgency,
        val daysUntilForgotten: Long
    )

    operator fun invoke(cards: List<Card>): List<ForgettingEdgeCard> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val tomorrowStr = today.plusDays(1).format(formatter)

        return cards
            .filter { card ->
                card.reps > 1 && card.interval > 1 && card.dueDate <= tomorrowStr
            }
            .map { card ->
                val dueDate = LocalDate.parse(card.dueDate, formatter)
                val daysLeft = ChronoUnit.DAYS.between(today, dueDate)
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
