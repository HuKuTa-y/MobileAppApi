plugins {
    id("com.android.application")
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

        // Оптимизация: указываем только нужные архитектуры (уменьшает размер APK на 40-60%)

    }

    buildTypes {
        release {
            // КРИТИЧЕСКИ ВАЖНО: включаем обфускацию и сжатие
            isMinifyEnabled = true
            isShrinkResources = true  // Удаляет неиспользуемые ресурсы

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isCrunchPngs = true
            buildConfigField("String", "VERSION_NAME", "\"${defaultConfig.versionName}\"")
            buildConfigField("Integer", "VERSION_CODE", "${defaultConfig.versionCode}")


        }
        debug {
            buildConfigField("String", "VERSION_NAME", "\"${defaultConfig.versionName}\"")
            buildConfigField("Integer", "VERSION_CODE", "${defaultConfig.versionCode}")
            // Для отладки можно отключить, чтобы быстрее собиралось
            isMinifyEnabled = false
            isShrinkResources = false
            isCrunchPngs = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        // Включаем десугаринг для использования новых API на старых Android
        isCoreLibraryDesugaringEnabled = true
    }


    // Включаем ViewBinding (быстрее и безопаснее findViewById)
    buildFeatures {
        buildConfig = true  //
        viewBinding = true
    }
    
    experimentalProperties["android.experimental.enable-art-profile"] = true

    // Разделение по архитектурам (ещё больше уменьшает размер)
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    // 🔥 Оптимизация упаковки ресурсов
    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )

        }
    }
}

dependencies {

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Material (одна версия)
    implementation("com.google.android.material:material:1.11.0")

    // Сеть + JSON
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    // Тесты
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}