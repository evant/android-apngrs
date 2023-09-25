plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("io.github.MatrixDev.android-rust") version "0.3.2"
    id("maven-publish")
    id("signing")
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
            withJavadocJar()
        }
    }
}

dependencies {
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
}

androidRust {
    module("android_apngrs") {
        path = file("src/main/rust")
        profile = "release"
        targets = listOf("arm", "arm64", "x86", "x86_64")
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "me.tatarka.android"
            artifactId = "apngrs"
            version = rootProject.version.toString()

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("android-apngrs")
                description.set("Bindings to image-rs for APNG support on Android")
                url.set("https://github.com/evant/android-apngrs")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("evant")
                        name.set("Eva Tatarka")
                    }
                }
                scm {
                    connection.set("https://github.com/evant/android-apngrs.git")
                    developerConnection.set("https://github.com/evant/android-apngrs.git")
                    url.set("https://github.com/evant/android-apngrs")
                }
            }
        }
    }
}

signing {
    setRequired {
        findProperty("signing.keyId") != null
    }

    publishing.publications.all {
        sign(this)
    }
}