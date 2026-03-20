package it.curzel.tamahero.usecase

import it.curzel.tamahero.auth.hashPassword
import it.curzel.tamahero.db.*
import it.curzel.tamahero.models.ActionType
import it.curzel.tamahero.models.Hero
import it.curzel.tamahero.models.HeroAction
import it.curzel.tamahero.models.HeroStats
import kotlin.test.*

class ActionUseCaseTest {

    private var userId: Long = 0
    private var heroId: Long = 0

    @BeforeTest
    fun setup() {
        Database.initInMemory()
        userId = UserRepository.createUser("testuser", hashPassword("password123"))
        val hero = HeroRepository.createHero(userId, "TestHero", System.currentTimeMillis())
        heroId = hero.id
    }

    @AfterTest
    fun teardown() {
        Database.close()
    }

    @Test
    fun testStartAction() {
        val result = ActionUseCase.startAction(heroId, userId, ActionType.TRAIN_STRENGTH)
        assertTrue(result is ActionResult.Started)
        val action = (result as ActionResult.Started).action
        assertEquals(ActionType.TRAIN_STRENGTH, action.type)
        assertEquals(heroId, action.heroId)
        assertFalse(action.completed)
    }

    @Test
    fun testStartActionHeroNotFound() {
        val result = ActionUseCase.startAction(999, userId, ActionType.TRAIN_STRENGTH)
        assertTrue(result is ActionResult.Error)
    }

    @Test
    fun testStartActionWrongUser() {
        val otherUserId = UserRepository.createUser("other", hashPassword("password123"))
        val result = ActionUseCase.startAction(heroId, otherUserId, ActionType.TRAIN_STRENGTH)
        assertTrue(result is ActionResult.Error)
    }

    @Test
    fun testStartActionAlreadyActive() {
        ActionUseCase.startAction(heroId, userId, ActionType.TRAIN_STRENGTH)
        val result = ActionUseCase.startAction(heroId, userId, ActionType.TRAIN_AGILITY)
        assertTrue(result is ActionResult.Error)
        assertEquals("Hero already has an active action", (result as ActionResult.Error).message)
    }

    @Test
    fun testResolveActionIdle() {
        val result = ActionUseCase.resolveAction(heroId, userId)
        assertTrue(result is ActionResult.Idle)
    }

    @Test
    fun testResolveActionInProgress() {
        ActionUseCase.startAction(heroId, userId, ActionType.TRAIN_STRENGTH)
        val result = ActionUseCase.resolveAction(heroId, userId)
        assertTrue(result is ActionResult.InProgress)
    }

    @Test
    fun testResolveActionCompleted() {
        val now = System.currentTimeMillis()
        ActionRepository.createAction(heroId, ActionType.TRAIN_STRENGTH, now - 600_000, now - 1)

        val result = ActionUseCase.resolveAction(heroId, userId)
        assertTrue(result is ActionResult.Completed)
        val hero = (result as ActionResult.Completed).hero
        assertEquals(11, hero.stats.strength)
        assertEquals(25L, hero.experience)
    }

    @Test
    fun testResolveActionHeroNotFound() {
        val result = ActionUseCase.resolveAction(999, userId)
        assertTrue(result is ActionResult.Error)
    }

    @Test
    fun testApplyActionRewardsStrength() {
        val hero = Hero(id = 1, userId = 1, name = "Test", stats = HeroStats())
        val action = HeroAction(type = ActionType.TRAIN_STRENGTH, startedAt = 0, completesAt = 0)
        val updated = ActionUseCase.applyActionRewards(hero, action)
        assertEquals(11, updated.stats.strength)
        assertEquals(10, updated.stats.agility)
        assertEquals(25L, updated.experience)
    }

    @Test
    fun testApplyActionRewardsAgility() {
        val hero = Hero(id = 1, userId = 1, name = "Test", stats = HeroStats())
        val action = HeroAction(type = ActionType.TRAIN_AGILITY, startedAt = 0, completesAt = 0)
        val updated = ActionUseCase.applyActionRewards(hero, action)
        assertEquals(11, updated.stats.agility)
        assertEquals(10, updated.stats.strength)
    }

    @Test
    fun testApplyActionRewardsWork() {
        val hero = Hero(id = 1, userId = 1, name = "Test", stats = HeroStats())
        val action = HeroAction(type = ActionType.WORK, startedAt = 0, completesAt = 0)
        val updated = ActionUseCase.applyActionRewards(hero, action)
        assertEquals(10, updated.stats.strength)
        assertEquals(10L, updated.experience)
    }

    @Test
    fun testLevelUpFromExperience() {
        val hero = Hero(id = 1, userId = 1, name = "Test", experience = 90, stats = HeroStats())
        val action = HeroAction(type = ActionType.TRAIN_STRENGTH, startedAt = 0, completesAt = 0)
        val updated = ActionUseCase.applyActionRewards(hero, action)
        assertEquals(115L, updated.experience)
        assertEquals(2, updated.level)
    }

    @Test
    fun testGetActionDuration() {
        assertEquals(5 * 60 * 1000L, ActionUseCase.getActionDuration(ActionType.TRAIN_STRENGTH))
        assertEquals(30 * 60 * 1000L, ActionUseCase.getActionDuration(ActionType.WORK))
        assertEquals(10 * 60 * 1000L, ActionUseCase.getActionDuration(ActionType.REST))
    }
}
