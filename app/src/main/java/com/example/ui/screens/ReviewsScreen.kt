package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import coil.compose.AsyncImage
import com.example.model.PictureItem
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.resolveImageModel
import com.example.ui.components.FullScreenPhotoLightbox
import com.example.ui.components.TagBrandProductFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    reviews: List<ReviewItem>,
    products: List<ProductItem>,
    onReviewClick: (ReviewItem) -> Unit,
    onEditReview: (ReviewItem) -> Unit,
    onWriteReview: () -> Unit,
    onDeleteReview: (String) -> Unit,
    onProductClick: (ProductItem) -> Unit = {},
    onImportFolder: (Uri) -> Unit = {},
    onImportZip: (Uri) -> Unit = {},
    onImportHtml: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableStateOf<String?>(null) }
    var isProductDropdownOpen by remember { mutableStateOf(false) }
    var dropdownSearchQuery by remember { mutableStateOf("") }
    var lightboxImages by remember { mutableStateOf<List<PictureItem>?>(null) }
    var lightboxInitialIndex by remember { mutableIntStateOf(0) }
    var showImportMenu by remember { mutableStateOf(false) }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { onImportFolder(it) }
    }
    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onImportZip(it) }
    }
    val htmlLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onImportHtml(it) }
    }

    // Map product IDs to review counts
    val productReviewCounts = remember(products, reviews) {
        products.associate { prod ->
            prod.id to reviews.count { r ->
                prod.id in r.productIds || prod.name in r.productTitles
            }
        }
    }

    var selectedFilterTag by remember { mutableStateOf("All") }

    val allTags = remember(products) {
        val set = linkedSetOf<String>()
        products.forEach { p -> p.tags.forEach { t -> if (t.isNotBlank()) set.add(t) } }
        set.toList()
    }
    val allBrands = remember(products) {
        val set = linkedSetOf<String>()
        products.forEach { p -> if (p.brand.isNotBlank() && !p.brand.equals("Generic", ignoreCase = true)) set.add(p.brand) }
        set.toList()
    }
    val allProductNames = remember(products) {
        products.map { it.name }
    }

    // Filter reviews
    val filteredReviews = remember(reviews, searchQuery, selectedProductId, selectedFilterTag, products) {
        val isAll = selectedFilterTag.equals("All", ignoreCase = true)
        reviews.filter { r ->
            val matchesProduct = selectedProductId == null ||
                    selectedProductId in r.productIds ||
                    products.find { it.id == selectedProductId }?.name in r.productTitles

            val matchesFilterTag = isAll || run {
                val matchedProds = products.filter { p ->
                    p.id in r.productIds || p.name in r.productTitles
                }
                matchedProds.any { p ->
                    p.tags.any { it.equals(selectedFilterTag, ignoreCase = true) } ||
                            p.brand.equals(selectedFilterTag, ignoreCase = true) ||
                            p.name.equals(selectedFilterTag, ignoreCase = true)
                } || r.title.contains(selectedFilterTag, ignoreCase = true) ||
                        r.body.contains(selectedFilterTag, ignoreCase = true)
            }

            val q = searchQuery.trim().lowercase()
            val matchesQuery = if (q.isBlank()) true else {
                r.title.lowercase().contains(q) ||
                        r.body.lowercase().contains(q) ||
                        r.authorName.lowercase().contains(q) ||
                        r.productTitles.any { it.lowercase().contains(q) }
            }
            matchesProduct && matchesFilterTag && matchesQuery
        }
    }

    // Filter products for dropdown
    val filteredDropdownProducts = remember(products, dropdownSearchQuery) {
        val q = dropdownSearchQuery.trim().lowercase()
        if (q.isBlank()) products else {
            products.filter {
                it.name.lowercase().contains(q) || it.brand.lowercase().contains(q)
            }
        }
    }

    val selectedProductName = remember(selectedProductId, products) {
        if (selectedProductId == null) null
        else products.find { it.id == selectedProductId }?.name
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header: Community User Reviews (IMG_9631.PNG)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Community User Reviews",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            lineHeight = 24.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Real feedback and benchmark impressions from Teable UserReview table",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp
                        )
                    }

                    // Total Reviews Pill Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEEF2FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E7FF))
                    ) {
                        Text(
                            text = "${reviews.size} Total Reviews",
                            color = Color(0xFF4F46E5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Review Actions Row: Batch Import HTML / Folder & + Write a Review (Screenshot 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Batch Import HTML / Folder Menu
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showImportMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0F172A)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("import_html_note_btn")
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Batch Import HTML / Folder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Import UpNote / HTML Folder", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Select nested folder with HTML reviews & images", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DriveFolderUpload, contentDescription = null, tint = Color(0xFF4F46E5))
                                },
                                onClick = {
                                    showImportMenu = false
                                    folderLauncher.launch(null)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Import Note ZIP Archive (.zip)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Extract and import HTML notes & pictures from ZIP", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = Color(0xFF0284C7))
                                },
                                onClick = {
                                    showImportMenu = false
                                    zipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Import Single .HTML Note (.html)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Import individual HTML or HTM review file", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF10B981))
                                },
                                onClick = {
                                    showImportMenu = false
                                    htmlLauncher.launch(arrayOf("text/html", "text/plain"))
                                }
                            )
                        }
                    }

                    // + Write a Review Primary Button
                    Button(
                        onClick = onWriteReview,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("write_review_btn")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Write a Review",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 2. Search and Product Filter Card (IMG_9631.PNG)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Filter Reviews",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Filter reviews by keyword, product name, or author tag.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search reviews by keyword or devic...",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // Product Filter Dropdown Row with Funnel icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { isProductDropdownOpen = !isProductDropdownOpen },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isProductDropdownOpen) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = selectedProductName ?: "All Products (${reviews.size})",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    Icon(
                                        if (isProductDropdownOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Expandable Product Filter Popover (IMG_9631.PNG)
                            DropdownMenu(
                                expanded = isProductDropdownOpen,
                                onDismissRequest = { isProductDropdownOpen = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(Color.White)
                            ) {
                                // Search inside dropdown
                                OutlinedTextField(
                                    value = dropdownSearchQuery,
                                    onValueChange = { dropdownSearchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Type to filter products...",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )

                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                // "All Products" Option
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Inventory2,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4F46E5),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    "All Products (${reviews.size})",
                                                    fontWeight = if (selectedProductId == null) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                            if (selectedProductId == null) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color(0xFF4F46E5),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedProductId = null
                                        isProductDropdownOpen = false
                                    }
                                )

                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                // Product List Items with count badge
                                filteredDropdownProducts.forEach { prod ->
                                    val count = productReviewCounts[prod.id] ?: 0
                                    val isChosen = selectedProductId == prod.id

                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFF1F5F9),
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                Icons.Default.Devices,
                                                                contentDescription = null,
                                                                tint = Color(0xFF64748B),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                    Column {
                                                        Text(
                                                            text = prod.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                                            color = Color(0xFF0F172A),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = prod.brand,
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                }

                                                // Count badge
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFF1F5F9),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "$count",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF475569)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedProductId = prod.id
                                            isProductDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2b. Filter reviews by tag / brand / product (Screenshot 3)
        item(span = { GridItemSpan(maxLineSpan) }) {
            TagBrandProductFilter(
                label = "Filter reviews by tag / brand / product",
                tags = allTags,
                brands = allBrands,
                products = allProductNames,
                selectedTag = selectedFilterTag,
                onSelectTag = { tag ->
                    selectedFilterTag = tag
                },
                searchPlaceholder = "Search tags, brands, or products..."
            )
        }

        // 3. Review Cards Feed (IMG_9632.PNG)
        if (filteredReviews.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.RateReview,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            "No reviews found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "Try adjusting your search query or product filter.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        items(filteredReviews, key = { it.id }) { review ->
            val linkedProductTitle = review.productTitles.firstOrNull() ?: "Product"
            val linkedProduct = products.find { it.name == linkedProductTitle || it.id in review.productIds }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReviewClick(review) }
                    .testTag("review_card_${review.id}")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top: Product Pill Badge with External Link (IMG_9632.PNG)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.clickable {
                                if (linkedProduct != null) onProductClick(linkedProduct)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = linkedProductTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5)
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    // Title
                    Text(
                        text = review.title.ifBlank { "Review for $linkedProductTitle" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    // Reviewer Metadata Row (Verified Reviewer, Stars, View/Edit/Delete actions)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Avatar circle 'V'
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = review.authorName.take(1).uppercase().ifBlank { "V" },
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "Verified Reviewer",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )

                            // 5 Gold Stars & 5.0 Rating Text
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                repeat(5) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "5.0",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }

                        // Right Actions: View, Edit, Delete (IMG_9632.PNG)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = { onReviewClick(review) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = "View",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("View", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                            }

                            IconButton(
                                onClick = { onEditReview(review) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDeleteReview(review.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Content Preview Container
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Strip HTML tags for clean card excerpt
                        val cleanBody = remember(review.body) {
                            review.body.replace(Regex("<[^>]*>"), " ").trim()
                        }
                        Text(
                            text = cleanBody,
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // Picture thumbnails gallery (Screenshot 8.png)
                    val reviewPictures = remember(review) {
                        val list = mutableListOf<PictureItem>()
                        if (review.pictures.isNotEmpty()) list.addAll(review.pictures)
                        val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                        imgRegex.findAll(review.body).forEachIndexed { idx, match ->
                            val url = match.groupValues[1]
                            if (list.none { it.url == url }) {
                                list.add(PictureItem("card_img_$idx", "Photo ${list.size + 1}", url))
                            }
                        }
                        list
                    }

                    if (reviewPictures.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            reviewPictures.take(6).forEachIndexed { idx, pic ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 68.dp, height = 68.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .clickable {
                                            lightboxImages = reviewPictures
                                            lightboxInitialIndex = idx
                                        }
                                ) {
                                    AsyncImage(
                                        model = resolveImageModel(context, pic.url),
                                        contentDescription = pic.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Card Footer (IMG_9632.PNG)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Synchronized from Teable UserReview",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Linked...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4F46E5)
                        )
                    }
                }
            }
        }
    }

    // Full-Screen Photo Lightbox Modal
    lightboxImages?.let { images ->
        FullScreenPhotoLightbox(
            images = images,
            initialIndex = lightboxInitialIndex,
            onDismiss = { lightboxImages = null }
        )
    }
}
