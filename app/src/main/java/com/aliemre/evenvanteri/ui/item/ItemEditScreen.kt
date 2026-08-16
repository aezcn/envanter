package com.aliemre.evenvanteri.ui.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliemre.evenvanteri.data.local.LocationEntity
import com.aliemre.evenvanteri.ui.UNITS
import com.aliemre.evenvanteri.ui.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    viewModel: ItemEditViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var datePickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Ürün ekle" else "Ürünü düzenle") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.Close, contentDescription = "Kapat")
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = viewModel::delete) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = "Sil",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = { Text("Ürün adı") },
                placeholder = { Text("örn. bulgur, zerdeçal, sarma") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.quantity,
                    onValueChange = viewModel::onQuantity,
                    label = { Text("Miktar") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                UnitPicker(
                    unit = state.unit,
                    onUnitChange = viewModel::onUnit,
                    modifier = Modifier.weight(1f),
                )
            }

            LocationPicker(
                locations = state.locations,
                selectedId = state.locationId,
                onSelect = viewModel::onLocation,
            )

            HorizontalDivider()

            Text(
                text = "İsteğe bağlı",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ExpiryField(
                date = state.expiryDate,
                onOpenPicker = { datePickerOpen = true },
                onClear = { viewModel.onExpiry(null) },
            )

            OutlinedTextField(
                value = state.lowThreshold,
                onValueChange = viewModel::onLowThreshold,
                label = { Text("Azalma eşiği") },
                supportingText = {
                    Text("Bu miktarın altına düşünce ana ekranda uyarır")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNote,
                label = { Text("Not") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isNew) "Ekle" else "Kaydet")
            }
        }
    }

    if (datePickerOpen) {
        ExpiryDatePicker(
            initial = state.expiryDate,
            onPick = {
                viewModel.onExpiry(it)
                datePickerOpen = false
            },
            onDismiss = { datePickerOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitPicker(
    unit: String,
    onUnitChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = unit,
            onValueChange = {},
            readOnly = true,
            label = { Text("Birim") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            UNITS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onUnitChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Konum seçimi açılır liste değil çip satırı: ev başına bir avuç dolap var ve
 * hepsini aynı anda görmek, listeyi açıp aramaktan hızlı.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocationPicker(
    locations: List<LocationEntity>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Nerede duruyor?", style = MaterialTheme.typography.titleSmall)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            locations.forEach { location ->
                FilterChip(
                    selected = location.id == selectedId,
                    onClick = { onSelect(location.id) },
                    label = { Text(location.name) },
                )
            }
        }
    }
}

@Composable
private fun ExpiryField(
    date: LocalDate?,
    onOpenPicker: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Son kullanma tarihi", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = date?.let { formatDate(it) } ?: "belirtilmedi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row {
            if (date != null) {
                TextButton(onClick = onClear) { Text("Kaldır") }
            }
            TextButton(onClick = onOpenPicker) {
                Text(if (date == null) "Tarih seç" else "Değiştir")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpiryDatePicker(
    initial: LocalDate?,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        // Seçici UTC gün başlangıcı döndürür; yerel saat diliminde
                        // yorumlarsak tarih bir gün kayabilir.
                        onPick(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate(),
                        )
                    }
                },
                enabled = pickerState.selectedDateMillis != null,
            ) {
                Text("Seç")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        },
    ) {
        DatePicker(state = pickerState)
    }
}
