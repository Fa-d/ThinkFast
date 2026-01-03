package dev.sadakat.thinkfaster.data.sync.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import dev.sadakat.thinkfaster.BuildConfig

/**
 * Google Sign-In Helper
 * Handles Google OAuth authentication for Supabase integration
 *
 * IMPORTANT: Before using, configure Google OAuth credentials:
 * 1. Go to Google Cloud Console (console.cloud.google.com)
 * 2. Navigate to APIs & Services → Credentials
 * 3. Find your OAuth 2.0 Web Client (the one configured for Supabase)
 * 4. Copy the Client ID (ends with .apps.googleusercontent.com)
 * 5. Add to keystore.properties: google_web_client_id=your_client_id
 */
object GoogleSignInHelper {

    /**
     * Get Google Web Client ID from BuildConfig
     * Loaded from keystore.properties for security
     */
    private fun getWebClientId(): String {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank() || clientId == "\"\"") {
            throw IllegalStateException(
                "Google Web Client ID not configured. " +
                "Please add 'google_web_client_id=your_client_id' to keystore.properties"
            )
        }
        return clientId.removeSurrounding("\"")
    }

    /**
     * Get Google Sign-In intent for launching the authentication flow
     *
     * @param context Application context
     * @return Intent to launch Google Sign-In
     */
    fun getSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getWebClientId())
            .requestEmail()
            .requestProfile()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        return googleSignInClient.signInIntent
    }

    /**
     * Handle the result from Google Sign-In activity
     *
     * @param data Intent data from the activity result
     * @return Google ID token (required for Supabase), or null if sign-in failed
     * @throws ApiException if sign-in fails
     */
    fun handleSignInResult(data: Intent): String? {
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken // This is what Supabase needs
        } catch (e: ApiException) {
            // Sign-in failed, return null
            null
        }
    }

    /**
     * Get the currently signed-in Google account
     *
     * @param context Application context
     * @return GoogleSignInAccount if user is signed in, null otherwise
     */
    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Sign out from Google
     *
     * @param context Application context
     * @param onComplete Callback when sign-out completes
     */
    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getWebClientId())
            .requestEmail()
            .build()

        val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            onComplete()
        }
    }

    /**
     * Revoke access (disconnects the app from the user's Google account)
     * Use this for complete account deletion
     *
     * @param context Application context
     * @param onComplete Callback when revoke completes
     */
    fun revokeAccess(context: Context, onComplete: () -> Unit = {}) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getWebClientId())
            .requestEmail()
            .build()

        val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInClient.revokeAccess().addOnCompleteListener {
            onComplete()
        }
    }

    /**
     * Helper function to remove surrounding quotes from string
     */
    private fun String.removeSurrounding(delimiter: String): String {
        return if (startsWith(delimiter) && endsWith(delimiter)) {
            substring(delimiter.length, length - delimiter.length)
        } else {
            this
        }
    }
}
