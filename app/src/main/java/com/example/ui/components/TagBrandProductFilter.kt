package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FilterOptionType {
    TAG, BRAND, PRODUCT
}

data class FilterOptionItem(
    val label: String,
    val type: FilterOptionType
)

@Composable
fun TagBrandProductFilter(
    label: String = "Filter products by tag / brand / product",
    tags: List<String> = emptyList(),
    brands: List<String> = emptyList(),
    products: List<String> = emptyList(),
    selectedTag: String = "All",
    onSelectTag: (String) -> Unit,
    searchPlaceholder: String = "Search tags, brands, or products...",
    modifier: Modifier = Modifier
) {
    var activeCategory by remember { mutableStateOf("all") } // "all", "tag", "brand", "product"
    var searchQuery by remember { mutableStateOf("") }
    val chipsScrollState = rememberScrollState()

    // Aggregate unique items
    val (allItems, tagList, brandList, productList) = remember(tags, brands, products) {
        val tagSet = linkedSetOf<String>()
        val brandSet = linkedSetOf<String>()
        val productSet = linkedSetOf<String>()

        tags.forEach {
            val t = it.trim()
            if (t.isNotBlank() && !t.equals("All", ignoreCase = true)) tagSet.add(t)
        }
        brands.forEach {
            val b = it.trim()
            if (b.isNotBlank() && !b.equals("All", ignoreCase = true)) brandSet.add(b)
        }
        products.forEach {
            val p = it.trim()
            if (p.isNotBlank() && !p.equals("All", ignoreCase = true)) productSet.add(p)
        }

        val sortedTags = tagSet.sortedWith(String.CASE_INSENSITIVE_ORDER)
        val sortedBrands = brandSet.sortedWith(String.CASE_INSENSITIVE_ORDER)
        val sortedProducts = productSet.sortedWith(String.CASE_INSENSITIVE_ORDER)

        val combined = mutableListOf<FilterOptionItem>()
        sortedTags.forEach { combined.add(FilterOptionItem(it, FilterOptionType.TAG)) }
        sortedBrands.forEach { combined.add(FilterOptionItem(it, FilterOptionType.BRAND)) }
        sortedProducts.forEach { combined.add(FilterOptionItem(it, FilterOptionType.PRODUCT)) }

        Tuple4(combined, sortedTags, sortedBrands, sortedProducts)
    }

    // Filter items by category and search text
    val displayedItems = remember(allItems, activeCategory, searchQuery) {
        var base = when (activeCategory) {
            "tag" -> allItems.filter { it.type == FilterOptionType.TAG }
            "brand" -> allItems.filter { it.type == FilterOptionType.BRAND }
            "product" -> allItems.filter { it.type == FilterOptionType.PRODUCT }
            else -> allItems
        }
        val q = searchQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            base = base.filter { it.label.lowercase().contains(q) }
        }
        base
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth().wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Header Row: Tag Icon + Title + Count + Search Bar
            Row(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sell,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "(${allItems.size} total options)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Compact Search Input
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .widthIn(min = 110.dp, max = 150.dp)
                        .height(26.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 10.5.sp,
                                color = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = searchPlaceholder.take(20),
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF64748B),
                                modifier = Modifier
                                    .size(11.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }
            }

            // Category Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Category
                CategoryTabPill(
                    icon = Icons.Default.AutoAwesome,
                    label = "All",
                    count = allItems.size,
                    isSelected = activeCategory == "all",
                    selectedBgColor = Color(0xFF0F172A),
                    onClick = { activeCategory = "all" }
                )

                // Tags Category
                CategoryTabPill(
                    icon = Icons.Default.Sell,
                    label = "Tags",
                    count = tagList.size,
                    isSelected = activeCategory == "tag",
                    selectedBgColor = Color(0xFF4F46E5),
                    onClick = { activeCategory = "tag" }
                )

                // Brands Category
                CategoryTabPill(
                    icon = Icons.Default.Storefront,
                    label = "Brands",
                    count = brandList.size,
                    isSelected = activeCategory == "brand",
                    selectedBgColor = Color(0xFFD97706),
                    onClick = { activeCategory = "brand" }
                )

                // Products Category (if present)
                if (productList.isNotEmpty()) {
                    CategoryTabPill(
                        icon = Icons.Default.Inventory2,
                        label = "Products",
                        count = productList.size,
                        isSelected = activeCategory == "product",
                        selectedBgColor = Color(0xFF2563EB),
                        onClick = { activeCategory = "product" }
                    )
                }
            }

            // Chips Row: Horizontal Scrollable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipsScrollState),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 'All' Chip
                val isAllSelected = selectedTag.equals("All", ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (isAllSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAllSelected) Color(0xFF4338CA) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .clickable { onSelectTag("All") }
                        .testTag("tag_chip_all")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (isAllSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = "All",
                            fontSize = 10.5.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllSelected) Color.White else Color(0xFF334155)
                        )
                    }
                }

                // Individual Filter Chips
                displayedItems.forEach { item ->
                    val isSelected = selectedTag.equals(item.label, ignoreCase = true)
                    val icon = when (item.type) {
                        FilterOptionType.TAG -> Icons.Default.Sell
                        FilterOptionType.BRAND -> Icons.Default.Storefront
                        FilterOptionType.PRODUCT -> Icons.Default.Inventory2
                    }

                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF4338CA) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .clickable {
                                if (isSelected) {
                                    onSelectTag("All")
                                } else {
                                    onSelectTag(item.label)
                                }
                            }
                            .testTag("tag_chip_${item.label}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = when (item.type) {
                                        FilterOptionType.TAG -> Color(0xFF818CF8)
                                        FilterOptionType.BRAND -> Color(0xFFF59E0B)
                                        FilterOptionType.PRODUCT -> Color(0xFF60A5FA)
                                    },
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Text(
                                text = item.label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    isSelected: Boolean,
    selectedBgColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = if (isSelected) selectedBgColor else Color(0xFFF1F5F9),
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF64748B),
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) Color.White else Color(0xFF475569)
            )
            Text(
                text = "($count)",
                fontSize = 9.5.sp,
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF94A3B8)
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
