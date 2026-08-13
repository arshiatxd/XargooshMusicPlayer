package com.example.xargoosh.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistCreditsTest {
    @Test
    fun parsesCommonCollaborations() {
        assertEquals(listOf("Kanye West", "JAY-Z"), ArtistCredits.names("Kanye West feat. JAY-Z"))
        assertEquals(listOf("A", "B", "C"), ArtistCredits.names("A & B with C"))
        assertEquals(
            listOf("Kanye West", "JAY-Z", "Patrick"),
            ArtistCredits.names("Kanye West, JAY-Z & Patrick")
        )
    }

    @Test
    fun renamePreservesCollaboratorsAndSeparator() {
        assertEquals("Ye feat. JAY-Z", ArtistCredits.replace("Kanye West feat. JAY-Z", "Kanye West", "Ye"))
    }

    @Test
    fun standaloneXIsAnArtistName() {
        assertEquals(listOf("Malcolm X"), ArtistCredits.names("Malcolm X"))
        assertEquals(listOf("X"), ArtistCredits.names("X"))
    }

    @Test
    fun preservesAmbiguousPunctuationInArtistNames() {
        assertEquals(listOf("AC/DC"), ArtistCredits.names("AC/DC"))
        assertEquals(listOf("Tyler, The Creator"), ArtistCredits.names("Tyler, The Creator"))
        assertEquals(listOf("Earth, Wind & Fire"), ArtistCredits.names("Earth, Wind & Fire"))
        assertEquals(
            listOf("Tyler, The Creator", "Kali Uchis"),
            ArtistCredits.names("Tyler, The Creator feat. Kali Uchis")
        )
        assertEquals(
            listOf("Earth, Wind & Fire", "Foo"),
            ArtistCredits.names("Earth, Wind & Fire x Foo")
        )
        assertEquals(
            listOf("Tyler, The Creator", "Kali Uchis"),
            ArtistCredits.names("Tyler, The Creator & Kali Uchis")
        )
    }

    @Test
    fun ranksExactAndNearFullNamesFirst() {
        assertEquals(0, ArtistCredits.matchScore("Kanye West", "kanye west"))
        assertEquals(1, ArtistCredits.matchScore("Kanye West", "kanye wes"))
        assertTrue(ArtistCredits.matchScore("Kanye West", "kanye wset") < ArtistCredits.matchScore("Kanye East", "kanye wset"))
    }
}
