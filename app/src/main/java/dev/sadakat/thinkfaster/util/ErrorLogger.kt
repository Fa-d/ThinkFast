package dev.sadakat.thinkfaster.util

import android.util.Log
import dev.sadakat.thinkfaster.BuildConfig

/**
 * Centralized error logging utility
 */
object ErrorLogger {

    private const val TAG = "Intently"

    /**
     * Log an error with exception details
     */
    fun error(
        exception: Throwable,
        message: String? = null,
        context: String? = null
    ) {
        val logMessage = buildLogMessage(message, context)
        try {
            when {
                BuildConfig.DEBUG -> {
                    Log.e(TAG, logMessage, exception)
                    exception.printStackTrace()
                }
                else -> {
                    // In production, only log the message without stack trace
                    Log.e(TAG, "$logMessage: ${exception.javaClass.simpleName}: ${exception.message}")
                }
            }
        } catch (e: RuntimeException) {
            // Log not mocked in unit tests - silently ignore
            // In tests, we can rely on the test output instead
        }
    }

    /**
     * Log a warning
     */
    fun warning(
        message: String,
        context: String? = null
    ) {
        val logMessage = buildLogMessage(message, context)
        try {
            Log.w(TAG, logMessage)
        } catch (e: RuntimeException) {
            // Log not mocked in unit tests - silently ignore
        }
    }

    /**
     * Log an info message
     */
    fun info(
        message: String,
        context: String? = null
    ) {
        val logMessage = buildLogMessage(message, context)
        try {
            Log.i(TAG, logMessage)
        } catch (e: RuntimeException) {
            // Log not mocked in unit tests - silently ignore
        }
    }

    /**
     * Log a debug message (only in debug builds)
     */
    fun debug(
        message: String,
        context: String? = null
    ) {
        if (BuildConfig.DEBUG) {
            val logMessage = buildLogMessage(message, context)
            try {
                Log.d(TAG, logMessage)
            } catch (e: RuntimeException) {
                // Log not mocked in unit tests - silently ignore
            }
        }
    }

    /**
     * Build a consistent log message format
     */
    private fun buildLogMessage(message: String?, context: String?): String {
        return buildString {
            if (context != null) {
                append("[$context] ")
            }
            if (message != null) {
                append(message)
            }
        }
    }

    /**
     * Log a IntentlyException with appropriate context
     */
    fun logError(exception: IntentlyException, context: String? = null) {
        val message = when (exception) {
            is PermissionDeniedException -> "Permission denied for ${exception.message}"
            is DataAccessException -> "Data access error: ${exception.message}"
            is ServiceInitializationException -> "Service initialization error: ${exception.message}"
            is AppDetectionException -> "App detection error: ${exception.message}"
            is DatabaseException -> "Database error: ${exception.message}"
            is SessionException -> "Session error: ${exception.message}"
            else -> "Error: ${exception.message}"
        }
        error(exception, message, context)
    }
}

/**
 * Extension function to log Result failures
 */
fun <T> dev.sadakat.thinkfaster.util.Result<T>.logError(context: String? = null): dev.sadakat.thinkfaster.util.Result<T> {
    if (this is dev.sadakat.thinkfaster.util.Result.Failure) {
        ErrorLogger.logError(exception, context)
    }
    return this
}

/**
 * Extension function to log exceptions inline
 */
fun Throwable.logError(message: String? = null, context: String? = null) {
    if (this is IntentlyException) {
        ErrorLogger.logError(this, context)
    } else {
        ErrorLogger.error(this, message, context)
    }
}

/**
 * Extension function to safely execute and log errors
 */
inline fun <T> safeCall(
    context: String? = null,
    block: () -> T
): T? = try {
    block()
} catch (e: Exception) {
    e.logError(context = context)
    null
}

/**
 * Extension function to safely execute suspending functions and log errors
 */
inline suspend fun <T> safeCallSuspend(
    context: String? = null,
    crossinline block: suspend () -> T
): T? = try {
    block()
} catch (e: Exception) {
    e.logError(context = context)
    null
}
