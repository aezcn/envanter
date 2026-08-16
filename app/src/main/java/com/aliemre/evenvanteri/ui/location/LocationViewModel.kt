package com.aliemre.evenvanteri.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliemre.evenvanteri.data.InventoryRepository
import com.aliemre.evenvanteri.data.local.ItemEntity
import com.aliemre.evenvanteri.ui.parseDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ItemSort(val label: String) {
    NAME("İsme göre"),
    EXPIRY("Son kullanma tarihine göre"),
    RECENT("Son eklenene göre"),
}

data class LocationUiState(
    val locationName: String = "",
    val items: List<ItemEntity> = emptyList(),
    val sort: ItemSort = ItemSort.NAME,
)

class LocationViewModel(
    private val repository: InventoryRepository,
    private val locationId: String,
) : ViewModel() {

    private val sort = MutableStateFlow(ItemSort.NAME)

    val uiState: StateFlow<LocationUiState> =
        combine(
            repository.observeLocation(locationId),
            repository.observeItemsIn(locationId),
            sort,
        ) { location, items, sortOrder ->
            LocationUiState(
                locationName = location?.name ?: "Dolap",
                items = items.sortedWith(comparatorFor(sortOrder)),
                sort = sortOrder,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LocationUiState(),
        )

    fun onSortChange(value: ItemSort) {
        sort.value = value
    }

    fun adjustQuantity(itemId: String, delta: Double) {
        viewModelScope.launch { repository.adjustQuantity(itemId, delta) }
    }

    /**
     * Tarihe göre sıralarken tarihi olmayan ürünler sona gider. Aksi halde
     * boş tarihler listenin başını doldurup asıl merak edilen "en yakında
     * bozulacak" ürünleri aşağı iterdi.
     */
    private fun comparatorFor(sortOrder: ItemSort): Comparator<ItemEntity> = when (sortOrder) {
        ItemSort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        ItemSort.RECENT -> compareByDescending { it.createdAt }
        ItemSort.EXPIRY -> compareBy<ItemEntity> { parseDate(it.expiryDate) == null }
            .thenBy { it.expiryDate ?: "" }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
}
