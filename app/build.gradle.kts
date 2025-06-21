plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
//    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

}

android {
    namespace = "com.example.laporbaranghilang"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.laporbaranghilang"
        minSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {

    implementation ("androidx.appcompat:appcompat:1.7.1")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.2.1")

    // Import the Firebase BoM (Bill of Materials) - Selalu gunakan versi terbaru
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))    // Firebase Authentication
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-auth")

    // Firebase Realtime Database
    implementation ("com.google.firebase:firebase-database")

    // RecyclerView dan CardView
    implementation ("androidx.recyclerview:recyclerview:1.4.0")
    implementation ("androidx.cardview:cardview:1.0.0")

    // Kotlin-specific dependencies (hanya tambahkan jika Anda memilih Kotlin)
    implementation ("androidx.core:core-ktx:1.16.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1") // Untuk coroutines di Kotlin
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid) // Untuk coroutines di Kotlin

    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.2.1")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.6.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.guava:guava:32.1.3-android")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") {
        // Exclude Guava jika sudah ada konflik dengan versi Android-nya
        exclude(group = "com.google.guava", module = "guava")
    }
// Tambahkan Guava versi Android yang kompatibel
    implementation("com.google.guava:guava:32.1.3-android")
}