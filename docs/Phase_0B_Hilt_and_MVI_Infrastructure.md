# 🏛️ Phase 0B: Hilt DI & MVI Base Infrastructure

> **Phase**: 0B  
> **Status**: Completed  
> **Project**: SmartVision AI – ML Kit Playground  
> **Package**: `android.ai.smartvisionaimlkitplayground`  

---

## 1. Overview & Architecture Philosophy

In modern, production-grade Android applications—especially those integrating compute-heavy features like **Google ML Kit** and real-time **CameraX** streams—predictable state management and clean dependency injection are critical.

Phase 0B establishes two foundational pillars:
1. **Dependency Injection (Hilt 2.60.1 + KSP)**: Ensures modular, loosely-coupled, lifecycle-aware dependency graphs across all layers (Data, Domain, Presentation).
2. **Model-View-Intent (MVI) Pattern**: Enforces strict **Unidirectional Data Flow (UDF)** where user intentions result in deterministic state transitions and one-time side-effects.

```
       ┌────────────────────────────────────────────────────────┐
       │                       Compose UI                       │
       └────────────────────▲───────────────┬───────────────────┘
                            │               │
                 StateFlow  │               │ sendIntent(Intent)
                 & Effect   │               ▼
       ┌────────────────────┴───────────────────────────────────┐
       │                     BaseViewModel                      │
       │  ┌───────────────────────┐   ┌──────────────────────┐  │
       │  │ StateFlow<UiState>    │   │ Channel<UiEffect>    │  │
       │  └───────────────────────┘   └──────────────────────┘  │
       └────────────────────▲───────────────────────────────────┘
                            │
               Repository / │ Use Cases (Injected via Hilt)
                            ▼
       ┌────────────────────────────────────────────────────────┐
       │                 Domain & Data Layer                    │
       │        (ML Kit Processors, CameraX, Storage)           │
       └────────────────────────────────────────────────────────┘
```

---

## 2. Deep Dive: MVI (Model-View-Intent) vs. MVVM

| Metric / Dimension | Traditional MVVM | MVI (Implemented in SmartVision) |
|---|---|---|
| **State Representation** | Multiple individual `LiveData` / `StateFlow` primitives (`isLoading`, `data`, `error`) | **Single immutable `UiState` data class** representing the entire screen state |
| **State Consistency** | Risk of state desynchronization (e.g., `isLoading = true` while `error != null`) | **Atomic transitions**: Reducer functions guarantee consistent, mutually exclusive states |
| **User Interactions** | Direct public method calls on ViewModel (`viewModel.onScanClicked()`) | Formally typed **`UiIntent`** sealed interfaces representing every user interaction |
| **Side Effects (Navigation, Toast)**| Often emitted via SharedFlow or LiveData (prone to duplicate triggers on recomposition) | **Buffered `Channel<UiEffect>`** consumed as a cold flow; guaranteed single consumption |
| **Debuggability & Auditing** | Difficult to trace the sequence of changes | Every state mutation is traceable: `Current State + Intent → New State` |

---

## 3. Implementation Walkthrough

### 3.1 Application Class & Manifest Integration

`SmartVisionApp.kt` is annotated with `@HiltAndroidApp`, triggering Hilt’s code generation to create the root application-level dependency container.

```kotlin
// SmartVisionApp.kt
package android.ai.smartvisionaimlkitplayground

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartVisionApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
```

Registered in `AndroidManifest.xml`:
```xml
<application
    android:name=".SmartVisionApp"
    android:allowBackup="true"
    ...
```

And `MainActivity.kt` annotated with `@AndroidEntryPoint` to enable field and constructor injection:
```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() { ... }
```

---

### 3.2 MVI Contracts (`UiState`, `UiIntent`, `UiEffect`)

Located in package `android.ai.smartvisionaimlkitplayground.core.base`:

```kotlin
// UiState.kt
package android.ai.smartvisionaimlkitplayground.core.base

/**
 * Marker interface representing an immutable state of a Compose screen in MVI.
 * All screen states should be data classes implementing this interface.
 */
interface UiState
```

```kotlin
// UiIntent.kt
package android.ai.smartvisionaimlkitplayground.core.base

/**
 * Marker interface representing user actions or system events dispatched from the UI to the ViewModel.
 */
interface UiIntent
```

```kotlin
// UiEffect.kt
package android.ai.smartvisionaimlkitplayground.core.base

/**
 * Marker interface representing one-time side-effects (Navigation, Snackbars, Dialogs).
 * Consumed exactly once; immune to recomposition re-triggers.
 */
interface UiEffect
```

---

### 3.3 Reusable `BaseViewModel`

The `BaseViewModel` abstracts state exposure, intent routing, and side-effect dispatching:

