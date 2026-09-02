package uz.nodirbek.flashcardsapp.shared.data.transfer

class AnkiImportException(message: String) : Exception(message)

/** Результат разбора .apkg: имя колоды -> карточки (front/back с очищенным HTML). */
data class AnkiImportResult(
    val deckName: String,
    val cards: List<FdeckCard>
)
