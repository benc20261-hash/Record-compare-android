package com.example.data.importer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.model.PictureItem
import com.example.model.ProductItem
import com.example.model.ReviewItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ImportedReviewDraft(
    val id: String = "draft_" + UUID.randomUUID().toString().take(12),
    var title: String,
    var bodyHtml: String,
    val pictures: MutableList<PictureItem>,
    val sourcePath: String,
    val sourceFolderName: String,
    var assignedProductId: String? = null,
    var rating: Int = 5,
    var authorName: String = "UpNote Reviewer",
    val wordCount: Int,
    val hasImages: Boolean
) {
    fun toReviewItem(
        availableProducts: List<ProductItem>,
        nextReviewNumber: Int
    ): ReviewItem {
        val targetProduct = availableProducts.find { it.id == assignedProductId }
        val productIds = if (targetProduct != null) listOf(targetProduct.id) else emptyList()
        val productTitles = if (targetProduct != null) listOf(targetProduct.name) else emptyList()

        return ReviewItem(
            id = "rev_" + UUID.randomUUID().toString().replace("-", "").take(17),
            reviewNumber = nextReviewNumber,
            title = title.ifBlank { "Imported Review #$nextReviewNumber" },
            body = bodyHtml,
            authorName = authorName.ifBlank { "UpNote Reviewer" },
            rating = rating.coerceIn(1, 5),
            productIds = productIds,
            productTitles = productTitles,
            pictures = pictures,
            createdTime = System.currentTimeMillis().toString()
        )
    }
}

object UpNoteHtmlImportManager {

