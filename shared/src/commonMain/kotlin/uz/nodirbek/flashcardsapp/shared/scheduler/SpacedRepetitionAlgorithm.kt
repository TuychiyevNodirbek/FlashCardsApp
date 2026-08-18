package uz.nodirbek.flashcardsapp.shared.scheduler

import uz.nodirbek.flashcardsapp.shared.model.Card

/** Стратегия планирования повторений. quality: 0=Забыл, 1=Трудно, 2=Хорошо, 3=Легко. */
interface SpacedRepetitionAlgorithm {
    fun rate(card: Card, quality: Int, todayDate: String): Card
}
