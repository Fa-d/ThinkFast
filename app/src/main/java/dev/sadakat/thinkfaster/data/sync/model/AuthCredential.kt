package dev.sadakat.thinkfaster.data.sync.model

/**
 * Represents authentication credentials for different providers
 * Phase 3: Firebase Backend Implementation
 */
sealed class AuthCredential {
    data class Facebook(val accessToken: String) : AuthCredential()
    data class Google(val idToken: String) : AuthCredential()
    data class Email(val email: String, val password: String) : AuthCredential()
}
