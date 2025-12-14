plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.room.plugin)
}

android {
    namespace = "io.github.cactric.swalsh"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.cactric.swalsh"
        // Increased to 29 for WifiNetworkSpecifier
        minSdk = 29
        targetSdk = 36
        versionCode = 4
        versionName = "2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    buildFeatures {
        viewBinding = true
    }
}

room {
    schemaDirectory(projectDir.path + "/roomSchema")
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    implementation(libs.preference.ktx)
    implementation(libs.zxing.core)
    implementation(libs.camera.core)
    implementation(libs.camera.view)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.camera2)

    implementation(libs.room.runtime)
    implementation(libs.lifecycle.service)
    annotationProcessor(libs.room.annotations)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.contrib)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
}