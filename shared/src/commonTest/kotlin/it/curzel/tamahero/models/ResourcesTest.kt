package it.curzel.tamahero.models

import kotlin.test.*

class ResourcesTest {

    @Test
    fun plus() {
        val a = Resources(gold = 100, wood = 200)
        val b = Resources(gold = 50, wood = 30, metal = 10)
        val result = a + b
        assertEquals(150, result.gold)
        assertEquals(230, result.wood)
        assertEquals(10, result.metal)
        assertEquals(0, result.mana)
    }

    @Test
    fun minus() {
        val a = Resources(gold = 100, wood = 200)
        val b = Resources(gold = 30, wood = 50)
        val result = a - b
        assertEquals(70, result.gold)
        assertEquals(150, result.wood)
    }

    @Test
    fun hasEnoughTrue() {
        val resources = Resources(gold = 100, wood = 200, metal = 50)
        assertTrue(resources.hasEnough(Resources(gold = 50, wood = 100)))
    }

    @Test
    fun hasEnoughFalse() {
        val resources = Resources(gold = 100, wood = 200)
        assertFalse(resources.hasEnough(Resources(gold = 150)))
    }

    @Test
    fun hasEnoughExact() {
        val resources = Resources(gold = 100)
        assertTrue(resources.hasEnough(Resources(gold = 100)))
    }

    @Test
    fun capAt() {
        val resources = Resources(gold = 5000, wood = 3000, metal = 100)
        val cap = Resources(gold = 1000, wood = 2500, metal = 500)
        val result = resources.capAt(cap)
        assertEquals(1000, result.gold)
        assertEquals(2500, result.wood)
        assertEquals(100, result.metal)
    }
}
