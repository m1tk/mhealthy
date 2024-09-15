import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "fr.android.mhealthy"
    compileSdk = 34

    defaultConfig {
        applicationId = "fr.android.mhealthy"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            val p = Properties()
            p.load(project.rootProject.file("local.properties").reader())
            val server: String = p.getProperty("SERVER_URL")
            buildConfigField("String", "SERVER_URL", "\"$server\"")
        }
        debug {
            val p = Properties()
            p.load(project.rootProject.file("local.properties").reader())
            val server: String = p.getProperty("SERVER_URL")
            buildConfigField("String", "SERVER_URL", "\"$server\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.preference:preference-ktx:1.2.0")
    // CameraX dependencies (use consistent versions)
    val camerax_version = "1.3.0"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // ZXing for QR code generation and scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.10")


    implementation("net.gotev:cookie-store:1.5.0")
    implementation("net.gotev:cookie-store-okhttp:1.5.0")

    implementation("org.greenrobot:eventbus:3.2.0")

    implementation("de.hdodenhof:circleimageview:3.1.0")
}
