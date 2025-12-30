package dev.sadakat.thinkfaster.data.repository

import android.content.Context
import android.content.SharedPreferences
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.repository.TrackedAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.core.content.edit

/**
 * Implementation of TrackedAppsRepository using SharedPreferences
 * Stores tracked app package names as a StringSet
 */
class TrackedAppsRepositoryImpl(
    context: Context,
    private val goalRepository: GoalRepository
) : TrackedAppsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val migrationPrefs: SharedPreferences = context.getSharedPreferences(MIGRATION_PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Mutex to prevent concurrent modifications
    private val mutex = Mutex()

    // State flow for reactive updates
    private val _trackedAppsFlow = MutableStateFlow<List<String>>(emptyList())

    init {
        // Perform one-time migration for existing users BEFORE loading initial state
        // This ensures the initial state is correct and prevents race conditions
        // Run the migration in a blocking manner during init to ensure data consistency
        scope.launch {
            performInitialMigration()
            // Load initial state AFTER migration completes to ensure we have correct data
            _trackedAppsFlow.value = loadTrackedApps()
        }
    }

    override suspend fun getTrackedApps(): List<String> {
        return loadTrackedApps()
    }

    override suspend fun addTrackedApp(packageName: String): Result<Unit> = mutex.withLock {
        return try {
            val current = loadTrackedApps().toMutableList()

            when {
                current.contains(packageName) -> {
                    Result.failure(Exception("App is already being tracked"))
                }
                current.size >= MAX_TRACKED_APPS -> {
                    Result.failure(Exception("Maximum $MAX_TRACKED_APPS apps allowed. Remove an app to add a new one."))
                }
                else -> {
                    current.add(packageName)
                    val saved = saveTrackedApps(current)
                    if (saved) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to save tracked apps. Please try again."))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error adding app: ${e.message}", e))
        }
    }

    override suspend fun removeTrackedApp(packageName: String): Result<Unit> = mutex.withLock {
        return try {
            val current = loadTrackedApps().toMutableList()

            if (current.remove(packageName)) {
                val saved = saveTrackedApps(current)
                if (saved) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to save tracked apps. Please try again."))
                }
            } else {
                Result.failure(Exception("App not found in tracked list"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error removing app: ${e.message}", e))
        }
    }

    override suspend fun isLimitReached(): Boolean {
        return loadTrackedApps().size >= MAX_TRACKED_APPS
    }

    override suspend fun getTrackedAppCount(): Int {
        return loadTrackedApps().size
    }

    override fun observeTrackedApps(): Flow<List<String>> {
        return _trackedAppsFlow.asStateFlow()
    }

    override suspend fun isAppTracked(packageName: String): Boolean {
        return loadTrackedApps().contains(packageName)
    }

    /**
     * Load tracked apps from SharedPreferences
     * Returns empty list if data is corrupted or missing
     */
    private fun loadTrackedApps(): List<String> {
        return try {
            prefs.getStringSet(KEY_TRACKED_APPS, null)?.toList()?.sorted() ?: emptyList()
        } catch (e: Exception) {
            // Data corrupted - log and return empty
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Save tracked apps to SharedPreferences and update flow
     * Returns true if save was successful, false otherwise
     */
    private fun saveTrackedApps(apps: List<String>): Boolean {
        return try {
            val success = prefs.edit()
                .putStringSet(KEY_TRACKED_APPS, apps.toSet())
                .commit() // Use commit() instead of apply() to verify success

            if (success) {
                // Only update flow if persistence succeeded
                _trackedAppsFlow.value = apps.sorted()
            }
            success
        } catch (e: Exception) {
            // Persistence failed
            e.printStackTrace()
            false
        }
    }

    /**
     * One-time migration: Auto-add Facebook and Instagram if they have existing goals
     * This ensures existing users don't lose their goal data
     */
    private suspend fun performInitialMigration() {
        val migrationDone = migrationPrefs.getBoolean(MIGRATION_KEY_V1, false)

        if (!migrationDone) {
            mutex.withLock {
                try {
                    // Double-check migration flag inside lock to avoid duplicate migrations
                    if (migrationPrefs.getBoolean(MIGRATION_KEY_V1, false)) {
                        return
                    }

                    // Check if user has existing goals
                    val existingGoals = goalRepository.getAllGoals()
                    val legacyPackages = existingGoals.map { it.targetApp }

                    if (legacyPackages.isNotEmpty()) {
                        // Add them to tracked apps if not already present
                        val current = loadTrackedApps().toMutableList()
                        var modified = false

                        legacyPackages.forEach { pkg ->
                            if (!current.contains(pkg) && current.size < MAX_TRACKED_APPS) {
                                current.add(pkg)
                                modified = true
                            }
                        }

                        if (modified) {
                            saveTrackedApps(current)
                        }
                    }

                    // Mark migration as complete
                    migrationPrefs.edit { putBoolean(MIGRATION_KEY_V1, true) }
                } catch (e: Exception) {
                    // Log error but don't crash - migration is best-effort
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "tracked_apps_prefs"
        private const val MIGRATION_PREFS_NAME = "tracked_apps_migration"
        private const val KEY_TRACKED_APPS = "tracked_app_list"
        private const val MIGRATION_KEY_V1 = "migration_v1_done"

        const val MAX_TRACKED_APPS = 10
    }
}
