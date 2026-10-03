package com.example.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.resolveImageModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RichReviewEditor(
    products: List<ProductItem>,
    initialReview: ReviewItem? = null,
    preselectedProduct: ProductItem? = null,
    onSaveReview: (review: ReviewItem) -> Unit,
    onDismiss: () -> Unit
) {
    // Current selected product for review
    var selectedProduct by remember {
        mutableStateOf(
            preselectedProduct
                ?: products.find { it.id in (initialReview?.productIds ?: emptyList()) }
                ?: products.find { it.name in (initialReview?.productTitles ?: emptyList()) }
                ?: products.firstOrNull()
        )
    }

    var isProductDropdownOpen by remember { mutableStateOf(false) }
    var isTableDropdownOpen by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf(initialReview?.title ?: "") }
    var authorName by remember { mutableStateOf(initialReview?.authorName ?: "Verified Reviewer") }
    var rating by remember { mutableIntStateOf(initialReview?.rating ?: 5) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var zoomPercent by remember { mutableStateOf(85) }
    var showZoomMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val reviewImagesDir = File(context.filesDir, "review_images")
                if (!reviewImagesDir.exists()) reviewImagesDir.mkdirs()
                val filename = "editor_${System.currentTimeMillis()}.jpg"
                val destFile = File(reviewImagesDir, filename)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { out -> input.copyTo(out) }
                }
                val picturesDir = File(context.filesDir, "pictures")
                if (!picturesDir.exists()) picturesDir.mkdirs()
                try { destFile.copyTo(File(picturesDir, filename), overwrite = true) } catch (_: Exception) {}

                val webUrl = "https://local.review/review_images/$filename"
                val js = "insertImage('$webUrl');"
                webViewInstance?.evaluateJavascript(js, null)
            } catch (_: Exception) {
                val js = "insertImage('${uri.toString()}');"
                webViewInstance?.evaluateJavascript(js, null)
            }
        }
    }

    val initialBodyHtml = remember(initialReview) {
        var body = initialReview?.body ?: "<p>Introduction and specs...</p>"
        // Rewrite file and relative image URLs to virtual https://local.review URLs
        val fileImgRegex = Regex("""src=["']file:/*([^"']+)["']""", RegexOption.IGNORE_CASE)
        body = fileImgRegex.replace(body) { match ->
            val fullPath = "/" + match.groupValues[1].trimStart('/')
            val file = File(fullPath)
            if (fullPath.contains("review_images/")) {
                "src=\"https://local.review/review_images/${file.name}\""
            } else {
                "src=\"https://local.review/image?path=${URLEncoder.encode(fullPath, "UTF-8")}\""
            }
        }
        val absolutePathImgRegex = Regex("""src=["'](\/data\/[^"']+)["']""", RegexOption.IGNORE_CASE)
        body = absolutePathImgRegex.replace(body) { match ->
            val fullPath = match.groupValues[1]
            val file = File(fullPath)
            if (fullPath.contains("review_images/")) {
                "src=\"https://local.review/review_images/${file.name}\""
            } else {
                "src=\"https://local.review/image?path=${URLEncoder.encode(fullPath, "UTF-8")}\""
            }
        }
        val relativeReviewImgRegex = Regex("""src=["'](?:review_images|images)\/([^"']+)["']""", RegexOption.IGNORE_CASE)
        body = relativeReviewImgRegex.replace(body) { match ->
            val filename = match.groupValues[1]
            "src=\"https://local.review/review_images/$filename\""
        }
        body
    }

    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("rich_review_editor_screen"),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF1E293B)
                                )
                            }
                            Column {
                                Text(
                                    text = if (initialReview != null) "Edit Review" else "Write Review",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = selectedProduct?.name ?: "No product selected",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Right: Interactive Rating Stars + Save Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Stars
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                (1..5).forEach { star ->
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "$star stars",
                                        tint = if (star <= rating) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { rating = star }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    webViewInstance?.evaluateJavascript("getEditorContent();") { resultJson ->
                                        // resultJson is a quoted JSON string with HTML content
                                        val content = if (resultJson != null && resultJson != "null") {
                                            try {
                                                org.json.JSONObject("{ \"body\": $resultJson }").getString("body")
                                            } catch (e: Exception) {
                                                resultJson.removeSurrounding("\"").replace("\\\"", "\"").replace("\\n", "\n")
                                            }
                                        } else {
                                            initialBodyHtml
                                        }

                                        val prod = selectedProduct
                                        val prodIds = if (prod != null) listOf(prod.id) else emptyList()
                                        val prodTitles = if (prod != null) listOf(prod.name) else emptyList()

                                        val savedItem = ReviewItem(
                                            id = initialReview?.id ?: "",
                                            reviewNumber = initialReview?.reviewNumber ?: 0,
                                            title = title.ifBlank { if (prod != null) "${prod.name} Review" else "User Review" },
                                            body = content,
                                            authorName = authorName.ifBlank { "Verified Reviewer" },
                                            rating = rating,
                                            productIds = prodIds,
                                            productTitles = prodTitles,
                                            pictures = initialReview?.pictures ?: emptyList(),
                                            createdTime = initialReview?.createdTime ?: ""
                                        )
                                        onSaveReview(savedItem)
                                        onDismiss()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4F46E5),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save Review", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Formatting Toolbar (Screenshot edit and add table.png)
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // H1, H2, H3
                        ToolbarTextButton("H1") { webViewInstance?.evaluateJavascript("formatBlock('h1');", null) }
                        ToolbarTextButton("H2") { webViewInstance?.evaluateJavascript("formatBlock('h2');", null) }
                        ToolbarTextButton("H3") { webViewInstance?.evaluateJavascript("formatBlock('h3');", null) }

                        VerticalDivider(modifier = Modifier.height(20.dp), color = Color(0xFFCBD5E1))

                        // B, I, U, S
                        ToolbarIconButton(Icons.Default.FormatBold, "Bold") { webViewInstance?.evaluateJavascript("execCommand('bold');", null) }
                        ToolbarIconButton(Icons.Default.FormatItalic, "Italic") { webViewInstance?.evaluateJavascript("execCommand('italic');", null) }
                        ToolbarIconButton(Icons.Default.FormatUnderlined, "Underline") { webViewInstance?.evaluateJavascript("execCommand('underline');", null) }
                        ToolbarIconButton(Icons.Default.FormatStrikethrough, "Strikethrough") { webViewInstance?.evaluateJavascript("execCommand('strikeThrough');", null) }

                        VerticalDivider(modifier = Modifier.height(20.dp), color = Color(0xFFCBD5E1))

                        // Bullet List, Numbered List, Quote
                        ToolbarIconButton(Icons.Default.FormatListBulleted, "Bullet List") { webViewInstance?.evaluateJavascript("execCommand('insertUnorderedList');", null) }
                        ToolbarIconButton(Icons.Default.FormatListNumbered, "Numbered List") { webViewInstance?.evaluateJavascript("execCommand('insertOrderedList');", null) }
                        ToolbarIconButton(Icons.Default.FormatQuote, "Quote") { webViewInstance?.evaluateJavascript("formatBlock('blockquote');", null) }

                        VerticalDivider(modifier = Modifier.height(20.dp), color = Color(0xFFCBD5E1))

                        // Table Dropdown Menu [⊞ ∨] (Screenshot edit and add table.png)
                        Box {
                            OutlinedButton(
                                onClick = { isTableDropdownOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = "Table", modifier = Modifier.size(14.dp), tint = Color(0xFF334155))
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF64748B))
                            }

                            DropdownMenu(
                                expanded = isTableDropdownOpen,
                                onDismissRequest = { isTableDropdownOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Insert 2x2 Table", fontSize = 12.sp) },
                                    onClick = {
                                        webViewInstance?.evaluateJavascript("insertTable(2, 2);", null)
                                        isTableDropdownOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Insert 3x3 Table", fontSize = 12.sp) },
                                    onClick = {
                                        webViewInstance?.evaluateJavascript("insertTable(3, 3);", null)
                                        isTableDropdownOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add Row Below", fontSize = 12.sp) },
                                    onClick = {
                                        webViewInstance?.evaluateJavascript("addRow();", null)
                                        isTableDropdownOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add Column Right", fontSize = 12.sp) },
                                    onClick = {
                                        webViewInstance?.evaluateJavascript("addColumn();", null)
                                        isTableDropdownOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Row", fontSize = 12.sp, color = Color(0xFFDC2626)) },
                                    onClick = {
                                        webViewInstance?.evaluateJavascript("deleteRow();", null)
                                        isTableDropdownOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Column", fontSize = 12.sp, color = Color(0xFFDC2626)) },
                                    onClick = {
                                        webViewInstance?.evaluateJavascript("deleteColumn();", null)
                                        isTableDropdownOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Table", fontSize = 12.sp, color = Color(0xFFDC2626)) },
                                    onClick = {
                                        webViewInstance?.evaluateJavascript("deleteTable();", null)
                                        isTableDropdownOpen = false
                                    }
                                )
                            }
                        }

                        // Insert Photo Button [🖼️ Insert Photo]
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFEEF2FF),
                                contentColor = Color(0xFF4F46E5)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2FE)),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Insert Photo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Clear formatting Tx
                        ToolbarIconButton(Icons.Default.FormatClear, "Clear Formatting") {
                            webViewInstance?.evaluateJavascript("execCommand('removeFormat');", null)
                        }

                        VerticalDivider(modifier = Modifier.height(20.dp), color = Color(0xFFCBD5E1))

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
                                            wv.settings.textZoom = 100
                                            wv.evaluateJavascript("if (typeof setEditorZoom === 'function') { setEditorZoom($zoomPercent); } else if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
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
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
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
                                                        wv.settings.textZoom = 100
                                                        wv.evaluateJavascript("if (typeof setEditorZoom === 'function') { setEditorZoom($zoomPercent); } else if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
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
                                            wv.settings.textZoom = 100
                                            wv.evaluateJavascript("if (typeof setEditorZoom === 'function') { setEditorZoom($zoomPercent); } else if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Zoom in", modifier = Modifier.size(13.dp), tint = Color(0xFF334155))
                                }
                            }
                        }
                    }
                }

                // Middle Inputs: REVIEWING PRODUCT banner, Report Name, Review Author
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reviewing Product Box (Screenshot edit and add table.png)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val thumb = selectedProduct?.pictures?.firstOrNull()?.url ?: ""
                                if (thumb.isNotBlank()) {
                                    AsyncImage(
                                        model = resolveImageModel(context, thumb),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFE2E8F0))
                                    )
                                }
                                Column {
                                    Text(
                                        text = "REVIEWING PRODUCT",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5)
                                    )
                                    Text(
                                        text = selectedProduct?.name ?: "Select a product",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            // Switch to dropdown
                            Box {
                                OutlinedButton(
                                    onClick = { isProductDropdownOpen = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Switch ∨", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }

                                DropdownMenu(
                                    expanded = isProductDropdownOpen,
                                    onDismissRequest = { isProductDropdownOpen = false }
                                ) {
                                    products.forEach { prod ->
                                        DropdownMenuItem(
                                            text = { Text(prod.name, fontSize = 12.sp) },
                                            onClick = {
                                                selectedProduct = prod
                                                isProductDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // REVIEW / REPORT NAME input
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "REVIEW / REPORT NAME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4F46E5)
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Give your review report a descriptive name", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }

                    // Review Author & Tip Row (Screenshot edit and add table.png)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Review Author:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                        OutlinedTextField(
                            value = authorName,
                            onValueChange = { authorName = it },
                            placeholder = { Text("Author name", fontSize = 11.sp) },
                            modifier = Modifier.width(150.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                        Text(
                            text = "💡 Tip: Click on any table to add or delete rows and columns, or press Tab in the last cell!",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Interactive WYSIWYG Content Area via WebView (contentEditable)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    val editorHtml = remember { buildEditorHtml(initialBodyHtml) }

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewInstance = this
                                // Prevent GPU MESA rendernode crash in virtualized container
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = false
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.textZoom = 100
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.allowFileAccessFromFileURLs = true
                                settings.allowUniversalAccessFromFileURLs = true
                                settings.defaultTextEncodingName = "UTF-8"
                                isVerticalScrollBarEnabled = true
                                isNestedScrollingEnabled = false
                                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

                                // Smooth scrolling inside Compose Dialog
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
                                        view?.settings?.textZoom = 100
                                        view?.evaluateJavascript("if (typeof setEditorZoom === 'function') { setEditorZoom($zoomPercent); } else if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
                                    }
                                }

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun notifyReady() {
                                        // Ready
                                    }
                                }, "AndroidBridge")

                                loadDataWithBaseURL("https://local.review/", editorHtml, "text/html", "UTF-8", null)
                            }
                        },
                        update = { wv ->
                            wv.settings.textZoom = 100
                            wv.evaluateJavascript("if (typeof setEditorZoom === 'function') { setEditorZoom($zoomPercent); } else if (document.body) { document.body.style.zoom = '${zoomPercent}%'; }", null)
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
}

@Composable
private fun ToolbarTextButton(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier
            .size(width = 30.dp, height = 28.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
        }
    }
}

@Composable
private fun ToolbarIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier
            .size(width = 30.dp, height = 28.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = Color(0xFF334155), modifier = Modifier.size(15.dp))
        }
    }
}

/**
 * Builds HTML document with rich contentEditable editor & interactive table operations.
 */
private fun buildEditorHtml(initialContent: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
          <style>
            * { box-sizing: border-box; }
            html, body {
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
              zoom: 85%;
              margin: 0;
              padding: 0;
              color: #1e293b;
              font-size: 14px;
              line-height: 1.6;
              background: #ffffff;
              width: 100%;
              overflow-x: hidden;
            }
            .editor-page-container {
              padding: 12px 14px 48px 14px;
              max-width: 720px;
              margin: 0 auto;
              box-sizing: border-box;
            }
            #editor {
              min-height: 240px;
              outline: none;
              padding-bottom: 60px;
              word-wrap: break-word;
              overflow-wrap: break-word;
            }
            h1 { font-size: 20px; font-weight: 800; color: #0f172a; margin: 16px 0 8px 0; border-bottom: 1px solid #e2e8f0; padding-bottom: 4px; }
            h2 { font-size: 17px; font-weight: 700; color: #0f172a; margin: 14px 0 6px 0; }
            h3 { font-size: 15px; font-weight: 600; color: #0f172a; margin: 12px 0 4px 0; }
            p { margin: 8px 0; color: #334155; }
            blockquote {
              border-left: 4px solid #0284c7;
              padding: 8px 12px;
              margin: 12px 0;
              background: #f0f9ff;
              border-radius: 0 6px 6px 0;
              color: #0369a1;
            }
            table {
              width: 100%;
              border-collapse: collapse;
              margin: 12px 0;
              border: 1px solid #cbd5e1;
              background-color: #f8fafc;
            }
            th, td {
              border: 1px solid #cbd5e1;
              padding: 8px 10px;
              text-align: left;
              min-width: 60px;
            }
            th {
              background-color: #f1f5f9;
              font-weight: 700;
            }
            img {
              max-width: 100%;
              max-height: 480px;
              object-fit: contain;
              height: auto;
              border-radius: 10px;
              border: 1px solid #e2e8f0;
              margin: 10px auto;
              display: block;
              box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
            }
          </style>
        </head>
        <body>
          <div class="editor-page-container">
            <div id="editor" contenteditable="true">$initialContent</div>
          </div>
          <script>
            function setEditorZoom(zoomPct) {
              var style = document.getElementById('dynamic-editor-zoom-style');
              if (!style) {
                style = document.createElement('style');
                style.id = 'dynamic-editor-zoom-style';
                document.head.appendChild(style);
              }
              style.textContent = 'html, body, #editor { zoom: ' + zoomPct + '% !important; }';
            }
            function execCommand(cmd, val) {
              document.execCommand(cmd, false, val || null);
            }
            function formatBlock(tag) {
              document.execCommand('formatBlock', false, '<' + tag + '>');
            }
            function insertImage(url) {
              document.execCommand('insertImage', false, url);
            }
            function insertTable(rows, cols) {
              var html = '<table><tbody>';
              for (var r = 0; r < rows; r++) {
                html += '<tr>';
                for (var c = 0; c < cols; c++) {
                  if (r === 0) {
                    html += '<th>Header ' + (c + 1) + '</th>';
                  } else {
                    html += '<td>Data</td>';
                  }
                }
                html += '</tr>';
              }
              html += '</tbody></table><p></p>';
              document.execCommand('insertHTML', false, html);
            }
            function getCurrentTable() {
              var sel = window.getSelection();
              if (!sel.rangeCount) return null;
              var node = sel.getRangeAt(0).commonAncestorContainer;
              while (node && node.nodeName !== 'TABLE' && node.id !== 'editor') {
                node = node.parentNode;
              }
              return node && node.nodeName === 'TABLE' ? node : null;
            }
            function addRow() {
              var table = getCurrentTable();
              if (table) {
                var cols = table.rows[0] ? table.rows[0].cells.length : 2;
                var row = table.insertRow(-1);
                for (var i = 0; i < cols; i++) {
                  var cell = row.insertCell(-1);
                  cell.innerText = 'New Data';
                }
              }
            }
            function addColumn() {
              var table = getCurrentTable();
              if (table) {
                for (var i = 0; i < table.rows.length; i++) {
                  var cell = table.rows[i].insertCell(-1);
                  cell.innerText = i === 0 ? 'Header' : 'Data';
                }
              }
            }
            function deleteRow() {
              var table = getCurrentTable();
              if (table && table.rows.length > 1) {
                table.deleteRow(table.rows.length - 1);
              }
            }
            function deleteColumn() {
              var table = getCurrentTable();
              if (table && table.rows.length && table.rows[0].cells.length > 1) {
                for (var i = 0; i < table.rows.length; i++) {
                  table.rows[i].deleteCell(table.rows[i].cells.length - 1);
                }
              }
            }
            function deleteTable() {
              var table = getCurrentTable();
              if (table && table.parentNode) {
                table.parentNode.removeChild(table);
              }
            }
            function getEditorContent() {
              var el = document.getElementById('editor');
              return el ? el.innerHTML : '';
            }
          </script>
        </body>
        </html>
    """.trimIndent()
}
