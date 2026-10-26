package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.util.UriHelper

/**
 * Normalizes initial text, converting legacy Markdown (**bold**, *italic*, [label](url))
 * and bullet lines into clean, editable HTML matching upstream HtmlWysiwygEditor.
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
        val url = match.groupValues[2]
        "<a href=\"$url\">$label</a>"
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
 * Robust WYSIWYG rich text editor component for Jetpack Compose matching upstream HtmlWysiwygEditor.tsx.
 * Provides a compact formatting toolbar (Bold, Italic, Underline, Bullet List, Link modal, and Clear Format)
 * with selection-aware tag insertion and live real-time formatted preview.
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
    var textFieldValue by remember(value) {
        // Only update internal TextFieldValue if the string changed externally
        val currentText = value
        mutableStateOf(TextFieldValue(text = currentText, selection = TextRange(currentText.length)))
    }

    var showLinkDialog by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(true) }

    // Helper to wrap selected text or insert formatting tags around cursor
    fun applyWrap(openTag: String, closeTag: String, fallbackText: String = "") {
        val sel = textFieldValue.selection
        val text = textFieldValue.text
        val newText: String
        val newCursor: Int

        if (sel.start != sel.end) {
            val selectedText = text.substring(sel.start, sel.end)
            newText = text.replaceRange(sel.start, sel.end, "$openTag$selectedText$closeTag")
            newCursor = sel.start + openTag.length + selectedText.length + closeTag.length
        } else {
            val insert = if (fallbackText.isNotBlank()) "$openTag$fallbackText$closeTag" else "$openTag$closeTag"
            newText = text.replaceRange(sel.start, sel.start, insert)
            newCursor = sel.start + openTag.length + fallbackText.length
        }

        textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
        onChange(newText)
    }

    // Helper to insert bullet list item
    fun insertBulletList() {
        val sel = textFieldValue.selection
        val text = textFieldValue.text
        val prefix = if (sel.start > 0 && text[sel.start - 1] != '\n') "\n• " else "• "
        val newText = text.replaceRange(sel.start, sel.end, prefix)
        val newCursor = sel.start + prefix.length

        textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
        onChange(newText)
    }

    // Helper to strip HTML tags from selection or whole text
    fun removeFormatting() {
        val sel = textFieldValue.selection
        val text = textFieldValue.text
        val tagRegex = Regex("""<[^>]*>""")

        if (sel.start != sel.end) {
            val selectedText = text.substring(sel.start, sel.end)
            val cleaned = selectedText.replace(tagRegex, "")
            val newText = text.replaceRange(sel.start, sel.end, cleaned)
            val newCursor = sel.start + cleaned.length
            textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
            onChange(newText)
        } else {
            val cleaned = text.replace(tagRegex, "")
            textFieldValue = TextFieldValue(text = cleaned, selection = TextRange(cleaned.length))
            onChange(cleaned)
        }
    }

    // Helper to insert an HTML link tag
    fun insertLink(url: String, label: String) {
        val sel = textFieldValue.selection
        val text = textFieldValue.text
        val cleanUrl = url.trim()
        val cleanLabel = label.trim().ifBlank { cleanUrl }
        val linkTag = "<a href=\"$cleanUrl\">$cleanLabel</a>"

        val newText = text.replaceRange(sel.start, sel.end, linkTag)
        val newCursor = sel.start + linkTag.length

        textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
        onChange(newText)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
            .testTag(testTagPrefix)
    ) {
        // Compact Formatting Toolbar matching upstream
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
                    // Bold (B)
                    IconButton(
                        onClick = { applyWrap("<b>", "</b>", "bold text") },
                        modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_btn_bold")
                    ) {
                        Text("B", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    }

                    // Italic (I)
                    IconButton(
                        onClick = { applyWrap("<i>", "</i>", "italic text") },
                        modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_btn_italic")
                    ) {
                        Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF334155))
                    }

                    // Underline (U)
                    IconButton(
                        onClick = { applyWrap("<u>", "</u>", "underlined text") },
                        modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_btn_underline")
                    ) {
                        Text(
                            text = "U",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF334155),
                            style = TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                        )
                    }

                    // Bullet List
                    IconButton(
                        onClick = { insertBulletList() },
                        modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_btn_list")
                    ) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", tint = Color(0xFF334155), modifier = Modifier.size(17.dp))
                    }

                    VerticalDivider(modifier = Modifier.height(18.dp).padding(horizontal = 2.dp), color = Color(0xFFCBD5E1))

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
                    // Preview Toggle Button
                    IconButton(
                        onClick = { showPreview = !showPreview },
                        modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_btn_toggle_preview")
                    ) {
                        Icon(
                            imageVector = if (showPreview) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Preview",
                            tint = if (showPreview) Color(0xFF4F46E5) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Remove Formatting Button
                    IconButton(
                        onClick = { removeFormatting() },
                        modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_btn_clear_format")
                    ) {
                        Icon(Icons.Default.FormatClear, contentDescription = "Clear Formatting", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFE2E8F0))

        // Content Editable Canvas (Multiline BasicTextField for full selection support)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .padding(12.dp)
        ) {
            if (textFieldValue.text.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Auto-link bare URLs on paste if user pasted a link
                    var updatedText = newValue.text
                    val isPaste = updatedText.length > textFieldValue.text.length + 5
                    if (isPaste && UriHelper.isAppSchemeUrl(updatedText) && !updatedText.contains("<a href=")) {
                        // Check if pasted snippet has bare scheme
                        val rawPattern = Regex("""(?<!href=["'])((?:https?|upnote|marginnote\w*|obsidian|notion|craftdocs|bear|things|onenote)://[^\s<>"'\)]+)""", RegexOption.IGNORE_CASE)
                        if (rawPattern.containsMatchIn(updatedText)) {
                            updatedText = rawPattern.replace(updatedText) { m ->
                                val linkUrl = m.value
                                val label = getSchemeDisplayName(linkUrl)
                                "<a href=\"$linkUrl\">$label</a>"
                            }
                        }
                    }

                    textFieldValue = if (updatedText != newValue.text) {
                        TextFieldValue(text = updatedText, selection = TextRange(updatedText.length))
                    } else {
                        newValue
                    }
                    onChange(textFieldValue.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_input"),
                textStyle = TextStyle(
                    color = Color(0xFF0F172A),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontFamily = FontFamily.Default
                ),
                cursorBrush = SolidColor(Color(0xFF4F46E5))
            )
        }

        // Live Real-Time Formatted Preview
        if (showPreview && textFieldValue.text.isNotBlank()) {
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Surface(
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(12.dp))
                        Text(
                            text = "REAL-TIME FORMATTED PREVIEW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(10.dp)) {
                            FormattedSpecHtml(
                                content = textFieldValue.text,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Link Insertion Dialog with Auto-Scheme Detection
    if (showLinkDialog) {
        val initialSelectedText = if (textFieldValue.selection.start != textFieldValue.selection.end) {
            textFieldValue.text.substring(textFieldValue.selection.start, textFieldValue.selection.end)
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
