package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppNavTab
import kotlin.math.roundToInt

@Composable
fun AppNavigationRail(
    activeTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    productCount: Int,
    compareCount: Int,
    reviewCount: Int,
    appFontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .testTag("app_navigation_rail"),
        containerColor = Color.White,
        contentColor = Color(0xFF0F172A),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(vertical = 14.dp, horizontal = 8.dp)
            ) {
                // Brand Icon
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF4F46E5),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Scale,
                            contentDescription = "ProductCompare Logo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Brand text & Live pill
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Compare",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF)
                    ) {
                        Text(
                            text = "Live",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }

                // Font size controls
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val next = ((appFontScale - 0.1f) * 10).roundToInt() / 10f
                                    onFontScaleChange(next.coerceAtLeast(0.7f))
                                },
                                modifier = Modifier.size(22.dp).testTag("rail_font_decrease"),
                                enabled = appFontScale > 0.75f
                            ) {
                                Text(
                                    "A-",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (appFontScale > 0.75f) Color(0xFF334155) else Color(0xFF94A3B8)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val next = ((appFontScale + 0.1f) * 10).roundToInt() / 10f
                                    onFontScaleChange(next.coerceAtMost(1.6f))
                                },
                                modifier = Modifier.size(22.dp).testTag("rail_font_increase"),
                                enabled = appFontScale < 1.55f
                            ) {
                                Text(
                                    "A+",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (appFontScale < 1.55f) Color(0xFF334155) else Color(0xFF94A3B8)
                                )
                            }
                        }
                        Text(
                            text = "${(appFontScale * 100).roundToInt()}%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            modifier = Modifier
                                .clickable { onFontScaleChange(1.0f) }
                                .padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main navigation items
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavigationRailItem(
                    selected = activeTab == AppNavTab.CATALOG,
                    onClick = { onTabSelected(AppNavTab.CATALOG) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (productCount > 0) {
                                    Badge(containerColor = Color(0xFF4F46E5)) {
                                        Text(productCount.toString(), fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = "Catalog")
                        }
                    },
                    label = { Text("Catalog", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("rail_tab_catalog"),
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color(0xFF4F46E5),
                        selectedTextColor = Color(0xFF4F46E5),
                        indicatorColor = Color(0xFFEEF2FF),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationRailItem(
                    selected = activeTab == AppNavTab.COMPARE,
                    onClick = { onTabSelected(AppNavTab.COMPARE) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (compareCount > 0) {
                                    Badge(containerColor = Color(0xFF4F46E5)) {
                                        Text(compareCount.toString(), fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.CompareArrows, contentDescription = "Compare")
                        }
                    },
                    label = { Text("Compare", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("rail_tab_compare"),
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color(0xFF4F46E5),
                        selectedTextColor = Color(0xFF4F46E5),
                        indicatorColor = Color(0xFFEEF2FF),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationRailItem(
                    selected = activeTab == AppNavTab.SMART_ADD,
                    onClick = { onTabSelected(AppNavTab.SMART_ADD) },
                    icon = {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Smart Add")
                    },
                    label = { Text("Smart Add", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("rail_tab_smart_add"),
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color(0xFF4F46E5),
                        selectedTextColor = Color(0xFF4F46E5),
                        indicatorColor = Color(0xFFEEF2FF),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationRailItem(
                    selected = activeTab == AppNavTab.REVIEWS,
                    onClick = { onTabSelected(AppNavTab.REVIEWS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (reviewCount > 0) {
                                    Badge(containerColor = Color(0xFF4F46E5)) {
                                        Text(reviewCount.toString(), fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.RateReview, contentDescription = "Reviews")
                        }
                    },
                    label = { Text("Reviews", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("rail_tab_reviews"),
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color(0xFF4F46E5),
                        selectedTextColor = Color(0xFF4F46E5),
                        indicatorColor = Color(0xFFEEF2FF),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationRailItem(
                    selected = activeTab == AppNavTab.DATA_BACKUP,
                    onClick = { onTabSelected(AppNavTab.DATA_BACKUP) },
                    icon = {
                        Icon(Icons.Default.Storage, contentDescription = "Backup")
                    },
                    label = { Text("Backup", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("rail_tab_backup"),
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color(0xFF4F46E5),
                        selectedTextColor = Color(0xFF4F46E5),
                        indicatorColor = Color(0xFFEEF2FF),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }

            // Bottom action: database status & sync
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .testTag("rail_sync_btn")
                ) {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = "Database Status",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
