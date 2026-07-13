import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

// Pin Kotlin's JVM target to 17 so it always matches the Java target below,
// independent of the JDK the build runs on (CI's instrumentation job runs on
// JDK 21 for firebase-tools; without this, Kotlin would target 21 and clash
// with compileDebugJavaWithJavac at 17).
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.workoutapp.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.workoutapp.android"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Test-only: proves the Firebase Local Emulator Suite wiring works end to
    // end (see FirestoreEmulatorSmokeTest). No production code depends on
    // Firebase yet — the backend choice (custom Ktor vs BaaS) is deferred to
    // Phase 2 (EXECUTION_PLAN.md §2).
    androidTestImplementation(platform(libs.firebase.bom))
    androidTestImplementation(libs.firebase.firestore)
    androidTestImplementation(libs.firebase.auth)
}
