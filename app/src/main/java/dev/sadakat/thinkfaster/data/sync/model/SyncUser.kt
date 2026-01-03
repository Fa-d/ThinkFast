package dev.sadakat.thinkfaster.data.sync.model

/**
 * Represents an authenticated user for sync purposes
 * Phase 3: Firebase Backend Implementation
 */
data class SyncUser(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val provider: AuthProvider,
    val isAnonymous: Boolean = false
)

enum class AuthProvider {
    FACEBOOK,
    GOOGLE,
    ANONYMOUS,
    EMAIL
}
