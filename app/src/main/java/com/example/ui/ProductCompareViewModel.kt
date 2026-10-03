package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ProductEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.SpecificationEntity
import com.example.data.repository.ProductCompareRepository
import com.example.model.PictureItem
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.SpecificationItem
import com.example.model.parsePicturesJson
import com.example.model.parseStringArrayJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import android.content.Context
import android.net.Uri
import com.example.data.backup.BackupArchiveManager
import com.example.data.backup.FileSaveResult
import com.example.data.backup.RestoreResult
import com.example.data.importer.ImportedReviewDraft
import com.example.data.importer.UpNoteHtmlImportManager
import com.example.data.local.UserEntity
import java.io.File
import java.util.UUID

enum class AppNavTab {
    CATALOG,
    COMPARE,
    SMART_ADD,
    ADD_PRODUCT,
    REVIEWS,
    DATA_BACKUP
}

class ProductCompareViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ProductCompareRepository(application, database.appDao())

    // Active Navigation Tab
    private val _activeTab = MutableStateFlow(AppNavTab.CATALOG)
    val activeTab: StateFlow<AppNavTab> = _activeTab.asStateFlow()

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Tag and Brand Filters
    private val _selectedTag = MutableStateFlow("All")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    private val _selectedBrand = MutableStateFlow("All")
    val selectedBrand: StateFlow<String> = _selectedBrand.asStateFlow()

    // Compared Product IDs
    private val _comparedProductIds = MutableStateFlow<Set<String>>(emptySet())
    val comparedProductIds: StateFlow<Set<String>> = _comparedProductIds.asStateFlow()

    // Selected items for Dialogs
    private val _selectedProductForDetail = MutableStateFlow<ProductItem?>(null)
    val selectedProductForDetail: StateFlow<ProductItem?> = _selectedProductForDetail.asStateFlow()

    private val _selectedProductForEdit = MutableStateFlow<ProductItem?>(null)
    val selectedProductForEdit: StateFlow<ProductItem?> = _selectedProductForEdit.asStateFlow()

    private val _selectedReviewForView = MutableStateFlow<ReviewItem?>(null)
    val selectedReviewForView: StateFlow<ReviewItem?> = _selectedReviewForView.asStateFlow()

    private val _isAddProductDialogOpen = MutableStateFlow(false)
    val isAddProductDialogOpen: StateFlow<Boolean> = _isAddProductDialogOpen.asStateFlow()

    private val _isAddSpecDialogOpen = MutableStateFlow<ProductItem?>(null)
    val isAddSpecDialogOpen: StateFlow<ProductItem?> = _isAddSpecDialogOpen.asStateFlow()

    private val _isAddReviewDialogOpen = MutableStateFlow<ProductItem?>(null)
    val isAddReviewDialogOpen: StateFlow<ProductItem?> = _isAddReviewDialogOpen.asStateFlow()

    private val _selectedReviewForEdit = MutableStateFlow<ReviewItem?>(null)
    val selectedReviewForEdit: StateFlow<ReviewItem?> = _selectedReviewForEdit.asStateFlow()

    // UpNote HTML & Folder Import state
    private val _pendingImportDrafts = MutableStateFlow<List<ImportedReviewDraft>?>(null)
    val pendingImportDrafts: StateFlow<List<ImportedReviewDraft>?> = _pendingImportDrafts.asStateFlow()

    private val _isImportProcessing = MutableStateFlow(false)
    val isImportProcessing: StateFlow<Boolean> = _isImportProcessing.asStateFlow()

    private val _importProgressText = MutableStateFlow<String?>(null)
    val importProgressText: StateFlow<String?> = _importProgressText.asStateFlow()

    // Status Banner message
    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    // Products mapped from Room
    val products: StateFlow<List<ProductItem>> = repository.productsFlow.map { entities ->
        entities.map { entity ->
            val tagsList = if (entity.tags.isBlank()) emptyList() else entity.tags.split(",").map { it.trim() }
            ProductItem(
                id = entity.id,
                name = entity.name,
                brand = entity.brand,
                tags = tagsList,
                overview = entity.overview,
                pictures = parsePicturesJson(entity.picturesJson),
                createdTime = entity.createdTime
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Specifications mapped from Room
    val specifications: StateFlow<List<SpecificationItem>> = repository.specificationsFlow.map { entities ->
        entities.map { entity ->
            SpecificationItem(
                id = entity.id,
                productId = entity.productId,
                productTitle = entity.productTitle,
                specName = entity.specName,
                details = entity.details,
                unit = entity.unit,
                orderIndex = entity.orderIndex,
                pictures = parsePicturesJson(entity.picturesJson)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reviews mapped from Room
    val reviews: StateFlow<List<ReviewItem>> = repository.reviewsFlow.map { entities ->
        entities.map { entity ->
            ReviewItem(
                id = entity.id,
                reviewNumber = entity.reviewNumber,
                title = entity.title,
                body = entity.body,
                authorName = entity.authorName,
                rating = entity.rating,
                productIds = parseStringArrayJson(entity.productIdsJson),
                productTitles = parseStringArrayJson(entity.productTitlesJson),
                pictures = parsePicturesJson(entity.picturesJson),
                createdTime = entity.createdTime
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Users mapped from Room
    val users: StateFlow<List<UserEntity>> = repository.usersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Tags across catalog with preset tags
    val allTags: StateFlow<List<String>> = products.map { prods ->
        val set = linkedSetOf(
            "Adidas", "Ai voice recorder", "Audio", "Boya", "dress", "Home appliance",
            "IT product", "Kid", "Plaud", "Puma", "Shoe", "Smart wear", "Smartphone", "Sony"
        )
        prods.flatMap { it.tags }.forEach { if (it.isNotBlank()) set.add(it) }
        set.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Backup & Archive State
    private val _isBackupProcessing = MutableStateFlow(false)
    val isBackupProcessing: StateFlow<Boolean> = _isBackupProcessing.asStateFlow()

    private val _backupProgressText = MutableStateFlow<String?>(null)
    val backupProgressText: StateFlow<String?> = _backupProgressText.asStateFlow()

    private val _lastExportedZipFile = MutableStateFlow<File?>(null)
    val lastExportedZipFile: StateFlow<File?> = _lastExportedZipFile.asStateFlow()

    private val _lastSavedPublicLocation = MutableStateFlow<String?>(null)
    val lastSavedPublicLocation: StateFlow<String?> = _lastSavedPublicLocation.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun setActiveTab(tab: AppNavTab) {
        _activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTag(tag: String) {
        _selectedTag.value = tag
    }

    fun setSelectedBrand(brand: String) {
        _selectedBrand.value = brand
    }

    fun setComparedIds(ids: Collection<String>) {
        _comparedProductIds.value = ids.filter { it.isNotBlank() }.toSet()
    }

    fun toggleCompare(productId: String) {
        val current = _comparedProductIds.value
        _comparedProductIds.value = if (productId in current) {
            current - productId
        } else {
            current + productId
        }
    }

    fun addCompareSlot(productId: String) {
        _comparedProductIds.value = _comparedProductIds.value + productId
    }

    fun removeCompareSlot(productId: String) {
        _comparedProductIds.value = _comparedProductIds.value - productId
    }

    fun replaceCompareSlot(oldProductId: String, newProductId: String) {
        val currentList = _comparedProductIds.value.toMutableList()
        val idx = currentList.indexOf(oldProductId)
        if (idx != -1) {
            currentList[idx] = newProductId
        } else {
            currentList.add(newProductId)
        }
        _comparedProductIds.value = currentList.toSet()
    }

    fun clearComparison() {
        _comparedProductIds.value = emptySet()
    }

    fun showProductDetail(product: ProductItem?) {
        _selectedProductForDetail.value = product
    }

    fun showEditProduct(product: ProductItem?) {
        _selectedProductForEdit.value = product
    }

    fun showReviewDetail(review: ReviewItem?) {
        _selectedReviewForView.value = review
    }

    fun openAddProductDialog(open: Boolean) {
        _isAddProductDialogOpen.value = open
    }

    fun openAddSpecDialog(product: ProductItem?) {
        _isAddSpecDialogOpen.value = product
    }

    fun openAddReviewDialog(product: ProductItem?) {
        _isAddReviewDialogOpen.value = product
    }

    fun openEditReviewDialog(review: ReviewItem?) {
        _selectedReviewForEdit.value = review
    }

    fun showBanner(message: String) {
        _bannerMessage.value = message
        viewModelScope.launch {
            kotlinx.coroutines.delay(4000)
            if (_bannerMessage.value == message) {
                _bannerMessage.value = null
            }
        }
    }

    // CRUD
    fun addProduct(name: String, brand: String, tags: List<String>, overview: String, pictures: List<PictureItem>) {
        viewModelScope.launch {
            val id = "rec_" + UUID.randomUUID().toString().replace("-", "").take(17)
            val picsArr = JSONArray()
            pictures.forEach { p ->
                val obj = org.json.JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("url", p.url)
                picsArr.put(obj)
            }
            val entity = ProductEntity(
                id = id,
                name = name,
                brand = brand,
                tags = tags.joinToString(","),
                overview = overview,
                picturesJson = picsArr.toString(),
                createdTime = System.currentTimeMillis().toString()
            )
            repository.insertProduct(entity)
            showBanner("Product \"$name\" created successfully!")
        }
    }

    fun updateProduct(product: ProductItem) {
        viewModelScope.launch {
            val picsArr = JSONArray()
            product.pictures.forEach { p ->
                val obj = org.json.JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("url", p.url)
                picsArr.put(obj)
            }
            val entity = ProductEntity(
                id = product.id,
                name = product.name,
                brand = product.brand,
                tags = product.tags.joinToString(","),
                overview = product.overview,
                picturesJson = picsArr.toString(),
                createdTime = product.createdTime
            )
            repository.insertProduct(entity)
            showBanner("Product updated!")
        }
    }

    fun saveProductWithSpecifications(
        product: ProductItem,
        specs: List<SpecificationItem>,
        isNew: Boolean
    ) {
        viewModelScope.launch {
            val picsArr = JSONArray()
            product.pictures.forEach { p ->
                val obj = org.json.JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("url", p.url)
                picsArr.put(obj)
            }
            val entity = ProductEntity(
                id = product.id,
                name = product.name,
                brand = product.brand,
                tags = product.tags.joinToString(","),
                overview = product.overview,
                picturesJson = picsArr.toString(),
                createdTime = if (isNew) System.currentTimeMillis().toString() else product.createdTime
            )
            repository.insertProduct(entity)

            // Remove specs that were removed in the workspace
            val existingSpecs = specifications.value.filter { it.productId == product.id }
            existingSpecs.forEach { existing ->
                if (specs.none { it.id == existing.id }) {
                    repository.deleteSpecification(existing.id)
                }
            }

            // Upsert all current specifications with their exact order
            specs.forEachIndexed { index, s ->
                val specPicsArr = JSONArray()
                s.pictures.forEach { sp ->
                    val obj = org.json.JSONObject()
                    obj.put("id", sp.id)
                    obj.put("name", sp.name)
                    obj.put("url", sp.url)
                    specPicsArr.put(obj)
                }
                val specEntity = SpecificationEntity(
                    id = if (s.id.isBlank()) "spec_" + UUID.randomUUID().toString().replace("-", "").take(17) else s.id,
                    productId = product.id,
                    productTitle = product.name,
                    specName = s.specName.ifBlank { "Spec" },
                    details = s.details,
                    unit = s.unit?.ifBlank { null },
                    orderIndex = index + 1,
                    picturesJson = specPicsArr.toString()
                )
                repository.insertSpecification(specEntity)
            }

            showBanner(if (isNew) "Product \"${product.name}\" created with ${specs.size} specifications!" else "Product \"${product.name}\" changes saved!")
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            _comparedProductIds.value = _comparedProductIds.value - productId
            if (_selectedProductForDetail.value?.id == productId) {
                _selectedProductForDetail.value = null
            }
            showBanner("Product deleted.")
        }
    }

    fun addSpecification(productId: String, productTitle: String, specName: String, details: String, unit: String?) {
        viewModelScope.launch {
            val id = "spec_" + UUID.randomUUID().toString().replace("-", "").take(17)
            val entity = SpecificationEntity(
                id = id,
                productId = productId,
                productTitle = productTitle,
                specName = specName,
                details = details,
                unit = unit?.ifBlank { null },
                orderIndex = (specifications.value.maxOfOrNull { it.orderIndex } ?: 0) + 1,
                picturesJson = "[]"
            )
            repository.insertSpecification(entity)
            showBanner("Specification \"$specName\" added!")
        }
    }

    fun createAndCompare(
        name: String,
        brand: String = "Generic",
        inheritSpecNames: List<String> = emptyList(),
        explicitId: String? = null,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val newId = explicitId ?: ("rec_" + UUID.randomUUID().toString().replace("-", "").take(17))
            val entity = ProductEntity(
                id = newId,
                name = name.trim(),
                brand = brand.trim(),
                tags = "Smart wear,IT product",
                overview = "Created from compare workspace matrix.",
                picturesJson = "[]",
                createdTime = System.currentTimeMillis().toString()
            )
            repository.insertProduct(entity)

            inheritSpecNames.distinct().forEachIndexed { index, specName ->
                val specEntity = SpecificationEntity(
                    id = "spec_" + UUID.randomUUID().toString().replace("-", "").take(17),
                    productId = newId,
                    productTitle = name.trim(),
                    specName = specName,
                    details = "",
                    unit = null,
                    orderIndex = index + 1,
                    picturesJson = "[]"
                )
                repository.insertSpecification(specEntity)
            }

            // Limit comparison to at most 5 records
            val updated = (_comparedProductIds.value + newId).toList().takeLast(5).toSet()
            _comparedProductIds.value = updated
            onCreated(newId)
            showBanner("Product \"$name\" created with ${inheritSpecNames.size} specifications and added to compare!")
        }
    }

    fun updateInlineSpec(
        productId: String,
        productTitle: String,
        specName: String,
        details: String,
        pictures: List<PictureItem> = emptyList(),
        unit: String? = null
    ) {
        viewModelScope.launch {
            val existing = specifications.value.find {
                (it.productId == productId || it.productTitle == productTitle) &&
                        it.specName.equals(specName, ignoreCase = true)
            }

            val picsArr = JSONArray()
            pictures.forEach { p ->
                val obj = org.json.JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("url", p.url)
                picsArr.put(obj)
            }

            val entity = SpecificationEntity(
                id = existing?.id ?: ("spec_" + UUID.randomUUID().toString().replace("-", "").take(17)),
                productId = productId,
                productTitle = productTitle,
                specName = specName,
                details = details,
                unit = unit ?: existing?.unit,
                orderIndex = existing?.orderIndex ?: ((specifications.value.maxOfOrNull { it.orderIndex } ?: 0) + 1),
                picturesJson = picsArr.toString()
            )
            repository.insertSpecification(entity)
            showBanner("Saved \"$specName\" for $productTitle")
        }
    }

    fun deleteSpecInline(productId: String, specName: String) {
        viewModelScope.launch {
            val existing = specifications.value.find {
                it.productId == productId && it.specName.equals(specName, ignoreCase = true)
            }
            if (existing != null) {
                repository.deleteSpecification(existing.id)
                showBanner("Specification \"$specName\" removed.")
            }
        }
    }

    fun updateProductBrand(productId: String, brand: String) {
        viewModelScope.launch {
            val existing = products.value.find { it.id == productId }
            if (existing != null) {
                val picsArr = JSONArray()
                existing.pictures.forEach { p ->
                    val obj = org.json.JSONObject()
                    obj.put("id", p.id)
                    obj.put("name", p.name)
                    obj.put("url", p.url)
                    picsArr.put(obj)
                }
                val entity = ProductEntity(
                    id = existing.id,
                    name = existing.name,
                    brand = brand.trim(),
                    tags = existing.tags.joinToString(","),
                    overview = existing.overview,
                    picturesJson = picsArr.toString(),
                    createdTime = existing.createdTime
                )
                repository.insertProduct(entity)
                showBanner("Brand updated to \"$brand\"")
            }
        }
    }

    fun updateProductTags(productId: String, tags: List<String>) {
        viewModelScope.launch {
            val existing = products.value.find { it.id == productId }
            if (existing != null) {
                val picsArr = JSONArray()
                existing.pictures.forEach { p ->
                    val obj = org.json.JSONObject()
                    obj.put("id", p.id)
                    obj.put("name", p.name)
                    obj.put("url", p.url)
                    picsArr.put(obj)
                }
                val entity = ProductEntity(
                    id = existing.id,
                    name = existing.name,
                    brand = existing.brand,
                    tags = tags.joinToString(","),
                    overview = existing.overview,
                    picturesJson = picsArr.toString(),
                    createdTime = existing.createdTime
                )
                repository.insertProduct(entity)
                showBanner("Tags updated!")
            }
        }
    }

    fun deleteSpecification(specId: String) {
        viewModelScope.launch {
            repository.deleteSpecification(specId)
            showBanner("Specification removed.")
        }
    }

    fun addReview(title: String, body: String, authorName: String, rating: Int, productIds: List<String>, productTitles: List<String>) {
        viewModelScope.launch {
            val id = "rev_" + UUID.randomUUID().toString().replace("-", "").take(17)
            val number = (reviews.value.maxOfOrNull { it.reviewNumber } ?: 0) + 1
            val entity = ReviewEntity(
                id = id,
                reviewNumber = number,
                title = title,
                body = body,
                authorName = authorName.ifBlank { "Verified Reviewer" },
                rating = rating,
                productIdsJson = JSONArray(productIds).toString(),
                productTitlesJson = JSONArray(productTitles).toString(),
                picturesJson = "[]",
                createdTime = System.currentTimeMillis().toString()
            )
            repository.insertReview(entity)
            showBanner("Review published!")
        }
    }

    fun saveReview(review: ReviewItem) {
        viewModelScope.launch {
            val picsArr = JSONArray()
            review.pictures.forEach { p ->
                val obj = org.json.JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("url", p.url)
                picsArr.put(obj)
            }
            val id = if (review.id.isBlank()) "rev_" + UUID.randomUUID().toString().replace("-", "").take(17) else review.id
            val number = if (review.reviewNumber <= 0) ((reviews.value.maxOfOrNull { it.reviewNumber } ?: 0) + 1) else review.reviewNumber
            val entity = ReviewEntity(
                id = id,
                reviewNumber = number,
                title = review.title,
                body = review.body,
                authorName = review.authorName.ifBlank { "Verified Reviewer" },
                rating = review.rating,
                productIdsJson = JSONArray(review.productIds).toString(),
                productTitlesJson = JSONArray(review.productTitles).toString(),
                picturesJson = picsArr.toString(),
                createdTime = if (review.createdTime.isBlank()) System.currentTimeMillis().toString() else review.createdTime
            )
            repository.insertReview(entity)
            if (_selectedReviewForView.value?.id == id) {
                _selectedReviewForView.value = review.copy(id = id, reviewNumber = number)
            }
            showBanner("Review saved successfully!")
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            val freedFiles = repository.deleteReview(reviewId)
            if (_selectedReviewForView.value?.id == reviewId) {
                _selectedReviewForView.value = null
            }
            if (freedFiles > 0) {
                showBanner("Review deleted and $freedFiles associated image(s) removed from storage.")
            } else {
                showBanner("Review deleted.")
            }
        }
    }

    private val _isCleaningStorage = MutableStateFlow(false)
    val isCleaningStorage: StateFlow<Boolean> = _isCleaningStorage.asStateFlow()

    fun getCachedMediaFilesCount(): Int {
        return repository.getCachedMediaFilesCount()
    }

    fun freeUpOrphanStorage(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            _isCleaningStorage.value = true
            try {
                val cleaned = repository.freeUpOrphanStorage()
                if (cleaned > 0) {
                    showBanner("Storage Optimization Complete! Freed up $cleaned orphan image file(s).")
                } else {
                    showBanner("All stored images are active and in use. No orphan images found.")
                }
                onComplete(cleaned)
            } finally {
                _isCleaningStorage.value = false
            }
        }
    }

    fun resetToSeedData() {
        viewModelScope.launch {
            repository.resetToSeedData()
            clearComparison()
            showBanner("Sample benchmark catalog reloaded (20 products, 64 specs, 16 reviews)!")
        }
    }

    fun clearAllCatalogData() {
        viewModelScope.launch {
            repository.clearAllData()
            clearComparison()
            showBanner("All catalog data cleared.")
        }
    }

    // --- FULL ZIP BACKUP & RESTORE ---

    fun createFullZipToDownloads(context: Context) {
        viewModelScope.launch {
            _isBackupProcessing.value = true
            _backupProgressText.value = "Creating full backup archive..."
            try {
                val zip = BackupArchiveManager.createFullZipBackup(
                    context = context,
                    products = products.value,
                    specifications = specifications.value,
                    reviews = reviews.value,
                    users = users.value
                ) { step, current, total ->
                    _backupProgressText.value = "$step ($current/$total)"
                }
                _lastExportedZipFile.value = zip

                _backupProgressText.value = "Saving to public Downloads folder..."
                val saveResult = BackupArchiveManager.saveZipToPublicDownloads(context, zip)
                if (saveResult.success) {
                    _lastSavedPublicLocation.value = saveResult.location
                    showBanner("Backup saved to file system: ${saveResult.location}!")
                } else {
                    _lastSavedPublicLocation.value = zip.absolutePath
                    showBanner("ZIP created in cache: ${saveResult.message}")
                }
            } catch (e: Exception) {
                showBanner("Backup creation failed: ${e.localizedMessage}")
            } finally {
                _isBackupProcessing.value = false
                _backupProgressText.value = null
            }
        }
    }

    fun exportZipToUri(context: Context, targetUri: Uri) {
        viewModelScope.launch {
            _isBackupProcessing.value = true
            _backupProgressText.value = "Writing backup ZIP to selected file..."
            try {
                var zip = _lastExportedZipFile.value
                if (zip == null || !zip.exists()) {
                    zip = BackupArchiveManager.createFullZipBackup(
                        context = context,
                        products = products.value,
                        specifications = specifications.value,
                        reviews = reviews.value,
                        users = users.value
                    )
                    _lastExportedZipFile.value = zip
                }

                val ok = BackupArchiveManager.writeZipToUri(context, zip, targetUri)
                if (ok) {
                    _lastSavedPublicLocation.value = targetUri.toString()
                    showBanner("Full backup ZIP saved to chosen location!")
                } else {
                    showBanner("Failed to write to selected destination.")
                }
            } catch (e: Exception) {
                showBanner("Export failed: ${e.localizedMessage}")
            } finally {
                _isBackupProcessing.value = false
                _backupProgressText.value = null
            }
        }
    }

    fun exportJsonToUri(context: Context, targetUri: Uri) {
        viewModelScope.launch {
            _isBackupProcessing.value = true
            _backupProgressText.value = "Writing database.json to selected file..."
            try {
                val jsonString = BackupArchiveManager.buildDatabaseJson(
                    products = products.value,
                    specifications = specifications.value,
                    reviews = reviews.value,
                    users = users.value
                )
                val ok = BackupArchiveManager.writeJsonToUri(context, jsonString, targetUri)
                if (ok) {
                    _lastSavedPublicLocation.value = targetUri.toString()
                    showBanner("database.json exported successfully!")
                } else {
                    showBanner("Failed to write JSON to selected destination.")
                }
            } catch (e: Exception) {
                showBanner("JSON export failed: ${e.localizedMessage}")
            } finally {
                _isBackupProcessing.value = false
                _backupProgressText.value = null
            }
        }
    }

    fun restoreFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isBackupProcessing.value = true
            _backupProgressText.value = "Restoring database from backup file..."
            try {
                val result = BackupArchiveManager.restoreFromZipUri(context, uri, database)
                if (result.success) {
                    showBanner(result.message)
                    clearComparison()
                } else {
                    showBanner(result.message)
                }
            } catch (e: Exception) {
                showBanner("Restore failed: ${e.localizedMessage}")
            } finally {
                _isBackupProcessing.value = false
                _backupProgressText.value = null
            }
        }
    }

    // UpNote HTML & Nested Folder Import Actions
    fun dismissImportDialog() {
        _pendingImportDrafts.value = null
    }

    fun startFolderTreeImport(context: Context, treeUri: Uri) {
        viewModelScope.launch {
            _isImportProcessing.value = true
            _importProgressText.value = "Scanning folder tree for reviews and pictures..."
            try {
                val drafts = UpNoteHtmlImportManager.parseFromFolderTree(context, treeUri, products.value)
                if (drafts.isEmpty()) {
                    showBanner("No HTML review notes found in selected folder.")
                } else {
                    _pendingImportDrafts.value = drafts
                }
            } catch (e: Exception) {
                showBanner("Folder import failed: ${e.localizedMessage}")
            } finally {
                _isImportProcessing.value = false
                _importProgressText.value = null
            }
        }
    }

    fun startZipImport(context: Context, zipUri: Uri) {
        viewModelScope.launch {
            _isImportProcessing.value = true
            _importProgressText.value = "Extracting ZIP and scanning reviews..."
            try {
                val drafts = UpNoteHtmlImportManager.parseFromZip(context, zipUri, products.value)
                if (drafts.isEmpty()) {
                    showBanner("No HTML notes found in ZIP archive.")
                } else {
                    _pendingImportDrafts.value = drafts
                }
            } catch (e: Exception) {
                showBanner("ZIP import failed: ${e.localizedMessage}")
            } finally {
                _isImportProcessing.value = false
                _importProgressText.value = null
            }
        }
    }

    fun startSingleHtmlImport(context: Context, htmlUri: Uri) {
        viewModelScope.launch {
            _isImportProcessing.value = true
            _importProgressText.value = "Parsing HTML review note..."
            try {
                val drafts = UpNoteHtmlImportManager.parseFromSingleHtmlFile(context, htmlUri, products.value)
                if (drafts.isEmpty()) {
                    showBanner("Could not parse HTML review note.")
                } else {
                    _pendingImportDrafts.value = drafts
                }
            } catch (e: Exception) {
                showBanner("HTML import failed: ${e.localizedMessage}")
            } finally {
                _isImportProcessing.value = false
                _importProgressText.value = null
            }
        }
    }

    fun handleShareIntent(
        context: Context,
        subject: String?,
        textOrHtml: String?,
        streamUris: List<Uri>
    ) {
        viewModelScope.launch {
            _isImportProcessing.value = true
            _importProgressText.value = "Importing shared note from UpNote..."
            try {
                val draft = UpNoteHtmlImportManager.parseFromShareIntent(
                    context = context,
                    subject = subject,
                    textOrHtml = textOrHtml,
                    streamUris = streamUris,
                    availableProducts = products.value
                )
                _pendingImportDrafts.value = listOf(draft)
                setActiveTab(AppNavTab.REVIEWS)
            } catch (e: Exception) {
                showBanner("Failed to import shared note: ${e.localizedMessage}")
            } finally {
                _isImportProcessing.value = false
                _importProgressText.value = null
            }
        }
    }

    fun confirmAndSaveImportedReviews(drafts: List<ImportedReviewDraft>) {
        viewModelScope.launch {
            if (drafts.isEmpty()) {
                _pendingImportDrafts.value = null
                return@launch
            }
            var currentMaxNumber = reviews.value.maxOfOrNull { it.reviewNumber } ?: 0
            val entitiesToInsert = mutableListOf<ReviewEntity>()
            val currentProducts = products.value

            drafts.forEach { draft ->
                currentMaxNumber++
                val reviewItem = draft.toReviewItem(currentProducts, currentMaxNumber)
                val picsArr = JSONArray()
                reviewItem.pictures.forEach { p ->
                    val obj = org.json.JSONObject()
                    obj.put("id", p.id)
                    obj.put("name", p.name)
                    obj.put("url", p.url)
                    picsArr.put(obj)
                }
                val entity = ReviewEntity(
                    id = reviewItem.id,
                    reviewNumber = reviewItem.reviewNumber,
                    title = reviewItem.title,
                    body = reviewItem.body,
                    authorName = reviewItem.authorName,
                    rating = reviewItem.rating,
                    productIdsJson = JSONArray(reviewItem.productIds).toString(),
                    productTitlesJson = JSONArray(reviewItem.productTitles).toString(),
                    picturesJson = picsArr.toString(),
                    createdTime = reviewItem.createdTime
                )
                entitiesToInsert.add(entity)
            }

            entitiesToInsert.forEach { entity ->
                repository.insertReview(entity)
            }

            val totalPics = drafts.sumOf { it.pictures.size }
            _pendingImportDrafts.value = null
            showBanner("Imported ${drafts.size} review(s) with $totalPics picture(s) successfully!")
            setActiveTab(AppNavTab.REVIEWS)
        }
    }
}
