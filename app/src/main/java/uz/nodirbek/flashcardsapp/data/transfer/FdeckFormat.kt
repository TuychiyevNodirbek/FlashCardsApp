package uz.nodirbek.flashcardsapp.data.transfer

import kotlinx.serialization.Serializable

/**
 * Формат файла обмена колодами `.fdeck` (версия 1).
 * Обычный UTF-8 JSON. SRS-состояние намеренно не экспортируется —
 * получатель начинает учить с нуля. UUID перегенерируются при импорте.
 */
@Serializable
data class FdeckFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long = 0L,
    val appVersion: String = "",
    val deck: FdeckDeck
) {
    companion object {
        const val FORMAT = "fdeck"
        const val VERSION = 1
        const val EXTENSION = "fdeck"
    }
}

@Serializable
data class FdeckDeck(
    val id: String,
    val name: String,
    val colorHex: String = "#4255FF",
    val subRows: List<FdeckSubRow> = emptyList(),
    /** Прямые карточки курса (вне тем). */
    val cards: List<FdeckCard> = emptyList()
)

@Serializable
data class FdeckSubRow(
    val id: String,
    val name: String,
    val colorHex: String = "#4255FF",
    val sortOrder: Int = 0,
    val cards: List<FdeckCard> = emptyList()
)

@Serializable
data class FdeckCard(
    val id: String,
    val front: String,
    val back: String
)
