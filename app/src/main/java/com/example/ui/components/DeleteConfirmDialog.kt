package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

/**
 * Compact M3 Confirmation Dialog for Delete, Remove, and Clear actions.
 * Features a clean layout, clear warning badge, distinct item/batch summary,
 * and quick Confirm & Cancel buttons.
 */
@Composable
fun DeleteConfirmDialog(
    title: String = "Delete Confirmation",
    message: String,
    confirmText: String = "Delete",
    dismissText: String = "Cancel",
    icon: ImageVector = Icons.Default.DeleteOutline,
    isDestructive: Boolean = true,
    testTagPrefix: String = "delete_confirm",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val confirmBtnColor = if (isDestructive) Color(0xFFDC2626) else Color(0xFF4F46E5)
    val badgeBgColor = if (isDestructive) Color(0xFFFEE2E2) else Color(0xFFEEF2FF)
    val badgeIconColor = if (isDestructive) Color(0xFFDC2626) else Color(0xFF4F46E5)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White,
        tonalElevation = 6.dp,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(badgeBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeIconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 13.sp,
                color = Color(0xFF475569),
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmBtnColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("${testTagPrefix}_confirm_btn")
            ) {
                Text(
                    text = confirmText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF475569)
                ),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("${testTagPrefix}_cancel_btn")
            ) {
                Text(
                    text = dismissText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
