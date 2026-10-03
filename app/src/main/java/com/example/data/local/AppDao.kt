package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Products
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: String)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    // Specifications
    @Query("SELECT * FROM specifications ORDER BY orderIndex ASC, specName ASC")
    fun getAllSpecifications(): Flow<List<SpecificationEntity>>

    @Query("SELECT * FROM specifications WHERE productId = :productId OR productTitle = :productTitle ORDER BY orderIndex ASC, specName ASC")
    fun getSpecsForProduct(productId: String, productTitle: String): Flow<List<SpecificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecification(spec: SpecificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecifications(specs: List<SpecificationEntity>)

    @Query("DELETE FROM specifications WHERE id = :id")
    suspend fun deleteSpecification(id: String)

    @Query("DELETE FROM specifications WHERE productId = :productId")
    suspend fun deleteSpecsForProduct(productId: String)

    @Query("DELETE FROM specifications")
    suspend fun clearSpecifications()

    // Reviews
    @Query("SELECT * FROM reviews ORDER BY reviewNumber DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<ReviewEntity>)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteReview(id: String)

    @Query("DELETE FROM reviews")
    suspend fun clearReviews()

    // Users
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}
