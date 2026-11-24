plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.zirakusa.telefono"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zirakusa.telefono"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0"
        // Modo MCP: the same public relay as the rest of the family (one device id per app). Empty = no MCP.
        val relayUrl = (project.findProperty("telefonoRelayUrl") as String?).orEmpty().trimEnd('/')
        buildConfigField("String", "RELAY_URL", "\"$relayUrl\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Signed like the rest of the family and Gateway Hub (the hub recognises the same developer).
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Screenshots of the screens land in app/build/screenshots .
        unitTests.all { it.systemProperty("roborazzi.test.record", "true") }
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    // Modo MCP: WebSocket to the relay (same library and version as the rest of the family).
    // Contactos' QR card (same library and version as the QR app; pure Java, on the phone).
    implementation("com.google.zxing:core:3.5.4")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.79.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.79.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
