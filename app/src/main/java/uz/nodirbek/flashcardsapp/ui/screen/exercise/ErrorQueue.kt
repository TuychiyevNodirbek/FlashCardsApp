package uz.nodirbek.flashcardsapp.ui.screen.exercise

import uz.nodirbek.flashcardsapp.domain.model.Card

/**
 * Очередь карточек с повторением ошибочных.
 * Ошибочная карточка добавляется в конец очереди максимум 2 раза.
 * Accuracy считается только по первой попытке (totalPrimary).
 */
class ErrorQueue(cards: List<Card>) {
    private val queue: ArrayDeque<Card> = ArrayDeque(cards)
    private val retryCount = mutableMapOf<String, Int>()
    val totalPrimary: Int = cards.size

    fun next(): Card? = queue.removeFirstOrNull()

    fun isFirstAttempt(cardId: String): Boolean = (retryCount[cardId] ?: 0) == 0

    fun addError(card: Card) {
        val count = retryCount.getOrDefault(card.id, 0)
        if (count < 2) {
            queue.addLast(card)
            retryCount[card.id] = count + 1
        }
    }
}
