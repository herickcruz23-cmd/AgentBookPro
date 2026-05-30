import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val mergedProps = Properties().apply {
    val files = listOf(
        rootProject.file("local.properties"),
        rootProject.file("keys.properties")
    )
    files.forEach { f ->
        if (f.exists()) {
            f.inputStream().use { load(it) }
        }
    }
}

fun prop(key: String, default: String = ""): String =
    (mergedProps.getProperty(key) ?: System.getenv(key) ?: default).trim()

// Acepta API_KEY genérica o cualquiera de los nombres específicos
val apiKey = listOf("API_KEY", "GROQ_API_KEY", "OPENAI_API_KEY")
    .firstNotNullOfOrNull { prop(it).takeIf { v -> v.isNotBlank() } }
    .orEmpty()

val supaUrl = prop("SUPABASE_URL")
val supaKey = prop("SUPABASE_ANON_KEY")
val tgToken = prop("TELEGRAM_BOT_TOKEN")
val tgChat = prop("TELEGRAM_CHAT_ID")

android {
    namespace = "com.agentbook.pro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.agentbook.pro"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "API_KEY", "\"${apiKey}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${supaUrl}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supaKey}\"")
        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"${tgToken}\"")
        buildConfigField("String", "TELEGRAM_CHAT_ID", "\"${tgChat}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.navigation:navigation-compose:2.8.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
