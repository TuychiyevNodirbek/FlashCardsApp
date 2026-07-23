package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Кнопка с эффектом "3D-нажатия" из дизайна FlashDeck v2.
 * В спокойном состоянии имеет нижнюю границу-тень [shadowDp] пикселей цвета [shadowColor].
 * При нажатии смещается на [shadowDp] вниз и граница исчезает (1dp).
 */
@Composable
fun PressButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color,
    shadowColor: Color,
    shape: Shape = RoundedCornerShape(14.dp),
    shadowDp: Dp = 4.dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOffset = if (isPressed) shadowDp else 0.dp
    val currentShadow = if (isPressed) 1.dp else shadowDp

    Surface(
        onClick = onClick,
        modifier = modifier.offset(y = currentOffset),
        shape = shape,
        color = color,
        border = BorderStroke(currentShadow, shadowColor),
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
