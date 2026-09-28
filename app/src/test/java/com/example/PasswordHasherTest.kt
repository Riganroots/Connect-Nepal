package com.example

import com.example.data.security.PasswordHasher
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PasswordHasherTest {

    @Test
    fun hashIsNotPlaintextAndVerifies() {
        val hash = PasswordHasher.hash("correct horse")
        assertFalse(hash.contains("correct horse"))
        assertTrue(hash.startsWith("pbkdf2-sha256$"))
        assertTrue(PasswordHasher.verify("correct horse", hash))
    }

    @Test
    fun wrongPasswordIsRejected() {
        val hash = PasswordHasher.hash("correct horse")
        assertFalse(PasswordHasher.verify("battery staple", hash))
    }

    @Test
    fun samePasswordGetsDifferentSalts() {
        assertNotEquals(PasswordHasher.hash("same password"), PasswordHasher.hash("same password"))
    }

    @Test
    fun blankOrMalformedHashesNeverVerify() {
        assertFalse(PasswordHasher.verify("", ""))
        assertFalse(PasswordHasher.verify("password123", ""))
        assertFalse(PasswordHasher.verify("password123", "password123"))
        assertFalse(PasswordHasher.verify("password123", "pbkdf2-md5$1$AAAA$AAAA"))
    }
}
