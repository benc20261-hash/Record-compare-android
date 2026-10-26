package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.SpecificationItem
import com.example.model.resolveImageModel
import com.example.ui.components.ButtonText
import com.example.ui.components.ComparisonTray
import com.example.ui.components.DeviceGraphicPlaceholder
import com.example.ui.components.TagBrandProductFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    products: List<ProductItem>,
    specifications: List<SpecificationItem>,
    reviews: List<ReviewItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    comparedIds: Set<String>,
    onToggleCompare: (String) -> Unit,
    onClearComparison: () -> Unit = {},
    onProductClick: (ProductItem) -> Unit,
    onEditProduct: (ProductItem) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onNavigateToSmartAdd: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToCompare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var catalogViewMode by remember { mutableStateOf("grid") }
    var selectedTagOrBrand by remember { mutableStateOf<String?>("All") }

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

    // Filtered products list
    val filteredProducts = remember(products, searchQuery, specifications, selectedTagOrBrand) {
        val q = searchQuery.trim().lowercase()
        val isAll = selectedTagOrBrand.isNullOrBlank() || selectedTagOrBrand.equals("All", ignoreCase = true)
        products.filter { p ->
            val matchesTag = isAll ||
                    p.tags.any { it.equals(selectedTagOrBrand, ignoreCase = true) } ||
                    p.brand.equals(selectedTagOrBrand, ignoreCase = true) ||
                    p.name.equals(selectedTagOrBrand, ignoreCase = true)

            val matchesQuery = if (q.isBlank()) true else {
                p.name.lowercase().contains(q) ||
                        p.brand.lowercase().contains(q) ||
                        p.overview.lowercase().contains(q) ||
                        p.tags.any { it.lowercase().contains(q) } ||
                        specifications.filter { it.productId == p.id || it.productTitle == p.name }
                            .any { it.specName.lowercase().contains(q) || it.details.lowercase().contains(q) }
            }

            matchesTag && matchesQuery
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.AppColors.CanvasBackground)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = if (comparedIds.isNotEmpty()) {
                com.example.ui.theme.AppSpacing.ScreenWithFloatingTrayPadding
            } else {
                com.example.ui.theme.AppSpacing.ScreenContentPaddingScrollable
            },
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Filter products by tag / brand / product Card with integrated search
            item(span = { GridItemSpan(maxLineSpan) }) {
                TagBrandProductFilter(
                    label = "Filter products by tag / brand / product",
                    tags = allTags,
                    brands = allBrands,
                    products = allProductNames,
                    selectedTag = selectedTagOrBrand ?: "All",
                    onSelectTag = { tag ->
                        selectedTagOrBrand = if (tag.equals("All", ignoreCase = true)) "All" else tag
                    },
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchChange,
                    searchPlaceholder = "Search tags, brands, or products...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Actions Row: View Toggle, Smart Add Product, Add Product
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Grid / Table View Switcher
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Grid Button
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (catalogViewMode == "grid") Color.White else Color.Transparent,
                                shadowElevation = if (catalogViewMode == "grid") 1.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clickable { catalogViewMode = "grid" }
                                    .testTag("catalog_view_mode_grid")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.GridView,
                                        contentDescription = "Grid view",
                                        tint = if (catalogViewMode == "grid") Color(0xFF4F46E5) else Color(0xFF64748B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        "Grid",
                                        fontSize = 11.sp,
                                        fontWeight = if (catalogViewMode == "grid") FontWeight.Bold else FontWeight.Medium,
                                        color = if (catalogViewMode == "grid") Color(0xFF0F172A) else Color(0xFF64748B)
                                    )
                                }
                            }

                            // Table Button
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (catalogViewMode == "table") Color.White else Color.Transparent,
                                shadowElevation = if (catalogViewMode == "table") 1.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clickable { catalogViewMode = "table" }
                                    .testTag("catalog_view_mode_table")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FormatListBulleted,
                                        contentDescription = "Table view",
                                        tint = if (catalogViewMode == "table") Color(0xFF4F46E5) else Color(0xFF64748B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        "Table",
                                        fontSize = 11.sp,
                                        fontWeight = if (catalogViewMode == "table") FontWeight.Bold else FontWeight.Medium,
                                        color = if (catalogViewMode == "table") Color(0xFF0F172A) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    // Smart Add Product Button
                    Button(
                        onClick = onNavigateToSmartAdd,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("catalog_smart_add_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        ButtonText(
                            text = "Smart Add Product",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Add Product Button
                    Button(
                        onClick = onNavigateToAddProduct,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("catalog_add_product_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        ButtonText(
                            text = "Add Product",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // 4. Products List or Grid
            if (filteredProducts.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No products matching \"${selectedTagOrBrand ?: searchQuery}\"",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else if (catalogViewMode == "table") {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CatalogTableView(
                        products = filteredProducts,
                        specifications = specifications,
                        reviews = reviews,
                        comparedIds = comparedIds,
                        onToggleCompare = onToggleCompare,
                        onProductClick = onProductClick
                    )
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
            val isCompared = product.id in comparedIds
            val prodSpecs = specifications.filter { it.productId == product.id || it.productTitle == product.name }
            val prodReviews = reviews.filter { product.id in it.productIds || product.name in it.productTitles }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProductClick(product) }
                    .testTag("product_card_${product.id}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Brand Badge on left, + Compare button on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF334155)
                        ) {
                            Text(
                                text = product.brand.uppercase().ifBlank { "GENERIC" },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                letterSpacing = 0.5.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { onToggleCompare(product.id) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isCompared) Color(0xFFEEF2FF) else Color.White,
                                contentColor = if (isCompared) Color(0xFF4F46E5) else Color(0xFF0F172A)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCompared) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                if (isCompared) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isCompared) "Compared" else "Compare",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Center Visual Graphic / Photo Container (Catalog module.PNG)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFAFAFA),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val firstPic = product.pictures.firstOrNull()?.url
                            val firstPicName = product.pictures.firstOrNull()?.name
                            val context = LocalContext.current

                            if (!firstPic.isNullOrBlank() || !firstPicName.isNullOrBlank()) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(resolveImageModel(context, firstPic ?: "", preferredName = firstPicName))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Fit,
                                    loading = {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF6366F1)
                                            )
                                        }
                                    },
                                    error = {
                                        DeviceGraphicPlaceholder(
                                            title = product.name,
                                            brand = product.brand,
                                            tags = product.tags,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                            } else {
                                DeviceGraphicPlaceholder(
                                    title = product.name,
                                    brand = product.brand,
                                    tags = product.tags,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Tags row
                    if (product.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            product.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Product Title
                    Text(
                        text = product.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    // Specification Preview Block (Catalog module.PNG / Catalog module 2.PNG)
                    if (prodSpecs.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                prodSpecs.take(2).forEach { spec ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = spec.specName,
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = if (spec.details.isNotBlank()) {
                                                buildString {
                                                    append(spec.details)
                                                    if (!spec.unit.isNullOrBlank()) append(" ${spec.unit}")
                                                }
                                            } else "—",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card Footer: Reviews / Specs count and Details > link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${prodReviews.size} reviews",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Text("•", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                            Icon(
                                Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${prodSpecs.size} specs",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onProductClick(product) }
                        ) {
                            Text(
                                text = "Details",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4F46E5)
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

    if (comparedIds.isNotEmpty()) {
        val comparedProducts = remember(comparedIds, products) {
            products.filter { it.id in comparedIds }
        }
        ComparisonTray(
            selectedProducts = comparedProducts,
            onRemoveProduct = { onToggleCompare(it) },
            onClearAll = onClearComparison,
            onCompareNow = onNavigateToCompare,
            onOpenSmartAdd = onNavigateToSmartAdd,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
}

/**
 * Responsive Desktop & Tablet Catalog Data Table matching Productc webapp table view.
 * Displays comparison checkboxes, thumbnails, product names, brands, category tags,
 * overview snippets, specifications count, reviews count, and view details action.
 */
@Composable
fun CatalogTableView(
    products: List<ProductItem>,
    specifications: List<SpecificationItem>,
    reviews: List<ReviewItem>,
    comparedIds: Set<String>,
    onToggleCompare: (String) -> Unit,
    onProductClick: (ProductItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        val horizontalScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            // Table Header Row
            Row(
                modifier = Modifier
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .widthIn(min = 960.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compare",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    modifier = Modifier.width(68.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = "Product",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    modifier = Modifier.width(220.dp)
                )
                Text(
                    text = "Brand",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    modifier = Modifier.width(130.dp)
                )
                Text(
                    text = "Category",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    modifier = Modifier.width(160.dp)
                )
                Text(
                    text = "Overview",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    modifier = Modifier.width(200.dp)
                )
                Text(
                    text = "Specs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    modifier = Modifier.width(70.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = "Reviews",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    modifier = Modifier.width(70.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = "Action",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    modifier = Modifier.width(100.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            // Table Body Rows
            products.forEachIndexed { index, product ->
                val isCompared = product.id in comparedIds
                val prodSpecs = specifications.filter { it.productId == product.id || it.productTitle == product.name }
                val prodReviews = reviews.filter { product.id in it.productIds || product.name in it.productTitles }

                Row(
                    modifier = Modifier
                        .background(if (index % 2 == 1) Color(0xFFFAFAFA) else Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .widthIn(min = 960.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Compare Checkbox
                    Box(modifier = Modifier.width(68.dp), contentAlignment = Alignment.Center) {
                        Checkbox(
                            checked = isCompared,
                            onCheckedChange = { onToggleCompare(product.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF4F46E5),
                                uncheckedColor = Color(0xFFCBD5E1)
                            )
                        )
                    }

                    // 2. Product Thumbnail + Name
                    Row(
                        modifier = Modifier
                            .width(220.dp)
                            .clickable { onProductClick(product) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.size(40.dp)
                        ) {
                            DeviceGraphicPlaceholder(
                                title = product.name,
                                brand = product.brand,
                                tags = product.tags,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 3. Brand
                    Text(
                        text = product.brand.ifBlank { "—" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155),
                        modifier = Modifier.width(130.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 4. Category Tags
                    Row(
                        modifier = Modifier
                            .width(160.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (product.tags.isEmpty()) {
                            Text("—", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        } else {
                            product.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 5. Overview snippet
                    Text(
                        text = product.overview.ifBlank { "No overview provided." },
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.width(200.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 6. Specs count
                    Text(
                        text = "${prodSpecs.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                        modifier = Modifier.width(70.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // 7. Reviews count
                    Text(
                        text = "${prodReviews.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                        modifier = Modifier.width(70.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // 8. Action: View Details
                    Box(modifier = Modifier.width(100.dp), contentAlignment = Alignment.CenterEnd) {
                        TextButton(
                            onClick = { onProductClick(product) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF4F46E5))
                        ) {
                            Text("Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (index < products.size - 1) {
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                }
            }
        }
    }
}
