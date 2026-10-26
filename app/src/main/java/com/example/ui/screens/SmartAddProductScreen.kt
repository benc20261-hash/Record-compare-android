package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.model.PictureItem
import com.example.model.ProductItem
import com.example.model.SpecificationItem
import com.example.ui.components.AppSummaryCard
import com.example.model.resolveImageModel
import com.example.ui.components.ButtonText
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.DeviceGraphicPlaceholder
import com.example.ui.components.FormattedSpecHtml
import com.example.ui.components.HtmlWysiwygEditor
import com.example.ui.components.SpecUnitBadge
import com.example.ui.components.SpecificationsOrderModal
import com.example.ui.components.TagBrandProductFilter
import com.example.util.computeHierarchicalSpecOrder
import com.example.util.buildInheritedBlankSpecs
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAddProductScreen(
    products: List<ProductItem>,
    specifications: List<SpecificationItem>,
    comparedIds: Set<String> = emptySet(),
    onNavigateToCompare: () -> Unit,
    onNavigateToCatalog: () -> Unit,
    onEditProduct: (ProductItem) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onCreateAndCompare: (String, String, List<String>, String?, (String) -> Unit) -> Unit,
    onAddSpecificationInline: (productId: String, productTitle: String, specName: String, details: String, unit: String?) -> Unit,
    onUpdateInlineSpec: (productId: String, productTitle: String, specName: String, details: String, pictures: List<PictureItem>, unit: String?) -> Unit,
    onDeleteSpecInline: (productId: String, specName: String) -> Unit,
    onUpdateProductBrand: (productId: String, brand: String) -> Unit,
    onUpdateProductTags: (productId: String, tags: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf("All") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var slotPickerIndex by remember { mutableStateOf<Int?>(null) }
    var isTagDropdownOpen by remember { mutableStateOf(false) }
    var slotToRemoveIndex by remember { mutableStateOf<Int?>(null) }
    var showResetSlotsConfirm by remember { mutableStateOf(false) }
    var specToDeleteInline by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Slots tracking (Screenshot 2.png)
    // Slots tracking (Screenshot 2.png) - Single reactive source of truth
    val productSlots = remember {
        val list = mutableStateListOf<String?>()
        if (comparedIds.isNotEmpty()) {
            comparedIds.forEach { list.add(it) }
            if (comparedIds.size == 1) list.add(null)
        } else if (products.size >= 2) {
            list.add(products[0].id)
            list.add(products[1].id)
        } else if (products.isNotEmpty()) {
            list.add(products[0].id)
            list.add(null)
        } else {
            list.add(null)
            list.add(null)
        }
        list
    }

    // Newly created products tracking (for (New) label & amber New badge)
    val newlyCreatedIds = remember { mutableStateListOf<String>() }
    val localNewProducts = remember { mutableStateListOf<ProductItem>() }
    var selectedProductForSpecOrder by remember { mutableStateOf<ProductItem?>(null) }

    // Quick intake inputs
    var newProductName by remember { mutableStateOf("") }
    var targetProductForSpecId by remember {
        mutableStateOf(products.firstOrNull()?.id ?: "")
    }
    var newSpecName by remember { mutableStateOf("") }
    var newSpecDetails by remember { mutableStateOf("") }

    // Combined available products (synchronous local products + Room database products)
    val allAvailableProducts = remember(products, localNewProducts.toList()) {
        val existingIds = products.map { it.id }.toSet()
        products + localNewProducts.filter { it.id !in existingIds }
    }

    // Inline editing states: key is "$productId|$specName"
    var activeInlineEditKey by remember { mutableStateOf<String?>(null) }
    var inlineSpecDetails by remember { mutableStateOf("") }
    val inlineSpecPictures = remember { mutableStateListOf<PictureItem>() }

    // Inline brand editing state: productId -> editing
    var editingBrandProductId by remember { mutableStateOf<String?>(null) }
    var editingBrandText by remember { mutableStateOf("") }

    // Photo picker for inline spec editor
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            inlineSpecPictures.add(
                PictureItem(
                    id = "pic_${UUID.randomUUID().toString().take(8)}",
                    name = "Specification photo",
                    url = it.toString()
                )
            )
        }
    }

    // Filtered products for checklist
    val filteredProducts = remember(allAvailableProducts, searchQuery, selectedTagFilter) {
        allAvailableProducts.filter { p ->
            val matchesTag = selectedTagFilter == "All" || p.tags.contains(selectedTagFilter)
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isBlank() ||
                    p.name.lowercase().contains(q) ||
                    p.brand.lowercase().contains(q) ||
                    p.tags.any { it.lowercase().contains(q) }
            matchesTag && matchesSearch
        }
    }

    // Selected products reactive directly from productSlots snapshot state
    val selectedProducts = remember(productSlots.toList(), allAvailableProducts) {
        productSlots.filterNotNull().mapNotNull { id ->
            allAvailableProducts.find { it.id == id }
        }
    }

    // Specifications belonging to the selected products following slot hierarchy
    val matrixSpecs = remember(selectedProducts, specifications) {
        computeHierarchicalSpecOrder(selectedProducts, specifications)
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val allTags = remember(allAvailableProducts) {
        val tags = linkedSetOf<String>()
        allAvailableProducts.forEach { p ->
            p.tags.forEach { if (it.isNotBlank()) tags.add(it) }
        }
        tags.toList()
    }
    val allBrands = remember(allAvailableProducts) {
        val brands = linkedSetOf<String>()
        allAvailableProducts.forEach { p ->
            if (p.brand.isNotBlank() && !p.brand.equals("Generic", ignoreCase = true)) {
                brands.add(p.brand)
            }
        }
        brands.toList()
    }
    val allProductNames = remember(allAvailableProducts) {
        allAvailableProducts.map { it.name }
    }

    // Fast product lookup for the open slot picker
    val slotMatchingProducts = remember(allAvailableProducts, searchQuery, selectedTagFilter, specifications) {
        val q = searchQuery.trim().lowercase()
        allAvailableProducts.filter { p ->
            val matchesTag = selectedTagFilter == "All" ||
                    p.tags.any { it.equals(selectedTagFilter, ignoreCase = true) } ||
                    p.brand.equals(selectedTagFilter, ignoreCase = true)
            val matchesSearch = q.isBlank() ||
                    p.name.lowercase().contains(q) ||
                    p.brand.lowercase().contains(q) ||
                    p.tags.any { it.lowercase().contains(q) } ||
                    specifications.filter { it.productId == p.id || it.productTitle == p.name }
                        .any { it.specName.lowercase().contains(q) || it.details.lowercase().contains(q) }
            matchesTag && matchesSearch
        }
    }

    val filledSlotsCount = productSlots.filterNotNull().size

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.AppColors.CanvasBackground),
        contentPadding = com.example.ui.theme.AppSpacing.ScreenContentPaddingScrollable,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // =================================================================
        // 1. SMART ADD & SPECIFICATION WORKSPACE HEADER (d8 in recordcompare-1.ai.studio)
        // =================================================================
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMART ADD & SPECIFICATION WORKSPACE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.2.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Choose products to compare & manage specs",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Pick records into slots using the fast search selector. Zero lag even with 1,000+ products.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Slot Action Controls Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (productSlots.size > 2) {
                            OutlinedButton(
                                onClick = {
                                    if (productSlots.size > 2) {
                                        slotToRemoveIndex = productSlots.size - 1
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFE11D48)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Remove last", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = {
                                productSlots.add(null)
                                slotPickerIndex = productSlots.size - 1
                                searchQuery = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add product slot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 1b. Unified Filter Tag & Brand Pills Bar (Lu in recordcompare-1.ai.studio / Screenshot 4)
        item {
            TagBrandProductFilter(
                label = "Filter products by tag / brand / product",
                tags = allTags,
                brands = allBrands,
                products = allProductNames,
                selectedTag = selectedTagFilter,
                onSelectTag = { tag ->
                    selectedTagFilter = tag
                },
                searchPlaceholder = "Search tags, brands, or products..."
            )
        }

        // =================================================================
        // 2. DYNAMIC PRODUCT SLOTS (Product slot 1, 2, ... with fast popup picker)
        // =================================================================
        items(productSlots.size) { index ->
            val slotProductId = productSlots[index]
            val slotProduct = allAvailableProducts.find { it.id == slotProductId }
            val isSlotOpen = slotPickerIndex == index
            val productSpecs = if (slotProduct != null) {
                specifications.filter { it.productId == slotProduct.id || it.productTitle == slotProduct.name }
            } else emptyList()

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isSlotOpen) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Slot Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = "Slot ${index + 1} of ${productSlots.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                maxLines = 1,
                                softWrap = false
                            )

                            if (slotProduct != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFECFDF5),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("✓", fontSize = 10.sp, color = Color(0xFF059669), fontWeight = FontWeight.Black)
                                        Text(
                                            slotProduct.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        "Empty slot",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Slot Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (slotProduct != null) {
                                TextButton(
                                    onClick = {
                                        slotPickerIndex = if (isSlotOpen) null else index
                                        searchQuery = ""
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        if (isSlotOpen) "Close" else "Change",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5)
                                    )
                                }

                                IconButton(
                                    onClick = { slotToRemoveIndex = index },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        if (productSlots.size > 2) Icons.Default.Delete else Icons.Default.Close,
                                        contentDescription = "Remove or clear slot",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                if (productSlots.size > 2) {
                                    IconButton(
                                        onClick = { slotToRemoveIndex = index },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove slot",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Slot Content Body
                    if (slotProduct != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    val firstPic = slotProduct.pictures.firstOrNull()?.url
                                    val firstPicName = slotProduct.pictures.firstOrNull()?.name
                                    val context = LocalContext.current
                                    if (!firstPic.isNullOrBlank() || !firstPicName.isNullOrBlank()) {
                                        SubcomposeAsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(resolveImageModel(context, firstPic ?: "", preferredName = firstPicName))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = slotProduct.name,
                                            contentScale = ContentScale.Fit,
                                            error = {
                                                DeviceGraphicPlaceholder(
                                                    title = slotProduct.name,
                                                    brand = slotProduct.brand,
                                                    tags = slotProduct.tags,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            },
                                            modifier = Modifier.fillMaxSize().padding(2.dp)
                                        )
                                    } else {
                                        DeviceGraphicPlaceholder(
                                            title = slotProduct.name,
                                            brand = slotProduct.brand,
                                            tags = slotProduct.tags,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = slotProduct.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                "✓ Active",
                                                color = Color(0xFF92400E),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${if (slotProduct.brand.isNotBlank()) slotProduct.brand else "No brand"} • ${productSpecs.size} specifications",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onEditProduct(slotProduct) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Specs", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    } else {
                        // Empty slot button trigger
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    slotPickerIndex = if (isSlotOpen) null else index
                                    searchQuery = ""
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEEF2FF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Search and select a product",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Tap to search and pick from your catalog (1,000+ records supported)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    // Fast Popover Product Picker (Instant search without lag)
                    if (isSlotOpen) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Search product name, brand, tags, or specs...",
                                            fontSize = 13.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = Color(0xFF4F46E5),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Instant lookup across all ${allAvailableProducts.size} products",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )

                                    if (selectedTagFilter != "All") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Filtered by: ${selectedTagFilter}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                                            Text(
                                                "Show all",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2563EB),
                                                modifier = Modifier.clickable { selectedTagFilter = "All" }
                                            )
                                        }
                                    }
                                }

                                // Scrollable search results list
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (slotMatchingProducts.isEmpty()) {
                                        Text(
                                            "No products found matching this search/filter.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    } else {
                                        slotMatchingProducts.take(30).forEach { itemProduct ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color.White,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        productSlots[index] = itemProduct.id
                                                        slotPickerIndex = null
                                                        searchQuery = ""
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFFF1F5F9),
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        val pPic = itemProduct.pictures.firstOrNull()?.url
                                                        val pPicName = itemProduct.pictures.firstOrNull()?.name
                                                        val context = LocalContext.current
                                                        if (!pPic.isNullOrBlank() || !pPicName.isNullOrBlank()) {
                                                            SubcomposeAsyncImage(
                                                                model = ImageRequest.Builder(context)
                                                                    .data(resolveImageModel(context, pPic ?: "", preferredName = pPicName))
                                                                    .crossfade(true)
                                                                    .build(),
                                                                contentDescription = itemProduct.name,
                                                                contentScale = ContentScale.Fit,
                                                                error = {
                                                                    DeviceGraphicPlaceholder(
                                                                        title = itemProduct.name,
                                                                        brand = itemProduct.brand,
                                                                        tags = itemProduct.tags,
                                                                        modifier = Modifier.fillMaxSize()
                                                                    )
                                                                },
                                                                modifier = Modifier.fillMaxSize().padding(1.dp)
                                                            )
                                                        } else {
                                                            DeviceGraphicPlaceholder(
                                                                title = itemProduct.name,
                                                                brand = itemProduct.brand,
                                                                tags = itemProduct.tags,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                    }

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = itemProduct.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0F172A),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${if (itemProduct.brand.isNotBlank()) itemProduct.brand else "No brand"}",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }

                                                    Icon(
                                                        Icons.Default.ChevronRight,
                                                        contentDescription = "Pick",
                                                        tint = Color(0xFF94A3B8),
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
                }
            }
        }

        // =================================================================
        // 3. SLOTS SUMMARY AND JUMP BAR
        // =================================================================
        item {
            AppSummaryCard(
                statsText = "${filledSlotsCount} of ${productSlots.size} slots filled",
                secondaryText = "${matrixSpecs.size} differences detected",
                onReset = { showResetSlotsConfirm = true },
                resetLabel = "Reset slots",
                actionButtonText = if (filledSlotsCount >= 2) "Go to matrix (${matrixSpecs.size} differences)" else null,
                onActionClick = if (filledSlotsCount >= 2) {
                    {
                        coroutineScope.launch {
                            listState.animateScrollToItem(productSlots.size + 3)
                        }
                    }
                } else null
            )
        }

        // =================================================================
        // 8. SELECTED PRODUCTS & SPECIFICATIONS MATRIX (Smart add product 3 - 5.PNG)
        // =================================================================
        if (selectedProducts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SELECTED PRODUCTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "All details and specifications",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${matrixSpecs.size} fields shown across ${selectedProducts.size} products",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    // Stats Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0F172A)
                        ) {
                            Text(
                                "${selectedProducts.size} selected",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFE0F2FE),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD))
                        ) {
                            Text(
                                "${matrixSpecs.size} differences",
                                color = Color(0xFF0284C7),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Card: New product name / Create and compare (Smart add product 3.PNG)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "New product name",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )

                        OutlinedTextField(
                            value = newProductName,
                            onValueChange = { newProductName = it },
                            placeholder = { Text("Enter product name", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFF64748B)
                                )
                                Text(
                                    "${matrixSpecs.size} blank specifications",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (newProductName.isNotBlank()) {
                                    val nameToCreate = newProductName.trim()
                                    val newId = "rec_" + UUID.randomUUID().toString().replace("-", "").take(17)
                                    val tempProd = ProductItem(
                                        id = newId,
                                        name = nameToCreate,
                                        brand = "Generic",
                                        tags = listOf("Smart Add"),
                                        overview = "Created with ${matrixSpecs.size} specifications extracted from comparison matrix.",
                                        pictures = emptyList(),
                                        createdTime = System.currentTimeMillis().toString()
                                    )
                                    localNewProducts.add(tempProd)
                                    val emptyIdx = productSlots.indexOfFirst { it == null }
                                    if (emptyIdx != -1) {
                                        productSlots[emptyIdx] = newId
                                    } else {
                                        productSlots.add(newId)
                                    }
                                    targetProductForSpecId = newId
                                    newProductName = ""

                                    onCreateAndCompare(
                                        nameToCreate,
                                        "Generic",
                                        matrixSpecs,
                                        newId
                                    ) {
                                        // Synchronously updated
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF334155),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Create and compare", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Card: Add specification inline (Smart add product 3.PNG)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Product",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )

                        // Target product picker dropdown
                        var isTargetPickerOpen by remember { mutableStateOf(false) }
                        val currentTargetProd = selectedProducts.find { it.id == targetProductForSpecId }
                            ?: allAvailableProducts.find { it.id == targetProductForSpecId }
                            ?: selectedProducts.firstOrNull()
                        val currentTargetLabel = if (currentTargetProd != null) {
                            "${currentTargetProd.name}${if (currentTargetProd.id in newlyCreatedIds) " (New)" else ""}"
                        } else "Select product"

                        Box {
                            OutlinedButton(
                                onClick = { isTargetPickerOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        currentTargetLabel,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isTargetPickerOpen,
                                onDismissRequest = { isTargetPickerOpen = false }
                            ) {
                                selectedProducts.forEach { p ->
                                    val isNew = p.id in newlyCreatedIds
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(p.name, fontWeight = if (isNew) FontWeight.Bold else FontWeight.Normal)
                                                if (isNew) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFFEF3C7)
                                                    ) {
                                                        Text(
                                                            "(New)",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF92400E),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            targetProductForSpecId = p.id
                                            isTargetPickerOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            "New specification",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                        OutlinedTextField(
                            value = newSpecName,
                            onValueChange = { newSpecName = it },
                            placeholder = { Text("Specification name", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Text(
                            "Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                        OutlinedTextField(
                            value = newSpecDetails,
                            onValueChange = { newSpecDetails = it },
                            placeholder = { Text("Optional value", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (newSpecName.isNotBlank() && currentTargetProd != null) {
                                    onAddSpecificationInline(
                                        currentTargetProd.id,
                                        currentTargetProd.name,
                                        newSpecName.trim(),
                                        newSpecDetails.trim(),
                                        null
                                    )
                                    newSpecName = ""
                                    newSpecDetails = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF64748B),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add specification", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Card: Selected products stack with reorder (Smart add product 4.PNG)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Selected products",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )

                        selectedProducts.forEachIndexed { index, product ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Thumbnail
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
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                product.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            if (product.id in newlyCreatedIds) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFFEF3C7)
                                                ) {
                                                    Text(
                                                        "New",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF92400E),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { onEditProduct(product) }
                                            )
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState())
                                        ) {
                                            Text(
                                                product.brand.ifBlank { "Generic" },
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Move down
                                        IconButton(
                                            onClick = {
                                                val activeList = productSlots.filterNotNull().toMutableList()
                                                if (index < activeList.size - 1) {
                                                    val temp = activeList[index]
                                                    activeList[index] = activeList[index + 1]
                                                    activeList[index + 1] = temp
                                                    productSlots.clear()
                                                    productSlots.addAll(activeList)
                                                    while (productSlots.size < 2) productSlots.add(null)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp),
                                            enabled = index < selectedProducts.size - 1
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowDownward,
                                                contentDescription = "Down",
                                                tint = if (index < selectedProducts.size - 1) Color(0xFF64748B) else Color(0xFFCBD5E1),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Move up
                                        IconButton(
                                            onClick = {
                                                val activeList = productSlots.filterNotNull().toMutableList()
                                                if (index > 0) {
                                                    val temp = activeList[index]
                                                    activeList[index] = activeList[index - 1]
                                                    activeList[index - 1] = temp
                                                    productSlots.clear()
                                                    productSlots.addAll(activeList)
                                                    while (productSlots.size < 2) productSlots.add(null)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp),
                                            enabled = index > 0
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowUpward,
                                                contentDescription = "Up",
                                                tint = if (index > 0) Color(0xFF64748B) else Color(0xFFCBD5E1),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Specs button (Screenshot 6.png)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF1F5F9),
                                            modifier = Modifier
                                                .height(28.dp)
                                                .clickable { selectedProductForSpecOrder = product }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Tune,
                                                    contentDescription = null,
                                                    tint = Color(0xFF475569),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    "Specs",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF334155)
                                                )
                                            }
                                        }

                                        // Remove
                                        IconButton(
                                            onClick = {
                                                val pIdx = productSlots.indexOf(product.id)
                                                if (pIdx != -1) {
                                                    slotToRemoveIndex = pIdx
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color(0xFF94A3B8),
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

            // Card: Brand Differences (Smart add product 4.PNG)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Label, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                Text("Brand", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0284C7)
                            ) {
                                Text(
                                    "Different",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Divider(color = Color(0xFFF1F5F9))

                        selectedProducts.forEach { product ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        product.name.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color(0xFF0F172A),
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                if (editingBrandProductId == product.id) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editingBrandText,
                                            onValueChange = { editingBrandText = it },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        IconButton(onClick = {
                                            onUpdateProductBrand(product.id, editingBrandText)
                                            editingBrandProductId = null
                                        }) {
                                            Icon(Icons.Default.Save, contentDescription = "Save", tint = Color(0xFF10B981))
                                        }
                                        IconButton(onClick = { editingBrandProductId = null }) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFF94A3B8))
                                        }
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(product.brand, fontSize = 14.sp, color = Color(0xFF334155))
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit brand",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    editingBrandProductId = product.id
                                                    editingBrandText = product.brand
                                                }
                                        )
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Clear brand",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    onUpdateProductBrand(product.id, "")
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Cards: Specifications with Different badges and Inline Editors (Smart add product 5.PNG)
            items(matrixSpecs) { specName ->
                val activeUnit = remember(specifications, specName) {
                    specifications.find {
                        it.specName.equals(specName, ignoreCase = true) && !it.unit.isNullOrBlank()
                    }?.unit?.trim()
                }
                val isWeight = specName.equals("Weight", ignoreCase = true)
                val isBattery = specName.equals("Battery", ignoreCase = true)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    specName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (!activeUnit.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Text(
                                            text = activeUnit,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0284C7)
                            ) {
                                Text(
                                    "Different",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Divider(color = Color(0xFFF1F5F9))

                        selectedProducts.forEach { product ->
                            val existingSpec = specifications.find {
                                (it.productId == product.id || it.productTitle == product.name) &&
                                        it.specName.equals(specName, ignoreCase = true)
                            }
                            val key = "${product.id}|$specName"
                            val isEditingThis = activeInlineEditKey == key

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        product.name.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color(0xFF0F172A),
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                if (isEditingThis) {
                                    // Expanding inline sub-card editor (IMG_9629 / Smart add product 5)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text("Specification", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                                            OutlinedTextField(
                                                value = specName,
                                                onValueChange = {},
                                                readOnly = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Details", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                                                Text("WYSIWYG: Bold, underline, list & links", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                            }
                                            HtmlWysiwygEditor(
                                                value = inlineSpecDetails,
                                                onChange = { inlineSpecDetails = it },
                                                placeholder = "Details, multi-line notes, or bold feature...",
                                                minHeight = 90.dp,
                                                testTagPrefix = "smart_add_spec_details"
                                            )

                                            // Photos picker
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Specification pictures", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                                OutlinedButton(
                                                    onClick = {
                                                        photoPickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Choose pictures", fontSize = 11.sp)
                                                }
                                            }

                                            // Action buttons
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Button(
                                                    onClick = {
                                                        onUpdateInlineSpec(
                                                            product.id,
                                                            product.name,
                                                            specName,
                                                            inlineSpecDetails,
                                                            inlineSpecPictures.toList(),
                                                            existingSpec?.unit
                                                        )
                                                        activeInlineEditKey = null
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                                                ) {
                                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Save", fontSize = 12.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = { activeInlineEditKey = null },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Cancel", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                } else if (existingSpec != null && existingSpec.details.isNotBlank()) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FormattedSpecHtml(
                                                content = existingSpec.details,
                                                fontSize = 14.sp
                                            )
                                            SpecUnitBadge(
                                                unit = existingSpec.unit ?: activeUnit,
                                                details = existingSpec.details
                                            )
                                        }

                                        if (existingSpec.pictures.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState())
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                existingSpec.pictures.forEach { pic ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(width = 64.dp, height = 52.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color(0xFFF8FAFC))
                                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                                    ) {
                                                        AsyncImage(
                                                            model = resolveImageModel(
                                                                LocalContext.current,
                                                                pic.url,
                                                                preferredName = pic.name
                                                            ),
                                                            contentDescription = pic.name,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Action buttons positioned down below, not on the right side
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    activeInlineEditKey = key
                                                    inlineSpecDetails = existingSpec.details
                                                    inlineSpecPictures.clear()
                                                    inlineSpecPictures.addAll(existingSpec.pictures)
                                                },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Color(0xFF64748B)
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text("Edit", fontSize = 12.sp, color = Color(0xFF64748B))
                                            }
                                            TextButton(
                                                onClick = { specToDeleteInline = Pair(product.id, specName) },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text("Delete", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                } else {
                                    // No data state (Smart add product 5.PNG)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            "No data",
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 13.sp
                                        )

                                        Button(
                                            onClick = {
                                                activeInlineEditKey = key
                                                inlineSpecDetails = ""
                                                inlineSpecPictures.clear()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFF1F5F9),
                                                contentColor = Color(0xFF0F172A)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Add data", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            "No products selected in slots yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            "Select products into the slots above to compare their specifications, brand, and details side-by-side.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    selectedProductForSpecOrder?.let { prod ->
        SpecificationsOrderModal(
            product = prod,
            specifications = specifications,
            onSaveOrder = { updatedSpecs ->
                updatedSpecs.forEach { spec ->
                    onUpdateInlineSpec(
                        spec.productId,
                        spec.productTitle,
                        spec.specName,
                        spec.details,
                        spec.pictures,
                        spec.unit
                    )
                }
            },
            onDeleteSpec = { specName ->
                onDeleteSpecInline(prod.id, specName)
            },
            onDismiss = { selectedProductForSpecOrder = null }
        )
    }

    // Delete Confirmation: Slot removal
    if (slotToRemoveIndex != null) {
        val index = slotToRemoveIndex!!
        val slotProdId = productSlots.getOrNull(index)
        val slotProd = slotProdId?.let { id -> allAvailableProducts.find { it.id == id } }
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
            testTagPrefix = "smart_add_remove_slot",
            onConfirm = {
                if (index in productSlots.indices) {
                    if (productSlots.size > 2) {
                        productSlots.removeAt(index)
                        if (slotPickerIndex == index) slotPickerIndex = null
                    } else {
                        productSlots[index] = null
                    }
                }
                slotToRemoveIndex = null
            },
            onDismiss = { slotToRemoveIndex = null }
        )
    }

    // Delete Confirmation: Reset all slots
    if (showResetSlotsConfirm) {
        val activeProducts = productSlots.filterNotNull().mapNotNull { id -> allAvailableProducts.find { it.id == id } }
        val message = if (activeProducts.isNotEmpty()) {
            val namesStr = activeProducts.joinToString(", ") { "\"${it.name}\"" }
            "Are you sure you want to reset all slots? This will remove ${activeProducts.size} product${if (activeProducts.size > 1) "s" else ""} ($namesStr) from comparison."
        } else {
            "Are you sure you want to reset all comparison slots?"
        }
        DeleteConfirmDialog(
            title = "Reset All Slots?",
            message = message,
            confirmText = "Reset",
            testTagPrefix = "smart_add_reset_slots",
            onConfirm = {
                productSlots.clear()
                productSlots.addAll(listOf(null, null))
                slotPickerIndex = null
                showResetSlotsConfirm = false
            },
            onDismiss = { showResetSlotsConfirm = false }
        )
    }

    // Delete Confirmation: Inline specification deletion
    if (specToDeleteInline != null) {
        val (prodId, specName) = specToDeleteInline!!
        val prod = allAvailableProducts.find { it.id == prodId }
        val prodName = prod?.name ?: "product"
        DeleteConfirmDialog(
            title = "Delete Specification?",
            message = "Are you sure you want to delete specification \"$specName\" from \"$prodName\"?",
            confirmText = "Delete",
            testTagPrefix = "smart_add_delete_spec",
            onConfirm = {
                onDeleteSpecInline(prodId, specName)
                specToDeleteInline = null
            },
            onDismiss = { specToDeleteInline = null }
        )
    }
}
