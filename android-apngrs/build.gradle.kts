plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.mozilla.rust-android-gradle.rust-android") version "0.9.3"
    id("maven-publish")
}

android {
    namespace = "me.tatarka.android.apngrs"
    compileSdk = 33

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles.add(file("consumer-rules.pro"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = false
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
}

cargo {
    module  = "src/main/rust"
    libname = "android_apngrs"
    targets = listOf("arm", "arm64", "x86", "x86_64")
    profile = "debug"
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "me.tatarka.android"
            artifactId = "apngrs"
            version = "0.1"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}