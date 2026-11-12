package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.SpecificationItem
import com.example.ui.AppNavTab
import com.example.ui.ProductCompareViewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ProductCompareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            // Ensure Chromium WebView cache directories exist so Chromium simple_file_enumerator doesn't fail with opendir errors
            val webViewCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            java.io.File(webViewCacheDir, "js").mkdirs()
            java.io.File(webViewCacheDir, "wasm").mkdirs()
        } catch (_: Exception) {}
        handleIncomingShareIntent(intent)
        setContent {
            MyApplicationTheme {
                ProductCompareApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val text = intent.getStringExtra(Intent.EXTRA_HTML_TEXT) ?: intent.getStringExtra(Intent.EXTRA_TEXT)
            val streamUris = mutableListOf<Uri>()

            if (action == Intent.ACTION_SEND) {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { streamUris.add(it) }
            } else if (action == Intent.ACTION_SEND_MULTIPLE) {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { streamUris.addAll(it) }
            }

            intent.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    val uri = clip.getItemAt(i).uri
                    if (uri != null && uri !in streamUris) {
                        streamUris.add(uri)
                    }
                }
            }

            if (!text.isNullOrBlank() || streamUris.isNotEmpty()) {
                viewModel.handleShareIntent(this, subject, text, streamUris)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCompareApp(viewModel: ProductCompareViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val selectedBrand by viewModel.selectedBrand.collectAsStateWithLifecycle()
    val comparedProductIds by viewModel.comparedProductIds.collectAsStateWithLifecycle()

    val products by viewModel.products.collectAsStateWithLifecycle()
    val specifications by viewModel.specifications.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()

    val selectedProductForDetail by viewModel.selectedProductForDetail.collectAsStateWithLifecycle()
    val selectedProductForEdit by viewModel.selectedProductForEdit.collectAsStateWithLifecycle()
    val selectedReviewForView by viewModel.selectedReviewForView.collectAsStateWithLifecycle()

    val isAddProductDialogOpen by viewModel.isAddProductDialogOpen.collectAsStateWithLifecycle()
    val isAddSpecDialogOpen by viewModel.isAddSpecDialogOpen.collectAsStateWithLifecycle()
    val isAddReviewDialogOpen by viewModel.isAddReviewDialogOpen.collectAsStateWithLifecycle()
    val addReviewDialogState by viewModel.addReviewDialogState.collectAsStateWithLifecycle()
    val selectedReviewForEdit by viewModel.selectedReviewForEdit.collectAsStateWithLifecycle()

    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val isBackupProcessing by viewModel.isBackupProcessing.collectAsStateWithLifecycle()
    val backupProgressText by viewModel.backupProgressText.collectAsStateWithLifecycle()
    val lastExportedZipFile by viewModel.lastExportedZipFile.collectAsStateWithLifecycle()
    val lastSavedPublicLocation by viewModel.lastSavedPublicLocation.collectAsStateWithLifecycle()

    val pendingImportDrafts by viewModel.pendingImportDrafts.collectAsStateWithLifecycle()
    val isImportProcessing by viewModel.isImportProcessing.collectAsStateWithLifecycle()
    val importProgressText by viewModel.importProgressText.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    var showSyncModal by remember { mutableStateOf(false) }
    var appFontScale by remember { mutableFloatStateOf(1.0f) }

    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * appFontScale
        )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isDesktopMode = maxWidth >= 720.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (isDesktopMode) {
                    AppNavigationRail(
                        activeTab = activeTab,
                        onTabSelected = { viewModel.setActiveTab(it) },
                        productCount = products.size,
                        compareCount = comparedProductIds.size,
                        reviewCount = reviews.size,
                        appFontScale = appFontScale,
                        onFontScaleChange = { appFontScale = it },
                        onSyncClick = { showSyncModal = true }
                    )
                    VerticalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                }

                Scaffold(
                    modifier = Modifier
                        .then(if (isDesktopMode) Modifier.weight(1f) else Modifier.fillMaxWidth())
                        .fillMaxHeight(),
                    containerColor = com.example.ui.theme.AppColors.CanvasBackground,
                    topBar = {
                        if (!isDesktopMode) {
                            Column(
                                modifier = Modifier
                                    .background(Color.White)
                                    .statusBarsPadding()
                            ) {
                        // Top App Bar Header with Logo & Live Badge (Catalog module.PNG / Compare module.PNG)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF4F46E5),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Scale,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "ProductCompare",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF)
                        ) {
                            Text(
                                text = "Live",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Font size controls right beside "Live" (A- / % / A+)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp),
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val next = ((appFontScale - 0.1f) * 10).roundToInt() / 10f
                                        appFontScale = next.coerceAtLeast(0.7f)
                                    },
                                    modifier = Modifier.size(24.dp).testTag("btn_font_decrease"),
                                    enabled = appFontScale > 0.75f
                                ) {
                                    Text(
                                        "A-",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (appFontScale > 0.75f) Color(0xFF334155) else Color(0xFF94A3B8)
                                    )
                                }
                                Text(
                                    text = "${(appFontScale * 100).roundToInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier
                                        .clickable { appFontScale = 1.0f }
                                        .padding(horizontal = 2.dp)
                                )
                                IconButton(
                                    onClick = {
                                        val next = ((appFontScale + 0.1f) * 10).roundToInt() / 10f
                                        appFontScale = next.coerceAtMost(1.6f)
                                    },
                                    modifier = Modifier.size(24.dp).testTag("btn_font_increase"),
                                    enabled = appFontScale < 1.55f
                                ) {
                                    Text(
                                        "A+",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (appFontScale < 1.55f) Color(0xFF334155) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    // Top Action Icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {


                        IconButton(
                            onClick = { showSyncModal = true },
                            modifier = Modifier.size(36.dp).testTag("nav_sync_btn")
                        ) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = "Database Status",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Module Navigation Pills Row (Matching Catalog module.PNG / Compare module.PNG / Smart add product.PNG)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Catalog Tab Pill
                    val isCatalogActive = activeTab == AppNavTab.CATALOG
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isCatalogActive) Color(0xFF4F46E5) else Color.White,
                        border = if (!isCatalogActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
                        modifier = Modifier
                            .clickable { viewModel.setActiveTab(AppNavTab.CATALOG) }
                            .testTag("top_nav_catalog")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = null,
                                tint = if (isCatalogActive) Color.White else Color(0xFF475569),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                "Catalog",
                                fontSize = 13.sp,
                                fontWeight = if (isCatalogActive) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isCatalogActive) Color.White else Color(0xFF334155)
                            )
                        }
                    }

                    // 2. Compare Tab Pill
                    val isCompareActive = activeTab == AppNavTab.COMPARE
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isCompareActive) Color(0xFF4F46E5) else Color.White,
                        border = if (!isCompareActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
                        modifier = Modifier
                            .clickable { viewModel.setActiveTab(AppNavTab.COMPARE) }
                            .testTag("top_nav_compare")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Scale,
                                contentDescription = null,
                                tint = if (isCompareActive) Color.White else Color(0xFF475569),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                "Compare",
                                fontSize = 13.sp,
                                fontWeight = if (isCompareActive) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isCompareActive) Color.White else Color(0xFF334155)
                            )
                        }
                    }

                    // 3. Smart Add Product Tab Pill
                    val isSmartAddActive = activeTab == AppNavTab.SMART_ADD
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSmartAddActive) Color(0xFF0F172A) else Color.White,
                        border = if (!isSmartAddActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
                        modifier = Modifier
                            .clickable { viewModel.setActiveTab(AppNavTab.SMART_ADD) }
                            .testTag("top_nav_smart_add")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isSmartAddActive) Color(0xFFFBBF24) else Color(0xFFD97706),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                "Smart Add Product",
                                fontSize = 13.sp,
                                fontWeight = if (isSmartAddActive) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSmartAddActive) Color.White else Color(0xFF334155)
                            )
                        }
                    }

                    // 4. Add Product Tab Pill
                    val isAddProductActive = activeTab == AppNavTab.ADD_PRODUCT
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isAddProductActive) Color(0xFF4F46E5) else Color.White,
                        border = if (!isAddProductActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
                        modifier = Modifier
                            .clickable { viewModel.setActiveTab(AppNavTab.ADD_PRODUCT) }
                            .testTag("top_nav_add_product")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = if (isAddProductActive) Color.White else Color(0xFF475569),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                "Add Product",
                                fontSize = 13.sp,
                                fontWeight = if (isAddProductActive) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isAddProductActive) Color.White else Color(0xFF334155)
                            )
                        }
                    }

                    // 5. Reviews Tab Pill
                    val isReviewsActive = activeTab == AppNavTab.REVIEWS
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isReviewsActive) Color(0xFF4F46E5) else Color.White,
                        border = if (!isReviewsActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
                        modifier = Modifier
                            .clickable { viewModel.setActiveTab(AppNavTab.REVIEWS) }
                            .testTag("top_nav_reviews")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.RateReview,
                                contentDescription = null,
                                tint = if (isReviewsActive) Color.White else Color(0xFF475569),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                "Reviews (${reviews.size})",
                                fontSize = 13.sp,
                                fontWeight = if (isReviewsActive) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isReviewsActive) Color.White else Color(0xFF334155)
                            )
                        }
                    }

                    // 6. Data Backup Tab Pill
                    val isBackupActive = activeTab == AppNavTab.DATA_BACKUP
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isBackupActive) Color(0xFF0F172A) else Color.White,
                        border = if (!isBackupActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
                        modifier = Modifier
                            .clickable { viewModel.setActiveTab(AppNavTab.DATA_BACKUP) }
                            .testTag("top_nav_backup")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                tint = if (isBackupActive) Color(0xFF38BDF8) else Color(0xFF475569),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                "Data Backup",
                                fontSize = 13.sp,
                                fontWeight = if (isBackupActive) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isBackupActive) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    }
                }
            },
            bottomBar = {
                if (!isDesktopMode) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        NavBarItem(
                            label = "Catalog",
                            icon = Icons.Default.GridView,
                            selected = activeTab == AppNavTab.CATALOG,
                            badgeCount = products.size,
                            onClick = { viewModel.setActiveTab(AppNavTab.CATALOG) },
                            testTag = "nav_tab_catalog"
                        )
                        NavBarItem(
                            label = "Compare",
                            icon = Icons.Default.CompareArrows,
                            selected = activeTab == AppNavTab.COMPARE,
                            badgeCount = if (comparedProductIds.isNotEmpty()) comparedProductIds.size else null,
                            onClick = { viewModel.setActiveTab(AppNavTab.COMPARE) },
                            testTag = "nav_tab_compare"
                        )
                        NavBarItem(
                            label = "Smart Add",
                            icon = Icons.Default.AutoAwesome,
                            selected = activeTab == AppNavTab.SMART_ADD,
                            onClick = { viewModel.setActiveTab(AppNavTab.SMART_ADD) },
                            testTag = "nav_tab_smart_add"
                        )
                        NavBarItem(
                            label = "Reviews",
                            icon = Icons.Default.RateReview,
                            selected = activeTab == AppNavTab.REVIEWS,
                            badgeCount = if (reviews.isNotEmpty()) reviews.size else null,
                            onClick = { viewModel.setActiveTab(AppNavTab.REVIEWS) },
                            testTag = "nav_tab_reviews"
                        )
                        NavBarItem(
                            label = "Backup",
                            icon = Icons.Default.Storage,
                            selected = activeTab == AppNavTab.DATA_BACKUP,
                            onClick = { viewModel.setActiveTab(AppNavTab.DATA_BACKUP) },
                            testTag = "nav_tab_backup"
                        )
                    }
                }
            }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(com.example.ui.theme.AppColors.CanvasBackground),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1200.dp)
                    .windowInsetsPadding(
                        if (isDesktopMode) WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                        else WindowInsets(0, 0, 0, 0)
                    )
            ) {
                ActiveTabContent(
                    activeTab = activeTab,
                    viewModel = viewModel,
                    products = products,
                    specifications = specifications,
                    reviews = reviews,
                    allTags = allTags,
                    searchQuery = searchQuery,
                    comparedProductIds = comparedProductIds,
                    isBackupProcessing = isBackupProcessing,
                    backupProgressText = backupProgressText,
                    lastExportedZipFile = lastExportedZipFile,
                    lastSavedPublicLocation = lastSavedPublicLocation,
                    context = context
                )

                // Status Banner Overlay
                if (bannerMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = bannerMessage!!,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

    // Modals
    if (!isDesktopMode || activeTab != AppNavTab.CATALOG) {
        selectedProductForDetail?.let { prod ->
        val prodSpecs = specifications.filter { it.productId == prod.id || it.productTitle == prod.name }
        val prodReviews = reviews.filter { prod.id in it.productIds || prod.name in it.productTitles }
        val isCompared = prod.id in comparedProductIds

        ProductDetailModal(
            product = prod,
            specifications = prodSpecs,
            reviews = prodReviews,
            isCompared = isCompared,
            onToggleCompare = { viewModel.toggleCompare(prod.id) },
            onEdit = {
                viewModel.showProductDetail(null)
                viewModel.showEditProduct(prod)
            },
            onDelete = {
                viewModel.showProductDetail(null)
                viewModel.deleteProduct(prod.id)
            },
            onAddSpec = { viewModel.openAddSpecDialog(prod) },
            onAddReview = {
                viewModel.showProductDetail(null)
                viewModel.openAddReviewDialog(prod)
            },
            onReviewClick = {
                viewModel.showProductDetail(null)
                viewModel.showReviewDetail(it)
            },
            onEditReview = {
                viewModel.showProductDetail(null)
                viewModel.openEditReviewDialog(it)
            },
            onDeleteSpec = { spec -> viewModel.deleteSpecification(spec.id) },
            onSaveSpec = { spec -> viewModel.saveSpecification(spec) },
            onDismiss = { viewModel.showProductDetail(null) }
        )
    }
    }

    selectedProductForEdit?.let { prod ->
        val prodSpecs = specifications.filter { it.productId == prod.id || it.productTitle == prod.name }
        ProductWorkspaceScreen(
            initialProduct = prod,
            allAvailableTags = allTags,
            initialSpecifications = prodSpecs,
            onSaveProduct = { product, specs, isNew ->
                viewModel.saveProductWithSpecifications(product, specs, isNew)
                viewModel.showEditProduct(null)
                viewModel.showProductDetail(product)
            },
            onDeleteProduct = { id ->
                viewModel.deleteProduct(id)
                viewModel.showEditProduct(null)
            },
            onNavigateBack = {
                viewModel.showEditProduct(null)
                viewModel.showProductDetail(prod)
            }
        )
    }

    if (isAddProductDialogOpen) {
        ProductWorkspaceScreen(
            initialProduct = null,
            allAvailableTags = allTags,
            initialSpecifications = emptyList(),
            onSaveProduct = { product, specs, isNew ->
                viewModel.saveProductWithSpecifications(product, specs, isNew)
                viewModel.openAddProductDialog(false)
            },
            onDeleteProduct = null,
            onNavigateBack = { viewModel.openAddProductDialog(false) }
        )
    }

    isAddSpecDialogOpen?.let { prod ->
        AddSpecModal(
            product = prod,
            onAdd = { prodId, title, specName, details, unit, isNumeric ->
                viewModel.addSpecification(prodId, title, specName, details, unit, isNumeric)
            },
            onDismiss = { viewModel.openAddSpecDialog(null) }
        )
    }

    selectedReviewForView?.let { rev ->
        RichReviewViewer(
            review = rev,
            products = products,
            onClose = { viewModel.showReviewDetail(null) },
            onEditReview = { r ->
                viewModel.showReviewDetail(null)
                viewModel.openEditReviewDialog(r)
            },
            onDeleteReview = { id -> viewModel.deleteReview(id) },
            onViewProduct = { prod ->
                viewModel.showReviewDetail(null)
                viewModel.showProductDetail(prod)
            }
        )
    }

    selectedReviewForEdit?.let { rev ->
        RichReviewEditor(
            products = products,
            initialReview = rev,
            preselectedProduct = products.find { it.id in rev.productIds } ?: products.firstOrNull(),
            onSaveReview = { updated ->
                viewModel.saveReview(updated)
                viewModel.openEditReviewDialog(null)
            },
            onDismiss = { viewModel.openEditReviewDialog(null) }
        )
    }

    addReviewDialogState?.let { state ->
        RichReviewEditor(
            products = products,
            initialReview = null,
            preselectedProduct = state.preselectedProduct ?: products.firstOrNull(),
            onSaveReview = { newRev ->
                viewModel.saveReview(newRev)
                viewModel.closeAddReviewDialog()
            },
            onDismiss = { viewModel.closeAddReviewDialog() }
        )
    }

    if (showSyncModal) {
        SyncModal(
            productCount = products.size,
            specCount = specifications.size,
            reviewCount = reviews.size,
            onDismiss = { showSyncModal = false }
        )
    }

    pendingImportDrafts?.let { drafts ->
        HtmlReviewImportDialog(
            drafts = drafts,
            products = products,
            onConfirmImport = { confirmed ->
                viewModel.confirmAndSaveImportedReviews(confirmed)
            },
            onDismiss = { viewModel.dismissImportDialog() }
        )
    }

    if (isImportProcessing) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF4F46E5), modifier = Modifier.size(32.dp))
                    Text(
                        text = importProgressText ?: "Processing...",
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    }
}
}

@Composable
private fun RowScope.NavBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int? = null,
    onClick: () -> Unit,
    testTag: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.testTag(testTag),
        icon = {
            BadgedBox(
                badge = {
                    if (badgeCount != null && badgeCount > 0) {
                        Badge(containerColor = Color(0xFF4F46E5)) {
                            Text(badgeCount.toString(), fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            ) {
                Icon(icon, contentDescription = label)
            }
        },
        label = {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF4F46E5),
            selectedTextColor = Color(0xFF4F46E5),
            unselectedIconColor = Color(0xFF64748B),
            unselectedTextColor = Color(0xFF64748B),
            indicatorColor = Color(0xFFEEF2FF)
        )
    )
}
