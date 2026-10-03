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
import kotlinx.coroutines.launch
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.SpecificationItem
import com.example.model.resolveImageModel
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
    var isGridView by remember { mutableStateOf(false) }
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
            .background(Color(0xFFF8FAFC))
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (isGridView) 240.dp else 340.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = if (comparedIds.isNotEmpty()) 130.dp else 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 1. Search Box (Catalog module.PNG)
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_search_input"),
                placeholder = {
                    Text(
                        "Search products, brands, or specifica...",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }

        // 2. Actions Row: View Toggle, Smart Add Product, Add Product (Catalog module.PNG)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Grid / List Toggle Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.size(46.dp).clickable { isGridView = !isGridView }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = "Grid view",
                                tint = if (isGridView) Color(0xFF4F46E5) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                Icons.Default.FormatListBulleted,
                                contentDescription = "List view",
                                tint = if (!isGridView) Color(0xFF4F46E5) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Smart Add Product Button (Dark pill button with star icon)
                Button(
                    onClick = onNavigateToSmartAdd,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("catalog_smart_add_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Smart Add\nProduct",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp
                    )
                }

                // Add Product Button (Purple pill button with plus icon)
                Button(
                    onClick = onNavigateToAddProduct,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("catalog_add_product_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Add\nProduct",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // 2b. Filter products by tag / brand / product Card (Screenshot 1)
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
                searchPlaceholder = "Search tags, brands, or products..."
            )
        }

        // 3. Product Cards List (Catalog module.PNG & Catalog module 2.PNG)
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
                            if (!firstPic.isNullOrBlank()) {
                                val context = LocalContext.current
                                AsyncImage(
                                    model = resolveImageModel(context, firstPic),
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    DeviceGraphicPlaceholder(
                                        title = product.name,
                                        brand = product.brand,
                                        tags = product.tags,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFE2E8F0).copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = product.brand.uppercase().ifBlank { "GENERIC" },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = product.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Ready for photo upload",
                                        fontSize = 8.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
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
