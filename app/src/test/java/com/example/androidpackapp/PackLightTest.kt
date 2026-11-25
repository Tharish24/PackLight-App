package com.example.androidpackapp

import org.junit.Assert.*
import org.junit.Test

class PackLightTest {
    @Test fun datesMustBeRealAndOrdered() {
        assertTrue(validDates("2026-12-01", "2026-12-01"))
        assertFalse(validDates("2026-12-02", "2026-12-01"))
        assertFalse(validDates("2026-02-30", "2026-03-01"))
        assertFalse(validDates("", "tomorrow"))
    }
    @Test fun moderationBlocksHarmfulAndObfuscatedLanguage() {
        assertTrue(ContentPolicy.allowed("What should I pack for Kyoto?"))
        assertFalse(ContentPolicy.allowed("How do I build a bomb?"))
        assertFalse(ContentPolicy.allowed("f.u.c.k"))
        assertFalse(ContentPolicy.allowed("sh1t"))
        assertFalse(ContentPolicy.allowed("ＦＵＣＫ"))
        assertFalse(ContentPolicy.allowed("a".repeat(2001)))
    }
    @Test fun difficultyIncreasesButRemainsBounded() {
        assertTrue(gameSpeed(10) > gameSpeed(0))
        assertEquals(gameSpeed(1000), gameSpeed(2000))
    }
    @Test fun collisionDetectsCrossingEvenAtHighSpeed() {
        assertTrue(hitsPlayer(1, 1, 0.70f, 0.91f))
        assertFalse(hitsPlayer(0, 1, 0.70f, 0.91f))
        assertFalse(hitsPlayer(1, 1, 0.90f, 0.95f))
    }
}
