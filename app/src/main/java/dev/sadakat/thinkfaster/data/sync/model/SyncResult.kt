package dev.sadakat.thinkfaster.data.sync.model

/**
 * Represents the result of a sync operation
 * Phase 3: Firebase Backend Implementation
 */
sealed class SyncResult<out T> {
    data class Success<T>(val data: T) : SyncResult<T>()
    data class Error(val exception: Throwable, val message: String) : SyncResult<Nothing>()
    data class Conflict<T>(
        val conflicts: List<SyncConflict<T>>,
        val remoteData: T
    ) : SyncResult<T>()
    data object InProgress : SyncResult<Nothing>()
}

/**
 * Extension to check if sync was successful
 */
fun <T> SyncResult<T>.isSuccess(): Boolean = this is SyncResult.Success

/**
 * Extension to get data if successful, null otherwise
 */
fun <T> SyncResult<T>.getOrNull(): T? = when (this) {
    is SyncResult.Success -> data
    else -> null
}

/**
 * Extension to get error if failed, null otherwise
 */
fun <T> SyncResult<T>.errorOrNull(): Throwable? = when (this) {
    is SyncResult.Error -> exception
    else -> null
}
