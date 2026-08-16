package com.aliemre.evenvanteri.data

import com.aliemre.evenvanteri.data.local.ItemDao
import com.aliemre.evenvanteri.data.local.ItemEntity
import com.aliemre.evenvanteri.data.local.LOCAL_HOUSEHOLD_ID
import com.aliemre.evenvanteri.data.local.LocationDao
import com.aliemre.evenvanteri.data.local.LocationEntity
import com.aliemre.evenvanteri.data.local.LocationWithCount
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

/** SKT uyarısının kaç gün önceden başlayacağı. */
const val EXPIRY_WARNING_DAYS = 7L

class InventoryRepository(
    private val locationDao: LocationDao,
    private val itemDao: ItemDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
) {

    // ---- Okuma ----

    fun observeLocations(): Flow<List<LocationWithCount>> = locationDao.observeWithCounts()

    fun observeAllLocations(): Flow<List<LocationEntity>> = locationDao.observeAll()

    fun observeLocation(id: String): Flow<LocationEntity?> = locationDao.observeById(id)

    fun observeItemsIn(locationId: String): Flow<List<ItemEntity>> =
        itemDao.observeByLocation(locationId)

    /**
     * Tarihi geçmiş ve yaklaşan ürünler tek akışta. Ayrımı çağıran yapıyor;
     * ikisi de aynı sorgudan geldiği için ana ekran tek abonelikle idare ediyor.
     */
    fun observeExpiring(): Flow<List<ItemEntity>> =
        itemDao.observeExpiringBefore(today().plusDays(EXPIRY_WARNING_DAYS).toString())

    fun observeLowStock(): Flow<List<ItemEntity>> = itemDao.observeLowStock()

    fun search(query: String): Flow<List<ItemEntity>> = itemDao.search(query)

    suspend fun item(id: String): ItemEntity? = itemDao.byId(id)

    suspend fun itemByBarcode(barcode: String): ItemEntity? = itemDao.byBarcode(barcode)

    // ---- Yazma: ürünler ----

    suspend fun saveItem(
        id: String?,
        locationId: String,
        name: String,
        quantity: Double,
        unit: String,
        expiryDate: LocalDate?,
        lowThreshold: Double?,
        barcode: String?,
        note: String,
    ) {
        val timestamp = now()
        val existing = id?.let { itemDao.byId(it) }
        itemDao.upsert(
            ItemEntity(
                id = id ?: UUID.randomUUID().toString(),
                householdId = existing?.householdId ?: LOCAL_HOUSEHOLD_ID,
                locationId = locationId,
                name = name.trim(),
                quantity = quantity,
                unit = unit,
                expiryDate = expiryDate?.toString(),
                lowThreshold = lowThreshold,
                barcode = barcode?.takeIf { it.isNotBlank() },
                note = note.trim(),
                createdAt = existing?.createdAt ?: timestamp,
                updatedAt = timestamp,
                deletedAt = null,
            ),
        )
    }

    /**
     * Miktarı adım adım değiştirir. Negatife düşürmez: bir dolapta -1 paket
     * bulgur olamaz ve bunu kullanıcıya hata olarak göstermek gereksiz.
     */
    suspend fun adjustQuantity(id: String, delta: Double) {
        val item = itemDao.byId(id) ?: return
        val next = (item.quantity + delta).coerceAtLeast(0.0)
        itemDao.setQuantity(id, next, now())
    }

    suspend fun deleteItem(id: String) = itemDao.softDelete(id, now())

    // ---- Yazma: konumlar ----

    suspend fun saveLocation(id: String?, name: String, icon: String, sortOrder: Int) {
        val timestamp = now()
        locationDao.upsert(
            LocationEntity(
                id = id ?: UUID.randomUUID().toString(),
                householdId = LOCAL_HOUSEHOLD_ID,
                name = name.trim(),
                icon = icon,
                sortOrder = sortOrder,
                createdAt = timestamp,
                updatedAt = timestamp,
                deletedAt = null,
            ),
        )
    }

    /** Konumu ve içindeki ürünleri birlikte siler; ürünler sahipsiz kalmamalı. */
    suspend fun deleteLocation(id: String) {
        val timestamp = now()
        locationDao.softDeleteItemsIn(id, timestamp)
        locationDao.softDelete(id, timestamp)
    }

    // ---- İlk açılış ----

    /**
     * Uygulama boş bir ekranla karşılamasın diye tipik ev konumlarını hazır kurar.
     * Kullanıcı bunları ayarlardan silebilir veya yeniden adlandırabilir.
     */
    suspend fun seedDefaultLocationsIfEmpty() {
        if (locationDao.count() > 0) return
        val timestamp = now()
        locationDao.upsert(
            DEFAULT_LOCATIONS.mapIndexed { index, (name, icon) ->
                LocationEntity(
                    id = UUID.randomUUID().toString(),
                    householdId = LOCAL_HOUSEHOLD_ID,
                    name = name,
                    icon = icon,
                    sortOrder = index + 1,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    deletedAt = null,
                )
            },
        )
    }

    companion object {
        private val DEFAULT_LOCATIONS = listOf(
            "Buzdolabı" to "fridge",
            "Buzluk" to "freezer",
            "Bakliyat Dolabı" to "grain",
            "Baharat Dolabı" to "spice",
            "Takviye Dolabı" to "pill",
            "Alt Dolap" to "cabinet",
            "Yüklük" to "closet",
        )
    }
}
