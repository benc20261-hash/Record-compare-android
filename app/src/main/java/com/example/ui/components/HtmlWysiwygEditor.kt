package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.util.UriHelper

/**
 * Representation of a rich formatting span within the editor's plain text.
 */
data class FormatSpan(
    val start: Int,
    val end: Int,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrike: Boolean = false,
    val url: String? = null
)

/**
 * Normalizes initial text, converting legacy Markdown (**bold**, *italic*, [label](url))
 * and bullet lines into clean HTML.
 */
fun normalizeToEditableHtml(content: String): String {
    if (content.isBlank()) return ""
    var html = content

    // 1. Convert legacy markdown bold: **text** or __text__ -> <b>text</b>
    html = html.replace(Regex("""\*\*(.*?)\*\*"""), "<b>$1</b>")
    html = html.replace(Regex("""__(.*?)__"""), "<b>$1</b>")

    // 2. Convert legacy markdown italic: *text* or _text_ -> <i>text</i>
    html = html.replace(Regex("""(^|[^\*])\*([^\*\n]+)\*([^\*]|$)"""), "$1<i>$2</i>$3")

    // 3. Convert legacy markdown strikethrough: ~~text~~ -> <s>text</s>
    html = html.replace(Regex("""~~(.*?)~~"""), "<s>$1</s>")

    // 4. Convert legacy markdown links: [label](url) -> <a href="url">label</a>
    val mdLinkRegex = Regex("""\[([^\]]+)\]\(((?:https?|upnote|marginnote\w*|obsidian|notion|craftdocs|bear|things|onenote|goodnotes|notability|logseq)://[^\s<>'"]+[\w/=&#?%-])\)""", RegexOption.IGNORE_CASE)
    html = mdLinkRegex.replace(html) { match ->
        val label = match.groupValues[1]
        val linkUrl = match.groupValues[2]
        "<a href=\"$linkUrl\">$label</a>"
    }

    // 5. Convert markdown bullet lines: "- Item" or "• Item" into <li>Item</li> if not already HTML
    val isRichHtml = Regex("""<\/?(p|div|ul|ol|li|table|h[1-6]|blockquote)""", RegexOption.IGNORE_CASE).containsMatchIn(html)
    if (!isRichHtml) {
        val lines = html.split(Regex("""\r?\n"""))
        val sb = StringBuilder()
        var inList = false
        for (line in lines) {
            val trimmed = line.trim()
            val bulletMatch = Regex("""^[-*•]\s+(.*)$""").find(trimmed)
            if (bulletMatch != null) {
                if (!inList) {
                    sb.append("<ul>")
                    inList = true
                }
                sb.append("<li>").append(bulletMatch.groupValues[1]).append("</li>")
            } else {
                if (inList) {
                    sb.append("</ul>")
                    inList = false
                }
                if (trimmed.isNotBlank()) {
                    sb.append("<p>").append(trimmed).append("</p>")
                }
            }
        }
        if (inList) sb.append("</ul>")
        if (sb.isNotEmpty()) {
            html = sb.toString()
        }
    }

    return html
}

/**
 * Parses an HTML/Markdown string into clean plain text and a list of FormatSpan objects.
 * Guarantees zero raw HTML tags remain in the user-visible plain text string.
 */
