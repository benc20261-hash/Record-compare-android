package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Size
import com.example.model.PictureItem
import com.example.model.resolveImageModel

@Composable
fun FullScreenPhotoLightbox(
    images: List<PictureItem>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) {
        onDismiss()
        return
    }

    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, images.size - 1)) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current

    // Reset zoom when switching images
    LaunchedEffect(currentIndex) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    val currentImage = images[currentIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
        ) {
            // Main Image with Double-Tap Zoom and Pinch/Pan gestures
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentIndex) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    val targetScale = 2.5f
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val maxOffsetX = (size.width * (targetScale - 1)) / 2
                                    val maxOffsetY = (size.height * (targetScale - 1)) / 2
                                    offsetX = ((centerX - tapOffset.x) * (targetScale - 1)).coerceIn(-maxOffsetX, maxOffsetX)
                                    offsetY = ((centerY - tapOffset.y) * (targetScale - 1)).coerceIn(-maxOffsetY, maxOffsetY)
                                    scale = targetScale
                                }
                            }
                        )
                    }
                    .pointerInput(currentIndex) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1)) / 2
                                val maxOffsetY = (size.height * (scale - 1)) / 2
                                offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                var isImageLoading by remember(currentIndex) { mutableStateOf(true) }

                val fullResModel = remember(currentImage.url, currentImage.id) {
                    resolveImageModel(context, currentImage.url, currentImage.name.ifBlank { null })
                }

                val imageRequest = remember(fullResModel, context) {
                    ImageRequest.Builder(context)
                        .data(fullResModel)
                        .size(Size.ORIGINAL) // True 100% full original un-downsampled resolution!
                        .precision(Precision.EXACT)
                        .crossfade(true)
                        .build()
                }

                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = currentImage.name,
                    contentScale = ContentScale.Fit,
                    onSuccess = { isImageLoading = false },
                    onError = { isImageLoading = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 80.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )

                if (isImageLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color(0xFF6366F1),
                        strokeWidth = 3.dp
                    )
                }
            }

            // Navigation Arrow: Left (<)
            if (currentIndex > 0) {
                IconButton(
                    onClick = { currentIndex-- },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000))
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous Image",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Navigation Arrow: Right (>)
            if (currentIndex < images.size - 1) {
                IconButton(
                    onClick = { currentIndex++ },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000))
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next Image",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Top Header Bar (Screenshot full screen pic.png)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color(0xCC0F172A))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title, Counter, and Full Res Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = currentImage.name.ifBlank { "gsmarena_${currentIndex + 1}.jpg" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = "${currentIndex + 1} / ${images.size}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Full Resolution Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E1B4B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1))
                    ) {
                        Text(
                            text = "Full Res",
                            color = Color(0xFFA5B4FC),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                }

                // Controls: Zoom Out, Zoom Level, Zoom In, Save, and Red Close Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Zoom Out
                    IconButton(
                        onClick = { scale = (scale - 0.25f).coerceAtLeast(1f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ZoomOut,
                            contentDescription = "Zoom out",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Zoom percentage (clickable to quickly reset to 100%)
                    Text(
                        text = "${(scale * 100).toInt()}%",
                        color = if (scale > 1f) Color(0xFFA5B4FC) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    // Zoom In
                    IconButton(
                        onClick = { scale = (scale + 0.25f).coerceAtMost(5f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = "Zoom in",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Save / Copy URL Button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Image URL", currentImage.url))
                            Toast.makeText(context, "Image URL copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Save / Copy",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Red Circular Close Button (Screenshot full screen pic.png)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE11D48))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom Thumbnail Strip (Screenshot full screen pic.png)
            if (images.size > 1) {
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = Color(0xCC0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        images.forEachIndexed { idx, pic ->
                            val isSelected = idx == currentIndex
                            Box(
                                modifier = Modifier
                                    .size(54.dp, 44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF6366F1) else Color(0xFF334155),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { currentIndex = idx }
                            ) {
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
