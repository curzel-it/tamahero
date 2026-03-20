package it.curzel.tamahero.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object GameTimer {
    private val scope = CoroutineScope(Dispatchers.Default)

    private var objects: MutableList<Pair<String, GameTimerItem>> = mutableListOf()
    private var objectsToAdd: MutableList<Pair<String, GameTimerItem>> = mutableListOf()
    private var objectsToRemove: MutableList<String> = mutableListOf()
    private const val TARGET_FPS = 60
    private val targetFrameTime = 1000L / TARGET_FPS
    private val minFrameTime = 1L
    private val maxFrameTime = 100L
    private var lastFrameTime: Long = Clock.System.now().toEpochMilliseconds()
    private var totalRunTime = 0L
    private var didStart = false

    fun start() {
        if (didStart) return
        didStart = true

        scope.launch {
            while (true) {
                updateObjects()
                loop()
            }
        }
    }

    fun clear() {
        didStart = false
        objects.clear()
    }

    fun add(key: String, thing: GameTimerItem) {
        objectsToAdd.add(key to thing)
    }

    fun remove(key: String) {
        objectsToRemove.add(key)
    }

    private fun updateObjects() {
        objects.removeAll { objectsToRemove.contains(it.first) }
        objects.addAll(objectsToAdd)

        objectsToAdd.clear()
        objectsToRemove.clear()
    }

    private suspend fun loop() {
        val start = Clock.System.now().toEpochMilliseconds()
        val timeSinceLastFrame = min(start - lastFrameTime, maxFrameTime)

        if (timeSinceLastFrame >= minFrameTime) {
            totalRunTime += timeSinceLastFrame
            objects.forEach {
                it.second.onFrameTick(totalRunTime, timeSinceLastFrame)
            }
        }

        val end = Clock.System.now().toEpochMilliseconds()
        lastFrameTime = start

        val timeSpentProcessing = end - start
        val timeToNextFrame = max(targetFrameTime - timeSpentProcessing, 0L)
        if (timeToNextFrame > 0) {
            delay(timeToNextFrame)
        }
    }
}
