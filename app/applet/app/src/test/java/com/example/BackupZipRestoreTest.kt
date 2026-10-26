package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupArchiveManager
import com.example.data.local.AppDatabase
import com.example.model.PictureItem
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.SpecificationItem
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
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupZipRestoreTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
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
    fun testRestoreFromWebappZipArchive() = runBlocking {
        // 1. Create a simulated Productc Webapp backup ZIP archive
        val zipFile = File(context.cacheDir, "test_webapp_backup.zip")
        if (zipFile.exists()) zipFile.delete()

        val manifestJson = JSONObject().apply {
            put("version", "3.0.0-localapp")
            put("exportedAt", "2026-10-16T12:00:00.000Z")
            put("appName", "ProductCompare Hub")
            put("productCount", 1)
            put("specificationCount", 1)
            put("reviewCount", 1)
            put("mediaFileCount", 1)
        }.toString(2)

        val databaseJson = JSONObject().apply {
            put("products", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "prod_webapp_1")
                    put("name", "Plaud Note AI")
                    put("brand", "Plaud")
                    put("tags", JSONArray(listOf("Voice Recorder", "AI Note")))
                    put("overview", "Ultra-thin AI Voice Recorder with ChatGPT transcription.")
                    put("pictures", JSONArray().apply {
                        put(JSONObject().apply {
                            put("id", "att_plaud_1")
                            put("name", "plaud_front.jpg")
                            put("url", "https://firebasestorage.googleapis.com/v0/b/app/o/images%2Fatt_plaud_1.jpg?alt=media")
                        })
                    })
                    put("createdTime", "2026-10-16T10:00:00Z")
                })
            })
            put("specifications", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "spec_webapp_1")
                    put("productId", "prod_webapp_1")
                    put("productTitle", "Plaud Note AI")
                    put("specName", "Battery Life")
                    put("details", "30 hours continuous recording")
                    put("unit", "hours")
                    put("order", 1)
                    put("pictures", JSONArray())
                })
            })
            put("reviews", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "rev_webapp_1")
                    put("reviewNumber", 1)
                    put("title", "Great Recording Hardware")
                    put("body", "<p>Check out the device photo below:</p><p><img src=\"images/att_plaud_1.jpg\" data-attachment-id=\"att_plaud_1\" alt=\"Plaud Photo\" /></p>")
                    put("authorName", "Alex")
                    put("rating", 5)
                    put("productIds", JSONArray(listOf("prod_webapp_1")))
                    put("productTitles", JSONArray(listOf("Plaud Note AI")))
                    put("pictures", JSONArray().apply {
                        put(JSONObject().apply {
                            put("id", "att_plaud_1")
                            put("name", "plaud_front.jpg")
                            put("url", "")
                        })
                    })
                    put("createdTime", "2026-10-16T11:00:00Z")
                })
            })
            put("users", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "user_1")
                    put("name", "Admin")
                    put("email", "admin@local.app")
                    put("role", "Admin")
                })
            })
        }.toString(2)

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // manifest.json
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // database.json
            zos.putNextEntry(ZipEntry("database.json"))
            zos.write(databaseJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // README.txt
            zos.putNextEntry(ZipEntry("README.txt"))
            zos.write("ProductCompare Webapp Backup".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // images/att_plaud_1.jpg
            zos.putNextEntry(ZipEntry("images/att_plaud_1.jpg"))
            zos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())) // JPEG header bytes
            zos.closeEntry()
        }

        // 2. Restore the webapp ZIP using BackupArchiveManager
        val result = BackupArchiveManager.tryRestoreFromZip(context, zipFile, database)
        assertTrue("Restore should succeed", result.success)
        assertEquals(1, result.restoredProducts)
        assertEquals(1, result.restoredSpecs)
        assertEquals(1, result.restoredReviews)
        assertTrue(result.restoredMedia >= 1)

        // 3. Verify Database entities and remapped local files
        val products = database.appDao().getAllProducts().first()
        assertEquals(1, products.size)
        val product = products[0]
        assertEquals("prod_webapp_1", product.id)
        assertEquals("Voice Recorder,AI Note", product.tags)
        assertTrue("Picture should be remapped to local file", product.picturesJson.contains("att_plaud_1.jpg"))
        assertTrue("Picture should use file:// schema", product.picturesJson.contains("file://"))

        val reviews = database.appDao().getAllReviews().first()
        assertEquals(1, reviews.size)
        val review = reviews[0]
        assertEquals("rev_webapp_1", review.id)
        assertTrue("Review body should remap images/ to local.review", review.body.contains("https://local.review/review_images/att_plaud_1.jpg"))

        // Cleanup
        zipFile.delete()
    }

    @Test
    fun testTwoWayExportCreatesWebappCompatibleZip() = runBlocking {
        val products = listOf(
            ProductItem(
                id = "p1",
                name = "Galaxy S24 Ultra",
                brand = "Samsung",
                tags = listOf("Flagship", "AMOLED"),
                overview = "Top tier phone",
                pictures = listOf(PictureItem(id = "att_s24", name = "s24.jpg", url = "file://${context.filesDir}/pictures/s24.jpg")),
                createdTime = "2026-10-16"
            )
        )
        val specs = listOf(
            SpecificationItem(
                id = "s1",
                productId = "p1",
                productTitle = "Galaxy S24 Ultra",
                specName = "Screen",
                details = "6.8 inch 120Hz",
                unit = "inches",
                orderIndex = 1,
                pictures = emptyList()
            )
        )
        val reviews = listOf(
            ReviewItem(
                id = "r1",
                reviewNumber = 1,
                title = "Excellent display",
                body = "<p>Super bright display.</p>",
                authorName = "Jordan",
                rating = 5,
                productIds = listOf("p1"),
                productTitles = listOf("Galaxy S24 Ultra"),
                pictures = emptyList(),
                createdTime = "2026-10-16"
            )
        )

        val zipFile = BackupArchiveManager.createFullZipBackup(
            context = context,
            products = products,
            specifications = specs,
            reviews = reviews,
            users = emptyList()
        )

        assertTrue(zipFile.exists())
        assertTrue(zipFile.length() > 0)

        // Inspect entries in created ZIP
        val zip = ZipFile(zipFile)
        val entryNames = zip.entries().asSequence().map { it.name }.toList()

        assertTrue("Must contain database.json", entryNames.contains("database.json"))
        assertTrue("Must contain manifest.json", entryNames.contains("manifest.json"))
        assertTrue("Must contain README.txt", entryNames.contains("README.txt"))

        // Read manifest.json and verify 3.0.0-localapp version
        val manifestEntry = zip.getEntry("manifest.json")
        assertNotNull(manifestEntry)
        val manifestContent = zip.getInputStream(manifestEntry).bufferedReader().readText()
        val manifestObj = JSONObject(manifestContent)
        assertEquals("3.0.0-localapp", manifestObj.getString("version"))
        assertEquals("ProductCompare Hub", manifestObj.getString("appName"))
        assertEquals(1, manifestObj.getInt("productCount"))

        zip.close()
        zipFile.delete()
    }
}
