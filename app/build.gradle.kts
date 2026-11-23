plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "android.ai.smartvisionaimlkitplayground"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "android.ai.smartvisionaimlkitplayground"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // ── Compose BOM ─────────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))

    // ── AndroidX Core ───────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // ── Compose UI ──────────────────────────────────────────────────────────
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // ── Navigation ──────────────────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // ── Hilt DI ─────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ── CameraX ─────────────────────────────────────────────────────────────
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ── Image Loading ───────────────────────────────────────────────────────
    implementation(libs.coil.compose)

    // ── ML Kit — Text Recognition ───────────────────────────────────────────
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.chinese)
    implementation(libs.mlkit.text.recognition.devanagari)
    implementation(libs.mlkit.text.recognition.japanese)
    implementation(libs.mlkit.text.recognition.korean)

    // ── ML Kit — Barcode Scanning ───────────────────────────────────────────
    implementation(libs.mlkit.barcode.scanning)

    // ── ML Kit — Face APIs ──────────────────────────────────────────────────
    implementation(libs.mlkit.face.detection)
    implementation(libs.mlkit.face.mesh.detection)

    // ── ML Kit — Image & Object ─────────────────────────────────────────────
    implementation(libs.mlkit.image.labeling)
    implementation(libs.mlkit.objectdetection)

    // ── ML Kit — Body ───────────────────────────────────────────────────────
    implementation(libs.mlkit.pose.detection)
    implementation(libs.mlkit.pose.detection.accurate)
    implementation(libs.mlkit.segmentation.selfie)

    // ── ML Kit — Natural Language ───────────────────────────────────────────
    implementation(libs.mlkit.language.id)
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.smart.reply)
    implementation(libs.mlkit.entity.extraction)

    // ── ML Kit — Other ──────────────────────────────────────────────────────
    implementation(libs.mlkit.digital.ink.recognition)
    implementation(libs.mlkit.document.scanner)
    implementation(libs.mlkit.subject.segmentation)

    // ── Testing ─────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
