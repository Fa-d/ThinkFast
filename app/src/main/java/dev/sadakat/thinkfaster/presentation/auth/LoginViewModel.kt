package dev.sadakat.thinkfaster.presentation.auth

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.CallbackManager
import dev.sadakat.thinkfaster.data.preferences.SyncPreferences
import dev.sadakat.thinkfaster.data.sync.SyncCoordinator
import dev.sadakat.thinkfaster.data.sync.auth.FacebookLoginHelper
import dev.sadakat.thinkfaster.data.sync.auth.GoogleSignInHelper
import dev.sadakat.thinkfaster.data.sync.backend.SyncBackend
import dev.sadakat.thinkfaster.data.sync.model.AuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * LoginViewModel - Handles authentication logic
 * Phase 5: UI Integration
 *
 * Manages sign-in flow with Facebook/Google
 * Now wired up to Supabase backend
 */
class LoginViewModel(
    private val syncPreferences: SyncPreferences,
    private val syncBackend: SyncBackend,
    private val syncCoordinator: SyncCoordinator,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Check if already authenticated
        _uiState.value = _uiState.value.copy(
            isAuthenticated = syncPreferences.isAuthenticated()
        )
    }

    /**
     * Handle Facebook login result
     * Call this from LoginScreen when Facebook login completes
     *
     * @param accessToken Facebook access token from FacebookLoginHelper
     */
    fun handleFacebookLoginResult(accessToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Sign in with Supabase using Facebook access token
                val result = syncBackend.signIn(AuthCredential.Facebook(accessToken))

                if (result.isSuccess) {
                    val user = result.getOrNull()!!
                    syncPreferences.setUserId(user.userId)
                    syncPreferences.setUserEmail(user.email)
                    syncPreferences.setUserProvider(user.provider.name)
                    syncPreferences.setAuthenticated(true)
                    syncPreferences.setSyncEnabled(true)

                    // Perform initial sync to upload local data to cloud
                    syncCoordinator.performInitialSync(user.userId)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                } else {
                    throw result.exceptionOrNull() ?: Exception("Facebook sign-in failed")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to sign in with Facebook"
                )
            }
        }
    }

    /**
     * Handle Facebook login error
     * Call this from LoginScreen when Facebook login fails
     */
    fun handleFacebookLoginError(error: Exception) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = "Facebook login failed: ${error.message}"
        )
    }

    /**
     * Handle Facebook login cancellation
     * Call this from LoginScreen when user cancels Facebook login
     */
    fun handleFacebookLoginCancel() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null
        )
    }

    /**
     * Get Google Sign-In intent
     * Call this from LoginScreen to launch Google Sign-In
     *
     * @return Intent to launch Google Sign-In activity
     */
    fun getGoogleSignInIntent(): Intent {
        return GoogleSignInHelper.getSignInIntent(context)
    }

    /**
     * Handle Google Sign-In result
     * Call this from LoginScreen when Google Sign-In completes
     *
     * @param data Intent data from activity result
     */
    fun handleGoogleSignInResult(data: Intent) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Extract ID token from Google Sign-In result
                val idToken = GoogleSignInHelper.handleSignInResult(data)
                    ?: throw Exception("No ID token received from Google")

                // Sign in with Supabase using Google ID token
                val result = syncBackend.signIn(AuthCredential.Google(idToken))

                if (result.isSuccess) {
                    val user = result.getOrNull()!!
                    syncPreferences.setUserId(user.userId)
                    syncPreferences.setUserEmail(user.email)
                    syncPreferences.setUserProvider(user.provider.name)
                    syncPreferences.setAuthenticated(true)
                    syncPreferences.setSyncEnabled(true)

                    // Perform initial sync to upload local data to cloud
                    syncCoordinator.performInitialSync(user.userId)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                } else {
                    throw result.exceptionOrNull() ?: Exception("Google sign-in failed")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to sign in with Google"
                )
            }
        }
    }

    /**
     * Sign out
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                // Sign out from auth provider (Supabase)
                syncBackend.signOut()

                // Clear Google Sign-In cached credentials
                // This ensures the account picker is shown next time
                GoogleSignInHelper.signOut(context) {}

                // Clear Facebook Login cached credentials
                FacebookLoginHelper.logout()

                // Clear all sync data (including email, provider)
                syncPreferences.clearSyncData()

                _uiState.value = _uiState.value.copy(
                    isAuthenticated = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to sign out: ${e.message}"
                )
            }
        }
    }
}

/**
 * UI state for login screen
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null
)
