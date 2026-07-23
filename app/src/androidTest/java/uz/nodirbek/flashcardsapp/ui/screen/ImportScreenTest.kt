package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uz.nodirbek.flashcardsapp.ui.theme.FlashCardsAppTheme

@RunWith(AndroidJUnit4::class)
class ImportScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun importScreen_displayTitle() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen with mocked ViewModel
            }
        }

        // Verify title
        composeTestRule.onNodeWithText("Импорт карточек")
            .assertExists()
    }

    @Test
    fun importScreen_showsDropZone() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup
            }
        }

        // Verify drop zone
        composeTestRule.onNodeWithText("Нажмите, чтобы выбрать файл")
            .assertExists()
    }

    @Test
    fun importScreen_showsFormatInstructions() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup
            }
        }

        // Verify format section
        composeTestRule.onNodeWithText("Формат файла")
            .assertExists()
        composeTestRule.onNodeWithText("CSV, TSV — до 5000 строк")
            .assertExists()
    }

    @Test
    fun importScreen_showsDeckSelection() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup with decks
            }
        }

        // Verify deck selection
        composeTestRule.onNodeWithText("Выберите колоду")
            .assertExists()
    }

    @Test
    fun importScreen_selectDeck() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup
            }
        }

        // Find and click a deck option
        composeTestRule.onAllNodesWithText("карточек", substring = true)
            .filterToOne(hasClickAction())
            .performClick()
    }

    @Test
    fun importScreen_createNewDeck() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup
            }
        }

        // Click create deck button
        composeTestRule.onNodeWithText("+ Новая колода")
            .assertExists()
            .performClick()

        // Verify dialog appears
        composeTestRule.onNodeWithText("Создать новую колоду")
            .assertExists()
    }

    @Test
    fun importScreen_typeNewDeckName() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup
            }
        }

        // Click create deck
        composeTestRule.onNodeWithText("+ Новая колода")
            .performClick()

        // Type deck name
        composeTestRule.onNodeWithText("Название колоды")
            .performTextInput("Новая колода")

        // Verify text entered
        composeTestRule.onNodeWithText("Новая колода")
            .assertExists()
    }

    @Test
    fun importScreen_confirmCreateDeck() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup
            }
        }

        // Create deck
        composeTestRule.onNodeWithText("+ Новая колода")
            .performClick()

        composeTestRule.onNodeWithText("Название колоды")
            .performTextInput("Test Deck")

        // Click create button
        composeTestRule.onNodeWithText("Создать")
            .performClick()
    }

    @Test
    fun importScreen_importButtonDisabledNoDeck() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup without deck selected
            }
        }

        // Import button should be disabled
        composeTestRule.onNodeWithText("Выбрать файл")
            .assertIsNotEnabled()
    }

    @Test
    fun importScreen_importButtonEnabledWithDeck() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup
            }
        }

        // Select a deck
        composeTestRule.onAllNodesWithText("карточек", substring = true)
            .filterToOne(hasClickAction())
            .performClick()

        // Import button should be enabled
        composeTestRule.onNodeWithText("Выбрать файл")
            .assertIsEnabled()
    }

    @Test
    fun importScreen_showsSuccessMessage() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup with successful import
            }
        }

        // Verify success message
        composeTestRule.onNodeWithText("✅ Импорт успешен!")
            .assertExists()
    }

    @Test
    fun importScreen_showsImportedCount() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup
            }
        }

        // Verify count displays
        composeTestRule.onNodeWithText("Добавлено", substring = true)
            .assertExists()
    }

    @Test
    fun importScreen_closeAfterSuccess() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup with success
            }
        }

        // Click done button
        composeTestRule.onNodeWithText("Готово")
            .assertExists()
            .performClick()
    }

    @Test
    fun importScreen_showsErrorMessage() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup with error state
            }
        }

        // Verify error message
        composeTestRule.onNodeWithText("⚠️ Ошибка")
            .assertExists()
    }

    @Test
    fun importScreen_formatExample() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup
            }
        }

        // Verify format example shows
        composeTestRule.onNodeWithText("Пример CSV:")
            .assertExists()
    }

    @Test
    fun importScreen_backButton() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup
            }
        }

        // Click back button
        composeTestRule.onNodeWithContentDescription("Back")
            .performClick()
    }

    @Test
    fun importScreen_emptyDeckList() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // ImportScreen setup with no decks
            }
        }

        // Should still allow creating new deck
        composeTestRule.onNodeWithText("+ Новая колода")
            .assertExists()
    }
}
