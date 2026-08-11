package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import uz.nodirbek.flashcardsapp.AppContainer
import uz.nodirbek.flashcardsapp.ui.screen.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    container: AppContainer
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in Screen.bottomNavRoots

    // State for match done params
    var matchTimeSeconds by remember { mutableIntStateOf(0) }
    // State for test results (сложный payload — через route не передать)
    var testResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }

    val hazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {}
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .haze(hazeState)
        ) {

            // ── Bottom-nav roots ─────────────────────────────────────────
            composable(
                Screen.Home.route,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                HomeScreen(
                    viewModel = homeViewModel,
                    deckTransferRepository = container.deckTransferRepository,
                    onNavigateToStudy = { navController.navigate(Screen.SrsReview.createRoute(HomeViewModel.ALL_DECKS)) },
                    onNavigateToImport = { navController.navigate(Screen.Import.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToDeck = { deckId -> navController.navigate(Screen.Deck.createRoute(deckId)) }
                )
            }

            composable(
                Screen.Stats.route,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                StatsScreen(viewModel = homeViewModel)
            }

            composable(
                Screen.Settings.route,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                SettingsScreen(
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() },
                    onImportClick = { navController.navigate(Screen.Import.route) },
                    onNavigateToDeleted = { navController.navigate(Screen.RecentlyDeleted.route) }
                )
            }

            // ── Deck detail ──────────────────────────────────────────────
            composable(
                route = Screen.Deck.route,
                arguments = listOf(navArgument(Screen.Deck.ARG) { type = NavType.StringType }),
                enterTransition = { DeckDetailTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { DeckDetailTransitions.popExit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.Deck.ARG) ?: return@composable
                DeckScreen(
                    deckId = deckId,
                    viewModel = homeViewModel,
                    unitRepository = container.unitRepository,
                    onBack = { navController.popBackStack() },
                    onNavigateToSrs = { id -> navController.navigate(Screen.SrsReview.createRoute(id)) },
                    onNavigateToFlashcards = { id -> navController.navigate(Screen.Flashcards.createRoute(id)) },
                    onNavigateToTestSetup = { id -> navController.navigate(Screen.TestSetup.createRoute(id)) },
                    onNavigateToMatch = { id -> navController.navigate(Screen.Match.createRoute(id)) },
                    onNavigateToForgetting = { id -> navController.navigate(Screen.ForgettingEdge.createRoute(id)) },
                    onOpenUnit = { subRowDeckId, unitIndex ->
                        navController.navigate(Screen.UnitFlow.createRoute(subRowDeckId, unitIndex))
                    }
                )
            }

            // ── SRS Review ───────────────────────────────────────────────
            composable(
                route = Screen.SrsReview.route,
                arguments = listOf(navArgument(Screen.SrsReview.ARG) { type = NavType.StringType }),
                enterTransition = { StudyTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { StudyTransitions.popExit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.SrsReview.ARG) ?: return@composable
                StudyScreen(
                    viewModel = homeViewModel,
                    deckId = deckId,
                    onBackClick = { navController.popBackStack() },
                    onSessionDone = { _, _, _ ->
                        // done screen is inlined in StudyScreen; this triggers after it
                    }
                )
            }

            // ── Flashcards ───────────────────────────────────────────────
            composable(
                route = Screen.Flashcards.route,
                arguments = listOf(navArgument(Screen.Flashcards.ARG) { type = NavType.StringType }),
                enterTransition = { StudyTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { StudyTransitions.popExit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.Flashcards.ARG) ?: return@composable
                FlashcardsScreen(
                    deckId = deckId,
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ── Test Setup → Test → Results ──────────────────────────────
            composable(
                route = Screen.TestSetup.route,
                arguments = listOf(navArgument(Screen.TestSetup.ARG) { type = NavType.StringType }),
                enterTransition = { GameSetupTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { GameSetupTransitions.popExit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.TestSetup.ARG) ?: return@composable
                TestSetupScreen(
                    deckId = deckId,
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() },
                    onStartTest = { id, count, isWritten ->
                        navController.navigate(Screen.Test.createRoute(id, count, isWritten))
                    }
                )
            }

            composable(
                route = Screen.Test.route,
                arguments = listOf(
                    navArgument(Screen.Test.ARG) { type = NavType.StringType },
                    navArgument(Screen.Test.ARG_COUNT) { type = NavType.IntType; defaultValue = 10 },
                    navArgument(Screen.Test.ARG_WRITTEN) { type = NavType.BoolType; defaultValue = false }
                ),
                enterTransition = { GameSetupTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { GameSetupTransitions.popExit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.Test.ARG) ?: return@composable
                TestScreen(
                    deckId = deckId,
                    viewModel = homeViewModel,
                    count = backStackEntry.arguments?.getInt(Screen.Test.ARG_COUNT) ?: 10,
                    isWritten = backStackEntry.arguments?.getBoolean(Screen.Test.ARG_WRITTEN) ?: false,
                    onBackClick = { navController.popBackStack() },
                    onFinished = { results ->
                        testResults = results
                        navController.navigate(Screen.TestResults.route) {
                            popUpTo(Screen.Test.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                Screen.TestResults.route,
                enterTransition = { ResultsTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { ResultsTransitions.popExit() }
            ) {
                TestResultsScreen(
                    results = testResults,
                    onDone = { navController.popBackStack() },
                    onRepeatMistakes = { navController.popBackStack() }
                )
            }

            // ── Match → Done ─────────────────────────────────────────────
            composable(
                route = Screen.Match.route,
                arguments = listOf(navArgument(Screen.Match.ARG) { type = NavType.StringType }),
                enterTransition = { GameSetupTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { GameSetupTransitions.popExit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.Match.ARG) ?: return@composable
                MatchScreen(
                    deckId = deckId,
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() },
                    onFinished = { seconds, _, _ ->
                        matchTimeSeconds = seconds
                        navController.navigate(Screen.MatchDone.route) {
                            popUpTo(Screen.Match.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                Screen.MatchDone.route,
                enterTransition = { ResultsTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { ResultsTransitions.popExit() }
            ) {
                MatchDoneScreen(
                    timeSeconds = matchTimeSeconds,
                    isNewRecord = false,
                    bestSeconds = matchTimeSeconds,
                    onPlayAgain = { navController.popBackStack() },
                    onDone = {
                        navController.popBackStack(Screen.Deck.route, inclusive = false)
                    }
                )
            }

            // ── Forgetting Edge ──────────────────────────────────────────
            composable(
                route = Screen.ForgettingEdge.route,
                arguments = listOf(navArgument(Screen.ForgettingEdge.ARG) { type = NavType.StringType }),
                enterTransition = { ForgettingEdgeTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { ForgettingEdgeTransitions.popExit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.ForgettingEdge.ARG) ?: return@composable
                ForgettingEdgeScreen(
                    deckId = deckId,
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() },
                    onStartReview = { id -> navController.navigate(Screen.SrsReview.createRoute(id)) }
                )
            }

            // ── Unit Flow ────────────────────────────────────────────────
            composable(
                route = Screen.UnitFlow.route,
                arguments = listOf(
                    navArgument(Screen.UnitFlow.ARG_DECK) { type = NavType.StringType },
                    navArgument(Screen.UnitFlow.ARG_UNIT) { type = NavType.IntType }
                ),
                enterTransition = { StudyTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { StudyTransitions.popExit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.UnitFlow.ARG_DECK) ?: return@composable
                val idx = backStackEntry.arguments?.getInt(Screen.UnitFlow.ARG_UNIT) ?: return@composable
                val ttsState by homeViewModel.uiState.collectAsState()
                UnitFlowScreen(
                    deckId = deckId,
                    unitIndex = idx,
                    unitRepository = container.unitRepository,
                    cardRepository = container.cardRepository,
                    statsRepository = container.statsRepository,
                    rateCardUseCase = container.rateCardUseCase,
                    ttsLang = ttsState.ttsLang,
                    ttsSpeed = ttsState.ttsSpeed,
                    onBack = { navController.popBackStack() },
                    onFinished = { correct, total ->
                        val xpEarned = homeViewModel.addXpForUnit(correct, total)
                        navController.navigate(Screen.UnitResult.createRoute(deckId, idx, correct, total, xpEarned)) {
                            popUpTo(Screen.UnitFlow.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Unit Result ──────────────────────────────────────────────
            composable(
                route = Screen.UnitResult.route,
                arguments = listOf(
                    navArgument(Screen.UnitResult.ARG_DECK) { type = NavType.StringType },
                    navArgument(Screen.UnitResult.ARG_UNIT) { type = NavType.IntType },
                    navArgument(Screen.UnitResult.ARG_CORRECT) { type = NavType.IntType },
                    navArgument(Screen.UnitResult.ARG_TOTAL) { type = NavType.IntType },
                    navArgument(Screen.UnitResult.ARG_XP) { type = NavType.IntType; defaultValue = 0 }
                ),
                enterTransition = { ResultsTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { ResultsTransitions.popExit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.UnitResult.ARG_DECK) ?: return@composable
                val unitIndex = backStackEntry.arguments?.getInt(Screen.UnitResult.ARG_UNIT) ?: return@composable
                val correct = backStackEntry.arguments?.getInt(Screen.UnitResult.ARG_CORRECT) ?: 0
                val total = backStackEntry.arguments?.getInt(Screen.UnitResult.ARG_TOTAL) ?: 0
                val xpEarned = backStackEntry.arguments?.getInt(Screen.UnitResult.ARG_XP) ?: 0
                val units by container.unitRepository.getUnits(deckId)
                    .collectAsState(initial = null)
                UnitResultScreen(
                    unitIndex = unitIndex,
                    correctAnswers = correct,
                    totalAnswers = total,
                    xpEarned = xpEarned,
                    hasNextUnit = units?.let { it.size > unitIndex + 1 } ?: false,
                    onBackToUnits = {
                        navController.popBackStack(Screen.Deck.route, inclusive = false)
                    },
                    onNextUnit = {
                        navController.navigate(Screen.UnitFlow.createRoute(deckId, unitIndex + 1)) {
                            popUpTo(Screen.UnitResult.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Recently Deleted ──────────────────────────────────────────────
            composable(
                Screen.RecentlyDeleted.route,
                enterTransition = { SideModalTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { SideModalTransitions.popExit() }
            ) {
                RecentlyDeletedScreen(
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ── Import ────────────────────────────────────────────────────
            composable(
                Screen.Import.route,
                enterTransition = { SideModalTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { SideModalTransitions.popExit() }
            ) {
                ImportScreen(
                    viewModel = homeViewModel,
                    deckTransferRepository = container.deckTransferRepository,
                    onCardsImported = { cards ->
                        homeViewModel.addCards(cards)
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() },
                    onBrowseAnkiWeb = { navController.navigate(Screen.BrowseSource.route) }
                )
            }

            // ── Выбор источника колод ─────────────────────────────────────
            composable(
                Screen.BrowseSource.route,
                enterTransition = { SideModalTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { SideModalTransitions.popExit() }
            ) {
                BrowseSourceScreen(
                    onBackClick = { navController.popBackStack() },
                    onSelectAnkiWeb = { navController.navigate(Screen.AnkiWebBrowse.route) },
                    onSelectOpenTDB = { navController.navigate(Screen.OpenTDBBrowse.route) }
                )
            }

            // ── AnkiWeb Shared Decks browser ─────────────────────────────
            composable(
                Screen.AnkiWebBrowse.route,
                enterTransition = { SideModalTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { SideModalTransitions.popExit() }
            ) {
                AnkiWebBrowseScreen(
                    viewModel = homeViewModel,
                    onCardsImported = { cards -> homeViewModel.addCards(cards) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ── Open Trivia DB browser ────────────────────────────────────
            composable(
                Screen.OpenTDBBrowse.route,
                enterTransition = { SideModalTransitions.enter() },
                exitTransition = { UnderlyingScreenTransitions.exit() },
                popEnterTransition = { UnderlyingScreenTransitions.popEnter() },
                popExitTransition = { SideModalTransitions.popExit() }
            ) {
                OpenTDBBrowseScreen(
                    viewModel = homeViewModel,
                    onCardsImported = { cards ->
                        homeViewModel.addCards(cards)
                        navController.popBackStack(Screen.BrowseSource.route, inclusive = true)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }

    if (showBottomBar) {
        BottomNavBar(
            navController = navController,
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    } // end Box
}
