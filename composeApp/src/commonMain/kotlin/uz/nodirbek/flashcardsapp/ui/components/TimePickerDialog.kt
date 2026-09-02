package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Замена android.app.TimePickerDialog: тот же UI (Material3 TimePicker) на всех платформах,
 * т.к. androidx.compose.material3.TimePicker — мультиплатформенный компонент.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Время напоминания", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.padding(top = 8.dp))
                TimePicker(state = state)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("ОК") }
                }
            }
        }
    }
}
