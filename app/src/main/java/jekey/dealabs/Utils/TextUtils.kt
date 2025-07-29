package jeky.dealabs.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Utility functions for text formatting
 */
object TextUtils {
    
    /**
     * Apply or remove style to/from selected text
     */
    fun toggleStyle(
        currentAnnotatedString: AnnotatedString,
        selection: TextRange,
        style: SpanStyle
    ): AnnotatedString {
        return buildAnnotatedString {
            append(currentAnnotatedString.text)

            currentAnnotatedString.spanStyles.forEach { span ->
                val hasOverlap = !(span.end <= selection.start || span.start >= selection.end)
                val isTargetStyle = (span.item.fontWeight == style.fontWeight && style.fontWeight != FontWeight.Normal) ||
                                    (span.item.fontStyle == style.fontStyle && style.fontStyle != FontStyle.Normal)

                if (!(hasOverlap && isTargetStyle)) {
                    addStyle(span.item, span.start, span.end)
                }
            }

            val isStyleCurrentlyAppliedInSelection = currentAnnotatedString.spanStyles.any { span ->
                val overlaps = !(span.end <= selection.start || span.start >= selection.end)
                overlaps && ((style.fontWeight != FontWeight.Normal && span.item.fontWeight == style.fontWeight) ||
                             (style.fontStyle != FontStyle.Normal && span.item.fontStyle == style.fontStyle))
            }

            if (selection.collapsed) {
                if (!isStyleCurrentlyAppliedInSelection) {
                    addStyle(style, 0, currentAnnotatedString.length)
                } else {
                    val inverseStyle = when {
                        style.fontWeight == FontWeight.Bold -> style.copy(fontWeight = FontWeight.Normal)
                        style.fontStyle == FontStyle.Italic -> style.copy(fontStyle = FontStyle.Normal)
                        else -> null
                    }
                    inverseStyle?.let { addStyle(it, 0, currentAnnotatedString.length) }
                }
            } else {
                if (!isStyleCurrentlyAppliedInSelection) {
                    addStyle(style, selection.start, selection.end)
                } else {
                    val inverseStyle = when {
                        style.fontWeight == FontWeight.Bold -> style.copy(fontWeight = FontWeight.Normal)
                        style.fontStyle == FontStyle.Italic -> style.copy(fontStyle = FontStyle.Normal)
                        else -> null
                    }
                    inverseStyle?.let { addStyle(it, selection.start, selection.end) }
                }
            }
        }
    }
}