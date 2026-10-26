package com.example

import com.example.model.ProductItem
import com.example.model.SpecificationItem
import com.example.ui.components.getSchemeDisplayName
import com.example.ui.util.UriHelper
import com.example.util.buildInheritedBlankSpecs
import com.example.util.computeHierarchicalSpecOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecOrderingTest {

    @Test
    fun testComputeHierarchicalSpecOrder() {
        val prod1 = ProductItem(id = "p1", name = "Product 1", brand = "Brand A")
        val prod2 = ProductItem(id = "p2", name = "Product 2", brand = "Brand B")
        val prod3 = ProductItem(id = "p3", name = "Product 3", brand = "Brand C")

        val specs = listOf(
            // Product 1: A (order 1), C (order 2)
            SpecificationItem(id = "s1", productId = "p1", productTitle = "Product 1", specName = "Weight", details = "100g", orderIndex = 1),
            SpecificationItem(id = "s2", productId = "p1", productTitle = "Product 1", specName = "Battery", details = "4000mAh", orderIndex = 2),

            // Product 2: D (order 1), A (order 2)
            SpecificationItem(id = "s3", productId = "p2", productTitle = "Product 2", specName = "Screen", details = "6.1 inch", orderIndex = 1),
            SpecificationItem(id = "s4", productId = "p2", productTitle = "Product 2", specName = "Weight", details = "120g", orderIndex = 2),

            // Product 3: B (order 1), E (order 2), A (order 3), D (order 4)
            SpecificationItem(id = "s5", productId = "p3", productTitle = "Product 3", specName = "Processor", details = "Snapdragon", orderIndex = 1),
            SpecificationItem(id = "s6", productId = "p3", productTitle = "Product 3", specName = "Camera", details = "50MP", orderIndex = 2),
            SpecificationItem(id = "s7", productId = "p3", productTitle = "Product 3", specName = "Weight", details = "110g", orderIndex = 3),
            SpecificationItem(id = "s8", productId = "p3", productTitle = "Product 3", specName = "Screen", details = "6.7 inch", orderIndex = 4)
        )

        val orderedSpecNames = computeHierarchicalSpecOrder(listOf(prod1, prod2, prod3), specs)

        // Slot 1 specs: Weight, Battery
        // Slot 2 specs appended: Screen
        // Slot 3 specs appended: Processor, Camera
        assertEquals(listOf("Weight", "Battery", "Screen", "Processor", "Camera"), orderedSpecNames)
    }

    @Test
    fun testBuildInheritedBlankSpecs() {
        val existingSpecs = listOf(
            SpecificationItem(
                id = "ref1",
                productId = "old",
                productTitle = "Old",
                specName = "Weight",
                details = "50g",
                unit = "g",
                isNumeric = true
            ),
            SpecificationItem(
                id = "ref2",
                productId = "old",
                productTitle = "Old",
                specName = "Color",
                details = "Black",
                unit = null,
                isNumeric = false
            )
        )

        val blankSpecs = buildInheritedBlankSpecs(
            orderedSpecNames = listOf("Weight", "Color", "NewFeature"),
            allSpecifications = existingSpecs,
            targetProductId = "new_prod",
            targetProductTitle = "New Product"
        )

        assertEquals(3, blankSpecs.size)

        // Spec 1: Weight inherits unit "g" and isNumeric true, order 1
        assertEquals("Weight", blankSpecs[0].specName)
        assertEquals("g", blankSpecs[0].unit)
        assertEquals(true, blankSpecs[0].isNumeric)
        assertEquals(1, blankSpecs[0].orderIndex)

        // Spec 2: Color inherits isNumeric false, order 2
        assertEquals("Color", blankSpecs[1].specName)
        assertEquals(false, blankSpecs[1].isNumeric)
        assertEquals(2, blankSpecs[1].orderIndex)

        // Spec 3: NewFeature has defaults, order 3
        assertEquals("NewFeature", blankSpecs[2].specName)
        assertEquals("", blankSpecs[2].unit)
        assertEquals(null, blankSpecs[2].isNumeric)
        assertEquals(3, blankSpecs[2].orderIndex)
    }

    @Test
    fun testUriHelperSchemeRecognition() {
        assertTrue(UriHelper.isAppSchemeUrl("upnote://x-callback-url/opennote?notedid=123"))
        assertTrue(UriHelper.isAppSchemeUrl("obsidian://open?vault=test"))
        assertTrue(UriHelper.isAppSchemeUrl("marginnote4app://note/abc"))
        assertTrue(UriHelper.isAppSchemeUrl("notion://page/123"))
        assertTrue(UriHelper.isAppSchemeUrl("craftdocs://open?space=1"))

        assertFalse(UriHelper.isAppSchemeUrl("https://google.com"))
        assertFalse(UriHelper.isAppSchemeUrl("http://example.com"))
        assertFalse(UriHelper.isAppSchemeUrl("mailto:test@example.com"))
        assertFalse(UriHelper.isAppSchemeUrl("tel:+1234567890"))
        assertFalse(UriHelper.isAppSchemeUrl(""))
    }

    @Test
    fun testGetSchemeDisplayName() {
        assertEquals("📝 Open in UpNote", getSchemeDisplayName("upnote://x-callback-url/opennote?notedid=xyz"))
        assertEquals("💎 Open in Obsidian", getSchemeDisplayName("obsidian://open?vault=test"))
        assertEquals("📓 Open in Notion", getSchemeDisplayName("notion://page/123"))
        assertEquals("📑 Open in MarginNote", getSchemeDisplayName("marginnote4app://note/abc"))
    }
}
