package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.util.UriHelper
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Prepares and normalizes specification details or review summaries as rich text,
 * properly respecting newlines, bullet points (- Item, • Item), markdown (**bold**, *italic*),
 * and safe HTML/markdown tags (<b>, <strong>, <i>, <u>, <a>, <br>).
 */
fun formatSpecificationDetails(rawText: String?): String {
    if (rawText.isNullOrBlank()) return ""
    var formatted = rawText

    // 1. Normalize escaped literal "\n" strings to real newlines
    formatted = formatted.replace("\\n", "\n")

    // 2. Normalize <br> or <br/> tags to newlines for cleaner text flow
    formatted = formatted.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")

    // 3. Clean up inline dashes like "from a distance. - Wide-Stereo" -> newlines with bullets
    formatted = formatted.replace(Regex("([.!?]|\\w|>)\\s+-\\s+([A-Z0-9<])"), "$1\n• $2")

    // 4. Handle start of string dashes: "- Item" -> "• Item"
    formatted = formatted.replace(Regex("(?m)^-\\s+"), "• ")

    // 5. Sanitize scripts or dangerous iframes
    formatted = formatted.replace(Regex("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>", RegexOption.IGNORE_CASE), "")
    formatted = formatted.replace(Regex("<iframe\\b[^<]*(?:(?!</iframe>)<[^<]*)*</iframe>", RegexOption.IGNORE_CASE), "")

    return formatted
}

/**
 * Detects HTML links and raw deep links (such as upnote://... or https://...),
 * converting them into standard markdown [label](url) syntax for consistent rendering.
 */
fun normalizeRawLinks(text: String): String {
    if (text.isBlank()) return text

    // 1. Convert HTML <a href="...">label</a> to markdown [label](url)
    var result = text.replace(Regex("<a\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", RegexOption.IGNORE_CASE)) { match ->
        val url = match.groupValues[1].trim()
        val label = match.groupValues[2].trim().ifBlank { url }
        "[$label]($url)"
    }

    // 2. Convert raw app schemes and web URLs into [label](url)
    val rawUrlPattern = Regex("""(?<!\]\()(?<!href=["'])\b((?:https?|upnote|marginnote\w*|obsidian|notion|craftdocs|bear|things|onenote|goodnotes|notability|logseq)://[^\s<>"'\)]+)""", RegexOption.IGNORE_CASE)
    result = rawUrlPattern.replace(result) { match ->
        var raw = match.value
        // Strip trailing punctuation like comma, dot, colon, semicolon
        var trailing = ""
        while (raw.isNotEmpty() && (raw.endsWith(",") || raw.endsWith(".") || raw.endsWith(";") || raw.endsWith(":") || raw.endsWith(">") || raw.endsWith("!"))) {
            trailing = raw.takeLast(1) + trailing
            raw = raw.dropLast(1)
        }
        if (raw.isBlank()) {
            match.value
        } else {
            val label = getSchemeDisplayName(raw)
            "[$label]($raw)$trailing"
        }
    }

    return result
}

fun getSchemeDisplayName(url: String): String {
    val lower = url.lowercase()
    return when {
        lower.startsWith("upnote://") -> "📝 Open in UpNote"
        lower.startsWith("obsidian://") -> "💎 Open in Obsidian"
        lower.startsWith("notion://") -> "📓 Open in Notion"
        lower.startsWith("onenote://") -> "📒 Open in OneNote"
        lower.startsWith("marginnote") -> "📑 Open in MarginNote"
        lower.startsWith("craftdocs://") -> "📄 Open in Craft"
        lower.startsWith("bear://") -> "🐻 Open in Bear"
        lower.startsWith("things://") -> "☑️ Open in Things"
        lower.startsWith("goodnotes://") -> "✍️ Open in GoodNotes"
        lower.startsWith("notability://") -> "✏️ Open in Notability"
        lower.startsWith("logseq://") -> "🪵 Open in Logseq"
        else -> url
    }
}

/**
 * Parses markdown and simple HTML tags into a Jetpack Compose AnnotatedString.
 */
