import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hiltAndroid)
}

if (providers.gradleProperty("enableCoverage").isPresent) {
    apply(plugin = "jacoco")
}

val releaseSigningPropertiesFile = rootProject.file("keystore.properties")
val releaseSigningProperties = Properties()
if (releaseSigningPropertiesFile.isFile) {
    releaseSigningPropertiesFile.inputStream().use(releaseSigningProperties::load)
}

val releaseSigningPropertyKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val hasReleaseSigning =
    releaseSigningPropertyKeys.all {
        !releaseSigningProperties.getProperty(it).isNullOrBlank()
    }
if (releaseSigningPropertiesFile.exists() && !hasReleaseSigning) {
    throw GradleException(
        "keystore.properties must define storeFile, storePassword, keyAlias, and keyPassword",
    )
}

android {
    namespace = "me.heartrace"

    defaultConfig {
        applicationId = "me.heartrace"
        versionCode = 1
        versionName = "1.0"

        androidResources {
            localeFilters += listOf("en", "zh")
        }
    }

    signingConfigs {
        create("test") {
            storeFile = file("test-signing-key.jks")
            storePassword = "android"
            keyAlias = "heartrace-test"
            keyPassword = "android"
        }
        if (hasReleaseSigning) {
            create("production") {
                storeFile = rootProject.file(releaseSigningProperties.getProperty("storeFile"))
                storePassword = releaseSigningProperties.getProperty("storePassword")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("test")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName(if (hasReleaseSigning) "production" else "test")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    packaging {
        resources {
            excludes +=
                setOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "/META-INF/*.version",
                    "/META-INF/*.kotlin_module",
                    "**/attach_hotspot_windows.dll",
                    "META-INF/licenses/**",
                )
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":feature"))
    implementation(project(":uikit"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.hilt.android)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
