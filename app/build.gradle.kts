plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.rainkaze.birdwatcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rainkaze.birdwatcher"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)


    implementation(libs.flexbox)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.cardview)

    implementation(libs.glide)
    annotationProcessor(libs.glide)

    // Lombok依赖声明
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)



    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // 添加Gson用于解析JSON
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2") //确保有RecyclerView
    implementation("androidx.cardview:cardview:1.0.0") //美化列表项
}