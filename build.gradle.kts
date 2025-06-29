// Top-level build file where you can add configuration options common to all sub-projects/modules.

// 1️⃣ Add this buildscript block so Gradle can fetch the OSS-Licenses plugin artifact
buildscript {
    repositories {
        google()       // Google's Maven repository
        mavenCentral() // Maven Central
    }
    dependencies {
        // Latest stable OSS-Licenses Gradle plugin
        classpath("com.google.android.gms:oss-licenses-plugin:0.10.6")
    }
}

// 2️⃣ Your existing plugins block
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
    alias(libs.plugins.google.firebase.firebase.perf) apply false

}