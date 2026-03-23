package it.curzel.tamahero.models

import kotlin.test.*

class ResourcesTest {

    @Test
    fun plus() {
        val a = Resources(credits = 100, alloy = 200)
        val b = Resources(credits = 50, alloy = 30, crystal = 10)
        val result = a + b
        assertEquals(150, result.credits)
        assertEquals(230, result.alloy)
        assertEquals(10, result.crystal)
        assertEquals(0, result.plasma)
    }

    @Test
    fun minus() {
        val a = Resources(credits = 100, alloy = 200)
        val b = Resources(credits = 30, alloy = 50)
        val result = a - b
        assertEquals(70, result.credits)
        assertEquals(150, result.alloy)
    }

    @Test
    fun hasEnoughTrue() {
        val resources = Resources(credits = 100, alloy = 200, crystal = 50)
        assertTrue(resources.hasEnough(Resources(credits = 50, alloy = 100)))
    }

    @Test
    fun hasEnoughFalse() {
        val resources = Resources(credits = 100, alloy = 200)
        assertFalse(resources.hasEnough(Resources(credits = 150)))
    }

    @Test
    fun hasEnoughExact() {
        val resources = Resources(credits = 100)
        assertTrue(resources.hasEnough(Resources(credits = 100)))
    }

    @Test
    fun timesHalf() {
        val resources = Resources(credits = 100, alloy = 50, crystal = 30)
        val result = resources * 0.5
        assertEquals(50, result.credits)
        assertEquals(25, result.alloy)
        assertEquals(15, result.crystal)
    }

    @Test
    fun capAt() {
        val resources = Resources(credits = 5000, alloy = 3000, crystal = 100)
        val cap = Resources(credits = 1000, alloy = 2500, crystal = 500)
        val result = resources.capAt(cap)
        assertEquals(1000, result.credits)
        assertEquals(2500, result.alloy)
        assertEquals(100, result.crystal)
    }
}
