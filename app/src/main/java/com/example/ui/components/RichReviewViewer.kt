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
import com.example.model.PictureItem
import com.example.model.ProductItem
import com.example.model.ReviewItem
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Format human-readable date
    val formattedDate = remember(review.createdTime) {
        if (review.createdTime.isBlank()) {
            "Recent"
        } else {
            try {
                val ts = review.createdTime.toLong()
                SimpleDateFormat("M/d/yyyy", Locale.US).format(Date(ts))
            } catch (_: Exception) {
                review.createdTime
            }
        }
    }

    // Extract images from review.pictures or <img> tags in review.body
    val allReviewPictures = remember(review) {
        val list = mutableListOf<PictureItem>()
        if (review.pictures.isNotEmpty()) {
            list.addAll(review.pictures)
        }
        val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        imgRegex.findAll(review.body).forEachIndexed { idx, match ->
            val url = match.groupValues[1]
            if (list.none { it.url == url }) {
                list.add(PictureItem(id = "body_img_$idx", name = "Photo ${list.size + 1}", url = url))
            }
        }
        list
    }

    // Zoom scaling state (Word-style zoom: default 85% for compact mobile comfort)
    var zoomPercent by remember { mutableIntStateOf(85) }
    var showZoomMenu by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Build the complete unified scrollable HTML document (Header Card + Full Article Body)
    val htmlDocument = remember(review, displayTitle, linkedProduct, formattedDate, allReviewPictures, zoomPercent) {
        buildViewerHtml(
            review = review,
            displayTitle = displayTitle,
            linkedProduct = linkedProduct,
            formattedDate = formattedDate,
            allPictures = allReviewPictures,
            zoomPercent = zoomPercent
        )
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
            Column(modifier = Modifier.fillMaxSize()) {
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
                            Column(modifier = Modifier.padding(end = 4.dp)) {
                                Text(
                                    text = displayTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Review #${review.reviewNumber} • ${linkedProduct?.name ?: "Community Review"}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Actions Row: Word-style Zoom Pill + Edit & Delete Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Word-Style Zoom Pill [- 85% +]
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
                                                wv.evaluateJavascript("document.body.style.zoom = '${zoomPercent}%';", null)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Zoom out", modifier = Modifier.size(13.dp), tint = Color(0xFF334155))
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
                                                            wv.evaluateJavascript("document.body.style.zoom = '${zoomPercent}%';", null)
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
                                                wv.evaluateJavascript("document.body.style.zoom = '${zoomPercent}%';", null)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Zoom in", modifier = Modifier.size(13.dp), tint = Color(0xFF334155))
                                    }
                                }
                            }

                            Button(
                                onClick = { onEditReview(review) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F172A),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // 100% UNIFIED SCROLL VIEW (Smooth vertical scrolling on mobile phones)
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
                                // Prevent GPU MESA rendernode crash in virtualized container
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.cacheMode = WebSettings.LOAD_NO_CACHE
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
                                        // Gracefully handle renderer crash or termination so host app does not terminate
                                        try {
                                            (view?.parent as? ViewGroup)?.removeView(view)
                                            view?.destroy()
                                        } catch (_: Exception) {}
                                        return true
                                    }

                                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                        val reqUrl = request.url ?: return null
                                        val host = reqUrl.host?.lowercase() ?: ""
                                        val path = reqUrl.path ?: ""
                                        val urlStr = reqUrl.toString()

                                        if (host == "local.review" || host == "appassets.local" || path.contains("review_images") || path.contains("pictures") || urlStr.contains("review_images") || urlStr.contains("pictures")) {
                                            try {
                                                val pathParam = reqUrl.getQueryParameter("path")
                                                val filename = reqUrl.lastPathSegment ?: ""
                                                var targetFile: File? = null

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
                                                    val decodedFilename = try { URLDecoder.decode(filename, "UTF-8") } catch (_: Exception) { filename }
                                                    val f = File(ctx.filesDir, "review_images/$decodedFilename")
                                                    if (f.exists() && f.isFile) targetFile = f
                                                }
                                                if (targetFile == null || !targetFile.exists()) {
                                                    val f = File(ctx.filesDir, "pictures/$filename")
                                                    if (f.exists() && f.isFile) targetFile = f
                                                }
                                                if (targetFile == null || !targetFile.exists()) {
                                                    val decodedFilename = try { URLDecoder.decode(filename, "UTF-8") } catch (_: Exception) { filename }
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
                                                    val cleanName = filename.lowercase()
                                                    val dirs = listOf(File(ctx.filesDir, "review_images"), File(ctx.filesDir, "pictures"))
                                                    for (dir in dirs) {
                                                        if (dir.exists() && dir.isDirectory) {
                                                            targetFile = dir.listFiles()?.firstOrNull {
                                                                it.name.lowercase() == cleanName || it.name.lowercase().contains(cleanName) || cleanName.contains(it.name.lowercase())
                                                            }
                                                            if (targetFile != null && targetFile.exists()) break
                                                        }
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
                                                        "Cache-Control" to "no-cache"
                                                    )
                                                    return WebResourceResponse(mime, null, 200, "OK", headers, targetFile.inputStream())
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        view?.evaluateJavascript("if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
                                    }
                                }

                                // Bridge for handling image clicks & product view
                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onImageClick(url: String) {
                                        mainHandler.post {
                                            val foundIdx = allReviewPictures.indexOfFirst { 
                                                it.url == url || url.contains(File(it.url.removePrefix("file://")).name)
                                            }
                                            if (foundIdx != -1) {
                                                lightboxInitialIndex = foundIdx
                                                lightboxImages = allReviewPictures
                                            } else {
                                                lightboxInitialIndex = 0
                                                lightboxImages = listOf(PictureItem("img_click", "Attached Image", url))
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
                            // CRITICAL FIX: Only reload if the HTML document actually changed, preventing scroll reset!
                            webView.settings.textZoom = zoomPercent
                            webView.evaluateJavascript("if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
                            if (webView.tag != htmlDocument) {
                                webView.tag = htmlDocument
                                webView.loadDataWithBaseURL("https://local.review/", htmlDocument, "text/html", "UTF-8", null)
                            }
                        },
                        onRelease = { webView ->
                            try {
                                webView.stopLoading()
                                webView.loadUrl("about:blank")
                                webView.destroy()
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Review") },
            text = { Text("Are you sure you want to delete this review? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteReview(review.id)
                        showDeleteConfirm = false
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
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

/**
 * Builds unified HTML document containing the Review Header Card + Full Article Content.
 * Exactly matches Screenshots 7, 8, and 9 with smooth scrolling and responsive mobile sizing!
 */
private fun buildViewerHtml(
    review: ReviewItem,
    displayTitle: String,
    linkedProduct: ProductItem?,
    formattedDate: String,
    allPictures: List<PictureItem>,
    zoomPercent: Int = 85
): String {
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

    // Rewrite any local file or relative image URLs to virtual https://local.review URLs so WebView loads them 100% reliably
    val fileImgRegex = Regex("""src=["']file:/*([^"']+)["']""", RegexOption.IGNORE_CASE)
    content = fileImgRegex.replace(content) { match ->
        val fullPath = "/" + match.groupValues[1].trimStart('/')
        val file = File(fullPath)
        if (fullPath.contains("review_images/")) {
            "src=\"https://local.review/review_images/${file.name}\""
        } else {
            "src=\"https://local.review/image?path=${URLEncoder.encode(fullPath, "UTF-8")}\""
        }
    }

    val absolutePathImgRegex = Regex("""src=["'](\/data\/[^"']+)["']""", RegexOption.IGNORE_CASE)
    content = absolutePathImgRegex.replace(content) { match ->
        val fullPath = match.groupValues[1]
        val file = File(fullPath)
        if (fullPath.contains("review_images/")) {
            "src=\"https://local.review/review_images/${file.name}\""
        } else {
            "src=\"https://local.review/image?path=${URLEncoder.encode(fullPath, "UTF-8")}\""
        }
    }

    val relativeReviewImgRegex = Regex("""src=["'](?:review_images|images)\/([^"']+)["']""", RegexOption.IGNORE_CASE)
    content = relativeReviewImgRegex.replace(content) { match ->
        val filename = match.groupValues[1]
        "src=\"https://local.review/review_images/$filename\""
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

    // Build Product Box HTML
    val productHtml = if (linkedProduct != null) {
        val thumbUrl = linkedProduct.pictures.firstOrNull()?.url ?: ""
        val thumbElement = if (thumbUrl.isNotBlank()) {
            "<img src=\"$thumbUrl\" class=\"prod-avatar\" alt=\"${linkedProduct.name}\" />"
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
            View ↗
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
                append("<p style=\"text-align:center;\"><img src=\"$webUrl\" alt=\"${pic.name}\" /></p>")
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
              zoom: ${zoomPercent}%;
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
            
            .prod-avatar {
              width: 38px;
              height: 38px;
              border-radius: 8px;
              object-fit: cover;
              background: #e2e8f0;
              border: 1px solid #e2e8f0;
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
            
            img {
              max-width: 100%;
              height: auto;
              border-radius: 10px;
              border: 1px solid #e2e8f0;
              margin: 10px 0;
              display: block;
              cursor: pointer;
              box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
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
