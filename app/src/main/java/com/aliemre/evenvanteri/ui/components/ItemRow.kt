package com.aliemre.evenvanteri.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliemre.evenvanteri.data.local.ItemEntity
import com.aliemre.evenvanteri.ui.ExpiryStatus
import com.aliemre.evenvanteri.ui.expiryLabel
import com.aliemre.evenvanteri.ui.expiryStatus
import com.aliemre.evenvanteri.ui.formatQuantity
import com.aliemre.evenvanteri.ui.parseDate

/**
 * Envanterdeki tek bir ürün satırı.
 *
 * Miktar butonları doğrudan satırın içinde: ürünü kullandıktan sonra güncellemek
 * bir dokunuştan fazla sürerse kimse envanteri güncel tutmaz, uygulama da bir
 * hafta içinde yalan söylemeye başlar.
 */
@Composable
fun ItemRow(
    item: ItemEntity,
    onClick: () -> Unit,
    onAdjust: (Double) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val expiry = parseDate(item.expiryDate)
    val status = expiry?.let { expiryStatus(it) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val details = buildList {
                    subtitle?.let { add(it) }
                    if (expiry != null) add(expiryLabel(expiry))
                }
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (status) {
                            ExpiryStatus.EXPIRED, ExpiryStatus.TODAY ->
                                MaterialTheme.colorScheme.error
                            ExpiryStatus.SOON -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            QuantityStepper(
                quantity = item.quantity,
                unit = item.unit,
                isLow = item.lowThreshold?.let { item.quantity <= it } == true,
                onAdjust = onAdjust,
            )
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Double,
    unit: String,
    isLow: Boolean,
    onAdjust: (Double) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onAdjust(-1.0) },
            enabled = quantity > 0.0,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Bir azalt")
        }

        Text(
            text = "${formatQuantity(quantity)} $unit",
            style = MaterialTheme.typography.labelLarge,
            color = if (isLow) MaterialTheme.colorScheme.error else Color.Unspecified,
        )

        IconButton(
            onClick = { onAdjust(1.0) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Bir artır")
        }
    }
}
