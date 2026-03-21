package it.curzel.tamahero.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreenView(viewModel: AuthViewModel) {
    val authState by viewModel.authState.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("TamaHero", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(32.dp))

        when (authState) {
            is AuthState.Loading -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text((authState as AuthState.Loading).message)
            }
            is AuthState.Error -> {
                Text((authState as AuthState.Error).message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { viewModel.clearError() }) { Text("Try again") }
            }
            else -> {
                when (currentScreen) {
                    AuthScreen.LOGIN -> LoginForm(viewModel)
                    AuthScreen.SIGNUP -> SignupForm(viewModel)
                }
            }
        }
    }
}

@Composable
private fun LoginForm(viewModel: AuthViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotPassword by remember { mutableStateOf(false) }

    Column(modifier = Modifier.widthIn(max = 360.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username or email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (username.isNotBlank() && password.isNotBlank()) viewModel.login(username, password) }),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.login(username, password) },
            enabled = username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Log in") }

        Spacer(Modifier.height(8.dp))
        SocialLoginButtons(viewModel)

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { showForgotPassword = true }) { Text("Forgot password?") }
        TextButton(onClick = { viewModel.showSignup() }) { Text("Don't have an account? Sign up") }
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(viewModel) { showForgotPassword = false }
    }
}

@Composable
private fun SignupForm(viewModel: AuthViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column(modifier = Modifier.widthIn(max = 360.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (username.isNotBlank() && password.isNotBlank()) viewModel.register(username, password, email.ifBlank { null }) }),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.register(username, password, email.ifBlank { null }) },
            enabled = username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Sign up") }

        Spacer(Modifier.height(8.dp))
        SocialLoginButtons(viewModel)

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { viewModel.showLogin() }) { Text("Already have an account? Log in") }
    }
}

@Composable
private fun SocialLoginButtons(viewModel: AuthViewModel) {
    val googleAvailable = remember {
        SocialAuthProviderHolder.isInitialized() && SocialAuthProviderHolder.instance.isGoogleAvailable()
    }
    val appleAvailable = remember {
        SocialAuthProviderHolder.isInitialized() && SocialAuthProviderHolder.instance.isAppleAvailable()
    }

    if (googleAvailable || appleAvailable) {
        Spacer(Modifier.height(8.dp))
        Text("or", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
    }
    if (googleAvailable) {
        OutlinedButton(onClick = { viewModel.socialLogin("google") }, modifier = Modifier.fillMaxWidth()) {
            Text("Continue with Google")
        }
    }
    if (appleAvailable) {
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = { viewModel.socialLogin("apple") }, modifier = Modifier.fillMaxWidth()) {
            Text("Continue with Apple")
        }
    }
}

@Composable
private fun ForgotPasswordDialog(viewModel: AuthViewModel, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (sent) "Check your email" else "Forgot password") },
        text = {
            if (sent) {
                Text("If an account exists with that email, we've sent a password reset link.")
            } else {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
            }
        },
        confirmButton = {
            if (sent) {
                TextButton(onClick = onDismiss) { Text("OK") }
            } else {
                TextButton(
                    onClick = { viewModel.forgotPassword(email) { sent = true } },
                    enabled = email.isNotBlank(),
                ) { Text("Send reset link") }
            }
        },
        dismissButton = { if (!sent) TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