    /**
     * Parses an imported Folder Tree (via SAF tree Uri).
     * Handles nested review folders:
     * - Parent Folder
     *   - Review Folder 1 (contains review.html, images/ subfolder)
     *   - Review Folder 2 (contains review.html, attachments/ subfolder)
     * Child subfolders like "images/" or "attachments/" inside a review folder are treated as
     * assets for that review, NOT as separate reviews.
     */
    suspend fun parseFromFolderTree(
        context: Context,
        treeUri: Uri,
        availableProducts: List<ProductItem>
    ): List<ImportedReviewDraft> = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "upnote_staging_${System.currentTimeMillis()}")
        if (stagingDir.exists()) stagingDir.deleteRecursively()
        stagingDir.mkdirs()

        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext emptyList()

            // Copy entire folder tree to local staging directory for fast parsing & IO
            copyDocumentTree(context, rootDoc, stagingDir)

            // Scan and parse reviews
            scanDirectoryForReviews(context, stagingDir, availableProducts)
        } finally {
            try {
                stagingDir.deleteRecursively()
            } catch (_: Exception) {}
        }
    }

    /**
     * Parses an imported ZIP file containing one or more review folders or HTML notes.
     */
    suspend fun parseFromZip(
        context: Context,
        zipUri: Uri,
        availableProducts: List<ProductItem>
    ): List<ImportedReviewDraft> = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "upnote_zip_${System.currentTimeMillis()}")
        if (stagingDir.exists()) stagingDir.deleteRecursively()
        stagingDir.mkdirs()

        try {
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                unzipToDirectory(input, stagingDir)
            }

            scanDirectoryForReviews(context, stagingDir, availableProducts)
        } finally {
            try {
                stagingDir.deleteRecursively()
            } catch (_: Exception) {}
        }
    }

    /**
     * Parses a single .html or .htm file.
     */
    suspend fun parseFromSingleHtmlFile(
        context: Context,
        htmlUri: Uri,
        availableProducts: List<ProductItem>
    ): List<ImportedReviewDraft> = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "upnote_single_${System.currentTimeMillis()}")
        if (stagingDir.exists()) stagingDir.deleteRecursively()
        stagingDir.mkdirs()

        try {
            val targetFile = File(stagingDir, "note.html")
            context.contentResolver.openInputStream(htmlUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            scanDirectoryForReviews(context, stagingDir, availableProducts)
        } finally {
            try {
                stagingDir.deleteRecursively()
            } catch (_: Exception) {}
        }
    }

    /**
     * Handles text, HTML, and images received directly via Android Share Sheet (ACTION_SEND / ACTION_SEND_MULTIPLE).
     */
    suspend fun parseFromShareIntent(
        context: Context,
        subject: String?,
        textOrHtml: String?,
        streamUris: List<Uri>,
        availableProducts: List<ProductItem>
    ): ImportedReviewDraft = withContext(Dispatchers.IO) {
        val reviewImagesDir = getPermanentImagesDir(context)
        val pictures = mutableListOf<PictureItem>()

        // 1. Copy any attached images to permanent storage
        streamUris.forEachIndexed { index, uri ->
            try {
                val filename = "share_img_${System.currentTimeMillis()}_$index.jpg"
                val destFile = File(reviewImagesDir, filename)
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    destFile.outputStream().use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                if (destFile.exists() && destFile.length() > 0) {
                    pictures.add(
                        PictureItem(
                            id = UUID.randomUUID().toString(),
                            name = "Image ${index + 1}",
                            url = "file://${destFile.absolutePath}"
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // 2. Build or clean HTML
        val raw = textOrHtml?.trim() ?: ""
        val isHtml = raw.contains("<p>", ignoreCase = true) ||
                raw.contains("<div>", ignoreCase = true) ||
                raw.contains("<h1>", ignoreCase = true) ||
                raw.contains("<br", ignoreCase = true) ||
                raw.contains("<html>", ignoreCase = true)

        val cleanBody = if (isHtml) {
            cleanUpHtmlContent(raw, emptyMap())
        } else {
            // Convert plain text or basic lines to HTML paragraphs
            val paragraphs = raw.split("\n\n").filter { it.isNotBlank() }
            if (paragraphs.isEmpty()) {
                "<p>Imported note content.</p>"
            } else {
                paragraphs.joinToString("\n") { p ->
                    val withBreaks = p.replace("\n", "<br/>")
                    "<p>$withBreaks</p>"
                }
            }
        }

        // Append pictures at the bottom if not already in the HTML
        val finalBody = buildString {
            append(cleanBody)
            if (pictures.isNotEmpty() && !cleanBody.contains("<img", ignoreCase = true)) {
                append("\n<br/>\n")
                pictures.forEach { pic ->
                    val filename = File(pic.url.removePrefix("file://")).name
                    val webUrl = "https://local.review/review_images/$filename"
                    append("<p><img src=\"$webUrl\" alt=\"${pic.name}\" style=\"max-width:100%; border-radius:8px; margin:8px 0;\" /></p>\n")
                }
            }
        }

        val title = subject?.trim()?.ifBlank { null }
            ?: extractTitleFromHtml(finalBody)
            ?: "Shared Note from UpNote"

        val suggestedProduct = guessMatchingProduct(title, finalBody, availableProducts)

        ImportedReviewDraft(
            title = title,
            bodyHtml = finalBody,
            pictures = pictures,
            sourcePath = "Android Share Sheet",
            sourceFolderName = "UpNote Share",
            assignedProductId = suggestedProduct?.id,
            rating = 5,
            authorName = "UpNote User",
            wordCount = countWords(finalBody),
            hasImages = pictures.isNotEmpty()
        )
    }

    /**
     * Scans a directory for .html notes.
     * Treats each .html file as a review.
     * Relative subfolders like "images/" or "attachments/" inside the review's folder
     * are treated as asset containers for that review, not separate reviews.
     */
    private fun scanDirectoryForReviews(
        context: Context,
        rootDir: File,
        availableProducts: List<ProductItem>
    ): List<ImportedReviewDraft> {
        val reviewImagesDir = getPermanentImagesDir(context)
        val results = mutableListOf<ImportedReviewDraft>()

        // Find all .html / .htm files (ignoring __MACOSX and hidden files)
        val htmlFiles = rootDir.walkTopDown()
            .filter { file ->
                file.isFile &&
                        (file.extension.equals("html", ignoreCase = true) || file.extension.equals("htm", ignoreCase = true)) &&
                        !file.name.startsWith(".") &&
                        !file.absolutePath.contains("__MACOSX")
            }
            .toList()

        for (htmlFile in htmlFiles) {
            try {
                val reviewFolder = htmlFile.parentFile ?: rootDir
                val rawHtml = htmlFile.readText(Charsets.UTF_8)
                if (rawHtml.isBlank()) continue

                // 1. Extract image references from HTML: <img ... src="relative_path" ...>
                val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
                val matches = imgRegex.findAll(rawHtml).toList()

                val imageReplacementMap = mutableMapOf<String, String>()
                val pictures = mutableListOf<PictureItem>()

                for (m in matches) {
                    val originalSrc = m.groupValues[1]
                    if (originalSrc.isBlank() || originalSrc.startsWith("http://") || originalSrc.startsWith("https://") || originalSrc.startsWith("data:")) {
                        continue
                    }

                    // Decode URI encoding (e.g. images/my%20pic.png -> images/my pic.png)
                    val decodedSrc = try {
                        URLDecoder.decode(originalSrc, "UTF-8")
                    } catch (_: Exception) {
                        originalSrc
                    }

                    // Locate local image file
                    val imageFile = locateImageFile(reviewFolder, rootDir, decodedSrc)
                    if (imageFile != null && imageFile.exists() && imageFile.isFile) {
                        val ext = imageFile.extension.ifBlank { "jpg" }
                        val persistentName = "upnote_img_${UUID.randomUUID().toString().take(12)}.$ext"
                        val permanentFile = File(reviewImagesDir, persistentName)
                        imageFile.copyTo(permanentFile, overwrite = true)

                        val localFileUri = "file://${permanentFile.absolutePath}"
                        val virtualWebUri = "https://local.review/review_images/$persistentName"
                        
                        // Map both virtual https URL and local file URI so WebView and Coil both work
                        imageReplacementMap[originalSrc] = virtualWebUri
                        if (decodedSrc != originalSrc) {
                            imageReplacementMap[decodedSrc] = virtualWebUri
                        }

                        pictures.add(
                            PictureItem(
                                id = UUID.randomUUID().toString(),
                                name = imageFile.nameWithoutExtension.ifBlank { "Note Picture" },
                                url = localFileUri
                            )
                        )
                    }
                }

                // 2. Also check if there are loose images in the review folder's asset subdirectories
                // that were not referenced directly in HTML, or in an images/ folder
                val assetFolders = listOf("images", "attachments", "assets", "_files", "pics", "media", "img")
                reviewFolder.listFiles()?.forEach { child ->
                    if (child.isDirectory && child.name.lowercase() in assetFolders) {
                        child.listFiles()?.filter { f ->
                            f.isFile && f.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif")
                        }?.forEach { img ->
                            val alreadyIncluded = pictures.any { it.name == img.nameWithoutExtension }
                            if (!alreadyIncluded) {
                                val ext = img.extension.ifBlank { "jpg" }
                                val persistentName = "upnote_img_${UUID.randomUUID().toString().take(12)}.$ext"
                                val permanentFile = File(reviewImagesDir, persistentName)
                                img.copyTo(permanentFile, overwrite = true)
                                pictures.add(
                                    PictureItem(
                                        id = UUID.randomUUID().toString(),
                                        name = img.nameWithoutExtension,
                                        url = "file://${permanentFile.absolutePath}"
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Extract Title
                val title = extractTitleFromHtml(rawHtml)
                    ?: htmlFile.nameWithoutExtension.takeIf { it != "note" && it != "index" }
                    ?: reviewFolder.name.takeIf { it != rootDir.name }
                    ?: "Imported Note"

                // 4. Clean up HTML and replace image sources, stripping redundant leading title
                var cleanBody = cleanUpHtmlContent(rawHtml, imageReplacementMap, title)

                // If some detected pictures are not in cleanBody, append them so they are visible in the HTML document
                val unlinkedPics = pictures.filter { pic -> 
                    val filename = File(pic.url.removePrefix("file://")).name
                    !cleanBody.contains(pic.url) && !cleanBody.contains(filename) 
                }
                if (unlinkedPics.isNotEmpty()) {
                    cleanBody = buildString {
                        append(cleanBody)
                        append("\n<div class=\"imported-images-section\" style=\"margin-top:24px; padding-top:16px; border-top:1px solid #e2e8f0;\">\n")
                        append("<h3 style=\"font-size:16px; font-weight:bold; color:#0f172a; margin-bottom:12px;\">Attached Photos & Screenshots</h3>\n")
                        unlinkedPics.forEach { pic ->
                            val filename = File(pic.url.removePrefix("file://")).name
                            val webUrl = "https://local.review/review_images/$filename"
                            append("<p style=\"text-align:center; margin:12px 0;\"><img src=\"$webUrl\" alt=\"${pic.name}\" style=\"max-width:100%; border-radius:10px; box-shadow:0 1px 3px rgba(0,0,0,0.1);\" /></p>\n")
                        }
                        append("</div>\n")
                    }
                }

                val folderLabel = if (reviewFolder != rootDir) reviewFolder.name else htmlFile.name

                // 5. Guess product match
                val suggestedProduct = guessMatchingProduct(title, cleanBody, availableProducts)

                results.add(
                    ImportedReviewDraft(
                        title = title,
                        bodyHtml = cleanBody,
                        pictures = pictures,
                        sourcePath = htmlFile.name,
                        sourceFolderName = folderLabel,
                        assignedProductId = suggestedProduct?.id,
                        rating = 5,
                        authorName = "UpNote Import",
                        wordCount = countWords(cleanBody),
                        hasImages = pictures.isNotEmpty()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return results
    }

    /**
     * Resolves an image path starting from the review's folder.
     * Looks for exact relative path, then searches subfolders (images/, attachments/, etc.)
     */
    private fun locateImageFile(reviewFolder: File, rootDir: File, relativePath: String): File? {
        // Direct relative file
        val direct = File(reviewFolder, relativePath)
        if (direct.exists() && direct.isFile) return direct

        // Normalize slashes
        val normalized = relativePath.replace('\\', '/')
        val directNorm = File(reviewFolder, normalized)
        if (directNorm.exists() && directNorm.isFile) return directNorm

        // Direct relative in rootDir
        val directRoot = File(rootDir, normalized)
        if (directRoot.exists() && directRoot.isFile) return directRoot

        // Search by filename inside reviewFolder
        val targetName = File(normalized).name
        val matched = reviewFolder.walkTopDown()
            .maxDepth(3)
            .filter { it.isFile && it.name.equals(targetName, ignoreCase = true) }
            .firstOrNull()

        if (matched != null) return matched

        // Search by filename inside rootDir (e.g. shared images/ folder)
        return rootDir.walkTopDown()
            .maxDepth(4)
            .filter { it.isFile && it.name.equals(targetName, ignoreCase = true) }
            .firstOrNull()
    }

    /**
     * Cleans up outer HTML boilerplate (doctype, html, head, scripts)
     * while preserving rich review formatting: headers, paragraphs, bold, tables, lists, checklists, and images.
     */
    private fun cleanUpHtmlContent(
        html: String,
        imageReplacements: Map<String, String>,
        extractedTitle: String? = null
    ): String {
        var content = html.trim()

        // Unescape HTML entities if body was escaped (e.g. &lt;h1&gt;)
        if ((content.contains("&lt;h") || content.contains("&lt;p") || content.contains("&lt;div") || content.contains("&lt;table")) &&
            !content.contains("<h") && !content.contains("<p")
        ) {
            content = content
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
        }

        // Replace image src links with virtual web URIs / permanent local URIs
        imageReplacements.forEach { (oldSrc, newUri) ->
            content = content.replace("src=\"$oldSrc\"", "src=\"$newUri\"")
            content = content.replace("src='$oldSrc'", "src='$newUri'")
            content = content.replace("src=$oldSrc", "src=\"$newUri\"")
        }

        // Remove <script>...</script>
        content = content.replace(Regex("""<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>""", RegexOption.IGNORE_CASE), "")

        // Extract body if present
        val bodyMatcher = Regex("""<body[^>]*>([\s\S]*?)<\/body>""", RegexOption.IGNORE_CASE).find(content)
        if (bodyMatcher != null) {
            content = bodyMatcher.groupValues[1]
        } else {
            // Remove <head>...</head> if body tag was missing
            content = content.replace(Regex("""<head[^>]*>[\s\S]*?<\/head>""", RegexOption.IGNORE_CASE), "")
            content = content.replace(Regex("""<!DOCTYPE[^>]*>""", RegexOption.IGNORE_CASE), "")
            content = content.replace(Regex("""<\/?html[^>]*>""", RegexOption.IGNORE_CASE), "")
        }

        // Clean up outer styles if any remained in body
        content = content.replace(Regex("""<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>""", RegexOption.IGNORE_CASE), "")

        // If the body begins with an <h1> or <h2> matching the extracted review title, strip it to prevent title duplication
        if (!extractedTitle.isNullOrBlank()) {
            val normalizedTitle = extractedTitle.trim().lowercase()
            val leadingHeadingRegex = Regex("""^\s*<h[12][^>]*>([\s\S]*?)<\/h[12]>\s*""", RegexOption.IGNORE_CASE)
            val headingMatch = leadingHeadingRegex.find(content)
            if (headingMatch != null) {
                val headingText = headingMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim().lowercase()
                if (headingText == normalizedTitle || headingText.contains(normalizedTitle) || normalizedTitle.contains(headingText)) {
                    content = content.substring(headingMatch.range.last + 1).trim()
                }
            }
        }

        // Clean up excessive whitespace
        return content.trim()
    }

    /**
     * Extracts title from <title> or first <h1> tag.
     */
    private fun extractTitleFromHtml(html: String): String? {
        val titleMatch = Regex("""<title[^>]*>([\s\S]*?)<\/title>""", RegexOption.IGNORE_CASE).find(html)
        val extractedTitle = titleMatch?.groupValues?.get(1)?.trim()
        if (!extractedTitle.isNullOrBlank() &&
            !extractedTitle.equals("UpNote", ignoreCase = true) &&
            !extractedTitle.equals("Untitled", ignoreCase = true)
        ) {
            return unescapeHtml(extractedTitle)
        }

        val h1Match = Regex("""<h1[^>]*>([\s\S]*?)<\/h1>""", RegexOption.IGNORE_CASE).find(html)
        val extractedH1 = h1Match?.groupValues?.get(1)?.replace(Regex("<[^>]*>"), "")?.trim()
        if (!extractedH1.isNullOrBlank()) {
            return unescapeHtml(extractedH1)
        }

        return null
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }

    private fun countWords(html: String): Int {
        val stripped = html.replace(Regex("<[^>]*>"), " ").trim()
        if (stripped.isBlank()) return 0
        return stripped.split(Regex("""\s+""")).size
    }

    /**
     * Automatically matches review title/content to catalog products.
     */
    private fun guessMatchingProduct(
        title: String,
        bodyHtml: String,
        products: List<ProductItem>
    ): ProductItem? {
        if (products.isEmpty()) return null
        val lowerTitle = title.lowercase()
        val lowerBody = bodyHtml.take(500).lowercase()

        // 1. Exact match in title
        for (prod in products) {
            if (lowerTitle.contains(prod.name.lowercase())) {
                return prod
            }
        }

        // 2. Brand match in title
        for (prod in products) {
            if (prod.brand.isNotBlank() && lowerTitle.contains(prod.brand.lowercase())) {
                return prod
            }
        }

        // 3. Match in first 500 chars of body
        for (prod in products) {
            if (lowerBody.contains(prod.name.lowercase())) {
                return prod
            }
        }

        return null
    }

    private fun getPermanentImagesDir(context: Context): File {
        val dir = File(context.filesDir, "review_images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun copyDocumentTree(context: Context, sourceDir: DocumentFile, targetDir: File) {
        targetDir.mkdirs()
        for (file in sourceDir.listFiles()) {
            val name = file.name ?: continue
            if (file.isDirectory) {
                val subDir = File(targetDir, name)
                copyDocumentTree(context, file, subDir)
            } else {
                val destFile = File(targetDir, name)
                try {
                    context.contentResolver.openInputStream(file.uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun unzipToDirectory(zipStream: InputStream, targetDir: File) {
        ZipInputStream(zipStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                // Prevent Zip Slip vulnerability
                if (!file.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    entry = zis.nextEntry
                    continue
                }

                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
