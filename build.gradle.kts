plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

/**
 * Mindest-JDK fuer den Build selbst.
 *
 * **Nicht zu verwechseln mit dem Bytecode-Ziel.** Hier geht es um die JVM, in der Gradle und AGP
 * laufen: AGP 8.13 laedt Klassen im Format 61 (Java 17) und scheitert unter einem aelteren JDK mit
 * einer `UnsupportedClassVersionError` tief im Plugin-Ladevorgang - eine Meldung, aus der niemand
 * die eigentliche Ursache abliest. Die Pruefung unten fangt das mit Klartext ab.
 *
 * Wovon das unabhaengig ist: `compileOptions.sourceCompatibility`/`targetCompatibility` in den
 * Modulen stehen auf **Java 11** und bleiben dort. Das ist das Format, in dem der *ausgelieferte*
 * Bytecode entsteht, und es richtet sich nach `minSdk` (26) und dem, was ART ohne aufwendiges
 * Desugaring versteht - nicht nach dem JDK des Entwicklers. Beide Zahlen duerfen und sollen
 * auseinanderliegen; siehe README, Abschnitt "Werkzeugkette".
 *
 * 21 (Android Studios gebuendeltes JBR) ist ebenfalls in Ordnung und der hier tatsaechlich
 * verwendete Stand - deshalb eine Untergrenze statt einer exakten Version.
 */
val requiredJdk = JavaVersion.VERSION_17
check(JavaVersion.current() >= requiredJdk) {
    """
    Dieser Build braucht mindestens JDK $requiredJdk, gefunden wurde JDK ${JavaVersion.current()}.

    Gradle laeuft gerade unter: ${System.getProperty("java.home")}

    Abhilfe: JAVA_HOME auf ein JDK >= $requiredJdk setzen. Android Studio bringt ein passendes mit,
    typischerweise unter <Android Studio>/jbr - unter Windows also etwa
      ${'$'}env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
    """.trimIndent()
}

/**
 * Ein Aufruf, der alles ausfuehrt, was ohne angeschlossenes Geraet pruefbar ist.
 *
 * **Wozu ueberhaupt ein Sammel-Task:** Die einzelnen Stufen (Unit-Tests, Lint, minifizierter
 * Release-Bau) waren bisher nur als Wissen im Kopf vorhanden und wurden entsprechend
 * unterschiedlich vollstaendig ausgefuehrt. Die CI ruft genau diesen Task auf - damit ist
 * "was pruefe ich vor dem Einchecken" und "was prueft die CI" per Konstruktion dasselbe,
 * statt zwei Listen, die auseinanderlaufen.
 *
 * Bewusst NICHT enthalten:
 *
 * - Alles, was ein Geraet oder einen Emulator braucht (`connectedDebugAndroidTest`, insbesondere
 *   die Room-Migrationstests). Diese Stufe muss laufen koennen, wo kein Geraet haengt, sonst wird
 *   sie uebersprungen statt benutzt. Sie steht als eigener Punkt im README und in der CI.
 * - Der **echte** Release-Bau (`assembleRelease`/`bundleRelease`). Der verlangt seit Phase 0.2
 *   vollstaendige Signaturdaten und scheitert ohne sie mit Absicht - er kann hier also gar nicht
 *   mitlaufen. Geprueft wird R8 stattdessen ueber den Build-Typ `releaseCheck`, der dieselbe
 *   Verkleinerung/Verschleierung fahrt, aber ohne Upload-Schluessel auskommt.
 *   Siehe app-sim/build.gradle.kts.
 */
tasks.register("verify") {
    group = "verification"
    description = "Unit-Tests, Lint und minifizierter Probe-Bau aller Module (ohne Geraet, ohne Schluessel)."
    dependsOn(
        subprojects.map { "${it.path}:test" } +
            subprojects.map { "${it.path}:lint" } +
            ":app-sim:assembleReleaseCheck" +
            ":app:assembleReleaseCheck"
    )
}
