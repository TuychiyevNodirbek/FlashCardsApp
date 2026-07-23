package uz.nodirbek.flashcardsapp.domain.usecase

import org.junit.Before
import org.junit.Test
import uz.nodirbek.flashcardsapp.domain.model.Card
import kotlin.math.roundToInt

class RateCardUseCaseTest {
    private lateinit var rateCardUseCase: RateCardUseCase

    @Before
    fun setup() {
        rateCardUseCase = RateCardUseCase()
    }

    private fun createTestCard(
        ease: Float = 2.5f,
        reps: Int = 0,
        interval: Int = 0,
        dueDate: String = "2024-01-01"
    ) = Card(
        id = "test-id",
        front = "test front",
        back = "test back",
        ease = ease,
        reps = reps,
        interval = interval,
        dueDate = dueDate,
        createdAt = System.currentTimeMillis()
    )

    // Tests for quality = 0 (Again/Wrong)
    @Test
    fun testAgainResets() {
        val card = createTestCard(ease = 2.5f, reps = 5, interval = 10)
        val result = rateCardUseCase(card, 0, "2024-01-15")

        assert(result.reps == 0) { "Reps should reset to 0" }
        assert(result.interval == 0) { "Interval should reset to 0" }
        assert(result.dueDate == "2024-01-15") { "Due date should be today" }
    }

    @Test
    fun testAgainDecreasesEase() {
        val card = createTestCard(ease = 2.5f)
        val result = rateCardUseCase(card, 0, "2024-01-15")

        val expectedEase = maxOf(1.3f, 2.5f - 0.2f)
        assert(result.ease == expectedEase) { "Ease should decrease by 0.2" }
    }

    @Test
    fun testAgainEaseDoesNotGoBelowMinimum() {
        val card = createTestCard(ease = 1.4f)
        val result = rateCardUseCase(card, 0, "2024-01-15")

        assert(result.ease == 1.3f) { "Ease should not go below 1.3" }
    }

    // Tests for quality = 1 (Hard)
    @Test
    fun testHardFirstReview() {
        val card = createTestCard(reps = 0, interval = 0, ease = 2.5f)
        val result = rateCardUseCase(card, 1, "2024-01-01")

        assert(result.reps == 1) { "Reps should be 1" }
        assert(result.interval == 1) { "First review interval should be 1" }
        assert(result.dueDate == "2024-01-02") { "Due date should be tomorrow" }
    }

    @Test
    fun testHardSecondReview() {
        val card = createTestCard(reps = 1, interval = 1, ease = 2.5f)
        val result = rateCardUseCase(card, 1, "2024-01-02")

        assert(result.reps == 2) { "Reps should be 2" }
        assert(result.interval == 2) { "Second review interval should be 3*0.8=2.4->2" }
        assert(result.dueDate == "2024-01-04") { "Due date should be 2 days later" }
    }

    @Test
    fun testHardDecreaseEase() {
        val card = createTestCard(ease = 2.5f, reps = 1)
        val result = rateCardUseCase(card, 1, "2024-01-02")

        val expectedEase = maxOf(1.3f, 2.5f - 0.15f)
        assert(result.ease == expectedEase) { "Ease should decrease by 0.15" }
    }

    @Test
    fun testHardThirdReview() {
        val card = createTestCard(reps = 2, interval = 3, ease = 2.5f)
        val result = rateCardUseCase(card, 1, "2024-01-05")

        val expectedInterval = (3 * 2.5).toInt()
        val adjustedInterval = (expectedInterval * 0.8).toInt()
        assert(result.reps == 3) { "Reps should be 3" }
        assert(result.interval == adjustedInterval) { "Interval should be (3*2.5)*0.8" }
    }

    // Tests for quality = 2 (Good)
    @Test
    fun testGoodFirstReview() {
        val card = createTestCard(reps = 0, interval = 0)
        val result = rateCardUseCase(card, 2, "2024-01-01")

        assert(result.reps == 1) { "Reps should be 1" }
        assert(result.interval == 1) { "First review interval should be 1" }
        assert(result.dueDate == "2024-01-02") { "Due date should be tomorrow" }
    }

