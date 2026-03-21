package it.curzel.tamahero.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LinkingState {
    data object Idle : LinkingState()
    data object Linking : LinkingState()
    data class Success(val provider: String) : LinkingState()
    data class Error(val message: String) : LinkingState()
}

class AuthViewModel(private val authClient: AuthClient) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val authState: StateFlow<AuthState> = authClient.state

    private val _currentScreen = MutableStateFlow(AuthScreen.LOGIN)
    val currentScreen: StateFlow<AuthScreen> = _currentScreen.asStateFlow()

    private val _linkingState = MutableStateFlow<LinkingState>(LinkingState.Idle)
    val linkingState: StateFlow<LinkingState> = _linkingState.asStateFlow()

    private val _linkedAccounts = MutableStateFlow<List<String>>(emptyList())
    val linkedAccounts: StateFlow<List<String>> = _linkedAccounts.asStateFlow()

    fun login(username: String, password: String) {
        scope.launch { authClient.login(username, password) }
    }

    fun register(username: String, password: String, email: String? = null) {
        scope.launch { authClient.register(username, password, email) }
    }

    fun socialLogin(provider: String) {
        scope.launch { authClient.socialLogin(provider) }
    }

    fun linkSocialAccount(provider: String) {
        scope.launch {
            _linkingState.value = LinkingState.Linking
            when (val result = authClient.linkSocialAccount(provider)) {
                is LinkResult.Success -> {
                    _linkingState.value = LinkingState.Success(result.provider)
                    refreshLinkedAccounts()
                }
                is LinkResult.Error -> _linkingState.value = LinkingState.Error(result.message)
            }
        }
    }

    fun refreshLinkedAccounts() {
        scope.launch { _linkedAccounts.value = authClient.getLinkedAccounts() }
    }

    fun clearLinkingState() { _linkingState.value = LinkingState.Idle }

    fun deleteAccount() {
        scope.launch {
            authClient.deleteAccount()
            _linkedAccounts.value = emptyList()
        }
    }

    fun logout() {
        authClient.logout()
        _linkedAccounts.value = emptyList()
    }

    fun clearError() { authClient.clearError() }

    fun forgotPassword(email: String, onResult: (Boolean) -> Unit) {
        scope.launch {
            val success = authClient.forgotPassword(email)
            onResult(success)
        }
    }

    fun showLogin() { _currentScreen.value = AuthScreen.LOGIN }
    fun showSignup() { _currentScreen.value = AuthScreen.SIGNUP }
}
