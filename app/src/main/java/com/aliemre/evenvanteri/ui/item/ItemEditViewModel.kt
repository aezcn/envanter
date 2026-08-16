package com.aliemre.evenvanteri.ui.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliemre.evenvanteri.data.InventoryRepository
import com.aliemre.evenvanteri.data.local.LocationEntity
import com.aliemre.evenvanteri.ui.parseDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ItemEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val quantity: String = "1",
    val unit: String = "adet",
    val locationId: String = "",
    val expiryDate: LocalDate? = null,
    val lowThreshold: String = "",
    val note: String = "",
    val barcode: String? = null,
    val locations: List<LocationEntity> = emptyList(),
    val saved: Boolean = false,
) {
    /** Kaydet düğmesi ancak anlamlı bir kayıt oluşabiliyorsa etkin. */
    val canSave: Boolean
        get() = name.isNotBlank() &&
            locationId.isNotBlank() &&
            quantity.toTurkishDoubleOrNull() != null
}

/**
 * Türkçe klavyede ondalık ayırıcı virgül; kullanıcı "1,5" yazdığında bunu
 * geçersiz sayıp reddetmek yerine kabul ediyoruz.
 */
fun String.toTurkishDoubleOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()

class ItemEditViewModel(
    private val repository: InventoryRepository,
    private val itemId: String?,
    private val presetLocationId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(ItemEditUiState())
    val state: StateFlow<ItemEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val locations = repository.observeAllLocations().first()
            val existing = itemId?.let { repository.item(it) }

            _state.value = if (existing != null) {
                ItemEditUiState(
                    loading = false,
                    isNew = false,
                    name = existing.name,
                    quantity = formatInput(existing.quantity),
                    unit = existing.unit,
                    locationId = existing.locationId,
                    expiryDate = parseDate(existing.expiryDate),
                    lowThreshold = existing.lowThreshold?.let { formatInput(it) } ?: "",
                    note = existing.note,
                    barcode = existing.barcode,
                    locations = locations,
                )
            } else {
                ItemEditUiState(
                    loading = false,
                    isNew = true,
                    locationId = presetLocationId
                        ?: locations.firstOrNull()?.id
                        ?: "",
                    locations = locations,
                )
            }
        }
    }

    fun onName(value: String) = _state.update { it.copy(name = value) }
    fun onQuantity(value: String) = _state.update { it.copy(quantity = value) }
    fun onUnit(value: String) = _state.update { it.copy(unit = value) }
    fun onLocation(value: String) = _state.update { it.copy(locationId = value) }
    fun onExpiry(value: LocalDate?) = _state.update { it.copy(expiryDate = value) }
    fun onLowThreshold(value: String) = _state.update { it.copy(lowThreshold = value) }
    fun onNote(value: String) = _state.update { it.copy(note = value) }

    fun save() {
        val current = _state.value
        if (!current.canSave) return

        viewModelScope.launch {
            repository.saveItem(
                id = itemId,
                locationId = current.locationId,
                name = current.name,
                quantity = current.quantity.toTurkishDoubleOrNull() ?: 0.0,
                unit = current.unit,
                expiryDate = current.expiryDate,
                lowThreshold = current.lowThreshold.toTurkishDoubleOrNull(),
                barcode = current.barcode,
                note = current.note,
            )
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val id = itemId ?: return
        viewModelScope.launch {
            repository.deleteItem(id)
            _state.update { it.copy(saved = true) }
        }
    }

    private fun formatInput(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
