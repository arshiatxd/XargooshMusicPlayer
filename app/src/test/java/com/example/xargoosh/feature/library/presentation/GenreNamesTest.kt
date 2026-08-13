package com.example.xargoosh.feature.library.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreNamesTest {
    @Test
    fun parsesCommonGenreSeparatorsAndRemovesDuplicates() {
        assertEquals(
            listOf("Rock", "Alternative", "Pop"),
            GenreNames.from("Rock; Alternative / Pop, rock")
        )
    }

    @Test
    fun matchesGenresCaseInsensitivelyWithoutPartialMatches() {
        assertTrue(GenreNames.contains("Rock; Alternative", "rock"))
        assertFalse(GenreNames.contains("Hard Rock", "Rock"))
    }
}
