package com.music.vivi.viewmodels

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeChipToggleTest {
    @Test
    fun tappingTheActiveChipRestoresTheGeneralFeed() {
        assertEquals(ChipToggleAction.RESTORE, chipToggleAction(chip = "mood", selected = "mood", pending = null))
    }

    @Test
    fun tappingTheChipStillLoadingCancelsIt() {
        assertEquals(ChipToggleAction.RESTORE, chipToggleAction(chip = "genre", selected = null, pending = "genre"))
    }

    @Test
    fun tappingANullChipAlwaysRestores() {
        assertEquals(ChipToggleAction.RESTORE, chipToggleAction<String>(chip = null, selected = null, pending = null))
        assertEquals(ChipToggleAction.RESTORE, chipToggleAction<String>(chip = null, selected = "mood", pending = null))
    }

    @Test
    fun tappingAnotherChipLoadsIt() {
        assertEquals(ChipToggleAction.LOAD, chipToggleAction(chip = "genre", selected = "mood", pending = null))
    }

    @Test
    fun tappingAChipWhileAnotherIsLoadingLoadsTheNewOne() {
        assertEquals(ChipToggleAction.LOAD, chipToggleAction(chip = "energy", selected = null, pending = "genre"))
    }

    @Test
    fun firstTapOnAChipWithNothingActiveLoadsIt() {
        assertEquals(ChipToggleAction.LOAD, chipToggleAction(chip = "mood", selected = null, pending = null))
    }
}
