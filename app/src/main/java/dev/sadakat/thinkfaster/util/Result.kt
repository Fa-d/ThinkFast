package dev.sadakat.thinkfaster.util

/**
 * Wrapper type for operations that can fail
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val exception: ThinkFastException) : Result<Nothing>()
    data class Loading(val message: String? = null) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw exception
        is Loading -> throw IllegalStateException("Cannot get value while loading")
    }

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
        is Loading -> this
    }

    fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
        is Loading -> this
    }

    fun onError(action: (ThinkFastException) -> Unit): Result<T> {
        if (this is Failure) {
            action(exception)
        }
        return this
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun <T> failure(exception: ThinkFastException): Result<T> = Failure(exception)
        fun <T> loading(message: String? = null): Result<T> = Loading(message)

        fun <T> catch(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: ThinkFastException) {
            Failure(e)
        } catch (e: Exception) {
            Failure(DataAccessException(e.message ?: "Unknown error", e))
        }
    }

    fun <T> catchAndReturn(block: () -> Result<T>): Result<T> = try {
        block()
    } catch (e: ThinkFastException) {
        Failure(e)
    } catch (e: Exception) {
        Failure(DataAccessException(e.message ?: "Unknown error", e))
    }
}

/**
 * Extension function to suspend exceptions and convert to Result
 */
suspend fun <T> resultOf(block: suspend () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: ThinkFastException) {
    Result.Failure(e)
} catch (e: Exception) {
    Result.Failure(DataAccessException(e.message ?: "Unknown error", e))
}

/**
 * Safe execute helper that catches all exceptions
 */
inline fun <T> safeExecute(
    onError: (Throwable) -> Unit = {},
    block: () -> T
): T? = try {
    block()
} catch (e: Exception) {
    onError(e)
    null
}

/**
 * Safe execute for suspending functions
 */
inline suspend fun <T> safeExecuteSuspend(
    crossinline onError: (Throwable) -> Unit = {},
    crossinline block: suspend () -> T
): T? = try {
    block()
} catch (e: Exception) {
    onError(e)
    null
}
