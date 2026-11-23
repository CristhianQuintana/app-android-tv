package com.cotasperu.tv.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginAttemptGuardTest {
    @Test
    fun noHardLockAtFourthAttempt_onlyShortCooldown() {
        var now = 1_000L
        val guard = LoginAttemptGuard { now }

        repeat(3) {
            assertNull(guard.registerCredentialFailure())
            assertFalse(guard.isCoolingDown())
        }
        val msg = guard.registerCredentialFailure()
        assertNotNull(msg)
        assertTrue(guard.isCoolingDown())
        assertEquals(3_000L, guard.remainingCoolDownMs())

        now += 3_100L
        assertFalse(guard.isCoolingDown())
        assertNull(guard.blockMessageOrNull())
    }

    @Test
    fun successResetsFailures() {
        val guard = LoginAttemptGuard { 0L }
        repeat(4) { guard.registerCredentialFailure() }
        guard.registerSuccess()
        assertFalse(guard.isCoolingDown())
        repeat(3) { assertNull(guard.registerCredentialFailure()) }
    }
}
