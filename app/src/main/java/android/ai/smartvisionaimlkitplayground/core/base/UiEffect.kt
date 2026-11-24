package android.ai.smartvisionaimlkitplayground.core.base

/**
 * Marker interface representing one-time side-effects (e.g., Navigation, SnackBar, Toast, Dialogs)
 * dispatched from the ViewModel to the UI.
 *
 * Side-effects should be consumed exactly once and must not be re-triggered during recomposition or configuration change.
 */
interface UiEffect
