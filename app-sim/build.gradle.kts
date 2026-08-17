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
 * **Frueher blieb der Release-Build ohne diese Datei stillschweigend unsigniert.** Das war als
 * Bequemlichkeit gedacht (R8 pruefen ohne Schluessel), hatte aber einen teuren Preis: `assembleRelease`
 * lieferte in genau demselben Ordner, unter genau demselben Namen, mal ein veroeffentlichungsfaehiges
 * und mal ein wertloses Paket - ohne dass man den Unterschied ansieht. Ein unsigniertes AAB laedt der
 * Play Store zwar nicht hoch, aber der Fehler faellt dann erst nach dem Bauen und Hochladen auf.
 *
 * Beides ist jetzt getrennt:
 *
 * - `assembleRelease` / `bundleRelease` sind **offiziell** und scheitern ohne vollstaendige
 *   Signaturdaten (siehe `validateRelease` unten).
 * - Der Build-Typ **`releaseCheck`** uebernimmt die alte Rolle: dieselbe R8-Verkleinerung, aber
 *   mit dem Debug-Schluessel signiert und unter eigenem Namen. Damit laesst sich weiter jederzeit
 *   pruefen, ob R8 etwas kaputtoptimiert - ohne Zugriff auf den echten Schluessel und ohne dass
 *   dabei je etwas entsteht, das man versehentlich fuer ein Release halten koennte.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

/** Die vier Eintraege, ohne die sich nicht signieren laesst. */
val signingKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")

/**
 * Was an den Signaturdaten fehlt - eine leere Liste heisst "vollstaendig und plausibel".
 *
 * Geprueft wird bewusst mehr als nur die Existenz der Datei: eine aus der Vorlage kopierte, aber
 * nie ausgefuellte `keystore.properties` ist der wahrscheinlichste Fehler ueberhaupt, und sie
 * wuerde sonst als "vorhanden" durchgehen und den Build erst viel spaeter mit einer
 * Keystore-Fehlermeldung aus dem Java-Unterbau abbrechen.
 */
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

/**
 * Versionsangaben als benannte Werte statt direkt in `defaultConfig`, damit `validateRelease`
 * unten dieselben Zahlen pruefen kann, die auch tatsaechlich ins Paket wandern.
 *
 * `versionCode` muss bei **jedem** Upload in den Play Store hoeher sein als beim vorherigen -
 * der Store lehnt eine bereits verwendete Nummer ab, und nachtraeglich aendern laesst sie sich
 * nicht. Siehe README, Abschnitt "Veroeffentlichen".
 *
 * **Zwei Quellen, mit Absicht.** Ohne Angabe gelten die Werte hier; ein lokal gebautes Paket
 * sieht damit aus wie bisher. Die Auslieferung ueber `.github/workflows/deliver-apk.yml`
 * uebersteuert sie dagegen bei jedem Lauf ueber `-PitoevaVersionCode`/`-PitoevaVersionName`,
 * weil dort aus demselben Quelltext fortlaufend neue Pakete entstehen: Ohne aufsteigende Nummer
 * liesse sich am Telefon nicht mehr ablesen, welcher Stand gerade installiert ist - jedes Update
 * hiesse weiterhin "1.0".
 *
 * Ein unbrauchbarer Wert bricht ab, statt still auf den Standard zurueckzufallen. Eine
 * zurueckgefallene Versionsnummer faellt sonst erst auf dem Geraet auf, und dort als
 * Downgrade, das sich gar nicht erst installieren laesst.
 */
val appVersionCode: Int = providers.gradleProperty("itoevaVersionCode").orNull
    ?.let { it.toIntOrNull() ?: error("itoevaVersionCode ist keine ganze Zahl: '$it'") }
    ?: 1
