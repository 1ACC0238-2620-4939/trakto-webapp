package pe.trako.traktoroute.features.vehicles.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class VehicleInputTest {
    @Test
    fun acceptsFormattedAndCanonicalPlates() {
        assertEquals("ABC123", normalizePlate(" abc-123 "))
        assertEquals("ABC123", normalizePlate("ABC123"))
        assertThrows(IllegalArgumentException::class.java) { normalizePlate("AB-123") }
    }

    @Test
    fun acceptsPositiveDecimalCapacityOnly() {
        assertEquals(BigDecimal("1500.50"), parseCapacity("1500,50"))
        assertThrows(IllegalArgumentException::class.java) { parseCapacity("0") }
        assertThrows(IllegalArgumentException::class.java) { parseCapacity("abc") }
    }
}
