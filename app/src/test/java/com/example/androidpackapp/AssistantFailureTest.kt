package com.example.androidpackapp
import org.junit.Assert.*
import org.junit.Test

class AssistantFailureTest {
    @Test fun quotaAndLocalRateLimitsAreDistinct() {
        assertTrue(assistantError(429, "provider_429").contains("usage limit"))
        assertTrue(assistantError(429, "").contains("wait a minute"))
    }
    @Test fun timeoutAndIncompleteResponsesCanBeRetried() {
        assertTrue(assistantError(503, "timeout").contains("Retry"))
        assertTrue(assistantError(503, "incomplete").contains("incomplete"))
    }
    @Test fun accountErrorsDoNotAskUsersForKeys() {
        assertTrue(assistantError(503, "provider_401").contains("app owner"))
        assertFalse(assistantError(503, "provider_401").contains("API key"))
    }
}
