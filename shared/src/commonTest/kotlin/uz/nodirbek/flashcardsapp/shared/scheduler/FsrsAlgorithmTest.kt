package uz.nodirbek.flashcardsapp.shared.scheduler

import kotlinx.datetime.Clock
import uz.nodirbek.flashcardsapp.shared.model.Card
import kotlin.test.Test
import kotlin.test.assertTrue

class FsrsAlgorithmTest {
    private val fsrs = FsrsAlgorithm()

    private fun newCard(
        dueDate: String = "2024-01-01",
        lastReviewed: String? = null,
        stability: Float = 0f,
        difficulty: Float = 0f,
        reps: Int = 0,
        lapses: Int = 0
    ) = Card(
        id = "test-id",
        front = "test front",
        back = "test back",
        dueDate = dueDate,
        lastReviewed = lastReviewed,
        createdAt = Clock.System.now().toEpochMilliseconds(),
        reps = reps,
        lapses = lapses,
        stability = stability,
        difficulty = difficulty
    )

    @Test
    fun `first review initializes stability and difficulty`() {
        val card = newCard()
        val result = fsrs.rate(card, quality = 2, todayDate = "2024-01-01") // Good

        assertTrue(result.stability > 0f, "Stability should be initialized")
        assertTrue(result.difficulty in 1f..10f, "Difficulty should be in [1,10], was ${result.difficulty}")
        assertTrue(result.reps == 1)
        assertTrue(result.lastReviewed == "2024-01-01")
    }

    @Test
    fun `again schedules card for today and increments lapses`() {
        val card = newCard()
        val result = fsrs.rate(card, quality = 0, todayDate = "2024-01-01") // Again

        assertTrue(result.interval == 0)
        assertTrue(result.dueDate == "2024-01-01")
        assertTrue(result.reps == 0)
        assertTrue(result.lapses == 1)
    }

    @Test
    fun `easy grade yields a longer or equal interval than good grade on first review`() {
        val goodCard = fsrs.rate(newCard(), quality = 2, todayDate = "2024-01-01")
        val easyCard = fsrs.rate(newCard(), quality = 3, todayDate = "2024-01-01")

        assertTrue(
            easyCard.interval >= goodCard.interval,
            "Easy interval (${easyCard.interval}) should be >= Good interval (${goodCard.interval})"
        )
    }

    @Test
    fun `repeated good reviews grow the interval`() {
        var card = newCard()
        var today = "2024-01-01"
        val intervals = mutableListOf<Int>()

        repeat(4) {
            card = fsrs.rate(card, quality = 2, todayDate = today) // Good
            intervals.add(card.interval)
            today = RateCardUseCase.addDays(today, card.interval)
        }

        for (i in 1 until intervals.size) {
            assertTrue(
                intervals[i] >= intervals[i - 1],
                "Interval should grow with successive Good reviews: $intervals"
            )
        }
    }

    @Test
    fun `forgetting a mature card resets interval but keeps accumulated stability lower than before`() {
        var card = newCard()
        card = fsrs.rate(card, quality = 2, todayDate = "2024-01-01")
        card = fsrs.rate(card, quality = 2, todayDate = RateCardUseCase.addDays("2024-01-01", card.interval))
        val stabilityBeforeLapse = card.stability

        card = fsrs.rate(card, quality = 0, todayDate = RateCardUseCase.addDays("2024-01-01", card.interval + 10))

        assertTrue(card.interval == 0)
        assertTrue(
            card.stability < stabilityBeforeLapse,
            "Stability after Again should drop below pre-lapse stability"
        )
    }

    @Test
    fun `difficulty stays within bounds across many reviews`() {
        var card = newCard()
        var today = "2024-01-01"
        repeat(10) { i ->
            val quality = if (i % 3 == 0) 0 else 2
            card = fsrs.rate(card, quality = quality, todayDate = today)
            assertTrue(card.difficulty in 1f..10f, "Difficulty out of bounds: ${card.difficulty}")
            today = RateCardUseCase.addDays(today, card.interval.coerceAtLeast(1))
        }
    }
}
