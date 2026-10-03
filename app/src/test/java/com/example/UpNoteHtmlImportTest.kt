package com.example

import com.example.data.importer.ImportedReviewDraft
import com.example.model.PictureItem
import com.example.model.ProductItem
import org.junit.Assert.*
import org.junit.Test

class UpNoteHtmlImportTest {

    @Test
    fun testImportedReviewDraft_toReviewItem_mapping() {
        val products = listOf(
            ProductItem(
                id = "prod_1",
                name = "iPhone 15 Pro",
                brand = "Apple",
                tags = listOf("flagship"),
                overview = "Great camera",
                pictures = emptyList(),
                createdTime = "123456"
            ),
            ProductItem(
                id = "prod_2",
                name = "Galaxy S24 Ultra",
                brand = "Samsung",
                tags = listOf("flagship", "stylus"),
                overview = "Ultra display",
                pictures = emptyList(),
                createdTime = "123457"
            )
        )

        val pictures = mutableListOf(
            PictureItem(id = "pic_1", name = "camera_test.jpg", url = "file:///data/user/0/com.example/files/review_images/upnote_img_1.jpg")
        )

        val draft = ImportedReviewDraft(
            title = "My iPhone 15 Pro In-Depth Review",
            bodyHtml = "<h1>Title</h1><p>This is a detailed review with an image.</p><p><img src=\"file:///data/user/0/com.example/files/review_images/upnote_img_1.jpg\" /></p>",
            pictures = pictures,
            sourcePath = "note.html",
            sourceFolderName = "iPhone 15 Pro Review",
            assignedProductId = "prod_1",
            rating = 5,
            authorName = "Alex",
            wordCount = 12,
            hasImages = true
        )

        val review = draft.toReviewItem(products, nextReviewNumber = 4)

        assertEquals(4, review.reviewNumber)
        assertEquals("My iPhone 15 Pro In-Depth Review", review.title)
        assertEquals("Alex", review.authorName)
        assertEquals(5, review.rating)
        assertEquals(listOf("prod_1"), review.productIds)
        assertEquals(listOf("iPhone 15 Pro"), review.productTitles)
        assertEquals(1, review.pictures.size)
        assertEquals("camera_test.jpg", review.pictures[0].name)
    }

    @Test
    fun testDraftWithoutProduct_createsStandaloneReview() {
        val draft = ImportedReviewDraft(
            title = "General Thoughts on Microphones",
            bodyHtml = "<p>Interesting voice recorder comparisons.</p>",
            pictures = mutableListOf(),
            sourcePath = "note.html",
            sourceFolderName = "General",
            assignedProductId = null,
            rating = 4,
            authorName = "UpNote Reviewer",
            wordCount = 5,
            hasImages = false
        )

        val review = draft.toReviewItem(emptyList(), nextReviewNumber = 1)
        assertEquals(1, review.reviewNumber)
        assertTrue(review.productIds.isEmpty())
        assertTrue(review.productTitles.isEmpty())
        assertEquals(4, review.rating)
    }
}
