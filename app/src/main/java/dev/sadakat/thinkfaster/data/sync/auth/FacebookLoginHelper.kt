package dev.sadakat.thinkfaster.data.sync.auth

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

/**
 * Facebook Login Helper
 * Handles Facebook OAuth authentication for Supabase integration
 *
 * IMPORTANT: Before using, ensure Facebook SDK is initialized:
 * 1. Add Facebook App ID to strings.xml
 * 2. Initialize Facebook SDK in ThinkFasterApplication.onCreate()
 * 3. Add Facebook meta-data to AndroidManifest.xml
 *
 * See AUTHENTICATION_STATUS.md for detailed setup instructions
 */
object FacebookLoginHelper {

    /**
     * Create a callback manager for Facebook login
     * This should be created in the Activity/Fragment
     */
    fun createCallbackManager(): CallbackManager {
        return CallbackManager.Factory.create()
    }

    /**
     * Login with Facebook using Activity Result API
     *
     * @param activity The ComponentActivity to launch Facebook login from
     * @param callbackManager Facebook callback manager
     * @param onSuccess Callback with Facebook access token when login succeeds
     * @param onError Callback when login fails
     * @param onCancel Callback when user cancels
     */
    fun login(
        activity: ComponentActivity,
        callbackManager: CallbackManager,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val loginManager = LoginManager.getInstance()

        loginManager.registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    // Get the access token - this is what Supabase needs
                    val accessToken = result.accessToken.token
                    onSuccess(accessToken)
                }

                override fun onCancel() {
                    onCancel()
                }

                override fun onError(error: FacebookException) {
                    onError(error)
                }
            }
        )

        // Launch Facebook login with required permissions
        loginManager.logInWithReadPermissions(
            activity,
            listOf("email", "public_profile")
        )
    }

    /**
     * Logout from Facebook
     */
    fun logout() {
        LoginManager.getInstance().logOut()
    }

    /**
     * Check if user is currently logged in with Facebook
     *
     * @return true if logged in, false otherwise
     */
    fun isLoggedIn(): Boolean {
        val accessToken = com.facebook.AccessToken.getCurrentAccessToken()
        return accessToken != null && !accessToken.isExpired
    }

    /**
     * Get current Facebook access token
     *
     * @return Access token string if logged in, null otherwise
     */
    fun getCurrentAccessToken(): String? {
        val accessToken = com.facebook.AccessToken.getCurrentAccessToken()
        return if (accessToken != null && !accessToken.isExpired) {
            accessToken.token
        } else {
            null
        }
    }
}
