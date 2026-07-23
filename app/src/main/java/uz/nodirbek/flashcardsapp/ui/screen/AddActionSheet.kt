package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.nodirbek.flashcardsapp.ui.theme.Neutral100
import uz.nodirbek.flashcardsapp.ui.theme.Neutral200
import uz.nodirbek.flashcardsapp.ui.theme.Neutral900

@Composable
fun AddActionSheet(
    onDismiss: () -> Unit,
    onAddCard: () -> Unit,
    onImport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = onDismiss
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.White)
                .border(width = 2.dp, color = Neutral900)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    onClick = {}
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(3.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Neutral200)
            )

            Button(
                onClick = onAddCard,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Neutral100, contentColor = Neutral900),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Добавить карточку", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Neutral100, contentColor = Neutral900),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text("Импортировать список", fontWeight = FontWeight.Bold)
            }
        }
    }
}
