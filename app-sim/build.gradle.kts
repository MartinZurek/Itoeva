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
 * Signaturdaten aus einer NICHT eingecheckten Datei `keystore.properties` im Projekt-Root
 * (Vorlage: `keystore.properties.example`). Der Upload-Schluessel und sein Passwort gehoeren
 * niemals ins Repository - wer ihn hat, kann Updates unter deinem App-Namen veroeffentlichen,
 * und verlierst du ihn, laesst sich die App im Play Store nie wieder aktualisieren.
 *
 * Fehlt die Datei, bleibt der Release-Build unsigniert statt fehlzuschlagen: so laesst sich
 * jederzeit `assembleRelease` bauen und pruefen (etwa ob R8 etwas kaputtoptimiert), auch ohne
 * Zugriff auf den echten Schluessel.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasSigningConfig = keystoreProperties.getProperty("storeFile") != null

android {
    namespace = "com.notime.glyphsim"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.notime.glyphminderwatch"
        // Kein Hardware-SDK-Zwang wie bei :app (Glyph Matrix SDK setzt API 34 voraus) -
        // diese App simuliert die Matrix nur auf dem Bildschirm und soll auf moeglichst
        // vielen Geraeten laufen, daher deutlich niedrigerer minSdk.
        minSdk = 26
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
            // R8: verkleinert und verschleiert den Code. Die noetigen Ausnahmen stehen in
            // proguard-rules.pro und core/consumer-rules.pro - vor allem fuer Enums, die als
            // Name persistiert werden, und fuer den reflektiv erzeugten Watchdog-Worker.
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

    // Room-Migrationstests (siehe androidTest/.../AppDatabaseMigrationTest.kt) laden die
    // exportierten Schema-JSONs zur Laufzeit aus den Assets - ohne diesen Eintrag findet der
    // MigrationTestHelper sie nicht.
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

/**
 * Schreibt bei jedem Build das aktuelle Room-Schema als JSON nach app-sim/schemas.
 *
 * Ohne diesen Export laesst sich keine einzige Migration schreiben oder testen: Room braucht den
 * ALTEN Schemastand, um dagegen zu migrieren, und der existiert sonst nirgends. Genau deshalb
 * sind die Staende vor Version 13 unwiederbringlich verloren (kein Export, keine Versionierung) -
 * ab 13 ist jeder Stand dauerhaft festgehalten und die JSONs gehoeren mit eingecheckt.
 */
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Gemeinsame Datenschicht und Alarmplanung, geteilt mit :app - siehe core/build.gradle.kts
    implementation(project(":core"))
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

    // Die Avatar-Animationslogik (matrix/AvatarAnimations.kt, data/FrameCrossfade.kt) ist reine
    // Kotlin-Punktrechnerei ohne Android-Abhaengigkeit - ihre Tests laufen deshalb als schnelle
    // JVM-Unit-Tests (./gradlew :app-sim:testDebugUnitTest), kein Geraet/Emulator noetig.
    testImplementation(libs.junit)

    // Migrationstests muessen gegen echtes SQLite laufen (Room fuehrt dabei wirklich ein
    // ALTER TABLE aus), deshalb instrumentiert statt als JVM-Unit-Test:
    // ./gradlew :app-sim:connectedDebugAndroidTest - braucht ein angeschlossenes Geraet/Emulator.
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

// Siehe :app/build.gradle.kts fuer die Begruendung (Kopie der fertigen Debug-APK ins
// Projekt-Root, eigener Dateiname damit sie die bestehende Glyphminder-debug.apk nicht
// ueberschreibt). applicationId wurde auf com.notime.glyphminderwatch geaendert (statt
// com.notime.glyphsim), damit sich die APK parallel zu einer bereits installierten
// alten com.notime.glyphsim-Version (anderer Debug-Signaturschluessel auf diesem
// Rechner) installieren laesst, statt mit "Konflikt mit vorhandenem Paket" zu scheitern.
/**
 * Zusaetzliches Ablageziel fuer die fertige Debug-APK, gesetzt ueber `apkDropDir` in
 * `local.properties` (nicht eingecheckt, weil es von Rechner zu Rechner verschieden ist).
 *
 * **Wozu.** Das Projekt lag urspruenglich direkt in einem Google-Drive-Ordner, damit die frisch
 * gebaute APK ohne Umweg auf dem Telefon auftaucht und sich von dort installieren laesst. Das
 * ist ein sinnvoller Weg - aber ein sehr teurer Ort zum Bauen: gemessen auf diesem Rechner ist
 * Schreiben auf dem Drive-Laufwerk rund **30-mal** langsamer als auf der lokalen Platte, und
 * genau das tut Gradle tausendfach. Aus Sekunden wurden Minuten.
 *
 * Mit diesem Eintrag laesst sich beides trennen: **gebaut wird lokal, abgelegt wird in der
 * Cloud.** Nur die eine fertige Datei wandert dorthin, statt des gesamten Zwischenstands.
 *
 * Beispiel in `local.properties`:
 * ```
 * apkDropDir=G:/Meine Ablage/Tama
 * ```
 * Fehlt der Eintrag, passiert nichts weiter - die Kopie im Projekt-Root entsteht wie bisher.
 */
val apkDropDir: String? = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}.getProperty("apkDropDir")?.takeIf { it.isNotBlank() }

tasks.register("copyDebugApkToProjectRoot") {
    dependsOn("assembleDebug")
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/debug/app-sim-debug.apk").get().asFile
        apk.copyTo(File(rootProject.projectDir, "Tama-debug.apk"), overwrite = true)

        // Bewusst fehlertolerant: Ist das Cloud-Laufwerk gerade nicht eingebunden oder der
        // Ordner nicht beschreibbar, soll das den Build NICHT scheitern lassen - kompiliert
        // ist schliesslich alles. Eine Warnung genuegt.
        apkDropDir?.let { target ->
            runCatching {
                val dir = File(target).apply { mkdirs() }
                apk.copyTo(File(dir, "Tama-debug.apk"), overwrite = true)
                logger.lifecycle("APK zusaetzlich abgelegt: ${File(dir, "Tama-debug.apk")}")
            }.onFailure {
                logger.warn("APK konnte nicht nach '$target' kopiert werden: ${it.message}")
            }
        }
    }
}

afterEvaluate {
    tasks.named("assembleDebug") {
        finalizedBy("copyDebugApkToProjectRoot")
    }
}
