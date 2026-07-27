package uz.nodirbek.flashcardsapp.domain.model

data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String
)

object Achievements {
    val all = listOf(
        Achievement("streak_7",       "🔥", "Неделя подряд",    "7 дней серии"),
        Achievement("streak_30",      "🏆", "Месячный марафон", "30 дней серии"),
        Achievement("cards_100",      "💯", "Сотня",            "100 повторений"),
        Achievement("cards_1000",     "🌟", "Тысяча",           "1000 повторений"),
        Achievement("perfect_session","⚡", "Идеальная сессия", "100% точность в сессии ≥10 карт"),
        Achievement("early_bird",     "🌅", "Ранняя пташка",    "Повторение до 8:00"),
        Achievement("night_owl",      "🦉", "Сова",             "Повторение после 22:00"),
        Achievement("deck_master",    "🎓", "Мастер деки",      "Все карты деки interval ≥ 21"),
        Achievement("lapse_recover",  "💪", "Пересилил",        "Сложная карточка перешла в интервал ≥ 7")
    )

    fun findById(id: String) = all.find { it.id == id }
}
