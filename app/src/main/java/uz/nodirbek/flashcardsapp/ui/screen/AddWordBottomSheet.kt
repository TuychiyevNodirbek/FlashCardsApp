package uz.nodirbek.flashcardsapp.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.data.transfer.parseCardsFromUri
import uz.nodirbek.flashcardsapp.shared.data.transfer.CardParseResult
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.shared.model.Deck
import uz.nodirbek.flashcardsapp.shared.scheduler.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordBottomSheet(
    onDismiss: () -> Unit,
    onSave: (Card) -> Unit,
    deckId: String = "default",
    subRows: List<Deck> = emptyList(),
    onSaveAndNext: ((Card) -> Unit)? = null,
    onSaveMultiple: ((List<Card>) -> Unit)? = null
) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var frontError by remember { mutableStateOf(false) }
    var backError by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    // Куда сохранять: последняя созданная тема по умолчанию, иначе сам курс
    var targetDeckId by remember { mutableStateOf(subRows.lastOrNull()?.id ?: deckId) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImporting = true
        importError = null
        scope.launch {
            when (val result = parseCardsFromUri(uri, context, targetDeckId)) {
                is CardParseResult.Success -> {
                    onSaveMultiple?.invoke(result.cards) ?: result.cards.forEach { onSave(it) }
                    onDismiss()
                }
                is CardParseResult.Error -> {
                    importError = result.message
                }
            }
            isImporting = false
        }
    }

    fun buildCard(): Card? {
        frontError = front.isBlank()
        backError = back.isBlank()
        if (frontError || backError) return null
        return Card(
            id = java.util.UUID.randomUUID().toString(),
            deckId = targetDeckId,
            front = front.trim(),
            back = back.trim(),
            dueDate = RateCardUseCase.getTodayDate(),
            createdAt = System.currentTimeMillis()
        )
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with close
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Новая карточка",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Import from file button
                    Box(
                        Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(FdPrimaryLight)
                            .border(1.dp, FdPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable(enabled = !isImporting) { fileLauncher.launch("*/*") }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = FdPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("📂 Файл", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FdPrimary)
                        }
                    }
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Import error
            if (importError != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FdRedLight)
                        .border(1.dp, FdRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⚠️", fontSize = 14.sp)
                        Text(importError!!, fontSize = 12.sp, color = FdRed, modifier = Modifier.weight(1f))
                        Box(Modifier.size(20.dp).clickable { importError = null }, contentAlignment = Alignment.Center) {
                            Text("✕", fontSize = 12.sp, color = FdRed)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Выбор темы (subRow), если они есть у курса
            if (subRows.isNotEmpty()) {
                Text(
                    "Тема",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SubRowChip(
                            label = "Общее",
                            isActive = targetDeckId == deckId,
                            onClick = { targetDeckId = deckId }
                        )
                    }
                    items(subRows.size) { i ->
                        val subRow = subRows[i]
                        SubRowChip(
                            label = subRow.name,
                            isActive = targetDeckId == subRow.id,
                            onClick = { targetDeckId = subRow.id }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Input fields
            SheetField(
                label = "Лицевая сторона",
                hint = "Слово или фраза",
                value = front,
                isError = frontError,
                onValueChange = { front = it; frontError = false }
            )
            Spacer(Modifier.height(16.dp))
            SheetField(
                label = "Оборотная сторона",
                hint = "Перевод или определение",
                value = back,
                isError = backError,
                onValueChange = { back = it; backError = false }
            )
            Spacer(Modifier.height(24.dp))

            // Actions: "+ ещё" (save & continue) and primary save — per design
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onSaveAndNext != null) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable {
                                buildCard()?.let { card ->
                                    onSaveAndNext(card)
                                    front = ""
                                    back = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+ ещё",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                PressButton(
                    onClick = { buildCard()?.let(onSave) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    color = FdPrimary,
                    shadowColor = FdPrimaryDark,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Сохранить",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SubRowChip(label: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) FdPrimary else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.5.dp,
                if (isActive) FdPrimaryDark else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            fontFamily = OutfitFamily,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardBottomSheet(
    card: Card,
    onDismiss: () -> Unit,
    onSave: (Card) -> Unit,
    onDelete: (Card) -> Unit
) {
    var front by remember(card) { mutableStateOf(card.front) }
    var back by remember(card) { mutableStateOf(card.back) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Редактировать карточку",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            SheetField(
                label = "Лицевая сторона",
                hint = "Слово или фраза",
                value = front,
                onValueChange = { front = it }
            )
            Spacer(Modifier.height(12.dp))
            SheetField(
                label = "Оборотная сторона",
                hint = "Перевод или определение",
                value = back,
                onValueChange = { back = it }
            )
            Spacer(Modifier.height(20.dp))

            if (showDeleteConfirm) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FdRedLight)
                        .border(1.5.dp, FdRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            "Удалить карточку?",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FdRed
                        )
                        Text("Это действие необратимо", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .clickable { showDeleteConfirm = false }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Отмена", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FdRed)
                                    .border(2.dp, FdRedDark, RoundedCornerShape(8.dp))
                                    .clickable { onDelete(card) }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Удалить", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Save button (full width)
            PressButton(
                onClick = {
                    if (front.isNotBlank() && back.isNotBlank()) {
                        onSave(card.copy(front = front.trim(), back = back.trim()))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                color = FdPrimary,
                shadowColor = FdPrimaryDark,
                shape = RoundedCornerShape(12.dp),
                enabled = front.isNotBlank() && back.isNotBlank()
            ) {
                Text(
                    "Сохранить",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            // Delete button
            if (!showDeleteConfirm) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FdRedLight)
                        .border(1.5.dp, FdRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { showDeleteConfirm = true }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🗑️ Удалить карточку",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FdRed
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetField(
    label: String,
    hint: String,
    value: String,
    isError: Boolean = false,
    onValueChange: (String) -> Unit
) {

    Column {
        Text(
            label,
            fontFamily = OutfitFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = FdRed,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                errorContainerColor = FdRedLight
            ),
            singleLine = false,
            minLines = 2,
            maxLines = 4
        )
        if (isError) {
            Text(
                "Обязательное поле",
                fontSize = 11.sp,
                color = FdRed,
                modifier = Modifier.padding(top = 3.dp, start = 4.dp)
            )
        }
    }
}
