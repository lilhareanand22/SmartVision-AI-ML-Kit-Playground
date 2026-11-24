package android.ai.smartvisionaimlkitplayground

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Base Application class annotated with [HiltAndroidApp] to trigger
 * Hilt's code generation, including a base class for the application that acts as
 * the application-level dependency container.
 */
@HiltAndroidApp
class SmartVisionApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
