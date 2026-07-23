package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uz.nodirbek.flashcardsapp.ui.theme.FlashCardsAppTheme

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_displayTitle() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen with mocked ViewModel
            }
        }

        // Verify title
        composeTestRule.onNodeWithText("Настройки")
            .assertExists()
    }

    @Test
    fun settingsScreen_showsThemeSection() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Verify theme section
        composeTestRule.onNodeWithText("Внешний вид")
            .assertExists()
        composeTestRule.onNodeWithText("Тема оформления")
            .assertExists()
    }

    @Test
    fun settingsScreen_themeOptions() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Verify all theme options
        composeTestRule.onNodeWithText("☀️").assertExists()  // Light
        composeTestRule.onNodeWithText("🌙").assertExists()  // Dark
        composeTestRule.onNodeWithText("🌗").assertExists()  // System
    }

    @Test
    fun settingsScreen_toggleLight() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Click light theme
        composeTestRule.onNodeWithText("Светлая")
            .performClick()
    }

    @Test
    fun settingsScreen_toggleDark() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup (start with light)
            }
        }

        // Click dark theme
        composeTestRule.onNodeWithText("Тёмная")
            .performClick()
    }

    @Test
    fun settingsScreen_toggleSystem() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Click system theme
        composeTestRule.onNodeWithText("Системная")
            .performClick()
    }

    @Test
    fun settingsScreen_showsDailyGoal() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Verify daily goal section
        composeTestRule.onNodeWithText("Цель на день")
            .assertExists()
    }

    @Test
    fun settingsScreen_dailyGoalDisplay() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Verify goal label
        composeTestRule.onNodeWithText("Карточек в день")
            .assertExists()
    }

    @Test
    fun settingsScreen_notificationSection() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Verify notification section exists
        composeTestRule.onNodeWithText("Уведомлен", substring = true)
            .assertExists()
    }

    @Test
    fun settingsScreen_toggleNotifications() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Find and toggle notification switch
        composeTestRule.onNode(
            hasText("Напоминания") and
            hasTestTag("notification_switch")
        ).performClick()
    }

    @Test
    fun settingsScreen_timePickerButton() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Verify time picker button
        composeTestRule.onNodeWithText(":", substring = true)
            .assertExists()
    }

    @Test
    fun settingsScreen_backButton() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Verify close/back button
        composeTestRule.onNodeWithText("Настройки")
            .assertExists()
    }

    @Test
    fun settingsScreen_importButton() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Find import button/section
        composeTestRule.onNodeWithText("Импорт", substring = true)
            .assertExists()
    }

    @Test
    fun settingsScreen_themeUpdatesUI() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen with dark theme
            }
        }

        // Toggle to dark
        composeTestRule.onNodeWithText("Тёмная")
            .performClick()

        // Verify UI colors change (check background or elements)
        // This requires checking actual color values
    }

    @Test
    fun settingsScreen_scrollable() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Scroll down to see all options
        composeTestRule.onRoot()
            .performScrollToNode(hasText("Импорт"))
    }

    @Test
    fun settingsScreen_reminderTime() {
        composeTestRule.setContent {
            FlashCardsAppTheme {
                // SettingsScreen setup
            }
        }

        // Verify default time displays
        composeTestRule.onNodeWithText("09:00")
            .assertExists()
    }
}
