plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.asamsungmusicplayerreplica"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.asamsungmusicplayerreplica"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.palette)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // For Slide screen in main activity
    implementation(libs.viewpager2)
    implementation(libs.material.v190)
    implementation (libs.media)


    implementation ("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
}