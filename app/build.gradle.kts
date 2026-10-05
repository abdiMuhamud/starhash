import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Release number: VERSION (e.g. 1.0) + the GitHub build number = 1.0.<run>.
val baseVersion = rootProject.file("VERSION").readText().trim()
val buildNumber = (System.getenv("STARHASH_BUILD") ?: "0").toIntOrNull() ?: 0

android {
    namespace = "com.innovii.starhash"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.innovii.starhash"
        minSdk = 26
        targetSdk = 35
        versionCode = maxOf(1, buildNumber)
        versionName = "$baseVersion.$buildNumber"
    }

    signingConfigs {
        // STARHASH_KEYSTORE* (CI secrets) when set; otherwise the shared key in keystore/, so every build
        // installs over the last one.
        create("shared") {
            val ks = System.getenv("STARHASH_KEYSTORE")
            if (!ks.isNullOrBlank()) {
                storeFile = file(ks)
                storePassword = System.getenv("STARHASH_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("STARHASH_KEY_ALIAS")
                keyPassword = System.getenv("STARHASH_KEY_PASSWORD")
            } else {
                storeFile = rootProject.file("keystore/starhash-shared.keystore")
                storePassword = "android"
                keyAlias = "starhash"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
