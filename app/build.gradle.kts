import com.android.build.api.variant.BuildConfigField
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.echoai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.echoai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.echoai.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        buildConfigField("String", "TRANSCRIBE_URL", "\"${localProperties.getProperty("TRANSCRIBE_URL", "")}\"")
        buildConfigField("String", "TRANSCRIBE_KEY", "\"${localProperties.getProperty("TRANSCRIBE_KEY", "")}\"")
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
        if (name.contains("Test")) {
            kotlinOptions.jvmTarget = "11"
        }
    }
    
    testOptions {
        unitTests.all {
            it.jvmArgs("-noverify") // Or other JVM args
        }
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.hilt.navigation.compose)

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Dagger/Hilt core (one version everywhere)
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")

    // AndroidX Hilt integrations (WorkManager)
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Hilt testing (both source sets wired to KSP)
    testImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspTest("com.google.dagger:hilt-compiler:2.51.1")

    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.51.1")

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Test Implementation
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    
    // AndroidTest Implementation
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.work:work-testing:2.9.0")
    androidTestImplementation("org.mockito:mockito-android:5.11.0")
    androidTestImplementation("com.google.truth:truth:1.1.3")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")


    debugImplementation(libs.androidx.ui.tooling)
}
