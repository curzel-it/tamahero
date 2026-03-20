package it.curzel.tamahero

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform