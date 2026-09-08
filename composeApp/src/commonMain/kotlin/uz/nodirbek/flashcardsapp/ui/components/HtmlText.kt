package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

/**
 * Рендерит текст карточки с сохранением базового форматирования Anki-импорта
 * (жирный/курсив/подчёркнутый, списки), не поддерживая произвольный HTML/CSS.
 * Для обычного текста без тегов ведёт себя как обычный Text.
 *
 * Общая реализация без expect/actual — разбор HTML-подмножества (те же теги,
 * что AnkiApkgImporter.allowedTags оставляет после stripHtml: b/strong, i/em,
 * u, ul/ol/li, br) не завязан ни на какое платформенное API, поэтому
 * android.text.Html (Android) и NSAttributedString (iOS) не нужны вовсе.
 */
@Composable
fun HtmlText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val annotated = remember(text) { text.toFormattedAnnotatedString() }
    Text(
        text = annotated,
        modifier = modifier,
        color = color,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        fontSize = fontSize,
        textAlign = textAlign,
        maxLines = maxLines,
    )
}

private val TAG_REGEX = Regex("""</?([a-zA-Z][a-zA-Z0-9]*)\b[^>]*>""")

private fun decodeEntities(raw: String): String = raw
    .replace("&nbsp;", " ")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&amp;", "&")

private fun String.toFormattedAnnotatedString(): AnnotatedString {
    if (!contains('<')) return AnnotatedString(decodeEntities(this))

    data class OpenTag(val name: String, val start: Int)

    val builder = StringBuilder()
    val openTags = ArrayDeque<OpenTag>()
    val spans = mutableListOf<Triple<SpanStyle, Int, Int>>()
    var isListItem = false

    var lastEnd = 0
    for (match in TAG_REGEX.findAll(this)) {
        // Текст между тегами
        val plain = decodeEntities(substring(lastEnd, match.range.first))
        builder.append(plain)
        lastEnd = match.range.last + 1

        val isClosing = match.value.startsWith("</")
        val tagName = match.groupValues[1].lowercase()

        when (tagName) {
            "br" -> builder.append('\n')
            "li" -> {
                if (!isClosing) {
                    if (builder.isNotEmpty() && builder.last() != '\n') builder.append('\n')
                    builder.append("• ")
                    isListItem = true
                } else if (isListItem) {
                    builder.append('\n')
                    isListItem = false
                }
            }
            "ul", "ol" -> Unit // просто группировка, собственного стиля не добавляет
            "b", "strong", "i", "em", "u" -> {
                if (!isClosing) {
                    openTags.addLast(OpenTag(tagName, builder.length))
                } else {
                    val idx = openTags.indexOfLast { it.name == tagName }
                    if (idx >= 0) {
                        val open = openTags.removeAt(idx)
                        val style = when (tagName) {
                            "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
                            "i", "em" -> SpanStyle(fontStyle = FontStyle.Italic)
                            "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
                            else -> null
                        }
                        if (style != null && open.start < builder.length) {
                            spans += Triple(style, open.start, builder.length)
                        }
                    }
                }
            }
            else -> Unit // неизвестный/неподдерживаемый тег — просто вырезается
        }
    }
    builder.append(decodeEntities(substring(lastEnd)))

    return buildAnnotatedString {
        append(builder.toString())
        for ((style, start, end) in spans) {
            if (start in 0..length && end in start..length) addStyle(style, start, end)
        }
    }
}
