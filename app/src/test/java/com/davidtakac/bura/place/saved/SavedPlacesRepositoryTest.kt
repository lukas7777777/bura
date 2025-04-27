package com.davidtakac.bura.place.saved

import com.davidtakac.bura.place.saved.SavedPlacesRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class SavedPlacesRepositoryTest {
    private lateinit var fileDir: File
    private lateinit var repo: SavedPlacesRepository

    @Before
    fun setUp() {
        fileDir = mock(File::class.java)
        repo = SavedPlacesRepository(fileDir)
    }

    @Test
    fun testSaveAndRetrievePlaces() {
        // TODO: Implement test for saving and retrieving places
    }
}
