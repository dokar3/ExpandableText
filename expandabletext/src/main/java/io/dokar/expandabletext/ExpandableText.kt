package io.dokar.expandabletext

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlin.math.max

private const val INLINE_CONTENT_ID = "EXPANDABLE_TEXT_TOGGLE"

private data class ToggleSize(
    val width: Int = 0,
    val widthSp: TextUnit = 0.sp,
    val height: Int = 0,
    val heightSp: TextUnit = 0.sp
)

/**
 * Display an expandable text, require `maxLines` to make text expandable.
 *
 * @param expanded Controls the expanded state of text.
 * @param text Text to display.
 * @param collapsedMaxLines The max lines when [expanded] is false.
 * @param expandedMaxLines The max lines when [expanded] is true. Defaults to [Int.MAX_VALUE].
 * @param toggle The toggle displayed at end of the text if text can not be fully displayed.
 * @see [Text]
 */
@Composable
fun ExpandableText(
    expanded: Boolean,
    text: String,
    collapsedMaxLines: Int,
    modifier: Modifier = Modifier,
    expandedMaxLines: Int = Int.MAX_VALUE,
    toggle: @Composable (() -> Unit)? = null,
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
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val annotatedString = remember(text) { AnnotatedString(text) }
    ExpandableText(
        expanded = expanded,
        text = annotatedString,
        modifier = modifier,
        toggle = toggle,
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
        collapsedMaxLines = collapsedMaxLines,
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
 * @param collapsedMaxLines The max lines when [expanded] is false.
 * @param expandedMaxLines The max lines when [expanded] is true. Defaults to [Int.MAX_VALUE].
 * @param toggle The toggle displayed at end of the text if text can not be fully displayed.
 * @see [Text]
 */
@Composable
fun ExpandableText(
    expanded: Boolean,
    text: AnnotatedString,
    collapsedMaxLines: Int,
    modifier: Modifier = Modifier,
    expandedMaxLines: Int = Int.MAX_VALUE,
    toggle: @Composable (() -> Unit)? = null,
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
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    // Use rememberSaveable to persist truncation state across config changes
    var visibleCharCount by rememberSaveable(text) {
        mutableIntStateOf(text.length)
    }
    var shouldShowToggleContent by rememberSaveable(text) {
        mutableStateOf(false)
    }

    val expandableText = remember(text, toggle as Any?, visibleCharCount, shouldShowToggleContent, expanded) {
        if (shouldShowToggleContent && toggle != null) {
            buildAnnotatedString {
                // When expanded, show full text to allow maxLines-driven animation
                // When collapsed, show truncated text
                append(if (expanded) text else text.subSequence(0, visibleCharCount))
                appendInlineContent(INLINE_CONTENT_ID)
            }
        } else {
            text
        }
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val toggleSize = measureToggle(toggle)

    val expandableInlineContent = remember(
        inlineContent,
        toggle as Any?,
        shouldShowToggleContent,
        toggleSize,
    ) {
        if (shouldShowToggleContent && toggle != null) {
            val content = InlineTextContent(
                placeholder = Placeholder(
                    width = toggleSize.widthSp,
                    height = toggleSize.heightSp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
                children = { toggle() }
            )
            inlineContent + Pair(INLINE_CONTENT_ID, content)
        } else {
            inlineContent
        }
    }

    fun tryUpdateTextInfo(
        toggleSize: ToggleSize,
        layoutRet: TextLayoutResult,
    ) {
        if (toggleSize.width == 0) return
        val actualMaxLines = if (expanded) expandedMaxLines else collapsedMaxLines
        if (layoutRet.lineCount == actualMaxLines) {
            val lineEnd = layoutRet.getLineEnd(layoutRet.lineCount - 1)
            if (lineEnd == expandableText.length) {
                // Text is fully displayed
                val showToggle = if (expanded) {
                    if (visibleCharCount < text.length) {
                        shouldShowToggleContent
                    } else {
                        layoutRet.lineCount > collapsedMaxLines
                    }
                } else {
                    shouldShowToggleContent
                }
                if (shouldShowToggleContent != showToggle) {
                    // Update only if changed to prevent infinite recomposition loops
                    shouldShowToggleContent = showToggle
                }
                if (expanded) {
                    if (visibleCharCount != text.length) {
                        // Update only if changed to prevent infinite recomposition loops
                        visibleCharCount = text.length
                    }
                } else if (!showToggle && visibleCharCount != text.length) {
                    // Update only if changed to prevent infinite recomposition loops
                    visibleCharCount = text.length
                }
                return
            }
            val lineTop = layoutRet.getLineTop(layoutRet.lineCount - 1)
            val isLtr = try {
                layoutRet.getParagraphDirection(lineEnd) == ResolvedTextDirection.Ltr
            } catch (e: ArrayIndexOutOfBoundsException) {
                // Error occurred in MultiParagraph.getParagraphDirection()
                true
            }
            val visibleChars = if (isLtr) {
                val toggleTopLeft = Offset(
                    x = layoutRet.size.width - toggleSize.width.toFloat(),
                    y = lineTop + toggleSize.height / 2f,
                )
                // Guard with lineStart to prevent over-truncation on newlines
                val lineStart = layoutRet.getLineStart(layoutRet.lineCount - 1)
                var count = layoutRet.getOffsetForPosition(toggleTopLeft)
                while (count > lineStart) {
                    val charRight = layoutRet.getBoundingBox(offset = count - 1).right
                    val isOverlapped = charRight >= toggleTopLeft.x
                    val isWhitespace = text[count - 1].isWhitespace()
                    if (isOverlapped || isWhitespace) {
                        count--
                    } else {
                        break
                    }
                }
                count
            } else {
                val toggleTopRight = Offset(
                    x = toggleSize.width.toFloat(),
                    y = lineTop + toggleSize.height / 2f,
                )
                val lineStart = layoutRet.getLineStart(layoutRet.lineCount - 1)
                var count = layoutRet.getOffsetForPosition(toggleTopRight)
                while (count > lineStart) {
                    val charLeft = layoutRet.getBoundingBox(offset = count - 1).left
                    val isOverlapped = charLeft <= toggleTopRight.x
                    val isWhitespace = text[count - 1].isWhitespace()
                    if (isOverlapped || isWhitespace) {
                        count--
                    } else {
                        break
                    }
                }
                count
            }
            if (visibleCharCount != visibleChars) {
                // Update only if changed to prevent infinite recomposition loops
                visibleCharCount = visibleChars
            }
            if (!shouldShowToggleContent) {
                shouldShowToggleContent = true
            }
        } else {
            val showToggle = expanded && layoutRet.lineCount > collapsedMaxLines
            if (visibleCharCount != text.length) {
                // Update only if changed to prevent infinite recomposition loops
                visibleCharCount = text.length
            }
            if (shouldShowToggleContent != showToggle) {
                shouldShowToggleContent = showToggle
            }
            if (expanded) {
                if (visibleCharCount != text.length) {
                    visibleCharCount = text.length
                }
            } else if (/*!showToggle && */visibleCharCount != text.length) {
                visibleCharCount = text.length
            }
        }
    }

    LaunchedEffect(
        expanded,
        collapsedMaxLines,
        expandedMaxLines,
        toggleSize,
        layoutResult.value,
    ) {
        val layoutRet = layoutResult.value ?: return@LaunchedEffect
        if (toggleSize.width > 0) {
            tryUpdateTextInfo(toggleSize, layoutRet)
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
        maxLines = if (expanded) expandedMaxLines else collapsedMaxLines,
        inlineContent = expandableInlineContent,
        onTextLayout = {
            onTextLayout(it)
            layoutResult.value = it
        },
        style = style
    )
}

@Composable
private fun measureToggle(
    content: @Composable (() -> Unit)?,
): ToggleSize {
    var size by remember(content as Any?) { mutableStateOf(ToggleSize()) }
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
            size = ToggleSize(
                width = maxWidth,
                widthSp = maxWidth.toSp(),
                height = maxHeight,
                heightSp = maxHeight.toSp(),
            )
            layout(0, 0) {}
        }
    }
    return size
}
