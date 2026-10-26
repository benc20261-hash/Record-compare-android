package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = "Search tags, brands, or products...",
    modifier: Modifier = Modifier
) {
    var activeCategory by remember { mutableStateOf("tag") } // "tag", "brand", "product"
    var internalQuery by remember { mutableStateOf("") }
    val chipsScrollState = rememberScrollState()

    val effectiveQuery = onSearchQueryChange?.let { searchQuery } ?: internalQuery
    val handleQueryChange: (String) -> Unit = { newQ ->
        if (onSearchQueryChange != null) {
            onSearchQueryChange(newQ)
        } else {
            internalQuery = newQ
        }
    }

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

    val totalOptionsCount = tagList.size + brandList.size + productList.size

    // Auto-switch category if current category has 0 matches for search text, but another category has matches
    LaunchedEffect(effectiveQuery) {
        val q = effectiveQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            val tagMatches = tagList.count { it.lowercase().contains(q) }
            val brandMatches = brandList.count { it.lowercase().contains(q) }
            val productMatches = productList.count { it.lowercase().contains(q) }

            val currentMatches = when (activeCategory) {
                "tag" -> tagMatches
                "brand" -> brandMatches
                "product" -> productMatches
                else -> 0
            }

            if (currentMatches == 0) {
                if (productMatches > 0) {
                    activeCategory = "product"
                } else if (brandMatches > 0) {
                    activeCategory = "brand"
                } else if (tagMatches > 0) {
                    activeCategory = "tag"
                }
            }
        }
    }

    // Filter items by active category and search text
    val displayedItems = remember(allItems, activeCategory, effectiveQuery) {
        val base = when (activeCategory) {
            "tag" -> allItems.filter { it.type == FilterOptionType.TAG }
            "brand" -> allItems.filter { it.type == FilterOptionType.BRAND }
            "product" -> allItems.filter { it.type == FilterOptionType.PRODUCT }
            else -> allItems
        }
        val q = effectiveQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            base.filter { it.label.lowercase().contains(q) }
        } else {
            base
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title with blue tag icon on left, total options count on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sell,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = Color(0xFF0F172A)
                    )
                }
                Text(
                    text = "($totalOptionsCount total options)",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Search Bar Input (Integrated into card, matching screenshots)
            OutlinedTextField(
                value = effectiveQuery,
                onValueChange = handleQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("catalog_search_input"),
                placeholder = {
                    Text(
                        text = searchPlaceholder,
                        fontSize = 12.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(17.dp)
                    )
                },
                trailingIcon = {
                    if (effectiveQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { handleQueryChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                )
            )

            // Category Segmented Tabs (Tags, Brands, Products)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tags Tab (Indigo when selected)
                CategoryTabPill(
                    icon = Icons.Default.Sell,
                    label = "Tags",
                    count = tagList.size,
                    isSelected = activeCategory == "tag",
                    selectedBgColor = Color(0xFF4F46E5),
                    selectedIconTint = Color.White,
                    unselectedIconTint = Color(0xFF818CF8),
                    onClick = { activeCategory = "tag" }
                )

                // Brands Tab (Amber when selected)
                CategoryTabPill(
                    icon = Icons.Default.Storefront,
                    label = "Brands",
                    count = brandList.size,
                    isSelected = activeCategory == "brand",
                    selectedBgColor = Color(0xFFD97706),
                    selectedIconTint = Color.White,
                    unselectedIconTint = Color(0xFFF59E0B),
                    onClick = { activeCategory = "brand" }
                )

                // Products Tab (Emerald/Green when selected)
                CategoryTabPill(
                    icon = Icons.Default.Inventory2,
                    label = "Products",
                    count = productList.size,
                    isSelected = activeCategory == "product",
                    selectedBgColor = Color(0xFF059669),
                    selectedIconTint = Color.White,
                    unselectedIconTint = Color(0xFF10B981),
                    onClick = { activeCategory = "product" }
                )
            }

            // Filtered Active Badge (e.g. "Filtered: Plaud ✕")
            val isFiltered = selectedTag.isNotBlank() && !selectedTag.equals("All", ignoreCase = true)
            if (isFiltered) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "Filtered:",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = selectedTag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear filter",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier
                                .size(13.dp)
                                .clickable { onSelectTag("All") }
                        )
                    }
                }
            }

            // Dynamic Filter Chips Row (Scrollable horizontally)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipsScrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 'All' Chip
                val isAllSelected = selectedTag.equals("All", ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAllSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                    border = BorderStroke(
                        1.dp,
                        if (isAllSelected) Color(0xFF4338CA) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .clickable { onSelectTag("All") }
                        .testTag("tag_chip_all")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            fontSize = 11.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllSelected) Color.White else Color(0xFF334155)
                        )
                    }
                }

                // Dynamic Matching Chips
                displayedItems.forEach { item ->
                    val isSelected = selectedTag.equals(item.label, ignoreCase = true)
                    val icon = when (item.type) {
                        FilterOptionType.TAG -> Icons.Default.Sell
                        FilterOptionType.BRAND -> Icons.Default.Storefront
                        FilterOptionType.PRODUCT -> Icons.Default.Inventory2
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFF8FAFC),
                        border = BorderStroke(
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
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = when (item.type) {
                                        FilterOptionType.TAG -> Color(0xFF818CF8)
                                        FilterOptionType.BRAND -> Color(0xFFF59E0B)
                                        FilterOptionType.PRODUCT -> Color(0xFF10B981)
                                    },
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
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
    selectedIconTint: Color,
    unselectedIconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) selectedBgColor else Color(0xFFF1F5F9),
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) selectedIconTint else unselectedIconTint,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) Color.White else Color(0xFF475569)
            )
            Text(
                text = "($count)",
                fontSize = 10.sp,
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF94A3B8)
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
