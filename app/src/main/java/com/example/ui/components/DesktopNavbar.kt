package com.example.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppNavTab

/**
 * Responsive Desktop & Tablet Top Navigation Bar matching the Productc webapp navbar layout.
 * Optimized for scrcpy phone mirroring in landscape mode and laptop displays.
 * Adapts padding, brand subtitle, tab dimensions, and action buttons dynamically.
 */
@Composable
fun DesktopNavbar(
    activeTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    productCount: Int,
    compareCount: Int,
    reviewCount: Int,
    onAddProductClick: () -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
        ) {
            val isNarrowLandscape = maxWidth < 1050.dp
            val isUltraCompact = maxWidth < 850.dp
            val isShortHeight = maxHeight < 500.dp

            val vertPadding = if (isShortHeight || isNarrowLandscape) 5.dp else 10.dp
            val horizPadding = if (isNarrowLandscape) 12.dp else 20.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizPadding, vertical = vertPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Brand Logo & App Info (Left)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isNarrowLandscape) 6.dp else 10.dp),
                    modifier = Modifier
                        .clickable { onTabSelected(AppNavTab.CATALOG) }
                        .testTag("desktop_brand_logo_btn")
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF4F46E5),
                        modifier = Modifier.size(if (isNarrowLandscape) 30.dp else 36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Scale,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (isNarrowLandscape) 18.dp else 22.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "ProductCompare",
                                fontWeight = FontWeight.Black,
                                fontSize = if (isNarrowLandscape) 14.sp else 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            if (!isUltraCompact) {
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDBEAFE))
                                ) {
                                    Text(
                                        text = "Live",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        if (!isNarrowLandscape) {
                            Text(
                                text = "Side-by-side consumer tech benchmark",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // 2. Desktop Segmented View Switcher (Center - Scrollable if space is constrained)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .padding(horizontal = if (isNarrowLandscape) 4.dp else 8.dp)
                        .weight(1f, fill = false)
                ) {
                    val tabScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .horizontalScroll(tabScrollState)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isNarrowLandscape) 2.dp else 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Catalog Tab
                        DesktopNavTabItem(
                            label = "Catalog",
                            icon = Icons.Default.Layers,
                            isSelected = activeTab == AppNavTab.CATALOG,
                            isCompact = isNarrowLandscape,
                            onClick = { onTabSelected(AppNavTab.CATALOG) },
                            testTag = "desktop_nav_catalog"
                        )

                        // Compare Tab
                        DesktopNavTabItem(
                            label = "Compare",
                            icon = Icons.Default.Scale,
                            isSelected = activeTab == AppNavTab.COMPARE,
                            badgeCount = if (compareCount > 0) compareCount else null,
                            isCompact = isNarrowLandscape,
                            onClick = { onTabSelected(AppNavTab.COMPARE) },
                            testTag = "desktop_nav_compare"
                        )

                        // Smart Add Tab
                        val isSmartAddSelected = activeTab == AppNavTab.SMART_ADD
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = if (isSmartAddSelected) Color(0xFF0F172A) else Color(0xFFFEF3C7),
                            shadowElevation = if (isSmartAddSelected) 1.dp else 0.dp,
                            modifier = Modifier
                                .clickable { onTabSelected(AppNavTab.SMART_ADD) }
                                .testTag("desktop_nav_smart_add")
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (isNarrowLandscape) 8.dp else 12.dp,
                                    vertical = if (isNarrowLandscape) 4.dp else 6.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isSmartAddSelected) Color(0xFFFBBF24) else Color(0xFFD97706),
                                    modifier = Modifier.size(if (isNarrowLandscape) 13.dp else 15.dp)
                                )
                                Text(
                                    text = if (isNarrowLandscape) "Smart Add" else "Smart Add Product",
                                    fontSize = if (isNarrowLandscape) 11.sp else 12.sp,
                                    fontWeight = if (isSmartAddSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSmartAddSelected) Color.White else Color(0xFF92400E)
                                )
                            }
                        }

                        // Reviews Tab
                        DesktopNavTabItem(
                            label = "Reviews",
                            icon = Icons.Default.RateReview,
                            isSelected = activeTab == AppNavTab.REVIEWS,
                            badgeCount = if (reviewCount > 0) reviewCount else null,
                            isCompact = isNarrowLandscape,
                            onClick = { onTabSelected(AppNavTab.REVIEWS) },
                            testTag = "desktop_nav_reviews"
                        )

                        // Data & Backup Tab
                        DesktopNavTabItem(
                            label = if (isNarrowLandscape) "Backup" else "Data & Backup",
                            icon = Icons.Default.Storage,
                            isSelected = activeTab == AppNavTab.DATA_BACKUP,
                            isCompact = isNarrowLandscape,
                            onClick = { onTabSelected(AppNavTab.DATA_BACKUP) },
                            testTag = "desktop_nav_backup"
                        )
                    }
                }

                // 3. Desktop Action Controls (Right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isNarrowLandscape) 4.dp else 8.dp)
                ) {
                    // + Add Product Button
                    Button(
                        onClick = onAddProductClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(
                            horizontal = if (isNarrowLandscape) 8.dp else 12.dp,
                            vertical = if (isNarrowLandscape) 4.dp else 6.dp
                        ),
                        modifier = Modifier
                            .height(if (isNarrowLandscape) 32.dp else 36.dp)
                            .testTag("desktop_add_product_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(if (isNarrowLandscape) 14.dp else 16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = if (isNarrowLandscape) "Add" else "Add Product",
                            fontSize = if (isNarrowLandscape) 11.sp else 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }



                    // Database Status / Sync Icon
                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier
                            .size(if (isNarrowLandscape) 32.dp else 36.dp)
                            .testTag("desktop_sync_btn")
                    ) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = "Database Status",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(if (isNarrowLandscape) 19.dp else 22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopNavTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    badgeCount: Int? = null,
    isCompact: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 8.dp else 12.dp,
                vertical = if (isCompact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF4F46E5) else Color(0xFF64748B),
                modifier = Modifier.size(if (isCompact) 13.dp else 15.dp)
            )
            Text(
                text = label,
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B)
            )
            if (badgeCount != null) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF94A3B8),
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Text(
                        text = "$badgeCount",
                        fontSize = if (isCompact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
