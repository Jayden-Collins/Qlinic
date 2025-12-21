plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.qlinic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.qlinic"
        minSdk = 26
        targetSdk = 36
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
    }
}

dependencies {
    // --- BOMs (Bill of Materials) ---
    // These manage the versions for all libraries in their respective families.
    implementation(platform(libs.firebase.bom))
    implementation(platform(libs.androidx.compose.bom)) // Use the Compose BoM to manage Compose versions

    // --- Core Android & Lifecycle Dependencies ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // --- Jetpack Compose UI Dependencies ---
    // Versions are managed by the Compose BoM
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // You only need this once
    implementation(libs.androidx.material.icons.extended) // CORRECTED: Use 'androidx' prefix
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // --- Firebase Dependencies ---
    // Versions are managed by the Firebase BoM
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation("com.google.firebase:firebase-messaging") // Add FCM dependency
    implementation(libs.material3)
    implementation(libs.play.services.coroutines)
    implementation(libs.ui)


    // --- Testing Dependencies ---
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // --- Debug Dependencies ---
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.compose.material3:material3:1.1.0")

    implementation("androidx.compose.material:material-icons-extended:1.5.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Material3
    implementation("androidx.compose.material3:material3:1.2.1")

    implementation("io.coil-kt:coil-compose:2.4.0")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("io.coil-kt:coil-compose:2.6.0")
}