fun parseHtmlToRichText(content: String): Pair<String, List<FormatSpan>> {
    if (content.isBlank()) return Pair("", emptyList())

    val normalized = normalizeToEditableHtml(content)
    val plainBuilder = StringBuilder()
    val spans = mutableListOf<FormatSpan>()

    data class OpenTag(val tag: String, val startIndex: Int, val attr: String? = null)
    val tagStack = mutableListOf<OpenTag>()

    var i = 0
    val len = normalized.length

    while (i < len) {
        if (normalized[i] == '<') {
            val closeIndex = normalized.indexOf('>', i)
            if (closeIndex != -1) {
                val fullTag = normalized.substring(i + 1, closeIndex).trim()
                val isClosing = fullTag.startsWith("/")
                val tagName = if (isClosing) {
                    fullTag.substring(1).trim().lowercase()
                } else {
                    fullTag.split(Regex("""\s+"""))[0].lowercase().removeSuffix("/")
                }

                when (tagName) {
                    "br" -> {
                        plainBuilder.append('\n')
                    }
                    "p", "div" -> {
                        if (isClosing) {
                            if (plainBuilder.isNotEmpty() && !plainBuilder.endsWith("\n")) {
                                plainBuilder.append('\n')
                            }
                        } else {
                            if (plainBuilder.isNotEmpty() && !plainBuilder.endsWith("\n")) {
                                plainBuilder.append('\n')
                            }
                        }
                    }
                    "li" -> {
                        if (!isClosing) {
                            if (plainBuilder.isNotEmpty() && !plainBuilder.endsWith("\n")) {
                                plainBuilder.append('\n')
                            }
                            plainBuilder.append("• ")
                        } else {
                            if (plainBuilder.isNotEmpty() && !plainBuilder.endsWith("\n")) {
                                plainBuilder.append('\n')
                            }
                        }
                    }
                    "b", "strong" -> {
                        if (isClosing) {
                            val open = tagStack.findLast { it.tag == "b" }
                            if (open != null) {
                                tagStack.remove(open)
                                if (open.startIndex < plainBuilder.length) {
                                    spans.add(FormatSpan(open.startIndex, plainBuilder.length, isBold = true))
                                }
                            }
                        } else {
                            tagStack.add(OpenTag("b", plainBuilder.length))
                        }
                    }
                    "i", "em" -> {
                        if (isClosing) {
                            val open = tagStack.findLast { it.tag == "i" }
                            if (open != null) {
                                tagStack.remove(open)
                                if (open.startIndex < plainBuilder.length) {
                                    spans.add(FormatSpan(open.startIndex, plainBuilder.length, isItalic = true))
                                }
                            }
                        } else {
                            tagStack.add(OpenTag("i", plainBuilder.length))
                        }
                    }
                    "u", "ins" -> {
                        if (isClosing) {
                            val open = tagStack.findLast { it.tag == "u" }
                            if (open != null) {
                                tagStack.remove(open)
                                if (open.startIndex < plainBuilder.length) {
                                    spans.add(FormatSpan(open.startIndex, plainBuilder.length, isUnderline = true))
                                }
                            }
                        } else {
                            tagStack.add(OpenTag("u", plainBuilder.length))
                        }
                    }
                    "s", "del", "strike" -> {
                        if (isClosing) {
                            val open = tagStack.findLast { it.tag == "s" }
                            if (open != null) {
                                tagStack.remove(open)
                                if (open.startIndex < plainBuilder.length) {
                                    spans.add(FormatSpan(open.startIndex, plainBuilder.length, isStrike = true))
                                }
                            }
                        } else {
                            tagStack.add(OpenTag("s", plainBuilder.length))
                        }
                    }
                    "a" -> {
                        if (isClosing) {
                            val open = tagStack.findLast { it.tag == "a" }
                            if (open != null) {
                                tagStack.remove(open)
                                if (open.startIndex < plainBuilder.length) {
                                    spans.add(FormatSpan(open.startIndex, plainBuilder.length, url = open.attr))
                                }
                            }
                        } else {
                            val hrefMatch = Regex("""href=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(fullTag)
                            val href = hrefMatch?.groupValues?.get(1) ?: ""
                            tagStack.add(OpenTag("a", plainBuilder.length, href))
                        }
                    }
                }
                i = closeIndex + 1
                continue
            }
        }

        // Entity decoding
        if (normalized[i] == '&') {
            val semi = normalized.indexOf(';', i)
            if (semi != -1 && semi - i <= 10) {
                val entity = normalized.substring(i, semi + 1)
                when (entity) {
                    "&nbsp;" -> plainBuilder.append(' ')
                    "&amp;" -> plainBuilder.append('&')
                    "&lt;" -> plainBuilder.append('<')
                    "&gt;" -> plainBuilder.append('>')
                    "&quot;" -> plainBuilder.append('"')
                    else -> plainBuilder.append(entity)
                }
                i = semi + 1
                continue
            }
        }

        plainBuilder.append(normalized[i])
        i++
    }

    return Pair(plainBuilder.toString(), spans)
}

/**
 * Serializes plain text and FormatSpan objects into clean HTML format for storage.
 */
fun richTextToHtml(plainText: String, spans: List<FormatSpan>): String {
    if (plainText.isEmpty()) return ""

    val boundaries = sortedSetOf(0, plainText.length)
    for (span in spans) {
        boundaries.add(span.start.coerceIn(0, plainText.length))
        boundaries.add(span.end.coerceIn(0, plainText.length))
    }

    val points = boundaries.toList()
    val sb = StringBuilder()

    for (idx in 0 until points.size - 1) {
        val start = points[idx]
        val end = points[idx + 1]
        if (start >= end) continue

        val slice = plainText.substring(start, end)
        val activeSpans = spans.filter { it.start <= start && it.end >= end }
        val isBold = activeSpans.any { it.isBold }
        val isItalic = activeSpans.any { it.isItalic }
        val isUnderline = activeSpans.any { it.isUnderline }
        val isStrike = activeSpans.any { it.isStrike }
        val linkUrl = activeSpans.firstOrNull { !it.url.isNullOrBlank() }?.url

        if (!linkUrl.isNullOrBlank()) sb.append("<a href=\"").append(linkUrl).append("\">")
        if (isBold) sb.append("<b>")
        if (isItalic) sb.append("<i>")
        if (isUnderline) sb.append("<u>")
        if (isStrike) sb.append("<s>")

        for (ch in slice) {
            when (ch) {
                '\n' -> sb.append("<br>")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '&' -> sb.append("&amp;")
                else -> sb.append(ch)
            }
        }

        if (isStrike) sb.append("</s>")
        if (isUnderline) sb.append("</u>")
        if (isItalic) sb.append("</i>")
        if (isBold) sb.append("</b>")
        if (!linkUrl.isNullOrBlank()) sb.append("</a>")
    }

    return sb.toString()
}

/**
 * Builds an AnnotatedString from plain text and formatting spans with full styling.
 */
fun buildFormattedAnnotatedString(plainText: String, spans: List<FormatSpan>): AnnotatedString {
    return buildAnnotatedString {
        append(plainText)
        for (span in spans) {
            val start = span.start.coerceIn(0, plainText.length)
            val end = span.end.coerceIn(0, plainText.length)
            if (start >= end) continue

            if (span.isBold) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)), start, end)
            }
            if (span.isItalic) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
            }
            if (span.isUnderline) {
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            }
            if (span.isStrike) {
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
            }
            if (!span.url.isNullOrBlank()) {
                addStyle(
                    SpanStyle(
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
                addStringAnnotation(tag = "URL", annotation = span.url, start = start, end = end)
            }
        }
    }
}

/**
 * Adjusts existing spans when text is edited (character insertion, deletion, or replacement).
 */
fun adjustSpansOnTextChange(
    oldText: String,
    newText: String,
    spans: List<FormatSpan>
): List<FormatSpan> {
    if (oldText == newText) return spans
    if (newText.isEmpty()) return emptyList()

    var prefixLen = 0
    while (prefixLen < oldText.length && prefixLen < newText.length && oldText[prefixLen] == newText[prefixLen]) {
        prefixLen++
    }

    var oldSuffixLen = 0
    var newSuffixLen = 0
    while (
        oldSuffixLen < (oldText.length - prefixLen) &&
        newSuffixLen < (newText.length - prefixLen) &&
        oldText[oldText.length - 1 - oldSuffixLen] == newText[newText.length - 1 - newSuffixLen]
    ) {
        oldSuffixLen++
        newSuffixLen++
    }

    val deletedStart = prefixLen
    val deletedEnd = oldText.length - oldSuffixLen
    val deletedLength = deletedEnd - deletedStart

    val insertedStart = prefixLen
    val insertedEnd = newText.length - newSuffixLen
    val insertedLength = insertedEnd - insertedStart
    val delta = insertedLength - deletedLength

    val newSpans = mutableListOf<FormatSpan>()
    for (span in spans) {
        var s = span.start
        var e = span.end

        if (delta > 0) {
            // Insertion
            if (s >= insertedStart) s += delta
            if (e > insertedStart) e += delta
        } else if (delta < 0) {
            // Deletion
            val delLen = -delta
            if (s >= deletedEnd) {
                s -= delLen
                e -= delLen
            } else if (e <= deletedStart) {
                // Before deleted segment, unaffected
            } else {
                s = s.coerceAtMost(deletedStart)
                e = (e - delLen).coerceAtLeast(deletedStart)
            }
        }

        s = s.coerceIn(0, newText.length)
        e = e.coerceIn(0, newText.length)
        if (s < e) {
            newSpans.add(span.copy(start = s, end = e))
        }
    }
    return newSpans
}

private fun findWordBoundary(text: String, index: Int): Pair<Int, Int>? {
    if (text.isEmpty()) return null
    val target = (index - 1).coerceAtLeast(0)
    if (target >= text.length) return null

    var start = if (index < text.length && !text[index].isWhitespace()) index else target
    if (start >= text.length || text[start].isWhitespace()) return null

    while (start > 0 && !text[start - 1].isWhitespace() && text[start - 1] !in "<>[](){},.;:!?\"'/\\") {
        start--
    }
    var end = start
    while (end < text.length && !text[end].isWhitespace() && text[end] !in "<>[](){},.;:!?\"'/\\") {
        end++
    }
    return if (start < end) Pair(start, end) else null
}

/**
 * Refactored WYSIWYG rich text editor using AnnotatedString and TextFieldValue for
 * handling local formatting states (bold/italic/underline) without using HTML-style string tags.
 * Ensures the cursor remains completely stable and text selection tools (native Cut/Copy/Paste
 * and top toolbar actions) function smoothly.
 */
@Composable
fun HtmlWysiwygEditor(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "Type details here... Format with bold, underline, lists or links.",
    minHeight: Dp = 100.dp,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "wysiwyg_editor"
) {
    val context = LocalContext.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    // In-memory plain text and formatting spans
    var currentPlainText by remember { mutableStateOf("") }
    var currentSpans by remember { mutableStateOf(listOf<FormatSpan>()) }
    var lastEmittedHtml by remember { mutableStateOf("") }

    // Pending formatting state when cursor is collapsed (active style for newly typed characters)
    var isTypingBold by remember { mutableStateOf(false) }
    var isTypingItalic by remember { mutableStateOf(false) }

    // Core TextFieldValue state carrying the AnnotatedString and stable cursor/selection
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(annotatedString = AnnotatedString("")))
    }

    // Stable sync: only parse external value if it differs from what the editor last emitted
    LaunchedEffect(value) {
        if (value != lastEmittedHtml) {
            val (plain, spans) = parseHtmlToRichText(value)
            currentPlainText = plain
            currentSpans = spans
            lastEmittedHtml = value
            val annotated = buildFormattedAnnotatedString(plain, spans)

            // Preserve current cursor position if valid, otherwise place at end
            val existingSel = textFieldValue.selection
            val safeStart = existingSel.start.coerceIn(0, plain.length)
            val safeEnd = existingSel.end.coerceIn(0, plain.length)
            val newSelection = if (textFieldValue.text.isEmpty()) {
                TextRange(plain.length)
            } else {
                TextRange(safeStart, safeEnd)
            }

            textFieldValue = TextFieldValue(annotatedString = annotated, selection = newSelection)
        }
    }

    var lastSelectionRange by remember { mutableStateOf(TextRange.Zero) }
    var showLinkDialog by remember { mutableStateOf(false) }

    // Determine active formatting state at cursor or selection
    val isSelectionActive = textFieldValue.selection.start != textFieldValue.selection.end
    val activeStart = if (isSelectionActive) minOf(textFieldValue.selection.start, textFieldValue.selection.end) else textFieldValue.selection.start
    val activeEnd = if (isSelectionActive) maxOf(textFieldValue.selection.start, textFieldValue.selection.end) else textFieldValue.selection.start

    val isBoldActive = if (isSelectionActive) {
        currentSpans.any { it.isBold && it.start <= activeStart && it.end >= activeEnd }
    } else {
        isTypingBold || currentSpans.any { it.isBold && it.start <= activeStart && it.end >= activeStart && it.start < it.end }
    }

    val isItalicActive = if (isSelectionActive) {
        currentSpans.any { it.isItalic && it.start <= activeStart && it.end >= activeEnd }
    } else {
        isTypingItalic || currentSpans.any { it.isItalic && it.start <= activeStart && it.end >= activeStart && it.start < it.end }
    }

    val isUnderlineActive = if (isSelectionActive) {
        currentSpans.any { it.isUnderline && it.start <= activeStart && it.end >= activeEnd }
    } else {
        currentSpans.any { it.isUnderline && it.start <= activeStart && it.end >= activeStart && it.start < it.end }
    }

    // Resolves target range for formatting (selection, or word boundary under cursor)
    fun resolveTargetRange(): Pair<Int, Int>? {
        val sel = if (textFieldValue.selection.start != textFieldValue.selection.end) {
            textFieldValue.selection
        } else if (lastSelectionRange.start != lastSelectionRange.end && lastSelectionRange.end <= currentPlainText.length) {
            lastSelectionRange
        } else {
            textFieldValue.selection
        }

        return if (sel.start != sel.end) {
            val s = minOf(sel.start, sel.end).coerceIn(0, currentPlainText.length)
            val e = maxOf(sel.start, sel.end).coerceIn(0, currentPlainText.length)
            if (s < e) Pair(s, e) else null
        } else {
            findWordBoundary(currentPlainText, sel.start.coerceIn(0, currentPlainText.length))
        }
    }

    // Toggle bold state using AnnotatedString and TextFieldValue
    fun toggleBold() {
        val range = resolveTargetRange()
        if (range != null) {
            val (start, end) = range
            val isAlreadyBold = currentSpans.any { it.isBold && it.start <= start && it.end >= end }
            val updated = currentSpans.toMutableList()
            if (isAlreadyBold) {
                updated.removeAll { it.isBold && it.start >= start && it.end <= end }
            } else {
                updated.add(FormatSpan(start = start, end = end, isBold = true))
            }
            currentSpans = updated
            val newHtml = richTextToHtml(currentPlainText, updated)
            lastEmittedHtml = newHtml
            val annotated = buildFormattedAnnotatedString(currentPlainText, updated)

            // Maintain stable selection range
            textFieldValue = TextFieldValue(annotatedString = annotated, selection = TextRange(start, end))
            lastSelectionRange = TextRange(start, end)
            onChange(newHtml)
        } else {
            isTypingBold = !isTypingBold
        }
    }

    // Toggle italic state using AnnotatedString and TextFieldValue
    fun toggleItalic() {
        val range = resolveTargetRange()
        if (range != null) {
            val (start, end) = range
            val isAlreadyItalic = currentSpans.any { it.isItalic && it.start <= start && it.end >= end }
            val updated = currentSpans.toMutableList()
            if (isAlreadyItalic) {
                updated.removeAll { it.isItalic && it.start >= start && it.end <= end }
            } else {
                updated.add(FormatSpan(start = start, end = end, isItalic = true))
            }
            currentSpans = updated
            val newHtml = richTextToHtml(currentPlainText, updated)
            lastEmittedHtml = newHtml
            val annotated = buildFormattedAnnotatedString(currentPlainText, updated)

            // Maintain stable selection range
            textFieldValue = TextFieldValue(annotatedString = annotated, selection = TextRange(start, end))
            lastSelectionRange = TextRange(start, end)
            onChange(newHtml)
        } else {
            isTypingItalic = !isTypingItalic
        }
    }

    // Toggle underline state using AnnotatedString and TextFieldValue
    fun toggleUnderline() {
        val range = resolveTargetRange() ?: return
        val (start, end) = range
        val isAlreadyUnderline = currentSpans.any { it.isUnderline && it.start <= start && it.end >= end }
        val updated = currentSpans.toMutableList()
        if (isAlreadyUnderline) {
            updated.removeAll { it.isUnderline && it.start >= start && it.end <= end }
        } else {
            updated.add(FormatSpan(start = start, end = end, isUnderline = true))
        }
        currentSpans = updated
        val newHtml = richTextToHtml(currentPlainText, updated)
        lastEmittedHtml = newHtml
        val annotated = buildFormattedAnnotatedString(currentPlainText, updated)

        // Maintain stable selection range
        textFieldValue = TextFieldValue(annotatedString = annotated, selection = TextRange(start, end))
        lastSelectionRange = TextRange(start, end)
        onChange(newHtml)
    }

    // Insert bullet list item with stable cursor
    fun insertBulletList() {
        val cursor = textFieldValue.selection.start.coerceIn(0, currentPlainText.length)
        val prefix = if (cursor > 0 && currentPlainText[cursor - 1] != '\n') "\n• " else "• "
        val newPlain = currentPlainText.replaceRange(cursor, cursor, prefix)
        val updatedSpans = adjustSpansOnTextChange(currentPlainText, newPlain, currentSpans)
        currentPlainText = newPlain
        currentSpans = updatedSpans
        val newHtml = richTextToHtml(newPlain, updatedSpans)
        lastEmittedHtml = newHtml
        val annotated = buildFormattedAnnotatedString(newPlain, updatedSpans)
        val newCursor = cursor + prefix.length
        textFieldValue = TextFieldValue(annotatedString = annotated, selection = TextRange(newCursor))
        lastSelectionRange = TextRange.Zero
        onChange(newHtml)
    }

    // Clear formatting from selection or all text
    fun removeFormatting() {
        val range = resolveTargetRange()
        val updated = if (range != null) {
            val (start, end) = range
            currentSpans.filterNot { it.start < end && it.end > start }
        } else {
            emptyList()
        }
        isTypingBold = false
        isTypingItalic = false
        currentSpans = updated
        val newHtml = richTextToHtml(currentPlainText, updated)
        lastEmittedHtml = newHtml
        val annotated = buildFormattedAnnotatedString(currentPlainText, updated)
        val currentSel = textFieldValue.selection
        textFieldValue = TextFieldValue(annotatedString = annotated, selection = currentSel)
        onChange(newHtml)
    }

    // Insert or format link
    fun insertLink(url: String, label: String) {
        val range = resolveTargetRange()
        val cleanUrl = url.trim()
        val cleanLabel = label.trim().ifBlank { cleanUrl }

        val newPlain: String
        val newSpans: MutableList<FormatSpan>
        val newCursor: Int

        if (range != null) {
            val (start, end) = range
            newPlain = currentPlainText
            newSpans = currentSpans.toMutableList()
            newSpans.removeAll { !it.url.isNullOrBlank() && it.start < end && it.end > start }
            newSpans.add(FormatSpan(start = start, end = end, url = cleanUrl))
            newCursor = end
        } else {
            val cursor = textFieldValue.selection.start.coerceIn(0, currentPlainText.length)
            newPlain = currentPlainText.replaceRange(cursor, cursor, cleanLabel)
            newSpans = adjustSpansOnTextChange(currentPlainText, newPlain, currentSpans).toMutableList()
            newSpans.add(FormatSpan(start = cursor, end = cursor + cleanLabel.length, url = cleanUrl))
            newCursor = cursor + cleanLabel.length
        }

        currentPlainText = newPlain
        currentSpans = newSpans
        val newHtml = richTextToHtml(newPlain, newSpans)
        lastEmittedHtml = newHtml
        val annotated = buildFormattedAnnotatedString(newPlain, newSpans)
        textFieldValue = TextFieldValue(annotatedString = annotated, selection = TextRange(newCursor))
        lastSelectionRange = TextRange.Zero
        onChange(newHtml)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
            .testTag(testTagPrefix)
    ) {
        // Compact Formatting Toolbar with active style indicators matching screenshot
        Surface(
            color = Color(0xFFF8FAFC),
            border = BorderStroke(0.dp, Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Bold (B) Button with Active State
                    IconButton(
                        onClick = { toggleBold() },
                        modifier = Modifier
                            .size(32.dp)
                            .then(
                                if (isBoldActive) {
                                    Modifier.background(Color(0xFFEEF2FF), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(6.dp))
                                } else Modifier
                            )
                            .testTag("${testTagPrefix}_btn_bold")
                    ) {
                        Text(
                            text = "B",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isBoldActive) Color(0xFF4F46E5) else Color(0xFF0F172A)
                        )
                    }

                    // Italic (I) Button with Active State
                    IconButton(
                        onClick = { toggleItalic() },
                        modifier = Modifier
                            .size(32.dp)
                            .then(
                                if (isItalicActive) {
                                    Modifier.background(Color(0xFFEEF2FF), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(6.dp))
                                } else Modifier
                            )
                            .testTag("${testTagPrefix}_btn_italic")
                    ) {
                        Text(
                            text = "I",
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (isItalicActive) Color(0xFF4F46E5) else Color(0xFF334155)
                        )
                    }

                    // Underline (U) Button with Active State
                    IconButton(
                        onClick = { toggleUnderline() },
                        modifier = Modifier
                            .size(32.dp)
                            .then(
                                if (isUnderlineActive) {
                                    Modifier.background(Color(0xFFEEF2FF), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(6.dp))
                                } else Modifier
                            )
                            .testTag("${testTagPrefix}_btn_underline")
                    ) {
                        Text(
                            text = "U",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isUnderlineActive) Color(0xFF4F46E5) else Color(0xFF334155),
                            style = TextStyle(textDecoration = TextDecoration.Underline)
                        )
                    }

                    // Bullet List
                    IconButton(
                        onClick = { insertBulletList() },
                        modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_btn_list")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "Bullet List",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(18.dp).padding(horizontal = 4.dp), color = Color(0xFFCBD5E1))

                    // Link Button with Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEEF2FF),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                        modifier = Modifier
                            .clickable { showLinkDialog = true }
                            .testTag("${testTagPrefix}_btn_link")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(14.dp))
                            Text("Link", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // In-Toolbar Copy Button
                    IconButton(
                        onClick = {
                            val sel = textFieldValue.selection
                            val copyText = if (sel.start != sel.end) {
                                val min = minOf(sel.start, sel.end).coerceIn(0, currentPlainText.length)
                                val max = maxOf(sel.start, sel.end).coerceIn(0, currentPlainText.length)
                                currentPlainText.substring(min, max)
                            } else {
                                currentPlainText
                            }
                            if (copyText.isNotEmpty()) {
                                clipboardManager?.setPrimaryClip(ClipData.newPlainText("text", copyText))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(30.dp).testTag("${testTagPrefix}_btn_copy")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // In-Toolbar Paste Button
                    IconButton(
                        onClick = {
                            val clipData = clipboardManager?.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val pasteText = clipData.getItemAt(0).text?.toString() ?: ""
                                if (pasteText.isNotEmpty()) {
                                    val sel = textFieldValue.selection
                                    val min = minOf(sel.start, sel.end).coerceIn(0, currentPlainText.length)
                                    val max = maxOf(sel.start, sel.end).coerceIn(0, currentPlainText.length)
                                    val newPlain = currentPlainText.replaceRange(min, max, pasteText)
                                    val updatedSpans = adjustSpansOnTextChange(currentPlainText, newPlain, currentSpans)
                                    currentPlainText = newPlain
                                    currentSpans = updatedSpans
                                    val newHtml = richTextToHtml(newPlain, updatedSpans)
                                    lastEmittedHtml = newHtml
                                    val annotated = buildFormattedAnnotatedString(newPlain, updatedSpans)
                                    val newCursor = min + pasteText.length
                                    textFieldValue = TextFieldValue(annotatedString = annotated, selection = TextRange(newCursor))
                                    onChange(newHtml)
                                }
                            }
                        },
                        modifier = Modifier.size(30.dp).testTag("${testTagPrefix}_btn_paste")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Remove Formatting (Tx icon on the right, matching screenshot)
                    IconButton(
                        onClick = { removeFormatting() },
                        modifier = Modifier.size(30.dp).testTag("${testTagPrefix}_btn_clear_format")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatClear,
                            contentDescription = "Clear Formatting",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFE2E8F0))

        // Single Unified Editable Canvas: pure clean styled text with zero visible HTML tags
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .padding(14.dp)
        ) {
            if (currentPlainText.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF94A3B8),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Track selection when range is active
                    if (newValue.selection.start != newValue.selection.end) {
                        lastSelectionRange = newValue.selection
                    }

                    val oldPlain = currentPlainText
                    val newPlain = newValue.text

                    if (newPlain != oldPlain) {
                        // Text was inserted, replaced, or deleted
                        var updatedSpans = adjustSpansOnTextChange(oldPlain, newPlain, currentSpans)

                        // If user activated typing style (bold/italic) at cursor, apply to typed char
                        if (isTypingBold && newPlain.length > oldPlain.length) {
                            val diffStart = newValue.selection.start - (newPlain.length - oldPlain.length)
                            if (diffStart >= 0) {
                                updatedSpans = updatedSpans + FormatSpan(diffStart, newValue.selection.start, isBold = true)
                            }
                        }
                        if (isTypingItalic && newPlain.length > oldPlain.length) {
                            val diffStart = newValue.selection.start - (newPlain.length - oldPlain.length)
                            if (diffStart >= 0) {
                                updatedSpans = updatedSpans + FormatSpan(diffStart, newValue.selection.start, isItalic = true)
                            }
                        }

                        currentPlainText = newPlain
                        currentSpans = updatedSpans
                        val newHtml = richTextToHtml(newPlain, updatedSpans)
                        lastEmittedHtml = newHtml
                        val annotated = buildFormattedAnnotatedString(newPlain, updatedSpans)

                        // Keep IME selection precisely as reported
                        textFieldValue = newValue.copy(annotatedString = annotated)
                        onChange(newHtml)
                    } else {
                        // Cursor movement or selection change only: keep annotatedString, update selection smoothly
                        textFieldValue = newValue.copy(annotatedString = textFieldValue.annotatedString)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_input"),
                textStyle = TextStyle(
                    color = Color(0xFF0F172A),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily.Default
                ),
                cursorBrush = SolidColor(Color(0xFF4F46E5))
            )
        }
    }

    // Link Insertion Dialog with Auto-Scheme Detection
    if (showLinkDialog) {
        val initialSelectedText = if (textFieldValue.selection.start != textFieldValue.selection.end) {
            val min = minOf(textFieldValue.selection.start, textFieldValue.selection.end).coerceIn(0, currentPlainText.length)
            val max = maxOf(textFieldValue.selection.start, textFieldValue.selection.end).coerceIn(0, currentPlainText.length)
            currentPlainText.substring(min, max)
        } else ""

        LinkInsertDialog(
            initialText = initialSelectedText,
            onDismiss = { showLinkDialog = false },
            onConfirm = { url, label ->
                insertLink(url, label)
                showLinkDialog = false
            }
        )
    }
}


/**
 * Clean, modern dialog for inserting links with automatic scheme detection
 * supporting UpNote, Obsidian, Notion, MarginNote, and standard web links.
 */
@Composable
fun LinkInsertDialog(
    initialText: String = "",
    onDismiss: () -> Unit,
    onConfirm: (url: String, label: String) -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    var labelText by remember { mutableStateOf(initialText) }

    // Auto-detect scheme and suggest label if user hasn't explicitly typed one
    LaunchedEffect(urlText) {
        if (labelText.isBlank() || labelText == initialText) {
            val trimmed = urlText.trim()
            if (trimmed.isNotBlank()) {
                labelText = getSchemeDisplayName(trimmed)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                        Text(
                            text = "Insert Link / App Link",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // URL Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "URL or Native App Scheme *",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        placeholder = { Text("upnote://..., obsidian://..., https://...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth().testTag("wysiwyg_link_url_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
                    )
                    Text(
                        text = "Supports UpNote, Obsidian, Notion, MarginNote & web links",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Display Text Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Link Display Text (Optional)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )
                    OutlinedTextField(
                        value = labelText,
                        onValueChange = { labelText = it },
                        placeholder = { Text("e.g. My UpNote Note", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth().testTag("wysiwyg_link_label_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (urlText.isNotBlank()) onConfirm(urlText, labelText)
                        })
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (urlText.isNotBlank()) onConfirm(urlText, labelText)
                        },
                        enabled = urlText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("wysiwyg_insert_link_confirm_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Insert Link", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
