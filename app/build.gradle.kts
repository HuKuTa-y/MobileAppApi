plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.lawapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lawapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // AndroidX
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("com.google.android.material:material:1.9.0")
    // Retrofit - ОБЯЗАТЕЛЬНО:
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp:
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson:
    implementation("com.google.code.gson:gson:2.10.1")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Lifecycle:
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation(libs.monitor)
    implementation(libs.ext.junit)

    androidTestImplementation(libs.junit.junit)
    androidTestImplementation(libs.junit.junit)
}
