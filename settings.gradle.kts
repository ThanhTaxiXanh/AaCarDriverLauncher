pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Secret token (scope DOWNLOADS:READ) dùng để tải Mapbox SDK từ Maven của Mapbox
val mapboxDownloadsToken: String = providers.environmentVariable("MAPBOX_DOWNLOADS_TOKEN")
    .orElse(providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN"))
    .orElse("")
    .get()

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication { create<BasicAuthentication>("basic") }
            credentials {
                username = "mapbox"
                password = mapboxDownloadsToken
            }
        }
    }
}

rootProject.name = "CarDriverLauncher"
include(":app")
