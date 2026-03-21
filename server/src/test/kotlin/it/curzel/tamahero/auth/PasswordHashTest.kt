package it.curzel.tamahero.auth

import kotlin.test.*

class PasswordHashTest {

    @Test
    fun testHashAndVerify() {
        val hash = hashPassword("test123")
        assertTrue(verifyPassword("test123", hash))
    }

    @Test
    fun testDifferentPasswordsDifferentHashes() {
        val hash1 = hashPassword("test123")
        val hash2 = hashPassword("test456")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testVerifyCorrectPassword() {
        val hash = hashPassword("mypassword")
        assertTrue(verifyPassword("mypassword", hash))
    }

    @Test
    fun testVerifyWrongPassword() {
        val hash = hashPassword("mypassword")
        assertFalse(verifyPassword("wrongpassword", hash))
    }
}
