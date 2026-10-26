package com.example.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object UriHelper {

    // Known package names for quick target matching and Google Play Store fallbacks
    val APP_PACKAGES = mapOf(
        "upnote" to "com.getupnote.android",
        "obsidian" to "md.obsidian",
        "notion" to "notion.id",
        "onenote" to "com.microsoft.office.onenote"
    )

    /**
     * Checks if a URL string uses a native application custom scheme
     * (e.g. upnote://, marginnote4app://, obsidian://, notion://, etc.)
     */
    fun isAppSchemeUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        if (trimmed.startsWith("javascript:", ignoreCase = true) ||
            trimmed.startsWith("mailto:", ignoreCase = true) ||
            trimmed.startsWith("tel:", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)) {
            return false
        }
        val appSchemeRegex = Regex("^(upnote|marginnote\\w*|obsidian|notion|craftdocs|bear|things|onenote|goodnotes|notability|logseq):", RegexOption.IGNORE_CASE)
        if (appSchemeRegex.containsMatchIn(trimmed)) return true
        return Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(trimmed)
    }

    /**
     * Checks specifically if a URL string is an UpNote deep link
     */
    fun isUpNoteUri(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.trim().startsWith("upnote://", ignoreCase = true)
    }

    /**
     * Safely opens any URI, with specialized support for native app deep links (upnote://, obsidian://, etc.)
     * and graceful fallback handling if the app is not installed.
     */
    fun openAppUri(context: Context, rawUrl: String) {
        val trimmedUrl = rawUrl.trim()
        if (trimmedUrl.isBlank()) return

        if (isAppSchemeUrl(trimmedUrl)) {
            openNativeAppScheme(context, trimmedUrl)
            return
        }

        // Standard web or scheme link
        try {
            val uri = Uri.parse(trimmedUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // If missing scheme (e.g. "www.example.com"), try https://
                if (!trimmedUrl.contains("://")) {
                    val fallbackUri = Uri.parse("https://$trimmedUrl")
                    val intent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {}

            copyToClipboard(context, "Link", trimmedUrl)
            Toast.makeText(context, "Unable to open link. Copied to clipboard.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens native application deep link directly, or opens Google Play Store if not installed.
     */
    fun openNativeAppScheme(context: Context, appUrl: String) {
        val uri = Uri.parse(appUrl)
        val scheme = uri.scheme?.lowercase() ?: ""
        val targetPackage = APP_PACKAGES[scheme]

        // 1. Try launching generic system intent for this scheme
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {}

        // 2. Try explicitly setting known target package name
        if (!targetPackage.isNullOrBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(targetPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        // 3. Fallback: App not found or not installed on this device
        val appName = when {
            scheme.startsWith("upnote") -> "UpNote"
            scheme.startsWith("obsidian") -> "Obsidian"
            scheme.startsWith("notion") -> "Notion"
            scheme.startsWith("onenote") -> "OneNote"
            scheme.startsWith("marginnote") -> "MarginNote"
            scheme.startsWith("craft") -> "Craft"
            else -> scheme.replaceFirstChar { it.uppercase() }
        }

        copyToClipboard(context, "$appName Link", appUrl)

        if (!targetPackage.isNullOrBlank()) {
            Toast.makeText(
                context,
                "$appName app is not installed. Link copied to clipboard. Opening Play Store...",
                Toast.LENGTH_LONG
            ).show()

            try {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$targetPackage")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(marketIntent)
            } catch (_: Exception) {
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$targetPackage")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Could not open Play Store. Link copied to clipboard.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "$appName is not installed. Link copied to clipboard.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openUpNoteLink(context: Context, upnoteUrl: String) {
        openNativeAppScheme(context, upnoteUrl)
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
        } catch (_: Exception) {}
    }
}
