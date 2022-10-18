package io.dokar.expandabletext

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlin.math.max


private const val INLINE_CONTENT_ID = "EXPANDABLE_TEXT_TOGGLE"

private data class ToggleSize(
    val width: Int = 0,
    val widthSp: TextUnit = 0.sp,
    val height: Int = 0,
    val heightSp: TextUnit = 0.sp
)

private data class ExpandableTextInfo(
    val visibleCharCount: Int,
    val shouldShowToggleContent: Boolean,
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
    var textInfo by remember(text) {
        mutableStateOf(
            ExpandableTextInfo(
                visibleCharCount = text.length,
                shouldShowToggleContent = false,
            )
        )
    }

    val expandableText = remember(text, toggle as Any?, textInfo) {
        if (textInfo.shouldShowToggleContent && toggle != null) {
            buildAnnotatedString {
                append(text.subSequence(0, textInfo.visibleCharCount))
                appendInlineContent(INLINE_CONTENT_ID)
            }
        } else {
            text
        }
    }

    val layoutResultFlow = remember(expandableText, expanded, collapsedMaxLines, expandedMaxLines) {
        MutableStateFlow<TextLayoutResult?>(null)
    }

    val toggleSizeFlow = remember { MutableStateFlow(ToggleSize()) }

    toggleSizeFlow.tryEmit(measureToggle(toggle))

    val currToggleSize by toggleSizeFlow.collectAsState()
    val expandableInlineContent = remember(
        inlineContent,
        toggle as Any?,
        textInfo,
        currToggleSize,
    ) {
        if (textInfo.shouldShowToggleContent && toggle != null) {
            val content = InlineTextContent(
                placeholder = Placeholder(
                    width = currToggleSize.widthSp,
                    height = currToggleSize.heightSp,
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
                val visibleChars = if (textInfo.shouldShowToggleContent) {
                    expandableText.length - 1
                } else {
                    expandableText.length
                }
                textInfo = textInfo.copy(visibleCharCount = visibleChars)
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
                layoutRet.getOffsetForPosition(toggleTopLeft)
            } else {
                val toggleTopRight = Offset(
                    x = toggleSize.width.toFloat(),
                    y = lineTop + toggleSize.height / 2f,
                )
                layoutRet.getOffsetForPosition(toggleTopRight)
            }
            textInfo = textInfo.copy(
                visibleCharCount = visibleChars,
                shouldShowToggleContent = true,
            )
        } else {
            textInfo = textInfo.copy(
                visibleCharCount = text.length,
            )
        }
    }

    LaunchedEffect(toggleSizeFlow, layoutResultFlow) {
        combine(
            toggleSizeFlow.filter { it.width > 0 },
            layoutResultFlow.filterNotNull(),
        ) { toggleSize, textLayoutResult ->
            tryUpdateTextInfo(toggleSize, textLayoutResult)
        }.collect()
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
            layoutResultFlow.update { _ -> it }
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
