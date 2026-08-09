// Explizit importiert: im Gradle-Kotlin-DSL zeigt `java` auf Gradles java-Extension,
// `java.util.Properties` laesst sich daher nicht voll qualifiziert schreiben.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * Signaturdaten aus `keystore.properties` - siehe app-sim/build.gradle.kts fuer die ausfuehrliche
 * Begruendung, auch zur Trennung von `release` und `releaseCheck`.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val signingKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val signingProblems: List<String> = buildList {
    if (!keystorePropertiesFile.exists()) {
        add("keystore.properties fehlt im Projekt-Root. Vorlage: keystore.properties.example")
        return@buildList
    }
    signingKeys.forEach { key ->
        val value = keystoreProperties.getProperty(key)
        when {
            value.isNullOrBlank() ->
                add("keystore.properties: Eintrag '$key' fehlt oder ist leer.")
            value.startsWith("DEIN_") ->
                add("keystore.properties: Eintrag '$key' steht noch auf dem Platzhalter der Vorlage.")
        }
    }
    keystoreProperties.getProperty("storeFile")?.takeIf { it.isNotBlank() }?.let { path ->
        if (!rootProject.file(path).exists()) {
            add("Der Schluesselspeicher '$path' existiert nicht (relativ zum Projekt-Root gesucht).")
        }
    }
}
val hasSigningConfig = signingProblems.isEmpty()

val appVersionCode = 1
val appVersionName = "1.0"

/**
 * Der Produktivschluessel fuer das Glyph Matrix SDK - **nie eingecheckt**.
 *
 * **Was er ist.** Das SDK laesst sich nur ansprechen, wenn die App sich mit einem Schluessel
 * ausweist, den Nothing auf ihren Paketnamen ausstellt (Nothing Developer Programme). Bis hierher
 * stand stattdessen der oeffentliche Test-Key `"test"` aus dem Demoprojekt fest im Manifest. Der
 * funktioniert nur gegen die Entwickler-Firmware: in einer veroeffentlichten App bliebe die Matrix
 * schlicht dunkel, und zwar ohne Fehlermeldung, die darauf hinweist.
 *
 * **Warum ueber einen Platzhalter statt direkt im Manifest.** Ein Schluessel im Manifest ist ein
 * Geheimnis in einer eingecheckten Datei - er waere in der Git-Historie und in jeder Kopie des
 * Repositories. Ueber `manifestPlaceholders` haengt er stattdessen am Build-Typ: Debug und
 * `releaseCheck` bekommen `"test"`, nur der echte Release-Bau den produktiven Wert. Fehlt er dort,
 * bricht `validateRelease` unten ab, statt ein Paket mit dunkler Matrix auszuliefern.
 *
 * **Wo er hingehoert.** Entweder als `nothingKey=...` in der (nicht eingecheckten)
 * `keystore.properties`, oder als Umgebungsvariable `NOTHING_KEY` - letzteres fuer die CI, wo die
 * Datei nicht existiert und der Wert aus dem Geheimnisspeicher kommt.
 */
val productionNothingKey: String? =
    (keystoreProperties.getProperty("nothingKey") ?: System.getenv("NOTHING_KEY"))
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it != "test" }

