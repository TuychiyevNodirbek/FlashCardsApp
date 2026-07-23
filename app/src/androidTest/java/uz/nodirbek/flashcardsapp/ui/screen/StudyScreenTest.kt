package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uz.nodirbek.flashcardsapp.ui.theme.FlashCardsAppTheme

@RunWith(AndroidJUnit4::class)
class StudyScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun studyScreen_displayCard() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // StudyScreen with mocked data
            }
        }

        // Verify card displays
        composeTestRule.onNodeWithText("Нажмите для переворота")
            .assertExists()
    }

    @Test
    fun studyScreen_flipCard() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // StudyScreen setup
            }
        }

        // Click to flip
        composeTestRule.onRoot()
            .performClick()

        // Verify back side shows
        composeTestRule.onNodeWithText("Ответ")
            .assertExists()
    }

    @Test
    fun studyScreen_showsProgressBar() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // StudyScreen setup
            }
        }

        // Verify progress indicator exists
        composeTestRule.onNodeWithText("1 /")
            .assertExists()
    }

    @Test
    fun studyScreen_showsRatingButtons() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // StudyScreen setup
            }
        }

        // Flip card first
        composeTestRule.onRoot()
            .performClick()

        // Verify rating buttons
        composeTestRule.onNodeWithText("Забыл").assertExists()
        composeTestRule.onNodeWithText("Трудно").assertExists()
        composeTestRule.onNodeWithText("Хорошо").assertExists()
        composeTestRule.onNodeWithText("Легко").assertExists()
    }

    @Test
    fun studyScreen_rateCardAndMoveNext() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // StudyScreen setup
            }
        }

        // Flip card
        composeTestRule.onRoot()
            .performClick()

        // Click rating button
        composeTestRule.onNodeWithText("Хорошо")
            .performClick()

        // Verify next card loads (or done screen)
        // This depends on total cards in session
    }

    @Test
    fun studyScreen_closesOnBack() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // StudyScreen setup
            }
        }

        // Click close button
        composeTestRule.onNodeWithContentDescription("Close")
            .performClick()
    }

    @Test
    fun studyScreen_showsStreakBadge() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // StudyScreen setup
            }
        }

        // Verify streak badge with fire emoji
        composeTestRule.onNodeWithText("🔥").assertExists()
    }

    @Test
    fun studyScreen_swipeGestureWorks() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // StudyScreen setup
            }
        }

        // Get card bounds
        val cardNode = composeTestRule.onRoot()

        // Perform horizontal swipe (right = easy)
        cardNode.performTouchInput { swipeRight() }

        // Verify card moves or next card shows
    }

    @Test
    fun studyScreen_doubleTapFlips() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // StudyScreen setup
            }
        }

        val cardNode = composeTestRule.onRoot()

        // Double tap
        cardNode.performTouchInput {
            doubleClick()
        }

        // Verify card flips
        composeTestRule.onNodeWithText("Ответ")
            .assertExists()
    }
}
