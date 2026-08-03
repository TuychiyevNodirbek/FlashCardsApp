package uz.nodirbek.flashcardsapp.data.transfer

/**
 * Формат файла обмена колодами `.md` (версия 1).
 * Обычный человекочитаемый Markdown: `# Название` — курс, `## Название` — подтема,
 * `front :: back` — карточка. SRS-состояние намеренно не экспортируется —
 * получатель начинает учить с нуля. UUID перегенерируются при импорте.
 */
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
        const val EXTENSION = "md"
    }
}

data class FdeckDeck(
    val id: String = "",
    val name: String,
    val colorHex: String = "#4255FF",
    val subRows: List<FdeckSubRow> = emptyList(),
    val cards: List<FdeckCard> = emptyList()
)

data class FdeckSubRow(
    val id: String = "",
    val name: String,
    val colorHex: String = "#4255FF",
    val sortOrder: Int = 0,
    val cards: List<FdeckCard> = emptyList()
)

data class FdeckCard(
    val id: String = "",
    val front: String,
    val back: String
)
