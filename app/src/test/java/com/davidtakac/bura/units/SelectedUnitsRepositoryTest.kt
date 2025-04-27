package com.davidtakac.bura.units

import com.davidtakac.bura.units.SelectedUnitsRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import android.content.SharedPreferences

class SelectedUnitsRepositoryTest {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var repo: SelectedUnitsRepository

    @Before
    fun setUp() {
        sharedPrefs = mock(SharedPreferences::class.java)
        repo = SelectedUnitsRepository(sharedPrefs)
    }

    @Test
    fun testSaveAndRetrieveUnits() {
        // TODO: Implement test for saving and retrieving units
    }
}
