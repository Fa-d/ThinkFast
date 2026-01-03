package dev.sadakat.thinkfaster.data.sync.model

/**
 * Represents the current state of sync
 */
data class SyncStatus(
    val state: SyncState,
    val lastSyncTime: Long,
    val pendingChanges: Int = 0,
    val errorMessage: String? = null
)

/**
 * Possible sync states
 */
enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    CONFLICT
}
