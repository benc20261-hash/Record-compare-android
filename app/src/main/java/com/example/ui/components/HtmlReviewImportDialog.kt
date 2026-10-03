package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.importer.ImportedReviewDraft
import com.example.model.PictureItem
import com.example.model.ProductItem
import com.example.model.resolveImageModel
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlReviewImportDialog(
    drafts: List<ImportedReviewDraft>,
    products: List<ProductItem>,
    onConfirmImport: (List<ImportedReviewDraft>) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    val context = LocalContext.current

    var editableDrafts by remember { mutableStateOf(drafts) }
    var selectedDraftIds by remember { mutableStateOf(drafts.map { it.id }.toSet()) }

    var previewingDraft by remember { mutableStateOf<ImportedReviewDraft?>(null) }
    var lightboxImages by remember { mutableStateOf<List<PictureItem>?>(null) }
    var lightboxInitialIndex by remember { mutableIntStateOf(0) }

    val totalImages = remember(editableDrafts) {
        editableDrafts.sumOf { it.pictures.size }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Import UpNote Reviews",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Found ${editableDrafts.size} note(s) with $totalImages picture(s)",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(Modifier.height(14.dp))

                // Scrollable list of reviews
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(editableDrafts) { index, draft ->
                        val isChecked = draft.id in selectedDraftIds

                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isChecked) Color(0xFFF8FAFC) else Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isChecked) Color(0xFFCBD5E1) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Top row: check box, source folder, image count
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedDraftIds = if (checked) {
                                                    selectedDraftIds + draft.id
                                                } else {
                                                    selectedDraftIds - draft.id
                                                }
                                            }
                                        )
                                        Text(
                                            text = "Review #${index + 1}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4F46E5)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFE2E8F0)
                                        ) {
                                            Text(
                                                text = draft.sourceFolderName.take(24),
                                                fontSize = 11.sp,
                                                color = Color(0xFF475569),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (draft.pictures.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFDCFCE7)
                                            ) {
                                                Text(
                                                    text = "📷 ${draft.pictures.size} pics",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF15803D),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Editable Title
                                OutlinedTextField(
                                    value = draft.title,
                                    onValueChange = { newTitle ->
                                        editableDrafts = editableDrafts.toMutableList().also { list ->
                                            list[index] = draft.copy(title = newTitle)
                                        }
                                    },
                                    label = { Text("Review Title") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                // Product Assignment Dropdown
                                var productDropdownExpanded by remember { mutableStateOf(false) }
                                val assignedProduct = products.find { it.id == draft.assignedProductId }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = assignedProduct?.name ?: "— No Product Selected (Standalone) —",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Assign to Device / Product") },
                                        trailingIcon = {
                                            IconButton(onClick = { productDropdownExpanded = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { productDropdownExpanded = true }
                                    )

                                    DropdownMenu(
                                        expanded = productDropdownExpanded,
                                        onDismissRequest = { productDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("— None (Standalone Review) —", color = Color.Gray) },
                                            onClick = {
                                                editableDrafts = editableDrafts.toMutableList().also { list ->
                                                    list[index] = draft.copy(assignedProductId = null)
                                                }
                                                productDropdownExpanded = false
                                            }
                                        )
                                        products.forEach { prod ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(prod.brand, fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                },
                                                onClick = {
                                                    editableDrafts = editableDrafts.toMutableList().also { list ->
                                                        list[index] = draft.copy(assignedProductId = prod.id)
                                                    }
                                                    productDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Star Rating & Author
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rating Stars
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text("Rating:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        (1..5).forEach { star ->
                                            val isFilled = star <= draft.rating
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = "$star stars",
                                                tint = if (isFilled) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        editableDrafts = editableDrafts.toMutableList().also { list ->
                                                            list[index] = draft.copy(rating = star)
                                                        }
                                                    }
                                            )
                                        }
                                    }

                                    // Author Field (compact)
                                    OutlinedTextField(
                                        value = draft.authorName,
                                        onValueChange = { newAuthor ->
                                            editableDrafts = editableDrafts.toMutableList().also { list ->
                                                list[index] = draft.copy(authorName = newAuthor)
                                            }
                                        },
                                        label = { Text("Author", fontSize = 10.sp) },
                                        modifier = Modifier.width(130.dp),
                                        singleLine = true
                                    )
                                }

                                // Pictures Thumbnail Row (tap to open lightbox)
                                if (draft.pictures.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Attached Pictures (${draft.pictures.size}):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF64748B)
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            draft.pictures.forEachIndexed { pIdx, pic ->
                                                AsyncImage(
                                                    model = resolveImageModel(context, pic.url),
                                                    contentDescription = pic.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            lightboxImages = draft.pictures
                                                            lightboxInitialIndex = pIdx
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Clean Text Excerpt
                                val cleanExcerpt = remember(draft.bodyHtml) {
                                    draft.bodyHtml
                                        .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
                                        .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
                                        .replace(Regex("<[^>]*>"), " ")
                                        .replace(Regex("&nbsp;"), " ")
                                        .replace(Regex("\\s+"), " ")
                                        .trim()
                                        .take(140)
                                }

                                if (cleanExcerpt.isNotBlank()) {
                                    Text(
                                        text = cleanExcerpt + (if (cleanExcerpt.length >= 140) "..." else ""),
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569),
                                        lineHeight = 16.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // High-Fidelity Rendered HTML & Photos Preview Button (compact & responsive)
                                Button(
                                    onClick = { previewingDraft = draft },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEEF2FF),
                                        contentColor = Color(0xFF4F46E5)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Preview Note & Photos (${draft.pictures.size}📷, ${draft.wordCount}w)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(Modifier.height(14.dp))

                // Bottom Action Buttons
                val readyToImportCount = editableDrafts.count { it.id in selectedDraftIds }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            val selected = editableDrafts.filter { it.id in selectedDraftIds }
                            onConfirmImport(selected)
                        },
                        enabled = readyToImportCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_import_btn")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (readyToImportCount == 1) "Import 1 Review" else "Import All ($readyToImportCount Reviews)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Modal to view full rendered note with styling and images
    previewingDraft?.let { draft ->
        RenderedHtmlNotePreviewModal(
            draft = draft,
            assignedProduct = products.find { it.id == draft.assignedProductId },
            onDismiss = { previewingDraft = null },
            onImageClick = { url ->
                val foundIdx = draft.pictures.indexOfFirst { it.url == url }
                if (foundIdx != -1) {
                    lightboxImages = draft.pictures
                    lightboxInitialIndex = foundIdx
                } else {
                    lightboxImages = listOf(PictureItem("prev_img", "Photo", url))
                    lightboxInitialIndex = 0
                }
            }
        )
    }

    // Full-screen photo zoom lightbox
    lightboxImages?.let { images ->
        FullScreenPhotoLightbox(
            images = images,
            initialIndex = lightboxInitialIndex,
            onDismiss = { lightboxImages = null }
        )
    }
}

/**
 * Dedicated preview modal showing the full rendered HTML note with proper typography,
 * tables, blockquotes, checklists, and all images loaded via WebView with local file interception.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RenderedHtmlNotePreviewModal(
    draft: ImportedReviewDraft,
    assignedProduct: ProductItem?,
    onDismiss: () -> Unit,
    onImageClick: (String) -> Unit
) {
    BackHandler { onDismiss() }
    val context = LocalContext.current

    val previewHtml = remember(draft.bodyHtml, draft.title) {
        var bodyContent = draft.bodyHtml.trim()

        // Strip duplicate leading title if present
        val leadingHeadingRegex = Regex("""^\s*<h[12][^>]*>([\s\S]*?)<\/h[12]>\s*""", RegexOption.IGNORE_CASE)
        val headingMatch = leadingHeadingRegex.find(bodyContent)
        if (headingMatch != null) {
            val headingText = headingMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim().lowercase()
            val normalizedTitle = draft.title.trim().lowercase()
            if (headingText == normalizedTitle || headingText.contains(normalizedTitle) || normalizedTitle.contains(headingText)) {
                bodyContent = bodyContent.substring(headingMatch.range.last + 1).trim()
            }
        }

        // Rewrite local file and relative image URLs to virtual https://local.review URLs
        val fileImgRegex = Regex("""src=["']file:/*([^"']+)["']""", RegexOption.IGNORE_CASE)
        bodyContent = fileImgRegex.replace(bodyContent) { match ->
            val fullPath = "/" + match.groupValues[1].trimStart('/')
            val file = File(fullPath)
            if (fullPath.contains("review_images/")) {
                "src=\"https://local.review/review_images/${file.name}\""
            } else {
                "src=\"https://local.review/image?path=${URLEncoder.encode(fullPath, "UTF-8")}\""
            }
        }

        val absolutePathImgRegex = Regex("""src=["'](\/data\/[^"']+)["']""", RegexOption.IGNORE_CASE)
        bodyContent = absolutePathImgRegex.replace(bodyContent) { match ->
            val fullPath = match.groupValues[1]
            val file = File(fullPath)
            if (fullPath.contains("review_images/")) {
                "src=\"https://local.review/review_images/${file.name}\""
            } else {
                "src=\"https://local.review/image?path=${URLEncoder.encode(fullPath, "UTF-8")}\""
            }
        }

        val relativeReviewImgRegex = Regex("""src=["'](?:review_images|images)\/([^"']+)["']""", RegexOption.IGNORE_CASE)
        bodyContent = relativeReviewImgRegex.replace(bodyContent) { match ->
            val filename = match.groupValues[1]
            "src=\"https://local.review/review_images/$filename\""
        }

        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">
          <style>
            * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
            html, body {
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
              margin: 0;
              padding: 0;
              color: #1e293b;
              line-height: 1.6;
              font-size: 14px;
              background: #ffffff;
              width: 100%;
              -webkit-overflow-scrolling: touch;
            }
            .content-container {
              padding: 12px 14px 48px 14px;
              max-width: 720px;
              margin: 0 auto;
              word-wrap: break-word;
            }
            h1 { font-size: 20px; font-weight: 800; color: #0f172a; margin-top: 14px; margin-bottom: 8px; border-bottom: 1px solid #e2e8f0; padding-bottom: 6px; line-height: 1.3; }
            h2 { font-size: 17px; font-weight: 700; color: #0f172a; margin-top: 14px; margin-bottom: 8px; line-height: 1.3; }
            h3 { font-size: 15px; font-weight: 700; color: #0f172a; margin-top: 12px; margin-bottom: 6px; }
            p { margin-bottom: 10px; color: #334155; line-height: 1.6; }
            blockquote {
              border-left: 4px solid #0284c7;
              padding: 8px 12px;
              margin: 12px 0;
              background: #f0f9ff;
              border-radius: 0 8px 8px 0;
              color: #0369a1;
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
            tr:nth-child(even) td { background-color: #ffffff; }
            img {
              max-width: 100%;
              height: auto;
              border-radius: 10px;
              border: 1px solid #e2e8f0;
              margin: 10px 0;
              cursor: pointer;
              box-shadow: 0 1px 3px rgba(0,0,0,0.06);
              display: block;
            }
            ul, ol { padding-left: 20px; margin: 8px 0; color: #334155; }
            li { margin-bottom: 4px; }
            b, strong { font-weight: 700; color: #0f172a; }
            mark { background-color: #fef08a; padding: 1px 4px; border-radius: 3px; }
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
            pre code { background-color: transparent; color: inherit; padding: 0; }
            input[type="checkbox"] { accent-color: #4f46e5; margin-right: 4px; }
          </style>
        </head>
        <body>
          <div class="content-container">
            <h1>${draft.title}</h1>
            $bodyContent
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header bar
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Preview: ${draft.title}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (assignedProduct != null) "Assigned to: ${assignedProduct.name} • ${draft.pictures.size} photos" else "Standalone review • ${draft.pictures.size} photos",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Rendered HTML View with smooth scrolling & image interception
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                tag = previewHtml
                                // Prevent GPU MESA rendernode crash in virtualized container
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = false
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.allowFileAccessFromFileURLs = true
                                settings.allowUniversalAccessFromFileURLs = true
                                settings.defaultTextEncodingName = "UTF-8"
                                settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                                isVerticalScrollBarEnabled = true
                                isNestedScrollingEnabled = false
                                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

                                // Ensure touch gestures directly scroll the preview WebView
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
                                }

                                addJavascriptInterface(object {
                                    @android.webkit.JavascriptInterface
                                    fun onImageClick(url: String) {
                                        onImageClick(url)
                                    }
                                }, "AndroidBridge")

                                loadDataWithBaseURL("https://local.review/", previewHtml, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            // CRITICAL FIX: Only reload if the HTML document actually changed
                            if (webView.tag != previewHtml) {
                                webView.tag = previewHtml
                                webView.loadDataWithBaseURL("https://local.review/", previewHtml, "text/html", "UTF-8", null)
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

                // Bottom Close button
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Done Previewing", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
