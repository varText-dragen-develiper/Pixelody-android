plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pixelody.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pixelody.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
        buildConfigField("boolean", "ENABLE_DEMO_LIBRARY", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            if (providers.gradleProperty("moduleTestBuild").orNull == "true") {
                buildConfigField("boolean", "ENABLE_DEMO_LIBRARY", "true")
                applicationIdSuffix = ".modules"
                versionNameSuffix = "-module-test"
            } else if (providers.gradleProperty("studioHierarchyTestBuild").orNull == "true") {
                buildConfigField("boolean", "ENABLE_DEMO_LIBRARY", "true")
                applicationIdSuffix = ".studioqa"
                versionNameSuffix = "-studio-review"
                resValue("string", "app_name", "Pixelody Studio test")
            } else if (providers.gradleProperty("firstUseTestBuild").orNull == "true") {
                applicationIdSuffix = ".firstuse"
                versionNameSuffix = "-first-use-test"
                resValue("string", "app_name", "Pixelody setup test")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    val activityComposeVersion = "1.9.3"
    val composeBomVersion = "2024.12.01"
    val lifecycleVersion = "2.8.7"
    val media3Version = "1.5.1"
    val cameraXVersion = "1.4.1"

    implementation("androidx.activity:activity-compose:$activityComposeVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.startup:startup-runtime:1.2.0")
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
