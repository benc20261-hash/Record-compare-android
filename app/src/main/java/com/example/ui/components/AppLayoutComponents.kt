package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppSpacing

/**
 * AppBadge: Safe, single-line pill badge that NEVER wraps vertically.
 * Prevents horizontal squeezing and runaway vertical card expansion.
 */
@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.PrimaryBrandLight,
    textColor: Color = AppColors.PrimaryBrand,
    borderColor: Color? = null,
    fontSize: TextUnit = 11.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    horizontalPadding: Dp = 7.dp,
    verticalPadding: Dp = 2.dp,
    cornerRadius: Dp = AppSpacing.badgeCornerRadius,
    icon: @Composable (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = borderColor?.let { BorderStroke(1.dp, it) },
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            if (icon != null) {
                icon()
            }
            Text(
                text = text,
                color = textColor,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * AppCardHeader: Standard card header layout ensuring:
 * 1. Title/subtitle Column always receives Modifier.weight(1f) when action/badge is present.
 * 2. Badges and subtitles wrap gracefully beneath the title, never getting squeezed into 1-character columns.
 * 3. Prevents multi-line vertical text wrapping and rogue white gaps across all screen sizes.
 */
@Composable
fun AppCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFF15803D),
    badgeBackground: Color = Color(0xFFF0FDF4),
    badgeBorder: Color? = Color(0xFFBBF7D0),
    icon: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (icon != null) {
            icon()
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = AppColors.HeadingText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (badgeText != null) {
                    AppBadge(
                        text = badgeText,
                        textColor = badgeColor,
                        backgroundColor = badgeBackground,
                        borderColor = badgeBorder,
                        fontSize = 10.sp
                    )
                }
            }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppColors.SubtitleText,
                    lineHeight = 16.sp
                )
            }
        }

        if (action != null) {
            action()
        }
    }
}

/**
 * AppSummaryCard: Standard layout for summary and status bars.
 * Keeps stats on top with secondary actions (like Reset),
 * and places prominent navigation actions in a dedicated full-width row below.
 * Completely eliminates the "squeezed action button" bug that created the 180dp tall black pillar.
 */
@Composable
fun AppSummaryCard(
    statsText: String,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    onReset: (() -> Unit)? = null,
    resetLabel: String = "Reset",
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionButtonColor: Color = AppColors.HeadingText
) {
    Card(
        shape = RoundedCornerShape(AppSpacing.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        border = BorderStroke(1.dp, AppColors.CardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = statsText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.HeadingText
                    )
                    if (secondaryText != null) {
                        Text("•", fontSize = 12.sp, color = AppColors.PlaceholderText)
                        Text(
                            text = secondaryText,
                            fontSize = 12.sp,
                            color = AppColors.SubtitleText
                        )
                    }
                }

                if (onReset != null) {
                    TextButton(
                        onClick = onReset,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = resetLabel,
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (actionButtonText != null && onActionClick != null) {
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(AppSpacing.buttonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = actionButtonColor,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    ButtonText(
                        text = actionButtonText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
