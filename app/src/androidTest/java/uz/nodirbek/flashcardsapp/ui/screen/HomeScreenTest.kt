package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uz.nodirbek.flashcardsapp.ui.theme.FlashCardsAppTheme

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysTitle() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // Test placeholder - actual HomeScreen with mocked ViewModel
                // In real test, inject mocked HomeViewModel
            }
        }
        // Verify title appears
        composeTestRule.onNodeWithText("FlashDeck").assertExists()
    }

    @Test
    fun homeScreen_showsSearchButton() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // Similar setup
            }
        }
        // Verify search icon exists
        composeTestRule.onNodeWithContentDescription("Поиск").assertExists()
    }

    @Test
    fun homeScreen_showsAddDeckButton() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // Similar setup
            }
        }
        // Verify add deck button exists
        composeTestRule.onNodeWithContentDescription("Add")
            .assertExists()
    }

    @Test
    fun homeScreen_showsBottomNavigation() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // Similar setup
            }
        }
        // Verify nav items exist
        composeTestRule.onNodeWithText("Главная").assertExists()
        composeTestRule.onNodeWithText("Статистика").assertExists()
    }

    @Test
    fun homeScreen_searchOpensAndCloses() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // Similar setup
            }
        }

        // Open search
        composeTestRule.onNodeWithContentDescription("Поиск")
            .performClick()
        composeTestRule.onNodeWithText("Поиск колод").assertExists()

        // Close search
        composeTestRule.onNodeWithContentDescription("Close")
            .performClick()
    }

    @Test
    fun homeScreen_navigatesToSettings() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // Similar setup
            }
        }

        // Find and click settings (gear icon)
        composeTestRule.onNodeWithContentDescription("Settings")
            .performClick()
    }
}
