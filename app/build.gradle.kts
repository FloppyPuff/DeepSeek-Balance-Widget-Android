import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val signingProperties = Properties().apply {
    val file = rootProject.file("signing.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

fun signingValue(key: String, env: String): String? =
    providers.environmentVariable(env).orNull?.takeIf { it.isNotBlank() }
        ?: signingProperties.getProperty(key)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "RELEASE_KEYSTORE_PATH")?.let { rootProject.file(it) }
val releaseStorePassword = signingValue("storePassword", "RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")
val hasCustomReleaseSigning = releaseStoreFile?.isFile == true &&
    releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null

android {
    namespace = "com.example.deepseekwidget"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.deepseekwidget"
        minSdk = 26
        targetSdk = 37
        versionCode = 15
        versionName = "1.4.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasCustomReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = false
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasCustomReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
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
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // Jetpack Glance (AppWidget)
    implementation("androidx.glance:glance:1.1.1")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.3")

    // Zero-Plaintext EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager for background periodic sync
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
}
