// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2" apply false
    // The Kotlin plugin version below drives all three Kotlin-ecosystem
    // plugins in this project. org.jetbrains.kotlin.plugin.compose (the
    // Compose compiler Gradle plugin) only exists from Kotlin 2.0.0 onward
    // and its version must always exactly match org.jetbrains.kotlin.android's
    // — they ship from the same Kotlin release and are not independently
    // versioned. 2.0.21 is documented as fully compatible with Gradle 8.7,
    // which this project already pins elsewhere (gradle-wrapper.properties,
    // the CI workflow), so it was chosen as a conservative, well-tested step
    // up from Kotlin 1.9.x rather than jumping to the newest available line.
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