android {
    namespace = "com.notime.glyphkalender"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.notime.glyphkalender"
        // Glyph Matrix SDK setzt Android 14 (API 34) voraus
        minSdk = 34
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
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
        debug {
            // Der oeffentliche Test-Key aus dem Nothing-Demoprojekt. Er autorisiert nur gegen die
            // Entwickler-Firmware und ist genau hier - und nur hier - richtig aufgehoben.
            manifestPlaceholders["nothingKey"] = "test"
        }
        release {
            // R8 aktiv - Ausnahmen in proguard-rules.pro (Glyph SDK) und
            // core/consumer-rules.pro (Enums, Worker, Entities).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["nothingKey"] = productionNothingKey ?: ""
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        /** Probe-Release, siehe app-sim/build.gradle.kts fuer die Begruendung. */
        create("releaseCheck") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".releasecheck"
            versionNameSuffix = "-releasecheck"
            // Hier zaehlt, ob R8 den Bau ueberlebt, nicht ob die Matrix wirklich freigeschaltet
            // wird - deshalb der Test-Key statt eines Abbruchs wegen fehlendem Produktivschluessel.
            manifestPlaceholders["nothingKey"] = "test"
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

    /** Siehe app-sim/build.gradle.kts fuer die Begruendung. */
    lint {
        lintConfig = rootProject.file("config/lint.xml")
        abortOnError = true
        warningsAsErrors = false
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

/**
 * Torwaechter vor dem offiziellen Release-Paket - siehe app-sim/build.gradle.kts fuer die
 * ausfuehrliche Begruendung. Zusaetzlich geprueft wird hier der Produktivschluessel fuers
 * Glyph Matrix SDK, den nur dieses Modul braucht.
 */
val validateRelease = tasks.register("validateRelease") {
    group = "verification"
    description = "Prueft Signaturdaten, NothingKey, Room-Schema-Export und Versionsangaben."

    val databaseSource = file("src/main/java/com/notime/glyphkalender/data/AppDatabase.kt")
    val schemaDir = file("schemas/com.notime.glyphkalender.data.AppDatabase")
    val problems = signingProblems
    val nothingKey = productionNothingKey
    val versionCode = appVersionCode
    val versionName = appVersionName

    doLast {
        val found = buildList {
            addAll(problems)

            if (nothingKey == null) {
                add(
                    "Kein Produktivschluessel fuers Glyph Matrix SDK. Ohne ihn bleibt die Matrix " +
                        "im veroeffentlichten Paket dunkel. Beantragen ueber das Nothing Developer " +
                        "Programme, dann als 'nothingKey=...' in keystore.properties oder als " +
                        "Umgebungsvariable NOTHING_KEY hinterlegen."
                )
            }

            val annotation = databaseSource.readText()
                .substringAfter("@Database(", "")
                .substringBefore(")")
            val declaredVersion = Regex("""version\s*=\s*(\d+)""").find(annotation)?.groupValues?.get(1)
            when {
                declaredVersion == null ->
                    add("Aus ${databaseSource.name} liess sich keine @Database(version = ...) lesen.")
                !schemaDir.resolve("$declaredVersion.json").exists() ->
                    add(
                        "Room-Schema $declaredVersion.json fehlt in ${schemaDir.relativeTo(rootDir)}. " +
                            "Einmal bauen und die erzeugte Datei mit einchecken."
                    )
            }

            if (versionCode < 1) {
                add("versionCode muss positiv sein, ist aber $versionCode.")
            }
            if (!Regex("""\d+\.\d+(\.\d+)?""").matches(versionName)) {
                add("versionName '$versionName' passt nicht auf das Schema major.minor[.patch].")
            }
        }

        if (found.isNotEmpty()) {
            error(
                buildString {
                    appendLine("Das Release-Paket ist so nicht veroeffentlichungsfaehig:")
                    appendLine()
                    found.forEach { appendLine("  - $it") }
                    appendLine()
                    appendLine("Nur R8 pruefen, ohne Schluessel? Dann stattdessen:")
                    append("  ./gradlew :app:assembleReleaseCheck")
                }
            )
        }
    }
}

tasks.matching { it.name == "packageRelease" || it.name == "packageReleaseBundle" }
    .configureEach { dependsOn(validateRelease) }

/**
 * Haelt kotlinx-serialization in den Instrumentierungstests zusammen - ausfuehrliche Begruendung
 * in app-sim/build.gradle.kts. Kurz: Room liest die Schema-JSONs damit, und ein zu alter Kern
 * laesst jeden Migrationstest mit `AbstractMethodError` scheitern, bevor er ueberhaupt anfaengt.
 */
configurations.matching { it.name.contains("AndroidTest") }.configureEach {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1")
    }
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
