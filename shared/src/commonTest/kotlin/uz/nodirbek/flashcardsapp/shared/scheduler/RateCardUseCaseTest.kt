package uz.nodirbek.flashcardsapp.shared.scheduler

import kotlinx.datetime.Clock
import uz.nodirbek.flashcardsapp.shared.model.Card
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * RateCardUseCase — тонкий делегат к [SpacedRepetitionAlgorithm] (по умолчанию [FsrsAlgorithm]).
 * Сами формулы планирования (FSRS) проверяются в [FsrsAlgorithmTest] — здесь проверяем только
 * контракт делегирования: что RateCardUseCase передаёт card/quality/todayDate заданному
 * алгоритму и возвращает его результат как есть, без собственной логики.
 */
class RateCardUseCaseTest {
    private lateinit var rateCardUseCase: RateCardUseCase

    @BeforeTest
    fun setup() {
        rateCardUseCase = RateCardUseCase()
    }

    private fun createTestCard(
        dueDate: String = "2024-01-01"
    ) = Card(
        id = "test-id",
        front = "test front",
        back = "test back",
        dueDate = dueDate,
        createdAt = Clock.System.now().toEpochMilliseconds()
    )

    private class RecordingAlgorithm(private val result: Card) : SpacedRepetitionAlgorithm {
        var lastCard: Card? = null
        var lastQuality: Int? = null
        var lastTodayDate: String? = null

        override fun rate(card: Card, quality: Int, todayDate: String): Card {
            lastCard = card
            lastQuality = quality
            lastTodayDate = todayDate
            return result
        }
    }

    @Test
    fun testDelegatesArgumentsToProvidedAlgorithm() {
        val card = createTestCard()
        val expected = card.copy(reps = 42)
        val algorithm = RecordingAlgorithm(expected)

        rateCardUseCase(card, quality = 2, todayDate = "2024-03-10", algorithm = algorithm)

        assertEquals(card, algorithm.lastCard, "Card should be forwarded to the algorithm unchanged")
        assertEquals(2, algorithm.lastQuality, "Quality should be forwarded to the algorithm unchanged")
        assertEquals("2024-03-10", algorithm.lastTodayDate, "todayDate should be forwarded to the algorithm unchanged")
    }

    @Test
    fun testReturnsAlgorithmResultUnmodified() {
        val card = createTestCard()
        val expected = card.copy(reps = 7, interval = 99, dueDate = "2099-01-01")
        val algorithm = RecordingAlgorithm(expected)

        val result = rateCardUseCase(card, quality = 3, todayDate = "2024-03-10", algorithm = algorithm)

        assertEquals(expected, result, "RateCardUseCase should return the algorithm's result as-is")
    }

    @Test
    fun testDefaultAlgorithmIsFsrs() {
        val card = createTestCard()

        val viaUseCase = rateCardUseCase(card, quality = 2, todayDate = "2024-01-01")
        val viaFsrsDirectly = FsrsAlgorithm().rate(card, quality = 2, todayDate = "2024-01-01")

        assertEquals(viaFsrsDirectly, viaUseCase, "Without an explicit algorithm, RateCardUseCase should delegate to FsrsAlgorithm")
    }

    @Test
    fun testGetTodayDateReturnsIsoDate() {
        val today = RateCardUseCase.getTodayDate()

        assertTrue(
            Regex("""\d{4}-\d{2}-\d{2}""").matches(today),
            "getTodayDate() should return an ISO-8601 date, was: $today"
        )
    }

    @Test
    fun testAddDaysAddsCalendarDays() {
        assertEquals("2024-01-15", RateCardUseCase.addDays("2024-01-01", 14))
        assertEquals("2024-02-01", RateCardUseCase.addDays("2024-01-31", 1))
    }
}