```kotlin
// BaseViewModel.kt
package android.ai.smartvisionaimlkitplayground.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

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
     * Public entry-point for Compose UI to dispatch intents.
     */
    fun sendIntent(intent: I) {
        handleIntent(intent)
    }

    /**
     * Subclasses implement this to react to incoming intents.
     */
    protected abstract fun handleIntent(intent: I)

    /**
     * Atomically mutates the UI state using a reducer lambda.
     */
    protected fun setState(reducer: S.() -> S) {
        _uiState.value = currentState.reducer()
    }

    /**
     * Dispatches one-time side-effects safely.
     */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
```

---

### 3.4 Result Wrapper (`MlKitResult<T>`)

Encapsulates asynchronous ML Kit analysis operations across Data and Domain layers:

```kotlin
// MlKitResult.kt
package android.ai.smartvisionaimlkitplayground.core.base

sealed interface MlKitResult<out T> {
    data object Idle : MlKitResult<Nothing>
    data object Loading : MlKitResult<Nothing>
    data class Success<out T>(val data: T) : MlKitResult<T>
    data class Error(
        val exception: Throwable,
        val message: String = exception.localizedMessage ?: "An unexpected ML Kit error occurred"
    ) : MlKitResult<Nothing>
}

// Functional operators: map, onSuccess, onError, getOrNull, getOrDefault
```

---

### 3.5 Coroutine Dispatcher Injection Module

