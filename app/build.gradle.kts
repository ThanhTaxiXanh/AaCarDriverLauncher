plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

// Số build tăng dần theo mỗi lần chạy GitHub Actions -> luôn cài đè được lên bản cũ
val buildNumber = env("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.cartaxi.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cartaxi.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"${env("MAPBOX_ACCESS_TOKEN") ?: ""}\"")
    }

    // Ký Release APK bằng keystore CỐ ĐỊNH, đọc từ biến môi trường
    val keystorePath = env("KEYSTORE_FILE")
    signingConfigs {
        if (keystorePath != null && file(keystorePath).exists()) {
            create("release") {
                storeFile = file(keystorePath)              // KEYSTORE_FILE
                storePassword = env("KEYSTORE_PASSWORD")    // storePassword
                keyAlias = env("KEY_ALIAS")                 // keyAlias
                keyPassword = env("KEY_PASSWORD")           // keyPassword
            }
        } else {
            logger.warn("KEYSTORE_FILE chưa được cấu hình: release APK sẽ KHÔNG được ký.")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.car.app:app:1.4.0")
    implementation("com.mapbox.maps:android:11.9.0")
}
