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
    val appName: String = "ProductCompare",
    val exportDate: String,
    val productCount: Int,
    val specCount: Int,
    val reviewCount: Int,
    val userCount: Int,
    val pictureCount: Int,
    val version: String = "1.0"
)

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val restoredProducts: Int = 0,
    val restoredSpecs: Int = 0,
    val restoredReviews: Int = 0,
    val restoredUsers: Int = 0
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

    /**
     * Builds standard database.json matching seed catalog format.
     */
    fun buildDatabaseJson(
        products: List<ProductItem>,
        specifications: List<SpecificationItem>,
        reviews: List<ReviewItem>,
        users: List<UserEntity>
    ): String {
        val root = JSONObject()

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
            po.put("createdTime", p.createdTime)
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
            if (s.isNumeric != null) {
                so.put("isNumeric", s.isNumeric)
            }

            val picsArr = JSONArray()
            s.pictures.forEach { pic ->
                val pobj = JSONObject()
                pobj.put("id", pic.id)
                pobj.put("name", pic.name)
                pobj.put("url", pic.url)
                picsArr.put(pobj)
            }
            so.put("pictures", picsArr)
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
     * Creates a full ZIP archive containing:
     * 1. manifest.json
     * 2. database.json
     * 3. pictures/ (downloaded image files referenced in DB)
     * 4. reviews/ (formatted markdown files for each review note)
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

        // Bundle ALL existing local review images in context.filesDir/review_images
        val localReviewImagesFolder = File(context.filesDir, "review_images")
        if (localReviewImagesFolder.exists() && localReviewImagesFolder.isDirectory) {
            localReviewImagesFolder.listFiles()?.forEach { f ->
                if (f.isFile) {
                    try {
                        val dest = File(reviewImagesDir, f.name)
                        f.copyTo(dest, overwrite = true)
                        packagedPicturesCount++
                    } catch (_: Exception) {}
                }
            }
        }

        // Bundle ALL existing local pictures in context.filesDir/pictures
        val localPicturesFolder = File(context.filesDir, "pictures")
        if (localPicturesFolder.exists() && localPicturesFolder.isDirectory) {
            localPicturesFolder.listFiles()?.forEach { f ->
                if (f.isFile) {
                    try {
                        val dest = File(picturesDir, f.name)
                        f.copyTo(dest, overwrite = true)
                        packagedPicturesCount++
                    } catch (_: Exception) {}
                }
            }
        }

        // Bundle all images embedded in review HTML bodies
        reviews.forEach { rev ->
            val srcRegex = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            srcRegex.findAll(rev.body).forEach { m ->
                val srcUrl = m.groupValues[1]
                if (srcUrl.contains("local.review") || srcUrl.contains("review_images") || srcUrl.contains("pictures") || srcUrl.startsWith("file://") || srcUrl.startsWith("/")) {
                    val rawFilename = srcUrl.substringAfterLast("/").substringBefore("?")
                    val decoded = try { URLDecoder.decode(rawFilename, "UTF-8") } catch (_: Exception) { rawFilename }
                    val c1 = File(File(context.filesDir, "review_images"), decoded)
                    val c2 = File(File(context.filesDir, "review_images"), rawFilename)
                    val c3 = File(File(context.filesDir, "pictures"), decoded)
                    val c4 = File(File(context.filesDir, "pictures"), rawFilename)
                    val found = listOf(c1, c2, c3, c4).firstOrNull { it.exists() && it.isFile }
                    if (found != null) {
                        try {
                            found.copyTo(File(reviewImagesDir, found.name), overwrite = true)
                            packagedPicturesCount++
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        allPictures.values.forEachIndexed { _, pic ->
            if (pic.url.isNotBlank()) {
                if (pic.url.startsWith("http://") || pic.url.startsWith("https://")) {
                    if (pic.url.contains("local.review")) {
                        // Virtual URL: extract local file
                        val filename = pic.url.substringAfterLast("/")
                        val localF1 = File(context.filesDir, "review_images/$filename")
                        val localF2 = File(context.filesDir, "pictures/$filename")
                        val foundF = if (localF1.exists() && localF1.isFile) localF1 else if (localF2.exists() && localF2.isFile) localF2 else null
                        if (foundF != null) {
                            try {
                                val dest = File(reviewImagesDir, filename)
                                foundF.copyTo(dest, overwrite = true)
                                foundF.copyTo(File(picturesDir, filename), overwrite = true)
                                packagedPicturesCount++
                            } catch (_: Exception) {}
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
                                    val picFile = File(picturesDir, "${pic.id}_$cleanName")
                                    picFile.outputStream().use { out ->
                                        body.byteStream().copyTo(out)
                                    }
                                    try {
                                        picFile.copyTo(File(reviewImagesDir, "${pic.id}_$cleanName"), overwrite = true)
                                    } catch (_: Exception) {}
                                    packagedPicturesCount++
                                }
                            }
                        } catch (_: Exception) {
                            // Continue gracefully
                        }
                    }
                } else if (pic.url.startsWith("content://")) {
                    try {
                        val cleanName = pic.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        val destName = if (cleanName.contains(".")) "${pic.id}_$cleanName" else "${pic.id}_$cleanName.jpg"
                        val destFile = File(picturesDir, destName)
                        context.contentResolver.openInputStream(Uri.parse(pic.url))?.use { input ->
                            destFile.outputStream().use { out -> input.copyTo(out) }
                        }
                        if (destFile.exists() && destFile.length() > 0) {
                            try {
                                destFile.copyTo(File(reviewImagesDir, destName), overwrite = true)
                            } catch (_: Exception) {}
                            packagedPicturesCount++
                        }
                    } catch (_: Exception) {}
                } else if (pic.url.startsWith("file://") || pic.url.startsWith("/")) {
                    val localPath = pic.url.removePrefix("file://")
                    val localF = File(localPath)
                    if (localF.exists() && localF.isFile) {
                        try {
                            val dest = File(reviewImagesDir, localF.name)
                            localF.copyTo(dest, overwrite = true)
                            localF.copyTo(File(picturesDir, localF.name), overwrite = true)
                            packagedPicturesCount++
                        } catch (_: Exception) {}
                    } else {
                        val fname = localPath.substringAfterLast("/")
                        val c1 = File(File(context.filesDir, "pictures"), fname)
                        val c2 = File(File(context.filesDir, "review_images"), fname)
                        val found = if (c1.exists()) c1 else if (c2.exists()) c2 else null
                        if (found != null && found.isFile) {
                            try {
                                found.copyTo(File(picturesDir, found.name), overwrite = true)
                                found.copyTo(File(reviewImagesDir, found.name), overwrite = true)
                                packagedPicturesCount++
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }

        // 4. Write manifest.json
        val manifestObj = JSONObject().apply {
            put("appName", "ProductCompare")
            put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
            put("productCount", products.size)
            put("specificationCount", specifications.size)
            put("reviewCount", reviews.size)
            put("userCount", users.size)
            put("pictureCount", packagedPicturesCount)
            put("version", "1.0")
        }
        File(backupDir, "manifest.json").writeText(manifestObj.toString(2))

        // 5. Compress backup folder into ZIP archive
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
     * Restores database from a given URI (supporting both .zip archives and raw .json backups).
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

            RestoreResult(false, "Could not restore: File is neither a valid ProductCompare ZIP archive nor a valid database.json file.")
        } catch (e: Exception) {
            RestoreResult(false, "Restore failed: ${e.localizedMessage ?: "Unknown error"}")
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private suspend fun tryRestoreFromZip(
        context: Context,
        zipFile: File,
        database: AppDatabase
    ): RestoreResult = withContext(Dispatchers.IO) {
        var databaseJsonString: String? = null
        var fallbackJsonString: String? = null
        val destReviewImagesDir = File(context.filesDir, "review_images")
        if (!destReviewImagesDir.exists()) destReviewImagesDir.mkdirs()
        val destPicturesDir = File(context.filesDir, "pictures")
        if (!destPicturesDir.exists()) destPicturesDir.mkdirs()

        try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val rawName = entry.name.replace('\\', '/')
                    val nameLower = rawName.lowercase()
                    val filename = rawName.substringAfterLast("/")
                    val decodedFilename = try { URLDecoder.decode(filename, "UTF-8") } catch (_: Exception) { filename }

                    if (nameLower == "database.json" || nameLower.endsWith("/database.json")) {
                        val buffer = ByteArrayOutputStream()
                        zis.copyTo(buffer)
                        databaseJsonString = buffer.toString("UTF-8")
                    } else if (nameLower.endsWith(".json") && !nameLower.contains("manifest") && databaseJsonString == null) {
                        val buffer = ByteArrayOutputStream()
                        zis.copyTo(buffer)
                        fallbackJsonString = buffer.toString("UTF-8")
                    } else if (!entry.isDirectory && filename.isNotBlank()) {
                        val ext = filename.substringAfterLast(".", "").lowercase()
                        val isMediaFolder = nameLower.startsWith("images/") || nameLower.contains("/images/") ||
                                            nameLower.startsWith("review_images/") || nameLower.contains("/review_images/") ||
                                            nameLower.startsWith("pictures/") || nameLower.contains("/pictures/") ||
                                            nameLower.startsWith("files/") || nameLower.contains("/files/") ||
                                            nameLower.startsWith("attachments/") || nameLower.contains("/attachments/") ||
                                            nameLower.startsWith("media/") || nameLower.contains("/media/")
                        val isImageExt = ext in listOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp", "jfif", "avif", "ico")
                        if (isMediaFolder || isImageExt) {
                            val targetFile = File(destReviewImagesDir, filename)
                            targetFile.outputStream().use { out -> zis.copyTo(out) }
                            try { targetFile.copyTo(File(destPicturesDir, filename), overwrite = true) } catch (_: Exception) {}
                            if (decodedFilename != filename && decodedFilename.isNotBlank()) {
                                try { targetFile.copyTo(File(destReviewImagesDir, decodedFilename), overwrite = true) } catch (_: Exception) {}
                                try { targetFile.copyTo(File(destPicturesDir, decodedFilename), overwrite = true) } catch (_: Exception) {}
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val finalJson = databaseJsonString ?: fallbackJsonString
            if (finalJson.isNullOrBlank()) {
                return@withContext RestoreResult(false, "ZIP does not contain database.json")
            }

            return@withContext parseAndInsertDatabaseJson(context, finalJson, database)
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
                return@withContext parseAndInsertDatabaseJson(context, content, database)
            }
            RestoreResult(false, "File does not match ProductCompare database schema")
        } catch (e: Exception) {
            RestoreResult(false, "JSON read error: ${e.localizedMessage}")
        }
    }

    private fun remapPictureArray(picsArr: JSONArray?, context: Context): String {
        if (picsArr == null) return "[]"
        val newArr = JSONArray()
        val reviewImagesDir = File(context.filesDir, "review_images")
        if (!reviewImagesDir.exists()) reviewImagesDir.mkdirs()
        val picturesDir = File(context.filesDir, "pictures")
        if (!picturesDir.exists()) picturesDir.mkdirs()

        val allLocalFiles = (reviewImagesDir.listFiles() ?: emptyArray()).toList() +
                            (picturesDir.listFiles() ?: emptyArray()).toList()

        for (j in 0 until picsArr.length()) {
            val picObj = picsArr.getJSONObject(j)
            val originalUrl = picObj.optString("url", "").trim()
            val picId = picObj.optString("id", "").trim()
            val picName = picObj.optString("name", "").trim()
            val dataUrl = picObj.optString("dataUrl", "").trim()

            var mappedUrl = originalUrl
            var existingFile: File? = null

            // 1. Direct path check if it's already an existing file
            if (originalUrl.startsWith("file://") || originalUrl.startsWith("/")) {
                val path = originalUrl.removePrefix("file://")
                val f = File(path)
                if (f.exists() && f.isFile) existingFile = f
            }

            // 2. Lookup by ID in directories
            if (existingFile == null && picId.isNotBlank()) {
                val cleanId = picId.lowercase()
                existingFile = allLocalFiles.firstOrNull { f ->
                    val fn = f.name.lowercase()
                    val fNoExt = f.nameWithoutExtension.lowercase()
                    fn == cleanId || fNoExt == cleanId ||
                    fn.startsWith("${cleanId}.") || fn.startsWith("${cleanId}_") ||
                    (cleanId.length >= 4 && fn.contains(cleanId))
                }
            }

            // 3. Lookup by URL filename
            if (existingFile == null && originalUrl.isNotBlank() && !originalUrl.startsWith("blob:")) {
                val rawFilename = originalUrl.substringAfterLast("/").substringBefore("?")
                val decodedName = try { URLDecoder.decode(rawFilename, "UTF-8") } catch (_: Exception) { rawFilename }
                val cand1 = File(reviewImagesDir, rawFilename).takeIf { it.exists() && it.isFile }
                val cand2 = File(reviewImagesDir, decodedName).takeIf { it.exists() && it.isFile }
                val cand3 = File(picturesDir, rawFilename).takeIf { it.exists() && it.isFile }
                val cand4 = File(picturesDir, decodedName).takeIf { it.exists() && it.isFile }
                existingFile = cand1 ?: cand2 ?: cand3 ?: cand4
                if (existingFile == null) {
                    val rawLower = rawFilename.lowercase()
                    val decLower = decodedName.lowercase()
                    existingFile = allLocalFiles.firstOrNull { f ->
                        val fn = f.name.lowercase()
                        fn == rawLower || fn == decLower || fn.contains(rawLower) || rawLower.contains(fn)
                    }
                }
            }

            // 4. Lookup by picture name
            if (existingFile == null && picName.isNotBlank() && picName != "Picture") {
                val cleanPicName = picName.replace(Regex("[^a-zA-Z0-9._-]"), "_").lowercase()
                existingFile = allLocalFiles.firstOrNull { f ->
                    val fn = f.name.lowercase()
                    fn == cleanPicName || fn.endsWith(cleanPicName) || fn.contains(cleanPicName)
                }
            }

            // 5. Decode base64 dataUrl if available
            if (existingFile == null && dataUrl.startsWith("data:image/")) {
                try {
                    val mime = dataUrl.substringAfter("data:image/").substringBefore(";").lowercase()
                    val ext = if (mime.contains("png")) "png" else if (mime.contains("webp")) "webp" else "jpg"
                    val b64 = dataUrl.substringAfter("base64,")
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    val fn = "${if (picId.isNotBlank()) picId else UUID.randomUUID().toString().take(12)}.$ext"
                    val f = File(reviewImagesDir, fn)
                    f.writeBytes(bytes)
                    try { f.copyTo(File(picturesDir, fn), overwrite = true) } catch (_: Exception) {}
                    existingFile = f
                } catch (_: Exception) {}
            }

            if (existingFile != null && existingFile.exists()) {
                mappedUrl = "file://${existingFile.absolutePath}"
            } else if (originalUrl.startsWith("https://local.review/review_images/")) {
                mappedUrl = originalUrl
            }

            val newObj = JSONObject().apply {
                put("id", if (picId.isNotBlank()) picId else UUID.randomUUID().toString())
                put("name", if (picName.isNotBlank()) picName else "Picture")
                put("url", mappedUrl)
            }
            newArr.put(newObj)
        }
        return newArr.toString()
    }

    private fun normalizeAndRemapReviewContent(
        rawBody: String,
        picsArr: JSONArray?,
        reviewId: String,
        context: Context
    ): Pair<String, String> {
        val reviewImagesDir = File(context.filesDir, "review_images")
        if (!reviewImagesDir.exists()) reviewImagesDir.mkdirs()
        val picturesDir = File(context.filesDir, "pictures")
        if (!picturesDir.exists()) picturesDir.mkdirs()

        val allLocalFiles = (reviewImagesDir.listFiles() ?: emptyArray()).toList() +
                            (picturesDir.listFiles() ?: emptyArray()).toList()

        fun findLocalFile(candidate: String?): File? {
            if (candidate.isNullOrBlank()) return null
            val clean = candidate.trim()
            val cleanLower = clean.lowercase()
            val decoded = try { URLDecoder.decode(clean, "UTF-8") } catch (_: Exception) { clean }
            val decodedLower = decoded.lowercase()

            val direct = File(reviewImagesDir, clean).takeIf { it.exists() && it.isFile }
                ?: File(picturesDir, clean).takeIf { it.exists() && it.isFile }
                ?: File(reviewImagesDir, decoded).takeIf { it.exists() && it.isFile }
                ?: File(picturesDir, decoded).takeIf { it.exists() && it.isFile }
            if (direct != null) return direct

            return allLocalFiles.firstOrNull { f ->
                val fName = f.name.lowercase()
                val fNoExt = f.nameWithoutExtension.lowercase()
                fName == cleanLower ||
                fNoExt == cleanLower ||
                fName == decodedLower ||
                fNoExt == decodedLower ||
                (cleanLower.length >= 3 && (fName.startsWith("${cleanLower}.") || fName.startsWith("${cleanLower}_"))) ||
                (decodedLower.length >= 3 && (fName.startsWith("${decodedLower}.") || fName.startsWith("${decodedLower}_"))) ||
                (cleanLower.length >= 4 && fName.contains(cleanLower)) ||
                (decodedLower.length >= 4 && fName.contains(decodedLower))
            }
        }

        val resolvedPictures = mutableListOf<JSONObject>()
        val pictureMapById = mutableMapOf<String, String>()
        val pictureMapByName = mutableMapOf<String, String>()

        if (picsArr != null) {
            for (j in 0 until picsArr.length()) {
                val pObj = picsArr.getJSONObject(j)
                val pId = pObj.optString("id", "").trim()
                val pName = pObj.optString("name", "").trim()
                val pUrl = pObj.optString("url", "").trim()
                val pDataUrl = pObj.optString("dataUrl", "").trim()

                var mappedUrl = pUrl
                var localFile: File? = null

                if (pId.isNotBlank()) {
                    localFile = findLocalFile(pId)
                }
                if (localFile == null && pUrl.isNotBlank() && !pUrl.startsWith("blob:")) {
                    val rawFn = pUrl.substringAfterLast('/').substringBefore('?')
                    localFile = findLocalFile(rawFn)
                }
                if (localFile == null && pName.isNotBlank() && pName != "Picture") {
                    localFile = findLocalFile(pName)
                }
                if (localFile == null && pDataUrl.startsWith("data:image/")) {
                    try {
                        val mime = pDataUrl.substringAfter("data:image/").substringBefore(";").lowercase()
                        val ext = if (mime.contains("png")) "png" else if (mime.contains("webp")) "webp" else "jpg"
                        val b64 = pDataUrl.substringAfter("base64,")
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        val fn = "rev_${reviewId}_${if (pId.isNotBlank()) pId else UUID.randomUUID().toString().take(8)}.$ext"
                        val f = File(reviewImagesDir, fn)
                        f.writeBytes(bytes)
                        try { f.copyTo(File(picturesDir, fn), overwrite = true) } catch (_: Exception) {}
                        localFile = f
                    } catch (_: Exception) {}
                }

                if (localFile != null && localFile.exists()) {
                    mappedUrl = "file://${localFile.absolutePath}"
                    val webUrl = "https://local.review/review_images/${localFile.name}"
                    if (pId.isNotBlank()) pictureMapById[pId] = webUrl
                    if (pName.isNotBlank()) pictureMapByName[pName] = webUrl
                } else if (pUrl.startsWith("https://local.review/review_images/")) {
                    if (pId.isNotBlank()) pictureMapById[pId] = pUrl
                    if (pName.isNotBlank()) pictureMapByName[pName] = pUrl
                }

                resolvedPictures.add(JSONObject().apply {
                    put("id", if (pId.isNotBlank()) pId else UUID.randomUUID().toString())
                    put("name", if (pName.isNotBlank()) pName else "Picture")
                    put("url", mappedUrl)
                })
            }
        }

        var body = rawBody.trim()

        // 1. Unescape HTML entities if needed
        if ((body.contains("&lt;h") || body.contains("&lt;p") || body.contains("&lt;div") || body.contains("&lt;img")) &&
            !body.contains("<h") && !body.contains("<p")) {
            body = body.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replace("&quot;", "\"")
        }

        // 2. Decode inline base64 images into local image files
        val inlineBase64Regex = Regex("""<img\b([^>]*?)src=["']data:image\/([a-zA-Z0-9+]+);base64,([A-Za-z0-9+/=]+)["']([^>]*)>""", RegexOption.IGNORE_CASE)
        var inlineIdx = 0
        body = inlineBase64Regex.replace(body) { match ->
            val prefix = match.groupValues[1]
            val mimeExt = match.groupValues[2].lowercase().let { if (it.contains("png")) "png" else if (it.contains("webp")) "webp" else "jpg" }
            val b64Data = match.groupValues[3]
            val suffix = match.groupValues[4]
            try {
                val bytes = Base64.decode(b64Data, Base64.DEFAULT)
                val fn = "rev_${reviewId}_inline_${inlineIdx++}.$mimeExt"
                val f = File(reviewImagesDir, fn)
                f.writeBytes(bytes)
                try { f.copyTo(File(picturesDir, fn), overwrite = true) } catch (_: Exception) {}
                resolvedPictures.add(JSONObject().apply {
                    put("id", "inline_${fn.substringBeforeLast('.')}")
                    put("name", "Review Photo $inlineIdx")
                    put("url", "file://${f.absolutePath}")
                })
                "<img ${prefix}src=\"https://local.review/review_images/$fn\"$suffix>"
            } catch (_: Exception) {
                match.value
            }
        }

        // 3. Convert markdown images ![alt](url) to HTML figures with normalized URLs
        val markdownImgRegex = Regex("""!\[(.*?)\]\((.*?)\)""")
        body = markdownImgRegex.replace(body) { match ->
            val alt = match.groupValues[1].trim()
            val urlOrPath = match.groupValues[2].trim()
            val resolvedFile = findLocalFile(urlOrPath.substringAfterLast('/').substringBefore('?'))
                ?: findLocalFile(urlOrPath)
                ?: (if (alt.isNotBlank()) findLocalFile(alt) else null)
            val finalUrl = when {
                resolvedFile != null -> "https://local.review/review_images/${resolvedFile.name}"
                urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://") -> urlOrPath
                else -> "https://local.review/review_images/${urlOrPath.substringAfterLast('/')}"
            }
            val cleanAlt = alt.ifBlank { "Review Photo" }
            if (resolvedFile != null && resolvedPictures.none { it.optString("url").contains(resolvedFile.name) }) {
                resolvedPictures.add(JSONObject().apply {
                    put("id", resolvedFile.nameWithoutExtension)
                    put("name", cleanAlt)
                    put("url", "file://${resolvedFile.absolutePath}")
                })
            }
            "<figure class=\"review-figure\"><img src=\"$finalUrl\" alt=\"$cleanAlt\" loading=\"lazy\" /><figcaption class=\"review-caption\">$cleanAlt</figcaption></figure>"
        }

        // 4. Normalize all <img> tags in HTML
        val imgTagRegex = Regex("""<img\b([^>]*)>""", RegexOption.IGNORE_CASE)
        body = imgTagRegex.replace(body) { match ->
            val attrs = match.groupValues[1]
            val srcMatch = Regex("""src=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(attrs)
            val attIdMatch = Regex("""(?:data-attachment-id|dataAttachmentId|data-attachment-token)=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)
            val altMatch = Regex("""alt=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(attrs)

            val rawSrc = srcMatch?.groupValues?.get(1)?.trim() ?: ""
            val attId = attIdMatch?.groupValues?.get(1)?.trim() ?: ""
            val alt = altMatch?.groupValues?.get(1)?.trim() ?: ""

            var resolvedFile: File? = null
            if (attId.isNotBlank()) {
                resolvedFile = findLocalFile(attId)
            }
            if (resolvedFile == null && rawSrc.isNotBlank() && !rawSrc.startsWith("blob:") && !rawSrc.startsWith("data:")) {
                val fn = rawSrc.substringAfterLast('/').substringBefore('?')
                resolvedFile = findLocalFile(fn)
            }
            if (resolvedFile == null && alt.isNotBlank()) {
                resolvedFile = findLocalFile(alt)
            }

            val targetUrl = when {
                resolvedFile != null -> "https://local.review/review_images/${resolvedFile.name}"
                attId.isNotBlank() && pictureMapById.containsKey(attId) -> pictureMapById[attId]!!
                alt.isNotBlank() && pictureMapByName.containsKey(alt) -> pictureMapByName[alt]!!
                rawSrc.startsWith("https://local.review/") -> rawSrc
                rawSrc.startsWith("http://") || rawSrc.startsWith("https://") -> rawSrc
                attId.isNotBlank() -> "https://local.review/review_images/$attId"
                rawSrc.isNotBlank() && !rawSrc.startsWith("blob:") -> "https://local.review/review_images/${rawSrc.substringAfterLast('/')}"
                else -> ""
            }

            if (resolvedFile != null && resolvedPictures.none { it.optString("url").contains(resolvedFile.name) }) {
                resolvedPictures.add(JSONObject().apply {
                    put("id", attId.ifBlank { resolvedFile.nameWithoutExtension })
                    put("name", alt.ifBlank { resolvedFile.name })
                    put("url", "file://${resolvedFile.absolutePath}")
                })
            }

            if (targetUrl.isNotBlank()) {
                var newAttrs = attrs
                if (srcMatch != null) {
                    newAttrs = newAttrs.replace(srcMatch.value, "src=\"$targetUrl\"")
                } else {
                    newAttrs = "$newAttrs src=\"$targetUrl\""
                }
                "<img$newAttrs>"
            } else {
                match.value
            }
        }

        val finalPicturesJson = JSONArray().apply {
            resolvedPictures.forEach { put(it) }
        }.toString()

        return Pair(body, finalPicturesJson)
    }

    private suspend fun parseAndInsertDatabaseJson(
        context: Context,
        jsonString: String,
        database: AppDatabase
    ): RestoreResult = withContext(Dispatchers.IO) {
        val dao = database.appDao()
        try {
            val root = JSONObject(jsonString)

            // Parse Products
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
                    val picsArr = p.optJSONArray("pictures")
                    productsList.add(
                        ProductEntity(
                            id = p.getString("id"),
                            name = p.getString("name"),
                            brand = p.optString("brand", ""),
                            tags = tagsList.joinToString(","),
                            overview = p.optString("overview", ""),
                            picturesJson = remapPictureArray(picsArr, context),
                            createdTime = p.optString("createdTime", "")
                        )
                    )
                }
            }

            // Parse Specs
            val specsList = mutableListOf<SpecificationEntity>()
            val specsArr = root.optJSONArray("specifications")
            if (specsArr != null) {
                for (i in 0 until specsArr.length()) {
                    val s = specsArr.getJSONObject(i)
                    val picsArr = s.optJSONArray("pictures")
                    specsList.add(
                        SpecificationEntity(
                            id = s.getString("id"),
                            productId = s.optString("productId", ""),
                            productTitle = s.optString("productTitle", ""),
                            specName = s.getString("specName"),
                            details = s.getString("details"),
                            unit = if (s.has("unit") && !s.isNull("unit") && s.getString("unit").isNotBlank()) s.getString("unit") else null,
                            orderIndex = s.optInt("order", i),
                            picturesJson = remapPictureArray(picsArr, context),
                            isNumeric = if (s.has("isNumeric") && !s.isNull("isNumeric")) s.getBoolean("isNumeric") else null
                        )
                    )
                }
            }

            // Parse Reviews with thorough media restoration
            val reviewsList = mutableListOf<ReviewEntity>()
            val revsArr = root.optJSONArray("reviews")
            if (revsArr != null) {
                for (i in 0 until revsArr.length()) {
                    val r = revsArr.getJSONObject(i)
                    val reviewId = r.getString("id")
                    val pids = r.optJSONArray("productIds")
                    val ptitles = r.optJSONArray("productTitles")
                    val pics = r.optJSONArray("pictures")
                    val rawBody = r.optString("body", "")

                    val (normalizedBody, picturesJson) = normalizeAndRemapReviewContent(rawBody, pics, reviewId, context)

                    reviewsList.add(
                        ReviewEntity(
                            id = reviewId,
                            reviewNumber = r.optInt("reviewNumber", i + 1),
                            title = r.optString("title", ""),
                            body = normalizedBody,
                            authorName = r.optString("authorName", "Verified Reviewer"),
                            rating = r.optInt("rating", 5),
                            productIdsJson = pids?.toString() ?: "[]",
                            productTitlesJson = ptitles?.toString() ?: "[]",
                            picturesJson = picturesJson,
                            createdTime = r.optString("createdTime", "")
                        )
                    )
                }
            }

            // Parse Users
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

            // Insert into Room SQLite database
            if (productsList.isNotEmpty()) {
                dao.clearProducts()
                dao.insertProducts(productsList)
            }
            if (specsList.isNotEmpty()) {
                dao.clearSpecifications()
                dao.insertSpecifications(specsList)
            }
            if (reviewsList.isNotEmpty()) {
                dao.clearReviews()
                dao.insertReviews(reviewsList)
            }
            if (usersList.isNotEmpty()) {
                dao.clearUsers()
                dao.insertUsers(usersList)
            }

            RestoreResult(
                success = true,
                message = "Restored ${productsList.size} products, ${specsList.size} specs, and ${reviewsList.size} reviews.",
                restoredProducts = productsList.size,
                restoredSpecs = specsList.size,
                restoredReviews = reviewsList.size,
                restoredUsers = usersList.size
            )
        } catch (e: Exception) {
            RestoreResult(false, "JSON parsing error: ${e.localizedMessage}")
        }
    }
}
