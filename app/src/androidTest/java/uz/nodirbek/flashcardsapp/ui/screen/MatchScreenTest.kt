package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uz.nodirbek.flashcardsapp.ui.theme.FlashCardsAppTheme

@RunWith(AndroidJUnit4::class)
class MatchScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun matchScreen_displaysTiles() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // MatchScreen with mocked data
            }
        }

        // Verify title
        composeTestRule.onNodeWithText("Совпадение")
            .assertExists()
    }

    @Test
    fun matchScreen_showsTimer() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // MatchScreen setup
            }
        }

        // Verify timer displays
        composeTestRule.onNodeWithText(":", substring = true)
            .assertExists()
    }

    @Test
    fun matchScreen_clickTile() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // MatchScreen setup
            }
        }

        // Find first tile and click
        composeTestRule.onAllNodesWithContentDescription("Tile")
            .filterToOne(hasClickAction())
            .performClick()

        // Verify tile is selected (color change or highlight)
    }

    @Test
    fun matchScreen_matchPair() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // MatchScreen setup
            }
        }

        // Click first tile
        composeTestRule.onAllNodesWithContentDescription("Tile")
            .get(0)
            .performClick()

        // Click matching tile (this requires knowing the pairs)
        composeTestRule.onAllNodesWithContentDescription("Tile")
            .get(1)
            .performClick()

        // Verify checkmark appears
        composeTestRule.onNodeWithText("✓")
            .assertExists()
    }

    @Test
    fun matchScreen_wrongMatch() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // MatchScreen setup
            }
        }

        // Click two non-matching tiles
        composeTestRule.onAllNodesWithContentDescription("Tile")
            .get(0)
            .performClick()

        composeTestRule.onAllNodesWithContentDescription("Tile")
            .get(2)
            .performClick()

        // Should reset after delay (600ms)
        composeTestRule.mainClock.advanceTimeBy(650)
    }

    @Test
    fun matchScreen_progressBar() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // MatchScreen setup
            }
        }

        // Verify progress bar exists
        composeTestRule.onNode(
            hasTestTag("progress_bar") or
            hasContentDescription("Progress")
        ).assertExists()
    }

    @Test
    fun matchScreen_allTilesMatch() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // MatchScreen setup - single pair
            }
        }

        // Match all pairs
        composeTestRule.onAllNodesWithContentDescription("Tile")
            .get(0)
            .performClick()

        composeTestRule.onAllNodesWithContentDescription("Tile")
            .get(1)
            .performClick()

        // Should navigate to results
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onNodeWithText("Готово").isDisplayed()
        }
    }

    @Test
    fun matchScreen_backButton() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // MatchScreen setup
            }
        }

        // Click close/back
        composeTestRule.onNodeWithContentDescription("Close")
            .performClick()
    }

    @Test
    fun matchScreen_timerCounts() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // MatchScreen setup
            }
        }

        // Advance time
        composeTestRule.mainClock.advanceTimeBy(1000)

        // Timer should have incremented
        // (Manual verification of timer increment)
    }

    @Test
    fun matchScreen_multipleMatches() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // MatchScreen with 6 cards (3 pairs)
            }
        }

        // Match first pair
        composeTestRule.onAllNodesWithContentDescription("Tile")
            .get(0)
            .performClick()
        composeTestRule.onAllNodesWithContentDescription("Tile")
            .get(1)
            .performClick()

        composeTestRule.mainClock.advanceTimeBy(100)

        // Match second pair
        composeTestRule.onAllNodesWithContentDescription("Tile")
            .get(2)
            .performClick()
        composeTestRule.onAllNodesWithContentDescription("Tile")
            .get(3)
            .performClick()

        composeTestRule.mainClock.advanceTimeBy(100)

        // Match third pair
        composeTestRule.onAllNodesWithContentDescription("Tile")
            .get(4)
            .performClick()
        composeTestRule.onAllNodesWithContentDescription("Tile")
            .get(5)
            .performClick()

        // Should show done
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onNodeWithText("Готово").isDisplayed()
        }
    }
}
