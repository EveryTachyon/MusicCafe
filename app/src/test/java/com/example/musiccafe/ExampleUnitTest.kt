package com.example.musiccafe

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun sanitizeFileName_removesUnsafeCharacters() {
        assertEquals("song_01.mp3", sanitizeFileName("song/01.mp3"))
        assertEquals("my_track.mp3", sanitizeFileName("my track?.mp3"))
    }

    @Test
    fun isSupportedAudioMimeType_acceptsMusicFiles() {
        assertTrue(isSupportedAudioMimeType("audio/mpeg"))
        assertTrue(isSupportedAudioMimeType("audio/mp4"))
        assertTrue(isSupportedAudioMimeType("audio/x-wav"))
        assertFalse(isSupportedAudioMimeType("video/mp4"))
    }
}