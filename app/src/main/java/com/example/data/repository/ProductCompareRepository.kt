package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDao
import com.example.data.local.AppDatabase
import com.example.data.local.ProductEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.SpecificationEntity
import com.example.data.local.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ProductCompareRepository(
    private val context: Context,
    private val appDao: AppDao
) {
    val productsFlow: Flow<List<ProductEntity>> = appDao.getAllProducts()
    val specificationsFlow: Flow<List<SpecificationEntity>> = appDao.getAllSpecifications()
    val reviewsFlow: Flow<List<ReviewEntity>> = appDao.getAllReviews()
    val usersFlow: Flow<List<UserEntity>> = appDao.getAllUsers()

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val currentProducts = appDao.getAllProducts().first()
        if (currentProducts.isEmpty()) {
            val db = AppDatabase.getInstance(context)
            AppDatabase.populateInitialData(context, db)
        }
    }

    suspend fun resetToSeedData() = withContext(Dispatchers.IO) {
        appDao.clearProducts()
        appDao.clearSpecifications()
        appDao.clearReviews()
        appDao.clearUsers()
        val db = AppDatabase.getInstance(context)
        AppDatabase.populateInitialData(context, db)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        appDao.clearProducts()
        appDao.clearSpecifications()
        appDao.clearReviews()
    }

    suspend fun insertProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        appDao.insertProduct(product)
    }

    suspend fun deleteProduct(id: String) = withContext(Dispatchers.IO) {
        appDao.deleteProduct(id)
        appDao.deleteSpecsForProduct(id)
    }

    suspend fun insertSpecification(spec: SpecificationEntity) = withContext(Dispatchers.IO) {
        appDao.insertSpecification(spec)
    }

    suspend fun deleteSpecification(id: String) = withContext(Dispatchers.IO) {
        appDao.deleteSpecification(id)
    }

    suspend fun insertReview(review: ReviewEntity) = withContext(Dispatchers.IO) {
        appDao.insertReview(review)
    }

    private fun extractImageNamesFromPicturesJson(jsonStr: String): Set<String> {
        val set = mutableSetOf<String>()
        try {
            val arr = org.json.JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val url = obj.optString("url", "")
                if (url.isNotBlank()) {
                    set.add(url)
                    val fname = url.substringAfterLast("/")
                    if (fname.isNotBlank()) set.add(fname)
                }
            }
        } catch (_: Exception) {}
        return set
    }

    private fun extractImageNamesFromHtml(html: String): Set<String> {
        val set = mutableSetOf<String>()
        val regex = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        regex.findAll(html).forEach { match ->
            val src = match.groupValues[1]
            set.add(src)
            val fname = src.substringAfterLast("/")
            if (fname.isNotBlank()) set.add(fname)
        }
        return set
    }

    private suspend fun getAllActiveMediaReferences(excludeReviewId: String? = null): Set<String> {
        val active = mutableSetOf<String>()
        val products = appDao.getAllProducts().first()
        products.forEach { p ->
            active.addAll(extractImageNamesFromPicturesJson(p.picturesJson))
        }
        val specs = appDao.getAllSpecifications().first()
        specs.forEach { s ->
            active.addAll(extractImageNamesFromPicturesJson(s.picturesJson))
        }
        val reviews = appDao.getAllReviews().first()
        reviews.forEach { r ->
            if (excludeReviewId == null || r.id != excludeReviewId) {
                active.addAll(extractImageNamesFromPicturesJson(r.picturesJson))
                active.addAll(extractImageNamesFromHtml(r.body))
            }
        }
        return active
    }

    suspend fun deleteReview(id: String): Int = withContext(Dispatchers.IO) {
        val reviews = appDao.getAllReviews().first()
        val reviewToDelete = reviews.find { it.id == id }
        var deletedFilesCount = 0

        if (reviewToDelete != null) {
            val reviewImages = extractImageNamesFromPicturesJson(reviewToDelete.picturesJson) +
                    extractImageNamesFromHtml(reviewToDelete.body)
            val otherActive = getAllActiveMediaReferences(excludeReviewId = id)

            val candidateDirs = listOf(
                java.io.File(context.filesDir, "review_images"),
                java.io.File(context.filesDir, "pictures"),
                java.io.File(context.cacheDir, "review_images"),
                java.io.File(context.cacheDir, "pictures")
            )

            candidateDirs.forEach { dir ->
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        val isReferencedByReview = reviewImages.contains(file.name) ||
                                reviewImages.contains(file.absolutePath)
                        val isReferencedElsewhere = otherActive.contains(file.name) ||
                                otherActive.contains(file.absolutePath)

                        if (isReferencedByReview && !isReferencedElsewhere) {
                            if (file.delete()) {
                                deletedFilesCount++
                            }
                        }
                    }
                }
            }
        }

        appDao.deleteReview(id)
        deletedFilesCount
    }

    suspend fun freeUpOrphanStorage(): Int = withContext(Dispatchers.IO) {
        val activeMedia = getAllActiveMediaReferences()
        var deletedCount = 0

        val candidateDirs = listOf(
            java.io.File(context.filesDir, "review_images"),
            java.io.File(context.filesDir, "pictures"),
            java.io.File(context.cacheDir, "review_images"),
            java.io.File(context.cacheDir, "pictures")
        )

        candidateDirs.forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    val isReferenced = activeMedia.contains(file.name) ||
                            activeMedia.contains(file.absolutePath)
                    if (!isReferenced) {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
            }
        }
        deletedCount
    }

    fun getCachedMediaFilesCount(): Int {
        val candidateDirs = listOf(
            java.io.File(context.filesDir, "review_images"),
            java.io.File(context.filesDir, "pictures")
        )
        return candidateDirs.sumOf { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.count { it.isFile } ?: 0
            } else 0
        }
    }
}
