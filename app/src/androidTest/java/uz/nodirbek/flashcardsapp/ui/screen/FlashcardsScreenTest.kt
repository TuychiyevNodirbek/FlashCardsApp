package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uz.nodirbek.flashcardsapp.ui.theme.FlashCardsAppTheme

@RunWith(AndroidJUnit4::class)
class FlashcardsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun flashcardsScreen_displayCard() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // FlashcardsScreen with mocked data
            }
        }

        // Verify card front displays
        composeTestRule.onNodeWithText("ЛИЦО")
            .assertExists()
    }

    @Test
    fun flashcardsScreen_showsProgressCount() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // FlashcardsScreen setup
            }
        }

        // Verify progress like "Карточки 1/10"
        composeTestRule.onNodeWithText("Карточки", substring = true)
            .assertExists()
    }

    @Test
    fun flashcardsScreen_flipCard() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // FlashcardsScreen setup
            }
        }

        // Click card to flip
        composeTestRule.onNode(hasTestTag("card"))
            .performClick()

        // Verify back side with ОБОРОТ label
        composeTestRule.onNodeWithText("ОБОРОТ")
            .assertExists()
    }

    @Test
    fun flashcardsScreen_nextButton() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // FlashcardsScreen setup
            }
        }

        // Click next button
        composeTestRule.onNodeWithText("Вперёд →")
            .assertExists()
            .performClick()
    }

    @Test
    fun flashcardsScreen_previousButton() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // FlashcardsScreen setup with multiple cards
            }
        }

        // Go to next card first
        composeTestRule.onNodeWithText("Вперёд →")
            .performClick()

        // Previous button should be enabled
        composeTestRule.onNodeWithText("← Назад")
            .assertExists()
            .performClick()
    }

    @Test
    fun flashcardsScreen_lastCardShowsDialog() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // FlashcardsScreen setup - single card deck
            }
        }

        // On last card, click next/done
        composeTestRule.onNodeWithText("Готово")
            .performClick()

        // Should show dialog with options
        composeTestRule.onNodeWithText("Карточки пройдены")
            .assertExists()
    }

    @Test
    fun flashcardsScreen_restartCards() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // FlashcardsScreen single card
            }
        }

        // Click done on last card
        composeTestRule.onNodeWithText("Готово")
            .performClick()

        // Click "Заново"
        composeTestRule.onNodeWithText("Заново")
            .performClick()

        // Verify progress resets to 1
        composeTestRule.onNodeWithText("1 /", substring = true)
            .assertExists()
    }

    @Test
    fun flashcardsScreen_shuffleButton() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // FlashcardsScreen setup
            }
        }

        // Click shuffle icon
        composeTestRule.onNodeWithText("🔀")
            .assertExists()
            .performClick()
    }

    @Test
    fun flashcardsScreen_backButton() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // FlashcardsScreen setup
            }
        }

        // Click back button
        composeTestRule.onNodeWithContentDescription("Back")
            .performClick()
    }

    @Test
    fun flashcardsScreen_progressBar() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // FlashcardsScreen setup
            }
        }

        // Verify progress bar exists and is visible
        composeTestRule.onNode(
            hasTestTag("progress_bar") or
            hasContentDescription("Progress")
        ).assertExists()
    }

    @Test
    fun flashcardsScreen_emptyDeck() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // FlashcardsScreen with empty deck
            }
        }

        // Verify empty state
        composeTestRule.onNodeWithText("Нет карточек")
            .assertExists()
        composeTestRule.onNodeWithText("Добавьте карточки в колоду")
            .assertExists()
    }
}
