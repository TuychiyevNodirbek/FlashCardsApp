package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable

/** Android: системная кнопка/жест "назад" (androidx.activity.compose.BackHandler).
 *  iOS: пока no-op — свайп-назад будет отдельной задачей вместе со сборкой на Mac. */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
