package it.curzel.tamahero.usecase

import it.curzel.tamahero.db.Database
import it.curzel.tamahero.db.UserRepository
import kotlin.test.*

class RegisterUseCaseTest {

    @BeforeTest
    fun setup() {
        Database.initInMemory()
    }

    @AfterTest
    fun teardown() {
        Database.close()
    }

    @Test
    fun testSuccessfulRegistration() {
        val result = RegisterUseCase.register("testuser", "password123")
        assertTrue(result is RegisterResult.Success)
        val user = UserRepository.findByUsername("testuser")
        assertNotNull(user)
        assertEquals("testuser", user.username)
    }

    @Test
    fun testBlankUsername() {
        val result = RegisterUseCase.register("", "password123")
        assertTrue(result is RegisterResult.Error)
        assertEquals("Username cannot be blank", (result as RegisterResult.Error).message)
    }

    @Test
    fun testShortPassword() {
        val result = RegisterUseCase.register("testuser", "12345")
        assertTrue(result is RegisterResult.Error)
        assertEquals("Password must be at least 6 characters", (result as RegisterResult.Error).message)
    }

    @Test
    fun testDuplicateUsername() {
        RegisterUseCase.register("testuser", "password123")
        val result = RegisterUseCase.register("testuser", "otherpass123")
        assertTrue(result is RegisterResult.Error)
        assertEquals("Username already taken", (result as RegisterResult.Error).message)
    }
}
