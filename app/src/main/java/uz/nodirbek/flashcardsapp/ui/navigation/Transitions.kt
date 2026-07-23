package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

// Единый easing для «премиального» ощущения
private val EaseOut = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EaseIn = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

// ── Общие переходы для экрана, который НАКРЫВАЮТ другим ──────────────
// Экран под новым остаётся на месте и слегка затухает,
// при возврате назад — плавно проявляется.
object UnderlyingScreenTransitions {
    fun exit(): ExitTransition = fadeOut(animationSpec = tween(250)) +
            scaleOut(targetScale = 0.96f, animationSpec = tween(350, easing = EaseOut))

    fun popEnter(): EnterTransition = fadeIn(animationSpec = tween(250)) +
            scaleIn(initialScale = 0.96f, animationSpec = tween(350, easing = EaseOut))
}

// ── BOTTOM NAV (быстро, просто) ──────────────────────────────────────
object BottomNavTransitions {
    fun enter(): EnterTransition = fadeIn(animationSpec = tween(200))
    fun exit(): ExitTransition = fadeOut(animationSpec = tween(200))
}

// ── SETTINGS / IMPORT (слайд справа) ──────────────────────────────────
object SideModalTransitions {
    fun enter(): EnterTransition = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(350, easing = EaseOut)
    ) + fadeIn(animationSpec = tween(350))

    fun popExit(): ExitTransition = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(300, easing = EaseIn)
    ) + fadeOut(animationSpec = tween(300))
}

// ── DECK DETAIL (слайд снизу) ─────────────────────────────────────────
object DeckDetailTransitions {
    fun enter(): EnterTransition = slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(400, easing = EaseOut)
    ) + fadeIn(animationSpec = tween(400))

    fun popExit(): ExitTransition = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(350, easing = EaseIn)
    ) + fadeOut(animationSpec = tween(350))
}

// ── STUDY / FLASHCARDS (масштаб + фейд) ──────────────────────────────
object StudyTransitions {
    fun enter(): EnterTransition = scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(400, easing = EaseOut)
    ) + fadeIn(animationSpec = tween(400))

    fun popExit(): ExitTransition = scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(300, easing = EaseIn)
    ) + fadeOut(animationSpec = tween(300))
}

// ── TEST / MATCH (слайд снизу наполовину + фейд) ─────────────────────
object GameSetupTransitions {
    fun enter(): EnterTransition = slideInVertically(
        initialOffsetY = { it / 2 },
        animationSpec = tween(400, easing = EaseOut)
    ) + fadeIn(animationSpec = tween(400))

    fun popExit(): ExitTransition = slideOutVertically(
        targetOffsetY = { it / 2 },
        animationSpec = tween(300, easing = EaseIn)
    ) + fadeOut(animationSpec = tween(300))
}

// ── RESULTS SCREENS (фейд + лёгкий масштаб) ──────────────────────────
object ResultsTransitions {
    fun enter(): EnterTransition = fadeIn(animationSpec = tween(350)) +
            scaleIn(initialScale = 1.05f, animationSpec = tween(350, easing = EaseOut))

    fun popExit(): ExitTransition = fadeOut(animationSpec = tween(250))
}

// ── FORGETTING EDGE (слайд снизу) ─────────────────────────────────────
object ForgettingEdgeTransitions {
    fun enter(): EnterTransition = slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(400, easing = EaseOut)
    ) + fadeIn(animationSpec = tween(400))

    fun popExit(): ExitTransition = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(350, easing = EaseIn)
    ) + fadeOut(animationSpec = tween(350))
}
