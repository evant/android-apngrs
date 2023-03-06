plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "me.tatarka.android.apngrs.coil"
    compileSdk = 33

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(project(":android-apngrs"))
    api("io.coil-kt:coil-base:2.2.2")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "me.tatarka.android"
            artifactId = "apngrs-coil"
            version = "0.1"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
