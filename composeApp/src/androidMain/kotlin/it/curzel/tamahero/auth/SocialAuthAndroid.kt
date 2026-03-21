package it.curzel.tamahero.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SocialAuthAndroid(private val context: Context) : SocialAuthProvider {
    private val googleClientId: String
        get() {
            val resId = context.resources.getIdentifier("google_client_id", "string", context.packageName)
            if (resId == 0) return ""
            return context.getString(resId).takeIf { it.isNotEmpty() } ?: ""
        }

    override suspend fun signInWithGoogle(): SocialAuthResult? {
        if (!isGoogleAvailable()) return null
        return withContext(Dispatchers.Main) {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(googleClientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                SocialAuthResult(idToken = googleIdTokenCredential.idToken, provider = "google")
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun signInWithApple(): SocialAuthResult? = null
    override fun isGoogleAvailable(): Boolean = googleClientId.isNotEmpty()
    override fun isAppleAvailable(): Boolean = false
}
