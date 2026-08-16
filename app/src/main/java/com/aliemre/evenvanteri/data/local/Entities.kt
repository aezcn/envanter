package com.aliemre.evenvanteri.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ev henüz Supabase'e bağlanmadan önce yerel kayıtların taşıdığı geçici ev kimliği.
 * Kullanıcı giriş yapıp bir eve katıldığında bu satırlar gerçek householdId ile
 * güncellenir (Faz 3), böylece uygulamayı hesapsız kullanmaya başlamak mümkün olur.
 */
const val LOCAL_HOUSEHOLD_ID = "00000000-0000-0000-0000-000000000000"

/**
 * Zaman damgaları epoch milisaniye; tarihler ISO "yyyy-MM-dd" metni.
 *
 * Tarihin metin olması bilinçli: ISO biçimi sözlük sırasıyla kronolojik sırayla
 * aynı olduğu için "önümüzdeki 7 gün" sorgusu düz bir BETWEEN ile yazılabiliyor,
 * ayrıca Postgres'teki `date` sütununa dönüşümsüz oturuyor.
 */
@Entity(
    tableName = "locations",
    indices = [Index("householdId"), Index("sortOrder")],
)
data class LocationEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val name: String,
    val icon: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    /** Yumuşak silme: null değilse silinmiş sayılır. Senkronizasyon için şart. */
    val deletedAt: Long?,
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("householdId"), Index("locationId"), Index("expiryDate")],
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val locationId: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    /** ISO "yyyy-MM-dd" ya da null — son kullanma tarihi isteğe bağlı. */
    val expiryDate: String?,
    /** Bu miktarın altına düşünce "azaldı" sayılır; null ise takip edilmez. */
    val lowThreshold: Double?,
    val barcode: String?,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

/** Ana ekrandaki dolap kutucuğu: konum + içindeki ürün sayısı. */
data class LocationWithCount(
    val id: String,
    val name: String,
    val icon: String,
    val sortOrder: Int,
    val itemCount: Int,
)
