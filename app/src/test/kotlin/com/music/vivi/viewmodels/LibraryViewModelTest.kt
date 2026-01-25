package com.music.vivi.viewmodels

import androidx.compose.runtime.getValue
import com.music.vivi.constants.LibraryFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryViewModelTest {

    @Test
    fun `initial state is LIBRARY`() {
        val viewModel = LibraryViewModel()
        assertEquals(LibraryFilter.LIBRARY, viewModel.filter.value)
    }

    @Test
    fun `updateFilter updates state`() {
        val viewModel = LibraryViewModel()
        viewModel.updateFilter(LibraryFilter.SONGS)
        assertEquals(LibraryFilter.SONGS, viewModel.filter.value)
    }
}
