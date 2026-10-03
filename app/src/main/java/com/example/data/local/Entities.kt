package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String,
    val tags: String, // Comma-separated or JSON list
    val overview: String,
    val picturesJson: String, // JSON array of Picture objects
    val createdTime: String = ""
)

@Entity(tableName = "specifications")
data class SpecificationEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val productTitle: String,
    val specName: String,
    val details: String,
    val unit: String? = null,
    val orderIndex: Int = 0,
    val picturesJson: String = "[]"
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val reviewNumber: Int,
    val title: String,
    val body: String,
    val authorName: String,
    val rating: Int = 5,
    val productIdsJson: String = "[]",
    val productTitlesJson: String = "[]",
    val picturesJson: String = "[]",
    val createdTime: String = ""
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String
)
