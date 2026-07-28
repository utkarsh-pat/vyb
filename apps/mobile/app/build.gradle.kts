import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val releaseStoreFile = providers.gradleProperty("vybReleaseStoreFile").orNull
val releaseStorePassword = providers.gradleProperty("vybReleaseStorePassword").orNull
val releaseKeyAlias = providers.gradleProperty("vybReleaseKeyAlias").orNull
val releaseKeyPassword = providers.gradleProperty("vybReleaseKeyPassword").orNull
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    kotlin("plugin.serialization") version "2.3.0"
}

android {
    namespace = "social.vyb.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "social.vyb.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.2"

    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            val debugApiUrl = providers.gradleProperty("vybApiBaseUrl")
                .orElse("http://10.0.2.2:4000/")
                .get()
            buildConfigField("String", "API_BASE_URL", "\"$debugApiUrl\"")
        }
        release {
            val releaseApiUrl = providers.gradleProperty("vybReleaseApiBaseUrl")
                .orElse("https://api.vybnet.app/")
                .get()
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiUrl\"")
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
