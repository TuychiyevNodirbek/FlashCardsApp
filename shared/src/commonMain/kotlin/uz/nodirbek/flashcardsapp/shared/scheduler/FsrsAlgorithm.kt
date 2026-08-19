package uz.nodirbek.flashcardsapp.shared.scheduler

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import uz.nodirbek.flashcardsapp.shared.model.Card
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * FSRS-4.5 (Free Spaced Repetition Scheduler) — предсказывает вероятность вспомнить карточку
 * (retrievability) на основе "прочности" памяти (stability) и сложности карточки (difficulty),
 * и выбирает следующий интервал так, чтобы к моменту повторения retrievability была около
 * [REQUEST_RETENTION]. Точнее классического SM-2, т.к. учитывает фактическое время, прошедшее
 * с последнего повторения, а не только номер повторения.
 *
 * Стандартные веса FSRS-4.5 (w0..w18), опубликованы проектом open-spaced-repetition/fsrs4anki.
 */
class FsrsAlgorithm(
    private val requestRetention: Double = 0.9,
    private val w: DoubleArray = DEFAULT_WEIGHTS
) : SpacedRepetitionAlgorithm {

    override fun rate(card: Card, quality: Int, todayDate: String): Card {
        val grade = quality + 1 // FSRS: 1=Again, 2=Hard, 3=Good, 4=Easy

        val isFirstReview = card.lastReviewed == null
        val elapsedDays = if (isFirstReview) 0 else daysBetween(card.lastReviewed!!, todayDate).coerceAtLeast(0)

        val newDifficulty: Double
        val newStability: Double

        if (isFirstReview) {
            newDifficulty = initDifficulty(grade)
            newStability = initStability(grade)
        } else {
            val oldStability = card.stability.toDouble().coerceAtLeast(0.1)
            val oldDifficulty = card.difficulty.toDouble().let { if (it <= 0.0) initDifficulty(3) else it }
            val retrievability = retrievability(elapsedDays, oldStability)

            newDifficulty = nextDifficulty(oldDifficulty, grade)
            newStability = if (grade == 1) {
                nextForgetStability(oldDifficulty, oldStability, retrievability)
            } else {
                nextRecallStability(oldDifficulty, oldStability, retrievability, grade)
            }
        }

        val intervalDays = nextIntervalDays(newStability)
        val newDueDate = RateCardUseCase.addDays(todayDate, intervalDays)

        return card.copy(
            reps = card.reps + 1,
            interval = intervalDays,
            dueDate = newDueDate,
            lastReviewed = todayDate,
            lapses = if (grade == 1) card.lapses + 1 else card.lapses,
            stability = newStability.toFloat(),
            difficulty = newDifficulty.toFloat()
        ).let {
            // При "Забыл" карточка уходит на сегодня, как и в SM-2, но stability/difficulty
            // из FSRS сохраняются — они отражают долгосрочную память, а не сбрасываются.
            if (grade == 1) it.copy(interval = 0, dueDate = todayDate, reps = 0) else it
        }
    }

    // --- FSRS formulas ---

    private fun initStability(grade: Int): Double = w[grade - 1].coerceAtLeast(0.1)

    private fun initDifficulty(grade: Int): Double =
        (w[4] - exp(w[5] * (grade - 1)) + 1).coerceIn(1.0, 10.0)

    private fun nextDifficulty(oldDifficulty: Double, grade: Int): Double {
        val delta = -w[6] * (grade - 3)
        val d = oldDifficulty + delta * (10 - oldDifficulty) / 9
        val meanReversionTarget = initDifficulty(4)
        return (w[7] * meanReversionTarget + (1 - w[7]) * d).coerceIn(1.0, 10.0)
    }

    private fun retrievability(elapsedDays: Int, stability: Double): Double =
        (1 + elapsedDays / (9.0 * stability)).pow(-1.0)

    private fun nextRecallStability(difficulty: Double, stability: Double, retrievability: Double, grade: Int): Double {
        val hardPenalty = if (grade == 2) w[15] else 1.0
        val easyBonus = if (grade == 4) w[16] else 1.0
        val factor = exp(w[8]) *
            (11 - difficulty) *
            stability.pow(-w[9]) *
            (exp((1 - retrievability) * w[10]) - 1) *
            hardPenalty * easyBonus
        return stability * (1 + factor)
    }

    private fun nextForgetStability(difficulty: Double, stability: Double, retrievability: Double): Double {
        return w[11] *
            difficulty.pow(-w[12]) *
            ((stability + 1).pow(w[13]) - 1) *
            exp((1 - retrievability) * w[14])
    }

    private fun nextIntervalDays(stability: Double): Int {
        val days = (9.0 * stability * (1.0 / requestRetention - 1.0))
        return days.roundToInt().coerceIn(1, 36500)
    }

    private fun daysBetween(fromDate: String, toDate: String): Int {
        val a = LocalDate.parse(fromDate)
        val b = LocalDate.parse(toDate)
        return a.daysUntil(b)
    }

    companion object {
        /** Дефолтные веса FSRS-4.5 (w0..w18). */
        val DEFAULT_WEIGHTS = doubleArrayOf(
            0.4072, 1.1829, 3.1262, 15.4722, 7.2102, 0.5316, 1.0651, 0.0234, 1.616,
            0.1544, 1.0824, 1.9813, 0.0953, 0.2975, 2.2042, 0.2407, 2.9466, 0.5034, 0.6567
        )
    }
}
