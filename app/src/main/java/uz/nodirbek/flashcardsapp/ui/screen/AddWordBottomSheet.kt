package uz.nodirbek.flashcardsapp.ui.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordBottomSheet(
    onDismiss: () -> Unit,
    onSave: (Card) -> Unit,
    deckId: String = "default"
) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var frontError by remember { mutableStateOf(false) }
    var backError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FdSurface,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(FdBorder)
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Новая карточка",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = FdText,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, FdBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = FdTextSub, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(20.dp))

            SheetField(
                label = "Лицевая сторона",
                hint = "Слово или фраза",
                value = front,
                isError = frontError,
                onValueChange = { front = it; frontError = false }
            )
            Spacer(Modifier.height(12.dp))
            SheetField(
                label = "Оборотная сторона",
                hint = "Перевод или определение",
                value = back,
                isError = backError,
                onValueChange = { back = it; backError = false }
            )
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Cancel
                Box(
                    Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FdSurface2)
                        .border(1.5.dp, FdBorder, RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Отмена", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FdText)
                }
                // Save
                PressButton(
                    onClick = {
                        frontError = front.isBlank()
                        backError = back.isBlank()
                        if (!frontError && !backError) {
                            onSave(
                                Card(
                                    id = java.util.UUID.randomUUID().toString(),
                                    deckId = deckId,
                                    front = front.trim(),
                                    back = back.trim(),
                                    dueDate = RateCardUseCase.getTodayDate(),
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    color = FdPrimary,
                    shadowColor = FdPrimaryDark,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }
        }
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
        containerColor = FdSurface,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(FdBorder)
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Редактировать карточку",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = FdText,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, FdBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = FdTextSub, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(20.dp))

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
                        .border(1.5.dp, FdRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                        Text("Это действие необратимо", fontSize = 12.sp, color = FdTextSub, modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FdSurface2)
                                    .border(1.5.dp, FdBorder, RoundedCornerShape(8.dp))
                                    .clickable { showDeleteConfirm = false }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Отмена", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FdText)
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
                Spacer(Modifier.height(12.dp))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Delete button
                Box(
                    Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FdRedLight)
                        .border(1.5.dp, FdRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { showDeleteConfirm = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🗑️", fontSize = 18.sp)
                }
                // Save button
                PressButton(
                    onClick = {
                        if (front.isNotBlank() && back.isNotBlank()) {
                            onSave(card.copy(front = front.trim(), back = back.trim()))
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    color = FdPrimary,
                    shadowColor = FdPrimaryDark,
                    shape = RoundedCornerShape(12.dp),
                    enabled = front.isNotBlank() && back.isNotBlank()
                ) {
                    Text("Сохранить", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
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
            color = FdTextSub,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(hint, color = FdTextSub, fontSize = 14.sp) },
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FdPrimary,
                unfocusedBorderColor = FdBorder,
                errorBorderColor = FdRed,
                focusedContainerColor = FdSurface,
                unfocusedContainerColor = FdSurface2,
                errorContainerColor = FdRedLight
            ),
            singleLine = true
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
