package it.curzel.tamahero.auth

import kotlin.test.*

class PasswordHashTest {

    @Test
    fun testHashIsConsistent() {
        val hash1 = hashPassword("test123")
        val hash2 = hashPassword("test123")
        assertEquals(hash1, hash2)
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
