package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.ProductItem
import com.example.model.ReviewItem
import com.example.model.SpecificationItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DataBackupScreen(
    products: List<ProductItem>,
    specifications: List<SpecificationItem>,
    reviews: List<ReviewItem>,
    isProcessing: Boolean,
    progressText: String?,
    lastExportedZip: File?,
    lastSavedPublicLocation: String?,
    onCreateFullZipToDownloads: () -> Unit,
    onExportZipToUri: (Uri) -> Unit,
    onExportJsonToUri: (Uri) -> Unit,
    onRestoreFromUri: (Uri) -> Unit,
    onResetToSeedData: () -> Unit,
    onClearAllData: () -> Unit,
    onFreeUpStorage: () -> Unit = {},
    onImportFolder: (Uri) -> Unit = {},
    onImportZip: (Uri) -> Unit = {},
    onImportHtml: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US) }

    // 1. SAF CreateDocument for saving ZIP directly to user-picked folder/file
    val createZipDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportZipToUri(uri)
        }
    }

    // 2. SAF CreateDocument for saving JSON directly to user-picked folder/file
    val createJsonDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportJsonToUri(uri)
        }
    }

    // 3. SAF OpenDocument for picking ANY backup file (.zip or .json) from storage
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    // 4. SAF Folder Tree launcher for UpNote / HTML folder import
    val importFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportFolder(uri)
        }
    }

    // 5. SAF ZIP launcher for UpNote notes
    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportZip(uri)
        }
    }

    // 6. SAF HTML launcher for individual note
    val importHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportHtml(uri)
        }
    }

    // Function to share or export ZIP via Android share sheet
    fun shareZipFile(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                putExtra(Intent.EXTRA_TEXT, "ProductCompare full backup archive: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share or Save Backup Archive"))
        } catch (e: Exception) {
            Toast.makeText(context, "Share failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val totalPhotos = remember(products, reviews) {
        products.sumOf { it.pictures.size } + reviews.sumOf { it.pictures.size }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        val isLaptop = maxWidth >= 720.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Header Card: Data & Backup Hub (Screenshot 4)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Data & Backup Hub",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Offline Persistence & Archive Engine",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Header architecture pills placed cleanly BELOW Data & Backup Hub title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "100% Local-App Architecture",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEEF2FF)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FolderZip,
                                        contentDescription = null,
                                        tint = Color(0xFF4338CA),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "IndexedDB & Local Archive",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4338CA)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Save and restore your complete product database, full specifications tables, user review notes, and offline pictures directly onto your device file system or Windows folders.",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // 2. 4 Stat Summary Cards (Responsive 4-column row on Laptop, 2x2 on Phone)
            item {
                if (isLaptop) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCardView("PRODUCTS", "${products.size}", "Active catalog records", Color(0xFF0284C7), Modifier.weight(1f))
                        StatCardView("SPECIFICATIONS", "${specifications.size}", "Matrix attributes", Color(0xFF7C3AED), Modifier.weight(1f))
                        StatCardView("USER REVIEWS", "${reviews.size}", "Feedback notes", Color(0xFFD97706), Modifier.weight(1f))
                        StatCardView("PHOTOS IN STORAGE", "$totalPhotos", "Indexed pictures", Color(0xFF16A34A), Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCardView("PRODUCTS", "${products.size}", "Active records", Color(0xFF0284C7), Modifier.weight(1f))
                            StatCardView("SPECIFICATIONS", "${specifications.size}", "Attributes", Color(0xFF7C3AED), Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCardView("USER REVIEWS", "${reviews.size}", "Feedback notes", Color(0xFFD97706), Modifier.weight(1f))
                            StatCardView("PHOTOS IN STORAGE", "$totalPhotos", "Indexed pictures", Color(0xFF16A34A), Modifier.weight(1f))
                        }
                    }
                }
            }

            // 3. Export & Restore Cards (Side-by-side on Laptop, stacked on Phone)
            item {
                if (isLaptop) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            ExportBackupSectionContent(
                                dateFormat = dateFormat,
                                createZipDocLauncher = createZipDocLauncher,
                                onCreateFullZipToDownloads = onCreateFullZipToDownloads,
                                lastExportedZip = lastExportedZip,
                                lastSavedPublicLocation = lastSavedPublicLocation,
                                shareZipFile = { shareZipFile(it) },
                                createJsonDocLauncher = createJsonDocLauncher,
                                context = context
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            RestoreBackupSectionContent(
                                openDocLauncher = openDocLauncher
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ExportBackupSectionContent(
                                dateFormat = dateFormat,
                                createZipDocLauncher = createZipDocLauncher,
                                onCreateFullZipToDownloads = onCreateFullZipToDownloads,
                                lastExportedZip = lastExportedZip,
                                lastSavedPublicLocation = lastSavedPublicLocation,
                                shareZipFile = { shareZipFile(it) },
                                createJsonDocLauncher = createJsonDocLauncher,
                                context = context
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RestoreBackupSectionContent(
                                openDocLauncher = openDocLauncher
                            )
                        }
                    }
                }
            }

            // 4. Section: Import UpNote HTML Reviews & Notes
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEEF2FF),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.DriveFolderUpload,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Import UpNote HTML Notes & Reviews",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Imports nested folders, HTML notes, and auto-relinks all pictures",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Text(
                            text = "Supports importing an entire parent folder containing multiple review subfolders with their image folders, or ZIP archives containing exported HTML notes.",
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { importFolderLauncher.launch(null) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Import Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0284C7))
                                Spacer(Modifier.width(6.dp))
                                Text("Import ZIP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { importHtmlLauncher.launch(arrayOf("text/html", "text/plain")) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF10B981))
                            Spacer(Modifier.width(6.dp))
                            Text("Import Single .HTML Note File", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 5. Section: Database Utilities & Seed Reset
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Database Management",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showResetConfirm = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF4F46E5)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reload Sample Data", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { showClearConfirm = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFDC2626)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Clear Catalog", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Storage Maintenance: Free Up Orphan Storage
                        OutlinedButton(
                            onClick = onFreeUpStorage,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF0F766E)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF99F6E4)),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0F766E))
                            Spacer(Modifier.width(8.dp))
                            Text("Free Up Storage (Remove Unused & Orphan Images)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(30.dp)) }
        }

        // Processing Overlay Dialog
        if (isProcessing) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp),
                        modifier = Modifier.width(300.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF4F46E5),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = progressText ?: "Processing...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation: Restore File
    if (showRestoreConfirmDialog && selectedRestoreUri != null) {
        val uri = selectedRestoreUri!!
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                selectedRestoreUri = null
            },
            title = { Text("Confirm Database Restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to restore from this backup file?")
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text(
                            text = uri.lastPathSegment ?: uri.toString(),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Text(
                        "This will populate your catalog with products, specifications, and reviews from this archive.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        onRestoreFromUri(uri)
                        selectedRestoreUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    selectedRestoreUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation: Reset to Seed Catalog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reload Sample Benchmark Catalog?") },
            text = { Text("This will reload the initial 20 benchmark products (Plaud Note, iPhone 16 Pro, Galaxy S24 Ultra, Sony A7 IV, etc.) and reset specifications.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetToSeedData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("Reload Sample Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Confirmation: Clear All Data
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Catalog Data?") },
            text = { Text("This will delete all products, specifications, and reviews from the database. Make sure you have exported a backup first.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatCardView(
    title: String,
    count: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = color,
                    modifier = Modifier.size(8.dp)
                ) {}
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = count,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun ExportBackupSectionContent(
    dateFormat: SimpleDateFormat,
    createZipDocLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onCreateFullZipToDownloads: () -> Unit,
    lastExportedZip: File?,
    lastSavedPublicLocation: String?,
    shareZipFile: (File) -> Unit,
    createJsonDocLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    context: Context
) {
    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFEEF2FF),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Archive,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Export Backup to File System",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Creates full .zip archive with database.json, review notes & photos",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Primary Action Button: Pick Folder to Save ZIP
        Button(
            onClick = {
                val timestamp = dateFormat.format(Date())
                createZipDocLauncher.launch("ProductCompare_Backup_$timestamp.zip")
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("btn_save_zip_to_folder")
        ) {
            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Save Backup ZIP to Storage...",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Secondary Action: Instant Auto-Save to Public Downloads
        OutlinedButton(
            onClick = onCreateFullZipToDownloads,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFFF8FAFC),
                contentColor = Color(0xFF0F172A)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("btn_auto_save_downloads")
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Auto-Save to Downloads Folder",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Additional Export Options Row: Share & Export JSON
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Share ZIP button
            OutlinedButton(
                onClick = {
                    if (lastExportedZip != null && lastExportedZip.exists()) {
                        shareZipFile(lastExportedZip)
                    } else {
                        Toast.makeText(context, "Create a backup first to share.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = lastExportedZip != null && lastExportedZip.exists(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share ZIP", fontSize = 12.sp)
            }

            // Export database.json
            OutlinedButton(
                onClick = {
                    val timestamp = dateFormat.format(Date())
                    createJsonDocLauncher.launch("ProductCompare_database_$timestamp.json")
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) {
                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export .JSON", fontSize = 12.sp)
            }
        }

        // Last Saved Location Notification
        if (lastSavedPublicLocation != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Last Saved File Location:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                        Text(
                            text = lastSavedPublicLocation,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF166534),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestoreBackupSectionContent(
    openDocLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF0FDF4),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.SettingsBackupRestore,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Restore Database from File",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Supports both .zip archives and direct database.json files",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Text(
            text = "Select any previously exported ProductCompare .zip archive or database.json file from your phone storage, Downloads folder, or Windows subsystem disk.",
            fontSize = 12.sp,
            color = Color(0xFF475569),
            lineHeight = 16.sp
        )

        Button(
            onClick = {
                openDocLauncher.launch(arrayOf("*/*"))
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF16A34A),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("btn_select_file_restore")
        ) {
            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Select Backup File to Restore (.zip / .json)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MetricPill(label: String, count: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(6.dp)
            ) {}
            Text(
                text = "$count $label",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
