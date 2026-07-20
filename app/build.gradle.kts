import java.util.Properties

plugins {
    id("com.android.application")
    id("com.android.compose.screenshot")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
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

    lint {
        // Compose BOM 2024.06's detector cannot read Kotlin 2.1 metadata.
        disable += setOf(
            "FlowOperatorInvokedInComposition",
            "StateFlowValueCalledInComposition",
            "CoroutineCreationDuringComposition",
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.4"))
    implementation("io.github.jan-tennert.supabase:supabase-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt") {
        exclude(group = "io.github.jan-tennert.supabase", module = "auth-kt")
        exclude(group = "io.github.jan-tennert.supabase", module = "auth-kt-android")
    }
    implementation("io.github.jan-tennert.supabase:realtime-kt") {
        exclude(group = "io.github.jan-tennert.supabase", module = "auth-kt")
        exclude(group = "io.github.jan-tennert.supabase", module = "auth-kt-android")
    }
    implementation("io.ktor:ktor-client-android:3.1.2")
    implementation("io.ktor:ktor-client-cio:3.1.2")

    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.robolectric:robolectric:4.13")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    screenshotTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    screenshotTestImplementation("androidx.compose.ui:ui-tooling")
    screenshotTestImplementation("androidx.compose.ui:ui-tooling-preview")
    screenshotTestImplementation("com.android.tools.screenshot:screenshot-validation-api:0.0.1-alpha10")
}

// The screenshot plugin repeats several large classpaths in JVM properties. On
// Windows that exceeds CreateProcess' 32K command limit, so launch the exact same
// Java command through an argument file. Linux CI keeps the plugin's native action.
if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
    fun String.toJavaArgFileLine(): String =
        "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

    tasks.withType<JavaExec>().configureEach {
        if (name == "updateDebugScreenshotTest") {
            val taskName = name
            actions.clear()
            doLast {
                val testEngineInput = javaClass
                    .getMethod("getTestEngineInput")
                    .invoke(this)
                val copiedJvmArgs = mutableListOf<String>()
                val copyJvmArgsMethod = Class.forName(
                    "com.android.compose.screenshot.tasks.PreviewScreenshotTestEngineInputKt",
                    true,
                    javaClass.classLoader,
                ).methods.single { it.name == "copyJvmArgsTo" }
                val addJvmArg: (String) -> Unit = { copiedJvmArgs += it }
                copyJvmArgsMethod.invoke(null, testEngineInput, addJvmArg)

                fun testProjectLocations(getterName: String): List<File> {
                    val property = testEngineInput.javaClass
                        .getMethod(getterName)
                        .invoke(testEngineInput) as org.gradle.api.provider.Provider<*>
                    return (property.get() as Iterable<*>)
                        .map { (it as org.gradle.api.file.FileSystemLocation).asFile }
                }
                val screenshotScanLocations =
                    testProjectLocations("getTestProjectJars") +
                        testProjectLocations("getTestProjectClassDirs")

                val argumentFile = layout.buildDirectory
                    .file("tmp/screenshotArgs/$taskName/args.txt")
                    .get()
                    .asFile
                argumentFile.parentFile.mkdirs()
                val launchArguments = buildList {
                    addAll(allJvmArgs)
                    addAll(copiedJvmArgs)
                    add("-cp")
                    add(classpath.asPath)
                    add(mainClass.get())
                    addAll(args.orEmpty())
                    argumentProviders.forEach { provider ->
                        addAll(provider.asArguments())
                    }
                    screenshotScanLocations.forEach { location ->
                        add("--scan-class-path=${location.absolutePath}")
                    }
                }
                argumentFile.writeText(
                    launchArguments.joinToString(System.lineSeparator()) {
                        it.toJavaArgFileLine()
                    },
                    Charsets.UTF_8,
                )

                val taskEnvironment = environment.mapValues { it.value.toString() }
                val javaExecutable = javaLauncher.orNull
                    ?.executablePath
                    ?.asFile
                    ?.absolutePath
                    ?: File(System.getProperty("java.home"), "bin/java.exe").absolutePath
                val consoleLog = layout.buildDirectory
                    .file("tmp/screenshotArgs/$taskName/console.log")
                    .get()
                    .asFile
                val process = ProcessBuilder(javaExecutable, "@${argumentFile.absolutePath}")
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .redirectOutput(consoleLog)
                    .apply {
                        environment().putAll(taskEnvironment)
                    }
                    .start()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    val output = consoleLog.takeIf { it.isFile }
                        ?.readText(Charsets.UTF_8)
                        .orEmpty()
                        .takeLast(8_000)
                    throw GradleException("$taskName failed with exit code $exitCode\n$output")
                }
            }
        }
    }

    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        if (name == "validateDebugScreenshotTest") {
            val taskName = name
            actions.clear()
            doLast {
                val testEngineInput = javaClass
                    .getMethod("getTestEngineInput")
                    .invoke(this)
                val copiedJvmArgs = mutableListOf<String>()
                val copyJvmArgsMethod = Class.forName(
                    "com.android.compose.screenshot.tasks.PreviewScreenshotTestEngineInputKt",
                    true,
                    javaClass.classLoader,
                ).methods.single { it.name == "copyJvmArgsTo" }
                val addJvmArg: (String) -> Unit = { copiedJvmArgs += it }
                copyJvmArgsMethod.invoke(null, testEngineInput, addJvmArg)

                fun testProjectLocations(getterName: String): List<File> {
                    val property = testEngineInput.javaClass
                        .getMethod(getterName)
                        .invoke(testEngineInput) as org.gradle.api.provider.Provider<*>
                    return (property.get() as Iterable<*>)
                        .map { (it as org.gradle.api.file.FileSystemLocation).asFile }
                }
                val screenshotScanLocations =
                    testProjectLocations("getTestProjectJars") +
                        testProjectLocations("getTestProjectClassDirs")
                val consoleLauncherJar = configurations.detachedConfiguration(
                    dependencies.create(
                        "org.junit.platform:junit-platform-console-standalone:1.12.0",
                    ),
                ).singleFile
                val launchClasspath = files(classpath, consoleLauncherJar).asPath

                val argumentFile = layout.buildDirectory
                    .file("tmp/screenshotArgs/$taskName/args.txt")
                    .get()
                    .asFile
                argumentFile.parentFile.mkdirs()
                val launchArguments = buildList {
                    addAll(allJvmArgs)
                    addAll(copiedJvmArgs)
                    add("-cp")
                    add(launchClasspath)
                    add("org.junit.platform.console.ConsoleLauncher")
                    add("execute")
                    add("--disable-banner")
                    add("--include-engine=preview-screenshot-test-engine")
                    add("--include-classname=.*")
                    add("--details=none")
                    add("--fail-if-no-tests")
                    screenshotScanLocations.forEach { location ->
                        add("--scan-class-path=${location.absolutePath}")
                    }
                }
                argumentFile.writeText(
                    launchArguments.joinToString(System.lineSeparator()) {
                        it.toJavaArgFileLine()
                    },
                    Charsets.UTF_8,
                )

                val junitXmlDirectory = reports.junitXml.outputLocation.get().asFile
                val htmlReportDirectory = reports.html.outputLocation.get().asFile
                project.delete(junitXmlDirectory, htmlReportDirectory)
                val taskEnvironment = environment.mapValues { it.value.toString() }
                val javaExecutable = javaLauncher.orNull
                    ?.executablePath
                    ?.asFile
                    ?.absolutePath
                    ?: File(System.getProperty("java.home"), "bin/java.exe").absolutePath
                val consoleLog = layout.buildDirectory
                    .file("tmp/screenshotArgs/$taskName/console.log")
                    .get()
                    .asFile
                val process = ProcessBuilder(javaExecutable, "@${argumentFile.absolutePath}")
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .redirectOutput(consoleLog)
                    .apply {
                        environment().putAll(taskEnvironment)
                    }
                    .start()
                val exitCode = process.waitFor()

                if (junitXmlDirectory.isDirectory) {
                    val reportClass = Class.forName(
                        "com.android.compose.screenshot.report.TestReport",
                        true,
                        javaClass.classLoader,
                    )
                    val report = reportClass
                        .getConstructor(File::class.java, File::class.java)
                        .newInstance(junitXmlDirectory, htmlReportDirectory)
                    reportClass
                        .getMethod("generateScreenshotTestReport")
                        .invoke(report)
                }

                if (exitCode != 0) {
                    val output = consoleLog.takeIf { it.isFile }
                        ?.readText(Charsets.UTF_8)
                        .orEmpty()
                        .takeLast(8_000)
                    throw GradleException("$taskName failed with exit code $exitCode\n$output")
                }
            }
        }
    }
}
