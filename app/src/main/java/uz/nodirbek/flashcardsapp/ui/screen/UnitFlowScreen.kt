package uz.nodirbek.flashcardsapp.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.nodirbek.flashcardsapp.data.repository.CardRepository
import uz.nodirbek.flashcardsapp.data.repository.StatsRepository
import uz.nodirbek.flashcardsapp.data.repository.UnitRepository
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.screen.exercise.AudioContent
import uz.nodirbek.flashcardsapp.ui.screen.exercise.ScrambleContent
import uz.nodirbek.flashcardsapp.ui.screen.exercise.WriteContent
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.FlowStep
import uz.nodirbek.flashcardsapp.ui.viewmodel.UnitFlowViewModel

@Composable
fun UnitFlowScreen(
    deckId: String,
    unitIndex: Int,
    unitRepository: UnitRepository,
    cardRepository: CardRepository,
    statsRepository: StatsRepository,
    rateCardUseCase: RateCardUseCase,
    ttsLang: String = "en",
    ttsSpeed: Float = 1f,
    onBack: () -> Unit,
    onFinished: (correct: Int, total: Int) -> Unit
) {
    val factory = remember(deckId, unitIndex) {
        UnitFlowViewModel.Factory(unitRepository, cardRepository, statsRepository, rateCardUseCase, deckId, unitIndex)
    }
    val vm: UnitFlowViewModel = viewModel(
        key = "unit_flow_${deckId}_$unitIndex",
        factory = factory
    )
    val uiState by vm.uiState.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler { showExitDialog = true }

    LaunchedEffect(uiState.finished) {
        if (uiState.finished) {
            onFinished(uiState.correctAnswers, uiState.totalAnswers)
        }
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FdPrimary)
        }
        return
    }

    val progress by animateFloatAsState(
        targetValue = if (uiState.totalSteps > 0) uiState.stepIndex.toFloat() / uiState.totalSteps else 0f,
        animationSpec = tween(400),
        label = "unit_progress"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showExitDialog = true }) {
                            Icon(Icons.Default.Close, "Выйти", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Юнит ${unitIndex + 1} · шаг ${uiState.stepIndex + 1}/${uiState.totalSteps}",
                            fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = FdPrimary,
                        trackColor = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = uiState.stepIndex,
            modifier = Modifier.fillMaxSize().padding(padding),
            transitionSpec = {
                (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)))
                    .togetherWith(slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(200)))
            },
            label = "flow_step"
        ) { stepIndex ->
            val step = uiState.steps.getOrNull(stepIndex)
            val shuffledCards = remember(stepIndex) { uiState.cards.shuffled() }
            when (step) {
                FlowStep.FLASHCARDS -> FlashcardsContent(
                    cards = shuffledCards,
                    onDone = { vm.onStepFinished(0, 0) },
                    onBackClick = { showExitDialog = true },
                    showTopBar = false
                )
                FlowStep.MATCH -> MatchContent(
                    cards = shuffledCards,
                    onBackClick = { showExitDialog = true },
                    onFinished = { _, _, _ -> vm.onStepFinished(0, 0) },
                    showTopBar = false
                )
                FlowStep.TEST -> TestContent(
                    cards = shuffledCards,
                    allCardsForDistractors = uiState.cards,
                    onBackClick = { showExitDialog = true },
                    onDone = { correct, total -> vm.onStepFinished(correct, total) },
                    showTopBar = false
                )
                FlowStep.AUDIO -> AudioContent(
                    cards = shuffledCards,
                    ttsLang = ttsLang,
                    ttsSpeed = ttsSpeed,
                    onBackClick = { showExitDialog = true },
                    onDone = { correct, total -> vm.onStepFinished(correct, total) },
                    showTopBar = false
                )
                FlowStep.SCRAMBLE -> ScrambleContent(
                    cards = shuffledCards,
                    onBackClick = { showExitDialog = true },
                    onDone = { correct, total -> vm.onStepFinished(correct, total) },
                    showTopBar = false
                )
                FlowStep.WRITE -> WriteContent(
                    cards = shuffledCards,
                    onBackClick = { showExitDialog = true },
                    onDone = { correct, total -> vm.onStepFinished(correct, total) },
                    showTopBar = false
                )
                null -> {}
            }
        }
    }

    if (showExitDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showExitDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Выйти из юнита?",
                    fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Весь прогресс юнита будет потерян. При следующем входе юнит начнётся заново.",
                    fontFamily = OutfitFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                PressButton(
                    onClick = { showExitDialog = false; onBack() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    color = FdRed, shadowColor = FdRedDark, shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Выйти", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
                PressButton(
                    onClick = { showExitDialog = false },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowColor = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Продолжить", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
