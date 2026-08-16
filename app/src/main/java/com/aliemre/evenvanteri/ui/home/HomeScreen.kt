package com.aliemre.evenvanteri.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliemre.evenvanteri.data.local.ItemEntity
import com.aliemre.evenvanteri.data.local.LocationWithCount
import com.aliemre.evenvanteri.ui.components.ItemRow
import com.aliemre.evenvanteri.ui.locationIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenLocation: (String) -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ev Envanteri") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Filled.Add, contentDescription = "Ürün ekle")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SearchField(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                )
            }

            // Arama açıkken diğer her şey gizlenir: kullanıcı o an tek bir soru
            // soruyor ve cevabın altında dolap listesi görmek istemiyor.
            if (query.isNotBlank()) {
                searchResultsSection(
                    results = results,
                    locationNames = state.locationNames,
                    onEditItem = onEditItem,
                    onAdjust = viewModel::adjustQuantity,
                )
                return@LazyColumn
            }

            alertSection(
                title = "Tarihi geçenler",
                items = state.expired,
                tone = AlertTone.ERROR,
                locationNames = state.locationNames,
                onEditItem = onEditItem,
                onAdjust = viewModel::adjustQuantity,
            )

            alertSection(
                title = "Yakında bitiyor",
                items = state.expiringSoon,
                tone = AlertTone.WARNING,
                locationNames = state.locationNames,
                onEditItem = onEditItem,
                onAdjust = viewModel::adjustQuantity,
            )

            alertSection(
                title = "Azalanlar",
                items = state.lowStock,
                tone = AlertTone.NEUTRAL,
                locationNames = state.locationNames,
                onEditItem = onEditItem,
                onAdjust = viewModel::adjustQuantity,
            )

            item {
                Text(
                    text = "Dolaplar",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Grid yerine ikişerli satırlar: LazyColumn içinde LazyVerticalGrid
            // iç içe kaydırma sorunu çıkarır.
            items(state.locations.chunked(2)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { location ->
                        LocationCard(
                            location = location,
                            onClick = { onOpenLocation(location.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }

            if (state.locations.isEmpty()) {
                item {
                    Text(
                        text = "Henüz dolap yok. Ayarlardan ekleyebilirsin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Tüm dolaplarda ara") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Aramayı temizle")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
    )
}

private enum class AlertTone { ERROR, WARNING, NEUTRAL }

/**
 * Uyarı kartı. Liste boşsa hiç çizilmez — envanter yolundayken ana ekran sade
 * kalmalı, yoksa uyarılar arka plan gürültüsüne dönüşür ve fark edilmez olur.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.alertSection(
    title: String,
    items: List<ItemEntity>,
    tone: AlertTone,
    locationNames: Map<String, String>,
    onEditItem: (String) -> Unit,
    onAdjust: (String, Double) -> Unit,
) {
    if (items.isEmpty()) return

    item(key = "alert-$title") {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = when (tone) {
                    AlertTone.ERROR -> MaterialTheme.colorScheme.errorContainer
                    AlertTone.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                    AlertTone.NEUTRAL -> MaterialTheme.colorScheme.secondaryContainer
                },
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = items.size.toString(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                items.forEach { item ->
                    ItemRow(
                        item = item,
                        subtitle = locationNames[item.locationId],
                        onClick = { onEditItem(item.id) },
                        onAdjust = { delta -> onAdjust(item.id, delta) },
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.searchResultsSection(
    results: List<ItemEntity>,
    locationNames: Map<String, String>,
    onEditItem: (String) -> Unit,
    onAdjust: (String, Double) -> Unit,
) {
    if (results.isEmpty()) {
        item {
            Text(
                text = "Eşleşen ürün yok.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }

    items(results, key = { it.id }) { item ->
        ItemRow(
            item = item,
            subtitle = locationNames[item.locationId],
            onClick = { onEditItem(item.id) },
            onAdjust = { delta -> onAdjust(item.id, delta) },
        )
    }
}

@Composable
private fun LocationCard(
    location: LocationWithCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = locationIcon(location.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = location.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (location.itemCount == 0) "boş" else "${location.itemCount} ürün",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
