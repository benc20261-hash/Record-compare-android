package com.example.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
        val destReviewImagesDir = File(context.filesDir, "review_images")
        destReviewImagesDir.mkdirs()
        val destPicturesDir = File(context.filesDir, "pictures")
        destPicturesDir.mkdirs()

        try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val rawName = entry.name
                    val nameLower = rawName.lowercase()
                    val filename = rawName.substringAfterLast("/")

                    if (nameLower.endsWith("database.json") || (nameLower.endsWith(".json") && !nameLower.contains("manifest"))) {
                        val buffer = ByteArrayOutputStream()
                        zis.copyTo(buffer)
                        databaseJsonString = buffer.toString("UTF-8")
                    } else if (!entry.isDirectory && filename.isNotBlank()) {
                        val ext = filename.substringAfterLast(".", "").lowercase()
                        if (ext in listOf("jpg", "jpeg", "png", "webp", "gif", "svg")) {
                            val targetFile = File(destReviewImagesDir, filename)
                            targetFile.outputStream().use { out ->
                                zis.copyTo(out)
                            }
                            try {
                                targetFile.copyTo(File(destPicturesDir, filename), overwrite = true)
                            } catch (_: Exception) {}
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (databaseJsonString.isNullOrBlank()) {
                return@withContext RestoreResult(false, "ZIP does not contain database.json")
            }

            return@withContext parseAndInsertDatabaseJson(context, databaseJsonString!!, database)
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
        val picturesDir = File(context.filesDir, "pictures")
        for (j in 0 until picsArr.length()) {
            val picObj = picsArr.getJSONObject(j)
            val originalUrl = picObj.optString("url", "")
            val picId = picObj.optString("id", "")
            val picName = picObj.optString("name", "")
            var mappedUrl = originalUrl
            if (originalUrl.isNotBlank()) {
                val rawFilename = originalUrl.substringAfterLast("/").substringBefore("?")
                val decodedName = try { URLDecoder.decode(rawFilename, "UTF-8") } catch (_: Exception) { rawFilename }
                val cleanPicName = picName.replace(Regex("[^a-zA-Z0-9._-]"), "_")

                // 1. Direct candidates
                val candidate1 = File(reviewImagesDir, decodedName)
                val candidate2 = File(reviewImagesDir, rawFilename)
                val candidate3 = File(picturesDir, decodedName)
                val candidate4 = File(picturesDir, rawFilename)

                var existingFile = listOf(candidate1, candidate2, candidate3, candidate4).firstOrNull { it.exists() && it.isFile }

                // 2. ID prefix or contains ID
                if (existingFile == null && picId.isNotBlank()) {
                    val allLocalFiles = (reviewImagesDir.listFiles() ?: emptyArray()) + (picturesDir.listFiles() ?: emptyArray())
                    existingFile = allLocalFiles.firstOrNull { f ->
                        f.isFile && (f.name.startsWith("${picId}_") || f.name.contains(picId))
                    }
                }

                // 3. Clean picture name match
                if (existingFile == null && cleanPicName.isNotBlank() && cleanPicName != "Picture") {
                    val allLocalFiles = (reviewImagesDir.listFiles() ?: emptyArray()) + (picturesDir.listFiles() ?: emptyArray())
                    existingFile = allLocalFiles.firstOrNull { f ->
                        f.isFile && (f.name.endsWith(cleanPicName) || f.name.contains(cleanPicName))
                    }
                }

                if (existingFile != null) {
                    mappedUrl = "file://${existingFile.absolutePath}"
                } else if (originalUrl.contains("review_images/") || originalUrl.contains("pictures/")) {
                    val candidate = File(reviewImagesDir, rawFilename)
                    if (candidate.exists()) {
                        mappedUrl = "file://${candidate.absolutePath}"
                    } else {
                        val candidatePic = File(picturesDir, rawFilename)
                        if (candidatePic.exists()) {
                            mappedUrl = "file://${candidatePic.absolutePath}"
                        }
                    }
                }
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
                            picturesJson = remapPictureArray(picsArr, context)
                        )
                    )
                }
            }

            // Parse Reviews
            val reviewsList = mutableListOf<ReviewEntity>()
            val revsArr = root.optJSONArray("reviews")
            if (revsArr != null) {
                for (i in 0 until revsArr.length()) {
                    val r = revsArr.getJSONObject(i)
                    val pids = r.optJSONArray("productIds")
                    val ptitles = r.optJSONArray("productTitles")
                    val pics = r.optJSONArray("pictures")

                    var body = r.getString("body")
                    // Remap any old file:///... paths in HTML src attributes
                    val oldFileSrcRegex = Regex("""src=["']file:/*([^"']+)["']""", RegexOption.IGNORE_CASE)
                    body = oldFileSrcRegex.replace(body) { match ->
                        val filename = match.groupValues[1].substringAfterLast('/')
                        "src=\"https://local.review/review_images/$filename\""
                    }
                    val oldDataSrcRegex = Regex("""src=["']\/data\/[^"']*\/review_images\/([^"']+)["']""", RegexOption.IGNORE_CASE)
                    body = oldDataSrcRegex.replace(body) { match ->
                        val filename = match.groupValues[1]
                        "src=\"https://local.review/review_images/$filename\""
                    }
                    val oldPicturesDataSrcRegex = Regex("""src=["']\/data\/[^"']*\/pictures\/([^"']+)["']""", RegexOption.IGNORE_CASE)
                    body = oldPicturesDataSrcRegex.replace(body) { match ->
                        val filename = match.groupValues[1]
                        "src=\"https://local.review/review_images/$filename\""
                    }
                    val relativeImgRegex = Regex("""src=["'](?:review_images|pictures)\/([^"']+)["']""", RegexOption.IGNORE_CASE)
                    body = relativeImgRegex.replace(body) { match ->
                        val filename = match.groupValues[1]
                        "src=\"https://local.review/review_images/$filename\""
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
