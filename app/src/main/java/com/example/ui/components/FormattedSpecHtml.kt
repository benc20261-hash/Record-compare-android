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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Converts HTML/Rich text to clean plain text, reliably preserving newlines (\n)
 * from <br>, </p>, </div>, </li>, </tr>, and raw newlines without joining adjacent words together.
 */
fun htmlToPlainText(html: String?): String {
    if (html.isNullOrBlank()) return ""
    var text = html
    // 0. Normalize escaped literal "\n" strings to real newlines
    text = text.replace("\\n", "\n")
    // 1. Convert <br> or <br/> to newline
    text = text.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
    text = text.replace(Regex("""&lt;br\s*/?&gt;""", RegexOption.IGNORE_CASE), "\n")
    // 2. Convert block closing tags </p>, </div>, </li>, </tr>, </h[1-6]> to newline
    text = text.replace(Regex("""</(p|div|li|tr|h[1-6])>""", RegexOption.IGNORE_CASE), "\n")
    // 3. Convert block opening tags <p>, <div>, <li>, <tr>, <h[1-6]> to newline
    text = text.replace(Regex("""<(p|div|li|tr|h[1-6])\b[^>]*>""", RegexOption.IGNORE_CASE), "\n")
    // 4. Strip remaining HTML tags
    text = text.replace(Regex("""<[^>]*>"""), "")
    // 5. Decode common HTML entities
    text = text.replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
    // 6. Clean up line-by-line whitespace while strictly preserving line breaks
    val cleanedLines = text.split("\n").map { it.trim() }
    return cleanedLines.joinToString("\n").replace(Regex("""\n{3,}"""), "\n\n").trim()
}

/**
 * Prepares and normalizes specification details or review summaries as rich text,
 * properly respecting newlines, bullet points (- Item, • Item), markdown (**bold**, *italic*),
 * and safe HTML/markdown tags (<b>, <strong>, <i>, <u>, <a>, <br>).
 */
