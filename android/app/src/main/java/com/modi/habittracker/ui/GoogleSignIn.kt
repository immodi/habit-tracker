package com.modi.habittracker.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.modi.habittracker.R

class GoogleSignInException(message: String) : Exception(message)

/** Requests a Google ID token with Credential Manager, then signs in to Firebase with it. */
suspend fun fetchGoogleIdToken(context: Context): String {
    val credentialManager = CredentialManager.create(context)
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build(),
        )
        .build()

    return try {
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } else {
            throw GoogleSignInException("Google sign-in returned an unexpected credential.")
        }
    } catch (error: NoCredentialException) {
        throw GoogleSignInException("No Google account was found on this device.")
    } catch (error: GetCredentialException) {
        throw GoogleSignInException(error.message ?: "Google sign-in did not finish.")
    }
}