val appVersionName: String = providers.gradleProperty("itoevaVersionName").orNull ?: "1.0"

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

        /**
         * Probe-Release: alles wie `release`, nur mit dem Debug-Schluessel.
         *
         * Dafuer da, R8 zu pruefen - Verkleinerung und Verschleierung koennen Dinge zerlegen, die
         * im Debug-Bau tadellos laufen (reflektiv erzeugte Worker, ueber ihren Namen persistierte
         * Enums, Room-Entities). Das faellt sonst erst dem Nutzer auf.
         *
         * Debug-signiert statt unsigniert, weil ein unsigniertes Paket sich nicht installieren
         * laesst - und "baut durch" ist die schwaechere Aussage. Erst das Starten auf einem Geraet
         * zeigt, ob R8 wirklich nichts weggeworfen hat, was zur Laufzeit gebraucht wird.
         *
         * `matchingFallbacks` ist noetig, weil :core diesen Build-Typ nicht kennt: ohne die Angabe
         * findet Gradle keine passende Variante der Bibliothek und bricht die Aufloesung ab.
         */
        create("releaseCheck") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            // Sonst haetten zwei verschiedene Pakete dieselbe applicationId und liessen sich nicht
            // nebeneinander installieren - gerade beim Vergleichen "Debug laeuft, minifiziert nicht"
            // will man aber beide gleichzeitig auf dem Geraet haben.
            applicationIdSuffix = ".releasecheck"
            versionNameSuffix = "-releasecheck"
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

    /**
     * Lint laeuft als Teil von `gradlew verify` (siehe Wurzel-Build) und bricht bei Fehlern ab -
     * das ist AGPs Voreinstellung und bleibt so.
     *
     * `warningsAsErrors` steht bewusst noch auf `false`: die Warnungen sind erst mit dieser
     * Pruefstufe ueberhaupt sichtbar geworden und werden ueber die spaeteren Phasen abgearbeitet
     * (Barrierefreiheit, Lokalisierung). Sie sofort scharfzuschalten hiesse, alles andere so lange
     * anzuhalten. Sobald die Liste leer ist, gehoert der Schalter auf `true`, sonst sammeln sich
     * neue Warnungen genauso unbemerkt an wie die alten.
     */
    lint {
        lintConfig = rootProject.file("config/lint.xml")
        abortOnError = true
        warningsAsErrors = false
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

/**
 * Torwaechter vor dem offiziellen Release-Paket.
 *
 * **Wogegen das schuetzt.** Jeder einzelne der hier geprueften Punkte hat gemeinsam, dass er den
 * Build *nicht* zum Scheitern bringt, sondern still ein Paket erzeugt, mit dem etwas nicht stimmt.
 * Genau solche Fehler faellt erst nach dem Hochladen auf - oder, im Fall des fehlenden
 * Room-Schemas, erst Monate spaeter, wenn sich die naechste Migration nicht mehr schreiben laesst,
 * weil der Ausgangsstand nirgends festgehalten wurde.
 *
 * Bewusst an die **Paketierung** gehaengt und nicht an `preReleaseBuild`: Unit-Tests der
 * Release-Variante (`testReleaseUnitTest`) haengen ebenfalls am Kompilieren der Release-Variante.
 * Wuerde die Pruefung dort ansetzen, liessen sich die Tests auf keinem Rechner mehr ausfuehren, der
 * den Upload-Schluessel nicht hat - also auch nicht in der CI, wo sie am noetigsten sind.
 */
val validateRelease = tasks.register("validateRelease") {
    group = "verification"
    description = "Prueft Signaturdaten, Room-Schema-Export und Versionsangaben des Release-Pakets."

    // Ausserhalb von doLast eingesammelt: im Ausfuehrungsteil darf nicht mehr auf das
    // Project-Objekt zugegriffen werden (Configuration Cache).
    val databaseSource = file("src/main/java/com/notime/glyphsim/data/AppDatabase.kt")
    val schemaDir = file("schemas/com.notime.glyphsim.data.AppDatabase")
    val problems = signingProblems
    val versionCode = appVersionCode
    val versionName = appVersionName

    doLast {
        val found = buildList {
            addAll(problems)

            // --- Room-Schema-Export ---------------------------------------------------------
            // Die Version steht in der @Database-Annotation; der zugehoerige JSON-Export muss
            // eingecheckt sein, sonst fehlt der naechsten Migration ihr Ausgangspunkt.
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

            // --- Versionsangaben ------------------------------------------------------------
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
                    append("  ./gradlew :app-sim:assembleReleaseCheck")
                }
            )
        }
    }
}

// `matching` statt `named`: die Namen der Paketier-Tasks gehoeren AGP, und ein Tippfehler oder eine
// Umbenennung in einer kuenftigen Version soll den Build nicht sofort zerlegen - sie soll die
// Pruefung hoechstens nicht mehr finden. Deshalb steht in der CI zusaetzlich ein Lauf, der
// `assembleRelease` ohne Schluessel erwartungsgemaess scheitern sieht.
tasks.matching { it.name == "packageRelease" || it.name == "packageReleaseBundle" }
    .configureEach { dependsOn(validateRelease) }

/**
 * Haelt kotlinx-serialization in den Instrumentierungstests zusammen.
 *
 * **Das Problem.** Room 2.8.4 liest die exportierten Schema-JSONs mit kotlinx-serialization -
 * `room-testing` zieht dafuer `kotlinx-serialization-json:1.8.1` herein. Gleichzeitig steht auf
 * `kotlinx-serialization-core` eine `strictly 1.7.3`-Bedingung, die den Kern auf 1.7.3
 * zurueckdrueckt. Damit laeuft JSON 1.8.1 gegen einen aelteren Kern.
 *
 * Zwischen beiden Staenden hat sich `GeneratedSerializer.typeParametersSerializers()` geaendert:
 * in 1.8 hat die Methode eine Standardimplementierung bekommen, in 1.7.3 ist sie abstrakt. Der
 * gegen 1.8 uebersetzte Code verlaesst sich auf die Standardimplementierung, findet sie im
 * aelteren Kern nicht - und jeder Zugriff endet in einem `AbstractMethodError`.
 *
 * **Was das bedeutete.** Nicht "die Tests waren noch nicht gelaufen", sondern: **sie konnten gar
 * nicht laufen.** Jeder Migrationstest scheiterte beim Laden des Schemas, noch bevor eine einzige
 * Migration ausgefuehrt wurde. Aufgefallen ist es erst, als zum ersten Mal ueberhaupt ein Emulator
 * da war - genau das ist der Preis von Tests, die nie jemand ausfuehrt.
 *
 * `force` und nicht einfach eine Versionsangabe: gegen ein `strictly` kommt ein gewoehnlicher
 * Wunsch nicht an. Nur die Testkonfigurationen sind betroffen - im ausgelieferten Paket steckt
 * kotlinx-serialization gar nicht.
 */
configurations.matching { it.name.contains("AndroidTest") }.configureEach {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1")
    }
}

dependencies {
    // Gemeinsame Datenschicht und Alarmplanung, geteilt mit :app - siehe core/build.gradle.kts
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
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
    // Compose-Tests, siehe androidTest/.../ReminderScreenTest.kt
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
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
