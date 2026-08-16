package com.aliemre.evenvanteri.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliemre.evenvanteri.data.InventoryRepository
import com.aliemre.evenvanteri.data.local.ItemEntity
import com.aliemre.evenvanteri.data.local.LocationWithCount
import com.aliemre.evenvanteri.ui.ExpiryStatus
import com.aliemre.evenvanteri.ui.expiryStatus
import com.aliemre.evenvanteri.ui.parseDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val locations: List<LocationWithCount> = emptyList(),
    val expired: List<ItemEntity> = emptyList(),
    val expiringSoon: List<ItemEntity> = emptyList(),
    val lowStock: List<ItemEntity> = emptyList(),
    val locationNames: Map<String, String> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(
    private val repository: InventoryRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.observeLocations(),
            repository.observeExpiring(),
            repository.observeLowStock(),
            repository.observeAllLocations(),
        ) { locations, expiring, lowStock, allLocations ->
            // Tek sorgudan gelen "yaklaşanlar" burada ikiye ayrılıyor; kullanıcı
            // için "tarihi geçti" ile "3 gün kaldı" bambaşka aciliyetler.
            val (expired, soon) = expiring.partition {
                parseDate(it.expiryDate)?.let { date ->
                    expiryStatus(date) == ExpiryStatus.EXPIRED
                } ?: false
            }
            HomeUiState(
                locations = locations,
                expired = expired,
                expiringSoon = soon,
                lowStock = lowStock,
                locationNames = allLocations.associate { it.id to it.name },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    /**
     * Arama her tuş vuruşunda veritabanına gitmesin diye kısa bir gecikme var;
     * boş sorgu hiç sorgulanmıyor.
     */
    val searchResults: StateFlow<List<ItemEntity>> =
        _query
            .debounce(200)
            .flatMapLatest { q ->
                if (q.isBlank()) flowOf(emptyList()) else repository.search(q.trim())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun adjustQuantity(itemId: String, delta: Double) {
        viewModelScope.launch { repository.adjustQuantity(itemId, delta) }
    }
}
