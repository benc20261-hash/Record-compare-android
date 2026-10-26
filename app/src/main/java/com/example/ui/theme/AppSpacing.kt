package com.example.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * AppSpacing: Central Design Language source of truth for all layout dimensions,
 * spacing tokens, content paddings, and background colors.
 *
 * DESIGN PATTERN RULES TO PREVENT UNWANTED WHITE SPACE & LAYOUT BUGS:
 *
 * 1. ROW CONSTRAINTS & WEIGHT RULES (CRITICAL):
 *    - In any horizontal `Row` containing text/titles and badges/buttons:
 *      ALWAYS give the primary text container `Modifier.weight(1f)` or `Modifier.weight(1f, fill = false)`.
 *    - NEVER place unconstrained text columns alongside badges on the far right without weights;
 *      otherwise, narrow screens will compress the badge into a 10dp width, wrapping characters
 *      letter-by-letter vertically into a 150dp tall column and creating runaway white space!
 *    - Badges MUST use [AppBadge] or set `maxLines = 1` and `softWrap = false`.
 *
 * 2. ACTION STACKING IN SUMMARY CARDS:
 *    - Summary cards (e.g. "X of Y slots filled") must place secondary actions (e.g. "Reset") in
 *      the header row and prominent navigation actions (e.g. "Go to matrix") on a dedicated full-width
 *      row below using [AppSummaryCard].
 *    - NEVER place multi-word navigation buttons inline with long stats strings on mobile.
 *
 * 3. SCROLLABLE CONTAINER PADDING:
 *    - NEVER use `Modifier.padding()` on `LazyColumn` or `LazyVerticalGrid` to set content margins;
 *      ALWAYS use `contentPadding = AppSpacing.ScreenContentPaddingScrollable`.
 *
 * 4. NO ARBITRARY TRAILING SPACERS:
 *    - NEVER append arbitrary trailing `Spacer(height = 30.dp)` or `Spacer(height = 40.dp)` inside list items;
 *      rely on `contentPadding` bottom clearance instead.
 *
 * 5. BACKGROUND HIERARCHY:
 *    - ALWAYS set container backgrounds to [AppColors.CanvasBackground] (#F8FAFC).
 *    - Cards and elevated surfaces MUST use [AppColors.CardBackground] (#FFFFFF) with [AppColors.CardBorder].
 *
 * 6. STATUS BADGE PLACEMENT IN STANDALONE CARDS & HEADERS:
 *    - Status and readiness badges (e.g. '✓ Ready to export') MUST be placed on the far right of the
 *      card title Row (using `horizontalArrangement = Arrangement.SpaceBetween` and `weight(1f, fill = false)`
 *      on the title column), NEVER stacked below the subtitle with spacers, which causes unwanted vertical gaps and white space.
 *
 * 7. SINGLE REACTIVE SOURCE OF TRUTH FOR SLOTS:
 *    - Slot states (e.g. `productSlots`) must directly drive selected product lists, comparison matrices,
 *      and reorders synchronously without intermediate `LaunchedEffect` or decoupled state lists.
 */
object AppSpacing {
    // Standard 4dp / 8dp Spacing Scale
    val none: Dp = 0.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val section: Dp = 28.dp

    // Semantic Spacing
    val screenMargin: Dp = 16.dp
    val cardPadding: Dp = 14.dp
    val cardGap: Dp = 10.dp
    val listGap: Dp = 10.dp
    val itemGap: Dp = 8.dp

    // Corner Radii
    val cardCornerRadius: Dp = 16.dp
    val cardCornerRadiusLarge: Dp = 20.dp
    val buttonCornerRadius: Dp = 12.dp
    val pillCornerRadius: Dp = 20.dp
    val badgeCornerRadius: Dp = 8.dp

    // Standard List Content Paddings
    val ScreenContentPadding = PaddingValues(
        start = screenMargin,
        top = 10.dp,
        end = screenMargin,
        bottom = 16.dp
    )

    val ScreenContentPaddingScrollable = PaddingValues(
        start = screenMargin,
        top = 10.dp,
        end = screenMargin,
        bottom = 24.dp
    )

    // Clearance when a floating bottom tray (e.g. ComparisonTray) is visible
    val ScreenWithFloatingTrayPadding = PaddingValues(
        start = screenMargin,
        top = 10.dp,
        end = screenMargin,
        bottom = 120.dp
    )

    // Modal & Dialog Internal Content Padding
    val ModalContentPadding = PaddingValues(
        horizontal = 16.dp,
        vertical = 12.dp
    )
}

/**
 * Central color palette tokens ensuring consistent background hierarchy
 */
object AppColors {
    val CanvasBackground = Color(0xFFF8FAFC) // Slate-50: Main app canvas background
    val CardBackground = Color.White        // Pure white: Elevated card and header surface
    val CardBorder = Color(0xFFE2E8F0)        // Slate-200: Subtle card border
    val CardBorderHover = Color(0xFFCBD5E1)   // Slate-300: Active/hovered border
    val DividerColor = Color(0xFFF1F5F9)      // Slate-100: Horizontal dividers
    val SubtitleText = Color(0xFF64748B)      // Slate-500: Subtitles and captions
    val PlaceholderText = Color(0xFF94A3B8)   // Slate-400: Inputs & placeholders
    val HeadingText = Color(0xFF0F172A)       // Slate-900: Primary titles and headers
    val PrimaryBrand = Color(0xFF4F46E5)      // Indigo-600: Accent & primary actions
    val PrimaryBrandLight = Color(0xFFEEF2FF) // Indigo-50: Badge & active pill backgrounds
}
