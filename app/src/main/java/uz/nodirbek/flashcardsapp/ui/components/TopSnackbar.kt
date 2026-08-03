package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.nodirbek.flashcardsapp.ui.theme.OutfitFamily

data class SnackbarData(
    val message: String,
    val subtitle: String? = null,
    val icon: String? = null,
    val color: Color,
    val durationMs: Long = 3000
)

@Composable
fun BoxScope.TopSnackbar(
    data: SnackbarData?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = data != null,
        modifier = modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        if (data != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(data.color)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (data.icon != null || data.subtitle != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        data.icon?.let { Text(it, fontSize = 18.sp) }
                        Column {
                            Text(
                                data.message,
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            data.subtitle?.let {
                                Text(
                                    it,
                                    fontFamily = OutfitFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        data.message,
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun rememberSnackbarState(): SnackbarState = remember { SnackbarState() }

class SnackbarState {
    var data by mutableStateOf<SnackbarData?>(null)
        private set

    suspend fun show(snackbarData: SnackbarData) {
        data = snackbarData
        delay(snackbarData.durationMs)
        data = null
    }

    fun dismiss() { data = null }
}
