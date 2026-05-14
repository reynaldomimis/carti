import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
}

val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties()
if (secretsFile.exists()) {
    secrets.load(FileInputStream(secretsFile))
}

android {
    namespace = "com.upreyvan.carti"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.upreyvan.carti"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Build Config Fields from secrets.properties
        buildConfigField("String", "APPWRITE_PROJECT_ID", "\"${secrets.getProperty("APPWRITE_PROJECT_ID", "")}\"")
        buildConfigField("String", "APPWRITE_ENDPOINT", "\"${secrets.getProperty("APPWRITE_ENDPOINT", "")}\"")
        buildConfigField("String", "APPWRITE_ENDPOINT_FUNCTION", "\"${secrets.getProperty("APPWRITE_ENDPOINT_FUNCTION", "")}\"")
        buildConfigField("String", "APPWRITE_GATEWAY_FUNCTION_ID", "\"${secrets.getProperty("APPWRITE_GATEWAY_FUNCTION_ID", "")}\"")
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.mpandroidchart)
    implementation(libs.gson)
    implementation(libs.ucrop)
    implementation(libs.appwrite)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}