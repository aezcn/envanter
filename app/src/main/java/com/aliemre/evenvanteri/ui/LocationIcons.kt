package com.aliemre.evenvanteri.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Konum ikonu anahtarını çizilebilir bir simgeye çevirir.
 *
 * Anahtarlar veritabanında metin olarak tutuluyor; ikon setini sonradan
 * değiştirmek veritabanına dokunmayı gerektirmesin diye.
 */
fun locationIcon(key: String): ImageVector = when (key) {
    "fridge" -> Icons.Filled.Kitchen
    "freezer" -> Icons.Filled.AcUnit
    "grain" -> Icons.Filled.Grass
    "spice" -> Icons.Outlined.Spa
    "pill" -> Icons.Filled.LocalPharmacy
    "closet" -> Icons.Filled.Checkroom
    "basket" -> Icons.Filled.ShoppingBasket
    else -> Icons.Filled.Inventory2
}

fun locationIconLabel(key: String): String = when (key) {
    "fridge" -> "Buzdolabı"
    "freezer" -> "Buzluk"
    "grain" -> "Bakliyat"
    "spice" -> "Baharat"
    "pill" -> "Takviye"
    "closet" -> "Yüklük"
    "basket" -> "Sepet"
    else -> "Dolap"
}
