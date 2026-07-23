package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.Alignment

// ── BOTTOM NAV (быстро, просто) ──────────────────────────────────────
object BottomNavTransitions {
    fun enter(): EnterTransition = fadeIn(animationSpec = tween(200))
    fun exit(): ExitTransition = fadeOut(animationSpec = tween(200))
}

// ── SETTINGS / IMPORT (слайд вправо) ──────────────────────────────────
object SideModalTransitions {
    fun enter(): EnterTransition = slideInHorizontally(
        initialOffsetX = { it },  // из правой стороны
        animationSpec = tween(400)
    ) + fadeIn(animationSpec = tween(400))

    fun exit(): ExitTransition = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(400)
    ) + fadeOut(animationSpec = tween(400))
}

// ── DECK DETAIL (слайд вверх) ─────────────────────────────────────────
object DeckDetailTransitions {
    fun enter(): EnterTransition = slideInVertically(
        initialOffsetY = { it },  // снизу вверх
        animationSpec = tween(500)
    ) + fadeIn(animationSpec = tween(500))

    fun exit(): ExitTransition = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(500)
    ) + fadeOut(animationSpec = tween(500))
}

// ── STUDY / FLASHCARDS (расширение + фейд, медленно) ────────────────
object StudyTransitions {
    fun enter(): EnterTransition = expandIn(
        expandFrom = Alignment.Center,
        animationSpec = tween(600)
    ) + fadeIn(animationSpec = tween(600))

    fun exit(): ExitTransition = shrinkOut(
        shrinkTowards = Alignment.Center,
        animationSpec = tween(600)
    ) + fadeOut(animationSpec = tween(600))
}

// ── TEST / MATCH SETUP (слайд вверх + фейд) ─────────────────────────
object GameSetupTransitions {
    fun enter(): EnterTransition = slideInVertically(
        initialOffsetY = { it / 2 },
        animationSpec = tween(500)
    ) + fadeIn(animationSpec = tween(500))

    fun exit(): ExitTransition = slideOutVertically(
        targetOffsetY = { it / 2 },
        animationSpec = tween(500)
    ) + fadeOut(animationSpec = tween(500))
}

// ── RESULTS SCREENS (масштаб + фейд, быстро) ──────────────────────────
object ResultsTransitions {
    fun enter(): EnterTransition = fadeIn(animationSpec = tween(400))

    fun exit(): ExitTransition = fadeOut(animationSpec = tween(400))
}

// ── FORGETTING EDGE (слайд вверх) ─────────────────────────────────────
object ForgettingEdgeTransitions {
    fun enter(): EnterTransition = slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(500)
    ) + fadeIn(animationSpec = tween(500))

    fun exit(): ExitTransition = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(500)
    ) + fadeOut(animationSpec = tween(500))
}
