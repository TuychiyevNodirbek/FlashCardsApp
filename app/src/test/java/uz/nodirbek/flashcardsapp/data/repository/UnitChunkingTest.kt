package uz.nodirbek.flashcardsapp.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.nodirbek.flashcardsapp.domain.model.Card

class UnitChunkingTest {

    private fun cards(n: Int): List<Card> = List(n) { i ->
        Card(
            id = "card-$i",
            deckId = "deck",
            front = "front-$i",
            back = "back-$i",
            dueDate = "2026-01-01",
            createdAt = i.toLong()
        )
    }

    @Test
    fun `empty list gives no chunks`() {
        assertEquals(emptyList<List<Card>>(), UnitRepository.buildChunks(cards(0)))
    }

    @Test
    fun `sizes are balanced and capped at 10`() {
        val cases = mapOf(
            1 to listOf(1),
            4 to listOf(4),
            10 to listOf(10),
            11 to listOf(6, 5),
            23 to listOf(8, 8, 7),
            30 to listOf(10, 10, 10),
            95 to listOf(10, 10, 10, 10, 10, 9, 9, 9, 9, 9)
        )
        cases.forEach { (n, expectedSizes) ->
            val chunks = UnitRepository.buildChunks(cards(n))
            assertEquals("n=$n", expectedSizes, chunks.map { it.size })
            chunks.forEach { chunk ->
                assertTrue("n=$n: chunk size ${chunk.size} > 10", chunk.size <= UnitRepository.UNIT_SIZE)
            }
        }
    }

    @Test
    fun `sum and order preserved`() {
        for (n in listOf(1, 4, 10, 11, 23, 30, 95)) {
            val input = cards(n)
            val chunks = UnitRepository.buildChunks(input)
            assertEquals("n=$n: sum", n, chunks.sumOf { it.size })
            assertEquals("n=$n: order", input, chunks.flatten())
        }
    }

    @Test
    fun `chunk sizes differ by at most one`() {
        for (n in 1..100) {
            val sizes = UnitRepository.buildChunks(cards(n)).map { it.size }
            val max = sizes.max()
            val min = sizes.min()
            assertTrue("n=$n: sizes $sizes differ by more than 1", max - min <= 1)
        }
    }
}
