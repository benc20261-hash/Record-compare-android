package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.SpecificationItem
import com.example.ui.AppNavTab
import com.example.ui.ProductCompareViewModel
import com.example.ui.screens.*
import java.io.File

@Composable
fun ActiveTabContent(
    activeTab: AppNavTab,
    viewModel: ProductCompareViewModel,
    products: List<ProductItem>,
    specifications: List<SpecificationItem>,
    reviews: List<ReviewItem>,
    allTags: List<String>,
    searchQuery: String,
    comparedProductIds: Set<String>,
    isBackupProcessing: Boolean,
    backupProgressText: String?,
    lastExportedZipFile: File?,
    lastSavedPublicLocation: String?,
    context: Context,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        when (activeTab) {
            AppNavTab.CATALOG -> {
                CatalogScreen(
                    products = products,
                    specifications = specifications,
                    reviews = reviews,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    comparedIds = comparedProductIds,
                    onToggleCompare = { viewModel.toggleCompare(it) },
                    onClearComparison = { viewModel.clearComparison() },
                    onProductClick = { viewModel.showProductDetail(it) },
                    onEditProduct = { viewModel.showEditProduct(it) },
                    onDeleteProduct = { viewModel.deleteProduct(it) },
                    onNavigateToSmartAdd = { viewModel.setActiveTab(AppNavTab.SMART_ADD) },
                    onNavigateToAddProduct = { viewModel.setActiveTab(AppNavTab.ADD_PRODUCT) },
                    onNavigateToCompare = { viewModel.setActiveTab(AppNavTab.COMPARE) }
                )
            }
            AppNavTab.COMPARE -> {
                CompareScreen(
                    allProducts = products,
                    specifications = specifications,
                    reviews = reviews,
                    comparedIds = comparedProductIds,
                    onToggleCompare = { viewModel.toggleCompare(it) },
                    onSetComparedIds = { viewModel.setComparedIds(it) },
                    onProductClick = { viewModel.showProductDetail(it) },
                    onEditProduct = { viewModel.showEditProduct(it) },
                    onReviewClick = { viewModel.showReviewDetail(it) },
                    onOpenAddReview = { viewModel.openAddReviewDialog(it) }
                )
            }
            AppNavTab.SMART_ADD -> {
                SmartAddProductScreen(
                    products = products,
                    specifications = specifications,
                    comparedIds = comparedProductIds,
                    onNavigateToCompare = { viewModel.setActiveTab(AppNavTab.COMPARE) },
                    onNavigateToCatalog = { viewModel.setActiveTab(AppNavTab.CATALOG) },
                    onEditProduct = { viewModel.showEditProduct(it) },
                    onDeleteProduct = { viewModel.deleteProduct(it) },
                    onCreateAndCompare = { name, brand, inheritSpecs, explicitId, onCreated ->
                        viewModel.createAndCompare(name, brand, inheritSpecs, explicitId, onCreated)
                    },
                    onAddSpecificationInline = { prodId, title, name, details, unit ->
                        viewModel.addSpecification(prodId, title, name, details, unit)
                    },
                    onUpdateInlineSpec = { prodId, title, name, details, pics, unit ->
                        viewModel.updateInlineSpec(prodId, title, name, details, pics, unit)
                    },
                    onDeleteSpecInline = { prodId, name ->
                        viewModel.deleteSpecInline(prodId, name)
                    },
                    onUpdateProductBrand = { prodId, brand ->
                        viewModel.updateProductBrand(prodId, brand)
                    },
                    onUpdateProductTags = { prodId, tags ->
                        viewModel.updateProductTags(prodId, tags)
                    }
                )
            }
            AppNavTab.ADD_PRODUCT -> {
                ProductWorkspaceScreen(
                    initialProduct = null,
                    allAvailableTags = allTags,
                    initialSpecifications = emptyList(),
                    onSaveProduct = { product, specs, isNew ->
                        viewModel.saveProductWithSpecifications(product, specs, isNew)
                        viewModel.setActiveTab(AppNavTab.CATALOG)
                    },
                    onDeleteProduct = null,
                    onNavigateBack = { viewModel.setActiveTab(AppNavTab.CATALOG) }
                )
            }
            AppNavTab.REVIEWS -> {
                ReviewsScreen(
                    reviews = reviews,
                    products = products,
                    onReviewClick = { viewModel.showReviewDetail(it) },
                    onEditReview = { rev -> viewModel.openEditReviewDialog(rev) },
                    onWriteReview = { viewModel.openAddReviewDialog(null) },
                    onDeleteReview = { viewModel.deleteReview(it) },
                    onProductClick = { viewModel.showProductDetail(it) },
                    onImportFolder = { uri -> viewModel.startFolderTreeImport(context, uri) },
                    onImportZip = { uri -> viewModel.startZipImport(context, uri) },
                    onImportHtml = { uri -> viewModel.startSingleHtmlImport(context, uri) }
                )
            }
            AppNavTab.DATA_BACKUP -> {
                DataBackupScreen(
                    products = products,
                    specifications = specifications,
                    reviews = reviews,
                    isProcessing = isBackupProcessing,
                    progressText = backupProgressText,
                    lastExportedZip = lastExportedZipFile,
                    lastSavedPublicLocation = lastSavedPublicLocation,
                    onCreateFullZipToDownloads = { viewModel.createFullZipToDownloads(context) },
                    onExportZipToUri = { uri -> viewModel.exportZipToUri(context, uri) },
                    onExportJsonToUri = { uri -> viewModel.exportJsonToUri(context, uri) },
                    onRestoreFromUri = { uri -> viewModel.restoreFromUri(context, uri) },
                    onResetToSeedData = { viewModel.resetToSeedData() },
                    onClearAllData = { viewModel.clearAllCatalogData() },
                    onFreeUpStorage = { viewModel.freeUpOrphanStorage() },
                    onImportFolder = { uri -> viewModel.startFolderTreeImport(context, uri) },
                    onImportZip = { uri -> viewModel.startZipImport(context, uri) },
                    onImportHtml = { uri -> viewModel.startSingleHtmlImport(context, uri) }
                )
            }
        }
    }
}
