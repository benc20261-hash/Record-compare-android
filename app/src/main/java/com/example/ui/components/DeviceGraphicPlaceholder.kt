package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.detectDeviceCategory

@Composable
fun DeviceGraphicPlaceholder(
    title: String,
    brand: String,
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    val category = detectDeviceCategory(title, brand, tags)

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8FAFC), Color(0xFFEEF2F6))
                )
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            when (category) {
                "phone" -> {
                    val phoneW = w * 0.38f
                    val phoneH = h * 0.65f
                    val left = cx - phoneW / 2
                    val top = cy - phoneH / 2 - 10f

                    // Outer chassis
                    drawRoundRect(
                        color = Color(0xFF1E1B4B),
                        topLeft = Offset(left, top),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                    // Inner screen
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF06B6D4))
                        ),
                        topLeft = Offset(left + 6f, top + 6f),
                        size = Size(phoneW - 12f, phoneH - 12f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    // Dynamic Island camera pill
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(cx - 16f, top + 10f),
                        size = Size(32f, 8f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
                "laptop" -> {
                    val screenW = w * 0.55f
                    val screenH = h * 0.38f
                    val left = cx - screenW / 2
                    val top = cy - screenH / 2 - 14f

                    // Screen
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(left, top),
                        size = Size(screenW, screenH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF334155))
                        ),
                        topLeft = Offset(left + 4f, top + 4f),
                        size = Size(screenW - 8f, screenH - 8f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    // Base
                    drawRoundRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(cx - screenW * 0.6f, top + screenH),
                        size = Size(screenW * 1.2f, 12f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
                "mic" -> {
                    val bodyW = w * 0.36f
                    val bodyH = h * 0.5f
                    val left = cx - bodyW / 2
                    val top = cy - bodyH / 2

                    // Transmitter body
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(left, top),
                        size = Size(bodyW, bodyH),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                    // Mic grille
                    drawRoundRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(cx - 20f, top - 12f),
                        size = Size(40f, 14f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    // OLED Screen
                    drawRoundRect(
                        color = Color(0xFF020617),
                        topLeft = Offset(left + 10f, top + 14f),
                        size = Size(bodyW - 20f, 26f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    // Signal lines
                    drawLine(
                        color = Color(0xFF10B981),
                        start = Offset(left + 16f, top + 24f),
                        end = Offset(left + 36f, top + 24f),
                        strokeWidth = 3f
                    )
                }
                "glasses" -> {
                    val frameW = w * 0.28f
                    val frameH = h * 0.32f
                    // Left Lens
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(Color(0xFF047857), Color(0xFF065F46))),
                        topLeft = Offset(cx - frameW - 8f, cy - frameH / 2),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    drawRoundRect(
                        color = Color(0xFF6366F1),
                        topLeft = Offset(cx - frameW - 8f, cy - frameH / 2),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(width = 4f)
                    )
                    // Right Lens
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(Color(0xFF047857), Color(0xFF065F46))),
                        topLeft = Offset(cx + 8f, cy - frameH / 2),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    drawRoundRect(
                        color = Color(0xFF6366F1),
                        topLeft = Offset(cx + 8f, cy - frameH / 2),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(width = 4f)
                    )
                    // Bridge
                    drawLine(
                        color = Color(0xFF4338CA),
                        start = Offset(cx - 8f, cy - 8f),
                        end = Offset(cx + 8f, cy - 8f),
                        strokeWidth = 5f
                    )
                }
                else -> {
                    // General device box
                    val boxW = w * 0.42f
                    val boxH = h * 0.48f
                    drawRoundRect(
                        color = Color(0xFFEEF2FF),
                        topLeft = Offset(cx - boxW / 2, cy - boxH / 2 - 10f),
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                    drawRoundRect(
                        color = Color(0xFF6366F1),
                        topLeft = Offset(cx - boxW / 2, cy - boxH / 2 - 10f),
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(16f, 16f),
                        style = Stroke(width = 3f)
                    )
                    drawCircle(
                        color = Color(0xFF818CF8),
                        radius = 10f,
                        center = Offset(cx, cy + boxH / 2 - 25f)
                    )
                }
            }
        }

        // Overlay text badge at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (brand.isNotBlank()) brand.uppercase() else title.take(16),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center
            )
        }
    }
}
