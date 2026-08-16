package com.aliemre.evenvanteri.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query(
        """
        SELECT l.id, l.name, l.icon, l.sortOrder,
               (SELECT COUNT(*) FROM items i
                 WHERE i.locationId = l.id AND i.deletedAt IS NULL) AS itemCount
        FROM locations l
        WHERE l.deletedAt IS NULL
        ORDER BY l.sortOrder, l.name
        """
    )
    fun observeWithCounts(): Flow<List<LocationWithCount>>

    @Query("SELECT * FROM locations WHERE deletedAt IS NULL ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id")
    fun observeById(id: String): Flow<LocationEntity?>

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(locations: List<LocationEntity>)

    @Upsert
    suspend fun upsert(location: LocationEntity)

    @Query("UPDATE locations SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    /**
     * Konum silinince içindeki ürünler de silinmiş sayılmalı. Foreign key CASCADE
     * burada işe yaramaz: satırları gerçekten silmiyoruz, işaretliyoruz.
     */
    @Query("UPDATE items SET deletedAt = :now, updatedAt = :now WHERE locationId = :id AND deletedAt IS NULL")
    suspend fun softDeleteItemsIn(id: String, now: Long)
}

@Dao
interface ItemDao {

    @Query(
        """
        SELECT * FROM items
        WHERE locationId = :locationId AND deletedAt IS NULL
        ORDER BY name COLLATE NOCASE
        """
    )
    fun observeByLocation(locationId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun byId(id: String): ItemEntity?

    /**
     * Son kullanma tarihi yaklaşanlar ve geçmiş olanlar birlikte, en acilden
     * başlayarak. Ana ekran bunu tek sorguda alıp "tarihi geçti" / "yaklaşıyor"
     * diye ikiye ayırıyor — iki ayrı sorgu atmaktansa.
     */
    @Query(
        """
        SELECT * FROM items
        WHERE deletedAt IS NULL
          AND expiryDate IS NOT NULL
          AND expiryDate <= :horizon
        ORDER BY expiryDate
        """
    )
    fun observeExpiringBefore(horizon: String): Flow<List<ItemEntity>>

    @Query(
        """
        SELECT * FROM items
        WHERE deletedAt IS NULL
          AND lowThreshold IS NOT NULL
          AND quantity <= lowThreshold
        ORDER BY name COLLATE NOCASE
        """
    )
    fun observeLowStock(): Flow<List<ItemEntity>>

    @Query(
        """
        SELECT * FROM items
        WHERE deletedAt IS NULL AND name LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE
        LIMIT 50
        """
    )
    fun search(query: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE deletedAt IS NULL AND barcode = :barcode LIMIT 1")
    suspend fun byBarcode(barcode: String): ItemEntity?

    @Upsert
    suspend fun upsert(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Query("UPDATE items SET quantity = :quantity, updatedAt = :now WHERE id = :id")
    suspend fun setQuantity(id: String, quantity: Double, now: Long)

    @Query("UPDATE items SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}
