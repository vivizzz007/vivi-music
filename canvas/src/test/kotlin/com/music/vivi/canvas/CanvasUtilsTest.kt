package com.music.vivi.canvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasUtilsTest {

    @Test
    fun testNormalizeForComparison() {
        // Casing and diacritics
        assertEquals("mexico", "México".normalizeForComparison())
        assertEquals("edicion", "Edición".normalizeForComparison())
        assertEquals("cancion", "canción".normalizeForComparison())

        // Punctuation and spacing
        assertEquals("mexico en la piel edicion diamante", "México en la Piel (Edición Diamante)".normalizeForComparison())
        assertEquals("mexico en la piel edicion diamante", "Mexico en la Piel (edicion diamante)".normalizeForComparison())

        // Full match check
        val normalized1 = "México en la Piel (Edición Diamante)".normalizeForComparison()
        val normalized2 = "Mexico en la Piel (edicion diamante)".normalizeForComparison()
        assertEquals(normalized1, normalized2)
        assertTrue(normalized1.isNotBlank())
    }
}
