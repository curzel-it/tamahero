package it.curzel.tamahero.usecase

import it.curzel.tamahero.auth.hashPassword
import it.curzel.tamahero.db.Database
import it.curzel.tamahero.db.UserRepository
import kotlin.test.*

class HeroUseCaseTest {

    private var userId: Long = 0

    @BeforeTest
    fun setup() {
        Database.initInMemory()
        userId = UserRepository.createUser("testuser", hashPassword("password123"))
    }

    @AfterTest
    fun teardown() {
        Database.close()
    }

    @Test
    fun testCreateHero() {
        val result = HeroUseCase.createHero(userId, "Aragorn")
        assertTrue(result is HeroResult.Success)
        val hero = (result as HeroResult.Success).hero
        assertEquals("Aragorn", hero.name)
        assertEquals(userId, hero.userId)
        assertEquals(1, hero.level)
        assertEquals(10, hero.stats.strength)
    }

    @Test
    fun testCreateHeroBlankName() {
        val result = HeroUseCase.createHero(userId, "")
        assertTrue(result is HeroResult.Error)
        assertEquals("Hero name cannot be blank", (result as HeroResult.Error).message)
    }

    @Test
    fun testGetHero() {
        val created = HeroUseCase.createHero(userId, "Gandalf") as HeroResult.Success
        val result = HeroUseCase.getHero(created.hero.id, userId)
        assertTrue(result is HeroResult.Success)
        assertEquals("Gandalf", (result as HeroResult.Success).hero.name)
    }

    @Test
    fun testGetHeroWrongUser() {
        val created = HeroUseCase.createHero(userId, "Gandalf") as HeroResult.Success
        val otherUserId = UserRepository.createUser("other", hashPassword("password123"))
        val result = HeroUseCase.getHero(created.hero.id, otherUserId)
        assertTrue(result is HeroResult.Error)
    }

    @Test
    fun testGetHeroNotFound() {
        val result = HeroUseCase.getHero(999, userId)
        assertTrue(result is HeroResult.Error)
    }

    @Test
    fun testGetHeroesByUser() {
        HeroUseCase.createHero(userId, "Hero1")
        HeroUseCase.createHero(userId, "Hero2")
        val heroes = HeroUseCase.getHeroesByUser(userId)
        assertEquals(2, heroes.size)
    }

    @Test
    fun testGetHeroesByUserEmpty() {
        val heroes = HeroUseCase.getHeroesByUser(userId)
        assertEquals(0, heroes.size)
    }
}
