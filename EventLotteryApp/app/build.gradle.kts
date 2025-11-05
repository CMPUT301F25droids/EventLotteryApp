plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.eventlotteryapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.eventlotteryapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.firebase:firebase-firestore:25.1.1")
    implementation("com.google.firebase:firebase-auth:23.2.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("com.google.firebase:firebase-messaging:24.0.0")
}
