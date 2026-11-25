package com.example.androidpackapp
import org.junit.Assert.*
import org.junit.Test

class OpenRouterErrorTest {
    @Test fun accountAndRateLimitFailuresGiveActionableMessages() {
        assertTrue(routerError(401).contains("key"))
        assertTrue(routerError(402).contains("credits"))
        assertTrue(routerError(429).contains("wait"))
        assertTrue(routerError(503).contains("503"))
    }
}
