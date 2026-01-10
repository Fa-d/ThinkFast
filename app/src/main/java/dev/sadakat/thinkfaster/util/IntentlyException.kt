package dev.sadakat.thinkfaster.util

/**
 * Base exception for Intently app errors
 */
sealed class IntentlyException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Permission related errors
 */
class PermissionDeniedException(
    permission: String,
    cause: Throwable? = null
) : IntentlyException("Required permission denied: $permission", cause)

class PermissionRationaleRequiredException(
    permission: String,
    message: String = "Permission requires user rationale"
) : IntentlyException("$message: $permission")

/**
 * Data access errors
 */
class DataAccessException(message: String, cause: Throwable? = null) : IntentlyException(message, cause)

class SessionNotFoundException(sessionId: Long) : IntentlyException("Session not found: $sessionId")

class GoalNotFoundException(appPackage: String) : IntentlyException("Goal not found for app: $appPackage")

/**
 * Service errors
 */
class ServiceInitializationException(message: String, cause: Throwable? = null) : IntentlyException(message, cause)

class ServiceStartException(message: String, cause: Throwable? = null) : IntentlyException(message, cause)

/**
 * App detection errors
 */
class AppDetectionException(message: String, cause: Throwable? = null) : IntentlyException(message, cause)

class UsageStatsManagerNotAvailableException : IntentlyException("UsageStatsManager not available")

/**
 * Database errors
 */
class DatabaseException(message: String, cause: Throwable? = null) : IntentlyException("Database error: $message", cause)

class DataValidationException(message: String) : IntentlyException("Data validation failed: $message")

/**
 * Session errors
 */
class SessionException(message: String, cause: Throwable? = null) : IntentlyException(message, cause)

class SessionEndException(message: String, cause: Throwable? = null) : IntentlyException("Failed to end session: $message", cause)

/**
 * File/IO errors
 */
class FileOperationException(message: String, cause: Throwable? = null) : IntentlyException("File operation failed: $message", cause)

/**
 * Network errors (if app gets network features)
 */
class NetworkException(message: String, cause: Throwable? = null) : IntentlyException("Network error: $message", cause)
