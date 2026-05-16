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

        buildConfigField("String", "APPWRITE_PROJECT_ID", "\"${secrets.getProperty("APPWRITE_PROJECT_ID", "")}\"")
        buildConfigField("String", "APPWRITE_ENDPOINT", "\"${secrets.getProperty("APPWRITE_ENDPOINT", "")}\"")
        buildConfigField("String", "APPWRITE_ENDPOINT_FUNCTION", "\"${secrets.getProperty("APPWRITE_ENDPOINT_FUNCTION", "")}\"")
        buildConfigField("String", "APPWRITE_GATEWAY_FUNCTION_ID", "\"${secrets.getProperty("APPWRITE_GATEWAY_FUNCTION_ID", "")}\"")
        buildConfigField("String", "APPWRITE_DATABASE_ID", "\"${secrets.getProperty("APPWRITE_DATABASE_ID", "")}\"")
        buildConfigField("String", "APPWRITE_COL_USERS", "\"${secrets.getProperty("APPWRITE_COL_USERS", "")}\"")
        buildConfigField("String", "APPWRITE_COL_TRANSACTIONS", "\"${secrets.getProperty("APPWRITE_COL_TRANSACTIONS", "")}\"")
        buildConfigField("String", "APPWRITE_COL_GOALS", "\"${secrets.getProperty("APPWRITE_COL_GOALS", "")}\"")
        buildConfigField("String", "APPWRITE_COL_DEBTS", "\"${secrets.getProperty("APPWRITE_COL_DEBTS", "")}\"")
        buildConfigField("String", "APPWRITE_COL_FAMILIES", "\"${secrets.getProperty("APPWRITE_COL_FAMILIES", "")}\"")
        buildConfigField("String", "APPWRITE_COL_MESSAGES", "\"${secrets.getProperty("APPWRITE_COL_MESSAGES", "")}\"")
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
    implementation(libs.shimmer)

    // Room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}