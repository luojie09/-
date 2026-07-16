import java.util.Properties

plugins {
    id("com.android.application")
    id("com.android.compose.screenshot")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun localProperty(name: String): String = localProperties.getProperty(name)?.trim().orEmpty()

fun appConfig(name: String): String =
    providers.gradleProperty(name).orNull?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }
        ?: localProperty(name)

fun String.toBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.secretbase.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.secretbase.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", appConfig("SUPABASE_URL").toBuildConfigString())
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            appConfig("SUPABASE_PUBLISHABLE_KEY").toBuildConfigString(),
        )

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
        screenshotTests {
            imageDifferenceThreshold = 0.001f
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.4"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt") {
        exclude(group = "io.github.jan-tennert.supabase", module = "auth-kt")
        exclude(group = "io.github.jan-tennert.supabase", module = "auth-kt-android")
    }
    implementation("io.ktor:ktor-client-android:3.1.2")

    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    screenshotTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    screenshotTestImplementation("androidx.compose.ui:ui-tooling")
    screenshotTestImplementation("androidx.compose.ui:ui-tooling-preview")
    screenshotTestImplementation("com.android.tools.screenshot:screenshot-validation-api:0.0.1-alpha10")
}
