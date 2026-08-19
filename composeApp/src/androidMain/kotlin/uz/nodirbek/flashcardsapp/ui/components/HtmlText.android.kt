package uz.nodirbek.flashcardsapp.ui.components

import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.core.text.HtmlCompat
import androidx.compose.material3.Text

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
        style = TextStyle.Default,
    )
}

private fun String.toFormattedAnnotatedString(): androidx.compose.ui.text.AnnotatedString {
    if (!contains('<') && !contains('&')) {
        return buildAnnotatedString { append(this@toFormattedAnnotatedString) }
    }
    val spanned: Spanned = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT)
    return buildAnnotatedString {
        append(spanned.toString())
        val spans = spanned.getSpans(0, spanned.length, Any::class.java)
        for (span in spans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start < 0 || end < 0 || start >= end) continue
            when (span) {
                is StyleSpan -> when (span.style) {
                    android.graphics.Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    android.graphics.Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    android.graphics.Typeface.BOLD_ITALIC -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                        start,
                        end
                    )
                }
                is UnderlineSpan -> addStyle(
                    SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                    start,
                    end
                )
            }
        }
    }
}