fun parseRichTextToAnnotatedString(
    text: String,
    baseColor: Color,
    linkColor: Color = Color(0xFF2563EB)
): AnnotatedString {
    val formatted = formatSpecificationDetails(text)
    if (formatted.isBlank()) {
        return AnnotatedString("")
    }

    val linkNormalized = normalizeRawLinks(formatted)

    // Replace HTML bold/italic/underline with markdown syntax first
    var processed = linkNormalized
        .replace(Regex("</?(?:b|strong)>", RegexOption.IGNORE_CASE), "**")
        .replace(Regex("</?(?:i|em)>", RegexOption.IGNORE_CASE), "*")
        .replace(Regex("</?u>", RegexOption.IGNORE_CASE), "<u>")

    return buildAnnotatedString {
        var i = 0
        val len = processed.length

        while (i < len) {
            when {
                // Markdown link: [label](url)
                processed.startsWith("[", i) -> {
                    val closeBracket = processed.indexOf(']', i)
                    if (closeBracket != -1 && closeBracket + 1 < len && processed[closeBracket + 1] == '(') {
                        val closeParen = processed.indexOf(')', closeBracket + 2)
                        if (closeParen != -1) {
                            val linkText = processed.substring(i + 1, closeBracket)
                            val linkUrl = processed.substring(closeBracket + 2, closeParen).trim()
                            val start = length
                            append(linkText)
                            val isApp = UriHelper.isAppSchemeUrl(linkUrl)
                            addStyle(
                                SpanStyle(
                                    color = if (isApp) Color(0xFF7C3AED) else linkColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = if (isApp) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                start,
                                length
                            )
                            addStringAnnotation(tag = "URL", annotation = linkUrl, start = start, end = length)
                            i = closeParen + 1
                            continue
                        }
                    }
                    append(processed[i])
                    i++
                }

                // Bold: **text** or __text__
                processed.startsWith("**", i) -> {
                    val endBold = processed.indexOf("**", i + 2)
                    if (endBold != -1) {
                        val boldContent = processed.substring(i + 2, endBold)
                        val start = length
                        append(boldContent)
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor), start, length)
                        i = endBold + 2
                    } else {
                        append(processed[i])
                        i++
                    }
                }

                // Underline tag <u>text</u>
                processed.startsWith("<u>", i, ignoreCase = true) -> {
                    val endU = processed.indexOf("<u>", i + 3, ignoreCase = true)
                    val endTag = if (endU != -1) endU else processed.indexOf("</u>", i + 3, ignoreCase = true)
                    if (endTag != -1) {
                        val uContent = processed.substring(i + 3, endTag)
                        val start = length
                        append(uContent)
                        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, length)
                        i = endTag + if (processed.substring(endTag).startsWith("</u>", ignoreCase = true)) 4 else 3
                    } else {
                        append(processed[i])
                        i++
                    }
                }

                // Italic: *text* (when not preceded or followed by *)
                processed.startsWith("*", i) && (i + 1 < len && processed[i + 1] != '*') -> {
                    val endItalic = processed.indexOf('*', i + 1)
                    if (endItalic != -1 && (endItalic + 1 >= len || processed[endItalic + 1] != '*')) {
                        val italicContent = processed.substring(i + 1, endItalic)
                        val start = length
                        append(italicContent)
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                        i = endItalic + 1
                    } else {
                        append(processed[i])
                        i++
                    }
                }

                // Strikethrough ~~text~~
                processed.startsWith("~~", i) -> {
                    val endStrike = processed.indexOf("~~", i + 2)
                    if (endStrike != -1) {
                        val strikeContent = processed.substring(i + 2, endStrike)
                        val start = length
                        append(strikeContent)
                        addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, length)
                        i = endStrike + 2
                    } else {
                        append(processed[i])
                        i++
                    }
                }

                else -> {
                    append(processed[i])
                    i++
                }
            }
        }
    }
}

/**
 * Rich spec details preview component matching upstream FormattedSpecHtml
 */
@Composable
fun FormattedSpecHtml(
    content: String?,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF1E293B),
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 18.sp
) {
    if (content.isNullOrBlank()) {
        Text(
            text = "No value provided",
            modifier = modifier,
            color = Color(0xFF94A3B8),
            fontStyle = FontStyle.Italic,
            fontSize = fontSize
        )
        return
    }

    val context = LocalContext.current
    val annotated = remember(content, color) {
        parseRichTextToAnnotatedString(content, color)
    }

    ClickableText(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight
        ),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    UriHelper.openAppUri(context, annotation.item)
                }
        }
    )
}

/**
 * Checks whether text content already includes the unit measurement as a suffix or word
 * (e.g. "30g" already has "g", "27db" already has "db", "2 years" already has "year"),
 * preventing redundant display like "30g [g]".
 */
fun hasUnitSuffix(text: String?, unit: String?): Boolean {
    if (text.isNullOrBlank() || unit.isNullOrBlank()) return false
    val cleanText = text.replace(Regex("<[^>]*>"), "").trim().lowercase()
    val cleanUnit = unit.trim().lowercase()
    if (cleanText.isEmpty() || cleanUnit.isEmpty()) return false

    // Exact suffix or pluralized suffix (e.g. "30g", "2 years")
    if (cleanText.endsWith(cleanUnit) || cleanText.endsWith("${cleanUnit}s")) {
        return true
    }

    // Word boundary or number boundary check (e.g. "30 cm distance", "30g net")
    val regex = Regex("(^|\\s|\\d)${Regex.escape(cleanUnit)}(s|\\b|$)", RegexOption.IGNORE_CASE)
    return regex.containsMatchIn(cleanText)
}

/**
 * Standard specification unit badge component matching upstream SpecUnitBadge
 */
@Composable
fun SpecUnitBadge(
    unit: String?,
    details: String? = null,
    modifier: Modifier = Modifier
) {
    if (unit.isNullOrBlank()) return
    if (!details.isNullOrBlank() && hasUnitSuffix(details, unit)) return

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
    ) {
        Text(
            text = unit.trim(),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

