package com.example.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.example.model.PictureItem
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.ui.util.UriHelper
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val webViewImageFileCache = java.util.concurrent.ConcurrentHashMap<String, File>(256)
private val webViewImageByteCache = object : LruCache<String, ByteArray>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ByteArray): Int = value.size
}
@Volatile private var cachedViewerLocalFiles: List<File>? = null
@Volatile private var cachedViewerFilesTimestamp: Long = 0L

private fun indexDirectoryFiles(dir: File) {
    if (dir.exists() && dir.isDirectory) {
        dir.listFiles()?.forEach { f ->
            if (f.isFile) {
                webViewImageFileCache[f.name] = f
                webViewImageFileCache[f.name.lowercase()] = f
                webViewImageFileCache[f.nameWithoutExtension.lowercase()] = f
                try {
                    val decoded = URLDecoder.decode(f.name, "UTF-8")
                    webViewImageFileCache[decoded] = f
                    webViewImageFileCache[decoded.lowercase()] = f
                } catch (_: Exception) {}
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RichReviewViewer(
    review: ReviewItem,
    products: List<ProductItem>,
    onClose: () -> Unit,
    onEditReview: (ReviewItem) -> Unit,
    onDeleteReview: (String) -> Unit,
    onViewProduct: ((ProductItem) -> Unit)? = null
) {
    BackHandler { onClose() }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var lightboxImages by remember { mutableStateOf<List<PictureItem>?>(null) }
    var lightboxInitialIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // Find linked product
    val linkedProduct = remember(review, products) {
        products.find { it.id in review.productIds }
            ?: products.find { it.name in review.productTitles }
            ?: products.firstOrNull()
    }

    val displayTitle = review.title.ifBlank {
        if (linkedProduct != null) "${linkedProduct.name} Review" else "Review Report"
    }

    // Format human-readable date (handles epoch milliseconds or ISO timestamps like "2026-10-25T01:19:48.906Z")
    val formattedDate = remember(review.createdTime) {
        if (review.createdTime.isBlank()) {
            "Recent"
        } else {
            formatBeautifulDate(review.createdTime)
        }
    }

    // Extract images from review.pictures or <img> tags in review.body
    val allReviewPictures = remember(review) {
        val list = mutableListOf<PictureItem>()
        if (review.pictures.isNotEmpty()) {
            list.addAll(review.pictures)
        }
        val imgRegex = Regex("""<img\b([^>]*)>""", RegexOption.IGNORE_CASE)
        imgRegex.findAll(review.body).forEachIndexed { idx, match ->
            val attrs = match.groupValues[1]
            val src = Regex("""src=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1)?.trim() ?: ""
            val attId = Regex("""(?:data-attachment-id|dataAttachmentId|data-attachment-token)=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1)?.trim() ?: ""
            val alt = Regex("""alt=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1)?.trim() ?: "Photo ${list.size + 1}"
            val effectiveUrl = when {
                src.isNotBlank() && !src.startsWith("blob:") -> src
                attId.isNotBlank() -> "https://local.review/review_images/$attId"
                else -> ""
            }
            if (effectiveUrl.isNotBlank() && list.none { it.url == effectiveUrl || (attId.isNotBlank() && it.id == attId) }) {
                list.add(PictureItem(id = attId.ifBlank { "body_img_$idx" }, name = alt, url = effectiveUrl))
            }
        }
        val mdImgRegex = Regex("""!\[(.*?)\]\((.*?)\)""")
        mdImgRegex.findAll(review.body).forEachIndexed { idx, match ->
            val alt = match.groupValues[1].ifBlank { "Photo ${list.size + 1}" }
            val url = match.groupValues[2].trim()
            if (url.isNotBlank() && list.none { it.url == url }) {
                list.add(PictureItem(id = "md_img_$idx", name = alt, url = url))
            }
        }
        list
    }

    // Zoom scaling state (Word-style zoom: default 85% for compact mobile comfort)
    var zoomPercent by remember { mutableIntStateOf(85) }
    var showZoomMenu by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isPageLoading by remember { mutableStateOf(true) }
    var renderProcessCrashed by remember(review.id) { mutableStateOf(false) }
    var webViewReloadKey by remember(review.id) { mutableIntStateOf(0) }

    // Build the complete unified scrollable HTML document (Header Card + Full Article Body)
    val htmlDocument = remember(review, displayTitle, linkedProduct, formattedDate, allReviewPictures) {
        buildViewerHtml(
            review = review,
            displayTitle = displayTitle,
            linkedProduct = linkedProduct,
            formattedDate = formattedDate,
            allPictures = allReviewPictures
        )
    }

    LaunchedEffect(zoomPercent) {
        try {
            webViewInstance?.settings?.textZoom = zoomPercent
            webViewInstance?.evaluateJavascript("if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("rich_review_viewer_screen"),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Fixed Top Header Bar matching Screenshot (compact & responsive)
                Surface(
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button and Title / Subtitle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF1E293B),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(end = 6.dp)) {
                                Text(
                                    text = displayTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Review #${review.reviewNumber} • ${linkedProduct?.name ?: "Community Review"}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Actions Row: Zoom Pill, Edit Button (Dark Pill) & Delete Button (Trash Icon)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Zoom Control Pill: [- 85% +] with Preset Dropdown Menu
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            zoomPercent = (zoomPercent - 10).coerceAtLeast(50)
                                            webViewInstance?.let { wv ->
                                                wv.settings.textZoom = zoomPercent
                                                wv.evaluateJavascript("if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
                                            }
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Zoom out",
                                            modifier = Modifier.size(13.dp),
                                            tint = Color(0xFF334155)
                                        )
                                    }

                                    Box {
                                        Text(
                                            text = "${zoomPercent}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier
                                                .clickable { showZoomMenu = true }
                                                .padding(horizontal = 3.dp, vertical = 2.dp)
                                        )
                                        DropdownMenu(
                                            expanded = showZoomMenu,
                                            onDismissRequest = { showZoomMenu = false }
                                        ) {
                                            listOf(50, 65, 75, 85, 100, 115, 130, 150, 200).forEach { preset ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = if (preset == 85) "$preset% (Default)" else "$preset%",
                                                            fontWeight = if (preset == zoomPercent) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 12.sp,
                                                            color = if (preset == zoomPercent) Color(0xFF4F46E5) else Color(0xFF1E293B)
                                                        )
                                                    },
                                                    onClick = {
                                                        zoomPercent = preset
                                                        webViewInstance?.let { wv ->
                                                            wv.settings.textZoom = zoomPercent
                                                            wv.evaluateJavascript("if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
                                                        }
                                                        showZoomMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            zoomPercent = (zoomPercent + 10).coerceAtMost(200)
                                            webViewInstance?.let { wv ->
                                                wv.settings.textZoom = zoomPercent
                                                wv.evaluateJavascript("if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
                                            }
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Zoom in",
                                            modifier = Modifier.size(13.dp),
                                            tint = Color(0xFF334155)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier
                                    .clickable { onEditReview(review) }
                                    .testTag("viewer_edit_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Edit",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.size(34.dp).testTag("viewer_delete_btn")
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Non-blocking top progress bar while assets stream in the background
                if (isPageLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = Color(0xFF6366F1),
                        trackColor = Color(0xFFEEF2FF)
                    )
                }

                // 100% UNIFIED SCROLL VIEW (Smooth vertical scrolling on mobile phones)
                if (renderProcessCrashed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(36.dp))
                                Text("Note View Recovered", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                Text(
                                    "System memory limit reached while rendering note. Tap below to reload note content.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        renderProcessCrashed = false
                                        webViewReloadKey++
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Reload Note", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    key(webViewReloadKey) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewInstance = this
                                tag = htmlDocument
                                // Prevent GPU MESA crash in virtualized container
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.loadsImagesAutomatically = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = false // Match mobile screen width exactly
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.textZoom = zoomPercent
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.allowFileAccessFromFileURLs = true
                                settings.allowUniversalAccessFromFileURLs = true
                                settings.defaultTextEncodingName = "UTF-8"
                                settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                                isVerticalScrollBarEnabled = true
                                isNestedScrollingEnabled = false
                                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

                                // Ensure touch gestures directly scroll the WebView without Compose parent interference
                                setOnTouchListener { v, _ ->
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                    false
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                        mainHandler.post {
                                            renderProcessCrashed = true
                                        }
                                        try {
                                            (view?.parent as? ViewGroup)?.removeView(view)
                                            view?.destroy()
                                        } catch (_: Exception) {}
                                        return true
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        val url = request.url?.toString() ?: return false
                                        if (UriHelper.isAppSchemeUrl(url) || url.startsWith("http://") || url.startsWith("https://")) {
                                            UriHelper.openAppUri(view.context, url)
                                            return true
                                        }
                                        return false
                                    }

                                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                        val reqUrl = request.url ?: return null
                                        val host = reqUrl.host?.lowercase() ?: ""
                                        val path = reqUrl.path ?: ""
                                        val urlStr = reqUrl.toString()

                                        if (host == "local.review" || host == "appassets.local" || path.contains("review_images") || path.contains("pictures") || path.contains("images") || urlStr.contains("review_images") || urlStr.contains("pictures") || urlStr.contains("/images/")) {
                                            try {
                                                val pathParam = reqUrl.getQueryParameter("path")
                                                val filename = reqUrl.lastPathSegment ?: ""
                                                val decodedFilename = try { URLDecoder.decode(filename, "UTF-8") } catch (_: Exception) { filename }

                                                // 1. Fast in-memory cache check (0ms, 0 disk I/O)
                                                var targetFile: File? = webViewImageFileCache[filename] 
                                                    ?: webViewImageFileCache[decodedFilename]
                                                    ?: webViewImageFileCache[filename.lowercase()]
                                                    ?: webViewImageFileCache[decodedFilename.lowercase()]

                                                if (targetFile == null || !targetFile.exists()) {
                                                    if (!pathParam.isNullOrBlank()) {
                                                        val decodedPath = try { URLDecoder.decode(pathParam, "UTF-8") } catch (_: Exception) { pathParam }
                                                        val f = File(decodedPath)
                                                        if (f.exists() && f.isFile) targetFile = f
                                                    }
                                                    if (targetFile == null || !targetFile.exists()) {
                                                        val f = File(ctx.filesDir, "review_images/$filename")
                                                        if (f.exists() && f.isFile) targetFile = f
                                                    }
                                                    if (targetFile == null || !targetFile.exists()) {
                                                        val f = File(ctx.filesDir, "review_images/$decodedFilename")
                                                        if (f.exists() && f.isFile) targetFile = f
                                                    }
                                                    if (targetFile == null || !targetFile.exists()) {
                                                        val f = File(ctx.filesDir, "pictures/$filename")
                                                        if (f.exists() && f.isFile) targetFile = f
                                                    }
                                                    if (targetFile == null || !targetFile.exists()) {
                                                        val f = File(ctx.filesDir, "pictures/$decodedFilename")
                                                        if (f.exists() && f.isFile) targetFile = f
                                                    }
                                                    if (targetFile == null || !targetFile.exists()) {
                                                        val f = File(ctx.cacheDir, "review_images/$filename")
                                                        if (f.exists() && f.isFile) targetFile = f
                                                    }
                                                    if (targetFile == null || !targetFile.exists()) {
                                                        val f = File(ctx.cacheDir, "pictures/$filename")
                                                        if (f.exists() && f.isFile) targetFile = f
                                                    }

                                                    if (targetFile == null || !targetFile.exists()) {
                                                        val now = System.currentTimeMillis()
                                                        if (cachedViewerLocalFiles == null || now - cachedViewerFilesTimestamp >= 15000L) {
                                                            indexDirectoryFiles(File(ctx.filesDir, "review_images"))
                                                            indexDirectoryFiles(File(ctx.filesDir, "pictures"))
                                                            indexDirectoryFiles(File(ctx.cacheDir, "review_images"))
                                                            indexDirectoryFiles(File(ctx.cacheDir, "pictures"))
                                                            cachedViewerFilesTimestamp = now
                                                            cachedViewerLocalFiles = emptyList()
                                                        }
                                                        targetFile = webViewImageFileCache[filename]
                                                            ?: webViewImageFileCache[decodedFilename]
                                                            ?: webViewImageFileCache[filename.lowercase()]
                                                            ?: webViewImageFileCache[decodedFilename.lowercase()]
                                                    }

                                                    if (targetFile != null && targetFile.exists() && targetFile.isFile) {
                                                        webViewImageFileCache[filename] = targetFile
                                                        webViewImageFileCache[decodedFilename] = targetFile
                                                        webViewImageFileCache[filename.lowercase()] = targetFile
                                                        webViewImageFileCache[decodedFilename.lowercase()] = targetFile
                                                    }
                                                }

                                                if (targetFile != null && targetFile.exists() && targetFile.isFile) {
                                                    val ext = targetFile.extension.lowercase()
                                                    val mime = when (ext) {
                                                        "png" -> "image/png"
                                                        "webp" -> "image/webp"
                                                        "gif" -> "image/gif"
                                                        "svg" -> "image/svg+xml"
                                                        else -> "image/jpeg"
                                                    }
                                                    val headers = mapOf(
                                                        "Access-Control-Allow-Origin" to "*",
                                                        "Access-Control-Allow-Methods" to "GET",
                                                        "Cache-Control" to "public, max-age=604800, immutable"
                                                    )

                                                    val cacheKey = "${targetFile.absolutePath}_${targetFile.lastModified()}"
                                                    val cachedBytes = webViewImageByteCache.get(cacheKey)
                                                    if (cachedBytes != null) {
                                                        return WebResourceResponse(mime, null, 200, "OK", headers, ByteArrayInputStream(cachedBytes))
                                                    }

                                                    // Downsample if file is large (> 200KB or > 1200px) for smooth 60fps mobile display
                                                    if ((ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp") && targetFile.length() > 200 * 1024) {
                                                        try {
                                                            val boundsOpt = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                                            BitmapFactory.decodeFile(targetFile.absolutePath, boundsOpt)
                                                            val w = boundsOpt.outWidth
                                                            val h = boundsOpt.outHeight
                                                            if (w > 1200 || h > 1200 || targetFile.length() > 500 * 1024) {
                                                                var inSampleSize = 1
                                                                val maxDim = maxOf(w, h)
                                                                while (maxDim / (inSampleSize * 2) >= 900) {
                                                                    inSampleSize *= 2
                                                                }
                                                                val decodeOpt = BitmapFactory.Options().apply {
                                                                    this.inSampleSize = inSampleSize
                                                                    this.inPreferredConfig = Bitmap.Config.RGB_565
                                                                }
                                                                val bmp = BitmapFactory.decodeFile(targetFile.absolutePath, decodeOpt)
                                                                if (bmp != null) {
                                                                    val baos = ByteArrayOutputStream()
                                                                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                                                                    bmp.recycle()
                                                                    val downsampledBytes = baos.toByteArray()
                                                                    webViewImageByteCache.put(cacheKey, downsampledBytes)
                                                                    return WebResourceResponse("image/jpeg", null, 200, "OK", headers, ByteArrayInputStream(downsampledBytes))
                                                                }
                                                            }
                                                        } catch (_: Exception) {}
                                                    }

                                                    return WebResourceResponse(mime, null, 200, "OK", headers, BufferedInputStream(targetFile.inputStream(), 64 * 1024))
                                                }

                                                // Clean placeholder fallback instead of broken image
                                                val cleanLabel = (if (filename.isNotBlank() && filename != "review_images") filename.substringBeforeLast(".") else "Review Image").take(24)
                                                val fallbackSvg = """<svg xmlns="http://www.w3.org/2000/svg" width="360" height="220" viewBox="0 0 360 220"><rect width="100%" height="100%" rx="12" fill="#f8fafc" stroke="#cbd5e1" stroke-width="1.5"/><text x="50%" y="44%" text-anchor="middle" font-family="-apple-system, sans-serif" font-size="28" fill="#94a3b8">📷</text><text x="50%" y="64%" text-anchor="middle" font-family="-apple-system, sans-serif" font-size="12" font-weight="600" fill="#64748b">$cleanLabel</text></svg>"""
                                                val headers = mapOf(
                                                    "Access-Control-Allow-Origin" to "*",
                                                    "Access-Control-Allow-Methods" to "GET",
                                                    "Cache-Control" to "public, max-age=604800, immutable"
                                                )
                                                return WebResourceResponse("image/svg+xml", "UTF-8", 200, "OK", headers, ByteArrayInputStream(fallbackSvg.toByteArray(Charsets.UTF_8)))
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                                        super.onPageCommitVisible(view, url)
                                        isPageLoading = false
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isPageLoading = false
                                        view?.evaluateJavascript("if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
                                    }
                                }

                                // Bridge for handling image clicks & product view
                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onImageClick(url: String) {
                                        mainHandler.post {
                                            val cleanUrlName = url.substringAfterLast("/").substringBefore("?")
                                            val decodedCleanName = try { URLDecoder.decode(cleanUrlName, "UTF-8") } catch (_: Exception) { cleanUrlName }

                                            val foundIdx = allReviewPictures.indexOfFirst { pic ->
                                                val picFn = File(pic.url.removePrefix("file://")).name
                                                pic.url == url || pic.url == decodedCleanName ||
                                                picFn.equals(cleanUrlName, ignoreCase = true) ||
                                                picFn.equals(decodedCleanName, ignoreCase = true) ||
                                                (pic.id.isNotBlank() && (cleanUrlName.startsWith(pic.id) || url.contains(pic.id)))
                                            }
                                            if (foundIdx != -1) {
                                                lightboxInitialIndex = foundIdx
                                                lightboxImages = allReviewPictures
                                            } else {
                                                val resolvedTarget = webViewImageFileCache[cleanUrlName]
                                                    ?: webViewImageFileCache[decodedCleanName]
                                                    ?: webViewImageFileCache[cleanUrlName.lowercase()]
                                                val fullUrl = resolvedTarget?.absolutePath ?: url
                                                val displayName = resolvedTarget?.name ?: cleanUrlName
                                                lightboxInitialIndex = 0
                                                lightboxImages = listOf(PictureItem("img_click", displayName, fullUrl))
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun viewProduct(productId: String) {
                                        mainHandler.post {
                                            val prod = products.find { it.id == productId } ?: linkedProduct
                                            if (prod != null) {
                                                onClose()
                                                onViewProduct?.invoke(prod)
                                            }
                                        }
                                    }
                                }, "AndroidBridge")

                                loadDataWithBaseURL("https://local.review/", htmlDocument, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            try {
                                webView.settings.textZoom = zoomPercent
                                webView.evaluateJavascript("if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
                                if (webView.tag != htmlDocument) {
                                    isPageLoading = true
                                    webView.tag = htmlDocument
                                    webView.loadDataWithBaseURL("https://local.review/", htmlDocument, "text/html", "UTF-8", null)
                                }
                            } catch (_: Exception) {}
                        },
                        onRelease = { webView ->
                            try {
                                webView.stopLoading()
                                webView.loadUrl("about:blank")
                                webView.destroy()
                            } catch (_: Exception) {}
                            webViewInstance = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            title = "Delete Review",
            message = "Are you sure you want to delete \"${review.title}\" by ${review.authorName}? This action cannot be undone.",
            confirmText = "Delete",
            testTagPrefix = "viewer_delete_review",
            onConfirm = {
                onDeleteReview(review.id)
                showDeleteConfirm = false
                onClose()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // Full-Screen Photo Lightbox
    lightboxImages?.let { images ->
        FullScreenPhotoLightbox(
            images = images,
            initialIndex = lightboxInitialIndex,
            onDismiss = { lightboxImages = null }
        )
    }
}
}

/**
 * Builds unified HTML document containing the Review Header Card + Full Article Content.
 * Exactly matches Screenshots 7, 8, and 9 with smooth scrolling and responsive mobile sizing!
 */
private fun buildViewerHtml(
    review: ReviewItem,
    displayTitle: String,
    linkedProduct: ProductItem?,
    formattedDate: String,
    allPictures: List<PictureItem>
): String {
    // Pre-populate webViewImageFileCache with all pictures for O(1) instantaneous lookup
    allPictures.forEach { pic ->
        val cleanUrl = pic.url.removePrefix("file://")
        val f = File(cleanUrl)
        if (f.exists() && f.isFile) {
            webViewImageFileCache[f.name] = f
            webViewImageFileCache[f.name.lowercase()] = f
            webViewImageFileCache[f.nameWithoutExtension.lowercase()] = f
            if (pic.id.isNotBlank()) webViewImageFileCache[pic.id] = f
        }
    }

    var content = review.body.trim()

    // Unescape HTML entities if needed
    if ((content.contains("&lt;h") || content.contains("&lt;p") || content.contains("&lt;div") || content.contains("&lt;table")) &&
        !content.contains("<h") && !content.contains("<p")) {
        content = content
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
    }

    // If pure markdown (starts with # or does not contain html tags), wrap simple elements
    val isHtml = Regex("""<\/?(p|div|h[1-6]|table|ul|ol|li|img|figure|blockquote|span|b|i|strong)[\s>]""", RegexOption.IGNORE_CASE).containsMatchIn(content)
    if (!isHtml) {
        val lines = content.split("\n")
        val sb = StringBuilder()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("### ") -> sb.append("<h3>${trimmed.removePrefix("### ")}</h3>")
                trimmed.startsWith("## ") -> sb.append("<h2>${trimmed.removePrefix("## ")}</h2>")
                trimmed.startsWith("# ") -> sb.append("<h1>${trimmed.removePrefix("# ")}</h1>")
                trimmed.startsWith("> ") -> sb.append("<blockquote>${trimmed.removePrefix("> ")}</blockquote>")
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> sb.append("<li>${trimmed.removePrefix("- ").removePrefix("* ")}</li>")
                trimmed.isBlank() -> sb.append("<br/>")
                else -> sb.append("<p>$trimmed</p>")
            }
        }
        content = sb.toString()
    }

    // Convert markdown images ![alt](url) to rich HTML figures with lazy and async decoding
    val markdownImgRegex = Regex("""!\[(.*?)\]\((.*?)\)""")
    content = markdownImgRegex.replace(content) { match ->
        val alt = match.groupValues[1].trim()
        val urlOrPath = match.groupValues[2].trim()
        val cleanAlt = alt.ifBlank { "Review Photo" }
        val filename = urlOrPath.substringAfterLast("/").substringBefore("?")
        val targetUrl = if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) urlOrPath else "https://local.review/review_images/$filename"
        "<figure class=\"review-figure\"><img src=\"$targetUrl\" alt=\"$cleanAlt\" loading=\"lazy\" decoding=\"async\" /><figcaption class=\"review-caption\">$cleanAlt</figcaption></figure>"
    }

    // Rewrite all <img> tags to virtual https://local.review URLs so WebView loads them 100% reliably
    val imgTagRegex = Regex("""<img\b([^>]*)>""", RegexOption.IGNORE_CASE)
    content = imgTagRegex.replace(content) { match ->
        val attrs = match.groupValues[1]
        val srcMatch = Regex("""src=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(attrs)
        val attIdMatch = Regex("""(?:data-attachment-id|dataAttachmentId|data-attachment-token)=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)
        val altMatch = Regex("""alt=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(attrs)

        val rawSrc = srcMatch?.groupValues?.get(1)?.trim() ?: ""
        val attId = attIdMatch?.groupValues?.get(1)?.trim() ?: ""
        val alt = altMatch?.groupValues?.get(1)?.trim() ?: ""

        val matchedPic = allPictures.firstOrNull { 
            (attId.isNotBlank() && it.id.equals(attId, ignoreCase = true)) ||
            (rawSrc.isNotBlank() && (it.url == rawSrc || it.url.endsWith(rawSrc.substringAfterLast('/')))) ||
            (alt.isNotBlank() && it.name.equals(alt, ignoreCase = true))
        }

        val targetUrl = when {
            matchedPic != null -> {
                val fn = File(matchedPic.url.removePrefix("file://")).name
                "https://local.review/review_images/$fn"
            }
            rawSrc.startsWith("file://") || rawSrc.startsWith("/data/") -> {
                val fn = rawSrc.substringAfterLast('/')
                "https://local.review/review_images/$fn"
            }
            rawSrc.contains("review_images/") || rawSrc.contains("pictures/") || rawSrc.contains("images/") -> {
                val fn = rawSrc.substringAfterLast('/').substringBefore('?')
                "https://local.review/review_images/$fn"
            }
            attId.isNotBlank() -> "https://local.review/review_images/$attId"
            rawSrc.startsWith("http://") || rawSrc.startsWith("https://") -> rawSrc
            rawSrc.isNotBlank() && !rawSrc.startsWith("blob:") && !rawSrc.startsWith("data:") -> {
                val fn = rawSrc.substringAfterLast('/').substringBefore('?')
                "https://local.review/review_images/$fn"
            }
            else -> ""
        }

        if (targetUrl.isNotBlank()) {
            var newAttrs = attrs
            if (srcMatch != null) {
                newAttrs = newAttrs.replace(srcMatch.value, "src=\"$targetUrl\"")
            } else {
                newAttrs = "$newAttrs src=\"$targetUrl\""
            }
            if (!newAttrs.contains("loading=")) {
                newAttrs = "$newAttrs loading=\"lazy\""
            }
            if (!newAttrs.contains("decoding=")) {
                newAttrs = "$newAttrs decoding=\"async\""
            }
            "<img$newAttrs>"
        } else {
            match.value
        }
    }

    // Strip duplicate leading title if present at the top of content
    val leadingHeadingRegex = Regex("""^\s*<h[12][^>]*>([\s\S]*?)<\/h[12]>\s*""", RegexOption.IGNORE_CASE)
    val headingMatch = leadingHeadingRegex.find(content)
    if (headingMatch != null) {
        val headingText = headingMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim().lowercase()
        val normalizedDisplayTitle = displayTitle.trim().lowercase()
        if (headingText == normalizedDisplayTitle || headingText.contains(normalizedDisplayTitle) || normalizedDisplayTitle.contains(headingText)) {
            content = content.substring(headingMatch.range.last + 1).trim()
        }
    }

    // Auto-link raw deep links and web URLs if not already inside an <a> tag or attribute
    val rawUrlPattern = Regex("""(?<!\]\()(?<!href=["'])(?<!src=["'])\b((?:https?|upnote|marginnote\w*|obsidian|notion|craftdocs|bear|things|onenote|goodnotes|notability|logseq)://[^\s<>"'\)]+)""", RegexOption.IGNORE_CASE)
    content = rawUrlPattern.replace(content) { match ->
        var raw = match.value
        var trailing = ""
        while (raw.isNotEmpty() && (raw.endsWith(",") || raw.endsWith(".") || raw.endsWith(";") || raw.endsWith(":") || raw.endsWith(">") || raw.endsWith("!"))) {
            trailing = raw.takeLast(1) + trailing
            raw = raw.dropLast(1)
        }
        if (raw.isBlank()) {
            match.value
        } else {
            val label = getSchemeDisplayName(raw)
            val isApp = UriHelper.isAppSchemeUrl(raw)
            val cssClass = if (isApp) "deep-link" else "web-link"
            "<a href=\"$raw\" class=\"$cssClass\">$label</a>$trailing"
        }
    }

    // Build Product Box HTML matching beautiful review header.jpg
    val productHtml = if (linkedProduct != null) {
        val rawThumb = linkedProduct.pictures.firstOrNull()?.url ?: ""
        val thumbUrl = when {
            rawThumb.startsWith("http://") || rawThumb.startsWith("https://") -> rawThumb
            rawThumb.isNotBlank() -> {
                val fn = rawThumb.substringAfterLast('/').substringBefore('?')
                "https://local.review/pictures/$fn"
            }
            else -> ""
        }
        val thumbElement = if (thumbUrl.isNotBlank()) {
            "<img src=\"$thumbUrl\" class=\"prod-avatar\" alt=\"${linkedProduct.name}\" onerror=\"this.style.display='none'\" />"
        } else {
            "<div class=\"prod-avatar-placeholder\">📦</div>"
        }
        """
        <div class="product-box">
          $thumbElement
          <div class="prod-text">
            <div class="prod-label">PRODUCT</div>
            <div class="prod-name">${linkedProduct.brand} ${linkedProduct.name}</div>
          </div>
          <button type="button" class="view-prod-btn" onclick="window.AndroidBridge.viewProduct('${linkedProduct.id}')">
            View Product ↗
          </button>
        </div>
        """.trimIndent()
    } else {
        ""
    }

    // Check if any attached pictures were not in the HTML body, append them nicely at the bottom
    val unlinkedPhotosHtml = buildString {
        val unlinked = allPictures.filter { pic -> 
            val filename = File(pic.url.removePrefix("file://")).name
            !content.contains(pic.url) && !content.contains(filename) 
        }
        if (unlinked.isNotEmpty()) {
            append("<div class=\"attached-section\">")
            append("<h3 class=\"attached-title\">Attached Photos & Screenshots (${unlinked.size})</h3>")
            unlinked.forEach { pic ->
                val filename = File(pic.url.removePrefix("file://")).name
                val webUrl = "https://local.review/review_images/$filename"
                append("<p style=\"text-align:center;\"><img src=\"$webUrl\" alt=\"${pic.name}\" loading=\"lazy\" decoding=\"async\" /></p>")
            }
            append("</div>")
        }
    }

    val authorInitial = review.authorName.take(1).uppercase().ifBlank { "V" }

    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">
          <style>
            * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
            html, body {
              margin: 0;
              padding: 0;
              background-color: #ffffff;
              color: #1e293b;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
              -webkit-font-smoothing: antialiased;
              font-size: 13.5px;
              line-height: 1.55;
              width: 100%;
              -webkit-overflow-scrolling: touch;
              zoom: 85%;
            }
            
            .page-container {
              padding: 10px 14px 48px 14px;
              max-width: 720px;
              margin: 0 auto;
            }
            
            /* Review Header Card matching mobile screenshot (compact) */
            .review-header-card {
              background: #ffffff;
              border: 1px solid #e2e8f0;
              border-radius: 14px;
              padding: 14px 14px;
              margin-bottom: 16px;
              box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
            }
            
            .review-badge {
              display: inline-block;
              background: #eef2ff;
              color: #4f46e5;
              font-size: 10px;
              font-weight: 800;
              padding: 3px 8px;
              border-radius: 5px;
              letter-spacing: 0.5px;
              margin-bottom: 8px;
            }
            
            .review-title {
              font-size: 19px;
              font-weight: 800;
              color: #0f172a;
              margin: 0 0 10px 0;
              line-height: 1.3;
            }
            
            .rating-row {
              display: inline-flex;
              align-items: center;
              gap: 5px;
              background: #ffffff;
              border: 1px solid #e2e8f0;
              padding: 4px 10px;
              border-radius: 10px;
              margin-bottom: 12px;
            }
            
            .stars {
              color: #f59e0b;
              font-size: 14px;
              letter-spacing: 1.5px;
            }
            
            .rating-val {
              color: #b45309;
              font-size: 12px;
              font-weight: 800;
              margin-left: 2px;
            }
            
            .product-box {
              display: flex;
              align-items: center;
              gap: 10px;
              background: #f8fafc;
              border: 1px solid #f1f5f9;
              border-radius: 12px;
              padding: 8px 12px;
              margin-bottom: 12px;
            }
            
            img.prod-avatar, .prod-avatar {
              width: 44px !important;
              height: 44px !important;
              min-height: 44px !important;
              max-height: 44px !important;
              border-radius: 8px;
              object-fit: cover !important;
              background: #f8fafc;
              border: 1px solid #e2e8f0;
              padding: 2px;
              margin: 0 !important;
              display: block;
              flex-shrink: 0;
            }
            
            .prod-avatar-placeholder {
              width: 38px;
              height: 38px;
              border-radius: 8px;
              background: #eef2ff;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 18px;
              flex-shrink: 0;
            }
            
            .prod-text {
              flex: 1;
              min-width: 0;
            }
            
            .prod-label {
              font-size: 9px;
              font-weight: 800;
              color: #94a3b8;
              letter-spacing: 0.5px;
              margin-bottom: 1px;
            }
            
            .prod-name {
              font-size: 13px;
              font-weight: 800;
              color: #0f172a;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
            
            .view-prod-btn {
              background: #ffffff;
              border: 1px solid #cbd5e1;
              border-radius: 6px;
              padding: 5px 10px;
              font-size: 11px;
              font-weight: 700;
              color: #334155;
              cursor: pointer;
              white-space: nowrap;
            }
            
            .author-row {
              display: flex;
              align-items: center;
              gap: 6px;
              font-size: 11px;
              color: #64748b;
              font-weight: 500;
            }
            
            .author-avatar {
              width: 22px;
              height: 22px;
              border-radius: 50%;
              background: #0f172a;
              color: #ffffff;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 10px;
              font-weight: 800;
              line-height: 22px;
              text-align: center;
            }
            
            .dot {
              color: #cbd5e1;
            }
            
            /* Content Typography & Graphs */
            .review-body-content {
              margin-top: 8px;
              word-wrap: break-word;
            }
            
            h1 {
              font-size: 20px;
              font-weight: 800;
              color: #0f172a;
              margin-top: 18px;
              margin-bottom: 8px;
              line-height: 1.3;
            }
            
            h2 {
              font-size: 17px;
              font-weight: 800;
              color: #0f172a;
              margin-top: 16px;
              margin-bottom: 8px;
              line-height: 1.3;
            }
            
            h3 {
              font-size: 15px;
              font-weight: 700;
              color: #0f172a;
              margin-top: 14px;
              margin-bottom: 6px;
            }
            
            p {
              margin-bottom: 10px;
              color: #334155;
              line-height: 1.6;
            }
            
            .review-body-content img, .attached-section img {
              max-width: 100%;
              height: auto;
              object-fit: contain;
              background-color: #f8fafc;
              border-radius: 8px;
              border: 1px solid #e2e8f0;
              margin: 8px auto;
              display: block;
              cursor: pointer;
              box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
            }

            .review-figure {
              margin: 14px 0;
              text-align: center;
            }

            .review-caption {
              font-size: 11px;
              color: #64748b;
              margin-top: 4px;
              font-style: italic;
            }
            
            blockquote {
              border-left: 4px solid #0284c7;
              padding: 8px 12px;
              margin: 12px 0;
              background: #f0f9ff;
              border-radius: 0 8px 8px 0;
              color: #0369a1;
              font-weight: 500;
            }
            
            table {
              width: 100%;
              border-collapse: collapse;
              margin: 14px 0;
              border: 1px solid #e2e8f0;
              border-radius: 8px;
              overflow: hidden;
              font-size: 13px;
              background-color: #f8fafc;
              display: block;
              overflow-x: auto;
              -webkit-overflow-scrolling: touch;
            }
            
            th, td {
              border: 1px solid #e2e8f0;
              padding: 7px 10px;
              text-align: left;
            }
            
            th {
              background-color: #f1f5f9;
              font-weight: 700;
              color: #0f172a;
            }
            
            tr:nth-child(even) td {
              background-color: #ffffff;
            }
            
            ul, ol {
              padding-left: 20px;
              margin: 8px 0;
              color: #334155;
            }
            
            li {
              margin-bottom: 4px;
            }
            
            /* UpNote checklist support */
            .checklist, ul.checklist {
              list-style-type: none;
              padding-left: 4px;
            }
            .checklist li {
              display: flex;
              align-items: baseline;
              gap: 6px;
            }
            input[type="checkbox"] {
              accent-color: #4f46e5;
              transform: scale(1.1);
              margin-right: 4px;
            }
            
            mark {
              background-color: #fef08a;
              color: #0f172a;
              padding: 1px 4px;
              border-radius: 3px;
            }
            
            pre {
              background-color: #0f172a;
              color: #f1f5f9;
              padding: 10px 12px;
              border-radius: 8px;
              overflow-x: auto;
              font-size: 12px;
              font-family: monospace;
              margin: 12px 0;
            }
            
            code {
              font-family: monospace;
              font-size: 12px;
              background-color: #f1f5f9;
              color: #0f172a;
              padding: 2px 4px;
              border-radius: 4px;
            }
            pre code {
              background-color: transparent;
              color: inherit;
              padding: 0;
            }
            
            .attached-section {
              margin-top: 24px;
              padding-top: 14px;
              border-top: 1px solid #e2e8f0;
            }
            
            .attached-title {
              font-size: 14px;
              font-weight: 800;
              color: #0f172a;
              margin-bottom: 10px;
            }

            a {
              color: #2563eb;
              text-decoration: underline;
              font-weight: 600;
            }
            a.deep-link {
              color: #6d28d9;
              background: #f5f3ff;
              border: 1px solid #ddd6fe;
              border-radius: 6px;
              padding: 2px 7px;
              text-decoration: none;
              font-weight: 700;
              font-size: 12px;
              display: inline-block;
              margin: 2px 0;
            }
            a.deep-link:active {
              background: #ede9fe;
            }
          </style>
        </head>
        <body>
          <div class="page-container">
            <!-- Review Info Card at the top: scrolls off-screen naturally! -->
            <div class="review-header-card">
              <div class="review-badge">REVIEW #${review.reviewNumber}</div>
              <div class="review-title">$displayTitle</div>
              <div class="rating-row">
                <span class="stars">★★★★★</span>
                <span class="rating-val">${review.rating}.0</span>
              </div>
              $productHtml
              <div class="author-row">
                <div class="author-avatar">$authorInitial</div>
                <span class="author-name">${review.authorName.ifBlank { "Verified Reviewer" }}</span>
                <span class="dot">•</span>
                <span class="date">$formattedDate</span>
              </div>
            </div>

            <!-- Full Article Content & Graphs -->
            <div class="review-body-content">
              $content
              $unlinkedPhotosHtml
            </div>
          </div>

          <script>
            document.querySelectorAll('img').forEach(function(img) {
              img.onclick = function() {
                if (window.AndroidBridge) {
                  window.AndroidBridge.onImageClick(img.src);
                }
              };
            });
          </script>
        </body>
        </html>
    """.trimIndent()
}
