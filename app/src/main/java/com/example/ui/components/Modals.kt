package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.PictureItem
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.SpecificationItem
import com.example.model.copyPickedUriToPermanentStorage
import com.example.model.resolveImageModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProductDetailModal(
    product: ProductItem,
    specifications: List<SpecificationItem>,
    reviews: List<ReviewItem>,
    isCompared: Boolean,
    onToggleCompare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddSpec: () -> Unit,
    onAddReview: () -> Unit,
    onReviewClick: (ReviewItem) -> Unit,
    onEditReview: ((ReviewItem) -> Unit)? = null,
    onEditSpec: ((SpecificationItem) -> Unit)? = null,
    onDeleteSpec: ((SpecificationItem) -> Unit)? = null,
    onSaveSpec: ((SpecificationItem) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("product_detail_modal"),
            color = Color.White
        ) {
            ProductDetailPane(
                product = product,
                specifications = specifications,
                reviews = reviews,
                isCompared = isCompared,
                onToggleCompare = onToggleCompare,
                onEdit = onEdit,
                onDelete = onDelete,
                onAddSpec = onAddSpec,
                onAddReview = onAddReview,
                onReviewClick = onReviewClick,
                onEditReview = onEditReview,
                onEditSpec = onEditSpec,
                onDeleteSpec = onDeleteSpec,
                onSaveSpec = onSaveSpec,
                onClose = onDismiss,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun ProductDetailPane(
    product: ProductItem,
    specifications: List<SpecificationItem>,
    reviews: List<ReviewItem>,
    isCompared: Boolean,
    onToggleCompare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddSpec: () -> Unit,
    onAddReview: () -> Unit,
    onReviewClick: (ReviewItem) -> Unit,
    onEditReview: ((ReviewItem) -> Unit)? = null,
    onEditSpec: ((SpecificationItem) -> Unit)? = null,
    onDeleteSpec: ((SpecificationItem) -> Unit)? = null,
    onSaveSpec: ((SpecificationItem) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletingSpec by remember { mutableStateOf<SpecificationItem?>(null) }
    var editingSpec by remember { mutableStateOf<SpecificationItem?>(null) }
    var activeLightboxImages by remember { mutableStateOf<List<PictureItem>?>(null) }
    var activeLightboxIndex by remember { mutableIntStateOf(0) }

    val formattedCreatedDate = remember(product.createdTime) {
        formatBeautifulDate(product.createdTime)
    }

    Surface(
        modifier = modifier.testTag("product_detail_pane"),
        color = Color.White
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Section: Back Button + Product Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                if (onClose != null) {
                                    IconButton(
                                        onClick = onClose,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("close_detail_modal_btn")
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp).padding(start = 4.dp)
                                    )
                                }
                                Text(
                                    text = product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Right Section: Review, Edit, Compare, Delete, Close
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. Review button (indigo pill)
                                Surface(
                                    onClick = onAddReview,
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF4F46E5),
                                    modifier = Modifier.testTag("detail_review_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Chat,
                                            contentDescription = "Review",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Review",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // 2. Edit button (dark slate pill with yellow icon)
                                Surface(
                                    onClick = onEdit,
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.testTag("detail_edit_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color(0xFFFACC15),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Edit",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // 3. Scale / Compare icon button
                                Surface(
                                    onClick = onToggleCompare,
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCompared) Color(0xFFEEF2FF) else Color.White,
                                    border = BorderStroke(1.dp, if (isCompared) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("detail_compare_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Scale,
                                            contentDescription = "Compare",
                                            tint = if (isCompared) Color(0xFF4F46E5) else Color(0xFF64748B),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }

                                // 4. Trash / Delete icon button
                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("detail_delete_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                if (onClose != null) {
                                    // 5. Close icon button
                                    IconButton(
                                        onClick = onClose,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("detail_close_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Hero Picture Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        ) {
                            if (product.pictures.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = resolveImageModel(
                                            LocalContext.current,
                                            product.pictures.first().url,
                                            preferredName = product.pictures.first().name
                                        ),
                                        contentDescription = product.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                activeLightboxImages = product.pictures
                                                activeLightboxIndex = 0
                                            },
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DeviceGraphicPlaceholder(
                                        title = product.name,
                                        brand = product.brand,
                                        tags = product.tags,
                                        modifier = Modifier.size(96.dp)
                                    )
                                }
                            }
                        }

                        if (product.pictures.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                product.pictures.forEachIndexed { pIdx, pPic ->
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                            .clickable {
                                                activeLightboxImages = product.pictures
                                                activeLightboxIndex = pIdx
                                            }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = resolveImageModel(
                                                LocalContext.current,
                                                pPic.url,
                                                preferredName = pPic.name
                                            ),
                                            contentDescription = pPic.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 1: PRODUCT INFORMATION
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalOffer,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "PRODUCT INFORMATION",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF0F172A),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // PRODUCT NAME
                                    Column {
                                        Text(
                                            text = "PRODUCT NAME",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    // BRAND
                                    Column {
                                        Text(
                                            text = "BRAND",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEEF2FF),
                                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                        ) {
                                            Text(
                                                text = product.brand.ifBlank { "UNBRANDED" }.uppercase(),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF4338CA),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // TAGS
                                    Column {
                                        Text(
                                            text = "TAGS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        if (product.tags.isEmpty()) {
                                            Text("None", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        } else {
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                product.tags.forEach { tag ->
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFF8FAFC),
                                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                                    ) {
                                                        Text(
                                                            text = tag,
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF334155),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    // Total Specifications
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Total Specifications:", fontSize = 13.sp, color = Color(0xFF334155))
                                        Text(
                                            "${specifications.size} attributes",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    // Community Reviews
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Community Reviews:", fontSize = 13.sp, color = Color(0xFF334155))
                                        Text(
                                            "${reviews.size} reviews",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    // Record Created
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Record Created:", fontSize = 13.sp, color = Color(0xFF334155))
                                        Text(
                                            formattedCreatedDate,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Divider between Section 1 and Section 2
                    item {
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                    }

                    // Section 2: PRODUCT OVERVIEW
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "PRODUCT OVERVIEW",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF0F172A),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FormattedSpecHtml(
                                    content = product.overview.ifBlank { "No overview provided." },
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    // Section 3: FULL SPECIFICATIONS SHEET
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Layers,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "FULL SPECIFICATIONS SHEET (${specifications.size})",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0F172A),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                TextButton(
                                    onClick = onAddSpec,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Spec", fontSize = 11.sp)
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    // Table Header Row
                                    Surface(
                                        color = Color(0xFFF8FAFC),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B),
                                                modifier = Modifier.width(22.dp)
                                            )
                                            Text(
                                                text = "SPECIFICATION",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B),
                                                modifier = Modifier.weight(0.28f)
                                            )
                                            Text(
                                                text = "DETAILS / VALUE",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B),
                                                modifier = Modifier.weight(0.54f)
                                            )
                                            Text(
                                                text = "ACTIONS",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B),
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.width(52.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    if (specifications.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No specifications recorded for this product.",
                                                fontSize = 12.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    } else {
                                        specifications.forEachIndexed { index, spec ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                // Column 1: #
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF94A3B8),
                                                    modifier = Modifier
                                                        .width(22.dp)
                                                        .padding(top = 2.dp)
                                                )

                                                // Column 2: SPECIFICATION
                                                Text(
                                                    text = spec.specName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A),
                                                    modifier = Modifier
                                                        .weight(0.28f)
                                                        .padding(top = 2.dp)
                                                )

                                                // Column 3: DETAILS / VALUE
                                                Column(
                                                    modifier = Modifier.weight(0.54f),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    FormattedSpecHtml(
                                                        content = spec.details,
                                                        fontSize = 13.sp,
                                                        lineHeight = 18.sp
                                                    )
                                                    if (!spec.unit.isNullOrBlank()) {
                                                        SpecUnitBadge(unit = spec.unit, details = spec.details)
                                                    }

                                                    // Thumbnail picture if present
                                                    if (spec.pictures.isNotEmpty()) {
                                                        val pic = spec.pictures.first()
                                                        Column(
                                                            modifier = Modifier.padding(top = 4.dp),
                                                            horizontalAlignment = Alignment.Start
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(width = 72.dp, height = 62.dp)
                                                                    .clip(RoundedCornerShape(10.dp))
                                                                    .background(Color(0xFFF8FAFC))
                                                                    .border(
                                                                        1.dp,
                                                                        Color(0xFFCBD5E1),
                                                                        RoundedCornerShape(10.dp)
                                                                    )
                                                                    .clickable {
                                                                        activeLightboxImages = spec.pictures
                                                                        activeLightboxIndex = 0
                                                                    }
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
                                                                Surface(
                                                                    color = Color(0xDD334155),
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .align(Alignment.BottomCenter)
                                                                ) {
                                                                    Text(
                                                                        text = pic.name.ifBlank {
                                                                            "IMG_${spec.id.takeLast(4)}"
                                                                        },
                                                                        color = Color.White,
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Medium,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis,
                                                                        modifier = Modifier.padding(
                                                                            horizontal = 3.dp,
                                                                            vertical = 2.dp
                                                                        ),
                                                                        textAlign = TextAlign.Center
                                                                    )
                                                                }
                                                            }

                                                            Row(
                                                                modifier = Modifier
                                                                    .padding(top = 4.dp)
                                                                    .clickable {
                                                                        activeLightboxImages = spec.pictures
                                                                        activeLightboxIndex = 0
                                                                    },
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.OpenInFull,
                                                                    contentDescription = "Expand",
                                                                    tint = Color(0xFF64748B),
                                                                    modifier = Modifier.size(10.dp)
                                                                )
                                                                Text(
                                                                    text = "1-click full screen • Drag left/right • Pinch zoom",
                                                                    fontSize = 10.sp,
                                                                    color = Color(0xFF64748B),
                                                                    lineHeight = 12.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // Column 4: ACTIONS
                                                Row(
                                                    modifier = Modifier.width(56.dp),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    IconButton(
                                                        onClick = { editingSpec = spec },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Edit,
                                                            contentDescription = "Edit spec",
                                                            tint = Color(0xFF64748B),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { deletingSpec = spec },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Delete,
                                                            contentDescription = "Delete spec",
                                                            tint = Color(0xFF64748B),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (index < specifications.size - 1) {
                                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Customer Reviews section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Chat,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "CUSTOMER REVIEWS (${reviews.size})",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0F172A),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                TextButton(
                                    onClick = onAddReview,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Add Review",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }

                            if (reviews.isEmpty()) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No reviews yet for this product.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    reviews.forEach { review ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Header row: Verified Reviewer on left, Review #X and actions on right
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = review.authorName.ifBlank { "Verified Reviewer" },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Review #${review.reviewNumber}",
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF64748B),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        IconButton(
                                                            onClick = { onReviewClick(review) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Visibility,
                                                                contentDescription = "View Review",
                                                                tint = Color(0xFF64748B),
                                                                modifier = Modifier.size(17.dp)
                                                            )
                                                        }
                                                        if (onEditReview != null) {
                                                            IconButton(
                                                                onClick = { onEditReview.invoke(review) },
                                                                modifier = Modifier.size(28.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.Edit,
                                                                    contentDescription = "Edit Review",
                                                                    tint = Color(0xFF64748B),
                                                                    modifier = Modifier.size(17.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                 // Extract all pictures (explicit + embedded markdown/HTML images)
                                                 val reviewPictures = remember(review) {
                                                     val list = mutableListOf<PictureItem>()
                                                     if (review.pictures.isNotEmpty()) list.addAll(review.pictures)
                                                     val imgRegex = Regex("""<img\b([^>]*)>""", RegexOption.IGNORE_CASE)
                                                     imgRegex.findAll(review.body).forEachIndexed { idx, match ->
                                                         val attrs = match.groupValues[1]
                                                         val src = Regex("""src=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1)?.trim() ?: ""
                                                         val attId = Regex("""(?:data-attachment-id|dataAttachmentId|data-attachment-token)=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1)?.trim() ?: ""
                                                         val alt = Regex("""alt=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1)?.trim() ?: "Photo ${list.size + 1}"
                                                         val effectiveUrl = when {
                                                             src.isNotBlank() && !src.startsWith("blob:") -> src
                                                             attId.isNotBlank() -> "https://local.review/review_images/$attId"
                                                             else -> ""
                                                         }
                                                         if (effectiveUrl.isNotBlank() && list.none { it.url == effectiveUrl || (attId.isNotBlank() && it.id == attId) }) {
                                                             list.add(PictureItem(id = attId.ifBlank { "card_img_$idx" }, name = alt, url = effectiveUrl))
                                                         }
                                                     }
                                                     val mdImgRegex = Regex("""!\[(.*?)\]\((.*?)\)""")
                                                     mdImgRegex.findAll(review.body).forEachIndexed { idx, match ->
                                                         val alt = match.groupValues[1].ifBlank { "Photo ${list.size + 1}" }
                                                         val url = match.groupValues[2].trim()
                                                         if (url.isNotBlank() && list.none { it.url == url }) {
                                                             list.add(PictureItem(id = "md_img_$idx", name = alt, url = url))
                                                         }
                                                     }
                                                     list
                                                 }

                                                 // Clean rich text review snippet with zero raw HTML tags or image URLs
                                                 val cleanPreviewText = remember(review.body) {
                                                     var text = review.body
                                                     text = text.replace(Regex("""!\[.*?\]\(.*?\)"""), " ")
                                                     text = text.replace(Regex("""<figure\b[^>]*>[\s\S]*?<\/figure>""", RegexOption.IGNORE_CASE), " ")
                                                     text = text.replace(Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE), " ")
                                                     text = text.replace(Regex("""https?://local\.review/[^\s<>"'\)]+""", RegexOption.IGNORE_CASE), " ")
                                                     text = text.replace(Regex("""<[^>]*>"""), " ")
                                                     text = text.replace(Regex("""\s+"""), " ").trim()
                                                     text
                                                 }

                                                 if (cleanPreviewText.isNotBlank()) {
                                                     Text(
                                                         text = cleanPreviewText,
                                                         fontSize = 13.sp,
                                                         lineHeight = 19.sp,
                                                         color = Color(0xFF334155),
                                                         maxLines = 3,
                                                         overflow = TextOverflow.Ellipsis,
                                                         modifier = Modifier.clickable { onReviewClick(review) }
                                                     )
                                                 }

                                                 // Thumbnail pictures gallery
                                                 if (reviewPictures.isNotEmpty()) {
                                                     Row(
                                                         modifier = Modifier
                                                             .fillMaxWidth()
                                                             .horizontalScroll(rememberScrollState()),
                                                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                     ) {
                                                         reviewPictures.forEachIndexed { picIndex, pic ->
                                                             Box(
                                                                 modifier = Modifier
                                                                     .size(64.dp)
                                                                     .clip(RoundedCornerShape(10.dp))
                                                                     .background(Color(0xFFF8FAFC))
                                                                     .border(
                                                                         1.dp,
                                                                         Color(0xFFCBD5E1),
                                                                         RoundedCornerShape(10.dp)
                                                                     )
                                                                     .clickable {
                                                                         activeLightboxImages = reviewPictures
                                                                         activeLightboxIndex = picIndex
                                                                     }
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
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            title = "Delete Product?",
            message = "Are you sure you want to delete \"${product.name}\"? This will also remove its associated specifications.",
            confirmText = "Delete",
            testTagPrefix = "detail_delete_product",
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
                onClose?.invoke()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (deletingSpec != null) {
        val specToDelete = deletingSpec!!
        DeleteConfirmDialog(
            title = "Delete Specification?",
            message = "Are you sure you want to delete specification \"${specToDelete.specName}\"?",
            confirmText = "Delete",
            testTagPrefix = "detail_delete_spec",
            onConfirm = {
                onDeleteSpec?.invoke(specToDelete)
                deletingSpec = null
            },
            onDismiss = { deletingSpec = null }
        )
    }

    if (editingSpec != null) {
        val specToEdit = editingSpec!!
        EditSpecModal(
            specification = specToEdit,
            onSave = { updated ->
                onSaveSpec?.invoke(updated)
                onEditSpec?.invoke(updated)
                editingSpec = null
            },
            onDelete = { specId ->
                onDeleteSpec?.invoke(specToEdit)
                editingSpec = null
            },
            onDismiss = { editingSpec = null }
        )
    }

    if (activeLightboxImages != null) {
        FullScreenPhotoLightbox(
            images = activeLightboxImages!!,
            initialIndex = activeLightboxIndex,
            onDismiss = { activeLightboxImages = null }
        )
    }
}

@Composable
fun EditProductModal(
    product: ProductItem,
    onSave: (ProductItem) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var brand by remember { mutableStateOf(product.brand) }
    var tagsInput by remember { mutableStateOf(product.tags.joinToString(", ")) }
    var overview by remember { mutableStateOf(product.overview) }

    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("edit_product_modal"),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.testTag("edit_product_back_btn")) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Column {
                                    Text("Edit Product", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                    Text(product.name, fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }

                            Button(
                                onClick = {
                                    val updatedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    onSave(
                                        product.copy(
                                            name = name.trim(),
                                            brand = brand.trim(),
                                            tags = updatedTags,
                                            overview = overview.trim()
                                        )
                                    )
                                    onDismiss()
                                },
                                enabled = name.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                modifier = Modifier.testTag("edit_product_save_btn")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Product Name *") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_product_name_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = brand,
                                    onValueChange = { brand = it },
                                    label = { Text("Brand") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_product_brand_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = tagsInput,
                                    onValueChange = { tagsInput = it },
                                    label = { Text("Tags (comma separated)") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_product_tags_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Product Overview", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF334155))
                                        Text("WYSIWYG: Bold, underline, list & links", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                    HtmlWysiwygEditor(
                                        value = overview,
                                        onChange = { overview = it },
                                        placeholder = "Summarize what the product is, who it is for, and what makes it notable...",
                                        minHeight = 110.dp,
                                        testTagPrefix = "edit_product_overview"
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

@Composable
fun AddProductModal(
    onAdd: (name: String, brand: String, tags: List<String>, overview: String, pictures: List<PictureItem>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var overview by remember { mutableStateOf("") }

    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("add_product_modal"),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.testTag("add_product_back_btn")) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Column {
                                    Text("Add New Product", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                    Text("Create a new product record", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }

                            Button(
                                onClick = {
                                    val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    onAdd(name.trim(), brand.trim(), tags, overview.trim(), emptyList())
                                    onDismiss()
                                },
                                enabled = name.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                modifier = Modifier.testTag("add_product_confirm_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Create", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Product Name *") },
                                    modifier = Modifier.fillMaxWidth().testTag("add_product_name_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = brand,
                                    onValueChange = { brand = it },
                                    label = { Text("Brand") },
                                    modifier = Modifier.fillMaxWidth().testTag("add_product_brand_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = tagsInput,
                                    onValueChange = { tagsInput = it },
                                    label = { Text("Tags (e.g. Smartphone, Audio)") },
                                    modifier = Modifier.fillMaxWidth().testTag("add_product_tags_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Product Overview", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF334155))
                                        Text("WYSIWYG: Bold, underline, list & links", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                    HtmlWysiwygEditor(
                                        value = overview,
                                        onChange = { overview = it },
                                        placeholder = "Summarize what the product is, who it is for, and what makes it notable...",
                                        minHeight = 110.dp,
                                        testTagPrefix = "add_product_overview"
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

@Composable
fun AddSpecModal(
    product: ProductItem,
    initialSpecName: String = "",
    initialUnit: String = "",
    initialIsNumeric: Boolean? = null,
    initialDetails: String = "",
    onAdd: (productId: String, productTitle: String, specName: String, details: String, unit: String?, isNumeric: Boolean?) -> Unit,
    onDismiss: () -> Unit
) {
    var specName by remember { mutableStateOf(initialSpecName) }
    var details by remember { mutableStateOf(initialDetails) }
    var unit by remember { mutableStateOf(initialUnit) }
    var isNumeric by remember { mutableStateOf<Boolean?>(initialIsNumeric) }

    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("add_spec_modal"),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.testTag("add_spec_back_btn")) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Column {
                                    Text("Add Specification", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                    Text("For ${product.name}", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }

                            Button(
                                onClick = {
                                    onAdd(
                                        product.id,
                                        product.name,
                                        specName.trim(),
                                        details.trim(),
                                        unit.trim().ifBlank { null },
                                        isNumeric
                                    )
                                    onDismiss()
                                },
                                enabled = specName.isNotBlank() && details.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                modifier = Modifier.testTag("add_spec_save_btn")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save Spec", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = specName,
                                    onValueChange = { specName = it },
                                    label = { Text("Specification Name * (e.g. Display, Battery)") },
                                    modifier = Modifier.fillMaxWidth().testTag("add_spec_name_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = unit,
                                    onValueChange = { unit = it },
                                    label = { Text("Unit (optional)") },
                                    placeholder = { Text("e.g. g, inch, cm, year, mAh, Hz, $") },
                                    modifier = Modifier.fillMaxWidth().testTag("add_spec_unit_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // Quick unit pills
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Quick unit:", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                    listOf("g", "kg", "inch", "cm", "mm", "year", "month", "mAh", "Hz", "$", "GB").forEach { qUnit ->
                                        val isSelected = unit.equals(qUnit, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFCBD5E1)),
                                            modifier = Modifier.clickable { unit = qUnit }
                                        ) {
                                            Text(
                                                text = qUnit,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else Color(0xFF334155),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Numeric Comparison Mode Toggle
                    item {
                        val hasNumericCandidate = remember(details) {
                            val clean = htmlToPlainText(details)
                            !clean.contains('\n') && !clean.contains('•') && clean.length <= 45 && Regex("[0-9]").containsMatchIn(clean)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (isNumeric) {
                                            true -> Color(0xFF4F46E5)
                                            false -> Color(0xFF475569)
                                            null -> Color(0xFF0F172A)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = when (isNumeric) {
                                                    true -> "#"
                                                    false -> "Aa"
                                                    null -> "⚡"
                                                },
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = when (isNumeric) {
                                                true -> "Numeric Comparison (Forced #)"
                                                false -> "Text Mode (Forced Aa)"
                                                null -> "Auto-Detect Mode"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = when (isNumeric) {
                                                true -> "Extracted as numeric value for matrix bar charts and higher/lower comparison"
                                                false -> "Preserved strictly as text/descriptive narrative without numeric sorting"
                                                null -> if (hasNumericCandidate) "Auto-detected: Numbers found in text" else "Auto-detected: Descriptive text"
                                            },
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        Triple("Auto", null, "add_spec_mode_auto"),
                                        Triple("Number (#)", true, "add_spec_mode_num"),
                                        Triple("Text (Aa)", false, "add_spec_mode_text")
                                    ).forEach { (label, value, tag) ->
                                        val isSelected = isNumeric == value
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { isNumeric = value }
                                                .testTag(tag)
                                        ) {
                                            Text(
                                                text = label,
                                                modifier = Modifier.padding(vertical = 7.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(0xFF334155)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Details field - Inline WYSIWYG Editor
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Specification Value / Details *", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF334155))
                                    Text("WYSIWYG: Bold, underline, list & links", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }

                                HtmlWysiwygEditor(
                                    value = details,
                                    onChange = { details = it },
                                    placeholder = "e.g. 5000 mAh fast charging with 65W adapter, or multi-line details with bullet points",
                                    minHeight = 110.dp,
                                    testTagPrefix = "add_spec_details"
                                )

                                if (unit.isNotBlank() && details.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text("Unit Badge:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        SpecUnitBadge(unit = unit, details = details)
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
fun EditSpecModal(
    specification: SpecificationItem,
    onSave: (SpecificationItem) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var specName by remember { mutableStateOf(specification.specName) }
    var details by remember { mutableStateOf(specification.details) }
    var unit by remember { mutableStateOf(specification.unit ?: "") }
    var isNumeric by remember { mutableStateOf(specification.isNumeric) }
    var pictures by remember { mutableStateOf(specification.pictures) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val specPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newPics = uris.map { uri ->
                copyPickedUriToPermanentStorage(context, uri, prefix = "spec_pic")
            }
            pictures = pictures + newPics
        }
    }

    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("edit_spec_modal"),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.testTag("edit_spec_back_btn")) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Column {
                                    Text(
                                        "Edit Specification",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        "${specification.productTitle} • ${specification.specName}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onSave(
                                        specification.copy(
                                            specName = specName.trim(),
                                            details = details.trim(),
                                            unit = unit.trim().ifBlank { null },
                                            isNumeric = isNumeric,
                                            pictures = pictures
                                        )
                                    )
                                    onDismiss()
                                },
                                enabled = specName.isNotBlank() && details.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                modifier = Modifier.testTag("edit_spec_save_top_btn")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.testTag("edit_spec_delete_btn")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete Spec", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = onDismiss) {
                                    Text("Cancel", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        onSave(
                                            specification.copy(
                                                specName = specName.trim(),
                                                details = details.trim(),
                                                unit = unit.trim().ifBlank { null },
                                                isNumeric = isNumeric,
                                                pictures = pictures
                                            )
                                        )
                                        onDismiss()
                                    },
                                    enabled = specName.isNotBlank() && details.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    modifier = Modifier.testTag("edit_spec_save_bottom_btn")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Save Changes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Target product banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Target Product", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(specification.productTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                                Text("ID: ${specification.id.take(12)}", fontSize = 10.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Spec Name & Unit inputs
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = specName,
                                    onValueChange = { specName = it },
                                    label = { Text("Specification Name *") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_spec_name_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = unit,
                                    onValueChange = { unit = it },
                                    label = { Text("Unit (optional)") },
                                    placeholder = { Text("e.g. g, inch, cm, year, mAh, Hz, $") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_spec_unit_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // Quick Unit Pills
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Quick unit:", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                    listOf("g", "kg", "inch", "cm", "mm", "year", "month", "mAh", "Hz", "$", "GB").forEach { qUnit ->
                                        val isSelected = unit.equals(qUnit, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFCBD5E1)),
                                            modifier = Modifier.clickable { unit = if (isSelected) "" else qUnit }
                                        ) {
                                            Text(
                                                text = qUnit,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else Color(0xFF334155),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Numeric Comparison Mode Toggle Card with Switch & Mode Selectors
                    item {
                        val hasNumericCandidate = remember(details) {
                            val clean = htmlToPlainText(details)
                            !clean.contains('\n') && !clean.contains('•') && clean.length <= 45 && Regex("[0-9]").containsMatchIn(clean)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFAFAFD),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF4F46E5),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#",
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Numeric Comparison",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isNumeric == true) Color(0xFFEEF2FF) else Color(0xFFF1F5F9)
                                                ) {
                                                    Text(
                                                        text = if (isNumeric == true) "Number ON" else "Number OFF",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isNumeric == true) Color(0xFF4F46E5) else Color(0xFF64748B),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Enables numerical comparison bar and Best value logic",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isNumeric == true,
                                        onCheckedChange = { isNumeric = if (it) true else false },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF4F46E5),
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Color(0xFFCBD5E1)
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        Triple("Auto", null, "edit_spec_mode_auto"),
                                        Triple("Number (#)", true, "edit_spec_mode_num"),
                                        Triple("Text (Aa)", false, "edit_spec_mode_text")
                                    ).forEach { (label, value, tag) ->
                                        val isSelected = isNumeric == value
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { isNumeric = value }
                                                .testTag(tag)
                                        ) {
                                            Text(
                                                text = label,
                                                modifier = Modifier.padding(vertical = 7.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(0xFF334155)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Details field - Inline WYSIWYG Editor
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Specification details", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text("Supports newlines, bold, italic & links", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }

                                HtmlWysiwygEditor(
                                    value = details,
                                    onChange = { details = it },
                                    placeholder = "e.g. 5000 mAh fast charging with 65W adapter, or multi-line details with bullet points",
                                    minHeight = 110.dp,
                                    testTagPrefix = "edit_spec_details"
                                )

                                if (unit.isNotBlank() && details.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text("Unit Badge:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        SpecUnitBadge(unit = unit, details = details)
                                    }
                                }
                            }
                        }
                    }

                    // Attached Photos
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Specification Pictures (${pictures.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF334155))
                                    OutlinedButton(
                                        onClick = {
                                            specPhotoPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Photos", fontSize = 11.sp)
                                    }
                                }

                                if (pictures.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(pictures) { pic ->
                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFF8FAFC))
                                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AsyncImage(
                                                    model = resolveImageModel(context, pic.url, preferredName = pic.name),
                                                    contentDescription = pic.name,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.fillMaxSize().padding(2.dp)
                                                )
                                                IconButton(
                                                    onClick = { pictures = pictures.filter { it.id != pic.id } },
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .align(Alignment.TopEnd)
                                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
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

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            title = "Delete Specification?",
            message = "Are you sure you want to delete \"${specification.specName}\"? This action cannot be undone.",
            confirmText = "Delete",
            testTagPrefix = "edit_spec_delete_confirm",
            onConfirm = {
                showDeleteConfirm = false
                onDelete(specification.id)
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
fun UnifiedReviewViewer(
    review: ReviewItem,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("unified_review_viewer"),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.testTag("unified_viewer_back_btn")) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Surface(
                                    color = Color(0xFFEEF2FF),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "#${review.reviewNumber}",
                                        color = Color(0xFF4F46E5),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Column(modifier = Modifier.padding(end = 8.dp)) {
                                    Text(
                                        text = review.title.ifBlank { "Review Note #${review.reviewNumber}" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "By ${review.authorName} • Rating: ${review.rating}/5",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.testTag("unified_viewer_delete_btn")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Scrollable Body
                    LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Linked products
                    if (review.productTitles.isNotEmpty()) {
                        item {
                            Text("Linked Products:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF64748B))
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                review.productTitles.forEach { title ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF1F5F9)
                                    ) {
                                        Text(title, fontSize = 11.sp, color = Color(0xFF334155), modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Attached pictures
                    if (review.pictures.isNotEmpty()) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(review.pictures) { pic ->
                                    Box(
                                        modifier = Modifier
                                            .size(140.dp, 100.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = resolveImageModel(LocalContext.current, pic.url, preferredName = pic.name),
                                            contentDescription = pic.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Exact body text verbatim from database
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FormattedSpecHtml(
                                content = review.body,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                }
            }
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            title = "Delete Review",
            message = "Are you sure you want to delete review \"${review.title}\" by ${review.authorName}? This action cannot be undone.",
            confirmText = "Delete",
            testTagPrefix = "unified_viewer_delete",
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
fun UnifiedReviewEditor(
    products: List<ProductItem>,
    preselectedProduct: ProductItem? = null,
    onSave: (title: String, body: String, authorName: String, rating: Int, productIds: List<String>, productTitles: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var authorName by remember { mutableStateOf("Verified Reviewer") }
    var rating by remember { mutableStateOf(5) }
    var selectedProductIds by remember {
        mutableStateOf(if (preselectedProduct != null) setOf(preselectedProduct.id) else emptySet())
    }

    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("unified_review_editor"),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.testTag("unified_editor_back_btn")) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Column {
                                    Text("Write Product Review", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                    Text("Create or edit a product review note", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }

                            Button(
                                onClick = {
                                    val selectedProducts = products.filter { it.id in selectedProductIds }
                                    onSave(
                                        title.trim(),
                                        body.trim(),
                                        authorName.trim(),
                                        rating,
                                        selectedProducts.map { it.id },
                                        selectedProducts.map { it.name }
                                    )
                                    onDismiss()
                                },
                                enabled = body.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                modifier = Modifier.testTag("unified_editor_save_btn")
                            ) {
                                Text("Publish", fontSize = 13.sp)
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Product selection
                    item {
                        Text("Select Linked Product:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(products) { prod ->
                                val isSelected = prod.id in selectedProductIds
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedProductIds = if (isSelected) selectedProductIds - prod.id else selectedProductIds + prod.id
                                    },
                                    label = { Text(prod.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = { Text("Review Body / Benchmark Notes *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            maxLines = 8
                        )
                    }
                }

                }
            }
        }
    }
}

@Composable
fun SyncModal(
    productCount: Int,
    specCount: Int,
    reviewCount: Int,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("sync_modal_screen"),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.testTag("sync_modal_back_btn")) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Column {
                                    Text("Database & Sync Status", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                    Text("Local SQLite / Room Persistence Engine", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF10B981))
                                Text("Storage Engine Status", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            }
                            Text(
                                "Connected to local SQLite / Room persistence with full offline capabilities, transaction isolation, and immediate query caching.",
                                fontSize = 13.sp,
                                color = Color(0xFF475569),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("DATABASE METRICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 1.sp)
                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Database Name:", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text("productcompare.db", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Database Schema:", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text("v2 (Numeric Override Support)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Products:", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text("$productCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Specs:", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text("$specCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Reviews:", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text("$reviewCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Sync State:", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text("Active & Up-to-date", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecificationsOrderModal(
    product: ProductItem,
    specifications: List<SpecificationItem>,
    onSaveOrder: (List<SpecificationItem>) -> Unit,
    onDeleteSpec: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val specsList = remember(product, specifications) {
        specifications
            .filter { it.productId == product.id || it.productTitle == product.name }
            .sortedBy { it.orderIndex }
            .toMutableStateList()
    }
    var specToDeleteName by remember { mutableStateOf<String?>(null) }

    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("spec_order_modal"),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.testTag("spec_order_back_btn")) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }

                                Column {
                                    Text(
                                        "Specifications Order",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        "${product.name} • ${specsList.size} attributes",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val updated = specsList.mapIndexed { idx, spec ->
                                        spec.copy(orderIndex = idx + 1)
                                    }
                                    onSaveOrder(updated)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                modifier = Modifier.testTag("save_spec_order_btn")
                            ) {
                                Text("Save Order", fontSize = 13.sp)
                            }
                        }
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "FLEXIBLE DATA ATTRIBUTES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    "Reorder specifications for \"${product.name}\". Use arrows or sorting tools below.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val sorted = specsList.sortedBy { it.specName.lowercase() }
                                    specsList.clear()
                                    specsList.addAll(sorted)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sort A-Z", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val sorted = specsList.sortedByDescending { it.specName.lowercase() }
                                    specsList.clear()
                                    specsList.addAll(sorted)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sort Z-A", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    items(specsList.size) { idx ->
                        val spec = specsList[idx]
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "${idx + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            spec.specName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        if (spec.details.isNotBlank()) {
                                            FormattedSpecHtml(
                                                content = spec.details,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            if (idx < specsList.size - 1) {
                                                val temp = specsList[idx]
                                                specsList[idx] = specsList[idx + 1]
                                                specsList[idx + 1] = temp
                                            }
                                        },
                                        enabled = idx < specsList.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Move down",
                                            tint = if (idx < specsList.size - 1) Color(0xFF475569) else Color(0xFFCBD5E1),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (idx > 0) {
                                                val temp = specsList[idx]
                                                specsList[idx] = specsList[idx - 1]
                                                specsList[idx - 1] = temp
                                            }
                                        },
                                        enabled = idx > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Move up",
                                            tint = if (idx > 0) Color(0xFF475569) else Color(0xFFCBD5E1),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            specToDeleteName = spec.specName
                                        },
                                        modifier = Modifier.size(28.dp).testTag("order_delete_spec_${idx}_btn")
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete spec",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
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

    if (specToDeleteName != null) {
        val sName = specToDeleteName!!
        DeleteConfirmDialog(
            title = "Delete Specification",
            message = "Are you sure you want to delete specification \"$sName\" from \"${product.name}\"?",
            confirmText = "Delete",
            testTagPrefix = "order_delete_spec",
            onConfirm = {
                onDeleteSpec(sName)
                specsList.removeAll { it.specName == sName }
                specToDeleteName = null
            },
            onDismiss = { specToDeleteName = null }
        )
    }
}