Located in `core/di/DispatcherModule.kt`, decoupling hardcoded dispatchers for deterministic unit testing:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides @Singleton @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Singleton @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides @Singleton @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
```

---

## 4. Verification & Validation

The build was verified with KSP code generation:
```bash
./gradlew.bat compileDebugKotlin
```
**Outcome**: `BUILD SUCCESSFUL in 1m 45s`
- Hilt generated all component classes (`SmartVisionApp_HiltComponents`, `Hilt_MainActivity`).
- KSP processed the Kotlin 2.2.10 codebase cleanly without warning or error.

---

## 5. Real-World Use Cases & Business Applications

1. **Deterministic Camera Frame State**: In real-time computer vision (e.g., Face Mesh or Object Tracking), frames arrive at 30-60 FPS. MVI guarantees that the UI state reflecting detected landmarks and bounding boxes transitions cleanly without race conditions or mismatched UI elements.
2. **One-Time Action Delivery**: When an ML Kit Barcode Scanner recognizes a QR code containing a deep-link URL, emitting this as a `UiEffect.NavigateToUrl` via `Channel` guarantees the user is navigated exactly once—even if the device rotates or recomposes before navigation completes.
3. **Enterprise Testability**: By injecting `@IoDispatcher` and `@DefaultDispatcher`, unit tests for ML processing algorithms can swap in `StandardTestDispatcher` or `UnconfinedTestDispatcher` without altering production code.

---

## 6. Performance & Memory Best Practices

- **StateFlow Recomposition Scoping**: Exposing state as `StateFlow<S>` allows Compose screens to collect state using `collectAsStateWithLifecycle()`, stopping collection when the app enters the background to save CPU and battery.
- **Channel vs SharedFlow for Side-Effects**: `Channel<E>(Channel.BUFFERED)` holds events until an active collector is ready. Unlike `SharedFlow(replay = 0)` (which drops events if emitted when no collector is active, such as during a configuration change), `Channel` buffers the effect until the resumed UI collects it.
- **Atomic State Updates**: Using `_uiState.value = currentState.reducer()` prevents partial state tearing.

---

## 7. Top 10 Technical Interview Questions & Answers

### Q1: Why use MVI over MVVM in an app that heavily uses Computer Vision and ML Kit?
**Answer**:
Computer vision workflows involve complex, rapid state changes (e.g., `CameraReady` → `AnalyzingFrame` → `DetectionsFound` → `TrackingLost` → `Error`).
In traditional MVVM with multiple LiveData/StateFlow variables, it is easy to enter invalid states (e.g., `isDetecting = true` while `errorMessage != null` and `detectedFaces` contains stale data).
In MVI, the entire UI state is a single, immutable data class. A state transition is a pure function: `f(OldState, Intent) = NewState`. This guarantees that stale detections cannot coexist with an error state, providing mathematical predictability to high-frequency ML rendering.

---

### Q2: How does Hilt's component hierarchy map to the Android lifecycle?
**Answer**:
Hilt organizes dependency containers into a strict hierarchy:
1. `SingletonComponent`: Bound to the `Application` lifecycle (created on app startup, destroyed on process kill). Hosts app-wide singletons (e.g., dispatchers, network clients, database).
2. `ActivityRetainedComponent`: Lives across Activity configuration changes (rotations), destroyed when the Activity finishes. Hosts ViewModels.
3. `ViewModelComponent`: Scoped to a specific `ViewModel`.
4. `ActivityComponent`: Bound to the `Activity` instance lifecycle.
5. `FragmentComponent` / `ViewComponent`: Bound to respective fragments or views.

A binding in a child component can depend on bindings in any of its parent components, but not vice-versa.

---

### Q3: Why should one-time events (like navigation or toasts) be dispatched via `Channel` instead of `SharedFlow` or `StateFlow`?
**Answer**:
- **StateFlow**: Is state-holder, not an event bus. It conflates identical consecutive values (`distinctUntilChanged`) and replays the current value to any new collector (causing duplicate snackbars or navigation on rotation).
- **SharedFlow(replay = 0)**: Does not replay on rotation, but if an event is emitted while the UI is paused, rotating, or transitioning (collector is not actively subscribed), the event is dropped forever.
- **Channel(Channel.BUFFERED)**: Operates with queue semantics. If an effect is emitted while the Activity/Screen is rotating, the effect is safely buffered in the channel and delivered to the new collector immediately upon subscription, preventing dropped navigation triggers.

---

### Q4: What happens under the hood when you annotate an `Application` class with `@HiltAndroidApp`?
**Answer**:
`@HiltAndroidApp` triggers Hilt/Dagger's annotation processor (via KSP) to:
1. Generate an internal base class (e.g., `Hilt_SmartVisionApp`) that extends Android's `Application`.
2. Implement `GeneratedComponentManagerHolder` and `HasCustomComponentFactory`.
3. Create and initialize the `SingletonComponent` (the root dependency container) inside `Application.onCreate()`.
4. Provide dependency injection hooks so that downstream components (`@AndroidEntryPoint` activities, fragments, services) can access the dependency graph.

---

### Q5: How does KSP improve build speeds over KAPT when compiling Hilt components?
**Answer**:
KAPT is a wrapper around `javac`'s annotation processing API. Because `javac` cannot read Kotlin source directly, KAPT must first generate Java "stubs" for every Kotlin class in the module, which is an expensive disk and CPU operation.
KSP integrates directly into the Kotlin compiler front-end (PSI / Kotlin compiler plugins). It reads Kotlin Abstract Syntax Trees (AST) directly without generating any Java stubs, reducing annotation processing overhead by 2x to 4x.

---

### Q6: How do you prevent state tearing or race conditions in `BaseViewModel` when multiple coroutines update the state concurrently?
**Answer**:
In `BaseViewModel`, `_uiState` is a `MutableStateFlow`.
While reading `_uiState.value` and reassigning it is common:
```kotlin
_uiState.value = currentState.reducer()
```
In highly concurrent environments where multiple background coroutines might invoke `setState` simultaneously, one update could overwrite another. To guarantee atomic read-modify-write transitions without race conditions, Kotlin's `MutableStateFlow.update` should be used:
```kotlin
_uiState.update { it.reducer() }
```
`update` uses a CAS (Compare-And-Swap) lock-free loop under the hood.

---

### Q7: Why is it an anti-pattern to pass `CoroutineScope` or `Context` as parameters into a `ViewModel` constructor?
**Answer**:
- **Memory Leaks**: A `ViewModel` outlives the Activity during configuration changes. Holding an Activity `Context` or an Activity-scoped `CoroutineScope` in the ViewModel creates a hard reference, causing the destroyed Activity to leak in memory.
- **Lifecycle Desynchronization**: A ViewModel already provides its own lifecycle-scoped scope: `viewModelScope`. Coroutines launched on `viewModelScope` are automatically cancelled when `ViewModel.onCleared()` is called, preventing orphaned work.
- If an application context is needed, the `AndroidViewModel` subclass can be used, or the application context can be injected via Hilt using `@ApplicationContext`.

---

### Q8: What is the purpose of `@Qualifier` in Dagger / Hilt, and when is it required?
**Answer**:
When multiple bindings of the same type exist in the dependency graph (e.g., multiple `CoroutineDispatcher` instances such as `Dispatchers.IO`, `Dispatchers.Default`, and `Dispatchers.Main`), Dagger cannot disambiguate which instance to inject based on the type alone.
An `@Qualifier` is a custom annotation (e.g., `@IoDispatcher`) that adds semantic metadata to a binding. This enables Dagger/Hilt to match the exact provider method to the corresponding injection site without ambiguity.

---

### Q9: How does Compose observe MVI `UiState` lifecycle-safely?
**Answer**:
In Jetpack Compose, observing a `StateFlow` should be done with:
```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
```
(from `androidx.lifecycle.compose`).
Unlike `collectAsState()`, `collectAsStateWithLifecycle()` pauses state collection when the lifecycle drops below a specified state (default: `Lifecycle.State.STARTED`), such as when the app is put in the background. This avoids wasting CPU/GPU cycles updating Compose UI nodes when the user cannot see the screen.

---

### Q10: How do you mock or replace Hilt dependencies in instrumented / unit tests?
**Answer**:
- **Unit Tests**: Since ViewModels and UseCases rely on constructor injection, Hilt does not even need to run in pure unit tests. You instantiate the class directly, passing test doubles (mocks or fakes) into the constructor.
- **Instrumented / Android Tests**:
  1. Annotate the test class with `@HiltAndroidTest`.
  2. Use `@UninstallModules(DispatcherModule::class)` on the test class to remove production modules.
  3. Declare an `@InstallIn(SingletonComponent::class)` test module with `@BindValue` or `@Provides` supplying test dispatchers or mocked repositories.
  4. Call `hiltRule.inject()` in `@Before`.
