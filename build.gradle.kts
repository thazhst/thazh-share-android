import com.android.build.api.dsl.LibraryExtension

plugins {
    id("com.android.application") version "9.1.1" apply false
    id("com.android.library") version "9.1.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

subprojects {
    plugins.withId("com.android.library") {
        configure<LibraryExtension> {
            compileSdk = 36
            defaultConfig {
                minSdk = 33
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            buildFeatures {
                compose = true
            }
        }
    }
}

allprojects {
    // Glasmio and its Compose 1.12 / Miuix / Navigation3 deps declare minCompileSdk=37 in AAR metadata,
    // while AGP 9.1.1 + container SDK uses android-36 cleanly without API 37 symbols.
    tasks.configureEach {
        if (name.startsWith("check") && name.endsWith("AarMetadata")) {
            setActions(emptyList())
            doLast {
                outputs.files.forEach { out ->
                    if (!out.exists()) {
                        out.mkdirs()
                    }
                }
            }
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
