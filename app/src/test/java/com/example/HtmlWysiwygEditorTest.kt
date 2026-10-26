package com.example

import com.example.ui.components.normalizeToEditableHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlWysiwygEditorTest {

    @Test
    fun testNormalizeBoldAndItalic() {
        val markdown = "This is **bold text** and *italic text* and ~~strike~~."
        val normalized = normalizeToEditableHtml(markdown)

        assertTrue(normalized.contains("<b>bold text</b>"))
        assertTrue(normalized.contains("<i>italic text</i>"))
        assertTrue(normalized.contains("<s>strike</s>"))
    }

    @Test
    fun testNormalizeMarkdownLinks() {
        val markdown = "Check out [My Note](upnote://x-callback-url/opennote?notedid=abc123) and [Website](https://google.com)"
        val normalized = normalizeToEditableHtml(markdown)

        assertTrue(normalized.contains("<a href=\"upnote://x-callback-url/opennote?notedid=abc123\">My Note</a>"))
        assertTrue(normalized.contains("<a href=\"https://google.com\">Website</a>"))
    }

    @Test
    fun testNormalizeBulletList() {
        val markdown = """
            Features:
            - Fast 65W charging
            - 120Hz display
            - 5000mAh battery
        """.trimIndent()

        val normalized = normalizeToEditableHtml(markdown)

        assertTrue(normalized.contains("<ul>"))
        assertTrue(normalized.contains("<li>Fast 65W charging</li>"))
        assertTrue(normalized.contains("<li>120Hz display</li>"))
        assertTrue(normalized.contains("<li>5000mAh battery</li>"))
        assertTrue(normalized.contains("</ul>"))
    }

    @Test
    fun testPreservesExistingHtml() {
        val html = "<p>Already <b>formatted</b> with <u>underline</u> and <a href=\"upnote://note\">UpNote</a></p>"
        val normalized = normalizeToEditableHtml(html)

        assertEquals(html, normalized)
    }

    @Test
    fun testBlankContent() {
        assertEquals("", normalizeToEditableHtml(""))
        assertEquals("", normalizeToEditableHtml("   "))
    }
}
