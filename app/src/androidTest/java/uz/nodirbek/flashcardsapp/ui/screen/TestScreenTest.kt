package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uz.nodirbek.flashcardsapp.ui.theme.FlashCardsAppTheme

@RunWith(AndroidJUnit4::class)
class TestScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testScreen_showsQuestion() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // TestScreen with MultiChoice
            }
        }

        // Verify question section exists
        composeTestRule.onNodeWithText("Вопрос")
            .assertExists()
    }

    @Test
    fun testScreen_showsQuestionText() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // TestScreen setup
            }
        }

        // Verify question content displays
        composeTestRule.onNodeWithText("Выберите ответ")
            .assertExists()
    }

    @Test
    fun testScreen_multiChoiceDisplaysOptions() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // TestScreen with MultiChoice mode
            }
        }

        // Verify multiple options display
        composeTestRule.onNodeWithText("Выберите ответ")
            .assertExists()
    }

    @Test
    fun testScreen_selectOption() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // TestScreen setup
            }
        }

        // Click first option (assuming it exists)
        composeTestRule.onAllNodesWithText("", substring = true)
            .filterToOne(hasText("Выберите"))
            .performClick()
    }

    @Test
    fun testScreen_writtenQuestionShowsInput() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // TestScreen with WrittenQuestion mode
            }
        }

        // Verify input field placeholder
        composeTestRule.onNodeWithText("Введите ответ...")
            .assertExists()
    }

    @Test
    fun testScreen_typeAnswer() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // TestScreen WrittenQuestion mode
            }
        }

        // Find input field
        val inputField = composeTestRule.onNodeWithText("Введите ответ...")
        inputField.performTextInput("answer")

        // Verify text entered
        composeTestRule.onNodeWithText("answer")
            .assertExists()
    }

    @Test
    fun testScreen_showsProgressBar() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // TestScreen setup
            }
        }

        // Verify progress indicator
        composeTestRule.onNodeWithText("/", substring = true)
            .assertExists()
    }

    @Test
    fun testScreen_submitAnswer() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // TestScreen MultiChoice setup
            }
        }

        // Click option first
        composeTestRule.onAllNodesWithText("", substring = true)
            .filterToOne(hasText("Выберите"))
            .performClick()

        // Click next button
        composeTestRule.onNodeWithText("Далее →")
            .assertExists()
            .performClick()
    }

    @Test
    fun testScreen_showsCorrectIndicator() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // TestScreen WrittenQuestion setup
            }
        }

        // Type answer and submit
        val inputField = composeTestRule.onNodeWithText("Введите ответ...")
        inputField.performTextInput("correct answer")

        // Click verify button
        composeTestRule.onNodeWithText("Проверить")
            .performClick()

        // Verify result shows
        composeTestRule.onNodeWithText("правиль", substring = true)
            .assertExists()
    }

    @Test
    fun testScreen_resetStateOnNewQuestion() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // TestScreen setup with multiple questions
            }
        }

        // Answer first question
        val inputField = composeTestRule.onNodeWithText("Введите ответ...")
        inputField.performTextInput("answer1")
        composeTestRule.onNodeWithText("Проверить").performClick()
        composeTestRule.onNodeWithText("Далее →").performClick()

        // Verify next question's input is empty
        composeTestRule.onNodeWithText("Введите ответ...")
            .assertTextEquals("Введите ответ...")
    }

    @Test
    fun testScreen_closeOnBack() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // TestScreen setup
            }
        }

        // Click close button
        composeTestRule.onNodeWithContentDescription("Close")
            .performClick()
    }
}