    @Test
    fun testGoodSecondReview() {
        val card = createTestCard(reps = 1, interval = 1, ease = 2.5f)
        val result = rateCardUseCase(card, 2, "2024-01-02")

        assert(result.reps == 2) { "Reps should be 2" }
        assert(result.interval == 3) { "Second review interval should be 3" }
        assert(result.dueDate == "2024-01-05") { "Due date should be 3 days later" }
    }

    @Test
    fun testGoodDoesNotChangeEase() {
        val card = createTestCard(ease = 2.5f)
        val result = rateCardUseCase(card, 2, "2024-01-01")

        assert(result.ease == 2.5f) { "Ease should not change" }
    }

    @Test
    fun testGoodThirdReview() {
        val card = createTestCard(reps = 2, interval = 3, ease = 2.5f)
        val result = rateCardUseCase(card, 2, "2024-01-05")

        val expectedInterval = (3 * 2.5).toInt()
        assert(result.reps == 3) { "Reps should be 3" }
        assert(result.interval == expectedInterval) { "Interval should be 3*2.5" }
    }

    // Tests for quality = 3 (Easy)
    @Test
    fun testEasyFirstReview() {
        val card = createTestCard(reps = 0, interval = 0)
        val result = rateCardUseCase(card, 3, "2024-01-01")

        assert(result.reps == 1) { "Reps should be 1" }
        assert(result.interval == 1) { "First review interval should be 1" }
        assert(result.dueDate == "2024-01-02") { "Due date should be tomorrow" }
    }

    @Test
    fun testEasySecondReview() {
        val card = createTestCard(reps = 1, interval = 1, ease = 2.5f)
        val result = rateCardUseCase(card, 3, "2024-01-02")

        val expectedInterval = (3 * 1.3).toInt()
        assert(result.reps == 2) { "Reps should be 2" }
        assert(result.interval == expectedInterval) { "Interval should be 3*1.3" }
    }

    @Test
    fun testEasyIncreasesEase() {
        val card = createTestCard(ease = 2.5f, reps = 1)
        val result = rateCardUseCase(card, 3, "2024-01-02")

        assert(result.ease == 2.65f) { "Ease should increase by 0.15" }
    }

    @Test
    fun testEasyThirdReview() {
        val card = createTestCard(reps = 2, interval = 3, ease = 2.5f)
        val result = rateCardUseCase(card, 3, "2024-01-05")

        val baseInterval = (3 * 2.5).toInt()
        val easyInterval = (baseInterval * 1.3).toInt()
        assert(result.reps == 3) { "Reps should be 3" }
        assert(result.interval == easyInterval) { "Interval should be (3*2.5)*1.3" }
    }

    // Edge cases
    @Test
    fun testProgressionSequence() {
        var card = createTestCard()
        val today = "2024-01-01"

        // First review - Good
        card = rateCardUseCase(card, 2, today)
        assert(card.reps == 1)
        assert(card.interval == 1)

        // Second review - Good
        card = rateCardUseCase(card, 2, RateCardUseCase.addDays(today, 1))
        assert(card.reps == 2)
        assert(card.interval == 3)

        // Third review - Good
        card = rateCardUseCase(card, 2, RateCardUseCase.addDays(today, 4))
        assert(card.reps == 3)
        assert(card.interval == (3 * 2.5).toInt())
    }

    @Test
    fun testRecoveryFromWrong() {
        var card = createTestCard(reps = 5, interval = 30, ease = 2.5f)

        // Rate as wrong
        card = rateCardUseCase(card, 0, "2024-01-15")
        assert(card.reps == 0)
        assert(card.interval == 0)

        // Next review should restart
        card = rateCardUseCase(card, 2, "2024-01-15")
        assert(card.reps == 1)
        assert(card.interval == 1)
    }

    @Test
    fun testLastReviewedDateIsUpdated() {
        val card = createTestCard()
        val reviewDate = "2024-01-15"

        val result = rateCardUseCase(card, 2, reviewDate)

        assert(result.lastReviewed == reviewDate) { "Last reviewed date should be updated" }
    }
}
