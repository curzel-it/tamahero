package it.curzel.tamahero.game

interface GameTimerItem {
    fun onFrameTick(totalRunTime: Long, timeDelta: Long)
}
