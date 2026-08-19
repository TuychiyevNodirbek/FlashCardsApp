package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

// TODO Фаза 6: разметка (жирный/курсив/подчёркнутый) через NSAttributedString на iOS.
// Пока — просто убираем теги, чтобы не показывать сырой HTML.
@Composable
actual fun HtmlText(
    text: String,
    modifier: Modifier,
    color: Color,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    fontSize: TextUnit,
    textAlign: TextAlign?,
    maxLines: Int,
) {
    val plain = remember(text) { text.stripHtmlTags() }
    Text(
        text = plain,
        modifier = modifier,
        color = color,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        fontSize = fontSize,
        textAlign = textAlign,
        maxLines = maxLines,
    )
}

private fun String.stripHtmlTags(): String =
    if (!contains('<')) this else replace(Regex("<[^>]*>"), "")
