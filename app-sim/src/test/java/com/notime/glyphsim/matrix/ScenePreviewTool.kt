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
    /** Gloops Verformung ueber alle Stufen - Ruhe, gequetscht, gestreckt. */
    @Test
    fun dumpGloop() {
        val out = StringBuilder()
        for (phase in -3..3) {
            out.append("Phase ").append(phase).append(System.lineSeparator())
            val body = AvatarBodies.forSpecies(AvatarSpecies.GLOOP)
            val points = AvatarAnimations.creatureFrame(body, accentPhase = phase).toSet()
            for (y in 0 until AvatarGeometry.HEIGHT) {
                val row = StringBuilder()
                for (x in 0 until AvatarGeometry.SIZE) {
                    row.append(if ((x to y) in points) '#' else '.')
                }
                out.append(row).append(System.lineSeparator())
            }
            out.append(System.lineSeparator())
        }
        val target = File(System.getProperty("java.io.tmpdir"), "gloop.txt")
        target.writeText(out.toString())
        println("Gloop geschrieben: ${target.absolutePath}")
    }

    /** Regen und Schnee - draussen und durchs Fenster gesehen. */
    @Test
    fun dumpWeather() {
        val out = StringBuilder()
        for (weather in listOf(PlayWeather.RAIN, PlayWeather.SNOW)) {
            PlayWeather.forceForPreview(weather)
            for (place in listOf(PlayScene.Place.PARK, PlayScene.Place.NOOK, PlayScene.Place.BEDROOM)) {
                out.append(weather.name).append(":").append(System.lineSeparator())
                out.append(ScenePreview.render(place, AvatarSpecies.HOOTLET))
                out.append(System.lineSeparator())
            }
        }
        PlayWeather.forceForPreview(null)
        val target = File(System.getProperty("java.io.tmpdir"), "scene_weather.txt")
        target.writeText(out.toString())
        println("Wetter geschrieben: ${target.absolutePath}")
    }

    /**
     * **Wie sich die Wohnung fuellt** - dieselben drei Raeume auf Stufe 0, 1, 2 und 3.
     *
     * Der einzige Weg, das zu beurteilen, ohne wochenlang zu fuettern: Ob ein Rucksack neben dem
     * Sessel gut aussieht, entscheidet sich im Bild und nicht in der Zahl.
     */
    @Test
    fun dumpAcquisitions() {
        val out = StringBuilder()
        for (path in com.notime.glyphsim.ui.PlayPath.entries) {
            for (stage in 0..3) {
                val owned = com.notime.glyphsim.ui.PlayPath.acquisitionsUpTo(path, stage)
                out.append("=== ").append(path).append(" / Stufe ").append(stage)
                    .append(System.lineSeparator())
                for (place in listOf(
                    PlayScene.Place.CRAFT, PlayScene.Place.LIVING, PlayScene.Place.BEDROOM
                )) {
                    out.append(
                        ScenePreview.render(
                            place, AvatarSpecies.PUFFLING, acquisitions = owned, showAvatar = false
                        )
                    ).append(System.lineSeparator())
                }
            }
        }
        val target = File(System.getProperty("java.io.tmpdir"), "scene_acquisitions.txt")
        target.writeText(out.toString())
        println("Erwerbungen geschrieben: ${target.absolutePath}")
    }

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
