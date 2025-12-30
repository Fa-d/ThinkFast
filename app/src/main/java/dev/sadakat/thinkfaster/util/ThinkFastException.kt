package dev.sadakat.thinkfaster.util

/**
 * Base exception for ThinkFast app errors
 */
sealed class ThinkFastException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Permission related errors
 */
class PermissionDeniedException(
    permission: String,
    cause: Throwable? = null
) : ThinkFastException("Required permission denied: $permission", cause)

class PermissionRationaleRequiredException(
    permission: String,
    message: String = "Permission requires user rationale"
) : ThinkFastException("$message: $permission")

/**
 * Data access errors
 */
class DataAccessException(message: String, cause: Throwable? = null) : ThinkFastException(message, cause)

class SessionNotFoundException(sessionId: Long) : ThinkFastException("Session not found: $sessionId")

class GoalNotFoundException(appPackage: String) : ThinkFastException("Goal not found for app: $appPackage")

/**
 * Service errors
 */
class ServiceInitializationException(message: String, cause: Throwable? = null) : ThinkFastException(message, cause)

class ServiceStartException(message: String, cause: Throwable? = null) : ThinkFastException(message, cause)

/**
 * App detection errors
 */
class AppDetectionException(message: String, cause: Throwable? = null) : ThinkFastException(message, cause)

class UsageStatsManagerNotAvailableException : ThinkFastException("UsageStatsManager not available")

/**
 * Database errors
 */
class DatabaseException(message: String, cause: Throwable? = null) : ThinkFastException("Database error: $message", cause)

class DataValidationException(message: String) : ThinkFastException("Data validation failed: $message")

/**
 * Session errors
 */
class SessionException(message: String, cause: Throwable? = null) : ThinkFastException(message, cause)

class SessionEndException(message: String, cause: Throwable? = null) : ThinkFastException("Failed to end session: $message", cause)

/**
 * File/IO errors
 */
class FileOperationException(message: String, cause: Throwable? = null) : ThinkFastException("File operation failed: $message", cause)

/**
 * Network errors (if app gets network features)
 */
class NetworkException(message: String, cause: Throwable? = null) : ThinkFastException("Network error: $message", cause)
