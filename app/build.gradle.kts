plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    // Apply the OSS‑Licenses plugin here (no version needed: pulled in via buildscript)
    id("com.google.android.gms.oss-licenses-plugin")
    alias(libs.plugins.google.firebase.crashlytics)
    alias(libs.plugins.google.firebase.firebase.perf)
}

configurations.all {
    exclude(group = "commons-logging", module = "commons-logging")
}

android {
    namespace = "com.example.myduka"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myduka"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { viewBinding = true }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }

    packagingOptions {
        resources.excludes.add("META-INF/LGPL2.1")
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.androidx.gridlayout)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.functions)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)



    implementation(libs.glide)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)

    // OSS-Licenses runtime library
    implementation(libs.play.services.oss.licenses)

    implementation(libs.ucrop)

    implementation (libs.text.recognition)

    implementation (libs.barcode.scanning)

    implementation (libs.androidx.camera.core)
    implementation (libs.androidx.camera.camera2)
    implementation (libs.androidx.camera.lifecycle)
    implementation (libs.androidx.camera.view)

    implementation (libs.guava)
    implementation (libs.androidx.camera.extensions)

    implementation("androidx.security:security-crypto:1.1.0-beta01")


    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")




}