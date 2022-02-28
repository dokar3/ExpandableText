package io.dokar.expandabletext

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlin.math.max


private const val INLINE_CONTENT_ID = "EXPANDABLE_TEXT_TOGGLE"

private data class TextUnitSize(
    val width: TextUnit = 0.sp,
    val height: TextUnit = 0.sp
)

private data class ExpandableTextInfo(
    val visibleCharCount: Int,
    val shouldShowToggleContent: Boolean,
    val isTextLaidOut: Boolean,
)

/**
 * Display an expandable text, require `maxLines` to make text expandable.
 *
 * @param expanded Controls the expanded state of text.
 * @param text Text to display.
 * @param toggleContent The content will be displayed at end of the text if it can not
 * be fully displayed.
 * @see [Text]
 */
@Composable
fun ExpandableText(
    expanded: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    toggleContent: @Composable (() -> Unit)? = null,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    expandedMaxLines: Int = Int.MAX_VALUE,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val annotatedString = remember(text) { AnnotatedString(text) }
    ExpandableText(
        expanded = expanded,
        text = annotatedString,
        modifier = modifier,
        toggleContent = toggleContent,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        expandedMaxLines = expandedMaxLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = style
    )
}

/**
 * Display an expandable text, require `maxLines` to make text expandable.
 *
 * @param expanded Controls the expanded state of text.
 * @param text Text to display.
 * @param toggleContent The content will be displayed at end of the text if it can not
 * be fully displayed.
 * @see [Text]
 */
@Composable
fun ExpandableText(
    expanded: Boolean,
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    toggleContent: @Composable (() -> Unit)? = null,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    expandedMaxLines: Int = Int.MAX_VALUE,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    var textInfo by remember(text) {
        mutableStateOf(
            ExpandableTextInfo(
                visibleCharCount = text.length,
                shouldShowToggleContent = false,
                isTextLaidOut = false,
            )
        )
    }

    val toggleContentSize = measureComposable(toggleContent)

    val expandableText = remember(text, textInfo) {
        if (textInfo.shouldShowToggleContent) {
            buildAnnotatedString {
                append(text.subSequence(0, textInfo.visibleCharCount - 1))
                appendInlineContent(INLINE_CONTENT_ID, " ")
            }
        } else {
            text
        }
    }

    val expandableInlineContent = remember(
        inlineContent,
        toggleContent as Any?,
        textInfo,
        toggleContentSize,
    ) {
        if (textInfo.shouldShowToggleContent &&
            textInfo.isTextLaidOut &&
            toggleContent != null
        ) {
            val content = InlineTextContent(
                placeholder = Placeholder(
                    width = toggleContentSize.width,
                    height = toggleContentSize.height,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
                children = { toggleContent() }
            )
            inlineContent + Pair(INLINE_CONTENT_ID, content)
        } else {
            inlineContent
        }
    }

    Text(
        text = expandableText,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = if (expanded) expandedMaxLines else maxLines,
        inlineContent = expandableInlineContent,
        onTextLayout = {
            onTextLayout(it)
            val maxVisibleLines = if (expanded) expandedMaxLines else maxLines
            var visibleCharCount = text.length
            var showToggleContent = textInfo.shouldShowToggleContent
            if (it.lineCount >= maxVisibleLines) {
                val lineEnd = it.getLineEnd(maxVisibleLines - 1)
                if (lineEnd < text.length - 1) {
                    visibleCharCount = lineEnd
                    showToggleContent = toggleContent != null
                }
            }
            textInfo = ExpandableTextInfo(
                visibleCharCount = visibleCharCount,
                shouldShowToggleContent = showToggleContent,
                isTextLaidOut = true,
            )
        },
        style = style
    )
}

@Composable
private fun measureComposable(
    content: @Composable (() -> Unit)?,
): TextUnitSize {
    var size by remember(content as Any?) { mutableStateOf(TextUnitSize()) }
    if (content != null) {
        Layout(content = content) { measurables, constraints ->
            var maxWidth = 0
            var maxHeight = 0
            measurables
                .fastMap { it.measure(constraints) }
                .fastForEach {
                    maxWidth = max(maxWidth, it.measuredWidth)
                    maxHeight = max(maxHeight, it.measuredHeight)
                }
            size = TextUnitSize(maxWidth.toSp(), maxHeight.toSp())
            layout(0, 0) {}
        }
    }
    return size
}
