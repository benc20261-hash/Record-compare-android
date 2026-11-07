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

    @Test
    fun testHtmlToPlainTextPreservesNewlinesAndDoesNotJoinWords() {
        val html = "width, heigh, <b>depth</b><br>29cm x 12.1cm x 2.75-5.09cm"
        val plain = com.example.ui.components.htmlToPlainText(html)

        assertTrue("Should contain newline between depth and 29cm", plain.contains("depth\n29cm"))
        assertTrue("Must NOT stick depth and 29cm together", !plain.contains("depth29cm"))
    }

    @Test
    fun testHtmlToPlainTextWithParagraphs() {
        val html = "<p>width, heigh, <b>depth</b></p><p>29cm x 12.1cm x 2.75-5.09cm</p>"
        val plain = com.example.ui.components.htmlToPlainText(html)

        assertTrue("Should contain newline between lines", plain.contains('\n'))
        assertTrue("Must NOT stick depth and 29cm together", !plain.contains("depth29cm"))
        assertTrue("Must preserve both depth and 29cm", plain.contains("depth") && plain.contains("29cm"))
    }

    @Test
    fun testParseNumericSpecWithNewlinesDoesNotStickWords() {
        val html = "width, heigh, <b>depth</b><br>29cm x 12.1cm x 2.75-5.09cm"
        val parsed = com.example.ui.screens.parseNumericSpec(html, "mm", "size", isNumeric = null)

        assertTrue("Must NOT stick depth and 29cm together", !parsed.rawText.contains("depth29cm"))
        assertTrue("Must contain newline in rawText", parsed.rawText.contains('\n'))
        assertEquals(false, parsed.hasNumeric)
    }

    @Test
    fun testSpecDashesAndQuotesPreservedVerbatim() {
        val rawInput = """
            -7"
            -1080p
            -120hz vrr
            -500 nits 100% srgb
            -gorilla protection
        """.trimIndent()

        val formatted = com.example.ui.components.formatSpecificationDetails(rawInput)
        assertTrue("Must preserve -7\" with quotes and dash intact", formatted.contains("-7\""))
        assertTrue("Must preserve -1080p with dash intact", formatted.contains("-1080p"))
        assertTrue("Must preserve -120hz vrr with dash intact", formatted.contains("-120hz vrr"))
        assertTrue("Must preserve -500 nits with dash intact", formatted.contains("-500 nits"))
        assertTrue("Must not contain bullet character •", !formatted.contains('•'))

        val (plain, _) = com.example.ui.components.parseHtmlToRichText(formatted)
        assertTrue("Plain text must keep -7\"", plain.contains("-7\""))
        assertTrue("Plain text must keep -1080p", plain.contains("-1080p"))
        assertTrue("Plain text must keep -500 nits", plain.contains("-500 nits"))
        assertTrue("Plain text must not contain •", !plain.contains('•'))
    }

    @Test
    fun testIsoDateFormattingInBeautifulDate() {
        val isoDate = "2026-10-25T01:19:48.906Z"
        val formatted = com.example.ui.components.formatBeautifulDate(isoDate)
        assertEquals("Oct 25, 2026", formatted)
    }
}

