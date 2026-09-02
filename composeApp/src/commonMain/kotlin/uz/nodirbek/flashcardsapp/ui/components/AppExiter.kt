package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable

/**
 * Закрывает приложение — используется для паттерна "нажмите ещё раз для выхода" на Android.
 * На iOS такого паттерна нет (стандартный жест — домой), поэтому actual там no-op.
 */
@Composable
expect fun rememberAppExiter(): () -> Unit
