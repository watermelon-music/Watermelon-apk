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

rootProject.name = "Watermelon"

include(
    ":app",
    ":core:common",
    ":core:designsystem",
    ":core:navigation",
    ":data",
    ":domain",
    ":feature-auth",
    ":feature-home",
    ":feature-search",
    ":feature-player",
    ":feature-library",
    ":feature-playlist",
    ":feature-downloads",
    ":feature-settings",
)
