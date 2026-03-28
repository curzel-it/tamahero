package it.curzel.tamahero.auth

class SocialAuthWasm : SocialAuthProvider {
    override suspend fun signInWithGoogle(): SocialAuthResult? = null
    override suspend fun signInWithApple(): SocialAuthResult? = null
    override fun isGoogleAvailable(): Boolean = false
    override fun isAppleAvailable(): Boolean = false
}
