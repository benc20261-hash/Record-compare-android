package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppNavTab

@Composable
fun DesktopSidebar(
    activeTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    productCount: Int,
    compareCount: Int,
    reviewCount: Int,
    onAddProductClick: () -> Unit,
    onSmartAddClick: () -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(250.dp)
            .fillMaxHeight(),
        color = Color.White,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // 1. App Header / Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, top = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF4F46E5),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Scale,
                            contentDescription = "App Logo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ProductCompare",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Text(
                            text = "Desktop Hub • WSA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // 2. Primary Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = onAddProductClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("desktop_sidebar_add_btn"),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Product", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSmartAddClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFFFBEB),
                        contentColor = Color(0xFFB45309)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("desktop_sidebar_smart_add_btn"),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Smart Add AI", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            // Navigation Label
            Text(
                text = "NAVIGATION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // 3. Navigation Items List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SidebarNavItem(
                    label = "Catalog",
                    icon = Icons.Default.GridView,
                    selected = activeTab == AppNavTab.CATALOG,
                    badgeText = "$productCount",
                    badgeColor = Color(0xFF4F46E5),
                    onClick = { onTabSelected(AppNavTab.CATALOG) },
                    testTag = "desktop_nav_catalog"
                )

                SidebarNavItem(
                    label = "Compare",
                    icon = Icons.Default.CompareArrows,
                    selected = activeTab == AppNavTab.COMPARE,
                    badgeText = if (compareCount > 0) "$compareCount" else null,
                    badgeColor = Color(0xFF0284C7),
                    onClick = { onTabSelected(AppNavTab.COMPARE) },
                    testTag = "desktop_nav_compare"
                )

                SidebarNavItem(
                    label = "Reviews",
                    icon = Icons.Default.RateReview,
                    selected = activeTab == AppNavTab.REVIEWS,
                    badgeText = if (reviewCount > 0) "$reviewCount" else null,
                    badgeColor = Color(0xFFD97706),
                    onClick = { onTabSelected(AppNavTab.REVIEWS) },
                    testTag = "desktop_nav_reviews"
                )

                SidebarNavItem(
                    label = "Smart Import",
                    icon = Icons.Default.AutoAwesome,
                    selected = activeTab == AppNavTab.SMART_ADD,
                    badgeText = "AI",
                    badgeColor = Color(0xFF10B981),
                    onClick = { onTabSelected(AppNavTab.SMART_ADD) },
                    testTag = "desktop_nav_smart_add"
                )

                SidebarNavItem(
                    label = "Data Backup",
                    icon = Icons.Default.Storage,
                    selected = activeTab == AppNavTab.DATA_BACKUP,
                    badgeText = null,
                    badgeColor = Color(0xFF64748B),
                    onClick = { onTabSelected(AppNavTab.DATA_BACKUP) },
                    testTag = "desktop_nav_backup"
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            // 4. Footer: Live Sync & Stats Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSyncClick)
                    .testTag("desktop_sidebar_sync_card")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = "Sync",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Database Sync",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "$productCount devices • $reviewCount reviews",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badgeText: String?,
    badgeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Color(0xFFEEF2FF) else Color.Transparent,
        label = "bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF4F46E5) else Color(0xFF475569),
        label = "fg"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) badgeColor else Color(0xFFE2E8F0),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = if (selected) Color.White else Color(0xFF475569),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