fun formatSpecificationDetails(rawText: String?): String {
    if (rawText.isNullOrBlank()) return ""
    var formatted = rawText

    // 0. Remove markdown images (![alt](url)) and html img tags so raw images/asset URLs never leak into text
    formatted = formatted.replace(Regex("""!\[.*?\]\(.*?\)"""), "")
    formatted = formatted.replace(Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE), "")
    formatted = formatted.replace(Regex("""<figure\b[^>]*>[\s\S]*?<\/figure>""", RegexOption.IGNORE_CASE), "")
    formatted = formatted.replace(Regex("""https?://local\.review/[^\s<>"'\)]+""", RegexOption.IGNORE_CASE), "")

    // 1. Normalize escaped literal "\n" strings to real newlines
    formatted = formatted.replace("\\n", "\n")

    // 2. Normalize <br> or <br/> or &lt;br&gt; tags to newlines for cleaner text flow
    formatted = formatted.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    formatted = formatted.replace(Regex("&lt;br\\s*/?&gt;", RegexOption.IGNORE_CASE), "\n")

    // 3. Clean up inline dashes after punctuation: "from a distance. - Wide-Stereo" -> newlines with dash
    // Preserves literal dashes without mutating to bullet points or dots
    formatted = formatted.replace(Regex("([.!?]|>)\\s+-\\s+([A-Za-z0-9<])"), "$1\n- $2")

    // 5. Sanitize scripts or dangerous iframes
    formatted = formatted.replace(Regex("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>", RegexOption.IGNORE_CASE), "")
    formatted = formatted.replace(Regex("<iframe\\b[^<]*(?:(?!</iframe>)<[^<]*)*</iframe>", RegexOption.IGNORE_CASE), "")

    // 6. Collapse excessive blank lines and trim leading/trailing empty lines
    formatted = formatted.replace(Regex("""\n{3,}"""), "\n\n").trim()

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
        if (url.contains("local.review") || url.contains("review_images") || url.contains("att_asset")) {
            ""
        } else {
            "[$label]($url)"
        }
    }

    // 2. Convert raw app schemes and web URLs into [label](url), skipping internal review image assets
    val rawUrlPattern = Regex("""(?<!\]\()(?<!href=["'])\b((?:https?|upnote|marginnote\w*|obsidian|notion|craftdocs|bear|things|onenote|goodnotes|notability|logseq)://[^\s<>"'\)]+)""", RegexOption.IGNORE_CASE)
    result = rawUrlPattern.replace(result) { match ->
        var raw = match.value
        // Strip trailing punctuation like comma, dot, colon, semicolon
        var trailing = ""
        while (raw.isNotEmpty() && (raw.endsWith(",") || raw.endsWith(".") || raw.endsWith(";") || raw.endsWith(":") || raw.endsWith(">") || raw.endsWith("!"))) {
            trailing = raw.takeLast(1) + trailing
            raw = raw.dropLast(1)
        }
        if (raw.isBlank() || raw.contains("local.review") || raw.contains("review_images") || raw.contains("att_asset")) {
            ""
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
 * Formats timestamps or date strings into a beautiful, human-readable format like "Sep 4, 2026".
 */
fun formatBeautifulDate(rawDate: String?): String {
    if (rawDate.isNullOrBlank()) {
        val now = System.currentTimeMillis()
        return SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(Date(now))
    }
    val trimmed = rawDate.trim()

    // 1. Try parsing numeric epoch millis
    val millis = trimmed.toLongOrNull()
    if (millis != null && millis > 0) {
        return SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(Date(millis))
    }

    // 2. Try parsing common date string formats
    val patterns = listOf(
        "M/d/yyyy",
        "MM/dd/yyyy",
        "M/d/yy",
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "dd/MM/yyyy",
        "d/M/yyyy",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    for (pattern in patterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
            sdf.isLenient = true
            val parsed = sdf.parse(trimmed)
            if (parsed != null) {
                return SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(parsed)
            }
        } catch (_: Exception) {}
    }

    return trimmed
}

/**
 * Parses markdown and HTML tags into a clean Jetpack Compose AnnotatedString.
 * Guarantees zero raw tags like <u>, <b>, <i>, or <u><u>* remain in the visible text.
 */
fun parseRichTextToAnnotatedString(
    text: String,
    baseColor: Color,
    linkColor: Color = Color(0xFF2563EB)
): AnnotatedString {
    if (text.isBlank()) {
        return AnnotatedString("")
    }

    // 1. Normalize linebreaks, bullets, and links
    val formatted = formatSpecificationDetails(text)
    val linkNormalized = normalizeRawLinks(formatted)

    // 2. Use parseHtmlToRichText to extract clean plain text and style spans
    val (plain, spans) = parseHtmlToRichText(linkNormalized)
    val trimmedPlain = plain.trimEnd('\r', '\n')
    if (trimmedPlain.isEmpty()) {
        return AnnotatedString("")
    }

    return buildAnnotatedString {
        append(trimmedPlain)
        for (span in spans) {
            val s = span.start.coerceIn(0, trimmedPlain.length)
            val e = span.end.coerceIn(0, trimmedPlain.length)
            if (s >= e) continue

            if (span.isBold) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor), s, e)
            }
            if (span.isItalic) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), s, e)
            }
            if (span.isUnderline) {
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), s, e)
            }
            if (span.isStrike) {
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), s, e)
            }
            if (!span.url.isNullOrBlank()) {
                val isApp = UriHelper.isAppSchemeUrl(span.url)
                addStyle(
                    SpanStyle(
                        color = if (isApp) Color(0xFF7C3AED) else linkColor,
                        fontWeight = if (isApp) FontWeight.Bold else FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    ),
                    s,
                    e
                )
                addStringAnnotation(tag = "URL", annotation = span.url, start = s, end = e)
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
    val cleanText = htmlToPlainText(text).lowercase()
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

