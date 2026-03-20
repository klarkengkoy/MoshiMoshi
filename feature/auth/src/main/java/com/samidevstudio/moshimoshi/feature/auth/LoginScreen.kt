package com.samidevstudio.moshimoshi.feature.auth

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to MoshiMoshi",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                scope.launch {
                    signIn(context, credentialManager, onLoginSuccess)
                }
            }
        ) {
            Text("Sign In")
        }
    }
}

private suspend fun signIn(
    context: Context,
    credentialManager: CredentialManager,
    onLoginSuccess: () -> Unit
) {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId("YOUR_SERVER_CLIENT_ID") // You'll need to set this up in Google Cloud Console
        .build()

    val passwordOption = GetPasswordOption()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .addCredentialOption(passwordOption)
        .build()

    try {
        val result = credentialManager.getCredential(
            context = context,
            request = request
        )
        handleSignIn(result)
        onLoginSuccess()
    } catch (e: GetCredentialException) {
        Log.e("Auth", "Error getting credential", e)
    }
}

private fun handleSignIn(result: GetCredentialResponse) {
    val credential = result.credential
    // Handle the specific credential type (Password, GoogleId, etc.)
    Log.d("Auth", "Signed in with: ${credential.type}")
}
