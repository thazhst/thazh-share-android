pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Thazh Share"
include(":app")
include(":glasmio-core")
include(":glasmio-glass")
include(":glasmio-icons")
include(":glasmio-ui")
include(":glasmio-scaffold")
include(":glasmio-preference")
