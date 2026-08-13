package com.example.xargoosh.feature.recognition

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicRecognitionTest {
    @Test
    fun `wav encoder writes valid mono pcm header and samples`() {
        val wav = encodePcm16MonoWav(shortArrayOf(1, -2), sampleRateHz = 16_000)
        val header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)

        assertArrayEquals("RIFF".toByteArray(), wav.copyOfRange(0, 4))
        assertEquals(40, header.getInt(4))
        assertArrayEquals("WAVE".toByteArray(), wav.copyOfRange(8, 12))
        assertEquals(1, header.getShort(20).toInt())
        assertEquals(1, header.getShort(22).toInt())
        assertEquals(16_000, header.getInt(24))
        assertEquals(4, header.getInt(40))
        assertEquals(1, header.getShort(44).toInt())
        assertEquals(-2, header.getShort(46).toInt())
    }

    @Test
    fun `parser reads successful match and secure links`() {
        val response = parseAudDResponse(
            """{
                "status":"success",
                "result":{
                    "artist":"Artist",
                    "title":"Song",
                    "album":"Album",
                    "release_date":"2026-01-02",
                    "song_link":"https://lis.tn/song",
                    "spotify":{"external_urls":{"spotify":"https://open.spotify.com/track/1"}},
                    "apple_music":{"url":"https://music.apple.com/song","artwork":{"url":"https://img/{w}x{h}.jpg"}}
                }
            }""".trimIndent(),
        )

        assertTrue(response is AudDResponse.Match)
        val music = (response as AudDResponse.Match).music
        assertEquals("Artist", music.artist)
        assertEquals("Song", music.title)
        assertEquals("https://lis.tn/song", music.songLink)
        assertEquals("https://open.spotify.com/track/1", music.spotifyUrl)
        assertEquals("https://music.apple.com/song", music.appleMusicUrl)
        assertEquals("https://img/600x600.jpg", music.artworkUrl)
        assertNull(music.deezerUrl)
    }

    @Test
    fun `parser distinguishes no match and api error`() {
        assertEquals(
            AudDResponse.NoMatch,
            parseAudDResponse("""{"status":"success","result":null}"""),
        )

        val response = parseAudDResponse(
            """{"status":"error","error":{"error_code":902,"error_message":"limit"}}""",
        )
        assertEquals(
            AudDResponse.ApiError(AudDApiError(code = 902, message = "limit")),
            response,
        )
    }
}
