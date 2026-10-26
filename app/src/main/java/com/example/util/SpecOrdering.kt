package com.example.util

import com.example.model.ProductItem
import com.example.model.SpecificationItem
import java.util.UUID

/**
 * Computes the unified specification order across products according to slot hierarchy:
 * 1. Product in Slot 1: takes all specifications in Product 1's defined order (1 to end).
 * 2. Product in Slot 2: appends any remaining specifications not already present in (1),
 *    in Product 2's defined order (1 to end).
 * 3. Product in Slot 3, etc.: appends any specifications not yet present, in Product 3's order.
 *
 * Example:
 * Product 1: A (order 1), C (order 2)
 * Product 2: D (order 1), A (order 2)
 * Product 3: B (order 1), E (order 2), A (order 3), D (order 4)
 * -> Result: A (1), C (2), D (3), B (4), E (5)
 */
fun computeHierarchicalSpecOrder(
    orderedProducts: List<ProductItem?>,
    allSpecifications: List<SpecificationItem>
): List<String> {
    val result = mutableListOf<String>()
    val seenLower = mutableSetOf<String>()

    orderedProducts.forEach { prod ->
        if (prod == null) return@forEach

        // Filter specifications for this product and sort by order
        val pSpecs = allSpecifications
            .filter { s ->
                (s.productId.isNotBlank() && s.productId == prod.id) ||
                (s.productTitle.isNotBlank() && s.productTitle.trim().equals(prod.name.trim(), ignoreCase = true))
            }
            .sortedBy { it.orderIndex }

        pSpecs.forEach { s ->
            val trimmed = s.specName.trim()
            if (trimmed.isEmpty()) return@forEach
            val lower = trimmed.lowercase()
            if (!seenLower.contains(lower)) {
                seenLower.add(lower)
                result.add(trimmed)
            }
        }
    }

    return result
}

/**
 * Builds a list of blank specifications for creating a new Smart Product,
 * assigning sequential order numbers (1 to N) following the hierarchical slot sequence,
 * and inheriting any unit or isNumeric configuration from existing specifications.
 */
fun buildInheritedBlankSpecs(
    orderedSpecNames: List<String>,
    allSpecifications: List<SpecificationItem>,
    targetProductId: String = "",
    targetProductTitle: String = ""
): List<SpecificationItem> {
    return orderedSpecNames.mapIndexed { index, specName ->
        // Find first existing spec for this attribute to inherit unit and isNumeric settings
        val existingRef = allSpecifications.find { s ->
            s.specName.trim().equals(specName.trim(), ignoreCase = true) &&
            (!s.unit.isNullOrBlank() || s.isNumeric != null)
        }

        SpecificationItem(
            id = UUID.randomUUID().toString(),
            productId = targetProductId,
            productTitle = targetProductTitle,
            specName = specName,
            details = "",
            unit = existingRef?.unit?.trim() ?: "",
            isNumeric = existingRef?.isNumeric,
            orderIndex = index + 1
        )
    }
}
