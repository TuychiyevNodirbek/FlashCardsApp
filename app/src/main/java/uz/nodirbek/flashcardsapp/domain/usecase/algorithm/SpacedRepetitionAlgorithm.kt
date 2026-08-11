package uz.nodirbek.flashcardsapp.domain.usecase.algorithm

import uz.nodirbek.flashcardsapp.domain.model.Card

/** Стратегия планирования повторений. quality: 0=Забыл, 1=Трудно, 2=Хорошо, 3=Легко. */
interface SpacedRepetitionAlgorithm {
    fun rate(card: Card, quality: Int, todayDate: String): Card
}
