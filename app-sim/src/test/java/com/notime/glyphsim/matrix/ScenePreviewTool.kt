package com.notime.glyphsim.matrix

import java.io.File
import org.junit.Test

/**
 * **Kein Regressionstest, sondern ein Knopf.** Schreibt alle Szenen als Zeichenraster in eine
 * Datei, damit sich das Aussehen der Welt ansehen laesst, ohne die App zu starten.
 *
 * Aufruf:
 * `gradlew :app-sim:testDebugUnitTest --tests "*ScenePreviewTool"`
 * Ergebnis: `scene_preview.txt` im Temp-Verzeichnis (Pfad steht am Ende der Datei).
 *
 * **Warum das hier steht und nicht jedes Mal neu geschrieben wird.** Beim Aufbau dieser Welt
 * wurde genau dieser Ausgabe-Code ein halbes Dutzend Mal angelegt, benutzt und wieder geloescht -
 * jedes Mal etwas anders, jedes Mal ein paar Minuten. Er gehoert dauerhaft dazu: Solange die
 * Gestaltung aus Zellen und Helligkeiten besteht, ist ein Zeichenraster die einzige Moeglichkeit,
 * eine Szene zu beurteilen, ohne ein Geraet in der Hand zu haben.
 *
 * Die Pruefung, ob eine Szene in Ordnung IST, macht dagegen SceneCompositionTest - dieses
 * Werkzeug zeigt nur.
 */
class ScenePreviewTool {

    @Test
    fun dumpAllScenes() {
        val target = File(System.getProperty("java.io.tmpdir"), "scene_preview.txt")
        ScenePreview.dumpAll(target)
        println("Szenen-Vorschau geschrieben: ${target.absolutePath}")
    }

    /**
     * Dasselbe im QUERFORMAT - die Breite, bei der gemeldet wurde, dass alles auseinandersteht.
     * Ein eigener Knopf, weil sich die Anordnung dort anders verhaelt (siehe
     * PlayScene.MAX_ROOM_CELLS) und man beides nebeneinander sehen koennen muss.
     */
    @Test
    fun dumpLandscape() {
        val out = StringBuilder()
        for (place in PlayScene.Place.entries) {
            out.append(ScenePreview.render(place, AvatarSpecies.PUFFLING, width = 84))
            out.append(System.lineSeparator())
        }
        val target = File(System.getProperty("java.io.tmpdir"), "scene_landscape.txt")
        target.writeText(out.toString())
        println("Querformat geschrieben: ${target.absolutePath}")
    }
}
