package com.example.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import com.example.data.local.AppDatabase
import com.example.data.local.ProductEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.SpecificationEntity
import com.example.data.local.UserEntity
import com.example.model.PictureItem
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.SpecificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupMetadata(
    val appName: String = "ProductCompare Hub",
    val exportDate: String,
    val productCount: Int,
    val specCount: Int,
    val reviewCount: Int,
    val userCount: Int,
    val pictureCount: Int,
    val version: String = "3.0.0-localapp"
)

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val restoredProducts: Int = 0,
    val restoredSpecs: Int = 0,
    val restoredReviews: Int = 0,
    val restoredUsers: Int = 0,
    val restoredMedia: Int = 0
)

data class FileSaveResult(
    val success: Boolean,
    val location: String?,
    val message: String
)

object BackupArchiveManager {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Builds standard database.json matching Productc webapp and seed catalog format.
     */
    fun buildDatabaseJson(
        products: List<ProductItem>,
        specifications: List<SpecificationItem>,
        reviews: List<ReviewItem>,
        users: List<UserEntity>
    ): String {
        val root = JSONObject()
        val nowIso = isoDateFormat.format(Date())

        root.put("sourceUrl", "local")
        root.put("shareId", "")
        root.put("baseId", "")
        root.put("lastSynced", nowIso)

        val prodsArr = JSONArray()
        products.forEach { p ->
            val po = JSONObject()
            po.put("id", p.id)
            po.put("name", p.name)
            po.put("brand", p.brand)
            po.put("tags", JSONArray(p.tags))
            po.put("overview", p.overview)

            val picsArr = JSONArray()
            p.pictures.forEach { pic ->
                val pobj = JSONObject()
                pobj.put("id", pic.id)
                pobj.put("name", pic.name)
                pobj.put("url", pic.url)
                picsArr.put(pobj)
            }
            po.put("pictures", picsArr)

            // Link matching spec IDs and review IDs
            val matchingSpecIds = specifications.filter { it.productId == p.id }.map { it.id }
            val matchingReviewIds = reviews.filter { it.productIds.contains(p.id) }.map { it.id }
            po.put("specificationIds", JSONArray(matchingSpecIds))
            po.put("reviewIds", JSONArray(matchingReviewIds))

            po.put("createdTime", p.createdTime)
            po.put("updatedAt", nowIso)
            prodsArr.put(po)
        }
        root.put("products", prodsArr)

        val specsArr = JSONArray()
        specifications.forEach { s ->
            val so = JSONObject()
            so.put("id", s.id)
            so.put("productId", s.productId)
            so.put("productTitle", s.productTitle)
            so.put("specName", s.specName)
            so.put("details", s.details)
            so.put("unit", s.unit ?: "")
            so.put("order", s.orderIndex)

            val picsArr = JSONArray()
            s.pictures.forEach { pic ->
                val pobj = JSONObject()
                pobj.put("id", pic.id)
                pobj.put("name", pic.name)
                pobj.put("url", pic.url)
                picsArr.put(pobj)
            }
            so.put("pictures", picsArr)
            so.put("updatedAt", nowIso)
            specsArr.put(so)
        }
        root.put("specifications", specsArr)

        val revsArr = JSONArray()
        reviews.forEach { r ->
            val ro = JSONObject()
            ro.put("id", r.id)
            ro.put("reviewNumber", r.reviewNumber)
            ro.put("title", r.title)
            ro.put("body", r.body)
            ro.put("authorName", r.authorName)
            ro.put("rating", r.rating)
            ro.put("productIds", JSONArray(r.productIds))
            ro.put("productTitles", JSONArray(r.productTitles))

            val picsArr = JSONArray()
            r.pictures.forEach { pic ->
                val pobj = JSONObject()
                pobj.put("id", pic.id)
                pobj.put("name", pic.name)
                pobj.put("url", pic.url)
                picsArr.put(pobj)
            }
            ro.put("pictures", picsArr)
            ro.put("createdTime", r.createdTime)
            ro.put("updatedAt", nowIso)
            revsArr.put(ro)
        }
        root.put("reviews", revsArr)

        val usersArr = JSONArray()
        users.forEach { u ->
            val uo = JSONObject()
            uo.put("id", u.id)
            uo.put("name", u.name)
            uo.put("email", u.email)
            uo.put("role", u.role)
            usersArr.put(uo)
        }
        root.put("users", usersArr)

        return root.toString(2)
    }

