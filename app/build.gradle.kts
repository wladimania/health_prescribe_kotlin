plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")




}

android {
    namespace = "com.example.health_prescribe"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.health_prescribe"
        minSdk = 26        //noinspection EditedTargetSdkVersion
        targetSdk = 33
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.txt",
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")  // Usa la versión correcta
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    implementation("org.postgresql:postgresql:42.2.5")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")
    implementation("org.bouncycastle:bcprov-jdk15on:1.68")
    implementation(files("libs/FDxSDKProFDAndroid.jar"))
    implementation("com.android.volley:volley:1.2.1")
    implementation("commons-codec:commons-codec:1.6")
    implementation("ch.qos.logback:logback-classic:1.2.6") // Cambia la versión según tu configuración


}