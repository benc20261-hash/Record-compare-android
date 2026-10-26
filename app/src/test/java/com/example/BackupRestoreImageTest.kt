package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupArchiveManager
import com.example.data.local.AppDatabase
import com.example.model.resolveImageModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRestoreImageTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testWebappZipRestore_normalizesAllReviewImages() = runBlocking {
        // Build mock Productc webapp ZIP
        val zipFile = File(context.cacheDir, "test_webapp_backup.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // 1. Add images/att_import_test1.png
            zos.putNextEntry(ZipEntry("images/att_import_test1.png"))
            zos.write("fake_png_data_1".toByteArray())
            zos.closeEntry()

            // 2. Add images/att_rel_test2.jpg
            zos.putNextEntry(ZipEntry("images/att_rel_test2.jpg"))
            zos.write("fake_jpg_data_2".toByteArray())
            zos.closeEntry()

            // 3. Add database.json with products and reviews containing diverse image references
            val dbJson = JSONObject().apply {
                put("products", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "prod_test_1")
                        put("name", "Test Smartphone")
                        put("brand", "Acme")
                        put("tags", JSONArray().apply { put("flagship") })
                        put("overview", "Smartphone overview")
                        put("pictures", JSONArray().apply {
                            put(JSONObject().apply {
                                put("id", "att_import_test1")
                                put("name", "Phone Photo")
                                put("url", "blob:http://localhost:5173/5c0d12e6")
                            })
                        })
                    })
                })
                put("specifications", JSONArray())
                put("reviews", JSONArray().apply {
                    // Review 1: data-attachment-id with empty src (from localStorage sanitize)
                    put(JSONObject().apply {
                        put("id", "rev_1")
                        put("title", "Review with data-attachment-id")
                        put("body", "<p>Great camera!</p><p><img src=\"\" data-attachment-id=\"att_import_test1\" alt=\"Camera Shot\"></p>")
                        put("pictures", JSONArray().apply {
                            put(JSONObject().apply {
                                put("id", "att_import_test1")
                                put("name", "Camera Shot")
                                put("url", "")
                            })
                        })
                    })
                    // Review 2: relative path images/att_rel_test2.jpg
                    put(JSONObject().apply {
                        put("id", "rev_2")
                        put("title", "Review with relative images/ path")
                        put("body", "<p>Battery performance review.</p><p><img src=\"images/att_rel_test2.jpg\" alt=\"Battery Graph\"></p>")
                        put("pictures", JSONArray())
                    })
                    // Review 3: blob URL with data-attachment-id
                    put(JSONObject().apply {
                        put("id", "rev_3")
                        put("title", "Review with blob URL")
                        put("body", "<p>Display test.</p><p><img src=\"blob:http://localhost:5173/blob-guid-456\" data-attachment-id=\"att_import_test1\" alt=\"Display Image\"></p>")
                        put("pictures", JSONArray())
                    })
                    // Review 4: Markdown image syntax ![Caption](images/att_rel_test2.jpg)
                    put(JSONObject().apply {
                        put("id", "rev_4")
                        put("title", "Review with markdown image")
                        put("body", "# Summary\n\n![Benchmark Chart](images/att_rel_test2.jpg)\n\nFast speeds.")
                        put("pictures", JSONArray())
                    })
                })
            }

            zos.putNextEntry(ZipEntry("database.json"))
            zos.write(dbJson.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        // Execute restore
        val uri = Uri.fromFile(zipFile)
        val result = BackupArchiveManager.restoreFromZipUri(context, uri, database)

        assertTrue("Restore should succeed: ${result.message}", result.success)
        assertEquals(4, result.restoredReviews)

        val reviews = database.appDao().getAllReviews().first()
        assertEquals(4, reviews.size)

        // Verify Review 1: empty src with data-attachment-id normalized to local review URL
        val r1 = reviews.find { it.id == "rev_1" }!!
        assertTrue("Review 1 body should contain local.review URL: ${r1.body}", r1.body.contains("https://local.review/review_images/att_import_test1.png"))
        assertTrue("Review 1 picturesJson should have local file: ${r1.picturesJson}", r1.picturesJson.contains("att_import_test1.png"))

        // Verify Review 2: images/ path normalized to local.review
        val r2 = reviews.find { it.id == "rev_2" }!!
        assertTrue("Review 2 body should contain local.review URL: ${r2.body}", r2.body.contains("https://local.review/review_images/att_rel_test2.jpg"))
        assertTrue("Review 2 picturesJson should have extracted image: ${r2.picturesJson}", r2.picturesJson.contains("att_rel_test2.jpg"))

        // Verify Review 3: blob: replaced by local.review
        val r3 = reviews.find { it.id == "rev_3" }!!
        assertFalse("Review 3 body should not contain localhost blob: ${r3.body}", r3.body.contains("blob:http"))
        assertTrue("Review 3 body should contain local.review URL: ${r3.body}", r3.body.contains("https://local.review/review_images/att_import_test1.png"))

        // Verify Review 4: markdown image converted to rich figure with local URL
        val r4 = reviews.find { it.id == "rev_4" }!!
        assertTrue("Review 4 body should contain figure or img: ${r4.body}", r4.body.contains("https://local.review/review_images/att_rel_test2.jpg"))

        // Verify resolveImageModel resolves both by URL and by ID
        val modelFromUrl = resolveImageModel(context, "https://local.review/review_images/att_import_test1.png")
        assertTrue("resolveImageModel should resolve file: $modelFromUrl", modelFromUrl is File && modelFromUrl.exists())

        val modelFromId = resolveImageModel(context, "", preferredName = "att_import_test1")
        assertTrue("resolveImageModel should resolve by ID: $modelFromId", modelFromId is File && modelFromId.exists())
    }
}
