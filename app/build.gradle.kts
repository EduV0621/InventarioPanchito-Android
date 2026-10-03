plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    // Fase 4: requiere que exista app/google-services.json (ver instrucciones de configuracion).
    id("com.google.gms.google-services")
}

android {
    namespace = "com.panchito.inventario"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.panchito.inventario"
        // minSdk 24: restriccion realista del proyecto por disponibilidad de dispositivos del equipo (ver informe, seccion "Restricciones realistas").
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-sprint1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // --- Core / Compose ---
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // --- Persistencia local: Room (Sprint 1: entidades y DAO definidos, sin logica de negocio) ---
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // --- WorkManager (sincronizacion Room <-> API REST PHP/MySQL en segundo plano) ---
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // --- DataStore (preferencias de sesion / rol, Sprint 1: definido sin logica) ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- Retrofit / API REST (Fase 2: consumo de API REST con Retrofit y JSON) ---
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // --- Firebase Authentication (unico uso de Firebase que conserva el proyecto: login/registro
    // de empleados. El inventario ya NO se guarda en Firestore, se guarda en Room + API REST propia) ---
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
