package dev.sadakat.thinkfaster.data.sync.model

/**
 * Represents a sync conflict between local and remote data
 * Phase 3: Firebase Backend Implementation
 */
data class SyncConflict<T>(
    val localData: T,
    val remoteData: T,
    val localTimestamp: Long,
    val remoteTimestamp: Long,
    val conflictType: ConflictType
)

enum class ConflictType {
    /**
     * Both local and remote data were modified since last sync
     */
    CONCURRENT_MODIFICATION,

    /**
     * Local data was deleted but remote was modified
     */
    LOCAL_DELETED_REMOTE_MODIFIED,

    /**
     * Remote data was deleted but local was modified
     */
    REMOTE_DELETED_LOCAL_MODIFIED,

    /**
     * Data exists in both but with different values
     */
    VALUE_MISMATCH
}

/**
 * Resolution strategy for conflicts
 */
enum class ConflictResolutionStrategy {
    /**
     * Keep the version with the latest timestamp
     */
    LAST_WRITE_WINS,

    /**
     * Keep local version
     */
    LOCAL_WINS,

    /**
     * Keep remote version
     */
    REMOTE_WINS,

    /**
     * Merge both versions (custom logic per entity type)
     */
    MERGE
}
