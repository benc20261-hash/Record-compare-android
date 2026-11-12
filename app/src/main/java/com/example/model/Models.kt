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
    val pictures: List<PictureItem> = emptyList(),
    val isNumeric: Boolean? = null
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

private val imageResolutionCache = java.util.concurrent.ConcurrentHashMap<String, Any>(256)
@Volatile private var cachedLocalFilesList: List<java.io.File>? = null
@Volatile private var cachedLocalFilesTimestamp: Long = 0L
@Volatile private var cachedAssetReviewImages: Set<String>? = null
@Volatile private var cachedAssetPictures: Set<String>? = null

fun resolveImageModel(context: android.content.Context, url: String, preferredName: String? = null): Any {
    val cleanUrl = url.trim()
    val cleanPreferred = preferredName?.trim()?.ifBlank { null }
    if (cleanUrl.isBlank() && cleanPreferred.isNullOrBlank()) return ""

    val cacheKey = "$cleanUrl|$cleanPreferred"
    imageResolutionCache[cacheKey]?.let { return it }

    val resolved: Any = try {
        val reviewImagesDir = java.io.File(context.filesDir, "review_images")
        val picturesDir = java.io.File(context.filesDir, "pictures")
        val cacheReviewImagesDir = java.io.File(context.cacheDir, "review_images")
        val cachePicturesDir = java.io.File(context.cacheDir, "pictures")
        val allDirs = listOf(reviewImagesDir, picturesDir, cacheReviewImagesDir, cachePicturesDir)

        // 1. Direct file path check if it's already an existing file
        if (cleanUrl.startsWith("file://") || cleanUrl.startsWith("/")) {
            val path = cleanUrl.removePrefix("file://")
            val f = java.io.File(path)
            if (f.exists() && f.isFile) return f.also { imageResolutionCache[cacheKey] = it }
        }

        // 2. Extract potential filename from disposition or URL path
        val decodedUrl = try { java.net.URLDecoder.decode(cleanUrl, "UTF-8") } catch (_: Exception) { cleanUrl }
        val dispositionMatch = Regex("""filename(?:\*=[^'"]*''|=["']?)([^"'&;\s]+)""", RegexOption.IGNORE_CASE).find(decodedUrl)
        val extractedDispositionFilename = dispositionMatch?.groupValues?.get(1)

        val rawFilename = cleanUrl.substringAfterLast("/")
        val cleanPathFilename = if (rawFilename.contains("?")) rawFilename.substringBefore("?") else rawFilename
        val decodedPathFilename = try { java.net.URLDecoder.decode(cleanPathFilename, "UTF-8") } catch (_: Exception) { cleanPathFilename }

        val candidateNames = mutableListOf<String>()
        if (!cleanPreferred.isNullOrBlank()) candidateNames.add(cleanPreferred)
        if (!extractedDispositionFilename.isNullOrBlank()) candidateNames.add(extractedDispositionFilename)
        if (decodedPathFilename.isNotBlank() && !decodedPathFilename.startsWith("blob:") && decodedPathFilename != cleanUrl) {
            candidateNames.add(decodedPathFilename)
        }
        if (cleanPathFilename.isNotBlank() && !cleanPathFilename.startsWith("blob:") && cleanPathFilename != cleanUrl) {
            candidateNames.add(cleanPathFilename)
        }

        var foundMatch: Any? = null

        // 3. Exact candidates search in dirs and cached assets
        for (cand in candidateNames) {
            val candidateFiles = listOf(
                java.io.File(reviewImagesDir, cand),
                java.io.File(picturesDir, cand),
                java.io.File(cacheReviewImagesDir, cand),
                java.io.File(cachePicturesDir, cand)
            )
            val match = candidateFiles.firstOrNull { it.exists() && it.isFile }
            if (match != null) {
                foundMatch = match
                break
            }

            // Check cached assets list (cached once in memory to avoid repeated asset manager disk calls)
            val candLower = cand.lowercase()
            val revAssets = cachedAssetReviewImages ?: try {
                context.assets.list("review_images")?.toSet() ?: emptySet()
            } catch (_: Exception) { emptySet() }.also { cachedAssetReviewImages = it }
            val foundRev = revAssets.firstOrNull { it.equals(cand, ignoreCase = true) }
            if (foundRev != null) {
                foundMatch = "file:///android_asset/review_images/$foundRev"
                break
            }

            val picAssets = cachedAssetPictures ?: try {
                context.assets.list("pictures")?.toSet() ?: emptySet()
            } catch (_: Exception) { emptySet() }.also { cachedAssetPictures = it }
            val foundPic = picAssets.firstOrNull { it.equals(cand, ignoreCase = true) }
            if (foundPic != null) {
                foundMatch = "file:///android_asset/pictures/$foundPic"
                break
            }
        }

        if (foundMatch != null) {
            foundMatch
        } else {
            // 4. Fuzzy search directories using cached directory files list (refreshed at most once every 10 seconds)
            val now = System.currentTimeMillis()
            val localFiles = if (cachedLocalFilesList != null && now - cachedLocalFilesTimestamp < 10000L) {
                cachedLocalFilesList!!
            } else {
                val list = mutableListOf<java.io.File>()
                for (dir in allDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        val fArray = dir.listFiles()
                        if (fArray != null) list.addAll(fArray)
                    }
                }
                cachedLocalFilesTimestamp = now
                cachedLocalFilesList = list
                list
            }

            var fuzzyMatch: java.io.File? = null
            for (cand in candidateNames) {
                val candLower = cand.lowercase()
                fuzzyMatch = localFiles.firstOrNull { file ->
                    val nameLower = file.name.lowercase()
                    val nameWithoutExt = file.nameWithoutExtension.lowercase()
                    nameLower == candLower ||
                    nameWithoutExt == candLower ||
                    (candLower.isNotBlank() && (nameLower.startsWith("${candLower}.") || nameLower.startsWith("${candLower}_"))) ||
                    (candLower.isNotBlank() && nameLower.startsWith(candLower)) ||
                    (candLower.isNotBlank() && nameLower.endsWith(candLower)) ||
                    (candLower.isNotBlank() && nameLower.contains(candLower)) ||
                    (nameWithoutExt.isNotBlank() && candLower.contains(nameWithoutExt))
                }
                if (fuzzyMatch != null && fuzzyMatch.isFile) break
            }

            if (fuzzyMatch != null && fuzzyMatch.isFile) {
                fuzzyMatch
            } else if (cleanUrl.startsWith("content://")) {
                try {
                    val uri = android.net.Uri.parse(cleanUrl)
                    val hash = Math.abs(cleanUrl.hashCode()).toString()
                    val cachedFile = java.io.File(picturesDir, "cached_$hash.jpg")
                    if (cachedFile.exists() && cachedFile.length() > 0) {
                        cachedFile
                    } else {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                if (!picturesDir.exists()) picturesDir.mkdirs()
                                cachedFile.outputStream().use { out -> input.copyTo(out) }
                            }
                            if (cachedFile.exists() && cachedFile.length() > 0) {
                                cachedFile
                            } else {
                                uri
                            }
                        } catch (_: Exception) {
                            uri
                        }
                    }
                } catch (_: Exception) {
                    if (cleanUrl.isNotBlank() && !cleanUrl.startsWith("blob:")) cleanUrl else (cleanPreferred ?: "")
                }
            } else if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://") || cleanUrl.startsWith("data:")) {
                cleanUrl
            } else {
                if (cleanUrl.isNotBlank() && !cleanUrl.startsWith("blob:")) cleanUrl else (cleanPreferred ?: "")
            }
        }
    } catch (_: Exception) {
        if (cleanUrl.isNotBlank() && !cleanUrl.startsWith("blob:")) cleanUrl else (cleanPreferred ?: "")
    }

    imageResolutionCache[cacheKey] = resolved
    return resolved
}

fun copyPickedUriToPermanentStorage(
    context: android.content.Context,
    uri: android.net.Uri,
    prefix: String = "pic"
): PictureItem {
    return try {
        val picturesDir = java.io.File(context.filesDir, "pictures")
        if (!picturesDir.exists()) picturesDir.mkdirs()
        val filename = "${prefix}_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.jpg"
        val destFile = java.io.File(picturesDir, filename)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { out -> input.copyTo(out) }
        }
        val reviewImagesDir = java.io.File(context.filesDir, "review_images")
        if (!reviewImagesDir.exists()) reviewImagesDir.mkdirs()
        try { destFile.copyTo(java.io.File(reviewImagesDir, filename), overwrite = true) } catch (_: Exception) {}

        PictureItem(
            id = "${prefix}_${System.currentTimeMillis()}",
            name = filename,
            url = "file://${destFile.absolutePath}"
        )
    } catch (_: Exception) {
        PictureItem(
            id = "${prefix}_${System.currentTimeMillis()}",
            name = "Photo ${System.currentTimeMillis().toString().takeLast(4)}",
            url = uri.toString()
        )
    }
}

