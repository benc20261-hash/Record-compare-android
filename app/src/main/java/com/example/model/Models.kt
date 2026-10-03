package com.example.model

import org.json.JSONArray

data class PictureItem(
    val id: String,
    val name: String,
    val url: String
)

data class ProductItem(
    val id: String,
    val name: String,
    val brand: String,
    val tags: List<String> = emptyList(),
    val overview: String = "",
    val pictures: List<PictureItem> = emptyList(),
    val createdTime: String = ""
)

data class SpecificationItem(
    val id: String,
    val productId: String,
    val productTitle: String,
    val specName: String,
    val details: String,
    val unit: String? = null,
    val orderIndex: Int = 0,
    val pictures: List<PictureItem> = emptyList()
)

data class ReviewItem(
    val id: String,
    val reviewNumber: Int,
    val title: String,
    val body: String,
    val authorName: String,
    val rating: Int = 5,
    val productIds: List<String> = emptyList(),
    val productTitles: List<String> = emptyList(),
    val pictures: List<PictureItem> = emptyList(),
    val createdTime: String = ""
)

fun parsePicturesJson(json: String?): List<PictureItem> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<PictureItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                PictureItem(
                    id = obj.optString("id", "pic_$i"),
                    name = obj.optString("name", "Image"),
                    url = obj.optString("url", "")
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

fun parseStringArrayJson(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

fun detectDeviceCategory(title: String, brand: String, tags: List<String>): String {
    val query = "$title $brand ${tags.joinToString(" ")}".lowercase()
    return when {
        "mic" in query || "boya" in query || "dji" in query -> "mic"
        "glasses" in query || "viture" in query || "ar" in query || "xr" in query -> "glasses"
        "laptop" in query || "macbook" in query || "asus" in query || "pc" in query || "notebook" in query -> "laptop"
        "watch" in query || "wearable" in query -> "watch"
        "headphone" in query || "headset" in query || "earphone" in query || "wh-" in query -> "headphones"
        "phone" in query || "iphone" in query || "samsung" in query || "fold" in query || "galaxy" in query || "mobile" in query -> "phone"
        else -> "general"
    }
}

fun resolveImageModel(context: android.content.Context, url: String): Any {
    if (url.isBlank()) return url
    return try {
        val reviewImagesDir = java.io.File(context.filesDir, "review_images")
        val picturesDir = java.io.File(context.filesDir, "pictures")
        val cacheReviewImagesDir = java.io.File(context.cacheDir, "review_images")
        val cachePicturesDir = java.io.File(context.cacheDir, "pictures")

        // 1. Direct file path check if it's already an existing file
        if (url.startsWith("file://") || url.startsWith("/")) {
            val path = url.removePrefix("file://")
            val f = java.io.File(path)
            if (f.exists() && f.isFile) return f
        }

        // 2. Extract potential filename and clean names
        val rawFilename = url.substringAfterLast("/")
        val cleanFilename = if (rawFilename.contains("?")) rawFilename.substringBefore("?") else rawFilename
        val decodedFilename = try { java.net.URLDecoder.decode(cleanFilename, "UTF-8") } catch (_: Exception) { cleanFilename }

        val candidateFiles = listOf(
            java.io.File(reviewImagesDir, decodedFilename),
            java.io.File(reviewImagesDir, cleanFilename),
            java.io.File(picturesDir, decodedFilename),
            java.io.File(picturesDir, cleanFilename),
            java.io.File(cacheReviewImagesDir, decodedFilename),
            java.io.File(cacheReviewImagesDir, cleanFilename),
            java.io.File(cachePicturesDir, decodedFilename),
            java.io.File(cachePicturesDir, cleanFilename)
        )

        val existingDirect = candidateFiles.firstOrNull { it.exists() && it.isFile }
        if (existingDirect != null) return existingDirect

        // 3. Search directories for partial match or suffix match
        val cleanLower = cleanFilename.lowercase()
        val allDirs = listOf(reviewImagesDir, picturesDir, cacheReviewImagesDir, cachePicturesDir)
        for (dir in allDirs) {
            if (dir.exists() && dir.isDirectory) {
                val match = dir.listFiles()?.firstOrNull { file ->
                    val nameLower = file.name.lowercase()
                    nameLower == cleanLower ||
                    (cleanLower.isNotBlank() && nameLower.endsWith(cleanLower)) ||
                    (cleanLower.isNotBlank() && cleanLower.endsWith(nameLower))
                }
                if (match != null && match.isFile) return match
            }
        }

        // 4. Content URIs
        if (url.startsWith("content://")) {
            try {
                return android.net.Uri.parse(url)
            } catch (_: Exception) {}
        }

        url
    } catch (_: Exception) {
        url
    }
}
