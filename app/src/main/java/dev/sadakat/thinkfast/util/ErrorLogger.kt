package dev.sadakat.thinkfast.util

import android.util.Log
import dev.sadakat.thinkfast.BuildConfig

/**
 * Centralized error logging utility
 */
object ErrorLogger {

    private const val TAG = "ThinkFast"

    /**
     * Log an error with exception details
     */
    fun error(
        exception: Throwable,
        message: String? = null,
        context: String? = null
    ) {
        val logMessage = buildLogMessage(message, context)
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
    }

    /**
     * Log a warning
     */
    fun warning(
        message: String,
        context: String? = null
    ) {
        val logMessage = buildLogMessage(message, context)
        Log.w(TAG, logMessage)
    }

    /**
     * Log an info message
     */
    fun info(
        message: String,
        context: String? = null
    ) {
        val logMessage = buildLogMessage(message, context)
        Log.i(TAG, logMessage)
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
            Log.d(TAG, logMessage)
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
     * Log a ThinkFastException with appropriate context
     */
    fun logError(exception: ThinkFastException, context: String? = null) {
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
fun <T> dev.sadakat.thinkfast.util.Result<T>.logError(context: String? = null): dev.sadakat.thinkfast.util.Result<T> {
    if (this is dev.sadakat.thinkfast.util.Result.Failure) {
        ErrorLogger.logError(exception, context)
    }
    return this
}

/**
 * Extension function to log exceptions inline
 */
fun Throwable.logError(message: String? = null, context: String? = null) {
    if (this is ThinkFastException) {
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
