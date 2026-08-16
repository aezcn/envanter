package com.aliemre.evenvanteri.ui

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DISPLAY_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy")

/** Son kullanma tarihinin aciliyeti. Renk ve sıralama bundan türer. */
enum class ExpiryStatus { EXPIRED, TODAY, SOON, LATER }

fun expiryStatus(date: LocalDate, today: LocalDate = LocalDate.now()): ExpiryStatus {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0L -> ExpiryStatus.EXPIRED
        days == 0L -> ExpiryStatus.TODAY
        days <= 7L -> ExpiryStatus.SOON
        else -> ExpiryStatus.LATER
    }
}

/**
 * "3 gün kaldı", "bugün son gün", "2 gün geçti" gibi kısa bir ifade.
 *
 * Tarihi ham göstermek kullanıcıyı her seferinde kafadan çıkarma yapmaya
 * zorluyor; asıl merak edilen kaç gün kaldığı.
 */
fun expiryLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days < -1L -> "${-days} gün geçti"
        days == -1L -> "dün doldu"
        days == 0L -> "bugün son gün"
        days == 1L -> "yarın doluyor"
        else -> "$days gün kaldı"
    }
}

fun formatDate(date: LocalDate): String = date.format(DISPLAY_DATE)

fun parseDate(iso: String?): LocalDate? =
    iso?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/**
 * Miktarı gereksiz ondalık olmadan yazar: 2 yerine "2.0", 1.5 yerine "1,5".
 * Envanterde çoğu miktar tam sayı; ondalık göstermek gürültü.
 */
fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else value.toString().replace('.', ',')

fun statusColor(status: ExpiryStatus, error: Color, warning: Color, normal: Color): Color =
    when (status) {
        ExpiryStatus.EXPIRED, ExpiryStatus.TODAY -> error
        ExpiryStatus.SOON -> warning
        ExpiryStatus.LATER -> normal
    }

/** Ürün ekleme ekranındaki birim seçenekleri. */
val UNITS = listOf("adet", "paket", "gr", "kg", "ml", "L", "kutu", "şişe")

/** Konum ikonu anahtarları; ayarlarda seçilebilir. */
val LOCATION_ICONS = listOf(
    "fridge", "freezer", "grain", "spice", "pill", "cabinet", "closet", "basket",
)
