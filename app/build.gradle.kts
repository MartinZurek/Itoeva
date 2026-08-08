// Explizit importiert: im Gradle-Kotlin-DSL zeigt `java` auf Gradles java-Extension,
// `java.util.Properties` laesst sich daher nicht voll qualifiziert schreiben.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/** Signaturdaten aus `keystore.properties` - siehe app-sim/build.gradle.kts fuer die Begruendung. */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasSigningConfig = keystoreProperties.getProperty("storeFile") != null

android {
    namespace = "com.notime.glyphkalender"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.notime.glyphkalender"
        // Glyph Matrix SDK setzt Android 14 (API 34) voraus
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 aktiv - Ausnahmen in proguard-rules.pro (Glyph SDK) und
            // core/consumer-rules.pro (Enums, Worker, Entities).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    // Siehe app-sim/build.gradle.kts - die exportierten Schema-JSONs muessen fuer den
    // MigrationTestHelper als Assets im androidTest-Sourceset liegen.
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

/** Schema-Export, siehe app-sim/build.gradle.kts fuer die ausfuehrliche Begruendung. */
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Gemeinsame Datenschicht und Alarmplanung, geteilt mit :app-sim - siehe core/build.gradle.kts
    implementation(project(":core"))
    // Glyph Matrix SDK (AAR) - siehe README.md zum Download
    implementation(files("libs/glyphsdk.aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)

    // Migrationstests, siehe app-sim/build.gradle.kts:
    // ./gradlew :app:connectedDebugAndroidTest (braucht Geraet/Emulator)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

// Kopiert die fertige Debug-APK zusaetzlich ins Projekt-Root, damit sie dort
// ohne Suchen im build/-Ordner zum Installieren griffbereit liegt.
// Bewusst kein deklarativer Copy-Task: das Ziel (Projekt-Root) enthaelt Gradles
// eigene .gradle/-Statusdateien, an denen die Incremental-Build-Snapshotting von
// Copy sich verschluckt (gesperrte buildOutputCleanup.lock).
tasks.register("copyDebugApkToProjectRoot") {
    dependsOn("assembleDebug")
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        apk.copyTo(File(rootProject.projectDir, "Glyphminder-debug.apk"), overwrite = true)
    }
}

afterEvaluate {
    tasks.named("assembleDebug") {
        finalizedBy("copyDebugApkToProjectRoot")
    }
}
