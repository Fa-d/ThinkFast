package dev.sadakat.thinkfaster.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfaster.data.preferences.SyncPreferences
import dev.sadakat.thinkfaster.data.sync.auth.FacebookLoginHelper
import dev.sadakat.thinkfaster.data.sync.auth.GoogleSignInHelper
import dev.sadakat.thinkfaster.data.sync.backend.SyncBackend
import dev.sadakat.thinkfaster.util.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * AccountManagementViewModel - Manages account and sync settings
 * Phase 5: UI Integration
 *
 * Handles sign out, account deletion, and sync management
 */
class AccountManagementViewModel(
    private val syncPreferences: SyncPreferences,
    private val syncBackend: SyncBackend,
    private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AccountManagementUiState())
    val uiState: StateFlow<AccountManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadAccountInfo()
    }
    
    private fun loadAccountInfo() {
        viewModelScope.launch {
            val userId = syncPreferences.getUserId() ?: "Unknown"
            var email = syncPreferences.getUserEmail()
            var provider = syncPreferences.getUserProvider()

            // If provider or email is missing from preferences, try to fetch from backend
            // This handles the case where user logged in before we added these fields
            if (email == null || provider == null) {
                try {
                    val currentUser = syncBackend.getCurrentUser()
                    if (currentUser != null) {
                        // Update preferences with the fetched data
                        if (email == null) {
                            email = currentUser.email
                            syncPreferences.setUserEmail(email)
                        }
                        if (provider == null) {
                            provider = currentUser.provider.name
                            syncPreferences.setUserProvider(provider)
                        }
                    }
                } catch (e: Exception) {
                    // Silently fail - user will see "Unknown" for provider
                }
            }

            val lastSyncTime = syncPreferences.getLastSyncTime()
            val syncStatus = syncPreferences.getSyncStatus()
            val syncError = syncPreferences.getLastSyncError()
            val pendingChanges = syncPreferences.getPendingChangesCount()
            val autoSyncEnabled = syncPreferences.isAutoSyncEnabled()

            val lastSyncFormatted = if (lastSyncTime > 0) {
                val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                sdf.format(Date(lastSyncTime))
            } else {
                "Never"
            }

            _uiState.value = AccountManagementUiState(
                userId = userId,
                email = email,
                provider = provider ?: "Unknown",
                lastSyncTime = lastSyncFormatted,
                syncStatus = syncStatus,
                syncError = syncError,
                pendingChanges = pendingChanges,
                autoSyncEnabled = autoSyncEnabled
            )
        }
    }
    
    /**
     * Trigger manual sync
     */
    fun triggerManualSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncStatus = "SYNCING")
            syncPreferences.setSyncStatus("SYNCING")

            try {
                // Trigger immediate sync
                SyncScheduler.triggerImmediateSync(context)

                // Poll sync status until it changes from SYNCING
                // This ensures UI updates when sync actually completes
                var pollCount = 0
                val maxPolls = 60  // Max 30 seconds (60 * 500ms)

                while (pollCount < maxPolls) {
                    kotlinx.coroutines.delay(500)
                    val currentStatus = syncPreferences.getSyncStatus()

                    if (currentStatus != "SYNCING") {
                        // Sync completed (either SUCCESS or ERROR)
                        loadAccountInfo()
                        break
                    }

                    pollCount++
                }

                // If still syncing after max polls, reload anyway
                if (pollCount >= maxPolls) {
                    loadAccountInfo()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    syncStatus = "ERROR",
                    syncError = e.message
                )
            }
        }
    }
    
    /**
     * Toggle auto-sync
     */
    fun toggleAutoSync() {
        val newValue = !_uiState.value.autoSyncEnabled
        syncPreferences.setAutoSyncEnabled(newValue)
        
        if (newValue && syncPreferences.isAuthenticated()) {
            SyncScheduler.schedulePeriodicSync(context)
        } else {
            SyncScheduler.cancelPeriodicSync(context)
        }
        
        _uiState.value = _uiState.value.copy(autoSyncEnabled = newValue)
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

                // Cancel sync worker
                SyncScheduler.cancelPeriodicSync(context)

                // Clear sync data
                syncPreferences.clearSyncData()

                // Reload UI state
                loadAccountInfo()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    syncError = "Failed to sign out: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Delete account data from cloud
     */
    fun deleteAccountData() {
        viewModelScope.launch {
            try {
                // TODO: Delete account data when backend is ready
                // authRepository.deleteAccountData()
                
                // For now, just sign out
                signOut()
                
                _uiState.value = _uiState.value.copy(
                    syncError = "Account deletion not yet implemented"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    syncError = "Failed to delete account: ${e.message}"
                )
            }
        }
    }
}

/**
 * UI state for account management screen
 */
data class AccountManagementUiState(
    val userId: String = "",
    val email: String? = null,
    val provider: String = "",
    val lastSyncTime: String = "Never",
    val syncStatus: String = "IDLE",
    val syncError: String? = null,
    val pendingChanges: Int = 0,
    val autoSyncEnabled: Boolean = true
)
