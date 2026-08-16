package com.aliemre.evenvanteri.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Sınır durumları: "bugün" ile "yarın" ve "7 gün" ile "8 gün" arasındaki çizgiler.
 * Bu hesap bir gün kayarsa uygulama sessizce yanlış çalışır — hiçbir yerde hata
 * vermez, sadece uyarıları yanlış günde gösterir.
 */
class ExpiryTest {

    private val today = LocalDate.of(2026, 8, 16)

    @Test
    fun `gecmis tarih EXPIRED`() {
        assertEquals(ExpiryStatus.EXPIRED, expiryStatus(today.minusDays(1), today))
        assertEquals(ExpiryStatus.EXPIRED, expiryStatus(today.minusDays(30), today))
    }

    @Test
    fun `bugun TODAY`() {
        assertEquals(ExpiryStatus.TODAY, expiryStatus(today, today))
    }

    @Test
    fun `yedi gune kadar SOON, sekizinci gun LATER`() {
        assertEquals(ExpiryStatus.SOON, expiryStatus(today.plusDays(1), today))
        assertEquals(ExpiryStatus.SOON, expiryStatus(today.plusDays(7), today))
        assertEquals(ExpiryStatus.LATER, expiryStatus(today.plusDays(8), today))
    }

    @Test
    fun `etiketler gun sayisini dogru yazar`() {
        assertEquals("bugün son gün", expiryLabel(today, today))
        assertEquals("yarın doluyor", expiryLabel(today.plusDays(1), today))
        assertEquals("3 gün kaldı", expiryLabel(today.plusDays(3), today))
        assertEquals("dün doldu", expiryLabel(today.minusDays(1), today))
        assertEquals("5 gün geçti", expiryLabel(today.minusDays(5), today))
    }

    @Test
    fun `ay ve yil siniri asilinca da dogru sayar`() {
        // 31 Aralık -> 1 Ocak: takvim sarması gün farkını bozmamalı.
        val yearEnd = LocalDate.of(2026, 12, 31)
        assertEquals("yarın doluyor", expiryLabel(LocalDate.of(2027, 1, 1), yearEnd))
        assertEquals(ExpiryStatus.SOON, expiryStatus(LocalDate.of(2027, 1, 1), yearEnd))
    }

    @Test
    fun `miktar gereksiz ondalik gostermez`() {
        assertEquals("2", formatQuantity(2.0))
        assertEquals("1,5", formatQuantity(1.5))
        assertEquals("0", formatQuantity(0.0))
    }
}
