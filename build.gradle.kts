import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.kotlin.dsl.configure

private val compileSdkVersion = 37
private val minSdkVersion = 30
private val javaCompatibility = JavaVersion.VERSION_11

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hiltAndroid) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> {
            compileSdk {
                version = release(compileSdkVersion)
            }
            defaultConfig {
                minSdk = minSdkVersion
                targetSdk = compileSdkVersion
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = javaCompatibility
                targetCompatibility = javaCompatibility
            }
        }
    }

    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension> {
            compileSdk {
                version = release(compileSdkVersion)
            }
            defaultConfig {
                minSdk = minSdkVersion
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = javaCompatibility
                targetCompatibility = javaCompatibility
            }
            buildTypes {
                getByName("release") {
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
            }
        }
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint().editorConfigOverride(
            mapOf(
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                "ktlint_standard_no-unused-imports" to "enabled",
            ),
        )
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**/*.kts")
        ktlint()
    }
}
