plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.mybussines"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mybussines"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Retrofit do obsługi API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

    // Coroutines (do operacji w tle)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    // Bezpieczne przechowywanie kluczy (Security Crypto)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("androidx.cardview:cardview:1.0.0")

    // Charts.kt - biblioteka wykresów
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // XTB / sieć
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
