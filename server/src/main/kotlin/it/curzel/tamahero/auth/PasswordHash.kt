package it.curzel.tamahero.auth

import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest

fun hashPassword(password: String): String =
    BCrypt.hashpw(password, BCrypt.gensalt(12))

fun verifyPassword(password: String, hash: String): Boolean =
    try {
        BCrypt.checkpw(password, hash)
    } catch (e: Exception) {
        false
    }

fun hashPasswordSha256(password: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
}

fun verifyPasswordSha256(password: String, hash: String): Boolean =
    hashPasswordSha256(password) == hash
