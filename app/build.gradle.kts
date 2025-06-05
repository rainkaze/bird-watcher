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
    implementation(libs.play.services.maps)
    implementation(libs.swiperefreshlayout)
    annotationProcessor(libs.glide)

    // Lombok依赖声明
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)



    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    //baidumap依赖

//    implementation(libs.baidumap.map)
//    implementation(libs.baidumap.search)
//    implementation(libs.baidumap.util)
//    implementation("com.baidu.mapapi:map:7.5.0")
//    implementation("com.baidu.mapapi:search:7.5.0")
//    implementation("com.baidu.mapapi:util:7.5.0")
 //   implementation("com.baidu.android:map-sdk:5.5.1")
   // implementation(libs.baidumap.location)
    implementation ("com.baidu.lbsyun:BaiduMapSDK_Map:7.5.0") // 检查官网更新版本
    // 百度地图检索功能（用于地理编码）
    implementation("com.baidu.lbsyun:BaiduMapSDK_Search:7.5.0")
    // 百度地图工具库
    implementation("com.baidu.lbsyun:BaiduMapSDK_Util:7.5.0")

    implementation("com.baidu.lbsyun:BaiduMapSDK_Location:9.4.0")
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
// Material Design 组件
    implementation("com.google.android.material:material:1.9.0")

// RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.0")

// Retrofit网络请求库
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
// Retrofit的GSON转换器
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Glide图片加载库
    implementation("com.github.bumptech.glide:glide:4.16.0")

// Glide编译器（用于注解处理）
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    // 图片加载
    implementation("com.github.bumptech.glide:glide:4.14.2")
    annotationProcessor("com.github.bumptech.glide:compiler:4.14.2")

}