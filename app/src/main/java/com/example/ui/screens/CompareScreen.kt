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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.PictureItem
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.SpecificationItem
import com.example.model.resolveImageModel
import com.example.ui.components.ButtonText
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.DeviceGraphicPlaceholder
import com.example.ui.components.FormattedSpecHtml
import com.example.ui.components.FullScreenPhotoLightbox
import com.example.ui.components.SpecUnitBadge
import com.example.ui.components.TagBrandProductFilter
import com.example.util.computeHierarchicalSpecOrder

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
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var slotToRemoveIndex by remember { mutableStateOf<Int?>(null) }

    // Higher / Lower sorting preferences per spec (defaults to "higher")
    val specSortModes = remember { mutableStateMapOf<String, String>() }
    // User manual overrides for numeric (#) vs text (Aa) per spec
    val specNumericOverrides = remember { mutableStateMapOf<String, Boolean?>() }

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

    val distinctSpecNames = remember(activeProducts, specifications) {
        computeHierarchicalSpecOrder(activeProducts, specifications)
    }

    // Full Screen Picture Lightbox
    if (fullScreenPictureUrl != null) {
        FullScreenPhotoLightbox(
            images = listOf(PictureItem(id = "compare_pic", name = "Photo", url = fullScreenPictureUrl!!)),
            initialIndex = 0,
            onDismiss = { fullScreenPictureUrl = null }
        )
    }

    // Slot Record Picker Screen (Full Screen with Back Button)
    if (slotIndexToPick != null) {
        val slotIndex = slotIndexToPick!!
        BackHandler { slotIndexToPick = null }
        Dialog(
            onDismissRequest = { slotIndexToPick = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFF8FAFC)
            ) {
                Scaffold(
                    contentWindowInsets = WindowInsets.systemBars,
                    topBar = {
                        Surface(
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { slotIndexToPick = null },
                                    modifier = Modifier.testTag("slot_picker_back_btn")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Select Record for Slot ${slotIndex + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Choose a product to populate comparison column",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                            modifier = Modifier.fillMaxWidth().testTag("slot_picker_search_input"),
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
                                        .testTag("slot_pick_item_${prod.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF1F5F9),
                                            modifier = Modifier.size(44.dp)
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
                                                fontSize = 15.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "${prod.brand} • ${prod.tags.joinToString(", ")}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(18.dp)
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

    // Confirmation dialog: Clear all comparison slots
    if (showClearAllConfirm) {
        val activeProducts = recordSlots.filterNotNull().mapNotNull { id -> allProducts.find { it.id == id } }
        val message = if (activeProducts.isNotEmpty()) {
            val namesStr = activeProducts.joinToString(", ") { "\"${it.name}\"" }
            "Are you sure you want to clear ${activeProducts.size} product${if (activeProducts.size > 1) "s" else ""} ($namesStr) from the comparison?"
        } else {
            "Are you sure you want to reset all comparison slots?"
        }
        DeleteConfirmDialog(
            title = "Clear Comparison?",
            message = message,
            confirmText = "Clear all",
            testTagPrefix = "compare_clear_all",
            onConfirm = {
                recordSlots = mutableListOf<String?>(null, null)
                onSetComparedIds(emptyList())
                showClearAllConfirm = false
            },
            onDismiss = { showClearAllConfirm = false }
        )
    }

    // Confirmation dialog: Remove individual slot
    if (slotToRemoveIndex != null) {
        val index = slotToRemoveIndex!!
        val slotProdId = recordSlots.getOrNull(index)
        val slotProd = slotProdId?.let { id -> allProducts.find { it.id == id } }
        val title = if (slotProd != null) "Remove from Comparison?" else "Remove Comparison Slot?"
        val message = if (slotProd != null) {
            "Are you sure you want to remove \"${slotProd.name}\" from Slot ${index + 1}?"
        } else {
            "Are you sure you want to remove comparison Slot ${index + 1}?"
        }
        DeleteConfirmDialog(
            title = title,
            message = message,
            confirmText = "Remove",
            testTagPrefix = "compare_remove_slot",
            onConfirm = {
                val updated = recordSlots.toMutableList()
                if (index in updated.indices) {
                    if (updated.size > 2) {
                        updated.removeAt(index)
                    } else {
                        updated[index] = null
                    }
                    recordSlots = updated
                    onSetComparedIds(updated.filterNotNull())
                }
                slotToRemoveIndex = null
            },
            onDismiss = { slotToRemoveIndex = null }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.AppColors.CanvasBackground)
    ) {
        val isLaptop = maxWidth >= 840.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = com.example.ui.theme.AppSpacing.ScreenContentPaddingScrollable,
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("compare_add_record_btn").height(38.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        ButtonText("Add record", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (recordSlots.any { it != null }) {
                        OutlinedButton(
                            onClick = { showClearAllConfirm = true },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            ButtonText("Clear all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
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

        // 3. Record Slots Cards (RECORD 1, RECORD 2... Screenshot 1.png & IMG_0002)
        if (isLaptop) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    recordSlots.indices.forEach { index ->
                        val slotProdId = recordSlots[index]
                        val slotProd = slotProdId?.let { id -> allProducts.find { it.id == id } }
                        val revCount = reviews.count { slotProd?.id in it.productIds || (slotProd != null && it.productTitles.any { t -> t.equals(slotProd.name, ignoreCase = true) }) }
                        RecordSlotCardView(
                            index = index,
                            slotProd = slotProd,
                            recordSlotsCount = recordSlots.size,
                            reviewsCount = revCount,
                            onSelectSlot = { slotIndexToPick = index },
                            onRemoveSlot = { slotToRemoveIndex = index },
                            onEditProduct = onEditProduct,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
            items(recordSlots.indices.toList()) { index ->
                val slotProdId = recordSlots[index]
                val slotProd = slotProdId?.let { id -> allProducts.find { it.id == id } }
                val revCount = reviews.count { slotProd?.id in it.productIds || (slotProd != null && it.productTitles.any { t -> t.equals(slotProd.name, ignoreCase = true) }) }
                RecordSlotCardView(
                    index = index,
                    slotProd = slotProd,
                    recordSlotsCount = recordSlots.size,
                    reviewsCount = revCount,
                    onSelectSlot = { slotIndexToPick = index },
                    onRemoveSlot = { slotToRemoveIndex = index },
                    onEditProduct = onEditProduct
                )
            }
        }

        // =================================================================
        // 4. SIDE BY SIDE COMPARISON SECTION (Shown when 2+ products chosen)
        // =================================================================
        if (activeProducts.size >= 2) {
            val isBrandDiff = activeProducts.map { it.brand.trim().lowercase() }.distinct().size > 1
            val isTagsDiff = activeProducts.map { it.tags.sorted().joinToString(",").lowercase() }.distinct().size > 1
            val diffSpecsCount = distinctSpecNames.count { specName ->
                val vals = activeProducts.map { p ->
                    activeSpecs.find { (it.productId == p.id || it.productTitle == p.name) && it.specName.equals(specName, ignoreCase = true) }?.details.orEmpty().trim().lowercase()
                }.distinct()
                vals.size > 1
            }
            val totalDifferCount = (if (isBrandDiff) 1 else 0) + (if (isTagsDiff) 1 else 0) + diffSpecsCount
            val totalFieldsCount = 2 + distinctSpecNames.size

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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = "${activeProducts.size} selected",
                                color = Color(0xFF0F172A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047))
                        ) {
                            Text(
                                text = "$totalDifferCount differences",
                                color = Color(0xFFB45309),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "$totalFieldsCount fields shown, $totalDifferCount differ",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Brand Comparison Card (IMG_0002)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Sell, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                                Text("Brand", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            }
                            if (isBrandDiff) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047))
                                ) {
                                    Text("Different", color = Color(0xFFB45309), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }

                        Divider(color = if (isBrandDiff) Color(0xFFFEF08A) else Color(0xFFF1F5F9))

                        activeProducts.forEachIndexed { idx, p ->
                            if (idx > 0) Spacer(Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = p.name.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = p.brand,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }

            // Tags Comparison Card (IMG_0002)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Sell, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                                Text("Tags", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            }
                            if (isTagsDiff) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047))
                                ) {
                                    Text(
                                        "DIFFERENT",
                                        color = Color(0xFFB45309),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Divider(color = if (isTagsDiff) Color(0xFFFEF08A) else Color(0xFFF1F5F9))

                        activeProducts.forEachIndexed { idx, p ->
                            if (idx > 0) Spacer(Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = p.name.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 0.5.sp
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (p.tags.isEmpty()) {
                                        Text("No tags", fontSize = 12.sp, color = Color(0xFF94A3B8), maxLines = 1, softWrap = false)
                                    } else {
                                        p.tags.forEach { tag ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFF1F5F9)
                                            ) {
                                                Text(
                                                    tag,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    color = Color(0xFF334155),
                                                    maxLines = 1,
                                                    softWrap = false
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

            // User Reviews Comparison Card (IMG_0003)
            val isReviewsDiff = activeProducts.map { p ->
                reviews.count { p.id in it.productIds || it.productTitles.any { t -> t.equals(p.name, ignoreCase = true) } }
            }.distinct().size > 1
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isReviewsDiff) Color(0xFFFEFCE8) else Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isReviewsDiff) Color(0xFFFDE047) else Color(0xFFE2E8F0)),
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
                                    Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("User reviews", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            }

                            if (isReviewsDiff) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047))
                                ) {
                                    Text(
                                        "Different",
                                        color = Color(0xFFB45309),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Divider(color = if (isReviewsDiff) Color(0xFFFEF08A) else Color(0xFFF1F5F9))

                        activeProducts.forEachIndexed { idx, p ->
                            if (idx > 0) Spacer(Modifier.height(10.dp))
                            val linkedReviews = reviews.filter { r ->
                                p.id in r.productIds ||
                                        r.productTitles.any { it.equals(p.name, ignoreCase = true) }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = p.name.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 0.5.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (linkedReviews.isNotEmpty()) {
                                        Text(
                                            text = "${linkedReviews.size} review${if (linkedReviews.size == 1) "" else "s"} available",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF0F172A)
                                        )

                                        OutlinedButton(
                                            onClick = { onReviewClick(linkedReviews.first()) },
                                            shape = RoundedCornerShape(20.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ChatBubbleOutline,
                                                contentDescription = null,
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "Read review",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "No linked reviews",
                                            fontSize = 13.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = Color(0xFF94A3B8)
                                        )

                                        // Dashed border style for Write review
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = Color(0xFFEEF2FF).copy(alpha = 0.6f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .clickable { onOpenAddReview(p) }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4F46E5),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Write review",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF4F46E5)
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

            // Specifications Comparison Cards with Comparative Progress Bars, Higher/Lower toggle and Spec Pictures
            items(distinctSpecNames) { specName ->
                val values = activeProducts.map { p ->
                    activeSpecs.find { (it.productId == p.id || it.productTitle == p.name) && it.specName.equals(specName, ignoreCase = true) }
                }

                val distinctValues = values.map { (it?.details ?: "").trim().lowercase() }.distinct()
                val isSpecDiff = distinctValues.size > 1

                val rowOverride = specNumericOverrides[specName]

                val parsedList = remember(values, specName, rowOverride) {
                    values.map { s ->
                        parseNumericSpec(
                            details = s?.details,
                            specUnit = s?.unit,
                            specName = specName,
                            isNumeric = rowOverride ?: s?.isNumeric
                        )
                    }
                }

                val isNumericSpec = remember(parsedList, values, rowOverride) {
                    when {
                        rowOverride != null -> rowOverride
                        values.any { it?.isNumeric == true } -> true
                        values.any { it?.isNumeric == false } && values.none { it?.isNumeric == true } -> false
                        else -> {
                            val numericCount = parsedList.count { it.hasNumeric && !it.rawText.contains('\n') && it.rawText.length <= 45 }
                            numericCount >= 2
                        }
                    }
                }

                val hasAnyNumeric = isNumericSpec && parsedList.any { it.hasNumeric }

                val detectedUnit = remember(parsedList, values, specName) {
                    parsedList.firstOrNull { it.displayUnit.isNotBlank() }?.displayUnit
                        ?: values.firstOrNull { !it?.unit.isNullOrBlank() }?.unit
                        ?: specifications.find { it.specName.equals(specName, ignoreCase = true) && !it.unit.isNullOrBlank() }?.unit.orEmpty()
                }

                val specKey = specName.trim().lowercase()
                val currentMode = getSpecBestValueMode(specName, detectedUnit, specSortModes)
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
                        // Card Header Row: Spec Name, Icon, Unit, Number Toggle, and DIFFERENT badge (IMG_0003)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (hasAnyNumeric) Icons.Default.BarChart else Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (hasAnyNumeric) Color(0xFF6366F1) else Color(0xFF4F46E5),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = specName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false
                            )

                            if (detectedUnit.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = detectedUnit,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Interactive Number ON/OFF Pill Toggle (IMG_0003)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .clickable {
                                        specNumericOverrides[specName] = when (rowOverride) {
                                            null -> if (isNumericSpec) false else true
                                            true -> false
                                            false -> true
                                        }
                                    }
                                    .testTag("spec_override_${specName.replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isNumericSpec) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF4F46E5),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("#", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text("Number ON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                                        Text("⇄ Text", fontSize = 9.sp, color = Color(0xFF64748B))
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("Aa", color = Color(0xFF334155), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text("Number OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                        Text("⇄ Number", fontSize = 9.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }

                            // DIFFERENT Badge (IMG_0003)
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

                            // Higher / Lower segment toggle (IMG_0003)
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
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (currentMode == "higher") Color.White else Color.Transparent,
                                            shadowElevation = if (currentMode == "higher") 1.dp else 0.dp,
                                            modifier = Modifier.clickable { specSortModes[specKey] = "higher" }
                                        ) {
                                            Text(
                                                text = "Higher",
                                                fontSize = 10.sp,
                                                fontWeight = if (currentMode == "higher") FontWeight.Bold else FontWeight.Medium,
                                                color = if (currentMode == "higher") Color(0xFF0F172A) else Color(0xFF64748B),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (currentMode == "lower") Color.White else Color.Transparent,
                                            shadowElevation = if (currentMode == "lower") 1.dp else 0.dp,
                                            modifier = Modifier.clickable { specSortModes[specKey] = "lower" }
                                        ) {
                                            Text(
                                                text = "Lower",
                                                fontSize = 10.sp,
                                                fontWeight = if (currentMode == "lower") FontWeight.Bold else FontWeight.Medium,
                                                color = if (currentMode == "lower") Color(0xFF0F172A) else Color(0xFF64748B),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = if (isSpecDiff) Color(0xFFFEF08A) else Color(0xFFF1F5F9))

                        // Product Rows stacked vertically inside Card (IMG_0003)
                        activeProducts.forEachIndexed { i, p ->
                            if (i > 0) Divider(color = if (isSpecDiff) Color(0xFFFEF08A).copy(alpha = 0.7f) else Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 6.dp))
                            val s = values[i]
                            val parsed = parsedList[i]
                            val isBest = parsed.hasNumeric && parsed.value == bestNumeric && numericItems.size > 1

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = p.name.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 0.5.sp
                                    )

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
                                }

                                // Spec Value Rendering (Vertical as IMG_0003)
                                if (parsed.hasNumeric && isNumericSpec) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val displayNum = if (parsed.value % 1f == 0f) parsed.value.toLong().toString() else parsed.value.toString()
                                            Text(
                                                text = if (parsed.displayUnit.isNotBlank()) "$displayNum ${parsed.displayUnit}" else displayNum,
                                                fontSize = 15.sp,
                                                fontWeight = if (isBest) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isBest) Color(0xFF0F172A) else Color(0xFF334155)
                                            )
                                        }
                                    }

                                    // Comparative Progress Bar
                                    if (maxNumeric > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE2E8F0).copy(alpha = 0.7f))
                                        ) {
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
                                } else if (parsed.rawText.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FormattedSpecHtml(
                                            content = parsed.rawText,
                                            fontSize = 14.sp,
                                            lineHeight = 18.sp,
                                            color = if (isBest) Color(0xFF0F172A) else Color(0xFF334155)
                                        )
                                        SpecUnitBadge(
                                            unit = s?.unit?.ifBlank { null } ?: detectedUnit.ifBlank { null },
                                            details = parsed.rawText
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "No data",
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                // Spec Image Thumbnails (IMG_0003: with Full Screen button overlay & filename)
                                val specPics = s?.pictures ?: emptyList()
                                if (specPics.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "SPECIFICATION PICTURES (${specPics.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 0.5.sp
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        items(specPics) { pic ->
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 170.dp, height = 115.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFF1F5F9))
                                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                                    .clickable { fullScreenPictureUrl = pic.url }
                                            ) {
                                                AsyncImage(
                                                    model = resolveImageModel(LocalContext.current, pic.url, preferredName = pic.name),
                                                    contentDescription = pic.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                // Full screen overlay button (IMG_0003)
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color.Black.copy(alpha = 0.7f),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                        .clickable { fullScreenPictureUrl = pic.url }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.OpenInFull,
                                                            contentDescription = "Full screen",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Spacer(Modifier.width(3.dp))
                                                        Text(
                                                            "Full screen",
                                                            fontSize = 9.sp,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                // Filename label at bottom overlay (IMG_0003)
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.55f),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.BottomCenter)
                                                ) {
                                                    Text(
                                                        text = pic.name.ifBlank { "IMG_${(s?.id ?: "0000").takeLast(4)}.png" },
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
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
}

@Composable
fun RecordSlotCardView(
    index: Int,
    slotProd: ProductItem?,
    recordSlotsCount: Int,
    reviewsCount: Int = 0,
    onSelectSlot: () -> Unit,
    onRemoveSlot: () -> Unit,
    onEditProduct: (ProductItem) -> Unit = {},
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (slotProd != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Transparent,
                            modifier = Modifier.clickable { onEditProduct(slotProd) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit product",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Edit product",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }
                    }

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
                // Filled Record Card matching IMG_0002_0aabfdf2.PNG
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSlot() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            val pic = slotProd.pictures.firstOrNull()?.url
                            val picName = slotProd.pictures.firstOrNull()?.name
                            if (!pic.isNullOrBlank() || !picName.isNullOrBlank()) {
                                val context = LocalContext.current
                                AsyncImage(
                                    model = resolveImageModel(context, pic ?: "", preferredName = picName),
                                    contentDescription = slotProd.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(3.dp)
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
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "✓ Selected",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                                Text(
                                    text = "/ ${slotProd.brand} / $reviewsCount reviews",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            Icons.Default.UnfoldMore,
                            contentDescription = "Change product",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
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
fun parseNumericSpec(details: String?, specUnit: String?, specName: String, isNumeric: Boolean? = null): ParsedNumericSpec {
    if (details.isNullOrBlank()) {
        return ParsedNumericSpec(hasNumeric = false, value = 0f, unit = specUnit ?: "", displayUnit = specUnit ?: "", rawText = "")
    }
    val clean = details.replace(Regex("<[^>]*>"), "").trim()
    if (clean.isBlank()) {
        return ParsedNumericSpec(hasNumeric = false, value = 0f, unit = specUnit ?: "", displayUnit = specUnit ?: "", rawText = "")
    }

    val preferredUnit = specUnit?.trim() ?: ""

    // If specification explicitly specifies isNumeric === false, NEVER treat as numeric!
    if (isNumeric == false) {
        return ParsedNumericSpec(hasNumeric = false, value = 0f, unit = preferredUnit, displayUnit = preferredUnit, rawText = clean)
    }

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

    // If text contains newlines, bullets, or is narrative (> 45 chars), do NOT auto-parse as numeric
    // unless spec explicitly has isNumeric === true
    if (isNumeric != true && (clean.contains('\n') || clean.contains('•') || clean.length > 45)) {
        return ParsedNumericSpec(hasNumeric = false, value = 0f, unit = preferredUnit, displayUnit = preferredUnit, rawText = clean)
    }

    // Number followed by optional unit: e.g. "201 g", "100g", "60hour", "11111"
    val regexPattern = if (isNumeric == true) {
        Regex("""([0-9,]+(?:\.[0-9]+)?)\s*([a-zA-Z$€£%°/"″]+)?""")
    } else {
        Regex("""^\s*([0-9,]+(?:\.[0-9]+)?)\s*([a-zA-Z$€£%°/"″]+)?""")
    }

    val generalMatch = regexPattern.find(clean)
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

fun getSpecBestValueMode(specName: String, unit: String, specSortModes: Map<String, String>): String {
    val specKey = specName.trim().lowercase()
    specSortModes[specKey]?.let { return it }
    val lowerName = specName.lowercase()
    val lowerUnit = unit.lowercase()
    if (
        lowerName.contains("weight") ||
        lowerName.contains("weigh") ||
        lowerName.contains("gram") ||
        lowerName.contains("price") ||
        lowerName.contains("cost") ||
        lowerName.contains("thickness") ||
        lowerName.contains("latency") ||
        lowerName.contains("delay") ||
        lowerUnit == "g" ||
        lowerUnit == "kg" ||
        lowerUnit == "$" ||
        lowerUnit == "€" ||
        lowerUnit == "£"
    ) {
        return "lower"
    }
    return "higher"
}

