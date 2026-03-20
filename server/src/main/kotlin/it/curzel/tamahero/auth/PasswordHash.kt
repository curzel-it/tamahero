package it.curzel.tamahero.auth

import java.security.MessageDigest

fun hashPassword(password: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
}

fun verifyPassword(password: String, hash: String): Boolean {
    return hashPassword(password) == hash
}
