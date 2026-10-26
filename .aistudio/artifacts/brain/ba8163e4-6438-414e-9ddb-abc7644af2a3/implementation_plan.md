# Compare Module Vertical Product & Specification Redesign

Redesign the Compare Module in `CompareScreen.kt` to match the layout and visual hierarchy shown in `IMG_0002_0aabfdf2.PNG` and `IMG_0003_8ac0ef22.PNG`, stacking both the selected product record cards and the specification items vertically with clear product labels, rich spec values, thumbnail lightboxes, and review actions.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following design choices were confirmed by the user in Phase 1:
> - **Product Selection Interaction**: Users can tap anywhere on the selected product card row or the unfold icon (`⇅`) to open the slot product picker modal.
> - **Specification Card Item Layout**: Inside each specification comparison card, products are stacked vertically one below the other with the product title in bold uppercase above its detailed spec value and thumbnail pictures.
> - **User Reviews Card Actions**: Displays a dashed "+ Write review" button for products without linked reviews, and an outlined "Read review" button for products with available reviews.

---

## 1. Overview & Core Concept

- **What It Delivers**:
  1. **Vertical Product Record Slots (`IMG_0002`)**: Directly under the tag/brand/product filter, `RECORD 1` and `RECORD 2` cards appear vertically stacked. Each card features the record index, quick actions (`Edit product` text button and `✕` remove button), a 48dp product thumbnail, product name, subtitle with green checkmark status (`✓ Selected / [Brand] / [N] reviews`), and an unfold icon (`⇅`).
  2. **Direct Comparison Transition**: Replaces the redundant horizontal carousel of product cards with a clean, focused "SIDE BY SIDE Complete comparison" summary header displaying the count badges (`[2 selected]` and `[28 differences]`) and subtitle (`30 fields shown, 28 differ`).
  3. **Vertical Specification Comparison Cards (`IMG_0003`)**:
     - **Header Row**: Spec name/icon/unit badge on the left, `# Number ON/OFF` toggle pill, and `DIFFERENT` badge. For numeric specs with Number ON, renders `Higher` / `Lower` preference buttons.
     - **Card Background**: Yellow tint (`#FEFCE8` with border `#FDE047`) when differences exist, clean white when identical.
     - **Stacked Product Specifications**: Each product has its uppercase name header (`ROG XBOX ALLY X`), followed by its formatted HTML details, unit badge, and attached specification pictures with a `Full screen` button.
  4. **Vertical User Reviews Card (`IMG_0003`)**: Displays `User reviews` with a `Different` badge, followed by each product stacked with review availability (`No linked reviews` vs `2 reviews available`) and targeted action buttons (`+ Write review` vs `Read review`).

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Selecting / Changing Products**:
   - Tapping an empty slot or tapping on an existing product's card opens the full-screen record picker modal to search and select products by name, tag, or brand.
   - Tapping `Edit product` opens the product edit dialog directly from the record header.
2. **Comparing Across Specifications**:
   - As the user scrolls vertically through the specifications, differences are prominently highlighted with yellow cards and `DIFFERENT` badges.
   - For each spec, the user sees Product 1's full value and pictures directly followed by Product 2's full value and pictures.
   - Tapping the `Full screen` button or image thumbnail opens the full-screen lightbox.
3. **Interactive Number / Text Toggle**:
   - Tapping `# Number ON / OFF` switches between numeric bar/best evaluation and raw text/HTML view.
   - In numeric mode, tapping `Higher` or `Lower` dynamically highlights the winning product value with the `🏆 Best` badge.

### Visual Styling Tokens
- **Different Card Container**: `Color(0xFFFEFCE8)` (warm light yellow), Border `Color(0xFFFDE047)`.
- **Identical Card Container**: `Color.White`, Border `Color(0xFFE2E8F0)`.
- **Different Badge**: `Color(0xFFFACC15)` background with `Color(0xFF451A03)` text.
- **Product Name Header in Spec**: `Color(0xFF64748B)`, `11.sp`, `FontWeight.ExtraBold`, letter spacing `0.5.sp`.
- **Review Buttons**:
  - Empty: Outlined dashed border `Color(0xFF6366F1)`, text `Color(0xFF4F46E5)`.
  - Available: Outlined solid border `Color(0xFFCBD5E1)`, text `Color(0xFF334155)`.

---

## 3. Key Product Decisions & Trade-Offs

- **Removing the Horizontal Product Carousel**:
  - *Chosen Approach*: Omit the horizontal scrolling 260dp cards under the comparison header.
  - *Why*: In mobile viewport comparisons, having both vertical record selection cards at the top AND a horizontal card carousel created clutter and forced users to scroll past duplicate information before reaching the actual specification comparison.
- **Vertical Spec Layout vs Side-by-Side Columns**:
  - *Chosen Approach*: Stack products vertically inside each spec card (`Product 1 name -> details -> photos`, then `Product 2 name -> details -> photos`).
  - *Why*: Side-by-side columns on mobile screens severely truncate long specification text (e.g. dimensions, multi-line notes) and squash thumbnail images. The vertical stack ensures full readability and accommodates rich HTML formatted text cleanly.

---

## 4. Technical Architecture & Data Strategy

### Layout & Component Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│ LazyColumn (CompareScreen)                                  │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ TagBrandProductFilter Card                              │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ RECORD 1 Card: Title, Edit, Delete, Product Row (⇅)     │ │
│ └─────────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ RECORD 2 Card: Title, Edit, Delete, Product Row (⇅)     │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Header: SIDE BY SIDE, Complete comparison, Badges       │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Brand Comparison Card (Vertical products)               │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ User Reviews Card (Vertical products with review btns)  │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Specification Cards (distinctSpecNames)                 │ │
│ │  - Header: Name, Unit, [Number ON/OFF], [DIFFERENT]     │ │
│ │  - Product 1: Uppercase name, Formatted HTML, Pictures  │ │
│ │  - Divider                                              │ │
│ │  - Product 2: Uppercase name, Formatted HTML, Pictures  │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Key Implementation Steps in `CompareScreen.kt`
1. **Update `RecordSlotCardView`**:
   - Add `onEditProduct: (ProductItem) -> Unit` parameter.
   - Render header row with `RECORD ${index + 1}` on left, and `✎ Edit product` text button + `✕` close button on right.
   - Ensure the inner product row is clickable (`Modifier.clickable { onSelectSlot() }`) with `Icons.Default.UnfoldMore` on the right.
2. **Update Main Content Items in `CompareScreen.kt`**:
   - Render `recordSlots` vertically using `RecordSlotCardView`.
   - Remove the redundant horizontal `LazyRow` of 260dp product cards.
   - Update `SIDE BY SIDE` header to display count chips matching `IMG_0002`.
3. **Update Brand & Reviews Cards**:
   - Brand card: Stack each product name in uppercase above the brand text.
   - User reviews card: Stack each product name above review count with `+ Write review` or `Read review`.
4. **Update Specification Cards (`distinctSpecNames`)**:
   - Redesign card header into a clean single or dual row matching `IMG_0003`.
   - Inside each card, loop through `activeProducts` vertically:
     - Text for uppercase product name (`p.name.uppercase()`).
     - Numeric value or `FormattedSpecHtml` for the details.
     - Specification pictures with `Full screen` button overlay and filename.
     - Subtle divider between products.
5. **Verification**:
   - Run `compile_applet` to ensure zero compilation or type errors.
   - Run `gradle :app:testDebugUnitTest` to guarantee all existing tests pass.
