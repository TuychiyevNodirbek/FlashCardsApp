package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import uz.nodirbek.flashcardsapp.ui.state.DeckWithStats

/**
 * Выбор колоды для импорта скачанных карточек: список существующих колод +
 * возможность создать новую прямо здесь, без выхода из экрана скачивания.
 *
 * Если колод ещё нет вообще, форма создания новой колоды раскрыта сразу —
 * раньше в этом месте показывалась заглушка «Нет колод. Создайте колоду
 * перед импортом», которая уводила пользователя с экрана и он терял
 * скачанный результат.
 *
 * @param suggestedName имя новой колоды по умолчанию (например, имя
 *   импортируемой колоды или выбранная категория) — пользователь может
 *   изменить его перед созданием
 * @param onCreateDeck колбэк создания: вызывающий код сам генерирует id,
 *   вставляет колоду через ViewModel и должен сразу выбрать её через
 *   [onSelectDeck], чтобы кнопка импорта стала активна не дожидаясь
 *   следующего кадра со свежим [allDecks]
 */
@Composable
fun DeckPickerSection(
    allDecks: List<DeckWithStats>,
    selectedDeckId: String?,
    onSelectDeck: (String) -> Unit,
    onCreateDeck: (name: String) -> Unit,
    modifier: Modifier = Modifier,
    suggestedName: String = ""
) {
    var creatingNew by remember(allDecks.isEmpty()) { mutableStateOf(allDecks.isEmpty()) }
    var newName by remember { mutableStateOf(suggestedName) }
    val focusRequester = remember { FocusRequester() }

    fun confirmCreate() {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        onCreateDeck(trimmed)
        creatingNew = false
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (creatingNew) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Название новой колоды") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirmCreate() })
                )
                FilledTonalButton(
                    onClick = ::confirmCreate,
                    enabled = newName.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Создать")
                }
            }
            if (allDecks.isNotEmpty()) {
                TextButton(onClick = { creatingNew = false }) {
                    Text("Выбрать существующую колоду", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                allDecks.forEach { d ->
                    val isSelected = selectedDeckId == d.deck.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectDeck(d.deck.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Text(d.deck.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
            OutlinedButton(
                onClick = { creatingNew = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Новая колода")
            }
        }
    }
}
