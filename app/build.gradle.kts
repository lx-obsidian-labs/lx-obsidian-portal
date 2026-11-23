plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

import java.util.Properties

android {
    namespace = "com.example.vista"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.vista"
        minSdk = 24
        targetSdk = 35
        versionCode = 20
        versionName = "1.0.13"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        // API Keys (from gradle.properties - see gradle.properties.example)
        val tmdbApiKey: String = properties["TMDB_API_KEY"] as? String
            ?: error("TMDB_API_KEY not found in gradle.properties. Copy gradle.properties.example to gradle.properties and fill in your keys.")
        val tmdbAccessToken: String = properties["TMDB_ACCESS_TOKEN"] as? String
            ?: error("TMDB_ACCESS_TOKEN not found in gradle.properties. Copy gradle.properties.example to gradle.properties and fill in your keys.")
        val watchmodeApiKey: String = properties["WATCHMODE_API_KEY"] as? String
            ?: error("WATCHMODE_API_KEY not found in gradle.properties. Copy gradle.properties.example to gradle.properties and fill in your keys.")

        // Optional NexStream movie fallback. Explicit Gradle/environment values override .env.
        val backupEnvFile = rootProject.file(".env")
        val backupEnv = Properties().apply {
            if (backupEnvFile.exists()) backupEnvFile.inputStream().use { load(it) }
        }
        val backupMoviesApiKey = providers.gradleProperty("BACKUP_MOVIES_API_KEY").orNull
            ?: providers.environmentVariable("BACKUP_MOVIES_API_KEY").orNull
            ?: backupEnv.getProperty("BACKUP_MOVIES_API_KEY", "")
        val escapedBackupKey = backupMoviesApiKey.trim().removeSurrounding("\"")
            .removeSurrounding("'").replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "BACKUP_MOVIES_API_KEY", "\"$escapedBackupKey\"")

        // First-party Vista analytics (Cloudflare Pages Function + D1). Blank disables tracking.
        val vistaAnalyticsBaseUrl: String = properties["VISTA_ANALYTICS_BASE_URL"] as? String ?: ""
        val vistaAnalyticsKey: String = properties["VISTA_ANALYTICS_KEY"] as? String ?: ""
        val aiBaseUrl: String = properties["VISTA_AI_BASE_URL"] as? String ?: ""
        buildConfigField("String", "VISTA_AI_BASE_URL", "\"${aiBaseUrl}\"")

        buildConfigField("String", "TMDB_API_KEY", "\"${tmdbApiKey}\"")
        buildConfigField("String", "TMDB_ACCESS_TOKEN", "\"${tmdbAccessToken}\"")
        buildConfigField("String", "WATCHMODE_API_KEY", "\"${watchmodeApiKey}\"")
        buildConfigField("String", "VISTA_ANALYTICS_BASE_URL", "\"${vistaAnalyticsBaseUrl}\"")
        buildConfigField("String", "VISTA_ANALYTICS_KEY", "\"${vistaAnalyticsKey}\"")
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties().apply {
                    load(keystorePropertiesFile.inputStream())
                }
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 disabled for shipping. Minified builds strip the reflectively-parsed
            // Gson DTOs under data.remote.** even with keep rules, producing a ~7 MB
            // APK that shows an EMPTY catalog ("no data"). See BUILD_REPORT.md.
            // Keep the shipped APK at the known-good ~21 MB (R8 off).
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.webkit:webkit:1.12.1")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-svg:2.6.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-paging:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson (main serialization)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Media3 / ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")
    implementation("androidx.media3:media3-common:1.3.1")
    implementation("androidx.media3:media3-database:1.3.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Paging
    implementation("androidx.paging:paging-runtime-ktx:3.3.0")
    implementation("androidx.paging:paging-compose:3.3.0")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Movie of the Day: survives reboots and doze, which AlarmManager would not
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Android TV
    implementation("androidx.tv:tv-foundation:1.0.0-alpha11")
    implementation("androidx.tv:tv-material:1.0.0-rc02")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Google Mobile Ads (AdMob)
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.10")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
