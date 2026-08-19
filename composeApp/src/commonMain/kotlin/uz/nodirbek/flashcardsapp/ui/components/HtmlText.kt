package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

/**
 * Рендерит текст карточки с сохранением базового форматирования Anki-импорта
 * (жирный/курсив/подчёркнутый, списки), не поддерживая произвольный HTML/CSS.
 * Для обычного текста без тегов ведёт себя как обычный Text.
 * Android — через android.text.Html; iOS — упрощённый парсер (TODO Фаза 6: NSAttributedString).
 */
@Composable
expect fun HtmlText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
)
