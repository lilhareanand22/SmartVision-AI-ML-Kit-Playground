package android.ai.smartvisionaimlkitplayground.core.base

/**
 * Sealed interface representing the state/result of an ML Kit or asynchronous operation.
 * Used across Data, Domain, and Presentation layers to provide safe, deterministic outcomes.
 */
sealed interface MlKitResult<out T> {

    /**
     * Represents the initial uninitialized state before an analysis has begun.
     */
    data object Idle : MlKitResult<Nothing>

    /**
     * Represents an active asynchronous analysis operation.
     */
    data object Loading : MlKitResult<Nothing>

    /**
     * Represents a successfully completed ML analysis containing [data].
     */
    data class Success<out T>(val data: T) : MlKitResult<T>

    /**
     * Represents a failed analysis containing the [exception] and descriptive [message].
     */
    data class Error(
        val exception: Throwable,
        val message: String = exception.localizedMessage ?: "An unexpected ML Kit error occurred"
    ) : MlKitResult<Nothing>
}

/**
 * Returns `true` if the result is [MlKitResult.Loading].
 */
val <T> MlKitResult<T>.isLoading: Boolean
    get() = this is MlKitResult.Loading

/**
 * Returns `true` if the result is [MlKitResult.Success].
 */
val <T> MlKitResult<T>.isSuccess: Boolean
    get() = this is MlKitResult.Success

/**
 * Returns `true` if the result is [MlKitResult.Error].
 */
val <T> MlKitResult<T>.isError: Boolean
    get() = this is MlKitResult.Error

/**
 * Returns data if [MlKitResult.Success], otherwise `null`.
 */
fun <T> MlKitResult<T>.getOrNull(): T? {
    return when (this) {
        is MlKitResult.Success -> data
        else -> null
    }
}

/**
 * Returns data if [MlKitResult.Success], otherwise [defaultValue].
 */
fun <T> MlKitResult<T>.getOrDefault(defaultValue: T): T {
    return when (this) {
        is MlKitResult.Success -> data
        else -> defaultValue
    }
}

/**
 * Transforms the wrapped data in [MlKitResult.Success] using [transform].
 */
inline fun <T, R> MlKitResult<T>.map(transform: (T) -> R): MlKitResult<R> {
    return when (this) {
        is MlKitResult.Success -> MlKitResult.Success(transform(data))
        is MlKitResult.Error -> this
        is MlKitResult.Loading -> MlKitResult.Loading
        is MlKitResult.Idle -> MlKitResult.Idle
    }
}

/**
 * Executes [action] when the result is [MlKitResult.Success].
 */
inline fun <T> MlKitResult<T>.onSuccess(action: (data: T) -> Unit): MlKitResult<T> {
    if (this is MlKitResult.Success) {
        action(data)
    }
    return this
}

/**
 * Executes [action] when the result is [MlKitResult.Error].
 */
inline fun <T> MlKitResult<T>.onError(action: (exception: Throwable, message: String) -> Unit): MlKitResult<T> {
    if (this is MlKitResult.Error) {
        action(exception, message)
    }
    return this
}
