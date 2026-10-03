package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.SpecificationItem
import com.example.model.resolveImageModel
import com.example.ui.components.DeviceGraphicPlaceholder
import com.example.ui.components.TagBrandProductFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    allProducts: List<ProductItem>,
    specifications: List<SpecificationItem>,
    reviews: List<ReviewItem>,
    comparedIds: Set<String>,
    onToggleCompare: (String) -> Unit = {},
    onSetComparedIds: (List<String>) -> Unit = {},
    onProductClick: (ProductItem) -> Unit,
    onEditProduct: (ProductItem) -> Unit = {},
    onReviewClick: (ReviewItem) -> Unit = {},
    onOpenAddReview: (ProductItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Record slot product IDs (nullable: null means empty slot waiting for selection)
    var recordSlots by remember {
        val initialList = mutableListOf<String?>()
        if (comparedIds.isNotEmpty()) {
            comparedIds.forEach { initialList.add(it) }
        }
        while (initialList.size < 2) {
            initialList.add(null)
        }
        mutableStateOf(initialList)
    }

    // Synchronize slots from external comparedIds only when active items differ
    LaunchedEffect(comparedIds) {
        val activeInSlots = recordSlots.filterNotNull()
        val incomingList = comparedIds.toList()
        if (activeInSlots != incomingList) {
            val newList = mutableListOf<String?>()
            if (incomingList.isNotEmpty()) {
                incomingList.forEach { newList.add(it) }
            }
            while (newList.size < 2) {
                newList.add(null)
            }
            recordSlots = newList
        }
    }

    // Filter by tag or brand in the slot header
    var filterSearchQuery by remember { mutableStateOf("") }
    var selectedFilterTag by remember { mutableStateOf("All") }
    var selectedCategoryFilter by remember { mutableStateOf("All") } // "All", "Tags", "Brands"

    // Slot picker dialog state
    var slotIndexToPick by remember { mutableStateOf<Int?>(null) }
    var fullScreenPictureUrl by remember { mutableStateOf<String?>(null) }

    // Higher / Lower sorting preferences per spec (defaults to "higher")
    val specSortModes = remember { mutableStateMapOf<String, String>() }

    val tagList = remember(allProducts) {
        val set = linkedSetOf<String>()
        allProducts.forEach { p -> p.tags.forEach { if (it.isNotBlank()) set.add(it) } }
        set.toList().sorted()
    }
    val brandList = remember(allProducts) {
        val set = linkedSetOf<String>()
        allProducts.forEach { p -> if (p.brand.isNotBlank()) set.add(p.brand) }
        set.toList().sorted()
    }

    // Collect all filter options (Tags + Brands)
    val allOptions = remember(allProducts, tagList, brandList) {
        val list = mutableListOf("All")
        list.addAll(brandList)
        list.addAll(tagList)
        list.distinct()
    }

    val visibleChips = remember(selectedCategoryFilter, tagList, brandList, allOptions, filterSearchQuery) {
        val baseList = when (selectedCategoryFilter) {
            "Tags" -> listOf("All") + tagList
            "Brands" -> listOf("All") + brandList
            else -> allOptions
        }
        val q = filterSearchQuery.trim().lowercase()
        if (q.isBlank()) baseList
        else baseList.filter { it.equals("All", ignoreCase = true) || it.lowercase().contains(q) }
    }

    // Filtered products for selection dialog
    val dialogFilteredProducts = remember(allProducts, filterSearchQuery, selectedFilterTag) {
        allProducts.filter { p ->
            val matchesTag = selectedFilterTag == "All" ||
                    p.tags.contains(selectedFilterTag) ||
                    p.brand.equals(selectedFilterTag, ignoreCase = true)
            val q = filterSearchQuery.trim().lowercase()
            val matchesSearch = q.isBlank() ||
                    p.name.lowercase().contains(q) ||
                    p.brand.lowercase().contains(q) ||
                    p.tags.any { it.lowercase().contains(q) }
            matchesTag && matchesSearch
        }
    }

    // Active products currently chosen in slots
    val activeProducts = remember(recordSlots, allProducts) {
        recordSlots.mapNotNull { id -> if (id != null) allProducts.find { it.id == id } else null }
    }

    // Specifications belonging to the active products
    val activeSpecs = remember(activeProducts, specifications) {
        val activeIds = activeProducts.map { it.id }.toSet()
        val activeTitles = activeProducts.map { it.name }.toSet()
        specifications.filter { it.productId in activeIds || it.productTitle in activeTitles }
    }

    val distinctSpecNames = remember(activeSpecs) {
        activeSpecs.map { it.specName }.distinct()
    }

    // Full Screen Picture Dialog
    if (fullScreenPictureUrl != null) {
        Dialog(onDismissRequest = { fullScreenPictureUrl = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = resolveImageModel(context, fullScreenPictureUrl ?: ""),
                        contentDescription = "Full picture",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                    IconButton(
                        onClick = { fullScreenPictureUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }

    // Slot Record Picker Dialog
    if (slotIndexToPick != null) {
        val slotIndex = slotIndexToPick!!
        Dialog(onDismissRequest = { slotIndexToPick = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Record for Slot ${slotIndex + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        IconButton(onClick = { slotIndexToPick = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }

                    OutlinedTextField(
                        value = filterSearchQuery,
                        onValueChange = { filterSearchQuery = it },
                        placeholder = { Text("Search by name, tag, or brand...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                        trailingIcon = {
                            if (filterSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { filterSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dialogFilteredProducts) { prod ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val updated = recordSlots.toMutableList()
                                        while (slotIndex >= updated.size) {
                                            updated.add(null)
                                        }
                                        updated[slotIndex] = prod.id
                                        recordSlots = updated
                                        onSetComparedIds(updated.filterNotNull())
                                        slotIndexToPick = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        DeviceGraphicPlaceholder(
                                            title = prod.name,
                                            brand = prod.brand,
                                            tags = prod.tags,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = prod.brand,
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
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
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        val isLaptop = maxWidth >= 840.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 1. COMPARE WORKSPACE Header & Action Buttons (Compare module.PNG)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "COMPARE WORKSPACE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Choose records",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Search all 5 fields, compare pictures, and open multiple reviews.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(Modifier.height(4.dp))

                // Actions Row: + Add record and Clear all (Screenshot 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (recordSlots.size < 5) {
                                val updated = recordSlots.toMutableList()
                                updated.add(null)
                                recordSlots = updated
                            }
                        },
                        enabled = recordSlots.size < 5,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("compare_add_record_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add record", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (recordSlots.any { it != null }) {
                        OutlinedButton(
                            onClick = {
                                recordSlots = mutableListOf<String?>(null, null)
                                onSetComparedIds(emptyList())
                            },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 2. Filter products by tag / brand / product Card (Screenshot 2)
        item {
            TagBrandProductFilter(
                label = "Filter products by tag / brand / product",
                tags = tagList,
                brands = brandList,
                products = allProducts.map { it.name },
                selectedTag = selectedFilterTag,
                onSelectTag = { tag ->
                    selectedFilterTag = tag
                },
                searchPlaceholder = "Search tags, brands, or products..."
            )
        }

        // 3. Record Slots Cards (RECORD 1, RECORD 2... Screenshot 1.png)
        if (isLaptop) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    recordSlots.indices.forEach { index ->
                        val slotProdId = recordSlots[index]
                        val slotProd = slotProdId?.let { id -> allProducts.find { it.id == id } }
                        RecordSlotCardView(
                            index = index,
                            slotProd = slotProd,
                            recordSlotsCount = recordSlots.size,
                            onSelectSlot = { slotIndexToPick = index },
                            onRemoveSlot = {
                                val updated = recordSlots.toMutableList()
                                if (updated.size > 2) {
                                    updated.removeAt(index)
                                } else {
                                    updated[index] = null
                                }
                                recordSlots = updated
                                onSetComparedIds(updated.filterNotNull())
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
            items(recordSlots.indices.toList()) { index ->
                val slotProdId = recordSlots[index]
                val slotProd = slotProdId?.let { id -> allProducts.find { it.id == id } }
                RecordSlotCardView(
                    index = index,
                    slotProd = slotProd,
                    recordSlotsCount = recordSlots.size,
                    onSelectSlot = { slotIndexToPick = index },
                    onRemoveSlot = {
                        val updated = recordSlots.toMutableList()
                        if (updated.size > 2) {
                            updated.removeAt(index)
                        } else {
                            updated[index] = null
                        }
                        recordSlots = updated
                        onSetComparedIds(updated.filterNotNull())
                    }
                )
            }
        }

        // =================================================================
        // 4. SIDE BY SIDE COMPARISON SECTION (Shown when 2+ products chosen)
        // =================================================================
        if (activeProducts.size >= 2) {
            item {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SIDE BY SIDE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Complete comparison",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEEF2FF)
                        ) {
                            Text(
                                text = "${activeProducts.size} compared",
                                color = Color(0xFF4338CA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "${distinctSpecNames.size} specifications",
                                color = Color(0xFFB45309),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Product Cards in Horizontal Scroll
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(activeProducts) { prod ->
                        val prodReviews = reviews.filter { prod.id in it.productIds || prod.name in it.productTitles }
                        val firstPic = prod.pictures.firstOrNull()?.url

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.width(260.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                ) {
                                    if (!firstPic.isNullOrBlank()) {
                                        val context = LocalContext.current
                                        AsyncImage(
                                            model = resolveImageModel(context, firstPic),
                                            contentDescription = prod.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().padding(8.dp)
                                        )
                                    } else {
                                        DeviceGraphicPlaceholder(
                                            title = prod.name,
                                            brand = prod.brand,
                                            tags = prod.tags,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Text(
                                    text = prod.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = prod.brand,
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onEditProduct(prod) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text("Edit", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { onProductClick(prod) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text("Details", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Brand Comparison Card
            val isBrandDiff = activeProducts.map { it.brand.trim().lowercase() }.distinct().size > 1
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBrandDiff) Color(0xFFFEFCE8) else Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isBrandDiff) Color(0xFFFDE047) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Brand", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            if (isBrandDiff) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text("DIFFERENT", color = Color(0xFFB45309), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }

                        Divider(color = if (isBrandDiff) Color(0xFFFEF08A) else Color(0xFFF1F5F9))

                        activeProducts.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.name, fontSize = 13.sp, color = Color(0xFF64748B))
                                Text(p.brand, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                        }
                    }
                }
            }

            // Tags Comparison Card
            val isTagsDiff = activeProducts.map { it.tags.sorted().joinToString(",") }.distinct().size > 1
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTagsDiff) Color(0xFFFEFCE8) else Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isTagsDiff) Color(0xFFFDE047) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tags", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            if (isTagsDiff) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text("DIFFERENT", color = Color(0xFFB45309), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Divider(color = if (isTagsDiff) Color(0xFFFEF08A) else Color(0xFFF1F5F9))

                        activeProducts.forEach { p ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(p.name, fontSize = 12.sp, color = Color(0xFF64748B))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (p.tags.isEmpty()) {
                                        Text("No tags", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                    } else {
                                        p.tags.forEach { tag ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFF1F5F9)
                                            ) {
                                                Text(tag, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFF334155))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // User Reviews Comparison Card (Matching screenshot & ComparisonMatrix.tsx)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.RateReview,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("User Reviews", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            }
                        }

                        Divider(color = Color(0xFFF1F5F9))

                        activeProducts.forEach { p ->
                            val linkedReviews = reviews.filter { r ->
                                p.id in r.productIds ||
                                        r.productTitles.any { it.equals(p.name, ignoreCase = true) }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        p.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (linkedReviews.isNotEmpty()) {
                                            "${linkedReviews.size} review${if (linkedReviews.size == 1) "" else "s"} available"
                                        } else {
                                            "No reviews yet"
                                        },
                                        fontSize = 12.sp,
                                        color = if (linkedReviews.isNotEmpty()) Color(0xFF059669) else Color(0xFF94A3B8)
                                    )
                                }

                                if (linkedReviews.isNotEmpty()) {
                                    Button(
                                        onClick = { onReviewClick(linkedReviews.first()) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4F46E5)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Read review", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onOpenAddReview(p) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Write", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Specifications Comparison Cards with Comparative Progress Bars, Higher/Lower toggle and Spec Pictures
            items(distinctSpecNames) { specName ->
                val values = activeProducts.map { p ->
                    activeSpecs.find { (it.productId == p.id || it.productTitle == p.name) && it.specName.equals(specName, ignoreCase = true) }
                }

                val distinctValues = values.map { (it?.details ?: "").trim().lowercase() }.distinct()
                val isSpecDiff = distinctValues.size > 1

                val parsedList = remember(values, specName) {
                    values.map { s ->
                        parseNumericSpec(s?.details, s?.unit, specName)
                    }
                }

                val hasAnyNumeric = remember(parsedList) {
                    parsedList.any { it.hasNumeric }
                }

                val commonUnit = remember(parsedList, values) {
                    parsedList.firstOrNull { it.displayUnit.isNotBlank() }?.displayUnit
                        ?: values.firstOrNull { !it?.unit.isNullOrBlank() }?.unit.orEmpty()
                }

                val specKey = specName.trim().lowercase()
                val currentMode = specSortModes[specKey] ?: "higher"
                val numericItems = parsedList.filter { it.hasNumeric }
                val maxNumeric = remember(numericItems) {
                    if (numericItems.isNotEmpty()) numericItems.maxOf { it.value } else 1f
                }
                val bestNumeric = remember(numericItems, currentMode) {
                    if (numericItems.isNotEmpty()) {
                        if (currentMode == "higher") numericItems.maxOf { it.value } else numericItems.minOf { it.value }
                    } else null
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSpecDiff) Color(0xFFFEFCE8) else Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSpecDiff) Color(0xFFFDE047) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Card Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Bar chart icon + Title + Unit
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (hasAnyNumeric) {
                                    Icon(
                                        Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = specName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                if (commonUnit.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFE2E8F0).copy(alpha = 0.8f)
                                    ) {
                                        Text(
                                            text = commonUnit,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Right: DIFFERENT badge + Higher/Lower Toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isSpecDiff) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFACC15),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAB308))
                                    ) {
                                        Text(
                                            text = "DIFFERENT",
                                            color = Color(0xFF451A03),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                if (hasAnyNumeric) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Higher Button
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (currentMode == "higher") Color.White else Color.Transparent,
                                                shadowElevation = if (currentMode == "higher") 1.dp else 0.dp,
                                                modifier = Modifier.clickable { specSortModes[specKey] = "higher" }
                                            ) {
                                                Text(
                                                    text = "Higher",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (currentMode == "higher") FontWeight.Bold else FontWeight.Medium,
                                                    color = if (currentMode == "higher") Color(0xFF0F172A) else Color(0xFF64748B),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }

                                            // Lower Button
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (currentMode == "lower") Color.White else Color.Transparent,
                                                shadowElevation = if (currentMode == "lower") 1.dp else 0.dp,
                                                modifier = Modifier.clickable { specSortModes[specKey] = "lower" }
                                            ) {
                                                Text(
                                                    text = "Lower",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (currentMode == "lower") FontWeight.Bold else FontWeight.Medium,
                                                    color = if (currentMode == "lower") Color(0xFF0F172A) else Color(0xFF64748B),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = if (isSpecDiff) Color(0xFFFEF08A) else Color(0xFFF1F5F9))

                        // Product Rows inside Card
                        activeProducts.forEachIndexed { i, p ->
                            val s = values[i]
                            val parsed = parsedList[i]
                            val isBest = parsed.hasNumeric && parsed.value == bestNumeric && numericItems.size > 1

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = p.name,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isBest) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFECFDF5),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                                            ) {
                                                Text(
                                                    text = "🏆 Best",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF047857),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (parsed.hasNumeric) {
                                                val displayNum = if (parsed.value % 1f == 0f) parsed.value.toLong().toString() else parsed.value.toString()
                                                if (parsed.displayUnit.isNotBlank()) "$displayNum ${parsed.displayUnit}" else displayNum
                                            } else if (parsed.rawText.isNotBlank()) {
                                                parsed.rawText
                                            } else {
                                                "—"
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = if (isBest) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isBest) Color(0xFF0F172A) else if (parsed.hasNumeric || parsed.rawText.isNotBlank()) Color(0xFF334155) else Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                // Comparative Progress Bar with empty track fallback
                                if (hasAnyNumeric) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFE2E8F0).copy(alpha = 0.7f))
                                    ) {
                                        if (parsed.hasNumeric && maxNumeric > 0f) {
                                            val ratio = (parsed.value / maxNumeric).coerceIn(0.04f, 1f)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction = ratio)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (isBest) Color(0xFF10B981) else Color(0xFF475569)
                                                    )
                                            )
                                        }
                                    }
                                }

                                // Spec Image Thumbnails (Under progress bar, as in screenshot 2.png)
                                val specPics = s?.pictures ?: emptyList()
                                if (specPics.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        specPics.forEach { pic ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                modifier = Modifier
                                                    .size(width = 60.dp, height = 44.dp)
                                                    .clickable { fullScreenPictureUrl = pic.url }
                                            ) {
                                                val context = LocalContext.current
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
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun RecordSlotCardView(
    index: Int,
    slotProd: ProductItem?,
    recordSlotsCount: Int,
    onSelectSlot: () -> Unit,
    onRemoveSlot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECORD ${index + 1}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.8.sp
                )

                if (recordSlotsCount > 2 || slotProd != null) {
                    IconButton(
                        onClick = onRemoveSlot,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = if (recordSlotsCount > 2) "Remove slot" else "Clear slot",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (slotProd == null) {
                // Dashed Box: Search and select a record (Screenshot 1.png)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFAFAFA),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSlot() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Search and select a record",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Tap to filter and choose a product to compare",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Icon(
                            Icons.Default.UnfoldMore,
                            contentDescription = "Select",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Filled Record Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            val pic = slotProd.pictures.firstOrNull()?.url
                            if (!pic.isNullOrBlank()) {
                                val context = LocalContext.current
                                AsyncImage(
                                    model = resolveImageModel(context, pic),
                                    contentDescription = slotProd.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                DeviceGraphicPlaceholder(
                                    title = slotProd.name,
                                    brand = slotProd.brand,
                                    tags = slotProd.tags,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = slotProd.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = slotProd.brand,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            if (slotProd.tags.isNotEmpty()) {
                                Text(
                                    text = slotProd.tags.take(2).joinToString(", "),
                                    fontSize = 11.sp,
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = onSelectSlot,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Change", fontSize = 11.sp)
                            }

                            IconButton(
                                onClick = onRemoveSlot,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Data class for parsed numeric specification
data class ParsedNumericSpec(
    val hasNumeric: Boolean,
    val value: Float,
    val unit: String,
    val displayUnit: String,
    val rawText: String
)

// Helper function to extract numeric values and units for specs
fun parseNumericSpec(details: String?, specUnit: String?, specName: String): ParsedNumericSpec {
    if (details.isNullOrBlank()) {
        return ParsedNumericSpec(hasNumeric = false, value = 0f, unit = specUnit ?: "", displayUnit = specUnit ?: "", rawText = "")
    }
    val clean = details.replace(Regex("<[^>]*>"), "").trim()
    if (clean.isBlank()) {
        return ParsedNumericSpec(hasNumeric = false, value = 0f, unit = specUnit ?: "", displayUnit = specUnit ?: "", rawText = "")
    }

    val preferredUnit = specUnit?.trim() ?: ""

    // Currency prefix match (e.g. $599, Rm300, €50)
    val currencyMatch = Regex("""^([$€£¥₹]|Rm|RM)\s*([0-9,]+(?:\.[0-9]+)?)""").find(clean)
    if (currencyMatch != null) {
        val sym = currencyMatch.groupValues[1]
        val num = currencyMatch.groupValues[2].replace(",", "").toFloatOrNull()
        if (num != null) {
            val u = if (preferredUnit.isNotBlank()) preferredUnit else sym
            return ParsedNumericSpec(hasNumeric = true, value = num, unit = u, displayUnit = u, rawText = clean)
        }
    }

    // Number followed by optional unit: e.g. "201 g", "100g", "60hour", "11111"
    val generalMatch = Regex("""([0-9,]+(?:\.[0-9]+)?)\s*([a-zA-Z$€£%°/"″]+)?""").find(clean)
    if (generalMatch != null) {
        val num = generalMatch.groupValues[1].replace(",", "").toFloatOrNull()
        if (num != null) {
            var rawUnit = if (preferredUnit.isNotBlank()) preferredUnit else generalMatch.groupValues[2]
            if (rawUnit.isBlank()) {
                val bracketMatch = Regex("""\(([^)]+)\)""").find(specName)
                if (bracketMatch != null) rawUnit = bracketMatch.groupValues[1]
            }
            rawUnit = rawUnit.trim()
            val lower = rawUnit.lowercase()
            val normalizedUnit = when {
                lower in listOf("g", "gram", "grams") -> "g"
                lower == "kg" -> "kg"
                lower in listOf("inch", "inches", "\"", "″") -> "inch"
                lower == "cm" -> "cm"
                lower == "mm" -> "mm"
                lower in listOf("year", "years") -> "year"
                lower in listOf("month", "months") -> "month"
                lower in listOf("hour", "hours", "hrs", "hr") -> "hrs"
                lower == "mah" -> "mAh"
                lower == "hz" -> "Hz"
                else -> rawUnit
            }
            return ParsedNumericSpec(hasNumeric = true, value = num, unit = normalizedUnit, displayUnit = normalizedUnit, rawText = clean)
        }
    }

    return ParsedNumericSpec(hasNumeric = false, value = 0f, unit = preferredUnit, displayUnit = preferredUnit, rawText = clean)
}
