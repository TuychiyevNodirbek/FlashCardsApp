package uz.nodirbek.flashcardsapp.ui.navigation

sealed class Screen(val route: String) {

    // ── Bottom-nav roots ──────────────────────────────────────────────
    object Home : Screen("home")
    object Stats : Screen("stats")
    object Settings : Screen("settings")

    // ── Deck detail ───────────────────────────────────────────────────
    object Deck : Screen("deck/{deckId}") {
        fun createRoute(deckId: String) = "deck/$deckId"
        const val ARG = "deckId"
    }

    // ── Study modes ───────────────────────────────────────────────────
    object SrsReview : Screen("study/srs/{deckId}") {
        fun createRoute(deckId: String) = "study/srs/$deckId"
        const val ARG = "deckId"
    }
    object ReviewDone : Screen("study/review-done")

    object Flashcards : Screen("study/flashcards/{deckId}") {
        fun createRoute(deckId: String) = "study/flashcards/$deckId"
        const val ARG = "deckId"
    }

    object TestSetup : Screen("study/test-setup/{deckId}") {
        fun createRoute(deckId: String) = "study/test-setup/$deckId"
        const val ARG = "deckId"
    }
    object Test : Screen("study/test/{deckId}") {
        fun createRoute(deckId: String) = "study/test/$deckId"
        const val ARG = "deckId"
    }
    object TestResults : Screen("study/test-results")

    object Match : Screen("study/match/{deckId}") {
        fun createRoute(deckId: String) = "study/match/$deckId"
        const val ARG = "deckId"
    }
    object MatchDone : Screen("study/match-done")

    object ForgettingEdge : Screen("study/forgetting/{deckId}") {
        fun createRoute(deckId: String) = "study/forgetting/$deckId"
        const val ARG = "deckId"
    }

    // ── Misc ──────────────────────────────────────────────────────────
    object Import : Screen("import")

    companion object {
        val bottomNavRoots = setOf(Home.route, Stats.route, Settings.route)
    }
}
