// build.gradle.kts (Project-level) - CORRECTED
plugins {
    // Android Application plugin
    // Use the direct ID and version for plugins applied with 'apply false' at the top level
    id("com.android.application") version "8.2.0" apply false // Check your AGP version (e.g., "8.2.0" or "8.2.2")
    // Kotlin Android plugin
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false // Check your Kotlin version (e.g., "1.9.0" or "1.9.22")
    // Google Services plugin for Firebase - This one was already correct
    id("com.google.gms.google-services") version "4.4.3" apply false
}