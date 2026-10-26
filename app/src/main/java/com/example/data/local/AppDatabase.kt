package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

import androidx.room.migration.Migration

@Database(
    entities = [ProductEntity::class, SpecificationEntity::class, ReviewEntity::class, UserEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE specifications ADD COLUMN isNumeric INTEGER DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "productcompare.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                copyBundledAssets(context.applicationContext)
                                populateInitialData(context.applicationContext, getInstance(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                CoroutineScope(Dispatchers.IO).launch {
                    copyBundledAssets(context.applicationContext)
                }
                instance
            }
        }

        fun copyBundledAssets(context: Context) {
            try {
                val assetManager = context.assets
                val picturesDir = java.io.File(context.filesDir, "pictures").apply { mkdirs() }
                val reviewImagesDir = java.io.File(context.filesDir, "review_images").apply { mkdirs() }

                // 1. Copy bundled pictures
                try {
                    val picFiles = assetManager.list("pictures") ?: emptyArray()
                    for (filename in picFiles) {
                        val target = java.io.File(picturesDir, filename)
                        if (!target.exists() || target.length() == 0L) {
                            assetManager.open("pictures/$filename").use { input ->
                                target.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                    }
                } catch (_: Exception) {}

                // 2. Copy bundled review images
                try {
                    val reviewFiles = assetManager.list("review_images") ?: emptyArray()
                    for (filename in reviewFiles) {
                        val target = java.io.File(reviewImagesDir, filename)
                        if (!target.exists() || target.length() == 0L) {
                            assetManager.open("review_images/$filename").use { input ->
                                target.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                    }
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }

        suspend fun populateInitialData(context: Context, database: AppDatabase) {
            try {
                copyBundledAssets(context)
                val jsonString = context.assets.open("seed_database.json").bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                val dao = database.appDao()

                // Products
                val productsJson = root.optJSONArray("products")
                if (productsJson != null) {
                    val productList = mutableListOf<ProductEntity>()
                    for (i in 0 until productsJson.length()) {
                        val p = productsJson.getJSONObject(i)
                        val tagsArr = p.optJSONArray("tags")
                        val tagsList = mutableListOf<String>()
                        if (tagsArr != null) {
                            for (j in 0 until tagsArr.length()) {
                                tagsList.add(tagsArr.getString(j))
                            }
                        }
                        val picsArr = p.optJSONArray("pictures")
                        val picsJson = picsArr?.toString() ?: "[]"

                        productList.add(
                            ProductEntity(
                                id = p.getString("id"),
                                name = p.getString("name"),
                                brand = p.optString("brand", ""),
                                tags = tagsList.joinToString(","),
                                overview = p.optString("overview", ""),
                                picturesJson = picsJson,
                                createdTime = p.optString("createdTime", "")
                            )
                        )
                    }
                    dao.insertProducts(productList)
                }

                // Specifications
                val specsJson = root.optJSONArray("specifications")
                if (specsJson != null) {
                    val specList = mutableListOf<SpecificationEntity>()
                    for (i in 0 until specsJson.length()) {
                        val s = specsJson.getJSONObject(i)
                        val picsArr = s.optJSONArray("pictures")
                        val picsJson = picsArr?.toString() ?: "[]"

                        specList.add(
                            SpecificationEntity(
                                id = s.getString("id"),
                                productId = s.optString("productId", ""),
                                productTitle = s.optString("productTitle", ""),
                                specName = s.getString("specName"),
                                details = s.getString("details"),
                                unit = if (s.has("unit") && !s.isNull("unit")) s.getString("unit") else null,
                                orderIndex = s.optInt("order", i),
                                picturesJson = picsJson,
                                isNumeric = if (s.has("isNumeric") && !s.isNull("isNumeric")) s.getBoolean("isNumeric") else null
                            )
                        )
                    }
                    dao.insertSpecifications(specList)
                }

                // Reviews
                val reviewsJson = root.optJSONArray("reviews")
                if (reviewsJson != null) {
                    val reviewList = mutableListOf<ReviewEntity>()
                    for (i in 0 until reviewsJson.length()) {
                        val r = reviewsJson.getJSONObject(i)
                        val prodIdsArr = r.optJSONArray("productIds")
                        val prodTitlesArr = r.optJSONArray("productTitles")
                        val picsArr = r.optJSONArray("pictures")

                        reviewList.add(
                            ReviewEntity(
                                id = r.getString("id"),
                                reviewNumber = r.optInt("reviewNumber", i + 1),
                                title = r.optString("title", ""),
                                body = r.getString("body"),
                                authorName = r.optString("authorName", "Verified Reviewer"),
                                rating = r.optInt("rating", 5),
                                productIdsJson = prodIdsArr?.toString() ?: "[]",
                                productTitlesJson = prodTitlesArr?.toString() ?: "[]",
                                picturesJson = picsArr?.toString() ?: "[]",
                                createdTime = r.optString("createdTime", "")
                            )
                        )
                    }
                    dao.insertReviews(reviewList)
                }

                // Users
                val usersJson = root.optJSONArray("users")
                if (usersJson != null) {
                    val userList = mutableListOf<UserEntity>()
                    for (i in 0 until usersJson.length()) {
                        val u = usersJson.getJSONObject(i)
                        userList.add(
                            UserEntity(
                                id = u.getString("id"),
                                name = u.getString("name"),
                                email = u.getString("email"),
                                role = u.optString("role", "User")
                            )
                        )
                    }
                    dao.insertUsers(userList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
