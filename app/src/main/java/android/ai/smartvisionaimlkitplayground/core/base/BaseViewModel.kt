package android.ai.smartvisionaimlkitplayground.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel class implementing the MVI (Model-View-Intent) architectural pattern.
 *
 * @param S The concrete [UiState] representing the immutable UI state.
 * @param I The concrete [UiIntent] representing user actions/intentions.
 * @param E The concrete [UiEffect] representing one-time side-effects (navigation, snackbars, etc.).
 * @property initialState The initial state for the screen.
 */
abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    val currentState: S
        get() = _uiState.value

    private val _uiEffect = Channel<E>(capacity = Channel.BUFFERED)
    val uiEffect: Flow<E> = _uiEffect.receiveAsFlow()

    /**
     * Entry point for processing user actions/intents dispatched from Compose UI.
     */
    fun sendIntent(intent: I) {
        handleIntent(intent)
    }

    /**
     * Subclasses must override this method to handle incoming [UiIntent]s.
     */
    protected abstract fun handleIntent(intent: I)

    /**
     * Safely and atomically updates the current [UiState] using a reducer function.
     * Uses atomic Compare-And-Swap (CAS) to prevent race conditions during concurrent mutations.
     */
    protected fun setState(reducer: S.() -> S) {
        _uiState.update { it.reducer() }
    }

    /**
     * Dispatches a one-time side-effect to the UI via buffered Channel.
     */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
