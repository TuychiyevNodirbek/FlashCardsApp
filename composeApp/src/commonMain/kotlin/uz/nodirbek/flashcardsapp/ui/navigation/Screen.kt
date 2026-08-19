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

    object Flashcards : Screen("study/flashcards/{deckId}") {
        fun createRoute(deckId: String) = "study/flashcards/$deckId"
        const val ARG = "deckId"
    }

    object TestSetup : Screen("study/test-setup/{deckId}") {
        fun createRoute(deckId: String) = "study/test-setup/$deckId"
        const val ARG = "deckId"
    }
    object Test : Screen("study/test/{deckId}?count={count}&written={written}") {
        fun createRoute(deckId: String, count: Int, isWritten: Boolean) =
            "study/test/$deckId?count=$count&written=$isWritten"
        const val ARG = "deckId"
        const val ARG_COUNT = "count"
        const val ARG_WRITTEN = "written"
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

    // ── Unit Flow ─────────────────────────────────────────────────────
    object UnitFlow : Screen("unit-flow/{deckId}/{unitIndex}") {
        fun createRoute(deckId: String, unitIndex: Int) = "unit-flow/$deckId/$unitIndex"
        const val ARG_DECK = "deckId"
        const val ARG_UNIT = "unitIndex"
    }
    object UnitResult : Screen("unit-result/{deckId}/{unitIndex}/{correct}/{total}/{xp}") {
        fun createRoute(deckId: String, unitIndex: Int, correct: Int, total: Int, xp: Int = 0) =
            "unit-result/$deckId/$unitIndex/$correct/$total/$xp"
        const val ARG_DECK = "deckId"
        const val ARG_UNIT = "unitIndex"
        const val ARG_CORRECT = "correct"
        const val ARG_TOTAL = "total"
        const val ARG_XP = "xp"
    }

    // ── Misc ──────────────────────────────────────────────────────────
    object RecentlyDeleted : Screen("recently-deleted")
    object Import : Screen("import")
    object BrowseSource : Screen("browse-source")
    object AnkiWebBrowse : Screen("anki-web-browse")
    object OpenTDBBrowse : Screen("opentdb-browse")

    companion object {
        val bottomNavRoots = setOf(Home.route, Stats.route, Settings.route)
    }
}