    /**
     * Creates a full ZIP archive matching Productc webapp layout containing:
     * 1. manifest.json (Productc standard version 3.0.0-localapp)
     * 2. database.json
     * 3. images/ (All pictures referenced in DB named by attachment ID and original name)
     * 4. pictures/ & review_images/ (Android internal compatibility)
     * 5. reviews/ (formatted markdown files for each review note)
     * 6. README.txt
     */
    suspend fun createFullZipBackup(
        context: Context,
        products: List<ProductItem>,
        specifications: List<SpecificationItem>,
        reviews: List<ReviewItem>,
        users: List<UserEntity>,
        onProgress: (step: String, current: Int, total: Int) -> Unit = { _, _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val timestamp = dateFormat.format(Date())
        val backupDir = File(context.cacheDir, "backup_staging_$timestamp")
        if (backupDir.exists()) backupDir.deleteRecursively()
        backupDir.mkdirs()

        // Standard webapp folder
        val imagesDir = File(backupDir, "images")
        imagesDir.mkdirs()

        // Android backward-compatible folders
        val picturesDir = File(backupDir, "pictures")
        picturesDir.mkdirs()
        val reviewsDir = File(backupDir, "reviews")
        reviewsDir.mkdirs()
        val reviewImagesDir = File(backupDir, "review_images")
        reviewImagesDir.mkdirs()

        // 1. Build database.json
        onProgress("Building database.json", 1, 4)
        val databaseJson = buildDatabaseJson(products, specifications, reviews, users)
        File(backupDir, "database.json").writeText(databaseJson)

        // 2. Export individual formatted review Markdown files
        onProgress("Exporting review notes", 2, 4)
        reviews.forEach { rev ->
            val cleanTitle = rev.title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
            val fileName = "review_${rev.reviewNumber}_${cleanTitle.ifBlank { "note" }}.md"
            val file = File(reviewsDir, fileName)
            val mdContent = buildString {
                appendLine("# Review #${rev.reviewNumber}: ${rev.title.ifBlank { "Product Review" }}")
                appendLine()
                appendLine("- **Author:** ${rev.authorName}")
                appendLine("- **Rating:** ${rev.rating} / 5")
                if (rev.productTitles.isNotEmpty()) {
                    appendLine("- **Linked Products:** ${rev.productTitles.joinToString(", ")}")
                }
                appendLine("- **Review ID:** `${rev.id}`")
                appendLine()
                appendLine("---")
                appendLine()
                appendLine("## Notes & Review Content")
                appendLine()
                appendLine(rev.body)
            }
            file.writeText(mdContent)
        }

        // 3. Collect and package unique pictures (both remote URLs and local files)
        onProgress("Packaging pictures", 3, 4)
        val allPictures = mutableMapOf<String, PictureItem>()
        products.forEach { p -> p.pictures.forEach { allPictures[it.id] = it } }
        specifications.forEach { s -> s.pictures.forEach { allPictures[it.id] = it } }
        reviews.forEach { r -> r.pictures.forEach { allPictures[it.id] = it } }

        var packagedPicturesCount = 0

        // Helper to copy an image into all three backup directories (images, pictures, review_images)
        fun copyImageToBackup(sourceFile: File, preferredIdName: String? = null) {
            try {
                // 1. Save to Android legacy folders
                val destReview = File(reviewImagesDir, sourceFile.name)
                sourceFile.copyTo(destReview, overwrite = true)
                val destPic = File(picturesDir, sourceFile.name)
                sourceFile.copyTo(destPic, overwrite = true)

                // 2. Save to standard webapp images/ folder by filename
                val destImg = File(imagesDir, sourceFile.name)
                sourceFile.copyTo(destImg, overwrite = true)

                // 3. Save by attachment ID name (e.g. att_123.jpg) for direct webapp IndexedDB matching
                if (!preferredIdName.isNullOrBlank()) {
                    val ext = sourceFile.extension.ifBlank { "jpg" }
                    val idFile = File(imagesDir, "$preferredIdName.$ext")
                    sourceFile.copyTo(idFile, overwrite = true)
                }

                packagedPicturesCount++
            } catch (_: Exception) {}
        }

        // Bundle ALL existing local review images in context.filesDir/review_images
        val localReviewImagesFolder = File(context.filesDir, "review_images")
        if (localReviewImagesFolder.exists() && localReviewImagesFolder.isDirectory) {
            localReviewImagesFolder.listFiles()?.forEach { f ->
                if (f.isFile) copyImageToBackup(f)
            }
        }

        // Bundle ALL existing local pictures in context.filesDir/pictures
        val localPicturesFolder = File(context.filesDir, "pictures")
        if (localPicturesFolder.exists() && localPicturesFolder.isDirectory) {
            localPicturesFolder.listFiles()?.forEach { f ->
                if (f.isFile) copyImageToBackup(f)
            }
        }

        // Bundle all images embedded in review HTML bodies
        reviews.forEach { rev ->
            val srcRegex = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            srcRegex.findAll(rev.body).forEach { m ->
                val srcUrl = m.groupValues[1]
                if (srcUrl.contains("local.review") || srcUrl.contains("review_images") || srcUrl.contains("pictures") || srcUrl.contains("images") || srcUrl.startsWith("file://") || srcUrl.startsWith("/")) {
                    val rawFilename = srcUrl.substringAfterLast("/").substringBefore("?")
                    val decoded = try { URLDecoder.decode(rawFilename, "UTF-8") } catch (_: Exception) { rawFilename }

                    val c1 = File(File(context.filesDir, "review_images"), decoded)
                    val c2 = File(File(context.filesDir, "review_images"), rawFilename)
                    val c3 = File(File(context.filesDir, "pictures"), decoded)
                    val c4 = File(File(context.filesDir, "pictures"), rawFilename)
                    val found = listOf(c1, c2, c3, c4).firstOrNull { it.exists() && it.isFile }
                    if (found != null) {
                        copyImageToBackup(found)
                    }
                }
            }
        }

        // Package picture items referenced in entity arrays
        allPictures.values.forEach { pic ->
            if (pic.url.isNotBlank()) {
                if (pic.url.startsWith("http://") || pic.url.startsWith("https://")) {
                    if (pic.url.contains("local.review")) {
                        // Virtual URL: extract local file
                        val filename = pic.url.substringAfterLast("/")
                        val localF1 = File(context.filesDir, "review_images/$filename")
                        val localF2 = File(context.filesDir, "pictures/$filename")
                        val foundF = if (localF1.exists() && localF1.isFile) localF1 else if (localF2.exists() && localF2.isFile) localF2 else null
                        if (foundF != null) {
                            copyImageToBackup(foundF, pic.id)
                        }
                    } else {
                        // Remote web image: download
                        try {
                            val request = Request.Builder().url(pic.url).build()
                            val response = httpClient.newCall(request).execute()
                            if (response.isSuccessful) {
                                val body = response.body
                                if (body != null) {
                                    val cleanName = pic.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                                    val ext = cleanName.substringAfterLast(".", "jpg")
                                    val picFile = File(picturesDir, "${pic.id}_$cleanName")
                                    picFile.outputStream().use { out ->
                                        body.byteStream().copyTo(out)
                                    }
                                    copyImageToBackup(picFile, pic.id)
                                }
                            }
                        } catch (_: Exception) {
                            // Continue gracefully
                        }
                    }
                } else if (pic.url.startsWith("content://")) {
                    try {
                        val cleanName = pic.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        val ext = if (cleanName.contains(".")) cleanName.substringAfterLast(".") else "jpg"
                        val destName = "${pic.id}_$cleanName"
                        val destFile = File(picturesDir, destName)
                        context.contentResolver.openInputStream(Uri.parse(pic.url))?.use { input ->
                            destFile.outputStream().use { out -> input.copyTo(out) }
                        }
                        if (destFile.exists() && destFile.length() > 0) {
                            copyImageToBackup(destFile, pic.id)
                        }
                    } catch (_: Exception) {}
                } else if (pic.url.startsWith("file://") || pic.url.startsWith("/")) {
                    val localPath = pic.url.removePrefix("file://")
                    val localF = File(localPath)
                    if (localF.exists() && localF.isFile) {
                        copyImageToBackup(localF, pic.id)
                    } else {
                        val fname = localPath.substringAfterLast("/")
                        val c1 = File(File(context.filesDir, "pictures"), fname)
                        val c2 = File(File(context.filesDir, "review_images"), fname)
                        val found = if (c1.exists()) c1 else if (c2.exists()) c2 else null
                        if (found != null && found.isFile) {
                            copyImageToBackup(found, pic.id)
                        }
                    }
                }
            }
        }

        // 4. Write manifest.json matching webapp standard
        val nowIso = isoDateFormat.format(Date())
        val manifestObj = JSONObject().apply {
            put("version", "3.0.0-localapp")
            put("appName", "ProductCompare Hub")
            put("exportedAt", nowIso)
            put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
            put("productCount", products.size)
            put("specificationCount", specifications.size)
            put("reviewCount", reviews.size)
            put("userCount", users.size)
            put("mediaFileCount", packagedPicturesCount)
            put("pictureCount", packagedPicturesCount)
        }
        File(backupDir, "manifest.json").writeText(manifestObj.toString(2))

        // 5. Write README.txt
        val readmeContent = """
ProductCompare Hub - Full Offline Backup Archive
===================================================
Exported: $nowIso
Products: ${products.size}
Specifications: ${specifications.size}
Reviews: ${reviews.size}
Media Files: $packagedPicturesCount

This archive contains your complete standalone catalog, specifications,
reviews, and all photos/attachments. Compatible with both the ProductCompare
Android App and the Productc Webapp (https://github.com/2017xlee-svg/Productc.git).

To restore this backup:
1. Open ProductCompare Hub (Webapp or Android App).
2. Open the Data & Backup view.
3. Select "Restore Archive (.ZIP)" and choose this file.
All products, specs, reviews, and high-resolution photos will be restored!
        """.trimIndent()
        File(backupDir, "README.txt").writeText(readmeContent)

        // 6. Compress backup folder into ZIP archive
        onProgress("Compressing into ZIP archive", 4, 4)
        val zipFile = File(context.cacheDir, "ProductCompare_Backup_$timestamp.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            zipDirectory(backupDir, backupDir, zos)
        }

        // Clean up temporary uncompressed staging directory
        backupDir.deleteRecursively()
        zipFile
    }

    /**
     * Saves a copy of the zip file to the public Downloads folder using MediaStore (Android 10+)
     * or standard external storage, so the file is visible in any File Manager app.
     */
    fun saveZipToPublicDownloads(context: Context, zipFile: File): FileSaveResult {
        val resolver = context.contentResolver
        val filename = zipFile.name
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ProductCompare")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return FileSaveResult(false, null, "Failed to create file in Downloads folder")

                resolver.openOutputStream(uri)?.use { out ->
                    zipFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                } ?: return FileSaveResult(false, null, "Failed to write into Downloads folder")

                return FileSaveResult(
                    true,
                    "Downloads/ProductCompare/$filename",
                    "Saved to Downloads/ProductCompare/$filename"
                )
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val pcDir = File(downloadsDir, "ProductCompare")
                if (!pcDir.exists()) pcDir.mkdirs()
                val targetFile = File(pcDir, filename)
                zipFile.copyTo(targetFile, overwrite = true)
                return FileSaveResult(
                    true,
                    targetFile.absolutePath,
                    "Saved to ${targetFile.absolutePath}"
                )
            }
        } catch (e: Exception) {
            return FileSaveResult(false, null, "Save to Downloads failed: ${e.localizedMessage}")
        }
    }

    /**
     * Writes the backup ZIP directly to a user-selected target URI (from CreateDocument).
     */
    fun writeZipToUri(context: Context, zipFile: File, destinationUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                zipFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Writes database JSON directly to a user-selected target URI.
     */
    fun writeJsonToUri(context: Context, jsonString: String, destinationUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun zipDirectory(rootDir: File, sourceDir: File, zos: ZipOutputStream) {
        val files = sourceDir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                zipDirectory(rootDir, file, zos)
            } else {
                val relativePath = file.relativeTo(rootDir).path.replace("\\", "/")
                val entry = ZipEntry(relativePath)
                zos.putNextEntry(entry)
                file.inputStream().use { input ->
                    input.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }

    /**
     * Restores database from a given URI (supporting both webapp .zip archives and raw .json backups).
     */
    suspend fun restoreFromZipUri(
        context: Context,
        uri: Uri,
        database: AppDatabase
    ): RestoreResult = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val tempFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}")
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext RestoreResult(false, "Could not open selected backup file. Please verify file access.")

            // 1. First attempt: Treat as ZIP archive
            val zipResult = tryRestoreFromZip(context, tempFile, database)
            if (zipResult.success) {
                return@withContext zipResult
            }

            // 2. Second attempt: Treat as raw database.json
            val jsonResult = tryRestoreFromRawJson(context, tempFile, database)
            if (jsonResult.success) {
                return@withContext jsonResult
            }

            RestoreResult(false, "Could not restore: File is neither a valid ProductCompare ZIP archive nor a valid database.json file. (${zipResult.message})")
        } catch (e: Exception) {
            RestoreResult(false, "Restore failed: ${e.localizedMessage ?: "Unknown error"}")
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    /**
     * Extracts and restores a ZIP archive (handling both Productc webapp and Android layouts).
     */
    suspend fun tryRestoreFromZip(
        context: Context,
        zipFile: File,
        database: AppDatabase
    ): RestoreResult = withContext(Dispatchers.IO) {
        var databaseJsonString: String? = null
        val destReviewImagesDir = File(context.filesDir, "review_images")
        destReviewImagesDir.mkdirs()
        val destPicturesDir = File(context.filesDir, "pictures")
        destPicturesDir.mkdirs()

        val validImageExtensions = listOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp", "ico", "avif")
        var extractedMediaCount = 0

        try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val rawName = entry.name
                    val nameLower = rawName.lowercase()
                    val filename = rawName.substringAfterLast("/")

                    // Check if entry is database.json (or fallback JSON)
                    if (nameLower.endsWith("database.json")) {
                        val buffer = ByteArrayOutputStream()
                        zis.copyTo(buffer)
                        databaseJsonString = buffer.toString("UTF-8")
                    } else if (databaseJsonString == null && nameLower.endsWith(".json") && !nameLower.contains("manifest") && !nameLower.contains("package")) {
                        val buffer = ByteArrayOutputStream()
                        zis.copyTo(buffer)
                        databaseJsonString = buffer.toString("UTF-8")
                    } else if (!entry.isDirectory && filename.isNotBlank()) {
                        val ext = filename.substringAfterLast(".", "").lowercase()
                        val isImageEntry = rawName.startsWith("images/") ||
                                rawName.contains("/images/") ||
                                rawName.startsWith("pictures/") ||
                                rawName.startsWith("review_images/") ||
                                ext in validImageExtensions

                        if (isImageEntry) {
                            val targetFile = File(destReviewImagesDir, filename)
                            targetFile.outputStream().use { out ->
                                zis.copyTo(out)
                            }
                            try {
                                targetFile.copyTo(File(destPicturesDir, filename), overwrite = true)
                            } catch (_: Exception) {}
                            extractedMediaCount++
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (databaseJsonString.isNullOrBlank()) {
                return@withContext RestoreResult(false, "ZIP does not contain database.json")
            }

            return@withContext parseAndInsertDatabaseJson(context, databaseJsonString!!, database, extractedMediaCount)
        } catch (e: Exception) {
            RestoreResult(false, "ZIP read error: ${e.localizedMessage}")
        }
    }

    private suspend fun tryRestoreFromRawJson(
        context: Context,
        jsonFile: File,
        database: AppDatabase
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val content = jsonFile.readText(Charsets.UTF_8).trim()
            if (content.startsWith("{") && (content.contains("\"products\"") || content.contains("\"specifications\""))) {
                return@withContext parseAndInsertDatabaseJson(context, content, database, mediaCount = 0)
            }
            RestoreResult(false, "File does not match ProductCompare database schema")
        } catch (e: Exception) {
            RestoreResult(false, "JSON read error: ${e.localizedMessage}")
        }
    }

    /**
     * Robust picture array remapper that cross-matches webapp attachment IDs,
     * decoded URLs, and local extracted files from images/.
     */
    private fun remapPictureArray(picsArr: JSONArray?, context: Context): String {
        if (picsArr == null) return "[]"
        val newArr = JSONArray()
        val reviewImagesDir = File(context.filesDir, "review_images")
        val picturesDir = File(context.filesDir, "pictures")

        val allLocalFiles = ((reviewImagesDir.listFiles() ?: emptyArray()) + (picturesDir.listFiles() ?: emptyArray())).filter { it.isFile }

        for (j in 0 until picsArr.length()) {
            val picObj = picsArr.getJSONObject(j)
            val originalUrl = picObj.optString("url", "").trim()
            val picId = picObj.optString("id", "").trim()
            val picName = picObj.optString("name", "").trim()
            val dataUrl = picObj.optString("dataUrl", "").trim()
            var mappedUrl = originalUrl

            var existingFile: File? = null

            // 1. Primary Match: Match by Attachment ID (Productc webapp stores images as images/${att.id}.ext)
            if (picId.isNotBlank()) {
                existingFile = allLocalFiles.firstOrNull { f ->
                    val base = f.name.substringBeforeLast(".")
                    base.equals(picId, ignoreCase = true) || f.name.equals(picId, ignoreCase = true)
                }
                if (existingFile == null) {
                    existingFile = allLocalFiles.firstOrNull { f ->
                        f.name.startsWith("${picId}_", ignoreCase = true) || f.name.startsWith(picId, ignoreCase = true)
                    }
                }
                if (existingFile == null && picId.length >= 4) {
                    existingFile = allLocalFiles.firstOrNull { f ->
                        f.name.contains(picId, ignoreCase = true)
                    }
                }
            }

            // 2. Secondary Match: Match by URL filename or decoded filename
            if (existingFile == null && originalUrl.isNotBlank()) {
                val rawFilename = originalUrl.substringAfterLast("/").substringBefore("?")
                val decodedName = try { URLDecoder.decode(rawFilename, "UTF-8") } catch (_: Exception) { rawFilename }

                val c1 = File(reviewImagesDir, decodedName)
                val c2 = File(reviewImagesDir, rawFilename)
                val c3 = File(picturesDir, decodedName)
                val c4 = File(picturesDir, rawFilename)
                existingFile = listOf(c1, c2, c3, c4).firstOrNull { it.exists() && it.isFile }

                if (existingFile == null && decodedName.isNotBlank()) {
                    val cleanDecoded = decodedName.substringAfterLast("/")
                    existingFile = allLocalFiles.firstOrNull { f ->
                        f.name.equals(cleanDecoded, ignoreCase = true) || f.name.endsWith(cleanDecoded, ignoreCase = true)
                    }
                }
            }

            // 3. Tertiary Match: Match by Picture name
            if (existingFile == null && picName.isNotBlank() && picName != "Picture" && picName != "Image") {
                val cleanPicName = picName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                existingFile = allLocalFiles.firstOrNull { f ->
                    f.name.equals(picName, ignoreCase = true) ||
                            f.name.equals(cleanPicName, ignoreCase = true) ||
                            f.name.endsWith(cleanPicName, ignoreCase = true) ||
                            f.name.contains(cleanPicName, ignoreCase = true)
                }
            }

            // 4. Base64 dataUrl recovery: Decode and save to disk if file missing
            if (existingFile == null) {
                val base64Candidate = if (dataUrl.startsWith("data:image/")) dataUrl else if (originalUrl.startsWith("data:image/")) originalUrl else null
                if (base64Candidate != null) {
                    try {
                        val base64Data = base64Candidate.substringAfter("base64,")
                        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                        val ext = if (base64Candidate.contains("png")) "png" else if (base64Candidate.contains("webp")) "webp" else "jpg"
                        val saveId = if (picId.isNotBlank()) picId else UUID.randomUUID().toString()
                        val savedFile = File(picturesDir, "$saveId.$ext")
                        savedFile.writeBytes(bytes)
                        try {
                            savedFile.copyTo(File(reviewImagesDir, "$saveId.$ext"), overwrite = true)
                        } catch (_: Exception) {}
                        existingFile = savedFile
                    } catch (_: Exception) {}
                }
            }

            if (existingFile != null) {
                mappedUrl = "file://${existingFile.absolutePath}"
            } else if (originalUrl.startsWith("data:image/") || originalUrl.startsWith("blob:")) {
                // Clear transient base64 or blob URLs that cannot be resolved offline
                mappedUrl = ""
            }

            val newObj = JSONObject().apply {
                put("id", if (picId.isNotBlank()) picId else UUID.randomUUID().toString())
                put("name", if (picName.isNotBlank()) picName else "Picture")
                put("url", mappedUrl)
                if (picObj.has("storagePath")) put("storagePath", picObj.optString("storagePath"))
                if (picObj.has("mimetype")) put("mimetype", picObj.optString("mimetype"))
                if (picObj.has("size")) put("size", picObj.optLong("size"))
            }
            newArr.put(newObj)
        }
        return newArr.toString()
    }

    /**
     * Parses the restored JSON content and commits all records into the local Room database,
     * fully replacing previous records as confirmed by user.
     */
    suspend fun parseAndInsertDatabaseJson(
        context: Context,
        jsonString: String,
        database: AppDatabase,
        mediaCount: Int = 0
    ): RestoreResult = withContext(Dispatchers.IO) {
        val dao = database.appDao()
        val reviewImagesDir = File(context.filesDir, "review_images")
        val picturesDir = File(context.filesDir, "pictures")
        val allLocalFiles = ((reviewImagesDir.listFiles() ?: emptyArray()) + (picturesDir.listFiles() ?: emptyArray())).filter { it.isFile }

        try {
            val root = JSONObject(jsonString)

            // 1. Parse Products
            val productsList = mutableListOf<ProductEntity>()
            val prodsArr = root.optJSONArray("products")
            if (prodsArr != null) {
                for (i in 0 until prodsArr.length()) {
                    val p = prodsArr.getJSONObject(i)
                    val tagsArr = p.optJSONArray("tags")
                    val tagsList = mutableListOf<String>()
                    if (tagsArr != null) {
                        for (j in 0 until tagsArr.length()) {
                            tagsList.add(tagsArr.getString(j))
                        }
                    }
                    val tagsStr = if (tagsArr != null) tagsList.joinToString(",") else p.optString("tags", "")
                    val picsArr = p.optJSONArray("pictures")

                    productsList.add(
                        ProductEntity(
                            id = p.getString("id"),
                            name = p.getString("name"),
                            brand = p.optString("brand", ""),
                            tags = tagsStr,
                            overview = p.optString("overview", ""),
                            picturesJson = remapPictureArray(picsArr, context),
                            createdTime = p.optString("createdTime", "")
                        )
                    )
                }
            }

            // 2. Parse Specifications
            val specsList = mutableListOf<SpecificationEntity>()
            val specsArr = root.optJSONArray("specifications")
            if (specsArr != null) {
                for (i in 0 until specsArr.length()) {
                    val s = specsArr.getJSONObject(i)
                    val picsArr = s.optJSONArray("pictures")
                    val orderVal = if (s.has("order")) s.optInt("order", i) else s.optInt("orderIndex", i)

                    specsList.add(
                        SpecificationEntity(
                            id = s.getString("id"),
                            productId = s.optString("productId", ""),
                            productTitle = s.optString("productTitle", ""),
                            specName = s.getString("specName"),
                            details = s.getString("details"),
                            unit = if (s.has("unit") && !s.isNull("unit") && s.getString("unit").isNotBlank()) s.getString("unit") else null,
                            orderIndex = orderVal,
                            picturesJson = remapPictureArray(picsArr, context)
                        )
                    )
                }
            }

            // 3. Parse Reviews
            val reviewsList = mutableListOf<ReviewEntity>()
            val revsArr = root.optJSONArray("reviews")
            if (revsArr != null) {
                for (i in 0 until revsArr.length()) {
                    val r = revsArr.getJSONObject(i)
                    val pids = r.optJSONArray("productIds")
                    val ptitles = r.optJSONArray("productTitles")
                    val pics = r.optJSONArray("pictures")
                    var body = r.optString("body", "")

                    // 3a. Remap images/ and ./images/ paths from webapp export
                    val webappImagesRegex = Regex("""src=["'](?:\./)?images\/([^"']+)["']""", RegexOption.IGNORE_CASE)
                    body = webappImagesRegex.replace(body) { match ->
                        val filename = match.groupValues[1].substringAfterLast('/')
                        "src=\"https://local.review/review_images/$filename\""
                    }

                    // 3b. Remap pictures/ and review_images/ paths
                    val relativeImgRegex = Regex("""src=["'](?:\./)?(?:review_images|pictures)\/([^"']+)["']""", RegexOption.IGNORE_CASE)
                    body = relativeImgRegex.replace(body) { match ->
                        val filename = match.groupValues[1].substringAfterLast('/')
                        "src=\"https://local.review/review_images/$filename\""
                    }

                    // 3c. Remap file: and /data/... paths
                    val oldFileSrcRegex = Regex("""src=["']file:/*([^"']+)["']""", RegexOption.IGNORE_CASE)
                    body = oldFileSrcRegex.replace(body) { match ->
                        val filename = match.groupValues[1].substringAfterLast('/')
                        "src=\"https://local.review/review_images/$filename\""
                    }
                    val oldDataSrcRegex = Regex("""src=["']\/data\/[^"']*\/(?:review_images|pictures)\/([^"']+)["']""", RegexOption.IGNORE_CASE)
                    body = oldDataSrcRegex.replace(body) { match ->
                        val filename = match.groupValues[1]
                        "src=\"https://local.review/review_images/$filename\""
                    }

                    // 3d. Remap images with data-attachment-id where src was cleared or empty
                    val attIdRegex = Regex("""<img\b([^>]*?)\bdata-attachment-id=["']([^"']+)["']([^>]*?)>""", RegexOption.IGNORE_CASE)
                    body = attIdRegex.replace(body) { match ->
                        val before = match.groupValues[1]
                        val attId = match.groupValues[2].trim()
                        val after = match.groupValues[3]

                        val matchingFile = allLocalFiles.firstOrNull { f ->
                            val base = f.name.substringBeforeLast(".")
                            base.equals(attId, ignoreCase = true) || f.name.startsWith("${attId}_", ignoreCase = true) || f.name.startsWith(attId, ignoreCase = true)
                        }
                        if (matchingFile != null) {
                            val virtualUrl = "https://local.review/review_images/${matchingFile.name}"
                            var tagContent = "$before data-attachment-id=\"$attId\"$after"
                            tagContent = if (tagContent.contains(Regex("""src=["'][^"']*["']""", RegexOption.IGNORE_CASE))) {
                                tagContent.replace(Regex("""src=["'][^"']*["']""", RegexOption.IGNORE_CASE), "src=\"$virtualUrl\"")
                            } else {
                                " src=\"$virtualUrl\" $tagContent"
                            }
                            "<img $tagContent>"
                        } else {
                            match.value
                        }
                    }

                    // 3e. Remap Firebase storage URLs in body if local file exists
                    val fbStorageRegex = Regex("""src=["']https:\/\/firebasestorage\.googleapis\.com\/[^"']*\/o\/([^"'\?]+)[^"']*["']""", RegexOption.IGNORE_CASE)
                    body = fbStorageRegex.replace(body) { match ->
                        val rawPath = match.groupValues[1]
                        val decoded = try { URLDecoder.decode(rawPath, "UTF-8") } catch (_: Exception) { rawPath }
                        val filename = decoded.substringAfterLast("/")
                        val matchingFile = allLocalFiles.firstOrNull { it.name.equals(filename, ignoreCase = true) }
                        if (matchingFile != null) {
                            "src=\"https://local.review/review_images/${matchingFile.name}\""
                        } else {
                            match.value
                        }
                    }

                    reviewsList.add(
                        ReviewEntity(
                            id = r.getString("id"),
                            reviewNumber = r.optInt("reviewNumber", i + 1),
                            title = r.optString("title", ""),
                            body = body,
                            authorName = r.optString("authorName", "Verified Reviewer"),
                            rating = r.optInt("rating", 5),
                            productIdsJson = pids?.toString() ?: "[]",
                            productTitlesJson = ptitles?.toString() ?: "[]",
                            picturesJson = remapPictureArray(pics, context),
                            createdTime = r.optString("createdTime", "")
                        )
                    )
                }
            }

            // 4. Parse Users
            val usersList = mutableListOf<UserEntity>()
            val usersArr = root.optJSONArray("users")
            if (usersArr != null) {
                for (i in 0 until usersArr.length()) {
                    val u = usersArr.getJSONObject(i)
                    usersList.add(
                        UserEntity(
                            id = u.getString("id"),
                            name = u.getString("name"),
                            email = u.getString("email"),
                            role = u.optString("role", "User")
                        )
                    )
                }
            }

            // 5. Replace previous records atomically in Room SQLite database
            dao.clearProducts()
            if (productsList.isNotEmpty()) {
                dao.insertProducts(productsList)
            }

            dao.clearSpecifications()
            if (specsList.isNotEmpty()) {
                dao.insertSpecifications(specsList)
            }

            dao.clearReviews()
            if (reviewsList.isNotEmpty()) {
                dao.insertReviews(reviewsList)
            }

            if (usersList.isNotEmpty()) {
                dao.clearUsers()
                dao.insertUsers(usersList)
            }

            RestoreResult(
                success = true,
                message = "Restored ${productsList.size} products, ${specsList.size} specs, ${reviewsList.size} reviews, and $mediaCount photos.",
                restoredProducts = productsList.size,
                restoredSpecs = specsList.size,
                restoredReviews = reviewsList.size,
                restoredUsers = usersList.size,
                restoredMedia = mediaCount
            )
        } catch (e: Exception) {
            RestoreResult(false, "JSON parsing error: ${e.localizedMessage}")
        }
    }
}
