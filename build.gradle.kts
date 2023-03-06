// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "7.4.0" apply false
    id("com.android.library") version "7.4.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false
    id("androidx.benchmark") version "1.1.1" apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

group = "me.tatarka.android"
version = "0.2-SNAPSHOT"

nexusPublishing {
    repositories {
        sonatype()
    }
}